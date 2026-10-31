package com.dedsi.arche.system.infrastructure.loginaudits.repositories;

import com.dedsi.arche.system.domain.loginaudits.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/** 仅使用 persist 新增，不提供合并或删除入口。 */
@Repository
public class JpaLoginAuditRepository implements ILoginAuditRepository {
    /* 当前事务实体管理器。 */
    private final EntityManager em;

    /** 注入事务依赖。 */
    public JpaLoginAuditRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void append(LoginAudit audit) {
        em.persist(audit);
        em.flush();
    }
}
