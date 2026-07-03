import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLogin } from '../../services/auth';
import { saveToken } from '../../services/session';

export default function AdminLogin() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  async function handleFinish(values: { username: string; password: string }) {
    setSubmitting(true);
    setErrorMessage('');

    try {
      const result = await adminLogin(values.username, values.password);
      saveToken(result.token);
      navigate('/admin');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '登录失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24
      }}
    >
      <Card style={{ width: '100%', maxWidth: 360 }}>
        <Typography.Title level={3}>管理员登录</Typography.Title>
        <Form layout="vertical" onFinish={handleFinish}>
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          {errorMessage ? (
            <Alert style={{ marginBottom: 16 }} message={errorMessage} type="error" showIcon />
          ) : null}
          <Button block htmlType="submit" loading={submitting} type="primary">
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
