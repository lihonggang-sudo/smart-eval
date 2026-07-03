import { apiGet, apiSend, buildQuery, type PageResponse } from './common';

export type AdminUser = {
  id: number;
  username: string;
  displayName: string;
  enabled: boolean;
  createdAt: string;
};

export type AdminUserListParams = {
  page?: number;
  pageSize?: number;
};

export type CreateAdminUserPayload = {
  username: string;
  password: string;
  displayName: string;
};

export type UpdateAdminUserPayload = {
  displayName: string;
};

export function listAdminUsers(params: AdminUserListParams = {}) {
  return apiGet<PageResponse<AdminUser>>(`/api/admin/admin-users${buildQuery(params)}`);
}

export function createAdminUser(payload: CreateAdminUserPayload) {
  return apiSend<AdminUser>('/api/admin/admin-users', 'POST', payload);
}

export function updateAdminUser(id: number, payload: UpdateAdminUserPayload) {
  return apiSend<AdminUser>(`/api/admin/admin-users/${id}`, 'PUT', payload);
}

export function setAdminUserEnabled(id: number, enabled: boolean) {
  return apiSend<AdminUser>(`/api/admin/admin-users/${id}/enabled`, 'PUT', { enabled });
}

export function resetAdminUserPassword(id: number, password: string) {
  return apiSend<void>(`/api/admin/admin-users/${id}/reset-password`, 'POST', { password });
}
