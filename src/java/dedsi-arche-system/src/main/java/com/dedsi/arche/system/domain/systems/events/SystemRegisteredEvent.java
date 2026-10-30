package com.dedsi.arche.system.domain.systems.events;

/** 系统首次登记的领域事实，用于同步已有同编码引用。 */
public record SystemRegisteredEvent(
        /* 系统编码。 */
        String systemCode,
        /* 系统名称。 */
        String name
) {
}
