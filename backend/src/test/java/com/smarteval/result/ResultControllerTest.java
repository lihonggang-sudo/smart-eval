package com.smarteval.result;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.evaluation.EvaluationRecord;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ResultControllerTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

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

    private String adminBearer;
    private Department departmentA;
    private Department departmentB;
    private Evaluatee evaluateeA1;
    private Evaluatee evaluateeA2;
    private EvaluatorUser evaluatorA1;
    private EvaluatorUser evaluatorA2;
    private EvaluatorUser evaluatorA3;

    @BeforeEach
    void setUp() throws Exception {
        evaluationRecordRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        evaluateeRepository.deleteAll();
        departmentRepository.deleteAll();
        adminUserRepository.deleteAll();

        adminUserRepository.save(new AdminUser("admin", passwordEncoder.encode("secret"), "系统管理员", true));

        departmentA = departmentRepository.save(new Department("组织部", 1));
        departmentB = departmentRepository.save(new Department("研发部", 2));

        evaluateeA1 = evaluateeRepository.save(new Evaluatee("张三", departmentA, true));
        evaluateeA2 = evaluateeRepository.save(new Evaluatee("李四", departmentA, true));
        Evaluatee evaluateeB1 = evaluateeRepository.save(new Evaluatee("赵六", departmentB, true));

        evaluatorA1 = evaluatorUserRepository.save(new EvaluatorUser("eva-1", "plain-1", departmentA, true));
        evaluatorA2 = evaluatorUserRepository.save(new EvaluatorUser("eva-2", "plain-2", departmentA, true));
        evaluatorA3 = evaluatorUserRepository.save(new EvaluatorUser("eva-4", "plain-4", departmentA, true));
        EvaluatorUser evaluatorB1 = evaluatorUserRepository.save(new EvaluatorUser("eva-3", "plain-3", departmentB, true));

        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorA1, evaluateeA1, departmentA, EvaluationGrade.EXCELLENT, "工作扎实", "协作顺畅"));
        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorA2, evaluateeA1, departmentA, EvaluationGrade.COMPETENT, "执行稳定", "配合到位"));
        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorA3, evaluateeA1, departmentA, EvaluationGrade.INCOMPETENT, "结果偏差", "需要改进"));
        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorA1, evaluateeA2, departmentA, EvaluationGrade.BASIC_COMPETENT, "基础达标", "继续提升"));
        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorB1, evaluateeB1, departmentB, EvaluationGrade.EXCELLENT, "表现突出", "带动团队"));

        adminBearer = "Bearer " + login("admin", "secret");
    }

    @Test
    void summaryReturnsAggregatedCountsAndSupportsFiltersAndPaging() throws Exception {
        mockMvc.perform(get("/api/admin/results/summary")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(evaluateeA1.getId()))
                .andExpect(jsonPath("$.data.items[0].evaluateeName").value("张三"))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].excellentCount").value(1))
                .andExpect(jsonPath("$.data.items[0].competentCount").value(1))
                .andExpect(jsonPath("$.data.items[0].basicCompetentCount").value(0))
                .andExpect(jsonPath("$.data.items[0].incompetentCount").value(1))
                .andExpect(jsonPath("$.data.items[0].totalCount").value(3))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        mockMvc.perform(get("/api/admin/results/summary")
                        .param("departmentId", departmentA.getId().toString())
                        .param("evaluateeName", "张三")
                        .param("page", "1")
                        .param("pageSize", "1")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(evaluateeA1.getId()))
                .andExpect(jsonPath("$.data.items[0].totalCount").value(3))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1));
    }

    @Test
    void detailsReturnTraceFieldsAndSupportsFiltersAndPaging() throws Exception {
        mockMvc.perform(get("/api/admin/results/details")
                        .param("departmentId", departmentA.getId().toString())
                        .param("evaluateeId", evaluateeA1.getId().toString())
                        .param("evaluatorUserId", evaluatorA1.getId().toString())
                        .param("grade", "EXCELLENT")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].evaluatorUserId").value(evaluatorA1.getId()))
                .andExpect(jsonPath("$.data.items[0].evaluatorUsername").value("eva-1"))
                .andExpect(jsonPath("$.data.items[0].evaluatorDepartmentName").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(evaluateeA1.getId()))
                .andExpect(jsonPath("$.data.items[0].evaluateeName").value("张三"))
                .andExpect(jsonPath("$.data.items[0].grade").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.items[0].gradeLabel").value("优秀"))
                .andExpect(jsonPath("$.data.items[0].comment1").value("工作扎实"))
                .andExpect(jsonPath("$.data.items[0].comment2").value("协作顺畅"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        mockMvc.perform(get("/api/admin/results/details")
                        .param("page", "2")
                        .param("pageSize", "2")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(2));
    }

    @Test
    void summaryUsesRecordedDepartmentAfterEvaluateeMovesAndMatchesDetailFiltering() throws Exception {
        evaluateeA1.update("张三", departmentB, true);
        evaluateeRepository.saveAndFlush(evaluateeA1);

        mockMvc.perform(get("/api/admin/results/summary")
                        .param("departmentId", departmentA.getId().toString())
                        .param("evaluateeName", "张三")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(evaluateeA1.getId()))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].totalCount").value(3));

        mockMvc.perform(get("/api/admin/results/summary")
                        .param("departmentId", departmentB.getId().toString())
                        .param("evaluateeName", "张三")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/admin/results/details")
                        .param("departmentId", departmentA.getId().toString())
                        .param("evaluateeId", evaluateeA1.getId().toString())
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void detailsRejectInvalidGradeFilterWithClientErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/admin/results/details")
                        .param("grade", "BAD_VALUE")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: grade"));
    }

    @Test
    void resultApisRejectDisabledAdminAfterTokenIssued() throws Exception {
        AdminUser adminUser = adminUserRepository.findByUsername("admin").orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/results/summary")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(get("/api/admin/results/details")
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
