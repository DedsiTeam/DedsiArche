package com.dedsi.arche.system.infrastructure.systems.queries;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import com.dedsi.arche.system.domain.systems.query.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

/** 使用相同筛选条件查询投影和总数，参数绑定防止输入被解释成查询语法。 */
@Repository
@Transactional(readOnly = true)
public class JpaManagedSystemQuery implements IManagedSystemQuery {
    private final EntityManager em;

    /** 注入只读投影使用的实体管理器。 */
    public JpaManagedSystemQuery(EntityManager em) {
        this.em = em;
    }

    /** 直接投影所需两列，不加载完整实体或执行分页总数查询。 */
    @Override
    public List<ManagedSystemOptionDto> options() {
        return em.createQuery(
                "select new com.dedsi.arche.system.domain.systems.query.ManagedSystemOptionDto"
                        + "(s.code, s.name) from ManagedSystem s order by s.sortOrder, s.id",
                ManagedSystemOptionDto.class
        ).getResultList();
    }

    /** 排序值相同时按 ULID 排序，保证稳定的分页顺序。 */
    @Override
    public ManagedSystemPage search(String keyword, SystemStatus status, int page, int size) {
        String where = " from ManagedSystem s where (locate(:keyword, lower(s.code)) > 0"
                + " or locate(:keyword, lower(s.name)) > 0)";
        if (status != null) {
            where += " and s.status = :status";
        }
        var rows = em.createQuery(
                "select new com.dedsi.arche.system.domain.systems.query.ManagedSystemDto"
                        + "(s.id,s.code,s.name,s.description,s.entryUrl,s.status,s.sortOrder,"
                        + "s.createdAt,s.updatedAt,s.version)" + where + " order by s.sortOrder, s.id",
                ManagedSystemDto.class
        );
        var count = em.createQuery("select count(s)" + where, Long.class);
        for (var query : List.of(rows, count)) {
            query.setParameter("keyword", keyword.toLowerCase(Locale.ROOT));
            if (status != null) {
                query.setParameter("status", status);
            }
        }
        return new ManagedSystemPage(
                rows.setFirstResult((page - 1) * size).setMaxResults(size).getResultList(),
                count.getSingleResult(),
                page,
                size
        );
    }
}
