package com.dedsi.arche.system.domain.users;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

/** 用户岗位关联子实体；快照由用户聚合维护，岗位本身仍是独立聚合。 */
@Entity
@Table(name = "sys_user_positions", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_positions_position", columnNames = {"user_id", "position_id"}))
public class UserPosition {
    /* 关联标识，后端生成的 26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属用户，user_id 为 UUID 外键；不改变用户原有主键格式。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /* 被引用岗位的 ULID；数据库外键禁止删除仍被用户关联的岗位。 */
    @Column(name = "position_id", length = 26, nullable = false, updatable = false)
    private String positionId;

    /* 配置时的岗位编码快照；重新提交岗位集合时刷新。 */
    @Column(name = "position_code", length = 100, nullable = false)
    private String positionCode;

    /* 配置时的岗位名称快照；岗位单独改名不自动同步。 */
    @Column(name = "position_name", length = 100, nullable = false)
    private String positionName;

    /* 配置时岗位所属系统编码快照，历史未登记岗位允许为空。 */
    @Column(name = "system_code", length = 100)
    private String systemCode;

    /* 配置时岗位所属系统名称快照，与系统编码成对为空；系统改名不自动同步。 */
    @Column(name = "system_name", length = 100)
    private String systemName;

    /** 供 JPA 实例化，业务侧必须通过用户聚合创建。 */
    protected UserPosition() {
    }

    /** 创建关联标识并保存已校验的完整快照。 */
    static UserPosition create(User user, UserPositionSelection selection) {
        UserPosition item = new UserPosition();
        item.id = UlidCreator.getMonotonicUlid().toString();
        item.user = user;
        item.positionId = selection.positionId();
        item.refreshSnapshot(selection);
        return item;
    }

    /** 只刷新同一岗位的快照，返回是否实际变化以驱动根版本；保留关联标识。 */
    boolean refreshSnapshot(UserPositionSelection selection) {
        if (!positionId.equals(selection.positionId())) {
            throw new IllegalArgumentException("用户岗位关联不能改绑到其他岗位");
        }
        boolean changed = !Objects.equals(positionCode, selection.positionCode())
                || !Objects.equals(positionName, selection.positionName())
                || !Objects.equals(systemCode, selection.systemCode())
                || !Objects.equals(systemName, selection.systemName());
        positionCode = selection.positionCode();
        positionName = selection.positionName();
        systemCode = selection.systemCode();
        systemName = selection.systemName();
        return changed;
    }

    public String getId() {
        return id;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public String getPositionId() {
        return positionId;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public String getPositionName() {
        return positionName;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getSystemName() {
        return systemName;
    }
}
