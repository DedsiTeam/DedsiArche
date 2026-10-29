package com.dedsi.arche.system.infrastructure.users.repositories;

import com.dedsi.arche.system.domain.users.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data 自动实现基础 CRUD。
 */
public interface UserJpaRepository extends JpaRepository<User, UUID> {

    /**
     * 按规范化后的用户名查找用户，不存在时返回空结果。
     */
    Optional<User> findByUsername(String username);
}
