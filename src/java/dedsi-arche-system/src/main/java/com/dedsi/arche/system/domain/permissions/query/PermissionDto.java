package com.dedsi.arche.system.domain.permissions.query;

import java.time.LocalDateTime;
import com.dedsi.arche.system.domain.permissions.Permission;

/**
 * 权限只读数据，供详情和分页接口返回。
 */
public record PermissionDto(
        /* 权限标识，由后端生成的 26 位 ULID。 */
        String id,
        /* 所属系统编码，与权限编码共同构成唯一键，创建后不可修改。 */
        String systemCode,
        /* 所属系统的展示名称。 */
        String systemName,
        /* 权限编码，在同一系统内唯一，创建后不可修改。 */
        String code,
        /* 权限展示名称。 */
        String name,
        /* 权限说明，允许为空。 */
        String description,
        /* 权限定义是否启用；当前不承担用户授权判定。 */
        boolean enabled,
        /* 展示顺序，非负数，值越小越靠前。 */
        int sortOrder,
        /* 创建时间，使用 北京时间。 */
        LocalDateTime createdAt,
        /* 最后修改时间，使用 北京时间。 */
        LocalDateTime updatedAt,
        /* 乐观锁版本，更新和删除时用于防止并发覆盖。 */
        Long version
) {

    /**
     * 将权限聚合转换为只读响应数据。
     */
    public static PermissionDto from(Permission p) {
        return new PermissionDto(p.getId(),
                p.getSystemCode(),
                p.getSystemName(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.isEnabled(),
                p.getSortOrder(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getVersion());
    }
}
