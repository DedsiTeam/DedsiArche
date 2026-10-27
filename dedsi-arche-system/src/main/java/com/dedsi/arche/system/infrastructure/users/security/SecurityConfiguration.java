package com.dedsi.arche.system.infrastructure.users.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 无 Cookie 的 Bearer API；只为明确的认证入口开放匿名请求。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * 解析 Base64 密钥并校验长度，构造 HS256 签名密钥。
     */
    @Bean
    SecretKey jwtKey(
            @Value("${app.jwt.secret}")
            String secret
    ) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET 必须为至少 32 字节随机密钥的 Base64");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * 创建使用共享密钥签发 JWT 的编码器。
     */
    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * 创建 JWT 解码器，验证签名、有效期、签发者和目标受众。
     */
    @Bean
    JwtDecoder jwtDecoder(
            SecretKey key,
            @Value("${app.jwt.issuer}")
            String issuer
    ) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience =
                jwt ->
                        jwt.getAudience().contains("dedsi-api")
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }

    /**
     * 创建工作因子为 12 的 BCrypt 密码编码器。
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 配置无状态 Bearer 认证、匿名入口和文档访问开关。
     */
    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            @Value("${app.docs.enabled:true}")
            boolean docsEnabled
    ) throws Exception {
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                HttpMethod.POST,
                                                "/api/users/register",
                                                "/api/users/login",
                                                "/api/users/refresh",
                                                "/api/users/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                "/scalar",
                                                "/scalar/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/v3/api-docs.yaml")
                                        .access(
                                                (authentication, context) ->
                                                        new AuthorizationDecision(docsEnabled))
                                        .requestMatchers("/actuator/health", "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> {
                            // 使用已注册的 JwtDecoder 及默认 Bearer 令牌转换规则。
                        }))
                .build();
    }
}
