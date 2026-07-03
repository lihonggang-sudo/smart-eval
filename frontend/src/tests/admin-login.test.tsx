import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminLogin from '../pages/admin/Login';
import { adminLogin } from '../services/auth';
import { saveToken } from '../services/session';

const navigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigate
  };
});

vi.mock('../services/auth', () => ({
  adminLogin: vi.fn()
}));

vi.mock('../services/session', () => ({
  saveToken: vi.fn()
}));

function renderLogin() {
  render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AdminLogin />
    </MemoryRouter>
  );
}

describe('AdminLogin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminLogin).mockResolvedValue({
      token: 'admin-token',
      user: {
        id: 1,
        username: 'admin',
        displayName: '系统管理员'
      }
    });
  });

  it('saves token and navigates to admin dashboard after login success', async () => {
    renderLogin();

    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'secret' } });
    fireEvent.click(screen.getByRole('button', { name: /登\s*录/ }));

    await waitFor(() => {
      expect(adminLogin).toHaveBeenCalledWith('admin', 'secret');
      expect(saveToken).toHaveBeenCalledWith('admin-token');
      expect(navigate).toHaveBeenCalledWith('/admin');
    });
  });

  it('shows API error message on login failure', async () => {
    vi.mocked(adminLogin).mockRejectedValueOnce(new Error('用户名或密码错误'));

    renderLogin();

    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'bad' } });
    fireEvent.click(screen.getByRole('button', { name: /登\s*录/ }));

    expect(await screen.findByText('用户名或密码错误')).toBeInTheDocument();
    expect(saveToken).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });
});
