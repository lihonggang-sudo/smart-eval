import { Alert, Button, Space, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitEvaluation } from '../../services/mobileEvaluation';
import type { SubmitEvaluationRecord } from '../../services/mobileEvaluation';
import MobileLayout, {
  clearMobileEvaluationDraft,
  getValidMobileGrades,
  readMobileEvaluationDraft
} from './MobileLayout';

export default function ConfirmPage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [draft] = useState(() => readMobileEvaluationDraft());

  const records = useMemo<SubmitEvaluationRecord[]>(() => {
    if (!draft) {
      return [];
    }

    const validGrades = getValidMobileGrades(draft.grades);
    if (
      !draft.departmentName ||
      draft.items.length === 0 ||
      validGrades.length === 0
    ) {
      return [];
    }

    const validGradeValues = new Set(validGrades.map((grade) => grade.value));

    return draft.items
      .map((item) => {
        const grade = draft.selections[String(item.evaluateeId)];
        if (!grade || !validGradeValues.has(grade)) {
          return null;
        }

        return {
          evaluateeId: item.evaluateeId,
          grade
        };
      })
      .filter((record): record is SubmitEvaluationRecord => record !== null);
  }, [draft]);

  const isDraftReady = Boolean(draft) && records.length === (draft?.items.length ?? 0);

  useEffect(() => {
    if (!isDraftReady) {
      navigate('/mobile/evaluation', { replace: true });
    }
  }, [isDraftReady, navigate]);

  async function handleConfirm() {
    if (!draft || !isDraftReady) {
      navigate('/mobile/evaluation', { replace: true });
      return;
    }

    setSubmitting(true);
    setErrorMessage('');

    try {
      await submitEvaluation(records);
      navigate('/mobile/complete', { replace: true });
      clearMobileEvaluationDraft();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <MobileLayout
      title="提交确认"
      subtitle="提交后无法再次修改本次测评结果。"
      footer={
        <Space direction="vertical" size={12} style={{ display: 'flex' }}>
          <Button
            block
            disabled={!isDraftReady}
            loading={submitting}
            size="large"
            type="primary"
            onClick={handleConfirm}
          >
            确认提交
          </Button>
          <Button block size="large" onClick={() => navigate('/mobile/evaluation')}>
            返回修改
          </Button>
        </Space>
      }
    >
      <div
        style={{
          padding: 16,
          borderRadius: 8,
          background: '#ffffff',
          border: '1px solid #f0f0f0'
        }}
      >
        <Typography.Paragraph style={{ marginBottom: 12 }}>
          已完成 <Typography.Text strong>{records.length}</Typography.Text> 人评分。
        </Typography.Paragraph>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          请确认评分无误后再提交，提交后不可撤销。
        </Typography.Paragraph>
      </div>
      {errorMessage ? (
        <Alert style={{ marginTop: 16 }} message={errorMessage} type="error" showIcon />
      ) : null}
    </MobileLayout>
  );
}
