package com.dedsi.arche.system.domain.loginaudits.query;

import java.util.List;

/** 登录审计分页结果。 */
public record LoginAuditPage(
        /* 当前页事实。 */
        List<LoginAuditDto> items,
        /* 匹配总数。 */
        long total,
        /* 从 1 开始的页码。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
