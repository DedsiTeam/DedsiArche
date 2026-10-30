package com.dedsi.arche.system.infrastructure.positions.queries;

import com.dedsi.arche.system.domain.positions.query.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

/**
 * 通过 JPA 投影查询岗位列表，避免将持久化实体直接作为响应返回。
 */
@Repository
@Transactional(readOnly = true)
public class JpaPositionQuery implements IPositionQuery {
    private final EntityManager em;

    /**
     * 初始化所需依赖。
     */
    public JpaPositionQuery(EntityManager em) {
        this.em = em;
    }

    /**
     * 按关键词和状态查询，使用固定排序保证分页顺序稳定。
     */
    public PositionPage search(String keyword, Boolean enabled, int page, int size) {
        String where = " from Position p where (locate(:keyword, lower(p.code)) > 0 or locate(:keyword, lower(p.name)) > "
                + "0)";
        if (enabled != null) {
            where += " and p.enabled=:enabled";
        }
        var rows = em.createQuery("select new com.dedsi.arche.system.domain.positions.query.PositionListItemDto(p.id,"
                + "p.systemCode,p.systemName,p.code,p.name,p.description,p.enabled,p.sortOrder,p.createdAt,p.updatedAt,p.version)" + where + " order by p.sortOrder, p.id",
                PositionListItemDto.class);
        var count = em.createQuery("select count(p)" + where, Long.class);
        for (var q : java.util.List.of(rows, count)) {
            q.setParameter("keyword", keyword.toLowerCase(Locale.ROOT));
            if (enabled != null) {
                q.setParameter("enabled", enabled);
            }
        }
        return new PositionPage(rows.setFirstResult((page - 1) * size).setMaxResults(size).getResultList(),
                count.getSingleResult(),
                page,
                size);
    }
}
