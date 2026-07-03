import { apiGet, apiSend, buildQuery, type PageResponse } from './common';

export type Evaluatee = {
  id: number;
  name: string;
  departmentId: number;
  departmentName: string;
  enabled: boolean;
  createdAt: string;
};

export type EvaluateePayload = {
  name: string;
  departmentId: number;
  enabled: boolean;
};

export type EvaluateeListParams = {
  page?: number;
  pageSize?: number;
  departmentId?: number;
  name?: string;
  enabled?: boolean;
};

export function listEvaluatees(params: EvaluateeListParams = {}) {
  return apiGet<PageResponse<Evaluatee>>(`/api/admin/evaluatees${buildQuery(params)}`);
}

export function createEvaluatee(payload: EvaluateePayload) {
  return apiSend<Evaluatee>('/api/admin/evaluatees', 'POST', payload);
}

export function updateEvaluatee(id: number, payload: EvaluateePayload) {
  return apiSend<Evaluatee>(`/api/admin/evaluatees/${id}`, 'PUT', payload);
}

export function deleteEvaluatee(id: number) {
  return apiSend<void>(`/api/admin/evaluatees/${id}`, 'DELETE');
}

export function setEvaluateeEnabled(id: number, enabled: boolean) {
  return apiSend<Evaluatee>(`/api/admin/evaluatees/${id}/enabled`, 'PUT', { enabled });
}
