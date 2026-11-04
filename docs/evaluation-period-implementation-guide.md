# 测评批次管理功能实施说明

## 已完成的核心功能

### 后端实现 ✅
1. 数据库迁移 - `V2__add_evaluation_period.sql`
2. EvaluationPeriod 实体、Repository、Service、Controller
3. 批次上下文管理（Context、Filter、SecurityConfig 集成）
4. 所有核心实体添加 `evaluationPeriodId` 字段
5. Service 层完整集成批次上下文
6. 管理员登录返回最新批次信息

### 前端实现 ✅
1. 批次 API 服务 (`evaluationPeriods.ts`)
2. 批次状态管理 (`periodStore.ts`)
3. 请求拦截器自动添加批次请求头
4. 批次选择器组件 (`PeriodSelector.tsx`)
5. 管理端布局集成批次选择器
6. 登录时设置初始批次

### 示例：页面监听批次切换

已为 `Departments.tsx` 添加批次切换监听，其他管理页面需要按相同模式修改：

```typescript
import { usePeriodStore } from '../../stores/periodStore';

export default function YourPage() {
  const actionRef = useRef<ActionType>();
  const { currentPeriod } = usePeriodStore();

  // 监听批次切换并刷新数据
  useEffect(() => {
    actionRef.current?.reload();
  }, [currentPeriod]);

  // ... 其余代码
}
```

需要添加此监听的页面：
- ✅ `Departments.tsx` (已完成)
- ⚠️ `Evaluatees.tsx`
- ⚠️ `EvaluatorUsers.tsx`
- ⚠️ `ResultSummary.tsx`
- ⚠️ `ResultDetails.tsx`

## 功能测试建议

### 后端测试
```bash
cd backend
# 启动应用
mvn spring-boot:run

# 测试批次 API
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用返回的 token 和 latestPeriod.id 测试批次操作
```

### 前端测试
```bash
cd frontend
npm run dev

# 测试流程：
# 1. 管理员登录
# 2. 右上角查看批次选择器
# 3. 创建新批次
# 4. 切换批次
# 5. 验证各管理页面数据切换
# 6. 测试删除批次
```

## 剩余工作

1. **其他管理页面添加批次监听** - 按上述模式修改 4 个页面
2. **端到端测试** - 完整测试批次创建、切换、删除流程
3. **更新 README** - 添加批次管理功能说明

## 技术要点

- **批次隔离**：所有数据按 `evaluation_period_id` 隔离
- **自动过滤**：请求拦截器自动添加 `X-Evaluation-Period-Id` 请求头
- **状态持久化**：批次选择使用 zustand persist 本地存储
- **级联删除**：数据库外键设置为 `ON DELETE CASCADE`
- **删除保护**：不能删除有数据的批次；不能删除最后一个批次

## 数据迁移处理

迁移脚本自动检测现有数据：
- 如果存在数据，自动创建"默认批次"并关联所有现有数据
- 如果是空数据库，直接添加约束

## API 端点

### 批次管理
- `GET /api/admin/evaluation-periods` - 获取所有批次列表
- `POST /api/admin/evaluation-periods` - 创建新批次
- `DELETE /api/admin/evaluation-periods/{id}` - 删除批次

### 请求头
- `X-Evaluation-Period-Id: {periodId}` - 管理员接口自动添加
