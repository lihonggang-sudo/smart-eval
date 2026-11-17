export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message: string;
  errors: Record<string, string>;
};

export async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> ?? {}),
  };

  // Add authorization token
  const token = localStorage.getItem('smart_eval_token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Add evaluation period header for admin requests
  if (url.startsWith('/api/admin/') && !url.includes('/auth/login')) {
    try {
      const stored = localStorage.getItem('evaluation-period-storage');
      if (stored) {
        const data = JSON.parse(stored);
        if (data.state?.currentPeriod?.id) {
          headers['X-Evaluation-Period-Id'] = String(data.state.currentPeriod.id);
        }
      }
    } catch (e) {
      // Ignore parse errors
    }
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  // Handle 204 No Content and other empty responses
  const text = await response.text();
  if (!text) {
    if (!response.ok) {
      throw new Error('请求失败');
    }
    return undefined as T;
  }

  let body: ApiResponse<T>;
  try {
    body = JSON.parse(text) as ApiResponse<T>;
  } catch {
    throw new Error(response.ok ? '响应格式错误' : '请求失败');
  }

  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}
