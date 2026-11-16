package com.smarteval.schema;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.department.Department;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.evaluation.EvaluationRecord;
import com.smarteval.evaluatoruser.EvaluatorUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationTest extends PostgresIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @PersistenceContext
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from evaluation_record");
        jdbcTemplate.update("delete from evaluator_user");
        jdbcTemplate.update("delete from evaluatee");
        jdbcTemplate.update("delete from admin_user");
        jdbcTemplate.update("delete from department");
    }

    @Test
    void createsRequiredSchemaSurface() {
        assertTableExists("admin_user");
        assertTableExists("department");
        assertTableExists("evaluatee");
        assertTableExists("evaluator_user");
        assertTableExists("evaluation_record");

        assertColumnExists("admin_user", "password_hash");
        assertColumnExists("evaluator_user", "password_plain");
        assertColumnExists("evaluator_user", "submitted_at");
        assertColumnExists("evaluation_record", "comment1");
        assertColumnExists("evaluation_record", "comment2");
        assertColumnMissing("department", "evaluation_period_id");

        assertUniqueConstraintExists("admin_user", "uk_admin_user_username", "username");
        assertUniqueConstraintExists("department", "uk_department_name", "name");
        assertUniqueConstraintExists("evaluator_user", "uk_evaluator_user_period_username",
                "evaluation_period_id", "username");
        assertUniqueConstraintExists("evaluation_record", "uk_evaluation_record_period_user_evaluatee",
                "evaluation_period_id", "evaluator_user_id", "evaluatee_id");

        assertForeignKeyExists("evaluatee", "department_id", "department", "id");
        assertForeignKeyExists("evaluator_user", "department_id", "department", "id");
        assertForeignKeyExists("evaluation_record", "evaluator_user_id", "evaluator_user", "id");
        assertForeignKeyExists("evaluation_record", "evaluatee_id", "evaluatee", "id");
        assertForeignKeyExists("evaluation_record", "department_id", "department", "id");

        assertGradeCheckConstraintExists();
    }

    @Test
    @Transactional
    void persistsEntitiesWithoutManuallySettingAuditTimestamps() {
        Department department = new Department("Engineering", 1);
        entityManager.persist(department);

        AdminUser adminUser = new AdminUser("schema-admin", "hash", "Administrator", true);
        entityManager.persist(adminUser);

        Evaluatee evaluatee = new Evaluatee("Alice", department, true);
        entityManager.persist(evaluatee);

        EvaluatorUser evaluatorUser = new EvaluatorUser("eva-1", "plain", department, true, false, null);
        entityManager.persist(evaluatorUser);

        EvaluationRecord evaluationRecord = new EvaluationRecord(
                evaluatorUser,
                evaluatee,
                department,
                EvaluationGrade.COMPETENT,
                "steady output",
                "good collaborator"
        );
        entityManager.persist(evaluationRecord);
        entityManager.flush();

        assertThat(adminUser.getCreatedAt()).isNotNull();
        assertThat(adminUser.getUpdatedAt()).isNotNull();
        assertThat(department.getCreatedAt()).isNotNull();
        assertThat(department.getUpdatedAt()).isNotNull();
        assertThat(evaluatee.getCreatedAt()).isNotNull();
        assertThat(evaluatee.getUpdatedAt()).isNotNull();
        assertThat(evaluatorUser.getCreatedAt()).isNotNull();
        assertThat(evaluatorUser.getUpdatedAt()).isNotNull();
        assertThat(evaluationRecord.getCreatedAt()).isNotNull();
    }

    private void assertTableExists(String tableName) {
        assertThat(count("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = ?
                """, tableName)).isEqualTo(1);
    }

    private void assertColumnExists(String tableName, String columnName) {
        assertThat(count("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """, tableName, columnName)).isEqualTo(1);
    }

    private void assertColumnMissing(String tableName, String columnName) {
        assertThat(count("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """, tableName, columnName)).isZero();
    }

    private void assertUniqueConstraintExists(String tableName, String constraintName, String... columnNames) {
        assertThat(jdbcTemplate.queryForList("""
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                 and tc.table_name = kcu.table_name
                where tc.table_schema = 'public'
                  and tc.table_name = ?
                  and tc.constraint_name = ?
                  and tc.constraint_type = 'UNIQUE'
                order by kcu.ordinal_position
                """, String.class, tableName, constraintName))
                .containsExactly(columnNames);
    }

    private void assertForeignKeyExists(String tableName, String columnName, String foreignTable, String foreignColumn) {
        assertThat(count("""
                select count(*)
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                join information_schema.constraint_column_usage ccu
                  on tc.constraint_name = ccu.constraint_name
                 and tc.table_schema = ccu.table_schema
                where tc.constraint_type = 'FOREIGN KEY'
                  and tc.table_schema = 'public'
                  and tc.table_name = ?
                  and kcu.column_name = ?
                  and ccu.table_name = ?
                  and ccu.column_name = ?
                """, tableName, columnName, foreignTable, foreignColumn)).isEqualTo(1);
    }

    private void assertGradeCheckConstraintExists() {
        assertThat(count("""
                select count(*)
                from pg_constraint con
                join pg_class rel on rel.oid = con.conrelid
                join pg_namespace nsp on nsp.oid = rel.relnamespace
                where nsp.nspname = 'public'
                  and rel.relname = 'evaluation_record'
                  and con.conname = 'ck_evaluation_record_grade'
                  and pg_get_constraintdef(con.oid) = 'CHECK (((grade)::text = ANY ((ARRAY[''EXCELLENT''::character varying, ''COMPETENT''::character varying, ''BASIC_COMPETENT''::character varying, ''INCOMPETENT''::character varying])::text[])))'
                """)).isEqualTo(1);
    }

    private int count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
