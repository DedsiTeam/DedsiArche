package com.dedsi.arche.system.domain.users.query;

import java.util.UUID;

/** 用户岗位子实体的只读快照，不用于动态鉴权。 */
public record UserPositionDto(
        /* 用户岗位关联 ULID。 */
        String id,
        /* 所属用户 UUID。 */
        UUID userId,
        /* 原岗位 ULID。 */
        String positionId,
        /* 配置时的岗位编码。 */
        String positionCode,
        /* 配置时的岗位名称。 */
        String positionName,
        /* 配置时的系统编码，历史未登记岗位为空。 */
        String systemCode,
        /* 配置时的系统名称，与系统编码同时为空或非空。 */
        String systemName
) {
}
