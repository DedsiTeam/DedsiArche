package com.dedsi.arche.system.api.dictionaries.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** 部分更新字典根、整体替换子项；系统与字典编码不可修改。 */
public record UpdateDictionaryRequest(
        /* null 保持名称，空白拒绝。 */
        @Schema(description = "名称，null 保持，空白拒绝，最多 100 字符")
        @Size(max = 100) String name,
        /* null 保持说明，空白清除。 */
        @Schema(description = "说明，null 保持，空白清除，最多 500 字符")
        @Size(max = 500) String description,
        /* null 保持启用状态。 */
        @Schema(description = "启用状态，省略或 null 保持")
        Boolean enabled,
        /* null 保持集合，空数组清空，非空数组全量替换。 */
        @Schema(description = "子项，null 保持、空数组清空、非空全量替换；已有项 ID 及编码保持")
        List<@NotNull @Valid DictionaryItemRequest> items,
        /* 必填根版本，保护整个字典聚合。 */
        @Schema(description = "当前字典 version，必填")
        @NotNull @Min(0) Long version
) {
}
