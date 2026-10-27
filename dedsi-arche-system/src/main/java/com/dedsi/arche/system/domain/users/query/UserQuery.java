package com.dedsi.arche.system.domain.users.query;

import java.util.Optional;
import java.util.UUID;

/**
 * 与 HTTP 无关的用户查询契约。
 */
public interface UserQuery {

    /**
     * 按用户标识查询只读资料，不存在时返回空结果。
     */
    Optional<UserQueryItemDto> findById(UUID id);
}
