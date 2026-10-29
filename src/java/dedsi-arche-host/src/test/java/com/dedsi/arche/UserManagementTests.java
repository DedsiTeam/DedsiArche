package com.dedsi.arche;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="app.users.admin-usernames=manager_test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    JsonNode send(MockHttpServletRequestBuilder request, Object body, int status) throws Exception {
        if(body!=null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var result=mvc.perform(request).andExpect(status().is(status)).andReturn();
        String text=result.getResponse().getContentAsString();
        return text.isEmpty()?null:json.readTree(text);
    }
    @Test void managementIsRestrictedAndUpdatesArePersisted() throws Exception {
        String password="Management#2026!";
        var admin=send(post("/api/users/register"),Map.of("username","manager_test","displayName","Manager","password",password),201);
        var login=send(post("/api/users/login"),Map.of("username","manager_test","password",password),200);
        assertThat(login.path("user").path("canManageUsers").asBoolean()).isTrue();
        String bearer="Bearer "+login.path("accessToken").asText();
        String name="managed_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
        send(post("/api/users/page"),Map.of(),401);
        var created=send(post("/api/users").header("Authorization",bearer),Map.of("username",name,"displayName","Managed","password",password),201);
        String id=created.path("id").asText();
        var normal=send(post("/api/users/login"),Map.of("username",name,"password",password),200);
        String normalBearer="Bearer "+normal.path("accessToken").asText();
        assertThat(normal.path("user").path("canManageUsers").asBoolean()).isFalse();
        send(post("/api/users/page").header("Authorization",normalBearer),Map.of(),403);
        send(post("/api/users").header("Authorization",normalBearer),Map.of("username","should_not_create","displayName","No","password",password),403);
        send(patch("/api/users/"+id).header("Authorization",normalBearer),Map.of("displayName","Bad","enabled",false,"version",0),403);
        send(post("/api/users/page").header("Authorization",bearer),Map.of("page",0),400);
        send(post("/api/users/page").header("Authorization",bearer),Map.of("size",101),400);
        var page=send(post("/api/users/page").header("Authorization",bearer),Map.of("keyword",name),200);
        assertThat(page.path("total").asLong()).isEqualTo(1);
        assertThat(page.toString()).doesNotContain("passwordHash","password_hash","refreshToken");
        send(patch("/api/users/"+admin.path("id").asText()).header("Authorization",bearer),Map.of("displayName","Manager","enabled",false,"version",0),400);
        var changed=send(patch("/api/users/"+id).header("Authorization",bearer),Map.of("displayName","Updated","enabled",false,"version",0),200);
        assertThat(changed.path("version").asLong()).isEqualTo(1);
        send(get("/api/users/me").header("Authorization",normalBearer),null,401);
        send(post("/api/users/refresh"),Map.of("refreshToken",normal.path("refreshToken").asText()),401);
        send(post("/api/users/login"),Map.of("username",name,"password",password),400);
        send(patch("/api/users/"+id).header("Authorization",bearer),Map.of("displayName","Stale","enabled",true,"version",0),409);
        send(patch("/api/users/"+id).header("Authorization",bearer),Map.of("displayName","Updated","enabled",true,"version",1),200);
        var relogin=send(post("/api/users/login"),Map.of("username",name,"password",password),200);
        assertThat(relogin.path("user").path("displayName").asText()).isEqualTo("Updated");
        var detail=send(get("/api/users/"+id).header("Authorization",bearer),null,200);
        assertThat(detail.path("username").asText()).isEqualTo(name);
        assertThat(detail.toString()).doesNotContain("password","credentialVersion");
        String validNormal="Bearer "+relogin.path("accessToken").asText();
        send(get("/api/users/"+id).header("Authorization",validNormal),null,403);
        send(post("/api/users/"+id+"/reset-password").header("Authorization",validNormal),Map.of("password","Reset#2026!Password","version",2),403);
        send(delete("/api/users/"+id).param("version","2").header("Authorization",validNormal),null,403);
        send(delete("/api/users/"+admin.path("id").asText()).param("version","0").header("Authorization",bearer),null,400);
        send(post("/api/users/"+id+"/reset-password").header("Authorization",bearer),Map.of("password","short","version",2),400);
        send(post("/api/users/"+id+"/reset-password").header("Authorization",bearer),Map.of("password","中".repeat(25),"version",2),400);
        send(post("/api/users/"+id+"/reset-password").header("Authorization",bearer),Map.of("password","Reset#2026!Password","version",0),409);
        send(post("/api/users/"+id+"/reset-password").header("Authorization",bearer),Map.of("password","Reset#2026!Password","version",2),204);
        send(get("/api/users/me").header("Authorization",validNormal),null,401);
        send(post("/api/users/refresh"),Map.of("refreshToken",relogin.path("refreshToken").asText()),401);
        send(post("/api/users/login"),Map.of("username",name,"password",password),401);
        var resetLogin=send(post("/api/users/login"),Map.of("username",name,"password","Reset#2026!Password"),200);
        send(get("/api/users/me").header("Authorization","Bearer "+resetLogin.path("accessToken").asText()),null,200);
        send(delete("/api/users/"+id).param("version","2").header("Authorization",bearer),null,409);
        send(delete("/api/users/"+id).param("version","3").header("Authorization",bearer),null,204);
        send(get("/api/users/"+id).header("Authorization",bearer),null,404);
        send(delete("/api/users/"+id).param("version","3").header("Authorization",bearer),null,404);
        send(get("/api/users/me").header("Authorization","Bearer "+resetLogin.path("accessToken").asText()),null,401);
        send(post("/api/users/refresh"),Map.of("refreshToken",resetLogin.path("refreshToken").asText()),401);
        send(post("/api/users/login"),Map.of("username",name,"password","Reset#2026!Password"),401);
        assertThat(send(post("/api/users/page").header("Authorization",bearer),Map.of("keyword",name),200).path("total").asLong()).isZero();
        send(patch("/api/users/"+UUID.randomUUID()).header("Authorization",bearer),Map.of("displayName","Missing","enabled",true,"version",0),404);
    }
}
