import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Form, Input, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminCopyright from '../../components/AdminCopyright';
import { adminLogin } from '../../services/auth';
import { saveToken } from '../../services/session';
import { usePeriodStore } from '../../stores/periodStore';

export default function AdminLogin() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const { setCurrentPeriod } = usePeriodStore();

  async function handleFinish(values: { username: string; password: string }) {
    setSubmitting(true);
    setErrorMessage('');

    try {
      const result = await adminLogin(values.username, values.password);
      saveToken(result.token);
      if (result.latestPeriod) {
        setCurrentPeriod(result.latestPeriod);
      }
      navigate('/admin');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '登录失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main
      data-testid="admin-login-shell"
      style={{
        minHeight: '100vh',
        width: '100%',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#eef3f8',
        padding: '32px 24px'
      }}
    >
      <div
        style={{
          width: '100%',
          flex: '1 1 auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <section
          data-testid="admin-login-layout"
          style={{
            width: '100%',
            maxWidth: 1040,
            minHeight: 560,
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 1fr) minmax(360px, 420px)',
            overflow: 'hidden',
            borderRadius: 8,
            background: '#ffffff',
            border: '1px solid #d8dee8',
            boxShadow: '0 20px 54px rgba(31, 45, 61, 0.12)'
          }}
        >
          <aside
            data-testid="admin-login-brand-panel"
            style={{
              position: 'relative',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              background: '#f7f9fc',
              borderRight: '1px solid #e5e9f0',
              padding: '48px 44px'
            }}
          >
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                height: 6,
                background: '#8a1f2d'
              }}
            />

            <div>
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: 8,
                  background: '#8a1f2d',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 22,
                  fontWeight: 700,
                  marginBottom: 28
                }}
              >
                评
              </div>
              <Typography.Text
                style={{
                  color: '#8a1f2d',
                  display: 'block',
                  fontSize: 14,
                  fontWeight: 600,
                  marginBottom: 10
                }}
              >
                管理后台
              </Typography.Text>
              <Typography.Title
                level={1}
                style={{ color: '#1f2937', fontSize: 34, margin: '0 0 12px' }}
              >
                鲁东大学
              </Typography.Title>
              <Typography.Title
                level={2}
                style={{ color: '#1f2937', fontSize: 24, fontWeight: 600, margin: 0 }}
              >
                领导干部试用期满民主测评系统
              </Typography.Title>
              <Typography.Paragraph
                style={{
                  color: '#5f6b7a',
                  fontSize: 15,
                  lineHeight: 1.8,
                  margin: '28px 0 0',
                  maxWidth: 430
                }}
              >
                用于测评对象、测评用户、测评结果与后台账号的统一管理。
              </Typography.Paragraph>
            </div>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                color: '#5f6b7a',
                fontSize: 14
              }}
            >
              <SafetyCertificateOutlined style={{ color: '#8a1f2d', fontSize: 18 }} />
              <span>仅限授权管理员使用</span>
            </div>
          </aside>

          <section
            style={{
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              padding: '52px 44px'
            }}
          >
            <div style={{ marginBottom: 28 }}>
              <Typography.Text
                style={{
                  color: '#8a1f2d',
                  display: 'block',
                  fontSize: 14,
                  fontWeight: 600,
                  marginBottom: 8
                }}
              >
                系统入口
              </Typography.Text>
              <Typography.Title level={2} style={{ color: '#1f2937', margin: 0 }}>
                管理员登录
              </Typography.Title>
              <Typography.Text style={{ color: '#6b7280', fontSize: 14 }}>
                请输入管理员账号和密码进入后台。
              </Typography.Text>
            </div>

            <Form layout="vertical" size="large" onFinish={handleFinish}>
              <Form.Item
                label="用户名"
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input autoComplete="username" placeholder="请输入用户名" prefix={<UserOutlined />} />
              </Form.Item>
              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password
                  autoComplete="current-password"
                  placeholder="请输入密码"
                  prefix={<LockOutlined />}
                />
              </Form.Item>
              {errorMessage ? (
                <Alert style={{ marginBottom: 16 }} message={errorMessage} type="error" showIcon />
              ) : null}
              <Button
                block
                htmlType="submit"
                loading={submitting}
                size="large"
                style={{
                  height: 46,
                  borderRadius: 6,
                  background: '#8a1f2d',
                  borderColor: '#8a1f2d',
                  fontWeight: 600
                }}
                type="primary"
              >
                登录系统
              </Button>
            </Form>
          </section>
        </section>
      </div>
      <AdminCopyright />
    </main>
  );
}
