import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AppRoutes } from '../app';
import { routes } from '../routes';
import { clearToken } from '../services/session';

vi.mock('../services/session', () => ({
  authHeaders: vi.fn(() => ({})),
  clearToken: vi.fn()
}));

describe('routes', () => {
  it('keeps mobile routes outside the admin layout', () => {
    const mobile = routes.find((route) => route.path === '/mobile');
    expect(mobile?.layout).toBe(false);
    expect(mobile?.redirect).toBe('/mobile/login');

    const mobileLogin = routes.find((route) => route.path === '/mobile/login');
    expect(mobileLogin?.layout).toBe(false);
    expect(mobileLogin?.component).toBe('./pages/mobile/Login');
  });

  it('renders mobile routes without the admin chrome', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/mobile']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: '评测登录' })).toBeInTheDocument();
    expect(screen.queryByTestId('admin-layout')).not.toBeInTheDocument();
  });

  it('renders admin routes inside the admin layout', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/admin']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: '管理员工作台' })).toBeInTheDocument();
    expect(screen.getByTestId('admin-layout')).toBeInTheDocument();
  });

  it('renders admin login outside the admin layout', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/admin/login']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(screen.queryByTestId('admin-layout')).not.toBeInTheDocument();
  });

  it('clears the admin token and returns to login when logging out', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/admin']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: '退出系统' }));

    expect(clearToken).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(screen.queryByTestId('admin-layout')).not.toBeInTheDocument();
  });
});
