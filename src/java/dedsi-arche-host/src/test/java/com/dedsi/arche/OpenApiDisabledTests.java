package com.dedsi.arche;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 禁用文档时，即使已认证也不能访问文档端点。
 */
@SpringBootTest(
        properties = {
            "app.docs.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:docs_off;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDisabledTests {

    /* 通过 Spring MVC 测试入口发送请求并断言响应。 */
    @Autowired
    MockMvc mvc;

    /**
     * 验证关闭文档后，已认证请求也无法访问文档端点。
     */
    @Test
    void disabledDocsAreInaccessible() throws Exception {
        mvc.perform(get("/scalar").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs").with(jwt())).andExpect(status().isForbidden());
    }
}
