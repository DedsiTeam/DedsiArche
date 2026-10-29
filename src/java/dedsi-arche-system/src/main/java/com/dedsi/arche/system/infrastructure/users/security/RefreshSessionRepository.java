package com.dedsi.arche.system.infrastructure.users.security;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

/**
 * 刷新会话持久化契约，通过行锁保证令牌消费的并发安全。
 */
interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    /**
     * 同一刷新令牌只能被消费一次；并发刷新在数据库行锁处串行化。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RefreshSession s where s.tokenHash=:hash")
    Optional<RefreshSession> lockByHash(
            @Param("hash")
            String hash
    );
}
