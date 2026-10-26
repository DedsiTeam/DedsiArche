package com.dedsi.arche.system.infrastructure.users.security;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
/** 认证内部存储记录，不是独立业务领域；仅保存刷新令牌的 SHA-256。 */
@Entity
@Table(name="sys_refresh_sessions")
class RefreshSession {
    @Id UUID id;
    @Column(name="user_id",nullable=false) UUID userId;
    @Column(name="token_hash",nullable=false,unique=true,length=64) String tokenHash;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
    @Column(nullable=false) boolean revoked;
    protected RefreshSession() {}
    RefreshSession(UUID userId,String hash,Instant expiry) {
        id=UUID.randomUUID(); this.userId=userId; tokenHash=hash; expiresAt=expiry;
    }
}
