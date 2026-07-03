package com.smarteval.evaluation;

import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EvaluationControllerTest extends PostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EvaluateeRepository evaluateeRepository;

    @Autowired
    private EvaluatorUserRepository evaluatorUserRepository;

    @Autowired
    private EvaluationRecordRepository evaluationRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String evaluatorBearer;
    private EvaluatorUser evaluatorUser;
    private Evaluatee departmentAEvaluatee1;
    private Evaluatee departmentAEvaluatee2;
    private Evaluatee departmentBEvaluatee;
    private Evaluatee disabledDepartmentAEvaluatee;

    @BeforeEach
    void setUp() throws Exception {
        evaluationRecordRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        evaluateeRepository.deleteAll();
        departmentRepository.deleteAll();

        Department departmentA = departmentRepository.save(new Department("组织部", 1));
        Department departmentB = departmentRepository.save(new Department("研发部", 2));

        evaluatorUser = evaluatorUserRepository.save(new EvaluatorUser("eva-1", "plain-secret", departmentA, true));
        departmentAEvaluatee1 = evaluateeRepository.save(new Evaluatee("张三", departmentA, true));
        departmentAEvaluatee2 = evaluateeRepository.save(new Evaluatee("李四", departmentA, true));
        disabledDepartmentAEvaluatee = evaluateeRepository.save(new Evaluatee("王五", departmentA, false));
        departmentBEvaluatee = evaluateeRepository.save(new Evaluatee("赵六", departmentB, true));

        evaluatorBearer = "Bearer " + login("eva-1", "plain-secret");
    }

    @Test
    void itemsReturnsOnlyEnabledOwnDepartmentEvaluateesAndFourGrades() throws Exception {
        mockMvc.perform(get("/api/mobile/evaluation/items")
                        .header("Authorization", evaluatorBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(evaluatorUser.getDepartment().getId()))
                .andExpect(jsonPath("$.data.departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.submitted").value(false))
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordPlain"))))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].evaluateeId").value(departmentAEvaluatee1.getId()))
                .andExpect(jsonPath("$.data.items[0].name").value("张三"))
                .andExpect(jsonPath("$.data.items[1].evaluateeId").value(departmentAEvaluatee2.getId()))
                .andExpect(jsonPath("$.data.items[1].name").value("李四"))
                .andExpect(jsonPath("$.data.grades", hasSize(4)))
                .andExpect(jsonPath("$.data.grades[0].value").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.grades[0].label").value("优秀"))
                .andExpect(jsonPath("$.data.grades[1].value").value("COMPETENT"))
                .andExpect(jsonPath("$.data.grades[1].label").value("称职"))
                .andExpect(jsonPath("$.data.grades[2].value").value("BASIC_COMPETENT"))
                .andExpect(jsonPath("$.data.grades[2].label").value("基本称职"))
                .andExpect(jsonPath("$.data.grades[3].value").value("INCOMPETENT"))
                .andExpect(jsonPath("$.data.grades[3].label").value("不称职"));
    }

    @Test
    void itemsRejectsDisabledEvaluatorAfterTokenIssued() throws Exception {
        evaluatorUser.updateEnabled(false);
        evaluatorUserRepository.saveAndFlush(evaluatorUser);

        mockMvc.perform(get("/api/mobile/evaluation/items")
                        .header("Authorization", evaluatorBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void submitCreatesRecordsAndMarksEvaluatorSubmitted() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"工作扎实","comment2":"协作顺畅"},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":"执行稳定"}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), departmentAEvaluatee2.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.submittedAt").isString())
                .andExpect(jsonPath("$.data").value(not(hasKey("passwordPlain"))));

        EvaluatorUser persistedEvaluator = evaluatorUserRepository.findById(evaluatorUser.getId()).orElseThrow();
        Assertions.assertTrue(persistedEvaluator.isSubmitted());
        Assertions.assertNotNull(persistedEvaluator.getSubmittedAt());

        List<EvaluationRecord> records = evaluationRecordRepository.findAll().stream()
                .sorted(Comparator.comparing(record -> record.getEvaluatee().getId()))
                .toList();
        Assertions.assertEquals(2, records.size());
        Assertions.assertEquals(departmentAEvaluatee1.getId(), records.get(0).getEvaluatee().getId());
        Assertions.assertEquals(EvaluationGrade.EXCELLENT, records.get(0).getGrade());
        Assertions.assertEquals("工作扎实", records.get(0).getComment1());
        Assertions.assertEquals("协作顺畅", records.get(0).getComment2());
        Assertions.assertEquals(departmentAEvaluatee2.getId(), records.get(1).getEvaluatee().getId());
        Assertions.assertEquals(EvaluationGrade.COMPETENT, records.get(1).getGrade());
        Assertions.assertEquals("", records.get(1).getComment1());
        Assertions.assertEquals("执行稳定", records.get(1).getComment2());
    }

    @Test
    void submitRejectsOverlongCommentsWithValidationError() throws Exception {
        String overlongComment = "x".repeat(256);

        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"%s","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), overlongComment, departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").value(hasKey("records[0].comment1")));
    }

    @Test
    void submitRejectsWhenEnabledEvaluateeIsMissing() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("必须完成本部门全部人员评价后才能提交"));
    }

    @Test
    void submitRejectsWhenRequestIncludesOtherDepartmentEvaluatee() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"BASIC_COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(
                                        departmentAEvaluatee1.getId(),
                                        departmentAEvaluatee2.getId(),
                                        departmentBEvaluatee.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsDuplicateEvaluateeIds() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), departmentAEvaluatee1.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsDisabledEvaluatee() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"BASIC_COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(
                                        departmentAEvaluatee1.getId(),
                                        departmentAEvaluatee2.getId(),
                                        disabledDepartmentAEvaluatee.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsDisabledEvaluatorAfterTokenIssued() throws Exception {
        evaluatorUser.updateEnabled(false);
        evaluatorUserRepository.saveAndFlush(evaluatorUser);

        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void submitRejectsNullEvaluateeIdWithBusinessMessage() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":null,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsNullGradeWithBusinessMessage() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":null,"comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsNullRecordWithBusinessMessage() throws Exception {
        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  null,
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能评价本部门启用人员"));
    }

    @Test
    void submitRejectsWhenEvaluatorAlreadySubmitted() throws Exception {
        jdbcTemplate.update(
                "update evaluator_user set submitted = true, submitted_at = now() where id = ?",
                evaluatorUser.getId());

        mockMvc.perform(post("/api/mobile/evaluation/submit")
                        .header("Authorization", evaluatorBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"records":[
                                  {"evaluateeId":%d,"grade":"EXCELLENT","comment1":"","comment2":""},
                                  {"evaluateeId":%d,"grade":"COMPETENT","comment1":"","comment2":""}
                                ]}
                                """.formatted(departmentAEvaluatee1.getId(), departmentAEvaluatee2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void findByIdForUpdateLoadsEvaluatorInsideTransaction() {
        EvaluatorUser locked = new TransactionTemplate(transactionManager).execute(status ->
                evaluatorUserRepository.findByIdForUpdate(evaluatorUser.getId()).orElseThrow());

        Assertions.assertNotNull(locked);
        Assertions.assertEquals(evaluatorUser.getId(), locked.getId());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/mobile/auth/login")
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
