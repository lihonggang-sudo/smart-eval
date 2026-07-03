import { authHeaders } from './session';
import { buildQuery, apiGet, apiSend, type PageResponse } from './common';

export type EvaluatorUser = {
  id: number;
  username: string;
  passwordPlain: string;
  departmentId: number;
  departmentName: string;
  enabled: boolean;
  submitted: boolean;
  submittedAt: string | null;
  createdAt: string;
};

export type EvaluatorUserListParams = {
  page?: number;
  pageSize?: number;
  departmentId?: number;
  submitted?: boolean;
  enabled?: boolean;
};

export type BatchGenerateEvaluatorUsersPayload = {
  departmentId: number;
  count: number;
};

export type BatchGenerateEvaluatorUsersResult = {
  created: number;
  items: EvaluatorUser[];
};

export type ResetEvaluatorPasswordResult = {
  passwordPlain: string;
};

export function listEvaluatorUsers(params: EvaluatorUserListParams = {}) {
  return apiGet<PageResponse<EvaluatorUser>>(`/api/admin/evaluator-users${buildQuery(params)}`);
}

export function batchGenerateEvaluatorUsers(payload: BatchGenerateEvaluatorUsersPayload) {
  return apiSend<BatchGenerateEvaluatorUsersResult>(
    '/api/admin/evaluator-users/batch-generate',
    'POST',
    payload
  );
}

export function resetEvaluatorPassword(id: number) {
  return apiSend<ResetEvaluatorPasswordResult>(`/api/admin/evaluator-users/${id}/reset-password`, 'POST');
}

export function setEvaluatorUserEnabled(id: number, enabled: boolean) {
  return apiSend<EvaluatorUser>(`/api/admin/evaluator-users/${id}/enabled`, 'PUT', { enabled });
}

export function printEvaluatorUsersUrl(params: Omit<EvaluatorUserListParams, 'page' | 'pageSize'> = {}) {
  return `/api/admin/evaluator-users/print${buildQuery(params)}`;
}

export async function fetchEvaluatorUsersPrintHtml(
  params: Omit<EvaluatorUserListParams, 'page' | 'pageSize'> = {}
) {
  const response = await fetch(printEvaluatorUsersUrl(params), {
    headers: authHeaders()
  });

  if (!response.ok) {
    throw new Error('打印页面加载失败');
  }

  return response.text();
}
