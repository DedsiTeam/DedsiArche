package com.dedsi.arche.system.domain.dictionaries.query;

import java.time.LocalDateTime;

/** 字典分页摘要，不加载子项。 */
public record DictionaryListItemDto(
        /* 字典 ULID。 */
        String id,
        /* 所属系统内唯一且不可变的字典编码。 */
        String code,
        /* 字典名称。 */
        String name,
        /* 不可变的所属系统编码。 */
        String systemCode,
        /* 所属系统当前名称，同事务事件同步。 */
        String systemName,
        /* 字典说明，可空。 */
        String description,
        /* 是否启用。 */
        boolean enabled,
        /* 创建时间，北京时间。 */
        LocalDateTime createdAt,
        /* 最近修改时间，北京时间。 */
        LocalDateTime updatedAt,
        /* 根聚合乐观锁版本。 */
        Long version
) {
}
