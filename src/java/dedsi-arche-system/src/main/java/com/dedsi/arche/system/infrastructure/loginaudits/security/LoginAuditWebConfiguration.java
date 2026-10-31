package com.dedsi.arche.system.infrastructure.loginaudits.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 仅为登录路径注册审计，拦截器还核对实际 handler 和 POST 方法。 */
@Configuration
public class LoginAuditWebConfiguration implements WebMvcConfigurer {
    private final LoginAuditInterceptor interceptor;

    public LoginAuditWebConfiguration(LoginAuditInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/users/login");
    }
}
