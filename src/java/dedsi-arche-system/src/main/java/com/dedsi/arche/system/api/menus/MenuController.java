package com.dedsi.arche.system.api.menus;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.api.menus.request.*;
import com.dedsi.arche.system.domain.menus.Menu;
import com.dedsi.arche.system.domain.menus.MenuType;
import com.dedsi.arche.system.domain.permissions.Permission;
import com.dedsi.arche.system.domain.permissions.IPermissionRepository;
import com.dedsi.arche.system.domain.menus.IMenuRepository;
import com.dedsi.arche.system.domain.menus.query.*;
import com.dedsi.arche.system.domain.systems.IManagedSystemRepository;
import com.dedsi.arche.system.domain.systems.ManagedSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 菜单管理编排，系统锁串行化同系统的结构与启停变更。 */
@RestController
@RequestMapping("/api/menus")
@Tag(name = "菜单管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class MenuController {
    /* 独立菜单节点聚合仓储。 */
    private final IMenuRepository menus;
    /* 服务端锁定权限并取得最新名称快照。 */
    private final IPermissionRepository permissions;
    /* 树及分页只读查询。 */
    private final IMenuQuery query;
    /* 所属系统与树结构写入协调锁。 */
    private final IManagedSystemRepository systems;
    /* 等待系统锁后刷新目标，保证版本预检基于最新状态。 */
    private final EntityManager em;

    /** 注入事务编排依赖。 */
    public MenuController(
            IMenuRepository menus, IMenuQuery query, IPermissionRepository permissions,
            IManagedSystemRepository systems, EntityManager em
    ) {
        this.menus = menus;
        this.permissions = permissions;
        this.query = query;
        this.systems = systems;
        this.em = em;
    }

    /** 创建独立节点，上级为空时为顶级。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建菜单", description = "所属系统与编码不可修改；上级必须是同系统目录，启用节点的上级必须启用；权限可空但必须同系统。")
    @Transactional
    public MenuDto create(@Valid @RequestBody CreateMenuRequest request) {
        var system = lockSystem(request.systemCode().trim());
        if (menus.existsBySystemCodeAndCode(system.getCode(), request.code().trim())) {
            throw conflict("该系统下菜单编码已存在");
        }
        boolean enabled = request.enabled() == null || request.enabled();
        var permission = resolvePermission(request.permissionId(), system.getCode());
        validateParent(null, system.getCode(), request.parentId(), enabled);
        checkRoute(system.getCode(), request.menuType(), request.routePath(), null);
        var menu = menus.save(Menu.create(
                system.getCode(), system.getName(), request.parentId(), request.code(), request.name(), request.menuType(),
                permission == null ? null : permission.getId(), permission == null ? null : permission.getCode(),
                permission == null ? null : permission.getName(), request.routePath(), request.componentPath(),
                request.sortOrder() == null ? 0 : request.sortOrder(), request.description(),
                request.visible() == null || request.visible(), enabled,
                request.cache() == null ? request.menuType() == MenuType.PAGE : request.cache(),
                request.affix() != null && request.affix()
        ));
        menus.flush();
        return MenuDto.from(menu);
    }

    /** 读取单一菜单节点。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询菜单详情")
    @Transactional(readOnly = true)
    public MenuDto detail(@PathVariable String id) {
        return MenuDto.from(find(id));
    }

    /** 完整树包含停用节点，每层按排序值、编码及标识排序。 */
    @GetMapping("/tree/{systemCode}")
    @Operation(summary = "查询菜单完整树", description = "返回根节点数组，每个节点附 children；包含停用节点，无节点时为空数组。")
    public List<MenuTreeDto> tree(@PathVariable @NotBlank @Size(max = 100) String systemCode) {
        return query.tree(systemCode.trim());
    }

    /** 分页结果为独立节点，不因筛选丢失上级而构造残缺树。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询菜单")
    public MenuPage page(@Valid @RequestBody MenuPageRequest request) {
        return query.search(request.keyword(), request.systemCode(), request.enabled(), request.menuType(), request.page(), request.size());
    }

    /** 父标识必须明确提交；移动节点不会修改后代的父关系。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑菜单", description = "version、parentId、permissionId 字段必填，parentId=null 顶级、permissionId=null 解除权限；其他字段 null 保持，路径、组件和说明空白清除。只有目录可作上级，禁止跨系统或成环。")
    @Transactional
    public MenuDto update(@PathVariable String id, @Valid @RequestBody UpdateMenuRequest request) {
        var menu = find(id);
        lockSystem(menu.getSystemCode());
        var permission = resolvePermission(request.permissionId(), menu.getSystemCode());
        refresh(menu);
        checkVersion(menu, request.version());
        boolean enabled = request.enabled() == null ? menu.isEnabled() : request.enabled();
        validateParent(id, menu.getSystemCode(), request.parentId(), enabled);
        if (!enabled && menus.existsByParentIdAndEnabledTrue(id)) {
            throw conflict("存在启用的下级菜单，不能停用");
        }
        var menuType = request.menuType() == null ? menu.getMenuType() : request.menuType();
        if (menuType != MenuType.DIRECTORY && menus.existsByParentId(id)) {
            throw conflict("存在下级菜单，不能改为非目录类型");
        }
        var routePath = keep(request.routePath(), menu.getRoutePath());
        checkRoute(menu.getSystemCode(), menuType, routePath, id);
        menu.update(
                request.parentId(), keep(request.name(), menu.getName()), menuType,
                permission == null ? null : permission.getId(), permission == null ? null : permission.getCode(),
                permission == null ? null : permission.getName(), routePath,
                keep(request.componentPath(), menu.getComponentPath()),
                request.sortOrder() == null ? menu.getSortOrder() : request.sortOrder(),
                keep(request.description(), menu.getDescription()),
                request.visible() == null ? menu.isVisible() : request.visible(), enabled,
                request.cache() == null ? menu.isCache() : request.cache(),
                request.affix() == null ? menu.isAffix() : request.affix()
        );
        menus.flush();
        return MenuDto.from(menu);
    }

    /** 仅允许删除叶子；停用子节点同样阻止删除。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除叶子菜单", description = "version 必填，有任何直属子节点时返回 409。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        var menu = find(id);
        lockSystem(menu.getSystemCode());
        refresh(menu);
        checkVersion(menu, version);
        if (menus.existsByParentId(id)) {
            throw conflict("菜单下存在子节点，不能删除");
        }
        menus.delete(menu);
        menus.flush();
    }

    /** 所有树结构校验在系统锁内完成，避免并发互相移动导致环。 */
    private void validateParent(String id, String systemCode, String parentId, boolean enabled) {
        if (parentId == null) {
            return;
        }
        validateId(parentId);
        var visited = new HashSet<String>();
        String cursor = parentId;
        boolean directParent = true;
        while (cursor != null) {
            if (Objects.equals(cursor, id) || !visited.add(cursor)) {
                throw invalid("上级菜单不能是自身或后代");
            }
            var parent = menus.findById(cursor).orElseThrow(() -> invalid("上级菜单不存在"));
            if (!systemCode.equals(parent.getSystemCode())) {
                throw invalid("上级菜单必须属于同一系统");
            }
            if (parent.getMenuType() != MenuType.DIRECTORY) {
                throw invalid("上级菜单必须是目录");
            }
            if (directParent && enabled && !parent.isEnabled()) {
                throw invalid("启用菜单的上级必须启用");
            }
            directParent = false;
            cursor = parent.getParentId();
        }
    }

    /** 所有绑定均先系统锁后权限读锁，服务端快照不信任客户端字段。 */
    private Permission resolvePermission(String permissionId, String systemCode) {
        if (permissionId == null) {
            return null;
        }
        if (!permissionId.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid("权限 ID 必须是大写 ULID");
        }
        var permission = permissions.lockByIdForMenu(permissionId)
                .orElseThrow(() -> invalid("所选权限不存在"));
        if (!systemCode.equals(permission.getSystemCode())) {
            throw invalid("菜单权限必须属于同一系统");
        }
        return permission;
    }

    /** 系统锁内预检页面路径，数据库部分唯一索引继续兜底。 */
    private void checkRoute(String systemCode, MenuType menuType, String routePath, String id) {
        if (menuType == MenuType.PAGE && routePath != null
                && menus.existsPageRoute(systemCode, routePath.trim(), id)) {
            throw conflict("该系统下页面路径已存在");
        }
    }

    /** 所属系统锁与系统改名、删除使用相同锁顺序。 */
    private ManagedSystem lockSystem(String code) {
        return systems.lockByCode(code).orElseThrow(() ->
                new BusinessException("MENU_SYSTEM_NOT_FOUND", "所属系统不存在，请先登记系统"));
    }

    /** 等待系统锁时被删除的节点视为并发冲突。 */
    private void refresh(Menu menu) {
        try {
            em.refresh(menu);
        } catch (EntityNotFoundException exception) {
            throw conflict("菜单已被修改，请刷新后重试");
        }
    }

    /** 格式错误与不存在分别返回 400、404。 */
    private Menu find(String id) {
        validateId(id);
        return menus.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "菜单不存在"));
    }

    private void validateId(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid("菜单 ID 必须是大写 ULID");
        }
    }

    /** 无变化请求也必须先通过最新版本检查。 */
    private void checkVersion(Menu menu, long version) {
        if (!Objects.equals(menu.getVersion(), version)) {
            throw conflict("菜单已被修改，请刷新后重试");
        }
    }

    private String keep(String incoming, String current) {
        return incoming == null ? current : incoming;
    }

    private BusinessException invalid(String message) {
        return new BusinessException("INVALID_MENU", message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
