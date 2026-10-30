package com.dedsi.arche.system.domain.systems.events;

/** 系统名称实际变更的不可变领域事实。 */
public record SystemRenamedEvent(
        /* 系统编码。 */
        String systemCode,
        /* 原名称。 */
        String previousName,
        /* 新名称。 */
        String name
) {
}
