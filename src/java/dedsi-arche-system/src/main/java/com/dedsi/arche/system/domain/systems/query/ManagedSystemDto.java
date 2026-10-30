package com.dedsi.arche.system.domain.systems.query;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import java.time.LocalDateTime;

/** 系统详情与分页只读数据，不直接序列化持久化实体。 */
public record ManagedSystemDto(
        /* 26 位 ULID 主键。 */
        String id,
        /* 系统编码，全局唯一且创建后不可修改。 */
        String code,
        /* 系统名称，与权限系统名称长度保持一致。 */
        String name,
        /* 系统说明，可为空。 */
        String description,
        /* HTTP/HTTPS 入口地址，可为空，不承担 SSO 回调配置。 */
        String entryUrl,
        /* 人工维护的业务状态，默认未上线。 */
        SystemStatus status,
        /* 展示排序，非负且数值越小越靠前。 */
        int sortOrder,
        /* 创建北京时间。 */
        LocalDateTime createdAt,
        /* 最后修改北京时间。 */
        LocalDateTime updatedAt,
        /* 乐观锁版本，防止并发覆盖。 */
        Long version
) {
}
