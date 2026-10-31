package com.dedsi.arche.system.api.dictionaries.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 字典分页条件，页码从 1 开始。 */
public record DictionaryPageRequest(
        /* 按编码或名称匹配，忽略大小写。 */
        @Schema(description = "编码或名称关键词，默认空串")
        @Size(max = 100) String keyword,
        /* 可选系统精确筛选。 */
        @Schema(description = "所属系统编码，省略或空白时不筛选")
        @Size(max = 100) String systemCode,
        /* null 不筛选。 */
        @Schema(description = "启用状态，null 不筛选")
        Boolean enabled,
        /* 默认第 1 页。 */
        @Schema(description = "页码，默认 1，最大 1000000")
        @Min(1) @Max(1000000) Integer page,
        /* 默认 10 条，上限 100。 */
        @Schema(description = "每页条数，默认 10，最大 100")
        @Min(1) @Max(100) Integer size
) {
    /** 规范化可选条件并补齐分页默认值。 */
    public DictionaryPageRequest {
        keyword = keyword == null ? "" : keyword.trim();
        systemCode = systemCode == null ? null : systemCode.trim();
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
