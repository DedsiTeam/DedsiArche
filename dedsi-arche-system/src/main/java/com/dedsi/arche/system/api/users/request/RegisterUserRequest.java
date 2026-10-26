package com.dedsi.arche.system.api.users.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
/** 用户注册 HTTP 请求。 */
public record RegisterUserRequest(
    @Schema(description="登录名，存储时统一为小写", example="alice")
    @NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,50}") String username,
    @Schema(description="显示名称", example="Alice") @NotBlank @Size(max=100) String displayName,
    @Schema(description="密码，UTF-8 最多 72 字节", format="password", accessMode=Schema.AccessMode.WRITE_ONLY)
    @NotBlank @Size(min=12,max=64) String password
) {}
