import { KeyOutlined, PrinterOutlined, PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  ProFormDigit,
  ProFormSelect,
  ProTable,
  type ActionType,
  type ProColumns
} from '@ant-design/pro-components';
import { Button, Space, Tag, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { listDepartments, type Department } from '../../services/departments';
import {
  batchGenerateEvaluatorUsers,
  fetchEvaluatorUsersPrintHtml,
  listEvaluatorUsers,
  resetEvaluatorPassword,
  setEvaluatorUserEnabled,
  type BatchGenerateEvaluatorUsersPayload,
  type EvaluatorUser,
  type EvaluatorUserListParams
} from '../../services/evaluatorUsers';
import { usePeriodStore } from '../../stores/periodStore';

function parseBoolean(value: unknown): boolean | undefined {
  if (typeof value === 'boolean') {
    return value;
  }
  if (value === 'true') {
    return true;
  }
  if (value === 'false') {
    return false;
  }
  return undefined;
}

export default function EvaluatorUsers() {
  const actionRef = useRef<ActionType>();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [filters, setFilters] = useState<Omit<EvaluatorUserListParams, 'page' | 'pageSize'>>({});
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
    actionRef.current?.reload();
  }, [currentPeriod]);

  const departmentOptions = useMemo(
    () => departments.map((department) => ({ label: department.name, value: department.id })),
    [departments]
  );

  const departmentValueEnum = useMemo(
    () =>
      departments.reduce<Record<string, { text: string }>>((result, department) => {
        result[String(department.id)] = { text: department.name };
        return result;
      }, {}),
    [departments]
  );

  const columns: ProColumns<EvaluatorUser>[] = [
    {
      title: '账号',
      dataIndex: 'username'
    },
    {
      title: '初始密码',
      dataIndex: 'passwordPlain',
      search: false
    },
    {
      title: '部门',
      dataIndex: 'departmentId',
      valueType: 'select',
      valueEnum: departmentValueEnum,
      render: (_, record) => record.departmentName
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      valueType: 'select',
      valueEnum: {
        true: { text: '启用' },
        false: { text: '禁用' }
      },
      render: (_, record) => (
        <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '禁用'}</Tag>
      )
    },
    {
      title: '是否提交',
      dataIndex: 'submitted',
      valueType: 'select',
      valueEnum: {
        true: { text: '已提交' },
        false: { text: '未提交' }
      },
      render: (_, record) => (
        <Tag color={record.submitted ? 'blue' : 'default'}>
          {record.submitted ? '已提交' : '未提交'}
        </Tag>
      )
    },
    {
      title: '提交时间',
      dataIndex: 'submittedAt',
      valueType: 'dateTime',
      search: false,
      renderText: (value) => value ?? '-'
    },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 280,
      render: (_, record) => [
        <Button
          key="reset"
          icon={<KeyOutlined />}
          type="link"
          onClick={async () => {
            const result = await resetEvaluatorPassword(record.id);
            message.success(`密码已重置：${result.passwordPlain}`);
            actionRef.current?.reload();
          }}
        >
          重置密码
        </Button>,
        <Button
          key={record.enabled ? 'disable' : 'enable'}
          type="link"
          onClick={async () => {
            await setEvaluatorUserEnabled(record.id, !record.enabled);
            message.success(record.enabled ? '已禁用' : '已启用');
            actionRef.current?.reload();
          }}
        >
          {record.enabled ? '禁用' : '启用'}
        </Button>
      ]
    }
  ];

  async function handleBatchGenerate(values: BatchGenerateEvaluatorUsersPayload) {
    const result = await batchGenerateEvaluatorUsers(values);
    message.success(`已生成 ${result.created} 个测评用户`);
    setGenerateOpen(false);
    actionRef.current?.reload();
    return true;
  }

  async function handlePrint() {
    try {
      const html = await fetchEvaluatorUsersPrintHtml(filters);
      const blobUrl = URL.createObjectURL(new Blob([html], { type: 'text/html;charset=utf-8' }));
      window.open(blobUrl, '_blank', 'noopener,noreferrer');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '打印页面加载失败');
    }
  }

  return (
    <>
      <ProTable<EvaluatorUser>
        actionRef={actionRef}
        columns={columns}
        headerTitle="测评用户"
        rowKey="id"
        request={async (params) => {
          const nextFilters = {
            departmentId:
              typeof params.departmentId === 'number'
                ? params.departmentId
                : typeof params.departmentId === 'string'
                  ? Number(params.departmentId)
                  : undefined,
            submitted: parseBoolean(params.submitted),
            enabled: parseBoolean(params.enabled)
          };
          setFilters(nextFilters);

          const response = await listEvaluatorUsers({
            page: params.current,
            pageSize: params.pageSize,
            ...nextFilters
          });

          return {
            data: response.items,
            success: true,
            total: response.total
          };
        }}
        toolBarRender={() => [
          <Space key="toolbar">
            <Button
              icon={<PlusOutlined />}
              type="primary"
              onClick={() => {
                setGenerateOpen(true);
              }}
            >
              批量生成账号
            </Button>
            <Button
              icon={<PrinterOutlined />}
              onClick={handlePrint}
            >
              打印账号
            </Button>
          </Space>
        ]}
      />
      <ModalForm<BatchGenerateEvaluatorUsersPayload>
        initialValues={{ count: 10, departmentId: departmentOptions[0]?.value }}
        modalProps={{ destroyOnHidden: true }}
        onFinish={handleBatchGenerate}
        open={generateOpen}
        onOpenChange={setGenerateOpen}
        title="批量生成测评用户"
      >
        <ProFormSelect
          label="部门"
          name="departmentId"
          options={departmentOptions}
          rules={[{ required: true, message: '请选择部门' }]}
        />
        <ProFormDigit
          label="生成数量"
          name="count"
          min={1}
          fieldProps={{ precision: 0 }}
          rules={[{ required: true, message: '请输入生成数量' }]}
        />
      </ModalForm>
    </>
  );
}
