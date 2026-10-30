package com.dedsi.arche.system.infrastructure.users.repositories;

import com.dedsi.arche.system.domain.users.*;

import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 基础设施将业务仓储契约适配至 Spring Data。
 */
@Repository
public class JpaUserRepository implements IUserRepository {
    public void delete(User user) { delegate.delete(user); }

    /* 负责实际数据库操作的 Spring Data 仓储。 */
    private final IUserJpaRepository delegate;

    /**
     * 注入 Spring Data 仓储以适配领域仓储契约。
     */
    public JpaUserRepository(IUserJpaRepository delegate) {
        this.delegate = delegate;
    }

    /**
     * 按用户标识加载聚合，不存在时返回空结果。
     */
    public Optional<User> findById(UUID id) {
        return delegate.findById(id);
    }

    /**
     * 按规范化后的账号查找用户，不存在时返回空结果。
     */
    public Optional<User> findByUsername(String username) {
        return delegate.findByUsername(username);
    }

    /**
     * 保存用户聚合并返回持久化后的实体。
     */
    public User save(User user) {
        return delegate.save(user);
    }
}
