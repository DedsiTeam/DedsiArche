package com.dedsi.arche.system.infrastructure.users.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
/** JWT 签发和刷新会话技术实现；不承载用户业务规则。 */
@Component
public class TokenService {
    private final JwtEncoder encoder;
    private final RefreshSessionRepository sessions;
    private final String issuer;
    private static final SecureRandom RANDOM=new SecureRandom();
    public TokenService(JwtEncoder encoder,RefreshSessionRepository sessions,@Value("${app.jwt.issuer}") String issuer) {
        this.encoder=encoder; this.sessions=sessions; this.issuer=issuer;
    }
    public record Tokens(String accessToken,Instant expiresAt,String refreshToken) {}
    @Transactional
    public Tokens issue(UUID userId) {
        Instant now=Instant.now(), expiry=now.plusSeconds(900);
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
            .issuedAt(now).expiresAt(expiry).id(UUID.randomUUID().toString())
            .audience(List.of("dedsi-api")).claim("scope","profile").build();
        String access=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes);
        String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new RefreshSession(userId,hash(refresh),now.plus(Duration.ofDays(7))));
        return new Tokens(access,expiry,refresh);
    }
    @Transactional
    public UUID consume(String refresh) {
        RefreshSession session=sessions.lockByHash(hash(refresh)).orElseThrow(()->new BadCredentialsException("刷新令牌无效"));
        if(session.revoked || !session.expiresAt.isAfter(Instant.now())) throw new BadCredentialsException("刷新令牌无效或已过期");
        session.revoked=true;
        return session.userId;
    }
    @Transactional
    public void revoke(String refresh) { sessions.lockByHash(hash(refresh)).ifPresent(s->s.revoked=true); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
