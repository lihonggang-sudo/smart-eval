# 高校处级干部测评系统详细设计

## 1. 背景与目标

本项目用于高校处级干部测评。系统面向两个角色：管理员和测评用户。管理员维护部门、被测评人员和测评用户账号，并查看测评结果；测评用户通过手机端登录后，对本部门被测评人员进行等级评价。

首版目标是交付一个结构清晰、部署简单、可追溯的测评系统。系统采用前后端分离架构，技术栈为 Spring Boot + React + PostgreSQL。管理员端使用 Ant Design Pro 实现桌面后台；测评用户端使用 React 实现移动端优先页面。

## 2. 首版范围

### 2.1 包含功能

管理员功能：

- 管理部门：新增、编辑、删除部门。
- 管理被测评人员：维护姓名、所属部门、启用状态。
- 管理测评用户：按部门批量随机生成账号和密码，查看账号，重置密码，启用或禁用账号。
- 导出打印账号：生成适合打印的 HTML 页面，包含用户名、明文密码、所属部门和访问地址。
- 管理管理员账号：支持多管理员账号，包含新增、编辑、启用、禁用、重置密码。
- 查看测评结果：查看每个被测评人员的等级汇总，以及每个测评用户的实名评价明细。

测评用户功能：

- 手机端登录。
- 登录后仅看到自己所属部门的被测评人员。
- 对每名被测评人员选择一个等级。
- 必须完成本部门全部启用被测评人员的评价后才能提交。
- 提交后结果锁定，不能再次修改。

### 2.2 明确不包含

首版不实现以下功能：

- 测评批次或测评活动。
- 数据导入。
- Excel 导出。
- 图表看板。
- 测评用户修改密码。
- 可配置测评指标表。
- 匿名评价。

## 3. 业务规则

### 3.1 角色

管理员：

- 可以访问管理员后台。
- 可以管理所有部门、被测评人员、测评用户、管理员账号和测评结果。
- 可以查看实名评价明细。

测评用户：

- 只能访问移动测评端。
- 只能查看自己所属部门的被测评人员。
- 只能提交自己的评价结果。
- 不能修改密码。
- 提交后不能修改评价。

### 3.2 测评等级

首版固定四个等级：

| 枚举值 | 展示名称 |
| --- | --- |
| `EXCELLENT` | 优秀 |
| `COMPETENT` | 称职 |
| `BASIC_COMPETENT` | 基本称职 |
| `INCOMPETENT` | 不称职 |

### 3.3 提交规则

- 测评用户必须对本部门所有启用的被测评人员完成评价。
- 提交请求中的被测评人员必须全部属于当前测评用户所在部门。
- 提交请求不能包含已禁用的被测评人员。
- 已提交的测评用户不能再次提交。
- 后端在一个数据库事务中写入评价记录，并更新测评用户提交状态。
- 管理员可以查看提交人与评价结果之间的对应关系。

## 4. 总体架构

系统采用前后端分离架构。

后端：

- Spring Boot 提供 REST API。
- PostgreSQL 存储业务数据。
- Spring Security 负责认证和鉴权。
- 认证令牌默认使用 JWT。
- Flyway 或 Liquibase 管理数据库迁移。
- 后端按领域拆分模块：认证、管理员账号、部门、被测评人员、测评用户、测评提交、结果统计、打印导出。

前端：

- React 构建单页应用。
- 管理员端基于 Ant Design Pro，提供登录页、后台框架、菜单路由、表格页、表单弹窗和结果页。
- 移动端使用 React 独立路由和移动端样式，不套用 Ant Design Pro 后台布局。
- 前端只做交互校验和体验优化，关键业务校验由后端最终执行。

部署：

- 首版可部署为一个后端服务、一个前端静态站点和一个 PostgreSQL 数据库。
- 前端静态资源可由 Nginx 托管，也可由后端统一托管。
- 生产环境应启用 HTTPS。

## 5. 模块设计

### 5.1 Auth 模块

职责：

- 管理员登录。
- 测评用户登录。
- 获取当前登录用户信息。
- 校验 Token 和角色权限。

边界：

- 管理员和测评用户使用不同登录入口。
- 后端可以统一签发 JWT，但 Token 中必须包含用户类型和用户 ID。
- 管理员接口只接受管理员 Token。
- 移动端接口只接受测评用户 Token。

### 5.2 AdminUser 模块

职责：

- 管理管理员账号。
- 新增、编辑、启用、禁用管理员。
- 重置管理员密码。

规则：

- 管理员用户名唯一。
- 管理员密码使用 BCrypt 哈希保存。
- 禁止禁用最后一个启用管理员。
- 禁用的管理员不能登录。

### 5.3 Department 模块

职责：

- 维护部门名称和排序。
- 为被测评人员和测评用户提供归属关系。

规则：

- 部门名称唯一。
- 如果部门下存在被测评人员、测评用户或评价记录，则禁止删除。

### 5.4 Evaluatee 模块

职责：

- 维护被测评人员姓名、所属部门和启用状态。

规则：

- 被测评人员必须属于一个部门。
- 已产生评价记录的被测评人员不物理删除，使用禁用状态控制是否参与后续评价。
- 移动端只展示当前测评用户所属部门中启用的被测评人员。

### 5.5 EvaluatorUser 模块

职责：

- 按部门批量生成测评用户账号。
- 查看、启用、禁用、重置测评用户账号。
- 生成打印凭证 HTML。

规则：

- 测评用户用户名唯一。
- 测评用户密码按业务要求以明文保存，字段为 `password_plain`。
- 明文密码只允许管理员端接口返回，移动端接口不得返回密码。
- 测评用户不能自行修改密码。
- 禁用的测评用户不能登录。
- 批量生成时由管理员选择部门和生成数量。
- 随机用户名和密码必须使用不可预测随机数生成，避免简单递增或可猜测规则。

### 5.6 Evaluation 模块

职责：

- 为测评用户返回待评价人员。
- 接收测评提交。
- 保存评价记录。

规则：

- 提交接口一次性接收该测评用户对本部门全部启用被测评人员的评价。
- 每个评价项包含 `evaluateeId`、`grade`，并预留 `comment1`、`comment2`。
- `comment1`、`comment2` 首版不要求填写，作为评价记录冗余字段保存。
- 每个测评用户对每个被测评人员最多存在一条评价记录。

### 5.7 Result 模块

职责：

- 提供管理员查看的汇总统计。
- 提供管理员查看的实名评价明细。

汇总统计：

- 按被测评人员聚合。
- 展示各等级票数和总票数。

实名明细：

- 展示测评用户、测评用户所属部门、被测评人员、等级、提交时间。
- 可按部门、被测评人员、测评用户、等级过滤。

### 5.8 PrintExport 模块

职责：

- 生成适合打印的测评用户账号凭证 HTML。

内容：

- 系统名称。
- 移动端访问地址。
- 所属部门。
- 用户名。
- 明文密码。
- 简短登录说明。

规则：

- 该页面只允许管理员访问。
- 支持按部门筛选要打印的账号。
- HTML 应适合浏览器直接打印。

## 6. 数据库设计

### 6.1 `admin_user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigserial | 主键 |
| `username` | varchar(64) | 登录名，唯一 |
| `password_hash` | varchar(255) | BCrypt 密码哈希 |
| `display_name` | varchar(64) | 显示名称 |
| `enabled` | boolean | 是否启用 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |

约束：

- `uk_admin_user_username`：`username` 唯一。

### 6.2 `department`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigserial | 主键 |
| `name` | varchar(128) | 部门名称，唯一 |
| `sort_order` | integer | 排序值 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |

约束：

- `uk_department_name`：`name` 唯一。

### 6.3 `evaluatee`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigserial | 主键 |
| `name` | varchar(64) | 被测评人员姓名 |
| `department_id` | bigint | 所属部门 ID |
| `enabled` | boolean | 是否启用 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |

约束：

- `fk_evaluatee_department`：`department_id` 关联 `department.id`。

索引：

- `idx_evaluatee_department_id`：按部门查询被测评人员。

### 6.4 `evaluator_user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigserial | 主键 |
| `username` | varchar(64) | 登录名，唯一 |
| `password_plain` | varchar(64) | 明文密码 |
| `department_id` | bigint | 所属部门 ID |
| `enabled` | boolean | 是否启用 |
| `submitted` | boolean | 是否已提交 |
| `submitted_at` | timestamptz | 提交时间 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |

约束：

- `uk_evaluator_user_username`：`username` 唯一。
- `fk_evaluator_user_department`：`department_id` 关联 `department.id`。

索引：

- `idx_evaluator_user_department_id`：按部门查询测评用户。
- `idx_evaluator_user_submitted`：按提交状态统计。

### 6.5 `evaluation_record`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigserial | 主键 |
| `evaluator_user_id` | bigint | 测评用户 ID |
| `evaluatee_id` | bigint | 被测评人员 ID |
| `department_id` | bigint | 冗余部门 ID，取测评用户提交时所属部门 |
| `grade` | varchar(32) | 评价等级 |
| `comment1` | varchar(255) | 冗余备注字段 1 |
| `comment2` | varchar(255) | 冗余备注字段 2 |
| `created_at` | timestamptz | 创建时间 |

约束：

- `fk_evaluation_record_evaluator_user`：`evaluator_user_id` 关联 `evaluator_user.id`。
- `fk_evaluation_record_evaluatee`：`evaluatee_id` 关联 `evaluatee.id`。
- `fk_evaluation_record_department`：`department_id` 关联 `department.id`。
- `uk_evaluation_record_user_evaluatee`：`evaluator_user_id, evaluatee_id` 唯一，防止重复评价。
- `ck_evaluation_record_grade`：`grade` 只能为 `EXCELLENT`、`COMPETENT`、`BASIC_COMPETENT`、`INCOMPETENT`。

索引：

- `idx_evaluation_record_evaluatee_id`：按被测评人员统计。
- `idx_evaluation_record_evaluator_user_id`：按测评用户查看明细。
- `idx_evaluation_record_department_id`：按部门过滤结果。

## 7. API 设计

所有接口返回统一响应结构：

```json
{
  "success": true,
  "data": {},
  "message": ""
}
```

错误响应：

```json
{
  "success": false,
  "message": "业务错误说明",
  "errors": {
    "fieldName": "字段错误说明"
  }
}
```

### 7.1 管理员认证

`POST /api/admin/auth/login`

请求：

```json
{
  "username": "admin",
  "password": "password"
}
```

响应：

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "username": "admin",
    "displayName": "系统管理员"
  }
}
```

`GET /api/admin/me`

返回当前管理员信息。

### 7.2 移动端认证

`POST /api/mobile/auth/login`

请求：

```json
{
  "username": "u001",
  "password": "x7p9k2"
}
```

响应：

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "username": "u001",
    "departmentId": 1,
    "departmentName": "组织部",
    "submitted": false
  }
}
```

`GET /api/mobile/me`

返回当前测评用户信息和提交状态，不返回明文密码。

### 7.3 部门管理

- `GET /api/admin/departments`
- `POST /api/admin/departments`
- `PUT /api/admin/departments/{id}`
- `DELETE /api/admin/departments/{id}`

部门列表应支持按名称搜索和排序。

### 7.4 被测评人员管理

- `GET /api/admin/evaluatees`
- `POST /api/admin/evaluatees`
- `PUT /api/admin/evaluatees/{id}`
- `DELETE /api/admin/evaluatees/{id}`
- `PUT /api/admin/evaluatees/{id}/enabled`

列表支持按部门、姓名、启用状态过滤。

### 7.5 测评用户管理

- `GET /api/admin/evaluator-users`
- `POST /api/admin/evaluator-users/batch-generate`
- `POST /api/admin/evaluator-users/{id}/reset-password`
- `PUT /api/admin/evaluator-users/{id}/enabled`
- `GET /api/admin/evaluator-users/print`

批量生成请求：

```json
{
  "departmentId": 1,
  "count": 30
}
```

批量生成响应：

```json
{
  "created": 30,
  "items": [
    {
      "id": 1,
      "username": "D001-0001",
      "passwordPlain": "A8K2P9",
      "departmentName": "组织部"
    }
  ]
}
```

打印接口参数：

- `departmentId`：可选，按部门过滤。
- `submitted`：可选，按提交状态过滤。

打印接口返回 HTML，供浏览器打印。

### 7.6 管理员账号管理

- `GET /api/admin/admin-users`
- `POST /api/admin/admin-users`
- `PUT /api/admin/admin-users/{id}`
- `PUT /api/admin/admin-users/{id}/enabled`
- `POST /api/admin/admin-users/{id}/reset-password`

### 7.7 移动端测评

`GET /api/mobile/evaluation/items`

响应：

```json
{
  "departmentId": 1,
  "departmentName": "组织部",
  "submitted": false,
  "items": [
    {
      "evaluateeId": 1,
      "name": "张三"
    }
  ],
  "grades": [
    {
      "value": "EXCELLENT",
      "label": "优秀"
    },
    {
      "value": "COMPETENT",
      "label": "称职"
    },
    {
      "value": "BASIC_COMPETENT",
      "label": "基本称职"
    },
    {
      "value": "INCOMPETENT",
      "label": "不称职"
    }
  ]
}
```

`POST /api/mobile/evaluation/submit`

请求：

```json
{
  "records": [
    {
      "evaluateeId": 1,
      "grade": "EXCELLENT",
      "comment1": "",
      "comment2": ""
    }
  ]
}
```

响应：

```json
{
  "submitted": true,
  "submittedAt": "2026-07-03T12:00:00Z"
}
```

### 7.8 结果查询

`GET /api/admin/results/summary`

查询参数：

- `departmentId`：可选。
- `evaluateeName`：可选。

响应项：

```json
{
  "evaluateeId": 1,
  "evaluateeName": "张三",
  "departmentName": "组织部",
  "excellentCount": 10,
  "competentCount": 15,
  "basicCompetentCount": 2,
  "incompetentCount": 1,
  "totalCount": 28
}
```

`GET /api/admin/results/details`

查询参数：

- `departmentId`：可选。
- `evaluateeId`：可选。
- `evaluatorUserId`：可选。
- `grade`：可选。

响应项：

```json
{
  "evaluatorUserId": 1,
  "evaluatorUsername": "D001-0001",
  "evaluatorDepartmentName": "组织部",
  "evaluateeId": 1,
  "evaluateeName": "张三",
  "grade": "EXCELLENT",
  "gradeLabel": "优秀",
  "comment1": "",
  "comment2": "",
  "createdAt": "2026-07-03T12:00:00Z"
}
```

## 8. 前端页面设计

### 8.1 管理员端

管理员端基于 Ant Design Pro。

页面结构：

- 登录页。
- 后台布局：侧边菜单、顶部用户信息、内容区。
- 首页概览。
- 部门管理。
- 被测评人员管理。
- 测评用户管理。
- 管理员账号管理。
- 结果汇总。
- 评价明细。

首页概览：

- 部门数量。
- 被测评人员数量。
- 测评用户数量。
- 已提交测评用户数量。
- 未提交测评用户数量。

部门管理页：

- 表格展示部门名称、排序、创建时间。
- 支持新增、编辑、删除。
- 删除失败时展示后端返回的关联数据提示。

被测评人员管理页：

- 表格展示姓名、部门、启用状态、创建时间。
- 支持按部门和姓名筛选。
- 支持新增、编辑、启用、禁用。

测评用户管理页：

- 表格展示用户名、明文密码、部门、启用状态、提交状态、提交时间。
- 支持按部门、提交状态、启用状态筛选。
- 支持按部门批量生成账号。
- 支持重置密码。
- 支持启用、禁用。
- 支持打开打印凭证页面。

管理员账号管理页：

- 表格展示用户名、显示名称、启用状态、创建时间。
- 支持新增、编辑、重置密码、启用、禁用。

结果汇总页：

- 表格展示被测评人员、部门、各等级票数、总票数。
- 支持按部门、姓名筛选。

评价明细页：

- 表格展示测评用户、测评用户部门、被测评人员、等级、备注字段、提交时间。
- 支持按部门、被测评人员、测评用户、等级筛选。

### 8.2 移动测评端

移动端页面使用 React 独立路由和移动端样式。

登录页：

- 用户名输入框。
- 密码输入框。
- 登录按钮。
- 登录失败展示统一错误信息。

测评页：

- 展示当前部门名称。
- 列表展示本部门所有启用被测评人员。
- 每名被测评人员展示四个等级选项。
- 页面底部固定提交按钮。
- 未完成全部评价时，提交按钮不可提交或提交时提示缺失人员。

提交确认页：

- 展示已完成评价数量。
- 提示提交后不可修改。
- 用户确认后调用提交接口。

完成页：

- 显示提交成功。
- 再次登录时，如果已提交，直接进入完成页或已提交提示页。

## 9. 权限与安全

### 9.1 鉴权

- 管理员接口统一位于 `/api/admin/**`。
- 移动端接口统一位于 `/api/mobile/**`。
- 后端根据 JWT 中的用户类型进行访问控制。
- 未登录返回 401。
- 角色不匹配返回 403。

### 9.2 密码

- 管理员密码使用 BCrypt 哈希保存。
- 测评用户密码按业务要求明文保存。
- 测评用户明文密码只在管理员端列表、重置、批量生成和打印接口中返回。
- 移动端登录成功后不返回明文密码。

### 9.3 登录与账号状态

- 登录失败统一返回“用户名或密码错误”。
- 禁用账号不能登录。
- 管理员禁用最后一个启用管理员时应拒绝。

### 9.4 传输与跨域

- 生产环境启用 HTTPS。
- 后端启用 CORS 白名单，只允许配置的前端域名访问。
- Token 存储方式由前端实现时确定，默认使用内存或安全的本地存储策略，并配合合理过期时间。

## 10. 错误处理

后端使用统一异常处理，错误类型包括：

- 参数校验错误。
- 认证失败。
- 权限不足。
- 资源不存在。
- 唯一约束冲突。
- 业务规则冲突。
- 系统内部错误。

典型错误：

- 部门名称重复。
- 用户名重复。
- 批量生成数量非法。
- 删除部门时存在关联数据。
- 提交测评时未评价全部人员。
- 提交测评时包含非本部门人员。
- 提交测评时包含禁用人员。
- 已提交账号再次提交。

提交测评时，如果缺少评价，错误响应应包含缺失人员列表，便于前端定位。

## 11. 测试策略

后端单元测试：

- 密码生成规则。
- 等级枚举校验。
- 管理员密码哈希验证。
- 测评用户明文密码登录验证。
- 提交完整性校验。

后端集成测试：

- 管理员登录。
- 测评用户登录。
- 部门 CRUD。
- 被测评人员 CRUD。
- 测评用户批量生成。
- 测评提交成功。
- 重复提交失败。
- 非本部门人员提交失败。
- 结果汇总统计。
- 实名明细查询。

前端测试：

- 管理员表格页查询和筛选。
- 批量生成账号弹窗。
- 打印凭证入口。
- 移动端登录。
- 移动端测评选择。
- 未完成全部评价时的提交限制。
- 提交成功后的锁定状态。

端到端测试：

1. 管理员登录。
2. 创建部门。
3. 创建被测评人员。
4. 按部门批量生成测评用户账号。
5. 使用测评用户账号登录移动端。
6. 完成本部门全部人员评价并提交。
7. 管理员查看汇总和实名明细。

数据库迁移测试：

- 唯一约束生效。
- 外键约束生效。
- 评价记录防重复约束生效。
- 等级枚举检查约束生效。

## 12. 关键实现约定

- 时间字段统一使用数据库 `timestamptz`，后端 API 输出 ISO 8601 字符串。
- 所有列表接口支持分页，分页参数统一为 `page` 和 `pageSize`。
- 后端内部使用枚举表示测评等级，API 使用枚举字符串。
- 软删除不作为首版通用机制；被测评人员和测评用户通过 `enabled` 控制是否参与业务。
- 管理端表格默认按创建时间倒序或排序字段升序展示，具体页面可按业务调整。
- 打印凭证 HTML 由后端生成，保证无需前端构建即可打印。

## 13. 后续可扩展方向

以下能力不进入首版，但当前设计保留合理扩展空间：

- 测评批次：未来可增加 `evaluation_batch`，并在测评用户、被测评人员、评价记录上关联批次。
- 指标配置：未来可将固定等级扩展为可配置问卷或多指标评分。
- 导入导出：未来可增加部门、被测评人员导入，以及结果 Excel 导出。
- 图表看板：未来可在结果模块增加图表统计。
- 更细权限：未来可拆分超级管理员和普通管理员权限。

## 14. 设计结论

首版采用标准前后端分离单体架构，使用 Spring Boot + React + PostgreSQL。管理员端基于 Ant Design Pro，移动端独立实现。系统不引入测评批次和可配置指标，聚焦部门内实名等级测评、账号批量生成、打印分发、一次性提交和结果追溯。

该设计满足当前需求，同时通过清晰的模块边界和数据库约束，为后续增加批次、导入导出和统计看板保留扩展路径。
