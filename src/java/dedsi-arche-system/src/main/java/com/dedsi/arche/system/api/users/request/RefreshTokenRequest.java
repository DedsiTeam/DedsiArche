package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

/**
 * 刷新或注销请求，不要传入 accessToken。
 *
 * @param refreshToken 用于刷新或撤销会话的凭据，每次刷新后必须替换。
 */
public record RefreshTokenRequest(
        @Schema(description = "登录或刷新接口返回的 refreshToken", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(max = 256)
        String refreshToken
) {
}
