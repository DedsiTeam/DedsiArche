package com.dedsi.arche.system.domain.positions;

import com.dedsi.arche.system.domain.permissions.Permission;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;

/** 岗位权限子实体；快照仅由岗位聚合根据真实权限定义维护。 */
@Entity
@Table(name = "sys_position_permissions", uniqueConstraints = @UniqueConstraint(
        name = "uk_position_permissions_permission", columnNames = {"position_id", "permission_id"}))
public class PositionPermission {
    /* 子实体标识，26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属岗位，对应数据库外键 position_id。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false, updatable = false)
    private Position position;

    /* 权限定义标识，对应数据库外键；已有岗位引用时禁止删除权限。 */
    @Column(name = "permission_id", length = 26, nullable = false, updatable = false)
    private String permissionId;

    /* 关联时的系统编码快照。 */
    @Column(name = "system_code", length = 100, nullable = false)
    private String systemCode;

    /* 所属系统当前名称，由系统改名事件同事务同步。 */
    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;

    /* 关联时的权限编码快照。 */
    @Column(name = "permission_code", length = 100, nullable = false)
    private String permissionCode;

    /* 当前权限名称；权限改名通过同步领域事件更新，其他快照字段不受影响。 */
    @Column(name = "permission_name", length = 100, nullable = false)
    private String permissionName;

    /** 供 JPA 使用。 */
    protected PositionPermission() {
    }

    /** 仅允许同包聚合创建关联。 */
    static PositionPermission create(Position position, Permission permission) {
        PositionPermission item = new PositionPermission();
        item.id = UlidCreator.getMonotonicUlid().toString();
        item.position = position;
        item.permissionId = permission.getId();
        item.refreshSnapshot(permission);
        return item;
    }

    /** 重新配置权限时刷新快照，保留关联标识。 */
    void refreshSnapshot(Permission permission) {
        this.systemCode = permission.getSystemCode();
        this.systemName = permission.getSystemName();
        this.permissionCode = permission.getCode();
        this.permissionName = permission.getName();
    }

    /** 仅同步所属系统名称，重复名称不修改实体；只能通过岗位聚合调用。 */
    boolean synchronizeSystemName(String name) {
        if (systemName.equals(name)) {
            return false;
        }
        this.systemName = name;
        return true;
    }

    /** 仅同步权限名称，重复名称不修改实体；只能通过岗位聚合调用。 */
    boolean synchronizePermissionName(String name) {
        if (permissionName.equals(name)) {
            return false;
        }
        this.permissionName = name;
        return true;
    }

    public String getId() {
        return id;
    }

    public String getPositionId() {
        return position.getId();
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }
}
