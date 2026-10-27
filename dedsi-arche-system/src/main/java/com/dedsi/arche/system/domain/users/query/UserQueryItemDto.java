package com.dedsi.arche.system.domain.users.query;

import java.util.UUID;

/**
 * 用户只读投影；不包含认证凭据。
 *
 * @param id 记录的唯一标识。
 * @param username 规范化为小写的登录用户名。
 * @param displayName 向调用方展示的用户名称。
 */
public record UserQueryItemDto(
        UUID id,
        String username,
        String displayName
) {
}
