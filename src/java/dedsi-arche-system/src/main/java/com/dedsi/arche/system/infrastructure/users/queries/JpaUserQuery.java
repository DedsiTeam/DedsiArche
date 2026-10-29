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
public class JpaUserQuery implements UserQuery {

    /**
     * 执行用户只读投影查询的 JPA 实体管理器。
     */
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
