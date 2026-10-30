package com.dedsi.arche.system.api.users.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * 对外用户信息，不包含密码哈希或内部认证数据。
 *
 * @param id 记录的唯一标识。
 * @param username 规范化为小写的账号。
 * @param displayName 向调用方展示的账号称。
 */
public record UserResponse(
        @Schema(description = "用户标识")
        UUID id,
        @Schema(description = "账号", example = "alice")
        String username,
        @Schema(description = "姓名", example = "Alice")
        String displayName,
        @Schema(description = "是否可以管理用户")
        boolean canManageUsers
) {
}
