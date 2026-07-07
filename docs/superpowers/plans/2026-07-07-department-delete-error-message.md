# 部门删除失败提示实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 管理员删除部门失败时，前端展示后端返回的业务错误 `message`。

**架构：** 请求层已经把失败响应转换成 `Error.message`，本次只在部门管理页面的删除确认回调中本地捕获异常并调用 Ant Design `message.error`。成功路径保持现状：提示删除成功并刷新表格；失败路径不刷新表格。

**技术栈：** React 18、Ant Design 5、Ant Design Pro Components、Vitest、Testing Library。

## 全局约束

- 设计文档和计划文档使用中文。
- 只修改部门删除失败提示，不改变新增部门、编辑部门或全局请求封装行为。
- 删除失败时优先展示 `Error.message`；非 `Error` 异常展示 `删除部门失败，请稍后重试`。
- 删除失败时不刷新部门列表。
- 不自动执行 `git commit`，实现完成后保留未提交变更给用户检查。

---

## 文件结构

- 修改：`frontend/src/pages/admin/Departments.tsx`
  - 职责：部门管理页面 UI 和删除确认回调。
- 修改：`frontend/src/tests/admin-pages.test.tsx`
  - 职责：后台管理页面渲染和交互测试，新增部门删除失败提示断言。

### Task 1: 部门删除失败提示

**Files:**
- Modify: `frontend/src/pages/admin/Departments.tsx`
- Test: `frontend/src/tests/admin-pages.test.tsx`

**Interfaces:**
- Consumes: `deleteDepartment(id: number): Promise<void>` from `frontend/src/services/departments.ts`
- Consumes: `message.error(content: string)` from `antd`
- Produces: 删除失败时展示后端错误文本，删除成功时保留原行为

- [x] **Step 1: 写失败测试**

在 `frontend/src/tests/admin-pages.test.tsx` 中扩展导入：

```tsx
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
```

把 services 导入改为：

```tsx
import { deleteDepartment, listDepartments } from '../services/departments';
```

在 `describe('admin management pages', () => { ... })` 中新增用例：

```tsx
  it('shows backend message when deleting a department fails', async () => {
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => {
      return undefined as never;
    });

    vi.mocked(deleteDepartment).mockRejectedValueOnce(
      new Error('部门下存在关联数据，不能删除')
    );

    render(<Departments />);

    fireEvent.click(await screen.findByText('删除'));
    fireEvent.click(await screen.findByRole('button', { name: /确 定|确定|OK/i }));

    await waitFor(() => {
      expect(deleteDepartment).toHaveBeenCalledWith(1);
      expect(errorSpy).toHaveBeenCalledWith('部门下存在关联数据，不能删除');
    });

    errorSpy.mockRestore();
  });
```

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
cd frontend
npm test -- src/tests/admin-pages.test.tsx --run
```

Expected: 新增用例失败，失败原因是 `message.error` 没有收到 `部门下存在关联数据，不能删除`。

- [x] **Step 3: 写最小实现**

在 `frontend/src/pages/admin/Departments.tsx` 中把删除确认回调改成：

```tsx
          onConfirm={async () => {
            try {
              await deleteDepartment(record.id);
              message.success('部门已删除');
              actionRef.current?.reload();
            } catch (error) {
              message.error(error instanceof Error ? error.message : '删除部门失败，请稍后重试');
            }
          }}
```

- [x] **Step 4: 运行相关测试确认通过**

Run:

```bash
cd frontend
npm test -- src/tests/admin-pages.test.tsx --run
```

Expected: `admin-pages.test.tsx` 全部通过。

- [x] **Step 5: 运行前端 lint**

Run:

```bash
cd frontend
npm run lint
```

Expected: lint 通过，没有新增错误。

- [x] **Step 6: 保留未提交变更**

```bash
git status --short
```

Expected: 显示本次修改文件，且没有产生新的提交。
