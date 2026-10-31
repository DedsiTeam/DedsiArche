package com.dedsi.arche.system.domain.dictionaries.query;

import io.swagger.v3.oas.annotations.media.Schema;

/** 下拉选项的最小只读响应，不暴露聚合及子项管理字段。 */
@Schema(description = "字典项下拉选项")
public record DictionaryOptionDto(
        /* 字典项编码。 */
        @Schema(description = "字典项编码") String code,
        /* 字典项显示名称。 */
        @Schema(description = "字典项名称") String name
) {
}
