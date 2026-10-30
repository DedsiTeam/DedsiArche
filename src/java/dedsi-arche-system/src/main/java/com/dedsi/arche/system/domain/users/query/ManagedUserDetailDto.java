package com.dedsi.arche.system.domain.users.query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 管理详情、新增与更新响应，包含完整岗位集合；分页继续使用原摘要 DTO。 */
public record ManagedUserDetailDto(
        /* 用户 UUID。 */
        UUID id,
        /* 规范化账号。 */
        String username,
        /* 用户姓名。 */
        String displayName,
        /* 是否允许登录。 */
        boolean enabled,
        /* 创建时间，北京时间。 */
        LocalDateTime createdAt,
        /* 资料和岗位集合共用的乐观锁版本。 */
        Long version,
        /* 联系电话，可空。 */
        String phone,
        /* 邮箱，可空。 */
        String email,
        /* 用户岗位子实体及其完整快照集合。 */
        List<UserPositionDto> positions
) {
}
