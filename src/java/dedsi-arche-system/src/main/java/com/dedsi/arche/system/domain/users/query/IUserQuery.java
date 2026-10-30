package com.dedsi.arche.system.domain.users.query;

import java.util.Optional;
import java.util.UUID;

/**
 * 与 HTTP 无关的用户查询契约。
 */
public interface IUserQuery {

    /**
     * 按用户标识查询只读资料，不存在时返回空结果。
     */
    Optional<UserQueryItemDto> findById(UUID id);
    /** 按关键词、联系方式和状态组合筛选，返回分页数据及相同条件下的总数。 */
    UserPage search(String keyword, String phone, String email, Boolean enabled, int page, int size);
}
