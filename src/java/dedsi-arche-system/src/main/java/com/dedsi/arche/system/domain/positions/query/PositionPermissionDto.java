package com.dedsi.arche.system.domain.positions.query;

import com.dedsi.arche.system.domain.positions.PositionPermission;
/** 只读响应数据。 */
public record PositionPermissionDto(
        /* 子实体 ULID。 */
        String id,
        /* 所属岗位 ID。 */
        String positionId,
        /* 权限定义 ID。 */
        String permissionId,
        /* 系统编码快照。 */
        String systemCode,
        /* 所属系统当前名称，系统改名后由同事务事件同步。 */
        String systemName,
        /* 权限编码快照。 */
        String permissionCode,
        /* 权限当前名称，权限改名后由同事务事件同步。 */
        String permissionName
) {

    /** 将子实体转换为快照响应。 */
    public static PositionPermissionDto from(PositionPermission item) {
        return new PositionPermissionDto(
                item.getId(), item.getPositionId(), item.getPermissionId(),
                item.getSystemCode(), item.getSystemName(),
                item.getPermissionCode(), item.getPermissionName()
        );
    }
}
