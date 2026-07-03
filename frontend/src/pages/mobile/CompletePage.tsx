import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';
import MobileLayout from './MobileLayout';

export default function CompletePage() {
  const navigate = useNavigate();

  return (
    <MobileLayout>
      <Result
        status="success"
        title="提交完成"
        subTitle="本次测评结果已提交。"
        extra={
          <Button type="primary" onClick={() => navigate('/mobile/login', { replace: true })}>
            返回登录
          </Button>
        }
      />
    </MobileLayout>
  );
}
