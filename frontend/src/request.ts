export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message: string;
  errors: Record<string, string>;
};

export async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {})
    }
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}
