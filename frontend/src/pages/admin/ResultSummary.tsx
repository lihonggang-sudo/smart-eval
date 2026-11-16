import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { message } from 'antd';
import { useEffect, useMemo, useState, useRef } from 'react';
import { listDepartments, type Department } from '../../services/departments';
import { getResultSummary, type ResultSummary as ResultSummaryRecord } from '../../services/results';
import { usePeriodStore } from '../../stores/periodStore';

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

export default function ResultSummary() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const actionRef = useRef();
  const { currentPeriod } = usePeriodStore();

  useEffect(() => {
    void listDepartments({ page: 1, pageSize: 200 })
      .then((response) => {
        setDepartments(response.items);
      })
      .catch(() => {
        message.error('部门列表加载失败，请稍后重试');
      });
  }, []);

  // Refresh data when period changes
  useEffect(() => {
    if (actionRef.current) {
      (actionRef.current as any).reload?.();
    }
  }, [currentPeriod]);

  const departmentValueEnum = useMemo(
    () =>
      departments.reduce<Record<string, { text: string }>>((result, department) => {
        result[String(department.id)] = { text: department.name };
        return result;
      }, {}),
    [departments]
  );

  const columns: ProColumns<ResultSummaryRecord>[] = [
    {
      title: '被测评人员',
      dataIndex: 'evaluateeName'
    },
    {
      title: '部门',
      dataIndex: 'departmentId',
      valueType: 'select',
      valueEnum: departmentValueEnum,
      render: (_, record) => record.departmentName
    },
    {
      title: '优秀',
      dataIndex: 'excellentCount',
      search: false
    },
    {
      title: '称职',
      dataIndex: 'competentCount',
      search: false
    },
    {
      title: '基本称职',
      dataIndex: 'basicCompetentCount',
      search: false
    },
    {
      title: '不称职',
      dataIndex: 'incompetentCount',
      search: false
    },
    {
      title: '合计',
      dataIndex: 'totalCount',
      search: false
    }
  ];

  return (
    <ProTable<ResultSummaryRecord>
      columns={columns}
      headerTitle="结果汇总"
      rowKey="evaluateeId"
      pagination={{
        defaultPageSize: 20,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50', '100', '500', '5000', '10000'],
      }}
      request={async (params) => {
        const response = await getResultSummary({
          page: params.current,
          pageSize: params.pageSize,
          departmentId: parseNumber(params.departmentId),
          evaluateeName: typeof params.evaluateeName === 'string' ? params.evaluateeName : undefined
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
