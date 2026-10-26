package com.dedsi.arche.system.domain.users;
import com.dedsi.arche.common.exception.BusinessException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
/** 用户聚合根。API 不直接序列化此实体，避免泄漏密码哈希。 */
@Entity
@Table(name="sys_users", uniqueConstraints=@UniqueConstraint(name="uk_users_username", columnNames="username"))
public class User {
    @Id private UUID id;
    @Column(nullable=false, length=50) private String username;
    @Column(name="display_name", nullable=false, length=100) private String displayName;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(nullable=false) private boolean enabled;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Version private Long version;
    protected User() {}
    public static User register(String username, String displayName, String passwordHash) {
        User user = new User(); user.id=UUID.randomUUID();
        user.username=normalizeUsername(username); user.rename(displayName);
        user.passwordHash=passwordHash; user.enabled=true; user.createdAt=Instant.now();
        return user;
    }
    public static String normalizeUsername(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    public void rename(String name) {
        if (name == null || name.isBlank() || name.trim().length()>100)
            throw new BusinessException("INVALID_DISPLAY_NAME", "显示名称不能为空且最多 100 个字符");
        displayName=name.trim();
    }
    public void ensureEnabled() {
        if (!enabled) throw new BusinessException("USER_DISABLED", "用户已停用");
    }
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
