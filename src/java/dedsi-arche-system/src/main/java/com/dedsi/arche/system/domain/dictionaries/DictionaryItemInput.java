package com.dedsi.arche.system.domain.dictionaries;

/** 字典项整体配置输入；新增项不传标识，已有项保留原标识。 */
public record DictionaryItemInput(
        /* 已有字典项标识；null 表示新增。 */
        String id,
        /* 字典内唯一且创建后不可修改的编码。 */
        String code,
        /* 展示名称。 */
        String name,
        /* 非负排序值，越小越靠前。 */
        int sortOrder,
        /* 默认项标记；默认项必须启用。 */
        boolean isDefault,
        /* 是否启用。 */
        boolean enabled
) {
}
