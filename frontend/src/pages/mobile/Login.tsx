import { Alert, Button, Form, Input } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mobileLogin } from '../../services/auth';
import { saveToken } from '../../services/session';
import MobileLayout, { clearMobileEvaluationDraft } from './MobileLayout';

export default function MobileLogin() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  async function handleFinish(values: { username: string; password: string }) {
    setSubmitting(true);
    setErrorMessage('');

    try {
      const result = await mobileLogin(values.username, values.password);
      saveToken(result.token);
      clearMobileEvaluationDraft();
      navigate('/mobile/evaluation');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '登录失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <MobileLayout title="评测登录" subtitle="请使用测评账号继续。">
      <Form layout="vertical" size="large" onFinish={handleFinish}>
        <Form.Item
          label="账号"
          name="username"
          rules={[{ required: true, message: '请输入账号' }]}
        >
          <Input autoComplete="username" placeholder="请输入账号" />
        </Form.Item>
        <Form.Item
          label="密码"
          name="password"
          rules={[{ required: true, message: '请输入密码' }]}
        >
          <Input.Password autoComplete="current-password" placeholder="请输入密码" />
        </Form.Item>
        {errorMessage ? (
          <Alert style={{ marginBottom: 16 }} message={errorMessage} type="error" showIcon />
        ) : null}
        <Button block htmlType="submit" loading={submitting} type="primary">
          登录
        </Button>
      </Form>
    </MobileLayout>
  );
}
