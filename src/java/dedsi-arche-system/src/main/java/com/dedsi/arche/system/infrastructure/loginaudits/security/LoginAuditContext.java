package com.dedsi.arche.system.infrastructure.loginaudits.security;

import com.dedsi.arche.system.domain.loginaudits.*;
import com.dedsi.arche.system.domain.users.User;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** 单次登录的受控元数据；只附着当前 Servlet 请求，不使用线程全局或保存请求正文。 */
public final class LoginAuditContext {
    /* 内部请求属性名。 */
    static final String ATTRIBUTE = LoginAuditContext.class.getName();
    /* 服务端生成的尝试标识，同时作为审计主键和日志关联标识。 */
    private final String traceId = UlidCreator.getUlid().toString();
    /* 进入登录处理时的北京时间。 */
    private final LocalDateTime loginTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    /* 连接来源，不读取 X-Forwarded-For。 */
    private final String clientIp;
    /* 有限长度的客户端标识。 */
    private final String userAgent;
    /* 账号快照；从已解析 DTO 提取，不保存 DTO。 */
    private String loginAccount;
    /* 身份确认后才记录的逻辑用户标识。 */
    private UUID userId;
    /* 身份确认后的历史名称。 */
    private String displayName;
    /* 默认视为服务异常，仅显式认证分支设置可预期失败原因。 */
    private LoginAuditReason failureReason = LoginAuditReason.SERVICE_ERROR;
    /* afterCommit 标记，响应写出失败不能将已提交认证改写成失败。 */
    private boolean committed;
    /* 防止重复完成回调。 */
    private boolean completed;

    LoginAuditContext(HttpServletRequest request) {
        clientIp = LoginAudit.sanitize(request.getRemoteAddr(), 64);
        userAgent = LoginAudit.sanitize(request.getHeader("User-Agent"), 512);
    }

    /** 缺少拦截器上下文时拒绝登录，不能绕过审计。 */
    public static LoginAuditContext require(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof LoginAuditContext context) {
            return context;
        }
        throw new IllegalStateException("登录审计上下文缺失");
    }

    /** 已解析账号有限快照；不持有密码或完整请求对象。 */
    void captureAccount(String account) {
        loginAccount = LoginAudit.sanitize(account, 200);
    }

    /** 保留现有登录外部错误，同时标记内部受控原因。 */
    public void failed(LoginAuditReason reason) {
        if (reason != LoginAuditReason.BAD_CREDENTIALS && reason != LoginAuditReason.USER_DISABLED) {
            throw new IllegalArgumentException("非法登录失败分支");
        }
        failureReason = reason;
    }

    /** 仅在密码确认后绑定用户，不在错误密码审计中泄露账号存在性。 */
    public void identify(User user) {
        userId = user.getId();
        displayName = LoginAudit.sanitize(user.getDisplayName(), 100);
    }

    LoginAudit fact(LoginAuditReason reason) {
        return LoginAudit.recordWithId(traceId, loginTime, reason, loginAccount, userId,
                displayName, clientIp, userAgent, traceId);
    }

    LoginAuditReason failureReason(int status) {
        return failureReason != LoginAuditReason.SERVICE_ERROR ? failureReason
                : (status == 400 || status == 415) ? LoginAuditReason.INVALID_REQUEST : LoginAuditReason.SERVICE_ERROR;
    }

    void committed() {
        committed = true;
    }

    boolean isCommitted() {
        return committed;
    }
    boolean finish() {
        if (completed) {
            return false;
        }
        completed = true;
        return true;
    }
    String traceId() {
        return traceId;
    }
}
