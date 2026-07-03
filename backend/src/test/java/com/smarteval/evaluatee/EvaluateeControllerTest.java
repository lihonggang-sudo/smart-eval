package com.smarteval.evaluatee;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.evaluation.EvaluationRecord;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EvaluateeControllerTest extends PostgresIntegrationTest {
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
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminBearer;

    @BeforeEach
    void setUp() throws Exception {
        evaluationRecordRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        evaluateeRepository.deleteAll();
        adminUserRepository.deleteAll();
        departmentRepository.deleteAll();

        adminUserRepository.save(new AdminUser("admin", passwordEncoder.encode("secret"), "系统管理员", true));
        adminBearer = "Bearer " + login("admin", "secret");
    }

    @Test
    void createsEvaluatee() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));

        mockMvc.perform(post("/api/admin/evaluatees")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","departmentId":%d,"enabled":true}
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.createdAt").isString());
    }

    @Test
    void rejectsCreatingEvaluateeWhenDepartmentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/admin/evaluatees")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","departmentId":999999,"enabled":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void listsEvaluateesByDepartmentAndFilters() throws Exception {
        Department targetDepartment = departmentRepository.save(new Department("组织部", 1));
        Department otherDepartment = departmentRepository.save(new Department("研发部", 2));

        evaluateeRepository.save(new Evaluatee("张三", targetDepartment, true));
        evaluateeRepository.save(new Evaluatee("李四", targetDepartment, false));
        evaluateeRepository.save(new Evaluatee("张五", otherDepartment, true));

        mockMvc.perform(get("/api/admin/evaluatees")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("departmentId", String.valueOf(targetDepartment.getId()))
                        .param("name", "张")
                        .param("enabled", "true")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].name").value("张三"))
                .andExpect(jsonPath("$.data.items[0].departmentId").value(targetDepartment.getId()))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].enabled").value(true))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void updatesEvaluatee() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        Department newDepartment = departmentRepository.save(new Department("综合部", 2));
        Long evaluateeId = evaluateeRepository.save(new Evaluatee("张三", department, true)).getId();

        mockMvc.perform(put("/api/admin/evaluatees/{id}", evaluateeId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"李四","departmentId":%d,"enabled":false}
                                """.formatted(newDepartment.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("李四"))
                .andExpect(jsonPath("$.data.departmentId").value(newDepartment.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("综合部"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void rejectsUpdatingEvaluateeWhenDepartmentDoesNotExist() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        Long evaluateeId = evaluateeRepository.save(new Evaluatee("张三", department, true)).getId();

        mockMvc.perform(put("/api/admin/evaluatees/{id}", evaluateeId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"李四","departmentId":999999,"enabled":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void setsEvaluateeEnabledState() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        Long evaluateeId = evaluateeRepository.save(new Evaluatee("张三", department, true)).getId();

        mockMvc.perform(put("/api/admin/evaluatees/{id}/enabled", evaluateeId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));

        Assertions.assertFalse(evaluateeRepository.findById(evaluateeId).orElseThrow().isEnabled());
    }

    @Test
    void deletesEvaluateeWithoutEvaluationRecords() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        Long evaluateeId = evaluateeRepository.save(new Evaluatee("张三", department, true)).getId();

        mockMvc.perform(delete("/api/admin/evaluatees/{id}", evaluateeId)
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Assertions.assertFalse(evaluateeRepository.findById(evaluateeId).isPresent());
    }

    @Test
    void deleteDisablesEvaluateeWhenEvaluationRecordsExist() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        Evaluatee evaluatee = evaluateeRepository.save(new Evaluatee("张三", department, true));
        EvaluatorUser evaluatorUser = evaluatorUserRepository.save(new EvaluatorUser("eva-1", "P12345", department, true));
        evaluationRecordRepository.save(new EvaluationRecord(
                evaluatorUser,
                evaluatee,
                department,
                EvaluationGrade.COMPETENT,
                "comment1",
                "comment2"));

        mockMvc.perform(delete("/api/admin/evaluatees/{id}", evaluatee.getId())
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Evaluatee persisted = evaluateeRepository.findById(evaluatee.getId()).orElseThrow();
        Assertions.assertFalse(persisted.isEnabled());
    }

    @Test
    void evaluateeApisRejectDisabledAdminAfterTokenIssued() throws Exception {
        AdminUser adminUser = adminUserRepository.findByUsername("admin").orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/evaluatees")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        Department department = departmentRepository.save(new Department("组织部", 1));

        mockMvc.perform(post("/api/admin/evaluatees")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","departmentId":%d,"enabled":true}
                                """.formatted(department.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        Long evaluateeId = evaluateeRepository.save(new Evaluatee("张三", department, true)).getId();
        Department otherDepartment = departmentRepository.save(new Department("综合部", 2));

        mockMvc.perform(put("/api/admin/evaluatees/{id}", evaluateeId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"李四","departmentId":%d,"enabled":false}
                                """.formatted(otherDepartment.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(put("/api/admin/evaluatees/{id}/enabled", evaluateeId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(delete("/api/admin/evaluatees/{id}", evaluateeId)
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
