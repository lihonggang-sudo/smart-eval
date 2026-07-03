package com.smarteval.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SmartEvalFlowTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EvaluateeRepository evaluateeRepository;

    @Autowired
    private EvaluatorUserRepository evaluatorUserRepository;

    @Autowired
    private EvaluationRecordRepository evaluationRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        evaluationRecordRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        evaluateeRepository.deleteAll();
        adminUserRepository.deleteAll();
        departmentRepository.deleteAll();

        if (adminUserRepository.findByUsername("admin").isEmpty()) {
            adminUserRepository.save(new AdminUser(
                    "admin",
                    passwordEncoder.encode("secret"),
                    "系统管理员",
                    true));
        }
    }

    @Test
    void completesAdminToEvaluatorToResultsFlow() throws Exception {
        JsonNode adminLogin = postJson("/api/admin/auth/login", """
                {"username":"admin","password":"secret"}
                """);
        String adminBearer = bearer(adminLogin.path("data").path("token").asText());

        JsonNode departmentResponse = postAuthorized(adminBearer, "/api/admin/departments", """
                {"name":"组织部","sortOrder":1}
                """);
        long departmentId = departmentResponse.path("data").path("id").asLong();

        JsonNode evaluateeOneResponse = postAuthorized(adminBearer, "/api/admin/evaluatees", """
                {"name":"张三","departmentId":%d,"enabled":true}
                """.formatted(departmentId));
        JsonNode evaluateeTwoResponse = postAuthorized(adminBearer, "/api/admin/evaluatees", """
                {"name":"李四","departmentId":%d,"enabled":true}
                """.formatted(departmentId));
        long evaluateeOneId = evaluateeOneResponse.path("data").path("id").asLong();
        long evaluateeTwoId = evaluateeTwoResponse.path("data").path("id").asLong();

        JsonNode batchGenerateResponse = postAuthorized(adminBearer, "/api/admin/evaluator-users/batch-generate", """
                {"departmentId":%d,"count":1}
                """.formatted(departmentId));
        assertEquals(1, batchGenerateResponse.path("data").path("created").asInt());
        JsonNode evaluatorItem = batchGenerateResponse.path("data").path("items").get(0);
        String evaluatorUsername = evaluatorItem.path("username").asText();
        String evaluatorPassword = evaluatorItem.path("passwordPlain").asText();
        long evaluatorUserId = evaluatorItem.path("id").asLong();

        JsonNode evaluatorLogin = postJson("/api/mobile/auth/login", """
                {"username":"%s","password":"%s"}
                """.formatted(evaluatorUsername, evaluatorPassword));
        String evaluatorBearer = bearer(evaluatorLogin.path("data").path("token").asText());

        mockMvc.perform(get("/api/mobile/evaluation/items")
                        .header("Authorization", evaluatorBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(departmentId))
                .andExpect(jsonPath("$.data.departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.submitted").value(false))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(evaluateeOneId))
                .andExpect(jsonPath("$.data.items[0].name").value("张三"))
                .andExpect(jsonPath("$.data.items[1].evaluateeId").value(evaluateeTwoId))
                .andExpect(jsonPath("$.data.items[1].name").value("李四"));

        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"工作扎实","comment2":"协作顺畅"},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"执行稳定","comment2":"反馈及时"}
                                ]}
                                """.formatted(evaluateeOneId, evaluateeTwoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.submittedAt").isString());

        mockMvc.perform(get("/api/admin/results/summary")
                        .param("departmentId", String.valueOf(departmentId))
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].totalCount").value(1))
                .andExpect(jsonPath("$.data.items[1].totalCount").value(1));

        mockMvc.perform(get("/api/admin/results/details")
                        .param("departmentId", String.valueOf(departmentId))
                        .param("evaluatorUserId", String.valueOf(evaluatorUserId))
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].evaluatorUsername").value(evaluatorUsername))
                .andExpect(jsonPath("$.data.items[1].evaluatorUsername").value(evaluatorUsername));

        JsonNode summary = getJson(adminBearer, "/api/admin/results/summary", departmentId, null);
        assertSummaryCount(summary, "张三", 1);
        assertSummaryCount(summary, "李四", 1);

        JsonNode details = getJson(adminBearer, "/api/admin/results/details", departmentId, evaluatorUserId);
        assertEquals(2, details.path("data").path("items").size());
        assertTrue(details.path("data").path("items").findValuesAsText("evaluatorUsername").stream()
                .allMatch(evaluatorUsername::equals));
    }

    private JsonNode postAuthorized(String bearer, String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postJson(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getJson(String bearer, String path, Long departmentId, Long evaluatorUserId) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = get(path)
                .param("page", "1")
                .param("pageSize", "20")
                .header("Authorization", bearer);
        if (departmentId != null) {
            request.param("departmentId", String.valueOf(departmentId));
        }
        if (evaluatorUserId != null) {
            request.param("evaluatorUserId", String.valueOf(evaluatorUserId));
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertSummaryCount(JsonNode summary, String evaluateeName, int expectedTotalCount) {
        JsonNode item = summary.path("data").path("items").findParents("evaluateeName").stream()
                .filter(candidate -> evaluateeName.equals(candidate.path("evaluateeName").asText()))
                .findFirst()
                .orElseThrow();
        assertEquals(expectedTotalCount, item.path("totalCount").asInt());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
