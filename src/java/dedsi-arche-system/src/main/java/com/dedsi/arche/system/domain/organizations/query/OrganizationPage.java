package com.dedsi.arche.system.domain.organizations.query;

import java.util.List;

/** 组织分页结果。 */
public record OrganizationPage(
        /* 当前页节点。 */
        List<OrganizationDto> items,
        /* 匹配总数。 */
        long total,
        /* 从 1 开始的页码。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
