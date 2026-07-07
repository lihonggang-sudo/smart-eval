# 打印账号隐藏登录地址实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理员打印测评用户账号时，打印 HTML 不再展示登录地址，只展示部门、用户名和密码。

**Architecture:** 打印 HTML 由后端 `PrintCredentialService` 生成，前端只是鉴权获取 HTML 并打开新窗口。本次在后端模板中删除登录地址字段，并更新服务单元测试和接口测试来覆盖“包含账号信息、不包含登录地址”的行为。

**Tech Stack:** Java 21、Spring Boot、JUnit 5、AssertJ、Spring MockMvc、Maven。

## Global Constraints

- 设计文档和计划文档使用中文。
- 只调整 `/api/admin/evaluator-users/print` 返回的打印 HTML 内容。
- 不调整前端“打印账号”按钮位置和交互。
- 不调整前端筛选条件传递逻辑。
- 不调整前端鉴权获取打印 HTML 的逻辑。
- 不调整新窗口打开打印页面的方式。
- 不调整移动端登录地址配置本身。
- 不自动执行 `git commit`，实现完成后保留未提交变更给用户检查。

---

## 文件结构

- 修改：`backend/src/main/java/com/smarteval/print/PrintCredentialService.java`
  - 职责：生成测评用户账号打印 HTML。
  - 本次删除打印卡片中的登录地址标签和值，并移除不再使用的 `mobileLoginUrl` 构造依赖。
- 修改：`backend/src/test/java/com/smarteval/print/PrintCredentialServiceTest.java`
  - 职责：验证打印 HTML 模板内容。
  - 本次改为断言 HTML 不包含登录地址和移动端登录 URL。
- 修改：`backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`
  - 职责：验证测评用户管理 API，包括打印接口。
  - 本次改为断言打印接口返回 HTML 不包含登录地址和 `/mobile/login`。

### Task 1: 后端打印 HTML 移除登录地址

**Files:**
- Modify: `backend/src/main/java/com/smarteval/print/PrintCredentialService.java`
- Test: `backend/src/test/java/com/smarteval/print/PrintCredentialServiceTest.java`
- Test: `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java`

**Interfaces:**
- Consumes: `PrintCredentialService.renderHtml(List<EvaluatorUserListItem> items)`
- Produces: 返回的 HTML 卡片只包含部门、用户名、密码，不包含 `登录地址` 和移动端登录 URL。

- [x] **Step 1: 写失败测试**

把 `backend/src/test/java/com/smarteval/print/PrintCredentialServiceTest.java` 改成：

```java
package com.smarteval.print;

import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrintCredentialServiceTest {
    private final PrintCredentialService printCredentialService = new PrintCredentialService();

    @Test
    void rendersHtmlWithCredentialFieldsWithoutLoginUrl() {
        EvaluatorUserListItem item = new EvaluatorUserListItem(
                1L,
                "D001-0001",
                "A8K2P9",
                1L,
                "组织部",
                true,
                false,
                null,
                Instant.parse("2026-07-03T00:00:00Z")
        );

        String html = printCredentialService.renderHtml(List.of(item));

        assertThat(html).contains("组织部");
        assertThat(html).contains("D001-0001");
        assertThat(html).contains("A8K2P9");
        assertThat(html).contains("@media print");
        assertThat(html).doesNotContain("登录地址");
        assertThat(html).doesNotContain("http://localhost:5173/mobile/login");
    }
}
```

把 `backend/src/test/java/com/smarteval/evaluatoruser/EvaluatorUserControllerTest.java` 中 `printsFilteredEvaluatorCredentialsAsHtml` 的断言改成：

```java
    @Test
    void printsFilteredEvaluatorCredentialsAsHtml() throws Exception {
        mockMvc.perform(get("/api/admin/evaluator-users/print")
                        .param("departmentId", departmentOneId.toString())
                        .param("submitted", "true")
                        .param("enabled", "false")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("组织部")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("%s-0002".formatted(departmentOneCode))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PASS22")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("登录地址"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/mobile/login"))));
    }
```

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=PrintCredentialServiceTest,EvaluatorUserControllerTest
```

Expected: 测试失败。失败原因应包括：

- `PrintCredentialService` 没有无参构造函数，或
- HTML 仍包含 `登录地址` / `http://localhost:5173/mobile/login`

- [x] **Step 3: 写最小实现**

把 `backend/src/main/java/com/smarteval/print/PrintCredentialService.java` 改成：

```java
package com.smarteval.print;

import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrintCredentialService {
    public String renderHtml(List<EvaluatorUserListItem> items) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <title>考评账号打印</title>
                <style>
                body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
                .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
                .card { border: 1px solid #d1d5db; padding: 16px; border-radius: 8px; page-break-inside: avoid; }
                .label { color: #4b5563; font-size: 12px; margin-bottom: 4px; }
                .value { font-size: 16px; margin-bottom: 10px; }
                @media print {
                  body { margin: 0; }
                  .card { break-inside: avoid; }
                }
                </style>
                </head>
                <body>
                <div class="grid">
                """);
        for (EvaluatorUserListItem item : items) {
            html.append("""
                    <section class="card">
                      <div class="label">部门</div>
                      <div class="value">%s</div>
                      <div class="label">用户名</div>
                      <div class="value">%s</div>
                      <div class="label">密码</div>
                      <div class="value">%s</div>
                    </section>
                    """.formatted(
                    escapeHtml(item.departmentName()),
                    escapeHtml(item.username()),
                    escapeHtml(item.passwordPlain())
            ));
        }
        html.append("""
                </div>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
```

- [x] **Step 4: 运行相关后端测试确认通过**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=PrintCredentialServiceTest,EvaluatorUserControllerTest
```

Expected: `PrintCredentialServiceTest` 和 `EvaluatorUserControllerTest` 全部通过。

- [x] **Step 5: 运行后端完整测试**

Run:

```bash
mvn -f backend/pom.xml test
```

Expected: 后端测试全部通过。

- [x] **Step 6: 检查工作区，保留未提交变更**

Run:

```bash
git status --short
```

Expected: 显示本次修改文件，且没有产生新的提交。
