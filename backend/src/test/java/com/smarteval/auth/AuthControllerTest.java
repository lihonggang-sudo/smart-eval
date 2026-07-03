package com.smarteval.auth;

import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private EvaluatorUserRepository evaluatorUserRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adminId;
    private Long evaluatorId;
    private Long departmentId;

    @BeforeEach
    void setUp() {
        evaluatorUserRepository.deleteAll();
        adminUserRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = departmentRepository.save(new Department("研发部", 1));
        departmentId = department.getId();

        adminId = adminUserRepository.save(
                new com.smarteval.adminuser.AdminUser(
                        "admin",
                        passwordEncoder.encode("secret"),
                        "系统管理员",
                        true))
                .getId();

        evaluatorId = evaluatorUserRepository.save(
                new EvaluatorUser("eva", "plain-secret", department, true, true, null))
                .getId();
    }

    @Test
    void adminLoginReturnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.id").value(adminId))
                .andExpect(jsonPath("$.data.user.displayName").value("系统管理员"));
    }

    @Test
    void adminMeReturnsCurrentAdminProfile() throws Exception {
        String token = TestLogin.login(mockMvc, "/api/admin/auth/login", "admin", "secret");

        mockMvc.perform(get("/api/admin/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(adminId))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.displayName").value("系统管理员"));
    }

    @Test
    void adminMeRejectsDisabledAdminAfterTokenIssued() throws Exception {
        String token = TestLogin.login(mockMvc, "/api/admin/auth/login", "admin", "secret");
        com.smarteval.adminuser.AdminUser adminUser = adminUserRepository.findById(adminId).orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void evaluatorLoginReturnsDepartmentAndSubmittedWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"eva","password":"plain-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.id").value(evaluatorId))
                .andExpect(jsonPath("$.data.user.username").value("eva"))
                .andExpect(jsonPath("$.data.user.departmentId").value(departmentId))
                .andExpect(jsonPath("$.data.user.departmentName").value("研发部"))
                .andExpect(jsonPath("$.data.user.submitted").value(true))
                .andExpect(jsonPath("$.data.user").value(not(hasKey("passwordPlain"))));
    }

    @Test
    void evaluatorMeReturnsCurrentEvaluatorProfile() throws Exception {
        String token = TestLogin.login(mockMvc, "/api/mobile/auth/login", "eva", "plain-secret");

        mockMvc.perform(get("/api/mobile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(evaluatorId))
                .andExpect(jsonPath("$.data.username").value("eva"))
                .andExpect(jsonPath("$.data.departmentId").value(departmentId))
                .andExpect(jsonPath("$.data.departmentName").value("研发部"))
                .andExpect(jsonPath("$.data.submitted").value(true));
    }

    @Test
    void evaluatorMeRejectsDisabledEvaluatorAfterTokenIssued() throws Exception {
        String token = TestLogin.login(mockMvc, "/api/mobile/auth/login", "eva", "plain-secret");
        jdbcTemplate.update("update evaluator_user set enabled = false where id = ?", evaluatorId);

        mockMvc.perform(get("/api/mobile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    private static final class TestLogin {
        private TestLogin() {
        }

        static String login(MockMvc mockMvc, String path, String username, String password) throws Exception {
            String response = mockMvc.perform(post(path)
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
}
