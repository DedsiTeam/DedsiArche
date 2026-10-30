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

    /** 先按 ID 顺序锁定全部权限，再逐项刷新，避免事件同步使用一级缓存中的旧值。 */
    @Override
    public java.util.List<Permission> lockAllBySystemCode(String systemCode) {
        var permissions = em.createQuery(
                        "select p from Permission p where p.systemCode = :code order by p.id", Permission.class)
                .setParameter("code", systemCode)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        for (var permission : permissions) {
            em.refresh(permission, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        return permissions;
    }

    /**
     * 按 ULID 加载权限，不存在时返回空结果。
     */
    public Optional<Permission> findById(String id) {
        return delegate.findById(id);
    }

    /** 与权限更名写锁协调；刷新一级缓存，避免等待锁后复制旧名称。 */
    @Override
    public Optional<Permission> lockByIdForPosition(String id) {
        var permission = delegate.findById(id);
        if (permission.isPresent()) {
            try {
                em.refresh(permission.get(), jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            } catch (jakarta.persistence.EntityNotFoundException exception) {
                // 普通读取与加锁之间权限已被删除，仍按所选权限不存在处理；锁异常继续向上传播。
                return Optional.empty();
            }
        }
        return permission;
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
