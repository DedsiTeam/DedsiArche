package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

/**
 * 用户注册 HTTP 请求。
 *
 * @param username 规范化为小写的账号。
 * @param displayName 向调用方展示的账号称。
 * @param password 请求中的明文密码，仅用于当前认证或注册处理。
 */
public record RegisterUserRequest(
        @Schema(description = "账号，存储时统一为小写", example = "alice")
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9_]{3,50}")
        String username,
        @Schema(description = "姓名", example = "Alice")
        @NotBlank
        @Size(max = 100)
        String displayName,
        @Schema(description = "密码，UTF-8 最多 72 字节", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 12, max = 64)
        String password,
        @Schema(description = "手机号，选填") @Size(max = 16) String phone,
        @Schema(description = "邮箱，选填") @Size(max = 254) String email
) {
    public RegisterUserRequest(String username, String displayName, String password) {
        this(username, displayName, password, null, null);
    }
}
