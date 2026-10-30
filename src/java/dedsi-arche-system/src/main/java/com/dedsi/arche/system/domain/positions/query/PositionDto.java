package com.dedsi.arche.system.domain.positions.query;

import java.time.LocalDateTime;
import java.util.List;
import com.dedsi.arche.system.domain.positions.Position;
/** 只读响应数据。 */
public record PositionDto(
        /* 岗位 ULID。 */
        String id,
        /* 所属系统编码，历史未登记岗位为空。 */
        String systemCode,
        /* 所属系统当前名称，历史未登记岗位为空。 */
        String systemName,
        /* 不可变岗位编码。 */
        String code,
        /* 岗位名称。 */
        String name,
        /* 岗位说明，可空。 */
        String description,
        /* 是否启用。 */
        boolean enabled,
        /* 非负排序值。 */
        int sortOrder,
        /* 创建时间，北京时间。 */
        LocalDateTime createdAt,
        /* 修改时间，北京时间。 */
        LocalDateTime updatedAt,
        /* 聚合乐观锁版本。 */
        Long version,
        /* 岗位权限快照集合。 */
        List<PositionPermissionDto> permissions
) {

    /** 在事务内读取权限集合，避免直接序列化实体。 */
    public static PositionDto from(Position position) {
        return new PositionDto(
                position.getId(), position.getSystemCode(), position.getSystemName(),
                position.getCode(), position.getName(),
                position.getDescription(), position.isEnabled(), position.getSortOrder(),
                position.getCreatedAt(), position.getUpdatedAt(), position.getVersion(),
                position.getPermissions().stream().map(PositionPermissionDto::from).toList()
        );
    }
}
