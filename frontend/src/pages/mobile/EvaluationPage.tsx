import { Alert, Button, Input, Radio, Space, Spin, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEvaluationItems } from '../../services/mobileEvaluation';
import type { EvaluationGrade } from '../../services/results';
import MobileLayout, {
  clearMobileEvaluationDraft,
  getValidMobileGrades,
  readMobileEvaluationDraft,
  saveMobileEvaluationDraft,
  sanitizeMobileEvaluationComments,
  sanitizeMobileEvaluationSelections,
  type MobileEvaluationDraft
} from './MobileLayout';

export default function EvaluationPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [draft, setDraft] = useState<MobileEvaluationDraft | null>(null);

  useEffect(() => {
    let active = true;

    async function loadEvaluation() {
      setLoading(true);
      setErrorMessage('');

      try {
        const result = await getEvaluationItems();
        if (!active) {
          return;
        }

        if (result.submitted) {
          clearMobileEvaluationDraft();
          navigate('/mobile/complete', { replace: true });
          return;
        }

        const validGrades = getValidMobileGrades(result.grades);
        if (validGrades.length === 0) {
          setErrorMessage('暂无可用评分项');
          setDraft(null);
          clearMobileEvaluationDraft();
          return;
        }

        const savedDraft = readMobileEvaluationDraft();
        const itemIds = result.items.map((item) => item.evaluateeId);
        const nextDraft: MobileEvaluationDraft = {
          departmentId: result.departmentId,
          departmentName: result.departmentName,
          items: result.items,
          grades: validGrades,
          selections:
            savedDraft?.departmentId === result.departmentId
              ? sanitizeMobileEvaluationSelections(savedDraft.selections, itemIds, validGrades)
              : {},
          comments:
            savedDraft?.departmentId === result.departmentId
              ? sanitizeMobileEvaluationComments(savedDraft.comments, itemIds)
              : {}
        };

        saveMobileEvaluationDraft(nextDraft);
        setDraft(nextDraft);
      } catch (error) {
        if (active) {
          setErrorMessage(error instanceof Error ? error.message : '加载失败');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadEvaluation();

    return () => {
      active = false;
    };
  }, [navigate]);

  const allGraded = useMemo(() => {
    if (!draft) {
      return false;
    }

    return draft.items.every((item) => Boolean(draft.selections[String(item.evaluateeId)]));
  }, [draft]);

  function handleGradeChange(evaluateeId: number, grade: string) {
    setDraft((currentDraft) => {
      if (!currentDraft) {
        return currentDraft;
      }

      const matchedGrade = currentDraft.grades.find((item) => item.value === grade);
      if (!matchedGrade) {
        return currentDraft;
      }

      const nextDraft = {
        ...currentDraft,
        selections: {
          ...currentDraft.selections,
          [String(evaluateeId)]: matchedGrade.value as EvaluationGrade
        }
      };

      saveMobileEvaluationDraft(nextDraft);
      return nextDraft;
    });
  }

  function handleCommentChange(evaluateeId: number, field: 'comment1' | 'comment2', value: string) {
    setDraft((currentDraft) => {
      if (!currentDraft) {
        return currentDraft;
      }

      const key = String(evaluateeId);
      const currentComments = currentDraft.comments[key] ?? { comment1: '', comment2: '' };
      const nextDraft = {
        ...currentDraft,
        comments: {
          ...currentDraft.comments,
          [key]: {
            ...currentComments,
            [field]: value
          }
        }
      };

      saveMobileEvaluationDraft(nextDraft);
      return nextDraft;
    });
  }

  function handleSubmit() {
    if (!allGraded || !draft) {
      return;
    }

    saveMobileEvaluationDraft(draft);
    navigate('/mobile/confirm');
  }

  return (
    <MobileLayout
      title={draft?.departmentName ?? '测评列表'}
      subtitle="请完成所有人员评分后再提交。"
      footer={
        <Button block disabled={!allGraded} size="large" type="primary" onClick={handleSubmit}>
          提交
        </Button>
      }
    >
      {loading ? (
        <div style={{ paddingTop: 48, textAlign: 'center' }}>
          <Spin />
        </div>
      ) : null}

      {!loading && errorMessage ? <Alert message={errorMessage} type="error" showIcon /> : null}

      {!loading && draft ? (
        <Space direction="vertical" size={16} style={{ display: 'flex' }}>
          {draft.items.map((item) => (
            <section
              key={item.evaluateeId}
              style={{
                padding: 16,
                borderRadius: 8,
                background: '#ffffff',
                border: '1px solid #f0f0f0'
              }}
            >
              <Typography.Title level={5} style={{ marginBottom: 12 }}>
                {item.name}
              </Typography.Title>
              <Radio.Group
                buttonStyle="solid"
                optionType="button"
                style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 8 }}
                value={draft.selections[String(item.evaluateeId)]}
                onChange={(event) => handleGradeChange(item.evaluateeId, event.target.value)}
              >
                {draft.grades.map((grade) => (
                  <Radio.Button
                    key={grade.value}
                    style={{ textAlign: 'center', borderRadius: 8 }}
                    value={grade.value}
                  >
                    {grade.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
              <Space direction="vertical" size={12} style={{ display: 'flex', marginTop: 16 }}>
                <div>
                  <label htmlFor={`comment1-${item.evaluateeId}`}>
                    <Typography.Text strong>
                      是否存在慵懒散、不担当、不作为等问题及表现
                    </Typography.Text>
                  </label>
                  <Input.TextArea
                    id={`comment1-${item.evaluateeId}`}
                    aria-label="是否存在慵懒散、不担当、不作为等问题及表现"
                    maxLength={255}
                    rows={3}
                    showCount
                    style={{ marginTop: 8 }}
                    value={draft.comments[String(item.evaluateeId)]?.comment1 ?? ''}
                    onChange={(event) => handleCommentChange(item.evaluateeId, 'comment1', event.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor={`comment2-${item.evaluateeId}`}>
                    <Typography.Text strong>意见或建议</Typography.Text>
                  </label>
                  <Input.TextArea
                    id={`comment2-${item.evaluateeId}`}
                    aria-label="意见或建议"
                    maxLength={255}
                    rows={3}
                    showCount
                    style={{ marginTop: 8 }}
                    value={draft.comments[String(item.evaluateeId)]?.comment2 ?? ''}
                    onChange={(event) => handleCommentChange(item.evaluateeId, 'comment2', event.target.value)}
                  />
                </div>
              </Space>
            </section>
          ))}
        </Space>
      ) : null}
    </MobileLayout>
  );
}
