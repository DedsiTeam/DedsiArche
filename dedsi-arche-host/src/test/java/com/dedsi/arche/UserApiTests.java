package com.dedsi.arche;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dedsi.arche.system.domain.users.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

/**
 * 覆盖真实 JWT 验签、刷新轮换、注销和 Hibernate 脏检查。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTests {

    /**
     * 通过 Spring MVC 测试入口发送请求并断言响应。
     */
    @Autowired
    MockMvc mvc;

    /**
     * 测试请求与响应使用的 JSON 序列化器。
     */
    @Autowired
    ObjectMapper json;

    /**
     * 用户聚合的持久化访问契约。
     */
    @Autowired
    UserRepository users;

    /**
     * 验证注册、登录、JWT 验签、资料持久化及刷新令牌轮换和撤销。
     */
    @Test
    void authenticationAndDirtyChecking() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"alice\","
                                                + "\"displayName\":\"Alice\","
                                                + "\"password\":\"a-long-password-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(users.findByUsername("alice").orElseThrow().getPasswordHash())
                .startsWith("$2")
                .doesNotContain("a-long-password");
        mvc.perform(
                        post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"ALICE\","
                                                + "\"displayName\":\"Again\","
                                                + "\"password\":\"a-long-password-123\"}"))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        var login =
                mvc.perform(
                                post("/api/users/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\"alice\",\"password\":\"a-long-password-123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var node = json.readTree(login);
        String access = node.get("accessToken").asString();
        String refresh = node.get("refreshToken").asString();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access + "tampered"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        patch("/api/users/me")
                                .header("Authorization", "Bearer " + access)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\":\"New name\"}"))
                .andExpect(status().isOk());
        // 新 HTTP 请求从数据库重新读取，确认未显式 save 的修改已提交。
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New name"));
        String refreshBody = json.writeValueAsString(java.util.Map.of("refreshToken", refresh));
        var rotated =
                mvc.perform(
                                post("/api/users/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(refreshBody))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        mvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshBody))
                .andExpect(status().isUnauthorized());
        String next = json.readTree(rotated).get("refreshToken").asString();
        String nextBody = json.writeValueAsString(java.util.Map.of("refreshToken", next));
        mvc.perform(
                        post("/api/users/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(nextBody))
                .andExpect(status().isNoContent());
        mvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(nextBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 验证不符合字段约束的注册请求返回 HTTP 400。
     */
    @Test
    void invalidRegistrationRejected() throws Exception {
        mvc.perform(
                        post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"x\",\"displayName\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证 UTF-8 字节数超限的登录密码返回 HTTP 401。
     */
    @Test
    void oversizedUtf8PasswordReturnsUnauthorized() throws Exception {
        mvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                java.util.Map.of(
                                                        "username",
                                                        "missing",
                                                        "password",
                                                        "密".repeat(30)))))
                .andExpect(status().isUnauthorized());
    }
}
