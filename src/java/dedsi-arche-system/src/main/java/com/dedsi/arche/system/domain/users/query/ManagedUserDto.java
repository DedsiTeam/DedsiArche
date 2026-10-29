package com.dedsi.arche.system.domain.users.query;

import java.time.Instant;
import java.util.UUID;

/** 管理列表投影，禁止包含密码与令牌。 */
public record ManagedUserDto(UUID id, String username, String displayName, boolean enabled, Instant createdAt, Long version) {}
