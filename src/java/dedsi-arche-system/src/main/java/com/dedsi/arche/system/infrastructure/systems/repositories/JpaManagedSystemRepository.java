package com.dedsi.arche.system.infrastructure.systems.repositories;

import com.dedsi.arche.system.domain.systems.IManagedSystemRepository;
import com.dedsi.arche.system.domain.systems.ManagedSystem;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/** 将领域仓储契约适配到 Spring Data。 */
@Repository
public class JpaManagedSystemRepository implements IManagedSystemRepository {
    private final IManagedSystemJpaRepository delegate;

    /** 注入系统持久化接口。 */
    public JpaManagedSystemRepository(IManagedSystemJpaRepository delegate) {
        this.delegate = delegate;
    }

    /** 按标识读取系统。 */
    @Override
    public Optional<ManagedSystem> findById(String id) {
        return delegate.findById(id);
    }

    /** 按标识锁定系统。 */
    @Override
    public Optional<ManagedSystem> lockById(String id) {
        return delegate.lockById(id);
    }

    /** 按编码锁定系统。 */
    @Override
    public Optional<ManagedSystem> lockByCode(String code) {
        return delegate.lockByCode(code);
    }

    /** 检查编码是否已存在。 */
    @Override
    public boolean existsByCode(String code) {
        return delegate.existsByCode(code);
    }

    /** 保存系统聚合。 */
    @Override
    public ManagedSystem save(ManagedSystem system) {
        return delegate.save(system);
    }

    /** 删除系统聚合。 */
    @Override
    public void delete(ManagedSystem system) {
        delegate.delete(system);
    }
}
