import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchEvaluatorUsersPrintHtml } from '../services/evaluatorUsers';
import { authHeaders, clearToken, saveToken } from '../services/session';

describe('session', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    clearToken();
  });

  it('returns bearer header after saving token', () => {
    clearToken();
    saveToken('abc');
    expect(authHeaders()).toEqual({ Authorization: 'Bearer abc' });
  });

  it('fetches print HTML with the bearer token', async () => {
    clearToken();
    saveToken('abc');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      text: () => Promise.resolve('<html></html>')
    } as Response);

    await expect(fetchEvaluatorUsersPrintHtml({ departmentId: 1 })).resolves.toBe('<html></html>');

    expect(fetchSpy).toHaveBeenCalledWith('/api/admin/evaluator-users/print?departmentId=1', {
      headers: { Authorization: 'Bearer abc' }
    });
  });
});
