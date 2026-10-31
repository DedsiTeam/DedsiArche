package com.dedsi.arche.system.domain.menus;

/** 菜单节点类型；只有目录可承载下级。 */
public enum MenuType {
    /* 仅作层级分组，不绑定路由或组件。 */
    DIRECTORY,
    /* 站内页面，包含路由与逻辑组件标识。 */
    PAGE,
    /* HTTP 或 HTTPS 外部链接。 */
    EXTERNAL
}
