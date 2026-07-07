package com.smarteval.evaluatoruser;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EvaluatorUserControllerTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EvaluatorUserRepository evaluatorUserRepository;

    @Autowired
    private EvaluateeRepository evaluateeRepository;

    @Autowired
    private EvaluationRecordRepository evaluationRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long adminId;
    private String adminBearer;
    private Long departmentOneId;
    private Long departmentTwoId;
    private Long evaluatorId;
    private String departmentOneCode;
    private String departmentTwoCode;

    @BeforeEach
    void setUp() throws Exception {
        evaluationRecordRepository.deleteAll();
        evaluateeRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        adminUserRepository.deleteAll();
        departmentRepository.deleteAll();

        Department departmentOne = departmentRepository.save(new Department("组织部", 1));
        Department departmentTwo = departmentRepository.save(new Department("研发部", 2));
        departmentOneId = departmentOne.getId();
        departmentTwoId = departmentTwo.getId();
        departmentOneCode = "D%03d".formatted(departmentOneId);
        departmentTwoCode = "D%03d".formatted(departmentTwoId);

        adminId = adminUserRepository.save(new AdminUser("admin", passwordEncoder.encode("secret"), "系统管理员", true))
                .getId();
        adminBearer = "Bearer " + login("admin", "secret");

        evaluatorUserRepository.save(new EvaluatorUser("%s-0001".formatted(departmentOneCode), "OLD111", departmentOne, true));
        evaluatorId = evaluatorUserRepository.save(
                new EvaluatorUser("%s-0002".formatted(departmentOneCode), "PASS22", departmentOne, false, true, null))
                .getId();
        evaluatorUserRepository.save(new EvaluatorUser("%s-0001".formatted(departmentTwoCode), "PASS33", departmentTwo, true));
    }

    @Test
    void generatorCreatesRandomUsernameAndSixCharacterPassword() {
        RandomCredentialGenerator generator = new RandomCredentialGenerator();

        Assertions.assertTrue(generator.username().matches("[A-Z0-9]{8}"));
        Assertions.assertTrue(generator.password().matches("[A-Z0-9]{6}"));
    }

    @Test
    void listsEvaluatorUsersWithFiltersAndPlainPasswordForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/evaluator-users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("departmentId", departmentOneId.toString())
                        .param("submitted", "true")
                        .param("enabled", "false")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(evaluatorId))
                .andExpect(jsonPath("$.data.items[0].username").value("%s-0002".formatted(departmentOneCode)))
                .andExpect(jsonPath("$.data.items[0].passwordPlain").value("PASS22"))
                .andExpect(jsonPath("$.data.items[0].departmentId").value(departmentOneId))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].submitted").value(true))
                .andExpect(jsonPath("$.data.items[0].enabled").value(false))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void batchGeneratesEvaluatorUsersWithRandomUniqueNames() throws Exception {
        mockMvc.perform(post("/api/admin/evaluator-users/batch-generate")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"count":3}
                                """.formatted(departmentOneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.created").value(3))
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].username", matchesPattern("[A-Z0-9]{8}")))
                .andExpect(jsonPath("$.data.items[1].username", matchesPattern("[A-Z0-9]{8}")))
                .andExpect(jsonPath("$.data.items[2].username", matchesPattern("[A-Z0-9]{8}")))
                .andExpect(jsonPath("$.data.items[0].username").value(org.hamcrest.Matchers.not(matchesPattern("D\\d{3}-\\d{4}"))))
                .andExpect(jsonPath("$.data.items[1].username").value(org.hamcrest.Matchers.not(matchesPattern("D\\d{3}-\\d{4}"))))
                .andExpect(jsonPath("$.data.items[2].username").value(org.hamcrest.Matchers.not(matchesPattern("D\\d{3}-\\d{4}"))))
                .andExpect(jsonPath("$.data.items[0].departmentId").value(departmentOneId))
                .andExpect(jsonPath("$.data.items[1].departmentId").value(departmentOneId))
                .andExpect(jsonPath("$.data.items[2].departmentId").value(departmentOneId))
                .andExpect(jsonPath("$.data.items[0].passwordPlain", matchesPattern("[A-Z0-9]{6}")))
                .andExpect(jsonPath("$.data.items[1].passwordPlain", matchesPattern("[A-Z0-9]{6}")))
                .andExpect(jsonPath("$.data.items[2].passwordPlain", matchesPattern("[A-Z0-9]{6}")));

        var usernames = evaluatorUserRepository.findAll().stream()
                .map(EvaluatorUser::getUsername)
                .filter(username -> username.matches("[A-Z0-9]{8}"))
                .toList();

        Assertions.assertEquals(3, usernames.size());
        Assertions.assertEquals(3, usernames.stream().distinct().count());
    }

    @Test
    void rejectsBatchGenerationCountBelowMinimum() throws Exception {
        mockMvc.perform(post("/api/admin/evaluator-users/batch-generate")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"count":0}
                                """.formatted(departmentOneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.count").value("must be greater than or equal to 1"));
    }

    @Test
    void rejectsBatchGenerationCountAboveMaximum() throws Exception {
        mockMvc.perform(post("/api/admin/evaluator-users/batch-generate")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"count":501}
                                """.formatted(departmentOneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.count").value("must be less than or equal to 500"));
    }

    @Test
    void resetsEvaluatorPassword() throws Exception {
        mockMvc.perform(post("/api/admin/evaluator-users/{id}/reset-password", evaluatorId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.passwordPlain", matchesPattern("[A-Z0-9]{6}")));

        EvaluatorUser reloaded = evaluatorUserRepository.findById(evaluatorId).orElseThrow();
        Assertions.assertNotEquals("PASS22", reloaded.getPasswordPlain());
        Assertions.assertTrue(reloaded.getPasswordPlain().matches("[A-Z0-9]{6}"));
    }

    @Test
    void updatesEvaluatorEnabledState() throws Exception {
        mockMvc.perform(put("/api/admin/evaluator-users/{id}/enabled", evaluatorId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(evaluatorId))
                .andExpect(jsonPath("$.data.enabled").value(true));

        EvaluatorUser reloaded = evaluatorUserRepository.findById(evaluatorId).orElseThrow();
        Assertions.assertTrue(reloaded.isEnabled());
    }

    @Test
    void printsFilteredEvaluatorCredentialsAsHtml() throws Exception {
        mockMvc.perform(get("/api/admin/evaluator-users/print")
                        .param("departmentId", departmentOneId.toString())
                        .param("submitted", "true")
                        .param("enabled", "false")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:5173/mobile/login")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("组织部")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("%s-0002".formatted(departmentOneCode))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PASS22")));
    }

    @Test
    void evaluatorUserApisRejectDisabledAdminAfterTokenIssued() throws Exception {
        AdminUser adminUser = adminUserRepository.findById(adminId).orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/evaluator-users")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(post("/api/admin/evaluator-users/batch-generate")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"count":1}
                                """.formatted(departmentOneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(post("/api/admin/evaluator-users/{id}/reset-password", evaluatorId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(put("/api/admin/evaluator-users/{id}/enabled", evaluatorId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(get("/api/admin/evaluator-users/print")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
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
