package com.dedsi.arche;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:11126,http://127.0.0.1:11126")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsTests {
    @Autowired MockMvc mvc;

    @Test
    void allowsConfiguredOriginsAndBearerPreflight() throws Exception {
        for (var origin : new String[]{"http://localhost:11126", "http://127.0.0.1:11126"}) {
            mvc.perform(options("/api/users/login")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "content-type,authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void rejectsUnknownOriginAndUnapprovedHeaders() throws Exception {
        mvc.perform(options("/api/users/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/users/login").header("Origin", "http://localhost:11126")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "x-unapproved"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/users/login").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsDoesNotBypassAuthentication() throws Exception {
        mvc.perform(get("/api/users/me").header("Origin", "http://localhost:11126"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:11126"));
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
