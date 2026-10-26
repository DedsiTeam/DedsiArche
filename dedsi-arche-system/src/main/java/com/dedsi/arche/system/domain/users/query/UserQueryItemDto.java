package com.dedsi.arche.system.domain.users.query;
import java.util.UUID;
/** 用户只读投影；不包含认证凭据。 */
public record UserQueryItemDto(UUID id, String username, String displayName) {}
