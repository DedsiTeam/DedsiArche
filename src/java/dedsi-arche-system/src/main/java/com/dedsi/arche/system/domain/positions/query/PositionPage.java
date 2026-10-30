package com.dedsi.arche.system.domain.positions.query;

import java.util.List;
/** 岗位分页结果。 */
public record PositionPage(
        /* 当前页岗位。 */
        List<PositionListItemDto> items,
        /* 匹配总数。 */
        long total,
        /* 页码，从 1 开始。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
