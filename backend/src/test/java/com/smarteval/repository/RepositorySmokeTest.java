package com.smarteval.repository;

import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RepositorySmokeTest extends PostgresIntegrationTest {
    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from evaluation_record");
        jdbcTemplate.update("delete from evaluator_user");
        jdbcTemplate.update("delete from evaluatee");
        jdbcTemplate.update("delete from admin_user");
        jdbcTemplate.update("delete from department");
    }

    @Test
    void persistsDepartment() {
        Department department = departmentRepository.save(new Department("组织部", 1));

        assertThat(department.getId()).isNotNull();
        assertThat(departmentRepository.existsByName("组织部")).isTrue();
    }
}
