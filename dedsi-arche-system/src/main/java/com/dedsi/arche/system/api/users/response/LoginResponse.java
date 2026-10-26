package com.dedsi.arche.system.api.users.response;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
/** 登录或刷新返回的凭据。 */
public record LoginResponse(
    @Schema(description="访问令牌，在 Authorization 请求头中按 Bearer 方式传递") String accessToken,
    @Schema(description="令牌类型", example="Bearer") String tokenType,
    @Schema(description="访问令牌过期时间，ISO-8601 UTC") Instant expiresAt,
    @Schema(description="刷新令牌，每次刷新后必须用新值替换旧值") String refreshToken,
    @Schema(description="当前用户资料") UserResponse user
) {}
