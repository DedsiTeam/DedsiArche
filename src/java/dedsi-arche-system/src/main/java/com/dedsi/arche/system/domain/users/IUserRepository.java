package com.dedsi.arche.system.domain.users;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户聚合仓储契约。
 */
public interface IUserRepository {

    /**
     * 按用户标识加载聚合，不存在时返回空结果。
     */
    Optional<User> findById(UUID id);

    /** 管理用途单条查询加载完整用户聚合及岗位子实体，避免不同版本的片段组合。 */
    Optional<User> findAggregateById(UUID id);

    /** 检查岗位是否被任意用户引用，调用方已持有岗位删除写锁。 */
    boolean existsByPositionId(String positionId);

    /** 同步持久化变更与版本，确保冲突在事务内暴露。 */
    void flush();

    /**
     * 按规范化后的账号查找用户，不存在时返回空结果。
     */
    Optional<User> findByUsername(String username);

    /**
     * 保存用户聚合并返回持久化后的实体。
     */
    User save(User user);
    void delete(User user);
}
