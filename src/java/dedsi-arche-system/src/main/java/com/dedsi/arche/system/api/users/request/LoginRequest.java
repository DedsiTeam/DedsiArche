package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

/**
 * 账号密码登录 HTTP 请求。
 *
 * @param username 规范化为小写的登录用户名。
 * @param password 请求中的明文密码，仅用于当前认证或注册处理。
 */
public record LoginRequest(
        @Schema(description = "登录名", example = "alice")
        @NotBlank
        String username,
        @Schema(description = "登录密码", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        String password
) {
}
