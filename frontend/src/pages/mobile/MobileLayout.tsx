/* eslint-disable react-refresh/only-export-components */
import type { ReactNode } from 'react';
import { Layout, Typography } from 'antd';
import type { MobileEvaluateeItem, MobileGradeItem } from '../../services/mobileEvaluation';
import type { EvaluationGrade } from '../../services/results';

const DRAFT_KEY = 'smart_eval_mobile_draft';
const VALID_EVALUATION_GRADES: EvaluationGrade[] = [
  'EXCELLENT',
  'COMPETENT',
  'BASIC_COMPETENT',
  'INCOMPETENT'
];

export type MobileEvaluationDraft = {
  departmentId: number;
  departmentName: string;
  items: MobileEvaluateeItem[];
  grades: MobileGradeItem[];
  selections: Record<string, EvaluationGrade>;
  comments: Record<string, MobileEvaluationComments>;
};

export type MobileEvaluationComments = {
  comment1: string;
  comment2: string;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isValidMobileEvaluateeItem(value: unknown): value is MobileEvaluateeItem {
  return (
    isRecord(value) &&
    typeof value.evaluateeId === 'number' &&
    Number.isFinite(value.evaluateeId) &&
    typeof value.name === 'string'
  );
}

function isValidMobileGradeItem(value: unknown): value is MobileGradeItem {
  return (
    isRecord(value) &&
    isEvaluationGrade(value.value) &&
    typeof value.label === 'string'
  );
}

function isValidSelections(value: unknown): value is Record<string, EvaluationGrade> {
  return (
    isRecord(value) &&
    Object.values(value).every((grade) => isEvaluationGrade(grade))
  );
}

function isValidComments(value: unknown): value is MobileEvaluationComments {
  return (
    isRecord(value) &&
    typeof value.comment1 === 'string' &&
    typeof value.comment2 === 'string'
  );
}

export function isEvaluationGrade(value: unknown): value is EvaluationGrade {
  return (
    typeof value === 'string' &&
    (VALID_EVALUATION_GRADES as string[]).includes(value)
  );
}

export function getValidMobileGrades(grades: MobileGradeItem[] | unknown) {
  if (!Array.isArray(grades)) {
    return [];
  }

  return grades.filter((grade) => isEvaluationGrade(grade.value));
}

export function sanitizeMobileEvaluationSelections(
  selections: Record<string, unknown> | unknown,
  itemIds: number[],
  allowedGrades: MobileGradeItem[]
): Record<string, EvaluationGrade> {
  if (!isRecord(selections)) {
    return {};
  }

  const allowedGradeValues = new Set(allowedGrades.map((grade) => grade.value));

  return itemIds.reduce<Record<string, EvaluationGrade>>((result, itemId) => {
    const key = String(itemId);
    const value = selections[key];

    if (isEvaluationGrade(value) && allowedGradeValues.has(value)) {
      result[key] = value;
    }

    return result;
  }, {});
}

export function sanitizeMobileEvaluationComments(
  comments: Record<string, unknown> | unknown,
  itemIds: number[]
): Record<string, MobileEvaluationComments> {
  if (!isRecord(comments)) {
    return {};
  }

  return itemIds.reduce<Record<string, MobileEvaluationComments>>((result, itemId) => {
    const key = String(itemId);
    const value = comments[key];

    if (isValidComments(value)) {
      result[key] = {
        comment1: value.comment1,
        comment2: value.comment2
      };
    }

    return result;
  }, {});
}

type MobileLayoutProps = {
  title?: string;
  subtitle?: string;
  footer?: ReactNode;
  children: ReactNode;
};

export function readMobileEvaluationDraft(): MobileEvaluationDraft | null {
  const raw = sessionStorage.getItem(DRAFT_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw);

    if (
      !isRecord(parsed) ||
      typeof parsed.departmentId !== 'number' ||
      !Number.isFinite(parsed.departmentId) ||
      typeof parsed.departmentName !== 'string' ||
      !Array.isArray(parsed.items) ||
      !parsed.items.every((item) => isValidMobileEvaluateeItem(item)) ||
      !Array.isArray(parsed.grades) ||
      !parsed.grades.every((grade) => isValidMobileGradeItem(grade)) ||
      !isValidSelections(parsed.selections)
    ) {
      sessionStorage.removeItem(DRAFT_KEY);
      return null;
    }

    return {
      departmentId: parsed.departmentId,
      departmentName: parsed.departmentName,
      items: parsed.items,
      grades: parsed.grades,
      selections: parsed.selections,
      comments: sanitizeMobileEvaluationComments(
        isRecord(parsed.comments) ? parsed.comments : {},
        parsed.items.map((item) => item.evaluateeId)
      )
    };
  } catch {
    sessionStorage.removeItem(DRAFT_KEY);
    return null;
  }
}

export function saveMobileEvaluationDraft(draft: MobileEvaluationDraft) {
  sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
}

export function clearMobileEvaluationDraft() {
  sessionStorage.removeItem(DRAFT_KEY);
}

export default function MobileLayout({ title, subtitle, footer, children }: MobileLayoutProps) {
  return (
    <Layout
      style={{
        minHeight: '100vh',
        background: '#f5f5f5'
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 480,
          margin: '0 auto',
          padding: '24px 16px 112px'
        }}
      >
        {title ? (
          <div style={{ marginBottom: 24 }}>
            <Typography.Title level={3} style={{ marginBottom: 8 }}>
              {title}
            </Typography.Title>
            {subtitle ? <Typography.Text type="secondary">{subtitle}</Typography.Text> : null}
          </div>
        ) : null}
        {children}
      </div>
      {footer ? (
        <div
          style={{
            position: 'fixed',
            left: 0,
            right: 0,
            bottom: 0,
            padding: '12px 16px calc(12px + env(safe-area-inset-bottom, 0px))',
            background: '#ffffff',
            borderTop: '1px solid #f0f0f0'
          }}
        >
          <div
            style={{
              maxWidth: 480,
              margin: '0 auto'
            }}
          >
            {footer}
          </div>
        </div>
      ) : null}
    </Layout>
  );
}
