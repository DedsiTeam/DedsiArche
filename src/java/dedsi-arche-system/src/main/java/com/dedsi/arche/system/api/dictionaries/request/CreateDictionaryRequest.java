package com.dedsi.arche.system.api.dictionaries.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** 创建字典请求；所属系统名称只由服务端读取。 */
public record CreateDictionaryRequest(
        /* 已登记的所属系统编码，创建后不可变。 */
        @Schema(description = "所属系统编码，必填且最多 100 字符")
        @NotBlank @Size(max = 100) String systemCode,
        /* 系统内唯一字典编码，创建后不可变。 */
        @Schema(description = "字典编码，必填且最多 100 字符，在同一系统内唯一")
        @NotBlank @Size(max = 100) String code,
        /* 字典名称。 */
        @Schema(description = "字典名称，必填且最多 100 字符")
        @NotBlank @Size(max = 100) String name,
        /* 可空说明。 */
        @Schema(description = "说明，最多 500 字符")
        @Size(max = 500) String description,
        /* 默认启用。 */
        @Schema(description = "是否启用，省略或 null 默认 true")
        Boolean enabled,
        /* 整体创建子项，省略或 null 默认空集合。 */
        @Schema(description = "子项集合，省略或 null 默认空；创建时子项 ID 必须为空")
        List<@NotNull @Valid DictionaryItemRequest> items
) {
}
