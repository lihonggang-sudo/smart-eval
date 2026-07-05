import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { listDepartments, type Department } from '../../services/departments';
import { listEvaluatees, type Evaluatee } from '../../services/evaluatees';
import { listEvaluatorUsers, type EvaluatorUser } from '../../services/evaluatorUsers';
import {
  getResultDetails,
  type EvaluationGrade,
  type ResultDetail
} from '../../services/results';

const gradeValueEnum: Record<EvaluationGrade, { text: string }> = {
  EXCELLENT: { text: '优秀' },
  COMPETENT: { text: '称职' },
  BASIC_COMPETENT: { text: '基本称职' },
  INCOMPETENT: { text: '不称职' }
};

function parseNumber(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined;
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}

export default function ResultDetails() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [evaluatees, setEvaluatees] = useState<Evaluatee[]>([]);
  const [evaluatorUsers, setEvaluatorUsers] = useState<EvaluatorUser[]>([]);

  useEffect(() => {
    void Promise.all([
      listDepartments({ page: 1, pageSize: 200 }),
      listEvaluatees({ page: 1, pageSize: 200 }),
      listEvaluatorUsers({ page: 1, pageSize: 200 })
    ])
      .then(([departmentResponse, evaluateeResponse, evaluatorResponse]) => {
        setDepartments(departmentResponse.items);
        setEvaluatees(evaluateeResponse.items);
        setEvaluatorUsers(evaluatorResponse.items);
      })
      .catch(() => {
        message.error('筛选条件加载失败，请稍后重试');
      });
  }, []);

  const departmentValueEnum = useMemo(
    () =>
      departments.reduce<Record<string, { text: string }>>((result, department) => {
        result[String(department.id)] = { text: department.name };
        return result;
      }, {}),
    [departments]
  );

  const evaluateeValueEnum = useMemo(
    () =>
      evaluatees.reduce<Record<string, { text: string }>>((result, evaluatee) => {
        result[String(evaluatee.id)] = { text: evaluatee.name };
        return result;
      }, {}),
    [evaluatees]
  );

  const evaluatorValueEnum = useMemo(
    () =>
      evaluatorUsers.reduce<Record<string, { text: string }>>((result, evaluator) => {
        result[String(evaluator.id)] = { text: evaluator.username };
        return result;
      }, {}),
    [evaluatorUsers]
  );

  const columns: ProColumns<ResultDetail>[] = [
    {
      title: '部门',
      dataIndex: 'departmentId',
      valueType: 'select',
      valueEnum: departmentValueEnum,
      hideInTable: true
    },
    {
      title: '测评人',
      dataIndex: 'evaluatorUserId',
      valueType: 'select',
      valueEnum: evaluatorValueEnum,
      render: (_, record) => record.evaluatorUsername
    },
    {
      title: '测评人部门',
      dataIndex: 'evaluatorDepartmentName',
      search: false
    },
    {
      title: '被测评人员',
      dataIndex: 'evaluateeId',
      valueType: 'select',
      valueEnum: evaluateeValueEnum,
      render: (_, record) => record.evaluateeName
    },
    {
      title: '评价结果',
      dataIndex: 'grade',
      valueType: 'select',
      valueEnum: gradeValueEnum,
      render: (_, record) => record.gradeLabel
    },
    {
      title: '问题及表现',
      dataIndex: 'comment1',
      search: false,
      renderText: (value) => value ?? '-'
    },
    {
      title: '意见或建议',
      dataIndex: 'comment2',
      search: false,
      renderText: (value) => value ?? '-'
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      valueType: 'dateTime',
      search: false
    }
  ];

  return (
    <ProTable<ResultDetail>
      columns={columns}
      headerTitle="评价明细"
      rowKey={(record) => `${record.evaluatorUserId}-${record.evaluateeId}-${record.createdAt}`}
      request={async (params) => {
        const response = await getResultDetails({
          page: params.current,
          pageSize: params.pageSize,
          departmentId: parseNumber(params.departmentId),
          evaluateeId: parseNumber(params.evaluateeId),
          evaluatorUserId: parseNumber(params.evaluatorUserId),
          grade: typeof params.grade === 'string' ? (params.grade as EvaluationGrade) : undefined
        });

        return {
          data: response.items,
          success: true,
          total: response.total
        };
      }}
    />
  );
}
