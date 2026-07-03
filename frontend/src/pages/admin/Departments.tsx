import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  ProFormDigit,
  ProFormText,
  ProTable,
  type ActionType,
  type ProColumns
} from '@ant-design/pro-components';
import { Button, Popconfirm, message } from 'antd';
import { useRef, useState } from 'react';
import {
  createDepartment,
  deleteDepartment,
  listDepartments,
  updateDepartment,
  type Department,
  type DepartmentPayload
} from '../../services/departments';

export default function Departments() {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<Department | null>(null);

  const columns: ProColumns<Department>[] = [
    {
      title: '部门名称',
      dataIndex: 'name'
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      valueType: 'digit',
      width: 120
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      valueType: 'dateTime',
      width: 200,
      search: false
    },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 180,
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
        <Popconfirm
          key="delete"
          title="确认删除该部门吗？"
          onConfirm={async () => {
            await deleteDepartment(record.id);
            message.success('部门已删除');
            actionRef.current?.reload();
          }}
        >
          <Button danger icon={<DeleteOutlined />} type="link">
            删除
          </Button>
        </Popconfirm>
      ]
    }
  ];

  async function handleSubmit(values: DepartmentPayload) {
    if (editingRecord) {
      await updateDepartment(editingRecord.id, values);
      message.success('部门已更新');
    } else {
      await createDepartment(values);
      message.success('部门已创建');
    }

    setModalOpen(false);
    setEditingRecord(null);
    actionRef.current?.reload();
    return true;
  }

  return (
    <>
      <ProTable<Department>
        actionRef={actionRef}
        columns={columns}
        headerTitle="部门管理"
        rowKey="id"
        request={async (params) => {
          const response = await listDepartments({
            page: params.current,
            pageSize: params.pageSize,
            name: typeof params.name === 'string' ? params.name : undefined
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
            新增部门
          </Button>
        ]}
      />
      <ModalForm<DepartmentPayload>
        key={editingRecord?.id ?? 'create'}
        initialValues={
          editingRecord
            ? { name: editingRecord.name, sortOrder: editingRecord.sortOrder }
            : { name: '', sortOrder: 0 }
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
        title={editingRecord ? '编辑部门' : '新增部门'}
      >
        <ProFormText
          label="部门名称"
          name="name"
          placeholder="请输入部门名称"
          rules={[{ required: true, message: '请输入部门名称' }]}
        />
        <ProFormDigit
          label="排序"
          name="sortOrder"
          min={0}
          rules={[{ required: true, message: '请输入排序值' }]}
        />
      </ModalForm>
    </>
  );
}
