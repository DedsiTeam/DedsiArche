package com.dedsi.arche.system.domain.loginaudits;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** 不可变登录审计事实，无修改、删除、版本或用户级联关联。 */
@Entity
@Table(name = "sys_login_audits")
public class LoginAudit {
    /* 审计 ULID 标识。 */
    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id;

    /* 北京时间登录尝试时间。 */
    @Column(name = "login_time", nullable = false, updatable = false)
    private LocalDateTime loginTime;

    /* 登录结果。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10, nullable = false, updatable = false)
    private LoginAuditResult result;

    /* 受控原因编码。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 30, nullable = false, updatable = false)
    private LoginAuditReason reasonCode;

    /* 登录账号历史快照，可空。 */
    @Column(name = "login_account", length = 200, updatable = false)
    private String loginAccount;

    /* 已确认身份的用户逻辑标识，无外键级联。 */
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    /* 用户显示名称历史快照，可空。 */
    @Column(name = "display_name", length = 100, updatable = false)
    private String displayName;

    /* 连接来源 IP，不信任转发头。 */
    @Column(name = "client_ip", length = 64, updatable = false)
    private String clientIp;

    /* 截断清洗后的客户端标识。 */
    @Column(name = "user_agent", length = 512, updatable = false)
    private String userAgent;

    /* 由原因枚举产生的受控失败文案。 */
    @Column(name = "failure_message", length = 200, updatable = false)
    private String failureMessage;

    /* 服务端关联标识，不采信客户端头。 */
    @Column(name = "trace_id", length = 64, updatable = false)
    private String traceId;

    /** 仅供持久化框架恢复事实。 */
    protected LoginAudit() {
    }

    /** 由受控原因导出结果和文案，原始请求与异常文本从不进入审计。 */
    public static LoginAudit record(
            LocalDateTime loginTime, LoginAuditReason reason, String loginAccount, UUID userId,
            String displayName, String clientIp, String userAgent, String traceId
    ) {
        return recordWithId(UlidCreator.getUlid().toString(), loginTime, reason, loginAccount,
                userId, displayName, clientIp, userAgent, traceId);
    }

    /** 一次尝试共用标识，提交结果不明时数据库主键防止重复成功/失败事实。 */
    public static LoginAudit recordWithId(
            String auditId, LocalDateTime loginTime, LoginAuditReason reason, String loginAccount, UUID userId,
            String displayName, String clientIp, String userAgent, String traceId
    ) {
        if (auditId == null || !auditId.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new IllegalArgumentException("审计 ID 必须是大写 ULID");
        }
        var audit = new LoginAudit();
        audit.id = auditId;
        audit.loginTime = Objects.requireNonNull(loginTime, "登录时间不能为空");
        audit.reasonCode = Objects.requireNonNull(reason, "审计原因不能为空");
        audit.result = reason == LoginAuditReason.LOGIN_SUCCEEDED ? LoginAuditResult.SUCCESS : LoginAuditResult.FAILURE;
        if (audit.result == LoginAuditResult.SUCCESS && userId == null) {
            throw new IllegalArgumentException("成功审计必须包含已确认用户");
        }
        audit.loginAccount = sanitize(loginAccount, 200);
        audit.userId = userId;
        audit.displayName = sanitize(displayName, 100);
        audit.clientIp = sanitize(clientIp, 64);
        audit.userAgent = sanitize(userAgent, 512);
        audit.traceId = sanitize(traceId, 64);
        audit.failureMessage = switch (reason) {
            case LOGIN_SUCCEEDED -> null;
            case BAD_CREDENTIALS -> "账号或密码错误";
            case USER_DISABLED -> "用户已停用";
            case INVALID_REQUEST -> "登录请求格式或参数无效";
            case SERVICE_ERROR -> "登录服务暂时不可用";
        };
        return audit;
    }

    /** 只保留有限长度可显示字符，移除控制符与孤立代理字符，不保留完整恶意输入。 */
    public static String sanitize(String value, int maximum) {
        if (value == null) {
            return null;
        }
        var cleaned = new StringBuilder();
        value.codePoints().filter(code -> !Character.isISOControl(code)
                && Character.getType(code) != Character.FORMAT && Character.getType(code) != Character.SURROGATE)
                .limit(maximum).forEach(cleaned::appendCodePoint);
        var text = cleaned.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /** 返回id只读快照。 */
    public String getId() {
        return id;
    }

    /** 返回loginTime只读快照。 */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /** 返回result只读快照。 */
    public LoginAuditResult getResult() {
        return result;
    }

    /** 返回reasonCode只读快照。 */
    public LoginAuditReason getReasonCode() {
        return reasonCode;
    }

    /** 返回loginAccount只读快照。 */
    public String getLoginAccount() {
        return loginAccount;
    }

    /** 返回userId只读快照。 */
    public UUID getUserId() {
        return userId;
    }

    /** 返回displayName只读快照。 */
    public String getDisplayName() {
        return displayName;
    }

    /** 返回clientIp只读快照。 */
    public String getClientIp() {
        return clientIp;
    }

    /** 返回userAgent只读快照。 */
    public String getUserAgent() {
        return userAgent;
    }

    /** 返回failureMessage只读快照。 */
    public String getFailureMessage() {
        return failureMessage;
    }

    /** 返回traceId只读快照。 */
    public String getTraceId() {
        return traceId;
    }
}
