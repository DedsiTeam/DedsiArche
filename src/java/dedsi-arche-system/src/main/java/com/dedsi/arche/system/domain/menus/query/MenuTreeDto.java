package com.dedsi.arche.system.domain.menus.query;

import com.dedsi.arche.system.domain.menus.Menu;
import com.dedsi.arche.system.domain.menus.MenuType;
import java.time.LocalDateTime;
import java.util.List;

/** 菜单树只读响应。 */
public record MenuTreeDto(
        /* 菜单 ULID。 */
        String id,
        /* 不可变所属系统编码。 */
        String systemCode,
        /* 同步维护系统名称。 */
        String systemName,
        /* 上级目录标识，null 顶级。 */
        String parentId,
        /* 系统内唯一编码。 */
        String code,
        /* 菜单显示名称。 */
        String name,
        /* 菜单类型。 */
        MenuType menuType,
        /* 关联权限标识，可空。 */
        String permissionId,
        /* 服务端生成权限编码，可空。 */
        String permissionCode,
        /* 同步维护权限名称，可空。 */
        String permissionName,
        /* 页面路径或外链 URL，目录为空。 */
        String routePath,
        /* 页面逻辑组件标识，其他类型为空。 */
        String componentPath,
        /* 非负同层排序。 */
        int sortOrder,
        /* 可空说明。 */
        String description,
        /* 是否显示。 */
        boolean visible,
        /* 是否启用。 */
        boolean enabled,
        /* 页面是否缓存。 */
        boolean cache,
        /* 页面是否固定标签。 */
        boolean affix,
        /* 北京时间创建时间。 */
        LocalDateTime createdAt,
        /* 北京时间更新时间。 */
        LocalDateTime updatedAt,
        /* 乐观并发版本。 */
        Long version,
        /* 全部直属子菜单。 */
        List<MenuTreeDto> children
) {
    /** 由节点构造只读响应。 */
    public static MenuTreeDto from(Menu value, List<MenuTreeDto> children) {
        return new MenuTreeDto(
                value.getId(), value.getSystemCode(), value.getSystemName(), value.getParentId(),
                value.getCode(), value.getName(), value.getMenuType(), value.getPermissionId(),
                value.getPermissionCode(), value.getPermissionName(), value.getRoutePath(), value.getComponentPath(),
                value.getSortOrder(), value.getDescription(), value.isVisible(), value.isEnabled(),
                value.isCache(), value.isAffix(), value.getCreatedAt(), value.getUpdatedAt(),
                value.getVersion(), children
        );
    }
}
