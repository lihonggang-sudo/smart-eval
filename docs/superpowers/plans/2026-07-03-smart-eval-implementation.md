# Smart Eval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable version of the高校处级干部测评系统 with administrator management, mobile evaluation, printable evaluator credentials, traceable results, and PostgreSQL persistence.

**Architecture:** Use a monorepo with `backend/` for Spring Boot REST APIs and `frontend/` for a React application based on Ant Design Pro. The frontend contains administrator routes under `/admin/*` with the Pro layout and mobile evaluator routes under `/mobile/*` with a separate lightweight layout. PostgreSQL is the system of record; Flyway owns schema creation and test data.

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Testcontainers, React, TypeScript, Ant Design Pro, Ant Design, Vitest, Playwright.

## Global Constraints

- Use Spring Boot + React + PostgreSQL.
- Use Java 17 for backend compilation and tests.
- Administrator frontend uses Ant Design Pro.
- Mobile evaluator frontend uses React independent mobile routes and does not use the Ant Design Pro desktop layout.
- Administrator passwords are saved as BCrypt hashes.
- Evaluator user passwords are saved in plaintext in `evaluator_user.password_plain`.
- Evaluator plaintext passwords are visible only through administrator APIs and print HTML; mobile APIs never return passwords.
- Evaluation grades are fixed: `EXCELLENT`, `COMPETENT`, `BASIC_COMPETENT`, `INCOMPETENT`.
- Evaluation users only evaluate enabled evaluatees in their own department.
- Evaluation users must evaluate every enabled evaluatee in their department before submitting.
- Evaluation submission is one-time and locked after success.
- The first version does not include evaluation batches, imports, Excel export, chart dashboards, evaluator password change, configurable indicators, or anonymous evaluation.
- List APIs use `page` and `pageSize`.
- API timestamps use ISO 8601 strings.
- All task commits must avoid unrelated workspace files, including `.idea/` and `docs/需求描述.md`.

---

## Target File Structure

Create this structure during the tasks:

```text
smart-eval/
  backend/
    pom.xml
    src/main/java/com/smarteval/
      SmartEvalApplication.java
      common/
        ApiResponse.java
        PageResponse.java
        BusinessException.java
        GlobalExceptionHandler.java
      security/
        CurrentUser.java
        JwtService.java
        SecurityConfig.java
        TokenAuthenticationFilter.java
        UserRole.java
      auth/
        AuthController.java
        AuthService.java
        dto/
      adminuser/
      department/
      evaluatee/
      evaluatoruser/
      evaluation/
      result/
      print/
    src/main/resources/
      application.yml
      db/migration/V1__init_schema.sql
    src/test/java/com/smarteval/
      support/
      common/
      auth/
      department/
      evaluatee/
      evaluatoruser/
      evaluation/
      result/
  frontend/
    package.json
    vite.config.ts
    src/
      app.tsx
      routes.ts
      request.ts
      access.ts
      services/
      pages/
        admin/
        mobile/
      components/
      tests/
```

Back-end package responsibility:

- `common`: response envelope, pagination envelope, business exceptions, validation error formatting.
- `security`: JWT issuing/parsing, Spring Security filters, current user object, role model.
- `auth`: administrator and evaluator login plus `me` endpoints.
- `adminuser`: administrator account CRUD and password reset.
- `department`: department CRUD and delete guards.
- `evaluatee`: evaluatee CRUD and enabled-state changes.
- `evaluatoruser`: batch generation, plaintext password storage, reset, enabled-state changes.
- `evaluation`: mobile evaluation item list and one-time submission.
- `result`: summary and traceable detail queries.
- `print`: administrator-only printable HTML credentials.

Front-end responsibility:

- `services`: typed API clients with the same DTO names as backend responses.
- `pages/admin`: Ant Design Pro administrator pages.
- `pages/mobile`: mobile evaluator pages with independent layout.
- `components`: shared widgets that are not page-specific.

---

### Task 1: Backend Project Scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/smarteval/SmartEvalApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/smarteval/SmokeTest.java`
- Create: `backend/src/test/resources/application-test.yml`

**Interfaces:**
- Produces: Maven project runnable with `./mvnw test` or `mvn test`.
- Produces: Spring application class `com.smarteval.SmartEvalApplication`.

- [ ] **Step 1: Write the failing smoke test**

Create `backend/src/test/java/com/smarteval/SmokeTest.java`:

```java
package com.smarteval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmokeTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run the test to verify the project is not scaffolded**

Run:

```bash
cd backend
mvn test -Dtest=SmokeTest
```

Expected: FAIL because `pom.xml` or the application class does not exist.

- [ ] **Step 3: Add the Maven project files**

Create `backend/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>com.smarteval</groupId>
    <artifactId>smart-eval-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>smart-eval-backend</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.auth0</groupId>
            <artifactId>java-jwt</artifactId>
            <version>4.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Create `backend/src/main/java/com/smarteval/SmartEvalApplication.java`:

```java
package com.smarteval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartEvalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartEvalApplication.class, args);
    }
}
```

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: smart-eval
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_eval
    username: smart_eval
    password: smart_eval
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

app:
  jwt:
    issuer: smart-eval
    secret: change-this-secret-in-production-change-this-secret
    expires-in-minutes: 720
  frontend:
    mobile-url: http://localhost:5173/mobile/login
```

Create `backend/src/test/resources/application-test.yml`:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

Update `backend/src/test/java/com/smarteval/SmokeTest.java` to use the test profile:

```java
package com.smarteval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmokeTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Run the smoke test**

Run:

```bash
cd backend
mvn test -Dtest=SmokeTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/smarteval/SmartEvalApplication.java backend/src/main/resources/application.yml backend/src/test/resources/application-test.yml backend/src/test/java/com/smarteval/SmokeTest.java
git commit -m "chore: scaffold Spring Boot backend"
```

---

### Task 2: Database Schema and JPA Entities

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/java/com/smarteval/adminuser/AdminUser.java`
- Create: `backend/src/main/java/com/smarteval/department/Department.java`
- Create: `backend/src/main/java/com/smarteval/evaluatee/Evaluatee.java`
- Create: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUser.java`
- Create: `backend/src/main/java/com/smarteval/evaluation/EvaluationGrade.java`
- Create: `backend/src/main/java/com/smarteval/evaluation/EvaluationRecord.java`
- Create: `backend/src/test/java/com/smarteval/support/PostgresIntegrationTest.java`
- Create: `backend/src/test/java/com/smarteval/schema/SchemaMigrationTest.java`

**Interfaces:**
- Produces: table names and columns from the approved spec.
- Produces: enum `EvaluationGrade` with method `label(): String`.
- Produces: JPA entities used by all repository tasks.

- [ ] **Step 1: Write the failing schema migration test**

Create `backend/src/test/java/com/smarteval/support/PostgresIntegrationTest.java`:

```java
package com.smarteval.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public abstract class PostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("smart_eval_test")
            .withUsername("smart_eval")
            .withPassword("smart_eval");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Create `backend/src/test/java/com/smarteval/schema/SchemaMigrationTest.java`:

```java
package com.smarteval.schema;

import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationTest extends PostgresIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsEvaluationRecordWithCommentsAndGradeCheck() {
        Integer commentColumnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_name = 'evaluation_record'
                  and column_name in ('comment1', 'comment2')
                """, Integer.class);

        Integer gradeCheckCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.constraint_column_usage
                where table_name = 'evaluation_record'
                  and column_name = 'grade'
                """, Integer.class);

        assertThat(commentColumnCount).isEqualTo(2);
        assertThat(gradeCheckCount).isGreaterThanOrEqualTo(1);
    }
}
```

- [ ] **Step 2: Run the schema test to verify it fails**

Run:

```bash
cd backend
mvn test -Dtest=SchemaMigrationTest
```

Expected: FAIL because the migration and entities do not exist.

- [ ] **Step 3: Add schema migration**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
create table admin_user (
    id bigserial primary key,
    username varchar(64) not null,
    password_hash varchar(255) not null,
    display_name varchar(64) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_admin_user_username unique (username)
);

create table department (
    id bigserial primary key,
    name varchar(128) not null,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_department_name unique (name)
);

create table evaluatee (
    id bigserial primary key,
    name varchar(64) not null,
    department_id bigint not null references department(id),
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_evaluatee_department_id on evaluatee(department_id);

create table evaluator_user (
    id bigserial primary key,
    username varchar(64) not null,
    password_plain varchar(64) not null,
    department_id bigint not null references department(id),
    enabled boolean not null default true,
    submitted boolean not null default false,
    submitted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_evaluator_user_username unique (username)
);

create index idx_evaluator_user_department_id on evaluator_user(department_id);
create index idx_evaluator_user_submitted on evaluator_user(submitted);

create table evaluation_record (
    id bigserial primary key,
    evaluator_user_id bigint not null references evaluator_user(id),
    evaluatee_id bigint not null references evaluatee(id),
    department_id bigint not null references department(id),
    grade varchar(32) not null,
    comment1 varchar(255),
    comment2 varchar(255),
    created_at timestamptz not null default now(),
    constraint uk_evaluation_record_user_evaluatee unique (evaluator_user_id, evaluatee_id),
    constraint ck_evaluation_record_grade check (grade in ('EXCELLENT', 'COMPETENT', 'BASIC_COMPETENT', 'INCOMPETENT'))
);

create index idx_evaluation_record_evaluatee_id on evaluation_record(evaluatee_id);
create index idx_evaluation_record_evaluator_user_id on evaluation_record(evaluator_user_id);
create index idx_evaluation_record_department_id on evaluation_record(department_id);
```

- [ ] **Step 4: Add the entities**

Create `backend/src/main/java/com/smarteval/evaluation/EvaluationGrade.java`:

```java
package com.smarteval.evaluation;

public enum EvaluationGrade {
    EXCELLENT("优秀"),
    COMPETENT("称职"),
    BASIC_COMPETENT("基本称职"),
    INCOMPETENT("不称职");

    private final String label;

    EvaluationGrade(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
```

Create the five entity classes using JPA annotations with table names matching the migration. Each entity must include `id`, all columns from the migration, public getters, package-private no-argument constructor, and explicit constructors for tests. `EvaluationRecord.grade` must use `@Enumerated(EnumType.STRING)`.

The `EvaluationRecord` constructor signature used by later tasks is:

```java
public EvaluationRecord(EvaluatorUser evaluatorUser, Evaluatee evaluatee, Department department,
                        EvaluationGrade grade, String comment1, String comment2)
```

- [ ] **Step 5: Run the schema test**

Run:

```bash
cd backend
mvn test -Dtest=SchemaMigrationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V1__init_schema.sql backend/src/main/java/com/smarteval backend/src/test/java/com/smarteval
git commit -m "feat: add database schema and entities"
```

---

### Task 3: Common API Envelope, Errors, and Security Foundation

**Files:**
- Create: `backend/src/main/java/com/smarteval/common/ApiResponse.java`
- Create: `backend/src/main/java/com/smarteval/common/PageResponse.java`
- Create: `backend/src/main/java/com/smarteval/common/BusinessException.java`
- Create: `backend/src/main/java/com/smarteval/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/smarteval/security/UserRole.java`
- Create: `backend/src/main/java/com/smarteval/security/CurrentUser.java`
- Create: `backend/src/main/java/com/smarteval/security/JwtService.java`
- Create: `backend/src/main/java/com/smarteval/security/TokenAuthenticationFilter.java`
- Create: `backend/src/main/java/com/smarteval/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/smarteval/security/JwtServiceTest.java`

**Interfaces:**
- Produces: `ApiResponse<T>.ok(T data)` and `ApiResponse.fail(String message)`.
- Produces: `BusinessException(String message)` for business-rule failures.
- Produces: `JwtService.issue(long userId, UserRole role, String username)` and `JwtService.parse(String token)`.
- Produces: authenticated principal type `CurrentUser(long id, UserRole role, String username)`.

- [ ] **Step 1: Write the failing JWT test**

Create `backend/src/test/java/com/smarteval/security/JwtServiceTest.java`:

```java
package com.smarteval.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void issuesAndParsesRoleScopedToken() {
        JwtService jwtService = new JwtService("test-issuer", "01234567890123456789012345678901", 60);

        String token = jwtService.issue(7L, UserRole.ADMIN, "admin");
        CurrentUser user = jwtService.parse(token);

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.role()).isEqualTo(UserRole.ADMIN);
        assertThat(user.username()).isEqualTo("admin");
    }
}
```

- [ ] **Step 2: Run the JWT test to verify it fails**

Run:

```bash
cd backend
mvn test -Dtest=JwtServiceTest
```

Expected: FAIL because `JwtService`, `CurrentUser`, and `UserRole` do not exist.

- [ ] **Step 3: Add common response classes**

Create `ApiResponse` and `PageResponse`:

```java
package com.smarteval.common;

import java.util.Map;

public record ApiResponse<T>(boolean success, T data, String message, Map<String, String> errors) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "", Map.of());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, Map.of());
    }

    public static <T> ApiResponse<T> fail(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }
}
```

```java
package com.smarteval.common;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int pageSize) {
}
```

- [ ] **Step 4: Add business exception and exception handler**

Create `BusinessException`:

```java
package com.smarteval.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

Create `GlobalExceptionHandler` with handlers for `BusinessException`, `MethodArgumentNotValidException`, `AccessDeniedException`, and uncaught `Exception`. Business exceptions return HTTP 400 with `ApiResponse.fail(message)`. Validation errors return field errors in the `errors` map. Access denied returns HTTP 403.

- [ ] **Step 5: Add JWT and security classes**

Create:

```java
package com.smarteval.security;

public enum UserRole {
    ADMIN,
    EVALUATOR
}
```

```java
package com.smarteval.security;

public record CurrentUser(long id, UserRole role, String username) {
}
```

Implement `JwtService` with Auth0 Java JWT. Use claims `uid`, `role`, and `username`; validate issuer and expiry.

Implement `TokenAuthenticationFilter` to read `Authorization: Bearer <token>`, parse it, and store a `UsernamePasswordAuthenticationToken` whose principal is `CurrentUser`.

Implement `SecurityConfig`:

```java
package com.smarteval.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter tokenFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/auth/login", "/api/mobile/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/mobile/**").hasAuthority("EVALUATOR")
                        .anyRequest().authenticated())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 6: Run JWT and smoke tests**

Run:

```bash
cd backend
mvn test -Dtest=JwtServiceTest,SmokeTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/smarteval/common backend/src/main/java/com/smarteval/security backend/src/test/java/com/smarteval/security
git commit -m "feat: add API envelope and security foundation"
```

---

### Task 4: Repositories and Test Data Builders

**Files:**
- Create: `backend/src/main/java/com/smarteval/adminuser/AdminUserRepository.java`
- Create: `backend/src/main/java/com/smarteval/department/DepartmentRepository.java`
- Create: `backend/src/main/java/com/smarteval/evaluatee/EvaluateeRepository.java`
- Create: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserRepository.java`
- Create: `backend/src/main/java/com/smarteval/evaluation/EvaluationRecordRepository.java`
- Create: `backend/src/test/java/com/smarteval/support/TestData.java`
- Create: `backend/src/test/java/com/smarteval/repository/RepositorySmokeTest.java`

**Interfaces:**
- Produces: repository methods used by services:
  - `AdminUserRepository.findByUsername(String username)`
  - `DepartmentRepository.existsByName(String name)`
  - `EvaluateeRepository.findByDepartmentIdAndEnabledTrueOrderByIdAsc(Long departmentId)`
  - `EvaluatorUserRepository.findByUsername(String username)`
  - `EvaluationRecordRepository.existsByEvaluatorUserId(Long evaluatorUserId)`
  - `EvaluationRecordRepository.countByDepartmentId(Long departmentId)`

- [ ] **Step 1: Write the failing repository smoke test**

Create `backend/src/test/java/com/smarteval/repository/RepositorySmokeTest.java`:

```java
package com.smarteval.repository;

import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RepositorySmokeTest extends PostgresIntegrationTest {
    @Autowired
    DepartmentRepository departmentRepository;

    @Test
    void persistsDepartment() {
        Department department = departmentRepository.save(new Department("组织部", 1));

        assertThat(department.getId()).isNotNull();
        assertThat(departmentRepository.existsByName("组织部")).isTrue();
    }
}
```

- [ ] **Step 2: Run the repository smoke test to verify it fails**

Run:

```bash
cd backend
mvn test -Dtest=RepositorySmokeTest
```

Expected: FAIL because repositories are missing.

- [ ] **Step 3: Add repository interfaces**

Create repository interfaces extending `JpaRepository`. The minimum required signatures are:

```java
Optional<AdminUser> findByUsername(String username);
boolean existsByUsername(String username);
```

```java
boolean existsByName(String name);
```

```java
List<Evaluatee> findByDepartmentIdAndEnabledTrueOrderByIdAsc(Long departmentId);
long countByDepartmentId(Long departmentId);
```

```java
Optional<EvaluatorUser> findByUsername(String username);
boolean existsByUsername(String username);
long countByDepartmentId(Long departmentId);
long countBySubmittedTrue();
```

```java
boolean existsByEvaluatorUserId(Long evaluatorUserId);
long countByDepartmentId(Long departmentId);
```

- [ ] **Step 4: Add test data helper**

Create `backend/src/test/java/com/smarteval/support/TestData.java` with helper methods:

```java
package com.smarteval.support;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.department.Department;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatoruser.EvaluatorUser;

public final class TestData {
    private TestData() {
    }

    public static Department department(String name) {
        return new Department(name, 0);
    }

    public static Evaluatee evaluatee(String name, Department department) {
        return new Evaluatee(name, department, true);
    }

    public static EvaluatorUser evaluator(String username, Department department) {
        return new EvaluatorUser(username, "P12345", department, true);
    }

    public static AdminUser admin(String username, String passwordHash) {
        return new AdminUser(username, passwordHash, "管理员", true);
    }
}
```

- [ ] **Step 5: Run repository tests**

Run:

```bash
cd backend
mvn test -Dtest=RepositorySmokeTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/smarteval/*/*Repository.java backend/src/test/java/com/smarteval/support backend/src/test/java/com/smarteval/repository
git commit -m "feat: add repositories and test data helpers"
```

---

### Task 5: Authentication and Administrator Account APIs

**Files:**
- Create: `backend/src/main/java/com/smarteval/auth/AuthController.java`
- Create: `backend/src/main/java/com/smarteval/auth/AuthService.java`
- Create: `backend/src/main/java/com/smarteval/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/smarteval/auth/dto/LoginResponse.java`
- Create: `backend/src/main/java/com/smarteval/auth/dto/AdminMeResponse.java`
- Create: `backend/src/main/java/com/smarteval/auth/dto/EvaluatorMeResponse.java`
- Create: `backend/src/main/java/com/smarteval/adminuser/AdminUserController.java`
- Create: `backend/src/main/java/com/smarteval/adminuser/AdminUserService.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/adminuser/dto/`
- Create: `backend/src/test/java/com/smarteval/auth/AuthControllerTest.java`
- Create: `backend/src/test/java/com/smarteval/adminuser/AdminUserControllerTest.java`

**Interfaces:**
- Produces:
  - `POST /api/admin/auth/login`
  - `GET /api/admin/me`
  - `POST /api/mobile/auth/login`
  - `GET /api/mobile/me`
  - `GET /api/admin/admin-users`
  - `POST /api/admin/admin-users`
  - `PUT /api/admin/admin-users/{id}`
  - `PUT /api/admin/admin-users/{id}/enabled`
  - `POST /api/admin/admin-users/{id}/reset-password`

- [ ] **Step 1: Write failing authentication API tests**

Create tests that seed one enabled admin and one enabled evaluator. Verify:

```java
mockMvc.perform(post("/api/admin/auth/login")
        .contentType(APPLICATION_JSON)
        .content("""
                {"username":"admin","password":"secret"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.token").isString())
        .andExpect(jsonPath("$.data.user.username").value("admin"));
```

Also verify evaluator login response contains `departmentName` and `submitted`, and does not contain `passwordPlain`.

- [ ] **Step 2: Run the authentication tests to verify they fail**

Run:

```bash
cd backend
mvn test -Dtest=AuthControllerTest
```

Expected: FAIL because authentication endpoints are missing.

- [ ] **Step 3: Implement authentication DTOs and service**

Use these DTO shapes:

```java
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
```

```java
public record LoginResponse(String token, Object user) {
}
```

```java
public record AdminMeResponse(Long id, String username, String displayName) {
}
```

```java
public record EvaluatorMeResponse(Long id, String username, Long departmentId,
                                  String departmentName, boolean submitted) {
}
```

`AuthService.adminLogin` verifies BCrypt admin password. `AuthService.evaluatorLogin` compares submitted password to `EvaluatorUser.passwordPlain`. Both reject disabled accounts with the same message as wrong credentials: `用户名或密码错误`.

- [ ] **Step 4: Implement controllers**

`AuthController` exposes the four auth endpoints. Use `@AuthenticationPrincipal CurrentUser currentUser` for `me` endpoints and return `ApiResponse.ok(...)`.

- [ ] **Step 5: Write failing administrator account tests**

Verify administrator account creation hashes passwords and disabling the last enabled admin is rejected:

```java
mockMvc.perform(put("/api/admin/admin-users/{id}/enabled", onlyAdminId)
        .header("Authorization", adminBearer)
        .contentType(APPLICATION_JSON)
        .content("""
                {"enabled":false}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("不能禁用最后一个启用的管理员"));
```

- [ ] **Step 6: Implement administrator account service and controller**

DTO names used by frontend:

```java
public record AdminUserListItem(Long id, String username, String displayName,
                                boolean enabled, Instant createdAt) {
}
public record CreateAdminUserRequest(@NotBlank String username, @NotBlank String password,
                                     @NotBlank String displayName) {
}
public record UpdateAdminUserRequest(@NotBlank String displayName) {
}
public record SetEnabledRequest(boolean enabled) {
}
public record ResetPasswordRequest(@NotBlank String password) {
}
```

Hash every administrator password with `PasswordEncoder`. Use `BusinessException` for duplicate username and last-admin disable.

- [ ] **Step 7: Run authentication and admin account tests**

Run:

```bash
cd backend
mvn test -Dtest=AuthControllerTest,AdminUserControllerTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/smarteval/auth backend/src/main/java/com/smarteval/adminuser backend/src/test/java/com/smarteval/auth backend/src/test/java/com/smarteval/adminuser
git commit -m "feat: add authentication and admin user APIs"
```

---

### Task 6: Department Management API

**Files:**
- Create: `backend/src/main/java/com/smarteval/department/DepartmentController.java`
- Create: `backend/src/main/java/com/smarteval/department/DepartmentService.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/department/dto/`
- Create: `backend/src/test/java/com/smarteval/department/DepartmentControllerTest.java`

**Interfaces:**
- Produces:
  - `GET /api/admin/departments?page=1&pageSize=20&name=组织`
  - `POST /api/admin/departments`
  - `PUT /api/admin/departments/{id}`
  - `DELETE /api/admin/departments/{id}`

- [ ] **Step 1: Write failing department API tests**

Test create, list, duplicate name rejection, and delete guard when a department has evaluatees.

Duplicate test assertion:

```java
mockMvc.perform(post("/api/admin/departments")
        .header("Authorization", adminBearer)
        .contentType(APPLICATION_JSON)
        .content("""
                {"name":"组织部","sortOrder":1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("部门名称已存在"));
```

- [ ] **Step 2: Run the department tests to verify they fail**

Run:

```bash
cd backend
mvn test -Dtest=DepartmentControllerTest
```

Expected: FAIL because the endpoints are missing.

- [ ] **Step 3: Implement DTOs**

```java
public record DepartmentRequest(@NotBlank String name, int sortOrder) {
}
public record DepartmentListItem(Long id, String name, int sortOrder, Instant createdAt) {
}
```

- [ ] **Step 4: Implement service and controller**

Service behavior:

- Create rejects existing department names.
- Update rejects duplicate name when another row owns it.
- Delete rejects when `evaluateeRepository.countByDepartmentId(id) > 0`, `evaluatorUserRepository.countByDepartmentId(id) > 0`, or `evaluationRecordRepository.countByDepartmentId(id) > 0`.
- List returns `PageResponse<DepartmentListItem>` sorted by `sortOrder asc, id asc`.

- [ ] **Step 5: Run department tests**

Run:

```bash
cd backend
mvn test -Dtest=DepartmentControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/smarteval/department backend/src/test/java/com/smarteval/department
git commit -m "feat: add department management API"
```

---

### Task 7: Evaluatee Management API

**Files:**
- Create: `backend/src/main/java/com/smarteval/evaluatee/EvaluateeController.java`
- Create: `backend/src/main/java/com/smarteval/evaluatee/EvaluateeService.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/evaluatee/dto/`
- Create: `backend/src/test/java/com/smarteval/evaluatee/EvaluateeControllerTest.java`

**Interfaces:**
- Produces:
  - `GET /api/admin/evaluatees?page=1&pageSize=20&departmentId=1&name=张&enabled=true`
  - `POST /api/admin/evaluatees`
  - `PUT /api/admin/evaluatees/{id}`
  - `DELETE /api/admin/evaluatees/{id}`
  - `PUT /api/admin/evaluatees/{id}/enabled`

- [ ] **Step 1: Write failing evaluatee API tests**

Verify create, list by department, disable, and delete behavior. If an evaluatee has evaluation records, `DELETE` sets `enabled=false` instead of removing the row.

- [ ] **Step 2: Run the evaluatee tests to verify they fail**

Run:

```bash
cd backend
mvn test -Dtest=EvaluateeControllerTest
```

Expected: FAIL because the endpoints are missing.

- [ ] **Step 3: Implement DTOs**

```java
public record EvaluateeRequest(@NotBlank String name, @NotNull Long departmentId, boolean enabled) {
}
public record EvaluateeListItem(Long id, String name, Long departmentId, String departmentName,
                                boolean enabled, Instant createdAt) {
}
public record SetEnabledRequest(boolean enabled) {
}
```

- [ ] **Step 4: Implement service and controller**

Service behavior:

- Create and update require an existing department.
- List supports filters `departmentId`, `name`, and `enabled`.
- Enabled-state endpoint updates only `enabled`.
- Delete removes the row when no evaluation record exists; otherwise sets `enabled=false`.

- [ ] **Step 5: Run evaluatee tests**

Run:

```bash
cd backend
mvn test -Dtest=EvaluateeControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/smarteval/evaluatee backend/src/test/java/com/smarteval/evaluatee
git commit -m "feat: add evaluatee management API"
```

---

### Task 8: Evaluator User Batch Generation and Print HTML

**Files:**
- Create: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserController.java`
- Create: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java`
- Create: `backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/evaluatoruser/dto/`
- Create: `backend/src/main/java/com/smarteval/print/PrintController.java`
- Create: `backend/src/main/java/com/smarteval/print/PrintCredentialService.java`
- Create: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`
- Create: `backend/src/test/java/com/smarteval/print/PrintCredentialServiceTest.java`

**Interfaces:**
- Produces:
  - `GET /api/admin/evaluator-users`
  - `POST /api/admin/evaluator-users/batch-generate`
  - `POST /api/admin/evaluator-users/{id}/reset-password`
  - `PUT /api/admin/evaluator-users/{id}/enabled`
  - `GET /api/admin/evaluator-users/print`

- [ ] **Step 1: Write failing credential generator test**

Test that generated passwords are six uppercase letters/digits and generated usernames are unique for a batch:

```java
RandomCredentialGenerator generator = new RandomCredentialGenerator();
List<String> usernames = generator.usernames("D001", 20);
assertThat(usernames).hasSize(20).doesNotHaveDuplicates();
assertThat(generator.password()).matches("[A-Z0-9]{6}");
```

- [ ] **Step 2: Run the credential test to verify it fails**

Run:

```bash
cd backend
mvn test -Dtest=EvaluatorUserControllerTest
```

Expected: FAIL because the evaluator user module is missing.

- [ ] **Step 3: Implement DTOs**

```java
public record BatchGenerateEvaluatorRequest(@NotNull Long departmentId,
                                            @Min(1) @Max(500) int count) {
}
public record EvaluatorUserListItem(Long id, String username, String passwordPlain,
                                    Long departmentId, String departmentName,
                                    boolean enabled, boolean submitted, Instant submittedAt,
                                    Instant createdAt) {
}
public record BatchGenerateEvaluatorResponse(int created, List<EvaluatorUserListItem> items) {
}
public record ResetEvaluatorPasswordResponse(String passwordPlain) {
}
public record SetEnabledRequest(boolean enabled) {
}
```

- [ ] **Step 4: Implement batch generation**

Use `SecureRandom`. Username format is `D<departmentId padded to 3>-<sequence padded to 4>`, for example `D001-0001`. When a generated username already exists, increment the sequence until a free username is found.

- [ ] **Step 5: Implement list, reset, enabled-state APIs**

Service behavior:

- List supports filters `departmentId`, `submitted`, and `enabled`.
- Reset password creates a new plaintext password and saves it in `password_plain`.
- Enabled-state endpoint updates only `enabled`.
- Administrator APIs return `passwordPlain`; mobile APIs never use this DTO.

- [ ] **Step 6: Write failing print HTML test**

Verify HTML contains mobile URL, department name, username, and plaintext password:

```java
String html = printCredentialService.renderHtml(List.of(item), "http://localhost:5173/mobile/login");
assertThat(html).contains("http://localhost:5173/mobile/login");
assertThat(html).contains("组织部");
assertThat(html).contains("D001-0001");
assertThat(html).contains("A8K2P9");
```

- [ ] **Step 7: Implement print service and controller**

`GET /api/admin/evaluator-users/print` returns `text/html;charset=UTF-8`. The HTML contains a printable card per evaluator user and CSS `@media print`.

- [ ] **Step 8: Run evaluator user and print tests**

Run:

```bash
cd backend
mvn test -Dtest=EvaluatorUserControllerTest,PrintCredentialServiceTest
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/smarteval/evaluatoruser backend/src/main/java/com/smarteval/print backend/src/test/java/com/smarteval/evaluatoruser backend/src/test/java/com/smarteval/print
git commit -m "feat: add evaluator users and print credentials"
```

---

### Task 9: Mobile Evaluation Submission API

**Files:**
- Create: `backend/src/main/java/com/smarteval/evaluation/EvaluationController.java`
- Create: `backend/src/main/java/com/smarteval/evaluation/EvaluationService.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/evaluation/dto/`
- Create: `backend/src/test/java/com/smarteval/evaluation/EvaluationControllerTest.java`

**Interfaces:**
- Produces:
  - `GET /api/mobile/evaluation/items`
  - `POST /api/mobile/evaluation/submit`

- [ ] **Step 1: Write failing mobile item test**

Seed two departments, one evaluator in department A, one enabled evaluatee in department A, one enabled evaluatee in department B. Verify `/api/mobile/evaluation/items` returns only department A.

- [ ] **Step 2: Write failing submission tests**

Cover four cases:

- Success with all enabled department evaluatees.
- Failure when one enabled department evaluatee is missing.
- Failure when the request includes an evaluatee from another department.
- Failure when the evaluator has already submitted.

Missing evaluatee assertion:

```java
mockMvc.perform(post("/api/mobile/evaluation/submit")
        .header("Authorization", evaluatorBearer)
        .contentType(APPLICATION_JSON)
        .content("""
                {"records":[]}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("必须完成本部门全部人员评价后才能提交"));
```

- [ ] **Step 3: Run evaluation tests to verify they fail**

Run:

```bash
cd backend
mvn test -Dtest=EvaluationControllerTest
```

Expected: FAIL because mobile evaluation endpoints are missing.

- [ ] **Step 4: Implement DTOs**

```java
public record EvaluationItemResponse(Long departmentId, String departmentName,
                                     boolean submitted, List<EvaluateeItem> items,
                                     List<GradeItem> grades) {
}
public record EvaluateeItem(Long evaluateeId, String name) {
}
public record GradeItem(String value, String label) {
}
public record SubmitEvaluationRequest(@NotEmpty List<SubmitEvaluationRecord> records) {
}
public record SubmitEvaluationRecord(@NotNull Long evaluateeId, @NotNull EvaluationGrade grade,
                                     String comment1, String comment2) {
}
public record SubmitEvaluationResponse(boolean submitted, Instant submittedAt) {
}
```

- [ ] **Step 5: Implement item listing**

Return the current evaluator user's department, submission state, enabled evaluatees in that department, and all four grades with labels.

- [ ] **Step 6: Implement transactional submission**

Service rules:

- Reject if evaluator is disabled.
- Reject if `submitted=true`.
- Load enabled evaluatees for evaluator department.
- Compare expected evaluatee IDs to submitted evaluatee IDs.
- Reject missing IDs with message `必须完成本部门全部人员评价后才能提交`.
- Reject extra IDs with message `只能评价本部门启用人员`.
- Save one `EvaluationRecord` per item, including `comment1` and `comment2`.
- Set `evaluator_user.submitted=true` and `submitted_at=now`.
- Wrap the method in `@Transactional`.

- [ ] **Step 7: Run evaluation tests**

Run:

```bash
cd backend
mvn test -Dtest=EvaluationControllerTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/smarteval/evaluation backend/src/test/java/com/smarteval/evaluation
git commit -m "feat: add mobile evaluation submission"
```

---

### Task 10: Result Summary and Detail APIs

**Files:**
- Create: `backend/src/main/java/com/smarteval/result/ResultController.java`
- Create: `backend/src/main/java/com/smarteval/result/ResultService.java`
- Create: DTO classes under `backend/src/main/java/com/smarteval/result/dto/`
- Create: `backend/src/test/java/com/smarteval/result/ResultControllerTest.java`

**Interfaces:**
- Produces:
  - `GET /api/admin/results/summary`
  - `GET /api/admin/results/details`

- [ ] **Step 1: Write failing summary and detail tests**

Seed evaluation records with grades `EXCELLENT`, `COMPETENT`, and `INCOMPETENT` for one evaluatee. Verify counts and traceable detail fields:

```java
mockMvc.perform(get("/api/admin/results/summary")
        .header("Authorization", adminBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].excellentCount").value(1))
        .andExpect(jsonPath("$.data.items[0].competentCount").value(1))
        .andExpect(jsonPath("$.data.items[0].incompetentCount").value(1))
        .andExpect(jsonPath("$.data.items[0].totalCount").value(3));
```

- [ ] **Step 2: Run result tests to verify they fail**

Run:

```bash
cd backend
mvn test -Dtest=ResultControllerTest
```

Expected: FAIL because result endpoints are missing.

- [ ] **Step 3: Implement DTOs**

```java
public record ResultSummaryItem(Long evaluateeId, String evaluateeName, String departmentName,
                                long excellentCount, long competentCount,
                                long basicCompetentCount, long incompetentCount,
                                long totalCount) {
}
public record ResultDetailItem(Long evaluatorUserId, String evaluatorUsername,
                               String evaluatorDepartmentName, Long evaluateeId,
                               String evaluateeName, String grade, String gradeLabel,
                               String comment1, String comment2, Instant createdAt) {
}
```

- [ ] **Step 4: Implement queries**

Use repository JPQL constructor queries or Criteria API:

- Summary groups by evaluatee and department.
- Detail joins evaluator user, evaluator department, evaluatee, and record.
- Both endpoints return `PageResponse`.
- Filters: `departmentId`, `evaluateeName` for summary; `departmentId`, `evaluateeId`, `evaluatorUserId`, `grade` for detail.

- [ ] **Step 5: Run result tests**

Run:

```bash
cd backend
mvn test -Dtest=ResultControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/smarteval/result backend/src/test/java/com/smarteval/result
git commit -m "feat: add result summary and details"
```

---

### Task 11: Backend End-to-End Verification and Developer Runbook

**Files:**
- Create: `backend/src/test/java/com/smarteval/e2e/SmartEvalFlowTest.java`
- Create: `backend/README.md`
- Modify: `README.md`

**Interfaces:**
- Produces: one backend integration test covering administrator setup, evaluator login, submission, and result lookup.
- Produces: backend run commands.

- [ ] **Step 1: Write the full-flow failing test**

Create `SmartEvalFlowTest` that performs this HTTP flow with MockMvc:

1. Admin login.
2. Create department.
3. Create two evaluatees.
4. Batch-generate one evaluator.
5. Evaluator login with plaintext password from admin response.
6. Fetch mobile evaluation items.
7. Submit both grades.
8. Fetch result summary.
9. Fetch result details.

Assert result summary total count is `1` for each evaluatee and details include the evaluator username.

- [ ] **Step 2: Run the full-flow test**

Run:

```bash
cd backend
mvn test -Dtest=SmartEvalFlowTest
```

Expected: PASS after previous tasks are implemented.

- [ ] **Step 3: Add backend README**

Create `backend/README.md`:

```markdown
# Smart Eval Backend

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+

## Local Database

Create a database named `smart_eval` and a user named `smart_eval` with password `smart_eval`.

## Run Tests

```bash
mvn test
```

## Run API

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.
```

Modify root `README.md` to link the design and backend README:

```markdown
# smart-eval

高校处级干部测评系统。

- Design: `docs/superpowers/specs/2026-07-03-smart-eval-design.md`
- Backend: `backend/README.md`
```

- [ ] **Step 4: Run all backend tests**

Run:

```bash
cd backend
mvn test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/smarteval/e2e backend/README.md README.md
git commit -m "test: verify backend evaluation flow"
```

---

### Task 12: Frontend Scaffold with Ant Design Pro Routing

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/app.tsx`
- Create: `frontend/src/routes.ts`
- Create: `frontend/src/request.ts`
- Create: `frontend/src/access.ts`
- Create: `frontend/src/pages/admin/Login.tsx`
- Create: `frontend/src/pages/admin/Dashboard.tsx`
- Create: `frontend/src/pages/mobile/Login.tsx`
- Create: `frontend/src/pages/mobile/MobileLayout.tsx`
- Create: `frontend/src/tests/routes.test.tsx`

**Interfaces:**
- Produces: React app with administrator routes using Pro layout and mobile routes using `MobileLayout`.
- Produces: API client `request<T>(url: string, options?: RequestInit): Promise<T>`.

- [ ] **Step 1: Write failing route test**

Create `frontend/src/tests/routes.test.tsx`:

```tsx
import { describe, expect, it } from 'vitest';
import { routes } from '../routes';

describe('routes', () => {
  it('keeps mobile routes outside the admin layout', () => {
    const mobile = routes.find((route) => route.path === '/mobile');
    expect(mobile?.layout).toBe(false);
  });
});
```

- [ ] **Step 2: Run frontend tests to verify they fail**

Run:

```bash
cd frontend
npm test -- --run
```

Expected: FAIL because the frontend project is not scaffolded.

- [ ] **Step 3: Add package and config files**

Create `frontend/package.json`:

```json
{
  "name": "smart-eval-frontend",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "test": "vitest",
    "lint": "eslint src --ext .ts,.tsx"
  },
  "dependencies": {
    "@ant-design/icons": "^5.5.0",
    "@ant-design/pro-components": "^2.8.0",
    "antd": "^5.22.0",
    "dayjs": "^1.11.13",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.28.0"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.6.3",
    "@testing-library/react": "^16.1.0",
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.3",
    "eslint": "^9.15.0",
    "typescript": "^5.6.3",
    "vite": "^6.0.1",
    "vitest": "^2.1.5"
  }
}
```

Create `vite.config.ts`, `tsconfig.json`, and a Vitest setup using jsdom.

- [ ] **Step 4: Add routing and layouts**

Create `routes.ts`:

```tsx
export const routes = [
  { path: '/', redirect: '/admin' },
  { path: '/admin/login', component: './pages/admin/Login', layout: false },
  { path: '/admin', component: './pages/admin/Dashboard', name: '首页' },
  { path: '/mobile', component: './pages/mobile/MobileLayout', layout: false },
];
```

Create `app.tsx` with `ProLayout` for administrator routes and regular React Router rendering for mobile routes. Mobile routes must not render sidebar or top administrator chrome.

- [ ] **Step 5: Add request helper**

Create `request.ts`:

```ts
export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message: string;
  errors: Record<string, string>;
};

export async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}
```

- [ ] **Step 6: Run frontend route test**

Run:

```bash
cd frontend
npm test -- --run
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend
git commit -m "chore: scaffold React frontend"
```

---

### Task 13: Frontend API Services and Authentication State

**Files:**
- Create: `frontend/src/services/auth.ts`
- Create: `frontend/src/services/departments.ts`
- Create: `frontend/src/services/evaluatees.ts`
- Create: `frontend/src/services/evaluatorUsers.ts`
- Create: `frontend/src/services/adminUsers.ts`
- Create: `frontend/src/services/results.ts`
- Create: `frontend/src/services/mobileEvaluation.ts`
- Create: `frontend/src/services/session.ts`
- Create: `frontend/src/tests/auth-service.test.ts`

**Interfaces:**
- Produces: typed functions matching backend endpoints.
- Produces: `session.ts` functions `saveToken`, `getToken`, `clearToken`, `authHeaders`.

- [ ] **Step 1: Write failing session test**

```ts
import { describe, expect, it } from 'vitest';
import { authHeaders, clearToken, saveToken } from '../services/session';

describe('session', () => {
  it('returns bearer header after saving token', () => {
    clearToken();
    saveToken('abc');
    expect(authHeaders()).toEqual({ Authorization: 'Bearer abc' });
  });
});
```

- [ ] **Step 2: Run service tests to verify they fail**

Run:

```bash
cd frontend
npm test -- --run src/tests/auth-service.test.ts
```

Expected: FAIL because service files are missing.

- [ ] **Step 3: Implement session helper**

Create `session.ts`:

```ts
const TOKEN_KEY = 'smart_eval_token';

export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}
```

- [ ] **Step 4: Implement typed API service files**

Each service imports `request` and `authHeaders`. Required function names:

- `adminLogin(username: string, password: string)`
- `mobileLogin(username: string, password: string)`
- `getAdminMe()`
- `getMobileMe()`
- `listDepartments(params)`
- `createDepartment(payload)`
- `updateDepartment(id, payload)`
- `deleteDepartment(id)`
- `listEvaluatees(params)`
- `createEvaluatee(payload)`
- `updateEvaluatee(id, payload)`
- `setEvaluateeEnabled(id, enabled)`
- `listEvaluatorUsers(params)`
- `batchGenerateEvaluatorUsers(payload)`
- `resetEvaluatorPassword(id)`
- `setEvaluatorUserEnabled(id, enabled)`
- `printEvaluatorUsersUrl(params)`
- `listAdminUsers(params)`
- `createAdminUser(payload)`
- `updateAdminUser(id, payload)`
- `setAdminUserEnabled(id, enabled)`
- `resetAdminUserPassword(id, password)`
- `getEvaluationItems()`
- `submitEvaluation(records)`
- `getResultSummary(params)`
- `getResultDetails(params)`

- [ ] **Step 5: Run service tests**

Run:

```bash
cd frontend
npm test -- --run src/tests/auth-service.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/services frontend/src/tests/auth-service.test.ts
git commit -m "feat: add frontend API services"
```

---

### Task 14: Administrator Data Management Pages

**Files:**
- Create: `frontend/src/pages/admin/Departments.tsx`
- Create: `frontend/src/pages/admin/Evaluatees.tsx`
- Create: `frontend/src/pages/admin/EvaluatorUsers.tsx`
- Create: `frontend/src/pages/admin/AdminUsers.tsx`
- Modify: `frontend/src/routes.ts`
- Create: `frontend/src/tests/admin-pages.test.tsx`

**Interfaces:**
- Produces administrator pages for departments, evaluatees, evaluator users, and admin users.
- Consumes service functions from Task 13.

- [ ] **Step 1: Write failing admin pages test**

Render `Departments` with mocked services and verify the primary button text:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Departments from '../pages/admin/Departments';

vi.mock('../services/departments', () => ({
  listDepartments: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
}));

describe('Departments page', () => {
  it('renders create action', async () => {
    render(<Departments />);
    expect(await screen.findByText('新增部门')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run page test to verify it fails**

Run:

```bash
cd frontend
npm test -- --run src/tests/admin-pages.test.tsx
```

Expected: FAIL because pages are missing.

- [ ] **Step 3: Implement department page**

Use Ant Design Pro `ProTable` and `ModalForm`. Columns: name, sortOrder, createdAt, actions. Actions: add, edit, delete.

- [ ] **Step 4: Implement evaluatee page**

Use `ProTable`. Filters: department, name, enabled. Columns: name, departmentName, enabled, createdAt, actions. Actions: add, edit, enable, disable.

- [ ] **Step 5: Implement evaluator users page**

Use `ProTable`. Filters: department, submitted, enabled. Columns: username, passwordPlain, departmentName, enabled, submitted, submittedAt, actions. Actions: batch generate, reset password, enable, disable, open print URL.

- [ ] **Step 6: Implement admin users page**

Use `ProTable`. Columns: username, displayName, enabled, createdAt, actions. Actions: add, edit, reset password, enable, disable.

- [ ] **Step 7: Register admin routes**

Add routes:

```ts
{ path: '/admin/departments', component: './pages/admin/Departments', name: '部门管理' },
{ path: '/admin/evaluatees', component: './pages/admin/Evaluatees', name: '被测评人员' },
{ path: '/admin/evaluator-users', component: './pages/admin/EvaluatorUsers', name: '测评用户' },
{ path: '/admin/admin-users', component: './pages/admin/AdminUsers', name: '管理员账号' },
```

- [ ] **Step 8: Run admin pages test**

Run:

```bash
cd frontend
npm test -- --run src/tests/admin-pages.test.tsx
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/pages/admin frontend/src/routes.ts frontend/src/tests/admin-pages.test.tsx
git commit -m "feat: add administrator management pages"
```

---

### Task 15: Administrator Result Pages and Dashboard

**Files:**
- Create: `frontend/src/pages/admin/ResultSummary.tsx`
- Create: `frontend/src/pages/admin/ResultDetails.tsx`
- Modify: `frontend/src/pages/admin/Dashboard.tsx`
- Modify: `frontend/src/routes.ts`
- Create: `frontend/src/tests/result-pages.test.tsx`

**Interfaces:**
- Produces result summary and traceable detail pages.
- Produces dashboard cards for department count, evaluatee count, evaluator count, submitted count, and unsubmitted count.

- [ ] **Step 1: Write failing result page test**

Mock `getResultSummary` and verify grade count columns render:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ResultSummary from '../pages/admin/ResultSummary';

vi.mock('../services/results', () => ({
  getResultSummary: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
}));

describe('ResultSummary page', () => {
  it('renders grade columns', async () => {
    render(<ResultSummary />);
    expect(await screen.findByText('优秀')).toBeInTheDocument();
    expect(screen.getByText('称职')).toBeInTheDocument();
    expect(screen.getByText('基本称职')).toBeInTheDocument();
    expect(screen.getByText('不称职')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run result page test to verify it fails**

Run:

```bash
cd frontend
npm test -- --run src/tests/result-pages.test.tsx
```

Expected: FAIL because result pages are missing.

- [ ] **Step 3: Implement result summary page**

Use `ProTable`. Filters: department, evaluateeName. Columns: evaluateeName, departmentName, excellentCount, competentCount, basicCompetentCount, incompetentCount, totalCount.

- [ ] **Step 4: Implement result details page**

Use `ProTable`. Filters: department, evaluatee, evaluatorUserId, grade. Columns: evaluatorUsername, evaluatorDepartmentName, evaluateeName, gradeLabel, comment1, comment2, createdAt.

- [ ] **Step 5: Implement dashboard**

Call list APIs with `pageSize=1` and submitted filters to populate statistic cards. The dashboard must avoid chart dependencies.

- [ ] **Step 6: Register result routes**

Add routes:

```ts
{ path: '/admin/results/summary', component: './pages/admin/ResultSummary', name: '结果汇总' },
{ path: '/admin/results/details', component: './pages/admin/ResultDetails', name: '评价明细' },
```

- [ ] **Step 7: Run result page tests**

Run:

```bash
cd frontend
npm test -- --run src/tests/result-pages.test.tsx
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/pages/admin frontend/src/routes.ts frontend/src/tests/result-pages.test.tsx
git commit -m "feat: add result pages and dashboard"
```

---

### Task 16: Mobile Evaluation Flow

**Files:**
- Create: `frontend/src/pages/mobile/EvaluationPage.tsx`
- Create: `frontend/src/pages/mobile/ConfirmPage.tsx`
- Create: `frontend/src/pages/mobile/CompletePage.tsx`
- Modify: `frontend/src/pages/mobile/Login.tsx`
- Modify: `frontend/src/pages/mobile/MobileLayout.tsx`
- Modify: `frontend/src/routes.ts`
- Create: `frontend/src/tests/mobile-flow.test.tsx`

**Interfaces:**
- Produces mobile login, evaluation list, confirmation, submit, and completed states.
- Consumes `mobileLogin`, `getEvaluationItems`, and `submitEvaluation`.

- [ ] **Step 1: Write failing mobile flow test**

Mock evaluation items with two evaluatees. Verify submit is disabled until both have grades:

```tsx
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import EvaluationPage from '../pages/mobile/EvaluationPage';

vi.mock('../services/mobileEvaluation', () => ({
  getEvaluationItems: vi.fn().mockResolvedValue({
    departmentId: 1,
    departmentName: '组织部',
    submitted: false,
    items: [
      { evaluateeId: 1, name: '张三' },
      { evaluateeId: 2, name: '李四' },
    ],
    grades: [
      { value: 'EXCELLENT', label: '优秀' },
      { value: 'COMPETENT', label: '称职' },
      { value: 'BASIC_COMPETENT', label: '基本称职' },
      { value: 'INCOMPETENT', label: '不称职' },
    ],
  }),
  submitEvaluation: vi.fn(),
}));

describe('EvaluationPage', () => {
  it('requires all evaluatees to be graded before submit', async () => {
    render(<EvaluationPage />);
    const submit = await screen.findByRole('button', { name: '提交' });
    expect(submit).toBeDisabled();

    fireEvent.click(screen.getAllByText('优秀')[0]);
    expect(submit).toBeDisabled();

    fireEvent.click(screen.getAllByText('称职')[1]);
    expect(submit).toBeEnabled();
  });
});
```

- [ ] **Step 2: Run mobile flow test to verify it fails**

Run:

```bash
cd frontend
npm test -- --run src/tests/mobile-flow.test.tsx
```

Expected: FAIL because mobile evaluation page is missing.

- [ ] **Step 3: Implement mobile login**

`Login.tsx` fields: username and password. On success save token and navigate to `/mobile/evaluation`. On failure show the API error message.

- [ ] **Step 4: Implement evaluation page**

Render department name and one compact section per evaluatee. Use segmented radio buttons for the four grades. Store selected grades in component state keyed by `evaluateeId`. The submit button is fixed at the bottom and enabled only when every evaluatee has a grade.

- [ ] **Step 5: Implement confirmation and completion**

On submit button click show a confirmation view with evaluated count and irreversible-submit message. Confirmation calls `submitEvaluation`. On success navigate to `/mobile/complete`. If `getEvaluationItems` returns `submitted=true`, navigate directly to `/mobile/complete`.

- [ ] **Step 6: Register mobile routes**

Routes:

```ts
{ path: '/mobile/login', component: './pages/mobile/Login', layout: false },
{ path: '/mobile/evaluation', component: './pages/mobile/EvaluationPage', layout: false },
{ path: '/mobile/confirm', component: './pages/mobile/ConfirmPage', layout: false },
{ path: '/mobile/complete', component: './pages/mobile/CompletePage', layout: false },
```

- [ ] **Step 7: Run mobile flow test**

Run:

```bash
cd frontend
npm test -- --run src/tests/mobile-flow.test.tsx
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/pages/mobile frontend/src/routes.ts frontend/src/tests/mobile-flow.test.tsx
git commit -m "feat: add mobile evaluation flow"
```

---

### Task 17: Full-Stack Verification and Developer Documentation

**Files:**
- Create: `docker-compose.yml`
- Create: `frontend/README.md`
- Modify: `README.md`
- Create: `frontend/e2e/smart-eval.spec.ts`
- Create: `frontend/playwright.config.ts`

**Interfaces:**
- Produces: local development instructions and one browser-level flow test.

- [ ] **Step 1: Add Docker Compose for PostgreSQL**

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: smart_eval
      POSTGRES_USER: smart_eval
      POSTGRES_PASSWORD: smart_eval
    ports:
      - "5432:5432"
    volumes:
      - smart_eval_pg:/var/lib/postgresql/data

volumes:
  smart_eval_pg:
```

- [ ] **Step 2: Add frontend README**

Create `frontend/README.md`:

```markdown
# Smart Eval Frontend

## Requirements

- Node.js 20+
- npm 10+

## Install

```bash
npm install
```

## Run

```bash
npm run dev
```

The app listens on `http://localhost:5173`.

Administrator routes start at `/admin`.
Mobile evaluator routes start at `/mobile/login`.
```

- [ ] **Step 3: Update root README**

Set `README.md` to:

```markdown
# smart-eval

高校处级干部测评系统。

## Documents

- Design: `docs/superpowers/specs/2026-07-03-smart-eval-design.md`
- Implementation plan: `docs/superpowers/plans/2026-07-03-smart-eval-implementation.md`

## Services

- Backend: `backend/README.md`
- Frontend: `frontend/README.md`

## Local Development

```bash
docker compose up -d postgres
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```
```

- [ ] **Step 4: Add Playwright config and e2e test**

Create a Playwright test that opens `/admin/login`, verifies the login page title, opens `/mobile/login`, and verifies username and password fields exist. Use real backend only when seeded credentials are available; this first e2e test checks route rendering.

- [ ] **Step 5: Run complete verification**

Run:

```bash
cd backend
mvn test
```

Expected: PASS.

Run:

```bash
cd frontend
npm test -- --run
npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml README.md frontend/README.md frontend/playwright.config.ts frontend/e2e
git commit -m "docs: add full-stack runbook"
```

---

## Self-Review Checklist

- Spec coverage:
  - Administrator management: Tasks 5, 14.
  - Department management: Tasks 6, 14.
  - Evaluatee management: Tasks 7, 14.
  - Evaluator user batch generation, plaintext password, reset, enabled state, print HTML: Task 8 and Task 14.
  - Mobile login, department-scoped item list, complete-before-submit rule, one-time submission: Tasks 5, 9, 16.
  - Traceable summary and detail results: Tasks 10, 15.
  - PostgreSQL schema, constraints, comments, grade enum: Task 2.
  - JWT role separation and BCrypt administrator passwords: Tasks 3 and 5.
  - Tests and full-flow verification: Tasks 11 and 17.
- Scope exclusions remain excluded:
  - No evaluation batch implementation.
  - No import implementation.
  - No Excel export implementation.
  - No chart dashboard implementation.
  - No evaluator password-change implementation.
  - No configurable indicator implementation.
  - No anonymous result mode.
- Type consistency:
  - Backend enum values match frontend grade strings.
  - `passwordPlain` is the API field for `evaluator_user.password_plain`.
  - `comment1` and `comment2` appear in submission and result detail DTOs.
  - All list APIs return `PageResponse<T>` through `ApiResponse`.
