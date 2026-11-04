# 测评批次管理功能

## 功能概述

测评批次管理功能允许管理员创建、切换和管理不同测评批次，实现数据隔离。每个批次的部门、被测评人员、测评用户和评价记录完全独立。

## 核心功能

### 批次管理
- **创建批次**：管理员可以创建新的测评批次
- **切换批次**：右上角批次选择器快速切换当前批次
- **删除批次**：删除空批次（有数据的批次不能删除）
- **自动选择**：登录后自动选中最新创建的批次

### 数据隔离
- 所有管理页面数据按当前选中批次过滤
- 切换批次后，所有页面数据自动刷新
- 创建数据时自动关联当前批次
- 批次间数据完全隔离，互不影响

## 使用说明

### 管理员端使用

1. **登录系统**
   - 使用管理员账号登录
   - 系统自动加载最新批次

2. **创建新批次**
   - 点击右上角批次选择器
   - 选择"新建批次"
   - 输入批次名称（例如：2026年11月测评）
   - 确认创建

3. **切换批次**
   - 点击右上角批次选择器
   - 从下拉列表选择要切换的批次
   - 所有页面数据自动更新

4. **删除批次**
   - 点击右上角批次选择器
   - 点击要删除批次旁的删除图标
   - 确认删除（注意：有数据的批次不能删除）

### 移动端使用

移动端测评用户不需要关心批次，系统自动根据其账号所属批次加载对应数据。

## 技术实现

### 后端实现
- 数据库表：`evaluation_period`
- 所有核心表添加 `evaluation_period_id` 外键
- 请求头：`X-Evaluation-Period-Id` 自动传递当前批次
- 级联删除：删除批次时自动删除关联数据

### 前端实现
- 状态管理：zustand + localStorage 持久化
- 请求拦截：自动添加批次请求头
- 组件：PeriodSelector 批次选择器
- 监听：所有管理页面监听批次切换并刷新

## 注意事项

1. **删除限制**
   - 不能删除最后一个批次
   - 不能删除当前选中的批次
   - 不能删除包含数据的批次

2. **数据迁移**
   - 首次运行迁移时，如果存在数据，会自动创建"默认批次"
   - 所有现有数据会关联到默认批次

3. **批次命名**
   - 批次名称最长 255 个字符
   - 建议使用描述性名称，例如：2026年11月测评、2026年度中期测评

## API 端点

### 批次管理
```
GET    /api/admin/evaluation-periods      # 获取批次列表
POST   /api/admin/evaluation-periods      # 创建批次
DELETE /api/admin/evaluation-periods/{id} # 删除批次
```

### 请求示例
```bash
# 创建批次
curl -X POST http://localhost:8080/api/admin/evaluation-periods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"2026年11月测评"}'

# 获取批次列表
curl http://localhost:8080/api/admin/evaluation-periods \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 故障排查

### 问题：切换批次后数据没有刷新
- 检查浏览器控制台是否有错误
- 确认请求头中包含 `X-Evaluation-Period-Id`
- 刷新页面重试

### 问题：无法删除批次
- 确认批次下没有数据（部门、人员、用户、记录）
- 确认不是最后一个批次
- 确认不是当前选中的批次

### 问题：登录后没有批次
- 检查数据库是否有批次记录
- 手动创建一个批次
- 检查后端日志

## 开发说明

如需添加新的管理页面并支持批次切换，参考以下代码：

```typescript
import { usePeriodStore } from '@/stores/periodStore';

export default function YourPage() {
  const actionRef = useRef<ActionType>();
  const { currentPeriod } = usePeriodStore();

  // 监听批次切换
  useEffect(() => {
    actionRef.current?.reload();
  }, [currentPeriod]);

  // 其余代码...
}
```
