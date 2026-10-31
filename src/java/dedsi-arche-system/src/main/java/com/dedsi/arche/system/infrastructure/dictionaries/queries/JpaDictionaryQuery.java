package com.dedsi.arche.system.infrastructure.dictionaries.queries;

import com.dedsi.arche.system.domain.dictionaries.query.DictionaryListItemDto;
import com.dedsi.arche.system.domain.dictionaries.query.DictionaryPage;
import com.dedsi.arche.system.domain.dictionaries.query.IDictionaryQuery;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

/** 直接投影字典摘要，分页不加载子项，所有条件通过绑定参数传递。 */
@Repository
@Transactional(readOnly = true)
public class JpaDictionaryQuery implements IDictionaryQuery {
    /* 只读投影实体管理器。 */
    private final EntityManager em;

    /** 注入查询依赖。 */
    public JpaDictionaryQuery(EntityManager em) {
        this.em = em;
    }

    @Override
    public DictionaryPage search(String keyword, String systemCode, Boolean enabled, int page, int size) {
        String where = " from Dictionary d where (locate(:keyword, lower(d.code)) > 0"
                + " or locate(:keyword, lower(d.name)) > 0)";
        if (systemCode != null && !systemCode.isBlank()) {
            where += " and d.systemCode = :systemCode";
        }
        if (enabled != null) {
            where += " and d.enabled = :enabled";
        }
        var rows = em.createQuery(
                "select new com.dedsi.arche.system.domain.dictionaries.query.DictionaryListItemDto"
                        + "(d.id,d.code,d.name,d.systemCode,d.systemName,d.description,d.enabled,"
                        + "d.createdAt,d.updatedAt,d.version)" + where
                        + " order by d.systemCode, d.code, d.id",
                DictionaryListItemDto.class
        );
        var count = em.createQuery("select count(d)" + where, Long.class);
        for (var query : List.of(rows, count)) {
            query.setParameter("keyword", keyword.toLowerCase(Locale.ROOT));
            if (systemCode != null && !systemCode.isBlank()) {
                query.setParameter("systemCode", systemCode);
            }
            if (enabled != null) {
                query.setParameter("enabled", enabled);
            }
        }
        return new DictionaryPage(
                rows.setFirstResult((page - 1) * size).setMaxResults(size).getResultList(),
                count.getSingleResult(), page, size
        );
    }
}
