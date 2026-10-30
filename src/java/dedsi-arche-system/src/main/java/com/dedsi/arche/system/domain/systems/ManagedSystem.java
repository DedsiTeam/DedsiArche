package com.dedsi.arche.system.domain.systems;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 系统聚合根，维护入口、状态和展示资料；不承担用户授权职责。 */
@Entity
@Table(name = "sys_systems", uniqueConstraints = @UniqueConstraint(
        name = "uk_systems_code", columnNames = "code"
))
public class ManagedSystem {

    /* 26 位 ULID 主键。 */
    @Id
    @Column(name = "id", nullable = false, length = 26)
    private String id;

    /* 系统编码，全局唯一且创建后不可修改。 */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /* 系统名称，与权限系统名称长度保持一致。 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /* 系统说明，可为空。 */
    @Column(name = "description", nullable = true, length = 512)
    private String description;

    /* HTTP/HTTPS 入口地址，可为空，不承担 SSO 回调配置。 */
    @Column(name = "entry_url", nullable = true, length = 2048)
    private String entryUrl;

    /* 人工维护的业务状态，默认未上线。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SystemStatus status;

    /* 展示排序，非负且数值越小越靠前。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 创建北京时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /* 最后修改北京时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 乐观锁版本，防止并发覆盖。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** 供 JPA 创建实例。 */
    protected ManagedSystem() {
    }

    /** 创建系统并生成 ULID；首次更新时间与创建时间一致。 */
    public static ManagedSystem create(
            String code,
            String name,
            String description,
            String entryUrl,
            SystemStatus status,
            int sortOrder
    ) {
        var system = new ManagedSystem();
        system.code = required(code, "系统编码");
        system.update(name, description, entryUrl, status, sortOrder);
        system.id = UlidCreator.getMonotonicUlid().toString();
        system.createdAt = system.updatedAt;
        return system;
    }

    /** 全部校验成功后才修改状态，失败操作不能留下部分更新。 */
    public void update(
            String name,
            String description,
            String entryUrl,
            SystemStatus status,
            int sortOrder
    ) {
        String nextName = required(name, "系统名称");
        String nextDescription = optional(description, 512, "系统说明");
        String nextUrl = validateEntryUrl(entryUrl);
        if (status == null) {
            throw new BusinessException("INVALID_SYSTEM_STATUS", "系统状态不能为空");
        }
        if (sortOrder < 0) {
            throw new BusinessException("INVALID_SYSTEM_SORT", "排序值不能小于零");
        }
        if (status == SystemStatus.NORMAL && nextUrl == null) {
            throw new BusinessException("SYSTEM_ENTRY_REQUIRED", "正常状态必须配置入口地址");
        }
        this.name = nextName;
        this.description = nextDescription;
        this.entryUrl = nextUrl;
        this.status = status;
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    /** 必填文本去除首尾空白，最长 100 字符。 */
    private static String required(String value, String label) {
        String result = optional(value, 100, label);
        if (result == null) {
            throw new BusinessException("INVALID_SYSTEM_TEXT", label + "不能为空");
        }
        return result;
    }

    /** 选填文本统一将空白存为空值。 */
    private static String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.trim();
        if (result.length() > max) {
            throw new BusinessException("INVALID_SYSTEM_TEXT", label + "最多 " + max + " 字符");
        }
        return result;
    }

    /** 仅接受绝对 HTTP/HTTPS 地址；允许尾斜杠，禁止内嵌账号密码。 */
    private static String validateEntryUrl(String value) {
        String result = optional(value, 2048, "入口地址");
        if (result == null) {
            return null;
        }
        try {
            URI uri = new URI(result);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535))) {
                return result;
            }
        } catch (URISyntaxException ignored) {
            // 将解析失败统一映射为业务错误，不暴露底层异常。
        }
        throw new BusinessException("INVALID_SYSTEM_ENTRY_URL", "入口地址必须为不含账号密码的 HTTP/HTTPS 地址");
    }

    /** 26 位 ULID 主键。 */
    public String getId() {
        return id;
    }

    /** 系统编码，全局唯一且创建后不可修改。 */
    public String getCode() {
        return code;
    }

    /** 系统名称，与权限系统名称长度保持一致。 */
    public String getName() {
        return name;
    }

    /** 系统说明，可为空。 */
    public String getDescription() {
        return description;
    }

    /** HTTP/HTTPS 入口地址，可为空，不承担 SSO 回调配置。 */
    public String getEntryUrl() {
        return entryUrl;
    }

    /** 人工维护的业务状态，默认未上线。 */
    public SystemStatus getStatus() {
        return status;
    }

    /** 展示排序，非负且数值越小越靠前。 */
    public int getSortOrder() {
        return sortOrder;
    }

    /** 创建北京时间。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 最后修改北京时间。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 乐观锁版本，防止并发覆盖。 */
    public Long getVersion() {
        return version;
    }
}
