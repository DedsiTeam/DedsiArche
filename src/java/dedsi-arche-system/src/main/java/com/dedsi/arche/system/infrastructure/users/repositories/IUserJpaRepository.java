package com.dedsi.arche.system.infrastructure.users.repositories;

import com.dedsi.arche.system.domain.users.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data 自动实现基础 CRUD。
 */
public interface IUserJpaRepository extends JpaRepository<User, UUID> {

    /** 管理完整读取与认证轻量读取分离；左连接保留无岗位的用户。 */
    @Query("select distinct u from User u left join fetch u.positions where u.id = :id")
    Optional<User> findAggregateById(@Param("id") UUID id);

    /** 按岗位检查用户引用，索引和外键共同保护删除。 */
    @Query("select count(item) > 0 from UserPosition item where item.positionId = :positionId")
    boolean existsByPositionId(@Param("positionId") String positionId);

    /**
     * 按规范化后的账号查找用户，不存在时返回空结果。
     */
    Optional<User> findByUsername(String username);
}
