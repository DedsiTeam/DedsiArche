package com.dedsi.arche.system.domain.positions;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.domain.permissions.Permission;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 岗位聚合根；权限快照随聚合维护，岗位编码创建后不可修改。 */
@Entity
@Table(name = "sys_positions", uniqueConstraints = @UniqueConstraint(
        name = "uk_positions_code", columnNames = "code"))
public class Position {
    /* 岗位标识，26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属系统编码；历史未登记数据可空，首次绑定后不可改变。 */
    @Column(name = "system_code", length = 100)
    private String systemCode;

    /* 所属系统当前名称，由系统目录读取并通过同事务事件同步；与系统编码同时为空或非空。 */
    @Column(name = "system_name", length = 100)
    private String systemName;

    /* 岗位编码，区分大小写且全局唯一。 */
    @Column(length = 100, nullable = false, updatable = false)
    private String code;

    /* 岗位名称。 */
    @Column(length = 100, nullable = false)
    private String name;

    /* 岗位职责说明，可空。 */
    @Column(length = 500)
    private String description;

    /* 是否启用；仅维护岗位状态，不改变现有鉴权。 */
    @Column(nullable = false)
    private boolean enabled;

    /* 非负展示顺序，越小越靠前。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 创建时间，北京时间。 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 修改时间，北京时间；子实体变化也推进，用于触发聚合版本检查。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 聚合乐观锁版本，同时保护基本信息和权限集合。 */
    @Version
    @Column(nullable = false)
    private Long version;

    /* 岗位权限快照；删除岗位或移除关联时删除对应子实体。 */
    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("systemCode ASC, permissionCode ASC, id ASC")
    private List<PositionPermission> permissions = new ArrayList<>();

    /** 供 JPA 使用。 */
    protected Position() {
    }

    /** 创建岗位并校验全部输入后生成权限快照。 */
    public static Position create(
            String systemCode, String systemName, String code, String name, String description, boolean enabled,
            int sortOrder, List<Permission> permissions
    ) {
        Position position = new Position();
        position.id = UlidCreator.getMonotonicUlid().toString();
        position.code = required(code, "岗位编码");
        position.systemCode = required(systemCode, "所属系统编码");
        position.systemName = required(systemName, "所属系统名称");
        position.createdAt = now();
        position.update(name, description, enabled, sortOrder, permissions);
        position.updatedAt = position.createdAt;
        return position;
    }

    /**
     * 完整更新基本信息；权限为 null 时保持原集合，空集合表示清空。
     * 先校验所有输入，避免失败操作留下部分变更。
     */
    public void update(
            String name, String description, boolean enabled,
            int sortOrder, List<Permission> nextPermissions
    ) {
        update(name, description, enabled, sortOrder, nextPermissions, null, null);
    }

    /** 兼容历史未登记岗位首次绑定系统；已绑定岗位只允许同编码，所有校验在状态赋值前完成。 */
    public void update(
            String name, String description, boolean enabled, int sortOrder,
            List<Permission> nextPermissions, String requestedSystemCode, String requestedSystemName
    ) {
        String nextSystemCode = systemCode;
        String nextSystemName = systemName;
        if (requestedSystemCode != null) {
            nextSystemCode = required(requestedSystemCode, "所属系统编码");
            nextSystemName = required(requestedSystemName, "所属系统名称");
            if (systemCode != null && !systemCode.equals(nextSystemCode)) {
                throw new BusinessException("POSITION_SYSTEM_IMMUTABLE", "岗位所属系统登记后不可修改");
            }
        }
        String nextName = required(name, "岗位名称");
        if (description != null && description.length() > 500) {
            throw invalid("岗位说明最多 500 字符");
        }
        if (sortOrder < 0) {
            throw invalid("排序值不能小于零");
        }
        if (nextPermissions != null) {
            Set<String> ids = new HashSet<>();
            for (Permission permission : nextPermissions) {
                if (permission == null || permission.getId() == null) {
                    throw invalid("权限不能为空");
                }
                if (!ids.add(permission.getId())) {
                    throw new BusinessException("DUPLICATE_POSITION_PERMISSION", "岗位权限不能重复");
                }
            }
        }
        this.systemCode = nextSystemCode;
        this.systemName = nextSystemName;
        this.name = nextName;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        if (nextPermissions != null) {
            Map<String, PositionPermission> existing = new HashMap<>();
            for (PositionPermission item : permissions) {
                existing.put(item.getPermissionId(), item);
            }
            Set<String> retained = new HashSet<>();
            for (Permission permission : nextPermissions) {
                retained.add(permission.getId());
                PositionPermission item = existing.get(permission.getId());
                if (item == null) {
                    permissions.add(PositionPermission.create(this, permission));
                } else {
                    item.refreshSnapshot(permission);
                }
            }
            permissions.removeIf(item -> !retained.contains(item.getPermissionId()));
        }
        touch();
    }

    /**
     * 同步已引用权限的当前名称，仅改匹配子实体的权限名称；无引用或同名时保持聚合版本和时间。
     * 版本由 JPA 在持久化根实体修改时递增，重复处理相同名称不产生额外修改。
     */
    public void synchronizePermissionName(String permissionId, String name) {
        if (permissionId == null || !permissionId.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid("权限 ID 必须是大写 ULID");
        }
        String nextName = required(name, "权限名称");
        boolean changed = false;
        for (PositionPermission item : permissions) {
            if (item.getPermissionId().equals(permissionId)) {
                changed |= item.synchronizePermissionName(nextName);
            }
        }
        if (changed) {
            touch();
        }
    }

    /** 系统名称变化一次更新根和全部匹配子项，只推进一次根时间；不改变系统归属或其他名称。 */
    public void synchronizeSystemName(String code, String name) {
        String normalizedCode = required(code, "系统编码");
        String nextName = required(name, "系统名称");
        boolean changed = false;
        if (normalizedCode.equals(systemCode) && !nextName.equals(systemName)) {
            this.systemName = nextName;
            changed = true;
        }
        for (PositionPermission item : permissions) {
            if (normalizedCode.equals(item.getSystemCode())) {
                changed |= item.synchronizeSystemName(nextName);
            }
        }
        if (changed) {
            touch();
        }
    }

    /** 推进根实体修改时间，使子实体变化参与根乐观锁检查。 */
    private void touch() {
        LocalDateTime current = now();
        // 与数据库微秒精度一致，即使同一时刻修改也保证根实体变脏。
        this.updatedAt = updatedAt != null && !current.isAfter(updatedAt)
                ? updatedAt.plusNanos(1000) : current;
    }

    /** 使用北京时间并对齐 PostgreSQL 微秒精度。 */
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
    }

    /** 校验必填文本，编码和名称上限均为 100 字符。 */
    private static String required(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw invalid(label + "不能为空且最多 100 字符");
        }
        return value.trim();
    }

    /** 统一领域输入错误码。 */
    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_POSITION", message);
    }

    public String getId() {
        return id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    /** 只读视图，调用方不能绕过聚合增删子实体。 */
    public List<PositionPermission> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }
}
