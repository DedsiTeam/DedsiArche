package com.dedsi.arche.system.infrastructure.loginaudits.security;

import com.dedsi.arche.system.api.users.UserController;
import com.dedsi.arche.system.api.users.request.LoginRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import java.lang.reflect.Type;

/** 在 DTO 校验前提取账号；JSON 解码失败时不读取、重放或缓存原始正文。 */
@ControllerAdvice(assignableTypes = UserController.class)
public class LoginAuditBodyAdvice extends RequestBodyAdviceAdapter {
    @Override
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return parameter.getMethod() != null && parameter.getMethod().getName().equals("login")
                && parameter.getParameterType() == LoginRequest.class;
    }

    @Override
    public Object afterBodyRead(
            Object body, HttpInputMessage input, MethodParameter parameter,
            Type type, Class<? extends HttpMessageConverter<?>> converter
    ) {
        if (body instanceof LoginRequest login && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                && attrs.getRequest().getAttribute(LoginAuditContext.ATTRIBUTE) instanceof LoginAuditContext context) {
            context.captureAccount(login.username());
        }
        return body;
    }
}
