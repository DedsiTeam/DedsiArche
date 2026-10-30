package com.dedsi.arche.system.domain.permissions;

import com.github.f4b6a3.ulid.UlidCreator;
import com.dedsi.arche.common.exception.BusinessException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 权限定义聚合；系统编码和权限编码创建后不可变。 */
@Entity
@Table(
        name = "sys_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permissions_system_code",
                columnNames = {"system_code", "code"}
        )
)
public class Permission {

    /* 权限标识，由后端生成的 26 位 ULID。 */
    @Id
    @Column(name = "id", nullable = false, length = 26)
    private String id;

    /* 所属系统编码，与权限编码共同构成唯一键，创建后不可修改。 */
    @Column(name = "system_code", nullable = false, length = 100)
    private String systemCode;

    /* 所属系统的展示名称。 */
    @Column(name = "system_name", nullable = false, length = 100)
    private String systemName;

    /* 权限编码，在同一系统内唯一，创建后不可修改。 */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /* 权限展示名称。 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /* 权限说明，允许为空。 */
    @Column(name = "description", nullable = true, length = 500)
    private String description;

    /* 权限定义是否启用；当前不承担用户授权判定。 */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /* 展示顺序，非负数，值越小越靠前。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 创建时间，使用 北京时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /* 最后修改时间，使用 北京时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 乐观锁版本，更新和删除时用于防止并发覆盖。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 供 JPA 实例化实体使用。
     */
    protected Permission() {
    }

    /**
     * 创建权限定义，生成 ULID 并初始化审计时间。
     */
    public static Permission create(
            String systemCode,
            String systemName,
            String code,
            String name,
            String description,
            boolean enabled,
            int sortOrder
    ) {
        var permission = new Permission();
        permission.id = UlidCreator.getMonotonicUlid().toString();
        permission.systemCode = required(systemCode, "系统编码", 100);
        permission.code = required(code, "权限编码", 100);
        permission.createdAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        permission.update(systemName, name, description, enabled, sortOrder);
        permission.updatedAt = permission.createdAt;
        return permission;
    }

    /**
     * 更新允许编辑的字段，保留系统编码和权限编码。
     */
    public void update(String systemName, String name, String description, boolean enabled, int sortOrder) {
        this.systemName = required(systemName, "系统名称", 100);
        this.name = required(name, "权限名称", 100);
        if (description != null && description.length() > 500) {
            throw new BusinessException("INVALID_PERMISSION", "权限说明最多 500 字符");
        }
        if (sortOrder < 0) {
            throw new BusinessException("INVALID_PERMISSION", "排序值不能小于零");
        }
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 校验必填文本及长度，并去除首尾空白。
     */
    private static String required(String value, String label, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new BusinessException("INVALID_PERMISSION", label + "不能为空且最多 " + max + " 字符");
        }
        return value.trim();
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
}
