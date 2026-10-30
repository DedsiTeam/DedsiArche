package com.dedsi.arche.system.api.systems.request;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 系统管理请求，字段校验与领域业务约束共同生效。 */
public record ManagedSystemPageRequest(
        /* 编码或名称关键词，默认空串。 */
        @Schema(description = "编码或名称关键词，默认空串。")
        @Size(max = 100)
        String keyword,
        /* 可选状态筛选。 */
        @Schema(description = "可选状态筛选。")
        SystemStatus status,
        /* 页码默认 1。 */
        @Schema(description = "页码默认 1。")
        @Min(1) @Max(1000000)
        Integer page,
        /* 每页条数默认 10。 */
        @Schema(description = "每页条数默认 10。")
        @Min(1) @Max(100)
        Integer size
) {
    /** 为省略的筛选和分页参数提供默认值。 */
    public ManagedSystemPageRequest {
        keyword = keyword == null ? "" : keyword.trim();
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
