package com.dedsi.arche.system.infrastructure.users.docs;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 当前 System 模块的接口文档分组；后续业务模块可声明自己的分组。
 */
@Configuration(proxyBeanMethods = false)
public class UserOpenApiConfiguration {

    /**
     * 注册 System 模块接口文档分组，限定业务接口扫描范围。
     */
    @Bean
    GroupedOpenApi systemOpenApi() {
        return GroupedOpenApi.builder()
                .group("system")
                .displayName("系统管理")
                .packagesToScan("com.dedsi.arche.system.api")
                .pathsToMatch("/api/**")
                .build();
    }
}
