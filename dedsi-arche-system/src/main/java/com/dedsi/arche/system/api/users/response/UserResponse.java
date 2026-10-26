package com.dedsi.arche.system.api.users.response;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
/** 对外用户信息，不包含密码哈希或内部认证数据。 */
public record UserResponse(
    @Schema(description="用户标识") UUID id,
    @Schema(description="登录名", example="alice") String username,
    @Schema(description="显示名称", example="Alice") String displayName
) {}
