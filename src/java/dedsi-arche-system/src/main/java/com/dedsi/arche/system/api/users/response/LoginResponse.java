package com.dedsi.arche.system.api.users.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 登录或刷新返回的凭据。
 *
 * @param accessToken 通过 Bearer 请求头传递的访问令牌。
 * @param tokenType 访问令牌类型，固定为 Bearer。
 * @param expiresAt 令牌过期时间。
 * @param refreshToken 用于刷新或撤销会话的凭据，每次刷新后必须替换。
 * @param user 不包含密码哈希的当前用户资料。
 */
public record LoginResponse(
        @Schema(description = "访问令牌，在 Authorization 请求头中按 Bearer 方式传递")
        String accessToken,
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "访问令牌过期时间，ISO-8601 UTC")
        Instant expiresAt,
        @Schema(description = "刷新令牌，每次刷新后必须用新值替换旧值")
        String refreshToken,
        @Schema(description = "当前用户资料")
        UserResponse user
) {
}
