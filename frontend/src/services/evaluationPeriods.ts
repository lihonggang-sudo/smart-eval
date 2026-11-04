import request from '@/utils/request';

export interface EvaluationPeriod {
  id: number;
  name: string;
  createdAt: string;
}

export interface CreateEvaluationPeriodRequest {
  name: string;
}

export async function getEvaluationPeriods(): Promise<API.Response<EvaluationPeriod[]>> {
  return request('/api/admin/evaluation-periods', {
    method: 'GET',
  });
}

export async function createEvaluationPeriod(
  data: CreateEvaluationPeriodRequest,
): Promise<API.Response<EvaluationPeriod>> {
  return request('/api/admin/evaluation-periods', {
    method: 'POST',
    data,
  });
}

export async function deleteEvaluationPeriod(id: number): Promise<void> {
  return request(`/api/admin/evaluation-periods/${id}`, {
    method: 'DELETE',
  });
}
