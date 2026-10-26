package com.dedsi.arche.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Host 汇总文档元信息和 Bearer 认证定义，业务模块维护接口说明。 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenAPI openApi(@Value("${spring.application.name}") String name) {
        return new OpenAPI()
            .info(new Info().title(name + " API").version("v1")
                .description("始于本原，成于至简。先调用登录接口取得 accessToken，再在 Scalar 的 Bearer 认证中填入令牌。"))
            .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("填入 accessToken，界面自动添加 Bearer 前缀。")));
    }
}
