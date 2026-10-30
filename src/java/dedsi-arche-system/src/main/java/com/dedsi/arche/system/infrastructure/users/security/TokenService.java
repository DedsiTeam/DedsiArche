package com.dedsi.arche.system.infrastructure.users.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/**
 * JWT 签发和刷新会话技术实现；不承载用户业务规则。
 */
@Component
public class TokenService {
    private final com.dedsi.arche.system.domain.users.IUserRepository users;

    /* 使用配置密钥签发 JWT 的编码器。 */
    private final JwtEncoder encoder;

    /* 刷新会话仓储，支持加锁查询与持久化。 */
    private final IRefreshSessionRepository sessions;

    /* 写入 JWT 并在验签时核对的签发者。 */
    private final String issuer;

    /* 生成不可预测刷新令牌的密码学安全随机数源。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 注入 JWT 编码器、刷新会话仓储和签发者配置。
     */
    public TokenService(
            JwtEncoder encoder,
            IRefreshSessionRepository sessions,
            com.dedsi.arche.system.domain.users.IUserRepository users,
            @Value("${app.jwt.issuer}")
            String issuer
    ) {
        this.encoder = encoder;
        this.sessions = sessions;
        this.users = users;
        this.issuer = issuer;
    }

    /**
     * 一次签发的访问令牌、过期时间和刷新令牌。
     *
     * @param accessToken 通过 Bearer 请求头传递的访问令牌。
     * @param expiresAt 令牌过期北京时间。
     * @param expiresAtEpochMs 认证过期计时用毫秒时间戳。
     * @param refreshToken 用于刷新或撤销会话的凭据，每次刷新后必须替换。
     */
    public record Tokens(
            String accessToken,
            LocalDateTime expiresAt,
            long expiresAtEpochMs,
            String refreshToken
    ) {
    }

    /**
     * 签发十五分钟访问令牌和七天刷新令牌，仅持久化刷新令牌哈希。
     */
    @Transactional
    public Tokens issue(UUID userId, long credentialVersion) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime expiry = now.plusSeconds(900);
        // JWT 标准要求 NumericDate；协议时间戳仅在认证边界生成。
        Instant issuedInstant = now.toInstant(ZoneOffset.ofHours(8));
        Instant expiryInstant = expiry.toInstant(ZoneOffset.ofHours(8));
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(userId.toString())
                        .issuedAt(issuedInstant)
                        .expiresAt(expiryInstant)
                        .id(UUID.randomUUID().toString())
                        .audience(List.of("dedsi-api"))
                        .claim("scope", "profile")
                        .claim("credential_version", credentialVersion)
                        .build();
        String access =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new RefreshSession(userId, hash(refresh), now.plus(Duration.ofDays(7)), credentialVersion));
        return new Tokens(access, expiry, expiryInstant.toEpochMilli(), refresh);
    }

    /**
     * 加锁校验刷新会话并撤销旧令牌，返回所属用户标识。
     */
    @Transactional
    public UUID consume(String refresh) {
        RefreshSession session =
                sessions.lockByHash(hash(refresh))
                        .orElseThrow(() -> new BadCredentialsException("刷新令牌无效"));
        if (session.revoked || !session.expiresAt.isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
        session.revoked = true;
        if (users.findById(session.userId).filter(u -> u.getCredentialVersion() == session.credentialVersion).isEmpty()) {
            throw new BadCredentialsException("登录凭据已失效");
        }
        return session.userId;
    }

    /**
     * 撤销匹配的刷新会话；令牌不存在时不执行更新。
     */
    @Transactional
    public void revoke(String refresh) {
        sessions.lockByHash(hash(refresh)).ifPresent(s -> s.revoked = true);
    }

    @Transactional
    public void revokeAll(UUID userId) { sessions.revokeAllByUserId(userId); }

    @Transactional
    public void deleteSessions(UUID userId) { sessions.deleteAllByUserId(userId); }

    /**
     * 计算令牌的 SHA-256 十六进制摘要，避免明文凭据落库。
     */
    private static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
