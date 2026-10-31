package com.dedsi.arche.system.api.dictionaries.request;

import com.dedsi.arche.system.domain.dictionaries.DictionaryItemInput;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 整体提交的字典项；编辑时已有项提交 ID，新项 ID 为空，创建字典时禁止指定任何子项 ID。 */
public record DictionaryItemRequest(
        /* 已有项 ULID；null 表示新增。 */
        @Schema(description = "已有项 ID，新项省略或 null；创建字典时不能指定")
        @Pattern(regexp = "[0-7][0-9A-HJKMNP-TV-Z]{25}") String id,
        /* 字典内唯一项编码，创建后不可改变。 */
        @Schema(description = "项编码，必填且最多 100 字符，创建后不可修改")
        @NotBlank @Size(max = 100) String code,
        /* 项名称。 */
        @Schema(description = "项名称，必填且最多 100 字符")
        @NotBlank @Size(max = 100) String name,
        /* 非负排序值，省略或 null 默认 0。 */
        @Schema(description = "排序值，默认 0，非负")
        @Min(0) Integer sortOrder,
        /* 是否默认项，省略或 null 默认 false。 */
        @Schema(description = "是否默认项，默认 false；最多一个默认项且该项必须启用")
        @JsonProperty("isDefault") Boolean isDefault,
        /* 是否启用，省略或 null 默认 true。 */
        @Schema(description = "是否启用，默认 true")
        Boolean enabled
) {
    /** 为整项输入补齐明确默认值；并非对子项进行部分编辑。 */
    public DictionaryItemRequest {
        sortOrder = sortOrder == null ? 0 : sortOrder;
        isDefault = isDefault != null && isDefault;
        enabled = enabled == null || enabled;
    }

    /** 将 API 数据转换为领域输入，不依赖数据库。 */
    public DictionaryItemInput toInput() {
        return new DictionaryItemInput(id, code, name, sortOrder, isDefault, enabled);
    }
}
