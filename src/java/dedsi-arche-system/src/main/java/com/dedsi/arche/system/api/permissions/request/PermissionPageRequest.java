package com.dedsi.arche.system.api.permissions.request;

import jakarta.validation.constraints.*;

/**
 * 权限分页筛选条件；未指定页码和条数时使用 1 和 10。
 */
public record PermissionPageRequest(
        /* 搜索关键词，匹配权限编码、权限名称和系统名称。 */
        @Size(max = 100) String keyword,
        /* 所属系统编码，与权限编码共同构成唯一键，创建后不可修改。 */
        @Size(max = 100) String systemCode,
        /* 权限定义是否启用；当前不承担用户授权判定。 */
        Boolean enabled,
        /* 页码，从 1 开始。 */
        @Min(1) @Max(1000000) Integer page,
        /* 每页条数，最多 100。 */
        @Min(1) @Max(100) Integer size
) {
    public PermissionPageRequest {
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
