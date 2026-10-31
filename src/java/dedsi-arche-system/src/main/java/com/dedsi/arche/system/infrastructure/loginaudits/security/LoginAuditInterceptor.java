package com.dedsi.arche.system.infrastructure.loginaudits.security;

import com.dedsi.arche.system.api.users.UserController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 参数解析前建立上下文，事务代理结束后统一记录失败；不记录原始异常或请求内容。 */
@Component
public class LoginAuditInterceptor implements HandlerInterceptor {
    /* 日志仅输出服务端关联标识。 */
    private static final Logger LOG = LoggerFactory.getLogger(LoginAuditInterceptor.class);
    /* 独立事务写入代理。 */
    private final LoginAuditWriter writer;

    public LoginAuditInterceptor(LoginAuditWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equals(request.getMethod()) && handler instanceof HandlerMethod method
                && UserController.class.isAssignableFrom(method.getBeanType())
                && method.getMethod().getName().equals("login")) {
            request.setAttribute(LoginAuditContext.ATTRIBUTE, new LoginAuditContext(request));
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception
    ) {
        if (!(request.getAttribute(LoginAuditContext.ATTRIBUTE) instanceof LoginAuditContext context)) {
            return;
        }
        try {
            if (context.finish() && !context.isCommitted()) {
                try {
                    writer.failure(context, context.failureReason(response.getStatus()));
                } catch (RuntimeException ignored) {
                    // 捕获包含事务提交阶段的代理异常；不打印异常、账号、请求头、SQL 或认证材料。
                    LOG.warn("登录失败审计写入未完成，traceId={}", context.traceId());
                }
            }
        } finally {
            request.removeAttribute(LoginAuditContext.ATTRIBUTE);
        }
    }
}
