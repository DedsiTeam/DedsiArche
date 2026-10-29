package com.dedsi.arche.system.api.users;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.domain.users.*;
import com.dedsi.arche.system.domain.users.query.*;
import com.dedsi.arche.system.api.users.request.*;
import com.dedsi.arche.system.infrastructure.users.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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

@RestController
@RequestMapping("/api/users")
@Tag(name="用户管理")
@SecurityRequirement(name="bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class UserManagementController {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;
    private final UserRepository users;
    private final UserQuery query;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final com.dedsi.arche.system.infrastructure.users.security.UserAdministration administration;
    public UserManagementController(UserRepository users, UserQuery query, PasswordEncoder passwords, TokenService tokens, com.dedsi.arche.system.infrastructure.users.security.UserAdministration administration) {
        this.users=users; this.query=query; this.passwords=passwords; this.tokens=tokens;
        this.administration=administration;
    }
    @GetMapping("/{id}")
    @Operation(summary="查询用户详情")
    @Transactional(readOnly=true)
    public ManagedUserDto detail(@PathVariable UUID id) { return dto(find(id)); }

    @PostMapping("/{id}/reset-password")
    @Operation(summary="重置用户密码", description="立即使该用户旧访问令牌和刷新令牌失效。")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        var user=find(id);
        checkVersion(user,request.version());
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72) throw new BusinessException("PASSWORD_TOO_LONG","密码 UTF-8 编码最多 72 字节");
        user.resetPassword(passwords.encode(request.password()));
        em.flush();
        tokens.revokeAll(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary="删除用户", description="删除用户及其刷新会话。当前账户和配置中的管理员不可删除。")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id, @RequestParam @Min(0) long version, @AuthenticationPrincipal Jwt jwt) {
        var user=find(id);
        if(id.toString().equals(jwt.getSubject()) || administration.isAdministrator(user.getUsername())) throw new BusinessException("PROTECTED_USER","不能删除当前账户或配置中的管理员");
        checkVersion(user,version);
        tokens.deleteSessions(id);
        users.delete(user);
        em.flush();
    }
    private User find(UUID id) { return users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"用户不存在")); }
    private void checkVersion(User user, long version) {
        if(user.getVersion()!=version) throw new ResponseStatusException(HttpStatus.CONFLICT,"资料已被修改，请刷新后重试");
    }
    @PostMapping("/page")
    @Operation(summary="分页查询用户", description="管理员可按用户名或显示名称搜索、按状态筛选。页码从 1 开始。")
    @Transactional(readOnly=true)
    public UserPage list(@Valid @RequestBody UserPageRequest request) {
        return query.search(request.keyword().trim(), request.enabled(), request.page(), request.size());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="新增用户", description="新增账户默认启用，不授予管理权限。")
    @Transactional
    public ManagedUserDto create(@Valid @RequestBody RegisterUserRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length>72) throw new BusinessException("PASSWORD_TOO_LONG","密码 UTF-8 编码最多 72 字节");
        if(users.findByUsername(User.normalizeUsername(request.username())).isPresent()) throw new BusinessException("USERNAME_EXISTS","用户名已存在");
        var user=users.save(User.register(request.username(),request.displayName(),passwords.encode(request.password())));
        return dto(user);
    }
    @PatchMapping("/{id}")
    @Operation(summary="编辑用户名称和启用状态", description="提交列表返回的 version 防止覆盖他人的更新；不能停用自己。停用会撤销刷新会话。")
    @Transactional
    public ManagedUserDto update(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ManageUserRequest request) {
        var user=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"用户不存在"));
        if(!Objects.equals(user.getVersion(),request.version())) throw new ResponseStatusException(HttpStatus.CONFLICT,"资料已被修改，请刷新后重试");
        if(id.toString().equals(jwt.getSubject()) && !request.enabled()) throw new BusinessException("CANNOT_DISABLE_SELF","不能停用当前登录账户");
        user.rename(request.displayName());
        if (user.isEnabled() && !request.enabled()) tokens.revokeAll(id);
        user.setEnabled(request.enabled());
        em.flush();
        return dto(user);
    }
    private ManagedUserDto dto(User u) {
        return new ManagedUserDto(u.getId(),u.getUsername(),u.getDisplayName(),u.isEnabled(),u.getCreatedAt(),u.getVersion());
    }
}
