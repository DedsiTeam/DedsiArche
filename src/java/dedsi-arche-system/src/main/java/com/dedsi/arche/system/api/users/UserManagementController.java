package com.dedsi.arche.system.api.users;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.domain.users.*;
import com.dedsi.arche.system.domain.users.query.*;
import com.dedsi.arche.system.domain.positions.IPositionRepository;
import com.dedsi.arche.system.api.users.request.*;
import com.dedsi.arche.system.infrastructure.users.security.TokenService;
import com.dedsi.arche.system.infrastructure.users.security.UserAdministration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 管理员维护完整用户聚合；岗位配置不改变现有管理员授权或凭据失效规则。 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class UserManagementController {
    /* 用户完整聚合仓储。 */
    private final IUserRepository users;
    /* 用户摘要投影查询。 */
    private final IUserQuery query;
    /* 密码摘要生成器。 */
    private final PasswordEncoder passwords;
    /* 原有会话撤销服务。 */
    private final TokenService tokens;
    /* 部署配置的管理员保护规则。 */
    private final UserAdministration administration;
    /* 岗位根锁定及快照数据来源。 */
    private final IPositionRepository positions;

    /** 注入原有管理依赖及岗位仓储。 */
    public UserManagementController(
            IUserRepository users, IUserQuery query, PasswordEncoder passwords,
            TokenService tokens, UserAdministration administration, IPositionRepository positions
    ) {
        this.users = users;
        this.query = query;
        this.passwords = passwords;
        this.tokens = tokens;
        this.administration = administration;
        this.positions = positions;
    }

    /** 同一查询快照返回用户资料及完整岗位子实体。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "包含完整 positions 快照集合；分页仍返回用户摘要。")
    @Transactional(readOnly = true)
    public ManagedUserDetailDto detail(@PathVariable UUID id) {
        return dto(find(id));
    }

    /** 保留密码重置的版本检查与会话失效规则。 */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "立即使该用户旧访问令牌和刷新令牌失效。")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        var user = find(id);
        checkVersion(user, request.version());
        validatePasswordBytes(request.password());
        user.resetPassword(passwords.encode(request.password()));
        users.flush();
        tokens.revokeAll(id);
    }

    /** 删除用户及其会话、岗位子实体，保留独立的岗位和权限定义。 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除用户、刷新会话和用户岗位关联；保留岗位与权限。当前账户和配置中的管理员不可删除。")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(
            @PathVariable UUID id, @RequestParam @Min(0) long version,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var user = find(id);
        if (id.toString().equals(jwt.getSubject()) || administration.isAdministrator(user.getUsername())) {
            throw new BusinessException("PROTECTED_USER", "不能删除当前账户或配置中的管理员");
        }
        checkVersion(user, version);
        tokens.deleteSessions(id);
        users.delete(user);
        users.flush();
    }

    /** 保留原有分页结构和筛选条件，不加载岗位集合。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询用户", description = "按账号或姓名、手机号、邮箱和状态筛选；分页为原摘要，不包含 positions。页码从 1 开始。")
    @Transactional(readOnly = true)
    public UserPage list(@Valid @RequestBody UserPageRequest request) {
        return query.search(request.keyword(), request.phone(), request.email(),
                request.enabled(), request.page(), request.size());
    }

    /** 管理员创建用户并整存岗位集合；公开注册请求不接受岗位分配。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增用户", description = "默认启用；positionIds 省略或 null 为空。岗位四个快照字段由服务端读取，不赋予管理或登录权限。")
    @Transactional
    public ManagedUserDetailDto create(@Valid @RequestBody CreateManagedUserRequest request) {
        validatePasswordBytes(request.password());
        if (users.findByUsername(User.normalizeUsername(request.username())).isPresent()) {
            throw new BusinessException("USERNAME_EXISTS", "账号已存在");
        }
        var selected = resolvePositions(request.positionIds());
        var user = User.register(request.username(), request.displayName(), passwords.encode(request.password()));
        user.updateContact(request.phone(), request.email());
        user.replacePositions(selected);
        user = users.save(user);
        users.flush();
        return dto(user);
    }

    /** 在同一用户事务维护资料和岗位集合，保留原有自身保护及停用会话撤销。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑用户资料、状态和岗位", description = "姓名、启用状态及 version 必填。positionIds 省略/null 保持，空数组清空，非空全量替换并刷新快照。不能停用自己；停用会撤销刷新会话。")
    @Transactional
    public ManagedUserDetailDto update(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ManageUserRequest request
    ) {
        var user = find(id);
        checkVersion(user, request.version());
        if (id.toString().equals(jwt.getSubject()) && !request.enabled()) {
            throw new BusinessException("CANNOT_DISABLE_SELF", "不能停用当前登录账户");
        }
        var selected = request.positionIds() == null ? null : resolvePositions(request.positionIds());
        user.rename(request.displayName());
        user.updateContact(request.phone() == null ? user.getPhone() : request.phone(),
                request.email() == null ? user.getEmail() : request.email());
        user.replacePositions(selected);
        if (user.isEnabled() && !request.enabled()) {
            tokens.revokeAll(id);
        }
        user.setEnabled(request.enabled());
        users.flush();
        return dto(user);
    }

    /** 按岗位 ID 稳定顺序取得根读锁并刷新，名称只能从服务端记录生成。 */
    private List<UserPositionSelection> resolvePositions(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException("DUPLICATE_USER_POSITION", "用户岗位不能重复");
        }
        List<UserPositionSelection> selected = new ArrayList<>();
        for (String id : ids.stream().sorted().toList()) {
            var position = positions.lockByIdForUser(id).orElseThrow(() ->
                    new BusinessException("USER_POSITION_NOT_FOUND", "所选岗位不存在，请刷新后重试"));
            selected.add(new UserPositionSelection(
                    position.getId(), position.getCode(), position.getName(),
                    position.getSystemCode(), position.getSystemName()
            ));
        }
        return selected;
    }

    /** 管理用途始终加载完整聚合；认证使用独立的轻量仓储读取。 */
    private User find(UUID id) {
        return users.findAggregateById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    /** 显式旧版本检查与数据库乐观锁共同保护整个用户聚合。 */
    private void checkVersion(User user, long version) {
        if (!Objects.equals(user.getVersion(), version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "资料已被修改，请刷新后重试");
        }
    }

    /** BCrypt 的 UTF-8 字节长度限制，不改变已有登录输入规则。 */
    private void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("PASSWORD_TOO_LONG", "密码 UTF-8 编码最多 72 字节");
        }
    }

    /** 在事务内转换为只读详情，不返回密码摘要、凭据版本或内部岗位修订号。 */
    private ManagedUserDetailDto dto(User user) {
        var items = user.getPositions().stream().map(item -> new UserPositionDto(
                item.getId(), item.getUserId(), item.getPositionId(),
                item.getPositionCode(), item.getPositionName(), item.getSystemCode(), item.getSystemName()
        )).toList();
        return new ManagedUserDetailDto(
                user.getId(), user.getUsername(), user.getDisplayName(), user.isEnabled(),
                user.getCreatedAt(), user.getVersion(), user.getPhone(), user.getEmail(), items
        );
    }
}
