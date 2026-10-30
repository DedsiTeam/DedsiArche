package com.dedsi.arche.system.api.positions;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.api.positions.request.*;
import com.dedsi.arche.system.domain.permissions.IPermissionRepository;
import com.dedsi.arche.system.domain.permissions.Permission;
import com.dedsi.arche.system.domain.positions.*;
import com.dedsi.arche.system.domain.systems.IManagedSystemRepository;
import com.dedsi.arche.system.domain.systems.ManagedSystem;
import com.dedsi.arche.system.domain.users.IUserRepository;
import com.dedsi.arche.system.domain.positions.query.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** 岗位管理；复用服务端管理员鉴权，不赋予用户新的访问权限。 */
@RestController
@RequestMapping("/api/positions")
@Tag(name = "岗位管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class PositionController {
    /* 聚合读写仓储。 */
    private final IPositionRepository positions;
    /* 分页只读查询。 */
    private final IPositionQuery query;
    /* 权限定义读取入口，快照不能由客户端伪造。 */
    private final IPermissionRepository permissions;
    /* 岗位所属系统名称只由目录读取，并通过系统锁协调改名与首次登记。 */
    private final IManagedSystemRepository systems;
    /* 岗位删除前检查用户岗位引用。 */
    private final IUserRepository users;

    /** 注入岗位与权限依赖。 */
    public PositionController(
            IPositionRepository positions, IPositionQuery query,
            IPermissionRepository permissions, IManagedSystemRepository systems, IUserRepository users
    ) {
        this.positions = positions;
        this.query = query;
        this.permissions = permissions;
        this.systems = systems;
        this.users = users;
    }

    /** 创建岗位和权限子实体，在事务内检查唯一约束。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建岗位", description = "systemCode 必填，systemName 由服务端读取；岗位编码全局唯一且不可修改；默认启用、排序 0、权限为空，允许跨系统权限。")
    @Transactional
    public PositionDto create(@Valid @RequestBody CreatePositionRequest request) {
        if (positions.existsByCode(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "岗位编码已存在");
        }
        var system = lockSystem(request.systemCode());
        var position = positions.save(Position.create(
                system.getCode(), system.getName(), request.code(), request.name(), request.description(),
                request.enabled() == null || request.enabled(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                resolvePermissions(request.permissionIds())
        ));
        positions.flush();
        return PositionDto.from(position);
    }

    /** 在只读事务内返回岗位及权限快照。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询岗位详情", description = "包含岗位权限子实体及其系统、权限编码和名称快照。")
    @Transactional(readOnly = true)
    public PositionDto detail(@PathVariable String id) {
        return PositionDto.from(find(id));
    }

    /** 分页返回岗位基本信息，权限集合由详情接口读取。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询岗位", description = "按编码或名称搜索及状态筛选；页码从 1 开始，默认 10 条，上限 100；不返回权限集合。")
    public PositionPage page(@Valid @RequestBody PositionPageRequest request) {
        return query.search(request.keyword(), request.enabled(), request.page(), request.size());
    }

    /** 按版本部分更新；权限列表全量替换且保留未移除的子实体 ID。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑岗位", description = "version 必填；其他字段省略或 null 保持。systemCode 允许历史空值首次绑定，已绑定不可改值或解绑。空白说明清除，空白名称拒绝。permissionIds 空数组清空，非空替换并刷新名称。")
    @Transactional
    public PositionDto update(
            @PathVariable String id, @Valid @RequestBody UpdatePositionRequest request
    ) {
        var position = find(id);
        checkVersion(position, request.version());
        ManagedSystem system = null;
        if (request.systemCode() != null) {
            String code = request.systemCode().trim();
            if (code.isEmpty()) {
                throw new BusinessException("INVALID_POSITION", "所属系统编码不能为空");
            }
            if (position.getSystemCode() != null && !position.getSystemCode().equals(code)) {
                throw new BusinessException("POSITION_SYSTEM_IMMUTABLE", "岗位所属系统登记后不可修改");
            }
            system = lockSystem(code);
        } else if (position.getSystemCode() != null) {
            // 已登记岗位即使省略系统字段，也先锁所属系统并读取当前名称，再获取权限锁。
            system = lockSystem(position.getSystemCode());
        }
        var selected = request.permissionIds() == null ? null : resolvePermissions(request.permissionIds());
        position.update(
                request.name() == null ? position.getName() : request.name(),
                request.description() == null ? position.getDescription() : request.description(),
                request.enabled() == null ? position.isEnabled() : request.enabled(),
                request.sortOrder() == null ? position.getSortOrder() : request.sortOrder(),
                selected,
                system == null ? null : system.getCode(),
                system == null ? null : system.getName()
        );
        positions.flush();
        return PositionDto.from(position);
    }

    /** 删除岗位及子实体，不删除权限定义。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除岗位", description = "必须提交当前 version；被用户引用时返回 POSITION_IN_USE/409。成功时删除岗位权限关联，保留权限定义。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        validateId(id);
        var position = positions.lockByIdForDeletion(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
        checkVersion(position, version);
        if (users.existsByPositionId(id)) {
            throw new BusinessException("POSITION_IN_USE", "岗位已被用户引用，请先解除用户岗位关联");
        }
        positions.delete(position);
        positions.flush();
    }

    /** 系统锁必须在权限读锁和岗位写入之前取得，名称不接受客户端提供。 */
    private ManagedSystem lockSystem(String code) {
        return systems.lockByCode(code.trim()).orElseThrow(() ->
                new BusinessException("POSITION_SYSTEM_NOT_FOUND", "所属系统不存在，请先登记系统"));
    }

    /** 读取真实权限定义；允许保存停用定义，不进行实际授权判定。 */
    private List<Permission> resolvePermissions(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException("DUPLICATE_POSITION_PERMISSION", "岗位权限不能重复");
        }
        List<Permission> selected = new ArrayList<>();
        // 排序后依次取权限读锁，再修改岗位；不得先锁岗位再等待权限，避免锁顺序反转。
        for (String id : ids.stream().sorted().toList()) {
            selected.add(permissions.lockByIdForPosition(id).orElseThrow(() ->
                    new BusinessException("POSITION_PERMISSION_NOT_FOUND", "所选权限不存在，请刷新后重试")));
        }
        return selected;
    }

    /** 区分非法标识与岗位不存在。 */
    private Position find(String id) {
        validateId(id);
        return positions.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
    }

    /** 普通读取和删除加锁共用标识校验。 */
    private void validateId(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "岗位 ID 必须是大写 ULID");
        }
    }

    /** 提前提示旧版本；数据库乐观锁继续兜底并发写入。 */
    private void checkVersion(Position position, long version) {
        if (!Objects.equals(position.getVersion(), version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "岗位已被修改，请刷新后重试");
        }
    }
}
