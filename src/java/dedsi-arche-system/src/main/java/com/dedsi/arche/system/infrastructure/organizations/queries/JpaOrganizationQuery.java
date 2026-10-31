package com.dedsi.arche.system.infrastructure.organizations.queries;

import com.dedsi.arche.system.domain.organizations.Organization;
import com.dedsi.arche.system.domain.organizations.query.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 只读分页与树查询，完整树一次加载，避免层级 N+1 与跨快照拼接。 */
@Repository
@Transactional(readOnly = true)
public class JpaOrganizationQuery implements IOrganizationQuery {
    /* 只读实体管理器。 */
    private final EntityManager em;

    /** 注入查询依赖。 */
    public JpaOrganizationQuery(EntityManager em) {
        this.em = em;
    }

    @Override
    public OrganizationPage search(String keyword, String systemCode, Boolean enabled, int page, int size) {
        String where = " from Organization o where (locate(:keyword, lower(o.code)) > 0"
                + " or locate(:keyword, lower(o.name)) > 0)";
        if (systemCode != null && !systemCode.isBlank()) {
            where += " and o.systemCode = :systemCode";
        }
        if (enabled != null) {
            where += " and o.enabled = :enabled";
        }
        var rows = em.createQuery("select o" + where + " order by o.systemCode, o.sortOrder, o.code, o.id", Organization.class);
        var count = em.createQuery("select count(o)" + where, Long.class);
        for (var statement : List.of(rows, count)) {
            statement.setParameter("keyword", keyword.toLowerCase(Locale.ROOT));
            if (systemCode != null && !systemCode.isBlank()) {
                statement.setParameter("systemCode", systemCode);
            }
            if (enabled != null) {
                statement.setParameter("enabled", enabled);
            }
        }
        return new OrganizationPage(
                rows.setFirstResult((page - 1) * size).setMaxResults(size).getResultList().stream()
                        .map(OrganizationDto::from).toList(),
                count.getSingleResult(), page, size
        );
    }

    @Override
    public List<OrganizationTreeDto> tree(String systemCode) {
        var rows = em.createQuery("select o from Organization o where o.systemCode = :systemCode"
                + " order by o.sortOrder, o.code, o.id", Organization.class)
                .setParameter("systemCode", systemCode).getResultList();
        // 用两遍迭代连接节点，避免深树递归查询及构造时的栈溢出。
        Map<String, OrganizationTreeDto> nodes = new LinkedHashMap<>();
        for (var row : rows) {
            nodes.put(row.getId(), OrganizationTreeDto.from(row, new ArrayList<>()));
        }
        List<OrganizationTreeDto> roots = new ArrayList<>();
        for (var node : nodes.values()) {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                var parent = nodes.get(node.parentId());
                if (parent == null) {
                    throw new IllegalStateException("组织上级引用不完整");
                }
                parent.children().add(node);
            }
        }
        return List.copyOf(roots);
    }
}
