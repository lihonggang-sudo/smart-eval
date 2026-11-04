import { request } from '@/request';

export interface EvaluationPeriod {
  id: number;
  name: string;
  createdAt: string;
}

export interface CreateEvaluationPeriodRequest {
  name: string;
}

export async function getEvaluationPeriods(): Promise<EvaluationPeriod[]> {
  return request('/api/admin/evaluation-periods', {
    method: 'GET',
  });
}

export async function createEvaluationPeriod(
  data: CreateEvaluationPeriodRequest,
): Promise<EvaluationPeriod> {
  return request('/api/admin/evaluation-periods', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function deleteEvaluationPeriod(id: number): Promise<void> {
  return request(`/api/admin/evaluation-periods/${id}`, {
    method: 'DELETE',
  });
}
