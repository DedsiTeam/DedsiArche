package com.dedsi.arche.system.domain.menus;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** 菜单节点独立聚合；树结构和关联权限规则由应用在系统锁内协调。 */
@Entity
@Table(name = "sys_menus", uniqueConstraints = @UniqueConstraint(
        name = "uk_menus_system_code", columnNames = {"system_code", "code"}))
public class Menu {
    /* 节点标识，26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属系统编码，创建后不可修改。 */
    @Column(name = "system_code", length = 100, nullable = false, updatable = false)
    private String systemCode;

    /* 系统当前名称，同事务领域事件同步。 */
    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;

    /* 直属上级菜单标识，null 表示顶级；不级联维护子树。 */
    @Column(name = "parent_id", length = 26)
    private String parentId;

    /* 系统内唯一且不可修改的菜单编码。 */
    @Column(length = 100, nullable = false, updatable = false)
    private String code;

    /* 菜单显示名称。 */
    @Column(length = 100, nullable = false)
    private String name;

    /* 目录、内部页面或外链。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", length = 20, nullable = false)
    private MenuType menuType;

    /* 可选关联权限标识，名称及编码由服务端读取。 */
    @Column(name = "permission_id", length = 26)
    private String permissionId;

    /* 关联权限编码，与权限标识及名称成组为空或非空。 */
    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    /* 关联权限当前名称，通过同步事件维护。 */
    @Column(name = "permission_name", length = 100)
    private String permissionName;

    /* 内页站内路径最多 500 字符，外链 HTTP(S) URL 最多 2048 字符；目录为空。 */
    @Column(name = "route_path", length = 2048)
    private String routePath;

    /* 内部页面的逻辑组件路径；目录和外链为空。 */
    @Column(name = "component_path", length = 255)
    private String componentPath;

    /* 非负同级排序，越小越靠前。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 可选菜单说明，最多 500 字符。 */
    @Column(length = 500)
    private String description;

    /* 导航显示配置，不代表后端访问授权。 */
    @Column(nullable = false)
    private boolean visible;

    /* 节点启用配置，不隐式级联修改子节点。 */
    @Column(nullable = false)
    private boolean enabled;

    /* 页面缓存配置，仅内部页面支持。 */
    @Column(nullable = false)
    private boolean cache;

    /* 固定标签配置，仅内部页面支持。 */
    @Column(nullable = false)
    private boolean affix;

    /* 创建时间，北京时间。 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 修改时间，北京时间；实际变化才推进。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 节点乐观锁版本，覆盖关联快照与全部配置。 */
    @Version
    @Column(nullable = false)
    private Long version;


    /** 供 JPA 使用。 */
    protected Menu() {
    }

    /** 创建节点，系统及权限资料必须由服务端目录读取。 */
    public static Menu create(
            String systemCode, String systemName, String parentId, String code, String name, MenuType menuType,
            String permissionId, String permissionCode, String permissionName, String routePath,
            String componentPath, int sortOrder, String description,
            boolean visible, boolean enabled, boolean cache, boolean affix
    ) {
        var menu = new Menu();
        menu.systemCode = required(systemCode, "系统编码", 100);
        menu.systemName = required(systemName, "系统名称", 100);
        menu.code = required(code, "菜单编码", 100);
        menu.id = UlidCreator.getMonotonicUlid().toString();
        menu.createdAt = now();
        menu.updatedAt = menu.createdAt;
        menu.update(parentId, name, menuType, permissionId, permissionCode, permissionName,
                routePath, componentPath, sortOrder, description, visible, enabled, cache, affix);
        menu.updatedAt = menu.createdAt;
        return menu;
    }

    /**
     * 完整更新节点，null 上级明确移为顶级，null 权限三字段明确解除关联。
     * 所有本地校验完成后才变更；父链、类型转换及启停规则须先在系统锁内验证。
     */
    public void update(
            String parentId, String name, MenuType menuType,
            String permissionId, String permissionCode, String permissionName, String routePath,
            String componentPath, int sortOrder, String description,
            boolean visible, boolean enabled, boolean cache, boolean affix
    ) {
        validateId(parentId, "上级菜单标识", true);
        if (Objects.equals(parentId, id) && parentId != null) {
            throw invalid("上级菜单不能是当前菜单自身");
        }
        String nextName = required(name, "菜单名称", 100);
        if (menuType == null) {
            throw invalid("菜单类型不能为空");
        }
        String nextPermissionCode = null;
        String nextPermissionName = null;
        if (permissionId != null || permissionCode != null || permissionName != null) {
            validateId(permissionId, "关联权限标识", false);
            nextPermissionCode = required(permissionCode, "权限编码", 100);
            nextPermissionName = required(permissionName, "权限名称", 100);
        }
        String nextRoutePath = optional(routePath, "路由路径", 2048);
        String nextComponentPath = optional(componentPath, "组件路径", 255);
        String nextDescription = optional(description, "菜单说明", 500);
        if (sortOrder < 0) {
            throw invalid("同级排序必须是非负整数");
        }
        validateType(menuType, nextRoutePath, nextComponentPath, cache, affix);
        boolean changed = !Objects.equals(this.parentId, parentId)
                || !Objects.equals(this.name, nextName) || this.menuType != menuType
                || !Objects.equals(this.permissionId, permissionId)
                || !Objects.equals(this.permissionCode, nextPermissionCode)
                || !Objects.equals(this.permissionName, nextPermissionName)
                || !Objects.equals(this.routePath, nextRoutePath)
                || !Objects.equals(this.componentPath, nextComponentPath)
                || !Objects.equals(this.description, nextDescription)
                || this.sortOrder != sortOrder || this.visible != visible || this.enabled != enabled
                || this.cache != cache || this.affix != affix;
        this.parentId = parentId;
        this.name = nextName;
        this.menuType = menuType;
        this.permissionId = permissionId;
        this.permissionCode = nextPermissionCode;
        this.permissionName = nextPermissionName;
        this.routePath = nextRoutePath;
        this.componentPath = nextComponentPath;
        this.sortOrder = sortOrder;
        this.description = nextDescription;
        this.visible = visible;
        this.enabled = enabled;
        this.cache = cache;
        this.affix = affix;
        if (changed) {
            touch();
        }
    }

    /** 同步所属系统当前名称；重复或不相关事件保持节点不变。 */
    public void synchronizeSystemName(String systemCode, String name) {
        String nextCode = required(systemCode, "系统编码", 100);
        String nextName = required(name, "系统名称", 100);
        if (this.systemCode.equals(nextCode) && !systemName.equals(nextName)) {
            systemName = nextName;
            touch();
        }
    }

    /** 同步关联权限当前名称；不修改关联及编码。 */
    public void synchronizePermissionName(String permissionId, String name) {
        validateId(permissionId, "关联权限标识", false);
        String nextName = required(name, "权限名称", 100);
        if (permissionId.equals(this.permissionId) && !nextName.equals(permissionName)) {
            permissionName = nextName;
            touch();
        }
    }

    /** 限制类型的合法配置，防止同时表示内页与外链。 */
    private static void validateType(MenuType type, String route, String component, boolean cache, boolean affix) {
        if (type == MenuType.DIRECTORY) {
            if (route != null || component != null || cache || affix) {
                throw invalid("目录不能配置路由、组件、缓存或固定标签");
            }
            return;
        }
        if (type == MenuType.EXTERNAL) {
            if (component != null || cache || affix) {
                throw invalid("外链不能配置组件、缓存或固定标签");
            }
            validateExternal(route);
            return;
        }
        validateInternal(route);
        if (component == null || !component.matches("[A-Za-z0-9_./-]+")
                || component.startsWith("/") || component.contains("//") || component.endsWith("/")) {
            throw invalid("内部页面必须填写相对的逻辑组件路径");
        }
        validateSegments(component, "组件路径不能包含当前目录或上级目录跳转");
    }

    /** 内页为站内绝对路径，不包含协议、查询参数、片段或目录跳转。 */
    private static void validateInternal(String route) {
        if (route != null && route.length() > 500) {
            throw invalid("内部页面路由最多 500 字符");
        }
        if (route == null || !route.startsWith("/") || route.startsWith("//")) {
            throw invalid("内部页面路由必须是以单个斜杠开头的站内路径");
        }
        try {
            var uri = new URI(route);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || uri.getPath() == null
                    || uri.getPath().startsWith("//") || uri.getPath().contains("\\")) {
                throw invalid("内部页面路由不能包含协议、查询参数或片段");
            }
            validateSegments(uri.getPath(), "内部页面路由不能包含目录跳转");
        } catch (URISyntaxException exception) {
            throw invalid("内部页面路由格式无效");
        }
    }

    /** 外链仅允许 HTTP(S)，拒绝内嵌账号凭据及无效主机、端口。 */
    private static void validateExternal(String route) {
        if (route == null) {
            throw invalid("外链地址不能为空");
        }
        try {
            var uri = new URI(route);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getPort() < -1 || uri.getPort() > 65535) {
                throw invalid("外链必须是无账号密码的完整 HTTP(S) 地址");
            }
        } catch (URISyntaxException exception) {
            throw invalid("外链地址格式无效");
        }
    }

    /** 阻止逻辑路径穿越；内部路由在 URI 解码后检查。 */
    private static void validateSegments(String path, String message) {
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                throw invalid(message);
            }
        }
    }

    /** 标识不得接受空白或非标准 ULID。 */
    private static void validateId(String value, String label, boolean nullable) {
        if (value == null && nullable) {
            return;
        }
        if (value == null || !value.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid(label + "必须是有效的大写 ULID");
        }
    }

    /** 仅实际变化推进根的微秒级修改时间，触发 JPA 版本更新。 */
    private void touch() {
        LocalDateTime current = now();
        updatedAt = updatedAt != null && !current.isAfter(updatedAt) ? updatedAt.plusNanos(1000) : current;
    }

    /** 获取北京时间并对齐数据库时间精度。 */
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
    }

    /** 必填文本保留大小写并去除首尾空格。 */
    private static String required(String value, String label, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum || value.trim().isEmpty()) {
            throw invalid(label + "不能为空且最多 " + maximum + " 字符");
        }
        return value.trim();
    }

    /** 可选文本空白转为 null。 */
    private static String optional(String value, String label, int maximum) {
        if (value != null && value.length() > maximum) {
            throw invalid(label + "最多 " + maximum + " 字符");
        }
        return value == null || value.isBlank() || value.trim().isEmpty() ? null : value.trim();
    }

    /** 菜单节点输入错误码。 */
    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_MENU", message);
    }

    public String getId() {
        return id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public String getRoutePath() {
        return routePath;
    }

    public String getComponentPath() {
        return componentPath;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCache() {
        return cache;
    }

    public boolean isAffix() {
        return affix;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
