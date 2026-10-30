package com.dedsi.arche.system.domain.systems.query;

import java.util.List;

/** 系统分页结果，沿用 User 的分页契约。 */
public record ManagedSystemPage(
        /* 当前页记录。 */
        List<ManagedSystemDto> items,
        /* 满足条件的总条数。 */
        long total,
        /* 从 1 开始的页码。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
