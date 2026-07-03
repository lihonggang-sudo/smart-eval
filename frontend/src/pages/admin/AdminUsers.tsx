import { EditOutlined, KeyOutlined, PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  ProFormText,
  ProTable,
  type ActionType,
  type ProColumns
} from '@ant-design/pro-components';
import { Button, Tag, message } from 'antd';
import { useRef, useState } from 'react';
import {
  createAdminUser,
  listAdminUsers,
  resetAdminUserPassword,
  setAdminUserEnabled,
  updateAdminUser,
  type AdminUser,
  type CreateAdminUserPayload,
  type UpdateAdminUserPayload
} from '../../services/adminUsers';

type UserModalState =
  | { mode: 'create' }
  | { mode: 'edit'; record: AdminUser }
  | null;

export default function AdminUsers() {
  const actionRef = useRef<ActionType>();
  const [userModal, setUserModal] = useState<UserModalState>(null);
  const [passwordModalRecord, setPasswordModalRecord] = useState<AdminUser | null>(null);

  const columns: ProColumns<AdminUser>[] = [
    {
      title: '用户名',
      dataIndex: 'username'
    },
    {
      title: '显示名称',
      dataIndex: 'displayName'
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
      search: false
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
            setUserModal({ mode: 'edit', record });
          }}
        >
          编辑
        </Button>,
        <Button
          key="reset"
          icon={<KeyOutlined />}
          type="link"
          onClick={() => {
            setPasswordModalRecord(record);
          }}
        >
          重置密码
        </Button>,
        <Button
          key={record.enabled ? 'disable' : 'enable'}
          type="link"
          onClick={async () => {
            await setAdminUserEnabled(record.id, !record.enabled);
            message.success(record.enabled ? '已禁用' : '已启用');
            actionRef.current?.reload();
          }}
        >
          {record.enabled ? '禁用' : '启用'}
        </Button>
      ]
    }
  ];

  async function handleUserSubmit(values: CreateAdminUserPayload | UpdateAdminUserPayload) {
    if (userModal?.mode === 'edit') {
      await updateAdminUser(userModal.record.id, values as UpdateAdminUserPayload);
      message.success('管理员已更新');
    } else {
      await createAdminUser(values as CreateAdminUserPayload);
      message.success('管理员已创建');
    }

    setUserModal(null);
    actionRef.current?.reload();
    return true;
  }

  return (
    <>
      <ProTable<AdminUser>
        actionRef={actionRef}
        columns={columns}
        headerTitle="管理员账号"
        rowKey="id"
        request={async (params) => {
          const response = await listAdminUsers({
            page: params.current,
            pageSize: params.pageSize
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
              setUserModal({ mode: 'create' });
            }}
          >
            新增管理员
          </Button>
        ]}
      />
      <ModalForm<CreateAdminUserPayload | UpdateAdminUserPayload>
        key={userModal?.mode === 'edit' ? userModal.record.id : 'create'}
        initialValues={
          userModal?.mode === 'edit'
            ? {
                displayName: userModal.record.displayName
              }
            : {
                username: '',
                password: '',
                displayName: ''
              }
        }
        modalProps={{ destroyOnHidden: true }}
        onFinish={handleUserSubmit}
        open={userModal !== null}
        onOpenChange={(open) => {
          if (!open) {
            setUserModal(null);
          }
        }}
        title={userModal?.mode === 'edit' ? '编辑管理员' : '新增管理员'}
      >
        {userModal?.mode !== 'edit' ? (
          <>
            <ProFormText
              label="用户名"
              name="username"
              placeholder="请输入用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
            />
            <ProFormText.Password
              label="初始密码"
              name="password"
              placeholder="请输入初始密码"
              rules={[{ required: true, message: '请输入初始密码' }]}
            />
          </>
        ) : null}
        <ProFormText
          label="显示名称"
          name="displayName"
          placeholder="请输入显示名称"
          rules={[{ required: true, message: '请输入显示名称' }]}
        />
      </ModalForm>
      <ModalForm<{ password: string }>
        key={passwordModalRecord?.id ?? 'password'}
        modalProps={{ destroyOnHidden: true }}
        onFinish={async (values) => {
          if (!passwordModalRecord) {
            return false;
          }

          await resetAdminUserPassword(passwordModalRecord.id, values.password);
          message.success('密码已重置');
          setPasswordModalRecord(null);
          return true;
        }}
        open={passwordModalRecord !== null}
        onOpenChange={(open) => {
          if (!open) {
            setPasswordModalRecord(null);
          }
        }}
        title="重置管理员密码"
      >
        <ProFormText.Password
          label="新密码"
          name="password"
          placeholder="请输入新密码"
          rules={[{ required: true, message: '请输入新密码' }]}
        />
      </ModalForm>
    </>
  );
}
