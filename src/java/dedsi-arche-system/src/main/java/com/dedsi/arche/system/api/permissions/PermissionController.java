package com.dedsi.arche.system.api.permissions;

import com.dedsi.arche.system.domain.permissions.*;
import com.dedsi.arche.system.domain.menus.IMenuRepository;
import jakarta.persistence.EntityNotFoundException;
import com.dedsi.arche.system.domain.permissions.query.*;
import com.dedsi.arche.system.api.permissions.request.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.persistence.LockModeType;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 权限管理接口；所有操作均校验当前用户的管理员身份。
 */
@RestController
@RequestMapping("/api/permissions")
@Tag(name = "权限管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class PermissionController {
    private final IPermissionRepository permissions;
    /* 权限删除前检查菜单引用。 */
    private final IMenuRepository menus;
    private final IPermissionQuery query;
    private final com.dedsi.arche.system.domain.systems.IManagedSystemRepository systems;
    private final jakarta.persistence.EntityManager em;
    /* 在现有写事务中同步发布纯领域事件。 */
    private final ApplicationEventPublisher events;

    /**
     * 初始化所需依赖。
     */
    public PermissionController(
            IPermissionRepository permissions,
            IPermissionQuery query,
            com.dedsi.arche.system.domain.systems.IManagedSystemRepository systems,
            jakarta.persistence.EntityManager em,
            ApplicationEventPublisher events,
            IMenuRepository menus
    ) {
        this.permissions = permissions;
        this.menus = menus;
        this.query = query;
        this.systems = systems;
        this.em = em;
        this.events = events;
    }

    /**
     * 创建权限定义，生成 ULID 并初始化审计时间。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建权限", description = "系统编码与权限编码联合唯一，创建后不可修改。默认启用，排序值默认 0。")
    @Transactional
    public PermissionDto create(@Valid @RequestBody CreatePermissionRequest request) {
        var system = lockSystem(request.systemCode().trim());
        if (permissions.existsBySystemCodeAndCode(request.systemCode().trim(), request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该系统下权限编码已存在");
        }
        var permission = permissions.save(Permission.create(request.systemCode(),
                system.getName(),
                request.code(),
                request.name(), request.description(), request.enabled() == null || request.enabled(),
                request.sortOrder() == null ? 0 : request.sortOrder()));
        permissions.flush();
        return PermissionDto.from(permission);
    }

    /**
     * 读取权限详情，不存在时返回 404。
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询权限详情")
    @Transactional(readOnly = true)
    public PermissionDto detail(@PathVariable String id) {
        return PermissionDto.from(find(id));
    }

    /**
     * 查询权限分页，补齐默认搜索条件。
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询权限", description = "按权限编码、权限名称、系统名称搜索，按系统编码和启用状态筛选；页码从 1 开始。")
    public PermissionPage page(@Valid @RequestBody PermissionPageRequest request) {
        return query.search(request.keyword() == null ? "" : request.keyword().trim(),
                request.systemCode() == null ? null : request.systemCode().trim(),
                        request.enabled(),
                        request.page(),
                        request.size());
    }

    /**
     * 更新允许编辑的字段，保留系统编码和权限编码。
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新权限", description = "提交全部可编辑字段及 version；description 为 null 或空串时清除。权限名称实际变化时，同事务同步岗位引用名称并推进岗位版本；enabled 用于启停。")
    @Transactional
    public PermissionDto update(@PathVariable String id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        var permission = find(id);
        var system = lockSystem(permission.getSystemCode());
        // 固定顺序为系统锁、权限写锁、岗位写入；刷新避免等待锁后仍使用旧缓存。
        em.refresh(permission, LockModeType.PESSIMISTIC_WRITE);
        checkVersion(permission, request.version());
        permission.update(system.getName(),
                request.name(),
                request.description(),
                request.enabled(),
                request.sortOrder()).ifPresent(events::publishEvent);
        permissions.flush();
        return PermissionDto.from(permission);
    }

    /**
     * 删除权限；接口入口同时要求匹配当前版本。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除权限", description = "必须提交当前 version。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        var permission = find(id);
        // 历史权限可能未登记系统，此路径仍允许删除；已登记系统必须先锁系统再锁权限。
        systems.lockByCode(permission.getSystemCode());
        try {
            em.refresh(permission, LockModeType.PESSIMISTIC_WRITE);
        } catch (EntityNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "权限已变化，请重新读取后提交");
        }
        checkVersion(permission, version);
        if (menus.existsByPermissionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "权限已被菜单引用，请先解除菜单权限关联");
        }
        permissions.delete(permission);
        permissions.flush();
    }

    /** 系统名称只由服务端读取；系统行锁协调权限写入与系统更名/删除。 */
    private com.dedsi.arche.system.domain.systems.ManagedSystem lockSystem(String code) {
        return systems.lockByCode(code).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属系统不存在，请先登记系统"));
    }

    /**
     * 校验 ULID 格式并加载权限，区分格式错误与记录不存在。
     */
    private Permission find(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "权限 ID 必须是大写 ULID");
        }
        return permissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "权限不存在"));
    }

    /**
     * 拒绝过期版本，避免覆盖其他请求已保存的修改。
     */
    private void checkVersion(Permission permission, long version) {
        if (permission.getVersion() != version) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "权限已被修改，请刷新后重试");
        }
    }
}
