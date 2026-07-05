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

  const gradedCount = useMemo(() => {
    if (!draft) {
      return 0;
    }

    return draft.items.filter((item) => Boolean(draft.selections[String(item.evaluateeId)])).length;
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
      subtitle="请逐项完成评分，提交前可返回确认。"
      footer={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
            data-testid="mobile-evaluation-footer-progress"
            style={{
              flex: '0 0 auto',
              color: '#5f6b7a',
              fontSize: 13,
              lineHeight: 1.4
            }}
          >
            已完成 {gradedCount}/{draft?.items.length ?? 0}
          </div>
          <Button
            block
            disabled={!allGraded}
            size="large"
            style={{
              height: 46,
              borderRadius: 6,
              background: allGraded ? '#8a1f2d' : undefined,
              borderColor: allGraded ? '#8a1f2d' : undefined,
              fontWeight: 600
            }}
            type="primary"
            onClick={handleSubmit}
          >
            提交测评
          </Button>
        </div>
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
          <section
            data-testid="mobile-evaluation-summary"
            style={{
              padding: 16,
              borderRadius: 8,
              background: '#ffffff',
              border: '1px solid #d8dee8',
              boxShadow: '0 8px 20px rgba(31, 45, 61, 0.06)'
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 12,
                marginBottom: 10
              }}
            >
              <Typography.Text strong style={{ color: '#1f2937', fontSize: 15 }}>
                部门：{draft.departmentName}
              </Typography.Text>
              <Typography.Text
                style={{
                  color: '#8a1f2d',
                  flex: '0 0 auto',
                  fontSize: 13,
                  fontWeight: 600
                }}
              >
                已评 {gradedCount} / {draft.items.length}
              </Typography.Text>
            </div>
            <Typography.Text style={{ color: '#6b7280', fontSize: 13, lineHeight: 1.7 }}>
              请逐项完成评分，提交前可返回确认。
            </Typography.Text>
          </section>

          {draft.items.map((item, index) => {
            const selectedGrade = draft.selections[String(item.evaluateeId)];
            const hasGrade = Boolean(selectedGrade);

            return (
              <section
                key={item.evaluateeId}
                data-testid={`mobile-evaluatee-card-${item.evaluateeId}`}
                style={{
                  overflow: 'hidden',
                  borderRadius: 8,
                  background: '#ffffff',
                  border: '1px solid #d8dee8',
                  boxShadow: '0 8px 20px rgba(31, 45, 61, 0.06)'
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '14px 16px',
                    background: '#f7f9fc',
                    borderBottom: '1px solid #eef1f5'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
                    <div
                      aria-hidden="true"
                      style={{
                        width: 34,
                        height: 34,
                        flex: '0 0 34px',
                        borderRadius: 6,
                        background: '#8a1f2d',
                        color: '#ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: 14,
                        fontWeight: 700
                      }}
                    >
                      {String(index + 1).padStart(2, '0')}
                    </div>
                    <Typography.Title
                      level={5}
                      style={{ color: '#1f2937', margin: 0, minWidth: 0 }}
                    >
                      {item.name}
                    </Typography.Title>
                  </div>
                  <Typography.Text
                    style={{
                      color: hasGrade ? '#8a1f2d' : '#8c8c8c',
                      flex: '0 0 auto',
                      fontSize: 13,
                      fontWeight: 600
                    }}
                  >
                    {hasGrade ? '已评分' : '未评分'}
                  </Typography.Text>
                </div>

                <div style={{ padding: 16 }}>
                  <div style={{ marginBottom: 16 }}>
                    <Typography.Text
                      strong
                      style={{
                        color: '#1f2937',
                        display: 'block',
                        fontSize: 14,
                        marginBottom: 10
                      }}
                    >
                      综合评价
                    </Typography.Text>
                    <Radio.Group
                      buttonStyle="solid"
                      optionType="button"
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                        gap: 8
                      }}
                      value={selectedGrade}
                      onChange={(event) => handleGradeChange(item.evaluateeId, event.target.value)}
                    >
                      {draft.grades.map((grade) => (
                        <Radio.Button
                          key={grade.value}
                          style={{
                            borderRadius: 6,
                            height: 40,
                            lineHeight: '38px',
                            textAlign: 'center'
                          }}
                          value={grade.value}
                        >
                          {grade.label}
                        </Radio.Button>
                      ))}
                    </Radio.Group>
                  </div>

                  <div
                    style={{
                      borderTop: '1px solid #eef1f5',
                      paddingTop: 14
                    }}
                  >
                    <Typography.Text
                      strong
                      style={{
                        color: '#1f2937',
                        display: 'block',
                        fontSize: 14,
                        marginBottom: 12
                      }}
                    >
                      文字意见
                    </Typography.Text>
                    <Space direction="vertical" size={12} style={{ display: 'flex' }}>
                      <div>
                        <label htmlFor={`comment1-${item.evaluateeId}`}>
                          <Typography.Text strong style={{ fontSize: 13 }}>
                            是否存在慵懒散、不担当、不作为等问题及表现
                          </Typography.Text>
                        </label>
                        <Input.TextArea
                          id={`comment1-${item.evaluateeId}`}
                          aria-label="是否存在慵懒散、不担当、不作为等问题及表现"
                          maxLength={200}
                          rows={3}
                          showCount
                          style={{ marginTop: 8 }}
                          value={draft.comments[String(item.evaluateeId)]?.comment1 ?? ''}
                          onChange={(event) =>
                            handleCommentChange(item.evaluateeId, 'comment1', event.target.value)
                          }
                        />
                      </div>
                      <div>
                        <label htmlFor={`comment2-${item.evaluateeId}`}>
                          <Typography.Text strong style={{ fontSize: 13 }}>
                            意见或建议
                          </Typography.Text>
                        </label>
                        <Input.TextArea
                          id={`comment2-${item.evaluateeId}`}
                          aria-label="意见或建议"
                          maxLength={200}
                          rows={3}
                          showCount
                          style={{ marginTop: 8 }}
                          value={draft.comments[String(item.evaluateeId)]?.comment2 ?? ''}
                          onChange={(event) =>
                            handleCommentChange(item.evaluateeId, 'comment2', event.target.value)
                          }
                        />
                      </div>
                    </Space>
                  </div>
                </div>
              </section>
            );
          })}
        </Space>
      ) : null}
    </MobileLayout>
  );
}
