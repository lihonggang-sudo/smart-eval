import { apiGet, apiSend } from './common';

export type AdminMe = {
  id: number;
  username: string;
  displayName: string;
};

export type MobileMe = {
  id: number;
  username: string;
  departmentId: number;
  departmentName: string;
  submitted: boolean;
};

export type LatestPeriod = {
  id: number;
  name: string;
} | null;

export type LoginResult<TUser> = {
  token: string;
  user: TUser;
  latestPeriod: LatestPeriod;
};

export function adminLogin(username: string, password: string) {
  return apiSend<LoginResult<AdminMe>>('/api/admin/auth/login', 'POST', { username, password });
}

export function mobileLogin(username: string, password: string) {
  return apiSend<LoginResult<MobileMe>>('/api/mobile/auth/login', 'POST', { username, password });
}

export function getAdminMe() {
  return apiGet<AdminMe>('/api/admin/me');
}

export function getMobileMe() {
  return apiGet<MobileMe>('/api/mobile/me');
}
