import { apiGet, apiSend } from './common';
import type { EvaluationGrade } from './results';

export type MobileEvaluateeItem = {
  evaluateeId: number;
  name: string;
};

export type MobileGradeItem = {
  value: string;
  label: string;
};

export type EvaluationItemsResult = {
  departmentId: number;
  departmentName: string;
  submitted: boolean;
  items: MobileEvaluateeItem[];
  grades: MobileGradeItem[];
};

export type SubmitEvaluationRecord = {
  evaluateeId: number;
  grade: EvaluationGrade;
  comment1?: string;
  comment2?: string;
};

export type SubmitEvaluationResult = {
  submitted: boolean;
  submittedAt: string;
};

export function getEvaluationItems() {
  return apiGet<EvaluationItemsResult>('/api/mobile/evaluation/items');
}

export function submitEvaluation(records: SubmitEvaluationRecord[]) {
  return apiSend<SubmitEvaluationResult>('/api/mobile/evaluation/submit', 'POST', { records });
}
