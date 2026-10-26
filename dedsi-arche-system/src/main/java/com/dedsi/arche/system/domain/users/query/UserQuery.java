package com.dedsi.arche.system.domain.users.query;
import java.util.Optional;
import java.util.UUID;
/** 与 HTTP 无关的用户查询契约。 */
public interface UserQuery { Optional<UserQueryItemDto> findById(UUID id); }
