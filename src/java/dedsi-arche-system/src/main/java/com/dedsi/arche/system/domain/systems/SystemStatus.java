package com.dedsi.arche.system.domain.systems;

/** 人工维护的业务状态，不代表实时健康或用户访问权限。 */
public enum SystemStatus {
    /* 已登记，尚未上线。 */
    NOT_LAUNCHED,
    /* 正常提供服务，必须已配置有效入口。 */
    NORMAL,
    /* 管理员停用。 */
    DISABLED,
    /* 发布或升级中。 */
    UPDATING,
    /* 临时维护。 */
    MAINTENANCE,
    /* 已退役，保留资料。 */
    RETIRED
}
