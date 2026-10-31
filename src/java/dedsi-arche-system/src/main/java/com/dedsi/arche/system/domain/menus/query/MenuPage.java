package com.dedsi.arche.system.domain.menus.query;

import java.util.List;

/** 菜单分页结果。 */
public record MenuPage(
        /* 当前页节点。 */
        List<MenuDto> items,
        /* 匹配总数。 */
        long total,
        /* 从 1 开始的页码。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
