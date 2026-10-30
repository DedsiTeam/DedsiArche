package com.dedsi.arche.system.infrastructure.permissions.repositories;

import com.dedsi.arche.system.domain.permissions.*;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 使用 Spring Data 实现权限领域仓储。
 */
@Repository
public class JpaPermissionRepository implements IPermissionRepository {
    private final IPermissionJpaRepository delegate;
    private final jakarta.persistence.EntityManager em;

    /**
     * 初始化所需依赖。
     */
    public JpaPermissionRepository(
            IPermissionJpaRepository delegate,
            jakarta.persistence.EntityManager em
    ) {
        this.delegate = delegate;
        this.em = em;
    }

    /** 系统删除前检查引用；调用方已持有系统行锁。 */
    @Override
    public boolean existsBySystemCode(String systemCode) {
        return em.createQuery("select count(p) from Permission p where p.systemCode = :code", Long.class)
                .setParameter("code", systemCode)
                .getSingleResult() > 0;
    }

    /** 批量更名同时递增版本，旧权限编辑请求不能覆盖系统名称变化。 */
    @Override
    public void synchronizeSystemName(String systemCode, String systemName) {
        em.createQuery("update Permission p set p.systemName = :name, p.updatedAt = :now,"
                        + " p.version = p.version + 1 where p.systemCode = :code and p.systemName <> :name")
                .setParameter("name", systemName)
                .setParameter("now", java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")))
                .setParameter("code", systemCode)
                .executeUpdate();
    }

    /**
     * 按 ULID 加载权限，不存在时返回空结果。
     */
    public Optional<Permission> findById(String id) {
        return delegate.findById(id);
    }

    /**
     * 检查同一系统下的权限编码是否已存在。
     */
    public boolean existsBySystemCodeAndCode(String systemCode, String code) {
        return delegate.existsBySystemCodeAndCode(systemCode, code);
    }

    /**
     * 保存权限聚合。
     */
    public Permission save(Permission permission) {
        return delegate.save(permission);
    }

    /**
     * 删除权限；接口入口同时要求匹配当前版本。
     */
    public void delete(Permission permission) {
        delegate.delete(permission);
    }

    /**
     * 立即同步持久化变更，使约束与版本冲突在事务内暴露。
     */
    public void flush() {
        delegate.flush();
    }
}
