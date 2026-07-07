# 打印账号隐藏登录地址设计

## 目标

管理员在“测评用户”页面点击“打印账号”后，打开的打印页面不再展示登录地址。打印卡片只展示测评用户发放所需的核心信息：

- 部门
- 用户名
- 密码

## 范围

本次只调整 `/api/admin/evaluator-users/print` 返回的打印 HTML 内容。

不调整：

- 前端“打印账号”按钮位置和交互
- 前端筛选条件传递逻辑
- 前端鉴权获取打印 HTML 的逻辑
- 新窗口打开打印页面的方式
- 移动端登录地址配置本身

## 当前行为

前端 `EvaluatorUsers.tsx` 中的“打印账号”按钮调用 `fetchEvaluatorUsersPrintHtml(filters)`，获取后端 `/api/admin/evaluator-users/print` 返回的 HTML，再通过 Blob URL 打开新窗口。

后端 `PrintCredentialService.renderHtml` 负责生成打印 HTML。当前每个账号卡片包含：

- 登录地址
- 部门
- 用户名
- 密码

其中“登录地址”使用配置项 `app.frontend.mobile-url` 输出，例如 `http://localhost:5173/mobile/login`。

## 方案

在 `PrintCredentialService.renderHtml` 中删除账号卡片里的“登录地址”标签和值。

修改后每个账号卡片只输出：

- 部门
- 用户名
- 密码

`PrintCredentialService` 可以暂时保留构造函数中的 `mobileLoginUrl` 依赖，以降低配置和 Spring Bean 初始化改动范围；如果实现时发现该字段完全无用且移除不会影响配置注入，可以同步删除字段和构造参数。

## 备选方案

1. 只改后端打印 HTML 模板。
   这是推荐方案。打印内容由后端生成，前端只是打开后端返回的 HTML，因此在后端模板中移除登录地址最直接，职责也最清晰。

2. 前端获取 HTML 后再删除登录地址。
   不推荐。这样会让前端解析和改写后端 HTML，容易受模板结构变化影响，也会让打印内容职责分散。

3. 增加后端参数控制是否展示登录地址。
   不推荐。当前没有继续展示登录地址的已知场景，增加参数会扩大接口复杂度。

## 测试

更新后端打印相关测试：

- `PrintCredentialServiceTest` 应断言 HTML 包含部门、用户名、密码和打印样式。
- `PrintCredentialServiceTest` 应断言 HTML 不包含 `登录地址`。
- `PrintCredentialServiceTest` 应断言 HTML 不包含配置的移动端登录 URL。
- `EvaluatorUserControllerTest.printsFilteredEvaluatorCredentialsAsHtml` 应断言接口返回的 HTML 不包含 `登录地址` 和 `/mobile/login`，并继续断言包含筛选后的部门、用户名、密码。

实现后运行后端相关测试，必要时再运行完整后端测试。
