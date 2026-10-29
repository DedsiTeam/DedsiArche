package com.dedsi.arche.system.infrastructure.users.security;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 认证内部存储记录，不是独立业务领域；仅保存刷新令牌的 SHA-256。
 */
@Entity
@Table(name = "sys_refresh_sessions")
class RefreshSession {

    /**
     * 记录的唯一标识。
     */
    @Id
    UUID id;

    /**
     * 刷新会话所属用户的唯一标识。
     */
    @Column(name = "user_id", nullable = false)
    UUID userId;

    /**
     * 刷新令牌的 SHA-256 摘要，用于检索会话。
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    String tokenHash;

    /**
     * 令牌过期时间。
     */
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    /**
     * 会话是否已因刷新或退出而撤销。
     */
    @Column(nullable = false)
    boolean revoked;
    @Column(name = "credential_version", nullable = false)
    long credentialVersion;

    /**
     * 供 JPA 实例化实体使用。
     */
    protected RefreshSession() {
    }

    /**
     * 使用用户标识、令牌摘要和过期时间创建未撤销的刷新会话。
     */
    RefreshSession(
            UUID userId,
            String hash,
            Instant expiry,
            long credentialVersion
    ) {
        id = UUID.randomUUID();
        this.userId = userId;
        tokenHash = hash;
        expiresAt = expiry;
        this.credentialVersion = credentialVersion;
    }
}
