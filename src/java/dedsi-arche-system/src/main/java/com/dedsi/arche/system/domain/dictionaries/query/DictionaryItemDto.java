package com.dedsi.arche.system.domain.dictionaries.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 字典项只读数据，isDefault 保持明确 JSON 字段名。 */
public record DictionaryItemDto(
        /* 字典项 ULID。 */
        String id,
        /* 所属字典 ULID。 */
        String dictionaryId,
        /* 字典内唯一且不可变的项编码。 */
        String code,
        /* 字典项名称。 */
        String name,
        /* 非负排序值。 */
        int sortOrder,
        /* 是否默认项，默认项必须启用。 */
        @JsonProperty("isDefault") boolean isDefault,
        /* 是否启用。 */
        boolean enabled
) {
}
