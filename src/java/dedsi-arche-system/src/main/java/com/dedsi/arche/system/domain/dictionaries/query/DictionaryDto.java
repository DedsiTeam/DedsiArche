package com.dedsi.arche.system.domain.dictionaries.query;

import com.dedsi.arche.system.domain.dictionaries.Dictionary;
import com.dedsi.arche.system.domain.dictionaries.DictionaryItem;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** 完整字典响应，不直接序列化 JPA 实体。 */
public record DictionaryDto(
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
        Long version,
        /* 字典项集合；管理详情全部返回，业务读取仅返回启用项。 */
        List<DictionaryItemDto> items
) {

    /** 在事务内转为完整只读详情。 */
    public static DictionaryDto from(Dictionary dictionary) {
        return from(dictionary, false);
    }

    /** 仅在 DTO 组装时过滤业务可用项，不改变持久化集合；排序统一为排序值、编码、ID。 */
    public static DictionaryDto from(Dictionary dictionary, boolean availableOnly) {
        var items = dictionary.getItems().stream()
                .filter(item -> !availableOnly || item.isEnabled())
                .sorted(Comparator.comparingInt(DictionaryItem::getSortOrder)
                        .thenComparing(DictionaryItem::getCode)
                        .thenComparing(DictionaryItem::getId))
                .map(item -> new DictionaryItemDto(
                        item.getId(), item.getDictionaryId(), item.getCode(), item.getName(),
                        item.getSortOrder(), item.isDefault(), item.isEnabled()
                ))
                .toList();
        return new DictionaryDto(
                dictionary.getId(), dictionary.getCode(), dictionary.getName(),
                dictionary.getSystemCode(), dictionary.getSystemName(), dictionary.getDescription(),
                dictionary.isEnabled(), dictionary.getCreatedAt(), dictionary.getUpdatedAt(),
                dictionary.getVersion(), items
        );
    }
}
