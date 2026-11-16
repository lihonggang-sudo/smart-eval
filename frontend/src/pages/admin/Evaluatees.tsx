import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  ProFormSelect,
  ProFormSwitch,
  ProFormText,
  ProTable,
  type ActionType,
  type ProColumns
} from '@ant-design/pro-components';
import { Button, Modal, Tag, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { listDepartments, type Department } from '../../services/departments';
import {
  createEvaluatee,
  deleteEvaluatee,
  listEvaluatees,
  setEvaluateeEnabled,
  updateEvaluatee,
  type Evaluatee,
  type EvaluateePayload
} from '../../services/evaluatees';
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

export default function Evaluatees() {
  const actionRef = useRef<ActionType>();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<Evaluatee | null>(null);
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

  const columns: ProColumns<Evaluatee>[] = [
    {
      title: '姓名',
      dataIndex: 'name'
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
      title: '创建时间',
      dataIndex: 'createdAt',
      valueType: 'dateTime',
      search: false,
      width: 200
    },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 280,
      render: (_, record) => [
        <Button
          key="edit"
          icon={<EditOutlined />}
          type="link"
          onClick={() => {
            setEditingRecord(record);
            setModalOpen(true);
          }}
        >
          编辑
        </Button>,
        <Button
          key={record.enabled ? 'disable' : 'enable'}
          type="link"
          onClick={async () => {
            await setEvaluateeEnabled(record.id, !record.enabled);
            message.success(record.enabled ? '已禁用' : '已启用');
            actionRef.current?.reload();
          }}
        >
          {record.enabled ? '禁用' : '启用'}
        </Button>,
        <Button
          key="delete"
          icon={<DeleteOutlined />}
          type="link"
          danger
          onClick={() => {
            Modal.confirm({
              title: '确认删除',
              content: `确定要删除被测评人员"${record.name}"吗？此操作不可恢复。`,
              okText: '确定',
              okType: 'danger',
              cancelText: '取消',
              onOk: async () => {
                try {
                  await deleteEvaluatee(record.id);
                  message.success('删除成功');
                  actionRef.current?.reload();
                } catch (error) {
                  message.error('删除失败，请稍后重试');
                }
              }
            });
          }}
        >
          删除
        </Button>
      ]
    }
  ];

  async function handleSubmit(values: EvaluateePayload) {
    if (editingRecord) {
      await updateEvaluatee(editingRecord.id, values);
      message.success('被测评人员已更新');
    } else {
      await createEvaluatee(values);
      message.success('被测评人员已创建');
    }

    setModalOpen(false);
    setEditingRecord(null);
    actionRef.current?.reload();
    return true;
  }

  return (
    <>
      <ProTable<Evaluatee>
        actionRef={actionRef}
        columns={columns}
        headerTitle="被测评人员"
        rowKey="id"
        request={async (params) => {
          const response = await listEvaluatees({
            page: params.current,
            pageSize: params.pageSize,
            departmentId:
              typeof params.departmentId === 'number'
                ? params.departmentId
                : typeof params.departmentId === 'string'
                  ? Number(params.departmentId)
                  : undefined,
            name: typeof params.name === 'string' ? params.name : undefined,
            enabled: parseBoolean(params.enabled)
          });

          return {
            data: response.items,
            success: true,
            total: response.total
          };
        }}
        toolBarRender={() => [
          <Button
            key="create"
            icon={<PlusOutlined />}
            type="primary"
            onClick={() => {
              setEditingRecord(null);
              setModalOpen(true);
            }}
          >
            新增被测评人员
          </Button>
        ]}
      />
      <ModalForm<EvaluateePayload>
        key={editingRecord?.id ?? 'create'}
        initialValues={
          editingRecord
            ? {
                name: editingRecord.name,
                departmentId: editingRecord.departmentId,
                enabled: editingRecord.enabled
              }
            : {
                name: '',
                departmentId: departmentOptions[0]?.value,
                enabled: true
              }
        }
        modalProps={{
          destroyOnHidden: true,
          onCancel: () => {
            setModalOpen(false);
            setEditingRecord(null);
          }
        }}
        onFinish={handleSubmit}
        open={modalOpen}
        onOpenChange={(open) => {
          setModalOpen(open);
          if (!open) {
            setEditingRecord(null);
          }
        }}
        title={editingRecord ? '编辑被测评人员' : '新增被测评人员'}
      >
        <ProFormText
          label="姓名"
          name="name"
          placeholder="请输入姓名"
          rules={[{ required: true, message: '请输入姓名' }]}
        />
        <ProFormSelect
          label="所属部门"
          name="departmentId"
          options={departmentOptions}
          rules={[{ required: true, message: '请选择所属部门' }]}
        />
        <ProFormSwitch label="启用" name="enabled" />
      </ModalForm>
    </>
  );
}
