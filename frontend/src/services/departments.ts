import { apiGet, apiSend, buildQuery, type PageResponse } from './common';

export type Department = {
  id: number;
  name: string;
  sortOrder: number;
  createdAt: string;
};

export type DepartmentPayload = {
  name: string;
  sortOrder: number;
};

export type DepartmentListParams = {
  page?: number;
  pageSize?: number;
  name?: string;
};

export function listDepartments(params: DepartmentListParams = {}) {
  return apiGet<PageResponse<Department>>(`/api/admin/departments${buildQuery(params)}`);
}

export function createDepartment(payload: DepartmentPayload) {
  return apiSend<Department>('/api/admin/departments', 'POST', payload);
}

export function updateDepartment(id: number, payload: DepartmentPayload) {
  return apiSend<Department>(`/api/admin/departments/${id}`, 'PUT', payload);
}

export function deleteDepartment(id: number) {
  return apiSend<void>(`/api/admin/departments/${id}`, 'DELETE');
}
