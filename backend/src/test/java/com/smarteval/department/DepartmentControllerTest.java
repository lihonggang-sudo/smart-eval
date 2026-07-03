package com.smarteval.department;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.evaluation.EvaluationRecord;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatee.EvaluateeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DepartmentControllerTest extends PostgresIntegrationTest {
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

    private Long adminId;
    private String adminBearer;

    @BeforeEach
    void setUp() throws Exception {
        evaluationRecordRepository.deleteAll();
        evaluatorUserRepository.deleteAll();
        evaluateeRepository.deleteAll();
        adminUserRepository.deleteAll();
        departmentRepository.deleteAll();

        adminId = adminUserRepository.save(
                new AdminUser("admin", passwordEncoder.encode("secret"), "系统管理员", true))
                .getId();

        adminBearer = "Bearer " + login("admin", "secret");
    }

    @Test
    void listsDepartmentsWithFilterAndPagination() throws Exception {
        departmentRepository.save(new Department("研发部", 1));
        departmentRepository.save(new Department("组织部", 2));
        departmentRepository.save(new Department("组织处", 3));

        mockMvc.perform(get("/api/admin/departments")
                        .param("page", "1")
                        .param("pageSize", "2")
                        .param("name", "组织")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].name").value("组织部"))
                .andExpect(jsonPath("$.data.items[0].sortOrder").value(2))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.items[1].name").value("组织处"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2));
    }

    @Test
    void createsDepartment() throws Exception {
        mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"组织部","sortOrder":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("组织部"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.createdAt").isString());
    }

    @Test
    void rejectsDuplicateDepartmentName() throws Exception {
        departmentRepository.save(new Department("组织部", 9));

        mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"组织部","sortOrder":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门名称已存在"));
    }

    @Test
    void updatesDepartment() throws Exception {
        Long departmentId = departmentRepository.save(new Department("组织部", 1)).getId();

        mockMvc.perform(put("/api/admin/departments/{id}", departmentId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"综合部","sortOrder":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("综合部"))
                .andExpect(jsonPath("$.data.sortOrder").value(5));
    }

    @Test
    void rejectsUpdatingDepartmentWithDuplicateName() throws Exception {
        departmentRepository.save(new Department("组织部", 1));
        Long departmentId = departmentRepository.save(new Department("综合部", 2)).getId();

        mockMvc.perform(put("/api/admin/departments/{id}", departmentId)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"组织部","sortOrder":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门名称已存在"));
    }

    @Test
    void returnsDepartmentNotFoundWhenUpdatingMissingDepartmentEvenIfNameExists() throws Exception {
        departmentRepository.save(new Department("组织部", 1));

        mockMvc.perform(put("/api/admin/departments/{id}", 999999L)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"组织部","sortOrder":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    void rejectsDeletingDepartmentWithEvaluatees() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        evaluateeRepository.save(new Evaluatee("张三", department, true));

        mockMvc.perform(delete("/api/admin/departments/{id}", department.getId())
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门下存在关联数据，不能删除"));
    }

    @Test
    void rejectsDeletingDepartmentWithEvaluatorUsers() throws Exception {
        Department department = departmentRepository.save(new Department("组织部", 1));
        evaluatorUserRepository.save(new EvaluatorUser("eva-1", "P12345", department, true));

        mockMvc.perform(delete("/api/admin/departments/{id}", department.getId())
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门下存在关联数据，不能删除"));
    }

    @Test
    void rejectsDeletingDepartmentWithEvaluationRecords() throws Exception {
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

        mockMvc.perform(delete("/api/admin/departments/{id}", department.getId())
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门下存在关联数据，不能删除"));
    }

    @Test
    void deletesDepartmentWithoutRelations() throws Exception {
        Long departmentId = departmentRepository.save(new Department("组织部", 1)).getId();

        mockMvc.perform(delete("/api/admin/departments/{id}", departmentId)
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void departmentApisRejectDisabledAdminAfterTokenIssued() throws Exception {
        AdminUser adminUser = adminUserRepository.findById(adminId).orElseThrow();
        adminUser.updateEnabled(false);
        adminUserRepository.saveAndFlush(adminUser);

        mockMvc.perform(get("/api/admin/departments")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", adminBearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"组织部","sortOrder":1}
                                """))
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
