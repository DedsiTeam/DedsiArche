package com.dedsi.arche.system.domain.organizations;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** 组织机构节点聚合根；上级为独立聚合引用，不级联保存或删除整棵组织树。 */
@Entity
@Table(name = "sys_organizations", uniqueConstraints = @UniqueConstraint(
        name = "uk_organizations_system_code", columnNames = {"system_code", "code"}))
public class Organization {
    /* 节点标识，后端生成的 26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属系统编码，创建后不可修改。 */
    @Column(name = "system_code", length = 100, nullable = false, updatable = false)
    private String systemCode;

    /* 所属系统当前名称，由服务端目录读取并通过同事务事件同步。 */
    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;

    /* 直属上级组织标识；null 表示顶级，数据库复合外键保证父子同系统。 */
    @Column(name = "parent_id", length = 26)
    private String parentId;

    /* 所属系统内唯一的机构编码，区分大小写且创建后不可修改。 */
    @Column(length = 100, nullable = false, updatable = false)
    private String code;

    /* 组织主名称。 */
    @Column(length = 100, nullable = false)
    private String name;

    /* 可选名称一，如英文名或别名，不限定语言语义。 */
    @Column(name = "name1", length = 100)
    private String name1;

    /* 可选名称二，如简称，不限定语言语义。 */
    @Column(name = "name2", length = 100)
    private String name2;

    /* 可选名称三。 */
    @Column(name = "name3", length = 100)
    private String name3;

    /* 可选名称四。 */
    @Column(name = "name4", length = 100)
    private String name4;

    /* 同级展示顺序，非负整数，越小越靠前。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 职能说明或备注，可空，最多 500 字符。 */
    @Column(length = 500)
    private String description;

    /* 当前节点启用状态；不得隐式级联修改后代。 */
    @Column(nullable = false)
    private boolean enabled;

    /* 创建时间，北京时间。 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 修改时间，北京时间，只有节点实际变化才推进。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 节点乐观锁版本；移动、启停和系统名称同步均参与版本检查。 */
    @Version
    @Column(nullable = false)
    private Long version;

    /** 供 JPA 使用。 */
    protected Organization() {
    }

    /** 创建独立节点；调用方先在系统锁内验证上级归属及启用规则。 */
    public static Organization create(
            String systemCode, String systemName, String parentId, String code, String name,
            String name1, String name2, String name3, String name4,
            int sortOrder, String description, boolean enabled
    ) {
        var organization = new Organization();
        organization.systemCode = required(systemCode, "系统编码");
        organization.systemName = required(systemName, "系统名称");
        organization.code = required(code, "组织机构编码");
        organization.id = UlidCreator.getUlid().toString();
        organization.createdAt = now();
        organization.updatedAt = organization.createdAt;
        organization.update(parentId, name, name1, name2, name3, name4, sortOrder, description, enabled);
        organization.updatedAt = organization.createdAt;
        return organization;
    }

    /**
     * 整体更新当前节点；parentId 为 null 明确移为顶级，可选文本 null 或空白明确清除。
     * 调用方须在所属系统锁内先验证父链、防环及最终启停规则；本方法只校验节点自身不变量。
     */
    public void update(
            String parentId, String name, String name1, String name2, String name3, String name4,
            int sortOrder, String description, boolean enabled
    ) {
        String nextParentId = validatedParentId(parentId);
        String nextName = required(name, "组织主名称");
        String nextName1 = optional(name1, "组织名称一", 100);
        String nextName2 = optional(name2, "组织名称二", 100);
        String nextName3 = optional(name3, "组织名称三", 100);
        String nextName4 = optional(name4, "组织名称四", 100);
        String nextDescription = optional(description, "职能说明或备注", 500);
        if (sortOrder < 0) {
            throw invalid("同级排序必须是非负整数");
        }
        boolean changed = !Objects.equals(this.parentId, nextParentId)
                || !Objects.equals(this.name, nextName)
                || !Objects.equals(this.name1, nextName1)
                || !Objects.equals(this.name2, nextName2)
                || !Objects.equals(this.name3, nextName3)
                || !Objects.equals(this.name4, nextName4)
                || !Objects.equals(this.description, nextDescription)
                || this.sortOrder != sortOrder || this.enabled != enabled;
        this.parentId = nextParentId;
        this.name = nextName;
        this.name1 = nextName1;
        this.name2 = nextName2;
        this.name3 = nextName3;
        this.name4 = nextName4;
        this.sortOrder = sortOrder;
        this.description = nextDescription;
        this.enabled = enabled;
        if (changed) {
            touch();
        }
    }

    /** 系统更名事件只同步匹配系统；实际变化才推进当前节点版本。 */
    public void synchronizeSystemName(String systemCode, String name) {
        String normalizedCode = required(systemCode, "系统编码");
        String normalizedName = required(name, "系统名称");
        if (this.systemCode.equals(normalizedCode) && !systemName.equals(normalizedName)) {
            systemName = normalizedName;
            touch();
        }
    }

    /** 上级引用只能为空或合法 ULID；自身作为上级由当前节点直接拒绝。 */
    private String validatedParentId(String parentId) {
        if (parentId != null && (!parentId.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}") || parentId.equals(id))) {
            throw invalid("上级组织必须是有效 ULID，且不能是当前组织自身");
        }
        return parentId;
    }

    /** 微秒精度推进修改时间，保证实际变更使根实体变脏。 */
    private void touch() {
        LocalDateTime current = now();
        updatedAt = updatedAt != null && !current.isAfter(updatedAt) ? updatedAt.plusNanos(1000) : current;
    }

    /** 获取北京时间，保持与数据库 timestamp 精度一致。 */
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
    }

    /** 必填文本最多 100 字符，保留大小写，去除首尾空格。 */
    private static String required(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 100 || value.trim().isEmpty()) {
            throw invalid(label + "不能为空且最多 100 字符");
        }
        return value.trim();
    }

    /** 可选文本按字段上限验证，空白统一存储为 null。 */
    private static String optional(String value, String label, int maximum) {
        if (value != null && value.length() > maximum) {
            throw invalid(label + "最多 " + maximum + " 字符");
        }
        if (value == null || value.isBlank() || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /** 当前节点自身输入校验的统一错误码。 */
    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_ORGANIZATION", message);
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

    public String getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getName1() {
        return name1;
    }

    public String getName2() {
        return name2;
    }

    public String getName3() {
        return name3;
    }

    public String getName4() {
        return name4;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
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
