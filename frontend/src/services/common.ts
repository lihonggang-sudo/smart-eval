import { request } from '../request';
import { authHeaders } from './session';

export type QueryValue = string | number | boolean | null | undefined;

export type PageResponse<T> = {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
};

export function buildQuery(params?: Record<string, QueryValue>): string {
  if (!params) {
    return '';
  }

  const searchParams = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') {
      continue;
    }
    searchParams.set(key, String(value));
  }

  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

export function apiGet<T>(url: string): Promise<T> {
  return request<T>(url, {
    headers: authHeaders()
  });
}

export function apiSend<T>(url: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown): Promise<T> {
  return request<T>(url, {
    method,
    headers: authHeaders(),
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}
