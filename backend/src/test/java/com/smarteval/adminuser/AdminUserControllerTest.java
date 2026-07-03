package com.smarteval.adminuser;

import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminUserControllerTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long onlyAdminId;
    private String adminBearer;

    @BeforeEach
    void setUp() throws Exception {
        adminUserRepository.deleteAll();

        onlyAdminId = adminUserRepository.save(
                new AdminUser("admin", passwordEncoder.encode("secret"), "系统管理员", true))
                .getId();

        adminBearer = "Bearer " + login("admin", "secret");
    }

    @Test
    void listsAdminUsers() throws Exception {
        adminUserRepository.save(new AdminUser("second", passwordEncoder.encode("secret2"), "第二管理员", true));

        mockMvc.perform(get("/api/admin/admin-users")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].username").value("second"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1));
    }

    @Test
    void createsAdminUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/admin/admin-users")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new-admin","password":"new-secret","displayName":"新管理员"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("new-admin"))
                .andExpect(jsonPath("$.data.displayName").value("新管理员"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        AdminUser created = adminUserRepository.findByUsername("new-admin").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("new-secret", created.getPasswordHash()));
    }

    @Test
    void updatesAdminDisplayName() throws Exception {
        mockMvc.perform(put("/api/admin/admin-users/{id}", onlyAdminId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"首席管理员"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("首席管理员"));
    }

    @Test
    void rejectsDisablingLastEnabledAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/admin-users/{id}/enabled", onlyAdminId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能禁用最后一个启用的管理员"));
    }

    @Test
    void adminUserApisRejectDisabledAdminAfterTokenIssued() throws Exception {
        AdminUser adminUser = adminUserRepository.findById(onlyAdminId).orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/admin-users")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(post("/api/admin/admin-users")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new-admin","password":"new-secret","displayName":"新管理员"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void resetsAdminPassword() throws Exception {
        mockMvc.perform(post("/api/admin/admin-users/{id}/reset-password", onlyAdminId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"updated-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AdminUser updated = adminUserRepository.findById(onlyAdminId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("updated-secret", updated.getPasswordHash()));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int tokenStart = response.indexOf("\"token\":\"") + 9;
        int tokenEnd = response.indexOf('"', tokenStart);
        return response.substring(tokenStart, tokenEnd);
    }
}
