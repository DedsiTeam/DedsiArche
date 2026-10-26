package com.dedsi.arche;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
/** 验证文档契约与实际认证边界保持一致。 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class OpenApiTests {
    @Autowired MockMvc mvc;
    @Test void scalarAndApiContractAreAvailable() throws Exception {
        mvc.perform(get("/scalar")).andExpect(status().isOk())
            .andExpect(content().string(containsString("scalar")));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.paths['/api/users/me'].get.security[0].bearerAuth").isArray())
            .andExpect(jsonPath("$.paths['/api/users/login'].post.security").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/users/me'].get.parameters").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.UserResponse.properties.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/users/login'].post.summary").value("账号密码登录"));
        mvc.perform(get("/v3/api-docs/system")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/users/register']").exists());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
