import { Alert, Button, Form, Input, Layout, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mobileLogin } from '../../services/auth';
import { saveToken } from '../../services/session';
import { clearMobileEvaluationDraft } from './MobileLayout';

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
    <Layout
      data-testid="mobile-login-shell"
      style={{
        minHeight: '100vh',
        width: '100%',
        boxSizing: 'border-box',
        background: '#eef3f8',
        overflowX: 'hidden',
        padding: '24px 12px'
      }}
    >
      <main
        data-testid="mobile-login-card-wrap"
        style={{
          width: '100%',
          maxWidth: 'min(100%, 390px)',
          boxSizing: 'border-box',
          display: 'flex',
          minHeight: 'calc(100vh - 48px)',
          margin: '0 auto'
        }}
      >
        <section
          data-testid="mobile-login-card"
          style={{
            overflow: 'hidden',
            borderRadius: 8,
            background: '#ffffff',
            boxSizing: 'border-box',
            display: 'flex',
            flexDirection: 'column',
            minHeight: 'calc(100vh - 48px)',
            width: '100%',
            border: '1px solid #d8dee8',
            boxShadow: '0 16px 40px rgba(31, 45, 61, 0.10)'
          }}
        >
          <div
            style={{
              height: 6,
              background: '#8a1f2d'
            }}
          />
          <div
            data-testid="mobile-login-card-body"
            style={{
              display: 'flex',
              flex: '1 1 auto',
              flexDirection: 'column',
              padding: '24px 18px 22px'
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                marginBottom: 20,
                minWidth: 0
              }}
            >
              <div
                aria-hidden="true"
                style={{
                  width: 42,
                  height: 42,
                  flex: '0 0 42px',
                  borderRadius: 6,
                  background: '#8a1f2d',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20,
                  fontWeight: 700
                }}
              >
                评
              </div>
              <div data-testid="mobile-login-title-text" style={{ minWidth: 0, flex: '1 1 0%' }}>
                <Typography.Title level={2} style={{ margin: 0, color: '#1f2937', fontSize: 26 }}>
                  鲁东大学
                </Typography.Title>
                <Typography.Text
                  style={{
                    color: '#5f6b7a',
                    display: 'block',
                    fontSize: 14,
                    lineHeight: 1.6,
                    whiteSpace: 'normal'
                  }}
                >
                  领导干部试用期满民主测评系统
                </Typography.Text>
              </div>
            </div>

            <div
              style={{
                borderTop: '1px solid #eef1f5',
                paddingTop: 18,
                marginBottom: 18
              }}
            >
              <Typography.Title level={4} style={{ margin: 0, color: '#1f2937' }}>
                测评登录
              </Typography.Title>
              <Typography.Text style={{ color: '#6b7280', fontSize: 13 }}>
                请核对账号信息后进入测评。
              </Typography.Text>
            </div>

            <Form
              layout="vertical"
              size="large"
              style={{ display: 'flex', flex: '1 1 auto', flexDirection: 'column' }}
              onFinish={handleFinish}
            >
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

              <div
                data-testid="mobile-login-actions"
                style={{
                  marginTop: 'auto',
                  paddingTop: 24
                }}
              >
                {errorMessage ? (
                  <Alert
                    style={{ marginBottom: 16 }}
                    message={errorMessage}
                    type="error"
                    showIcon
                  />
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
                  进入测评
                </Button>

                <Typography.Paragraph
                  style={{
                    margin: '18px 0 0',
                    color: '#6b7280',
                    fontSize: 13,
                    lineHeight: 1.7
                  }}
                >
                  请使用分配的测评账号登录，测评结果将按规定保密。
                </Typography.Paragraph>
              </div>
            </Form>
          </div>
        </section>
      </main>
    </Layout>
  );
}
