import { apiGet, buildQuery, type PageResponse } from './common';

export type EvaluationGrade = 'EXCELLENT' | 'COMPETENT' | 'BASIC_COMPETENT' | 'INCOMPETENT';

export type ResultSummary = {
  evaluateeId: number;
  evaluateeName: string;
  departmentName: string;
  excellentCount: number;
  competentCount: number;
  basicCompetentCount: number;
  incompetentCount: number;
  totalCount: number;
};

export type ResultDetail = {
  evaluatorUserId: number;
  evaluatorUsername: string;
  evaluatorDepartmentName: string;
  evaluateeId: number;
  evaluateeName: string;
  grade: string;
  gradeLabel: string;
  comment1: string | null;
  comment2: string | null;
  createdAt: string;
};

export type ResultSummaryParams = {
  page?: number;
  pageSize?: number;
  departmentId?: number;
  evaluateeName?: string;
};

export type ResultDetailsParams = {
  page?: number;
  pageSize?: number;
  departmentId?: number;
  evaluateeId?: number;
  evaluatorUserId?: number;
  grade?: EvaluationGrade;
};

export function getResultSummary(params: ResultSummaryParams = {}) {
  return apiGet<PageResponse<ResultSummary>>(`/api/admin/results/summary${buildQuery(params)}`);
}

export function getResultDetails(params: ResultDetailsParams = {}) {
  return apiGet<PageResponse<ResultDetail>>(`/api/admin/results/details${buildQuery(params)}`);
}
