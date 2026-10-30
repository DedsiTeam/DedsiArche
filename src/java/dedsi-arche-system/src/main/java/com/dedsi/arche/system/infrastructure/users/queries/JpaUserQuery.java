package com.dedsi.arche.system.infrastructure.users.queries;

import com.dedsi.arche.system.domain.users.query.*;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 直接查询只读投影，无需向 API 暴露实体。
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserQuery implements IUserQuery {
    /** 列表与总数共用筛选条件；参数绑定使输入按普通文本匹配。 */
    public UserPage search(String keyword, String phone, String email, Boolean enabled, int page, int size) {
        String where = " from User u where (locate(:keyword, lower(u.username)) > 0"
                + " or locate(:keyword, lower(u.displayName)) > 0)";
        if (!phone.isEmpty()) {
            where += " and locate(:phone, u.phone) > 0";
        }
        if (!email.isEmpty()) {
            where += " and locate(:email, lower(u.email)) > 0";
        }
        if (enabled != null) {
            where += " and u.enabled=:enabled";
        }
        var rows = em.createQuery("select new com.dedsi.arche.system.domain.users.query.ManagedUserDto"
                + "(u.id,u.username,u.displayName,u.enabled,u.createdAt,u.version,u.phone,u.email)"
                + where + " order by u.createdAt desc, u.id", ManagedUserDto.class);
        var count = em.createQuery("select count(u)" + where, Long.class);
        for (var query : List.of(rows, count)) {
            query.setParameter("keyword", keyword.toLowerCase(Locale.ROOT));
            if (!phone.isEmpty()) {
                query.setParameter("phone", phone);
            }
            if (!email.isEmpty()) {
                query.setParameter("email", email.toLowerCase(Locale.ROOT));
            }
            if (enabled != null) {
                query.setParameter("enabled", enabled);
            }
        }
        return new UserPage(rows.setFirstResult((page - 1) * size).setMaxResults(size).getResultList(),
                count.getSingleResult(), page, size);
    }

    /* 执行用户只读投影查询的 JPA 实体管理器。 */
    private final EntityManager em;

    /**
     * 注入实体管理器，用于执行只读资料投影查询。
     */
    public JpaUserQuery(EntityManager em) {
        this.em = em;
    }

    /**
     * 按用户标识查询只读资料，不存在时返回空结果。
     */
    public Optional<UserQueryItemDto> findById(UUID id) {
        return em.createQuery(
                        "select new"
                            + " com.dedsi.arche.system.domain.users.query.UserQueryItemDto"
                            + "(u.id,u.username,u.displayName)"
                            + " from User u where u.id=:id",
                        UserQueryItemDto.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }
}
