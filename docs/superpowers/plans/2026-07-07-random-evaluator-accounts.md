# 随机测评用户账号 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将批量生成测评用户账号从部门前缀递增格式改为完全随机且唯一的 8 位大写字母数字账号。

**Architecture:** 保持现有 API、DTO、前端表单和数据库结构不变，只修改后端账号生成策略。`RandomCredentialGenerator` 负责生成单个随机账号和密码，`EvaluatorUserService.batchGenerate` 负责批量内去重、数据库存在性检查、重试上限和保存。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Data JPA、JUnit 5、MockMvc、PostgreSQL/Testcontainers。

## Global Constraints

- 批量生成的测评用户账号必须完全随机。
- 账号不能包含部门 ID、部门编码或递增序号。
- 所有测评用户账号之间必须保持唯一。
- 同一批次内生成的账号在保存前也必须保持唯一。
- 初始密码保持现有行为：6 位大写字母或数字。
- 现有批量生成 UI 和请求结构保持不变：管理员选择部门和生成数量。
- 生成的用户仍然归属于所选部门。
- 账号格式为 8 位大写字母或数字，字符集为 `ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`。
- 如果在重试上限内无法生成足够数量的唯一账号，返回业务错误 `生成账号失败，请重试`。

---

## File Structure

- Modify: `backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java`
  - 删除未使用的顺序账号列表生成逻辑。
  - 新增 `username()`，返回 8 位大写字母数字随机账号。
  - 保留 `password()`，继续返回 6 位大写字母数字密码。
- Modify: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java`
  - 用随机账号生成替代 `Dxxx-0001` 递增逻辑。
  - 使用 `Set<String>` 做批次内去重。
  - 使用 `existsByUsername` 做数据库唯一性预检查。
  - 添加重试上限，超过后抛出 `BusinessException("生成账号失败，请重试")`。
- Modify: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`
  - 更新生成器测试。
  - 更新批量生成测试，断言账号随机格式、唯一、不匹配旧格式、部门仍正确。
  - 保持密码和数量校验测试。

---

### Task 1: 更新随机凭证生成器

**Files:**
- Modify: `backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java`
- Test: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`

**Interfaces:**
- Produces: `String RandomCredentialGenerator.username()`
- Produces: `String RandomCredentialGenerator.password()`
- Consumes: 无新增依赖。

- [ ] **Step 1: 写失败测试**

在 `EvaluatorUserControllerTest` 中把现有 `generatorCreatesUniqueUsernamesAndSixCharacterPasswords` 替换为：

```java
@Test
void generatorCreatesRandomUsernameAndSixCharacterPassword() {
    RandomCredentialGenerator generator = new RandomCredentialGenerator();

    Assertions.assertTrue(generator.username().matches("[A-Z0-9]{8}"));
    Assertions.assertTrue(generator.password().matches("[A-Z0-9]{6}"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -f backend/pom.xml -Dtest=EvaluatorUserControllerTest#generatorCreatesRandomUsernameAndSixCharacterPassword test
```

Expected: 编译失败或测试失败，因为 `RandomCredentialGenerator.username()` 尚不存在。

- [ ] **Step 3: 实现生成器**

将 `RandomCredentialGenerator.java` 改为：

```java
package com.smarteval.evaluatoruser;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomCredentialGenerator {
    private static final char[] CREDENTIAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int USERNAME_LENGTH = 8;
    private static final int PASSWORD_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String username() {
        return randomString(USERNAME_LENGTH);
    }

    public String password() {
        return randomString(PASSWORD_LENGTH);
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CREDENTIAL_CHARS[secureRandom.nextInt(CREDENTIAL_CHARS.length)]);
        }
        return builder.toString();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run:

```bash
mvn -f backend/pom.xml -Dtest=EvaluatorUserControllerTest#generatorCreatesRandomUsernameAndSixCharacterPassword test
```

Expected: PASS。

---

### Task 2: 更新批量生成服务逻辑

**Files:**
- Modify: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java`
- Test: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`

**Interfaces:**
- Consumes: `String RandomCredentialGenerator.username()`
- Produces: `BatchGenerateEvaluatorResponse EvaluatorUserService.batchGenerate(CurrentUser currentUser, BatchGenerateEvaluatorRequest request)`

- [ ] **Step 1: 写失败测试**

把 `batchGeneratesEvaluatorUsersWithSequentialUniqueNames` 替换为：

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -f backend/pom.xml -Dtest=EvaluatorUserControllerTest#batchGeneratesEvaluatorUsersWithRandomUniqueNames test
```

Expected: FAIL，因为当前服务仍生成 `Dxxx-0003` 这类旧格式账号。

- [ ] **Step 3: 实现服务逻辑**

在 `EvaluatorUserService.java` 中：

1. 添加 import：

```java
import java.util.HashSet;
import java.util.Set;
```

2. 添加常量：

```java
private static final String USERNAME_GENERATION_FAILED_MESSAGE = "生成账号失败，请重试";
private static final int USERNAME_GENERATION_ATTEMPT_MULTIPLIER = 20;
private static final int MIN_USERNAME_GENERATION_ATTEMPTS = 100;
```

3. 将 `batchGenerate` 中的部门编码和递增序号逻辑替换为：

```java
Department department = getRequiredDepartment(request.departmentId());
int maxAttempts = Math.max(MIN_USERNAME_GENERATION_ATTEMPTS,
        request.count() * USERNAME_GENERATION_ATTEMPT_MULTIPLIER);
int attempts = 0;
Set<String> usernamesInBatch = new HashSet<>();
List<EvaluatorUser> createdUsers = new ArrayList<>();

while (createdUsers.size() < request.count() && attempts < maxAttempts) {
    attempts++;
    String username = randomCredentialGenerator.username();
    if (usernamesInBatch.contains(username) || evaluatorUserRepository.existsByUsername(username)) {
        continue;
    }

    usernamesInBatch.add(username);
    createdUsers.add(new EvaluatorUser(
            username,
            randomCredentialGenerator.password(),
            department,
            true
    ));
}

if (createdUsers.size() < request.count()) {
    throw new BusinessException(USERNAME_GENERATION_FAILED_MESSAGE);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run:

```bash
mvn -f backend/pom.xml -Dtest=EvaluatorUserControllerTest#batchGeneratesEvaluatorUsersWithRandomUniqueNames test
```

Expected: PASS。

---

### Task 3: 运行回归测试并清理

**Files:**
- Verify: `backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java`
- Verify: `backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java`
- Verify: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`

**Interfaces:**
- Consumes: Task 1 和 Task 2 的最终代码。
- Produces: 已验证的随机账号生成行为。

- [ ] **Step 1: 运行测评用户控制器完整测试**

Run:

```bash
mvn -f backend/pom.xml -Dtest=EvaluatorUserControllerTest test
```

Expected: PASS，现有列表、批量生成、数量校验、重置密码、启禁用、打印和禁用管理员校验均通过。

- [ ] **Step 2: 运行后端测试套件**

Run:

```bash
mvn -f backend/pom.xml test
```

Expected: PASS。

- [ ] **Step 3: 检查改动范围**

Run:

```bash
git diff -- backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java docs/superpowers/specs/2026-07-07-random-evaluator-accounts-design.md docs/superpowers/plans/2026-07-07-random-evaluator-accounts.md
```

Expected: 只包含随机账号生成、对应测试、中文 spec 和本计划。

- [ ] **Step 4: 提交实现**

在用户确认需要提交时执行：

```bash
git add backend/src/main/java/com/smarteval/evaluatoruser/RandomCredentialGenerator.java backend/src/main/java/com/smarteval/evaluatoruser/EvaluatorUserService.java backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java docs/superpowers/specs/2026-07-07-random-evaluator-accounts-design.md docs/superpowers/plans/2026-07-07-random-evaluator-accounts.md
git commit -m "feat: generate random evaluator accounts"
```

Expected: 生成一个包含文档翻译、计划和实现的提交。

---

## Self-Review

- Spec coverage: 计划覆盖完全随机账号、无部门前缀、全局唯一、批次内唯一、密码保持 6 位、UI 不变、部门归属不变、失败重试错误。
- Placeholder scan: 无 `TBD`、`TODO` 或未定义后续实现。
- Type consistency: `RandomCredentialGenerator.username()`、`password()`、`EvaluatorUserService.batchGenerate(...)` 和现有 DTO/Repository 方法名称一致。
