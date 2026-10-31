package com.dedsi.arche.system.domain.dictionaries.query;

import java.util.List;

/** 字典分页结果，沿用统一分页契约。 */
public record DictionaryPage(
        /* 当前页字典摘要。 */
        List<DictionaryListItemDto> items,
        /* 符合条件的总数。 */
        long total,
        /* 页码，从 1 开始。 */
        int page,
        /* 每页条数。 */
        int size
) {
}
