package com.dedsi.arche.system.domain.users;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户聚合仓储契约。
 */
public interface UserRepository {

    /**
     * 按用户标识加载聚合，不存在时返回空结果。
     */
    Optional<User> findById(UUID id);

    /**
     * 按规范化后的用户名查找用户，不存在时返回空结果。
     */
    Optional<User> findByUsername(String username);

    /**
     * 保存用户聚合并返回持久化后的实体。
     */
    User save(User user);
    void delete(User user);
}
