package com.dedsi.arche.system.api.positions.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/** 岗位分页请求。 */
public record PositionPageRequest(
        /* 按岗位编码或名称搜索，默认空串。 */
        @Schema(description = "按岗位编码或名称搜索，默认空串。")
        @Size(max = 100) String keyword,
        /* 启用状态筛选，null 不筛选。 */
        @Schema(description = "启用状态筛选，null 不筛选。")
        Boolean enabled,
        /* 页码从 1 开始，默认 1。 */
        @Schema(description = "页码从 1 开始，默认 1。")
        @Min(1) @Max(1000000) Integer page,
        /* 每页条数，默认 10，上限 100。 */
        @Schema(description = "每页条数，默认 10，上限 100。")
        @Min(1) @Max(100) Integer size
) {
    public PositionPageRequest {
        keyword = keyword == null ? "" : keyword.trim();
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
