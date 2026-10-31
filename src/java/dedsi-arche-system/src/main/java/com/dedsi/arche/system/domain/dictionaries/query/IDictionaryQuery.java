package com.dedsi.arche.system.domain.dictionaries.query;

/** 字典只读投影查询，与聚合仓储分离。 */
public interface IDictionaryQuery {
    /** 按编码或名称搜索，并按系统编码及启用状态筛选。 */
    DictionaryPage search(String keyword, String systemCode, Boolean enabled, int page, int size);
}
