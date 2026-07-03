import { Card, Col, Row, Statistic, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { listDepartments } from '../../services/departments';
import { listEvaluatees } from '../../services/evaluatees';
import { listEvaluatorUsers } from '../../services/evaluatorUsers';

type DashboardStats = {
  departmentCount: number;
  evaluateeCount: number;
  evaluatorCount: number;
  submittedCount: number;
  unsubmittedCount: number;
};

const initialStats: DashboardStats = {
  departmentCount: 0,
  evaluateeCount: 0,
  evaluatorCount: 0,
  submittedCount: 0,
  unsubmittedCount: 0
};

export default function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats>(initialStats);

  useEffect(() => {
    void Promise.all([
      listDepartments({ page: 1, pageSize: 1 }),
      listEvaluatees({ page: 1, pageSize: 1 }),
      listEvaluatorUsers({ page: 1, pageSize: 1 }),
      listEvaluatorUsers({ page: 1, pageSize: 1, submitted: true }),
      listEvaluatorUsers({ page: 1, pageSize: 1, submitted: false })
    ])
      .then(
        ([
          departmentsResponse,
          evaluateesResponse,
          evaluatorsResponse,
          submittedResponse,
          unsubmittedResponse
        ]) => {
          setStats({
            departmentCount: departmentsResponse.total,
            evaluateeCount: evaluateesResponse.total,
            evaluatorCount: evaluatorsResponse.total,
            submittedCount: submittedResponse.total,
            unsubmittedCount: unsubmittedResponse.total
          });
        }
      )
      .catch(() => {
        message.error('工作台统计加载失败，请稍后重试');
      });
  }, []);

  return (
    <div>
      <Typography.Title level={4}>管理员工作台</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="部门总数" value={stats.departmentCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="被测评人员" value={stats.evaluateeCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="测评用户" value={stats.evaluatorCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="已提交" value={stats.submittedCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card>
            <Statistic title="未提交" value={stats.unsubmittedCount} />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
