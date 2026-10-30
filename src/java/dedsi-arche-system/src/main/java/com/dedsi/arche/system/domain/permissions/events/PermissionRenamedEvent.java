package com.dedsi.arche.system.domain.permissions.events;

/** 权限名称实际变更的不可变领域事实，不依赖 Spring 或持久化框架。 */
public record PermissionRenamedEvent(
        /* 已更名权限的 ULID。 */
        String permissionId,
        /* 变更前的规范化名称。 */
        String previousName,
        /* 变更后的规范化名称。 */
        String name
) {
}
