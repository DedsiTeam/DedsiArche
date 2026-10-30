package com.dedsi.arche.system.domain.permissions.query;

import java.util.List;

/**
 * 权限分页结果。
 */
public record PermissionPage(
        /* 当前页的权限记录。 */
        List<PermissionDto> items,
        /* 符合筛选条件的记录总数。 */
        long total,
        /* 页码，从 1 开始。 */
        int page,
        /* 每页条数，最多 100。 */
        int size
) {
}
