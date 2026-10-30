package com.dedsi.arche.system.domain.users;

import com.dedsi.arche.common.exception.BusinessException;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

/**
 * 用户聚合根。API 不直接序列化此实体，避免泄漏密码哈希。
 */
@Entity
@Table(
        name = "sys_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username"))
public class User {

    /* 记录的唯一标识。 */
    @Id
    private UUID id;

    /* 规范化为小写的账号。 */
    @Column(nullable = false, length = 50)
    private String username;

    /* 向调用方展示的账号称。 */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 16)
    private String phone;

    @Column(length = 254)
    private String email;

    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    /** 空串清除联系方式；格式约束在所有写入入口统一执行。 */
    public void updateContact(String phone, String email) {
        String nextPhone = phone == null || phone.isBlank() ? null : phone.trim();
        String nextEmail = email == null || email.isBlank() ? null : email.trim();
        if (nextPhone != null && !nextPhone.matches("[+]?[0-9]{7,15}"))
            throw new BusinessException("INVALID_PHONE", "手机号需为 7～15 位数字，可使用 + 国际区号");
        if (nextEmail != null && (nextEmail.length() > 254 || !nextEmail.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+")))
            throw new BusinessException("INVALID_EMAIL", "请输入有效邮箱，最多 254 个字符");
        this.phone = nextPhone;
        this.email = nextEmail;
    }

    /* BCrypt 密码哈希，不存储明文密码。 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    public long getCredentialVersion() { return credentialVersion; }
    public void resetPassword(String hash) { passwordHash = hash; credentialVersion++; }

    /* 用户是否允许登录和修改资料。 */
    @Column(nullable = false)
    private boolean enabled;

    /* 用户注册时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /* 用于检测并发更新的乐观锁版本。 */
    @Version
    private Long version;

    /**
     * 供 JPA 实例化实体使用。
     */
    protected User() {
    }

    /**
     * 创建已启用的用户聚合，规范化账号并记录注册时间。
     */
    public static User register(
            String username,
            String displayName,
            String passwordHash
    ) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = normalizeUsername(username);
        user.rename(displayName);
        user.passwordHash = passwordHash;
        user.enabled = true;
        user.createdAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return user;
    }

    /**
     * 去除账号首尾空白，并使用固定语言规则转换为小写。
     */
    public static String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验姓名并去除首尾空白后更新资料。
     */
    public void rename(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new BusinessException("INVALID_DISPLAY_NAME", "姓名不能为空且最多 100 个字符");
        }
        displayName = name.trim();
    }

    /**
     * 检查用户是否启用，停用时抛出业务异常。
     */
    public void ensureEnabled() {
        if (!enabled) {
            throw new BusinessException("USER_DISABLED", "用户已停用");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }

    /**
     * 返回用户唯一标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 返回规范化后的账号。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 返回用户姓名。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 返回用于密码校验的哈希值，不应向外部接口暴露。
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 返回用户注册时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 返回用于乐观锁并发控制的实体版本。
     */
    public Long getVersion() {
        return version;
    }
}
