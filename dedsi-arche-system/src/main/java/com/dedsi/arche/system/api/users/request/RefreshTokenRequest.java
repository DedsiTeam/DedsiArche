package com.dedsi.arche.system.api.users.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
/** 刷新或注销请求，不要传入 accessToken。 */
public record RefreshTokenRequest(
    @Schema(description="登录或刷新接口返回的 refreshToken", accessMode=Schema.AccessMode.WRITE_ONLY)
    @NotBlank @Size(max=256) String refreshToken
) {}
