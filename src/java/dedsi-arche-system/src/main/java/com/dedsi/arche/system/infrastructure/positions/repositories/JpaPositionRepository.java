package com.dedsi.arche.system.infrastructure.positions.repositories;

import com.dedsi.arche.system.domain.positions.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;

/** 使用 Spring Data 适配岗位领域仓储。 */
@Repository
public class JpaPositionRepository implements IPositionRepository {
    /* 实际执行持久化的 Spring Data 仓储。 */
    private final IPositionJpaRepository delegate;
    /* 岗位根单独加锁，用户配置不需要读取岗位权限集合。 */
    private final EntityManager em;

    /** 注入持久化委托。 */
    public JpaPositionRepository(IPositionJpaRepository delegate, EntityManager em) {
        this.delegate = delegate;
        this.em = em;
    }

    @Override
    public Optional<Position> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<Position> lockByIdForUser(String id) {
        return lockRoot(id, LockModeType.PESSIMISTIC_READ);
    }

    @Override
    public Optional<Position> lockByIdForDeletion(String id) {
        return lockRoot(id, LockModeType.PESSIMISTIC_WRITE);
    }

    /** 直接读取岗位根并刷新一级缓存，不走管理用的权限 fetch join；锁持有到事务结束。 */
    private Optional<Position> lockRoot(String id, LockModeType lockMode) {
        var position = em.find(Position.class, id, lockMode);
        if (position == null) {
            return Optional.empty();
        }
        try {
            em.refresh(position, lockMode);
        } catch (EntityNotFoundException exception) {
            return Optional.empty();
        }
        return Optional.of(position);
    }

    @Override
    public List<Position> findAllByPermissionId(String permissionId) {
        return delegate.findAllByPermissionId(permissionId);
    }

    @Override
    public List<Position> findAllReferencingSystem(String systemCode) {
        return delegate.findAllReferencingSystem(systemCode);
    }

    @Override
    public boolean existsBySystemCode(String systemCode) {
        return delegate.existsBySystemCode(systemCode);
    }

    @Override
    public boolean existsByCode(String code) {
        return delegate.existsByCode(code);
    }

    @Override
    public Position save(Position position) {
        return delegate.save(position);
    }

    @Override
    public void delete(Position position) {
        delegate.delete(position);
    }

    @Override
    public void flush() {
        delegate.flush();
    }
}
