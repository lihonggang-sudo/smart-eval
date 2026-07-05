import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AppRoutes } from '../app';
import { ADMIN_COPYRIGHT_TEXT } from '../components/AdminCopyright';
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

  it('renders the formal mobile login page without the admin chrome', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/mobile']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: '鲁东大学' })).toBeInTheDocument();
    expect(screen.getByText('领导干部试用期满民主测评系统')).toBeInTheDocument();
    expect(screen.getByText('测评登录')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '进入测评' })).toBeInTheDocument();
    expect(screen.getByText('请使用分配的测评账号登录，测评结果将按规定保密。')).toBeInTheDocument();
    expect(screen.queryByTestId('admin-layout')).not.toBeInTheDocument();
  });

  it('uses responsive containers on the formal mobile login page', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/mobile']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByTestId('mobile-login-shell')).toHaveStyle({
      boxSizing: 'border-box',
      width: '100%'
    });
    expect(screen.getByTestId('mobile-login-card-wrap')).toHaveStyle({
      boxSizing: 'border-box',
      maxWidth: 'min(100%, 390px)'
    });
    expect(screen.getByTestId('mobile-login-title-text')).toHaveStyle({
      minWidth: '0',
      flex: '1 1 0%'
    });
  });

  it('pins the mobile login action area to the bottom of the card', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/mobile']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByTestId('mobile-login-card')).toHaveStyle({
      display: 'flex',
      flexDirection: 'column',
      minHeight: 'calc(100vh - 48px)'
    });
    expect(screen.getByTestId('mobile-login-card-body')).toHaveStyle({
      display: 'flex',
      flex: '1 1 auto',
      flexDirection: 'column'
    });
    expect(screen.getByTestId('mobile-login-actions')).toHaveStyle({
      marginTop: 'auto'
    });
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
    expect(screen.getByTestId('admin-copyright')).toHaveTextContent(ADMIN_COPYRIGHT_TEXT);
  });

  it('renders the formal admin brand header in the side navigation', () => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={['/admin']}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(screen.getByTestId('admin-brand-header')).toHaveStyle({
      display: 'flex',
      minHeight: '64px'
    });
    expect(screen.getByTestId('admin-brand-mark')).toHaveTextContent('评');
    expect(screen.getByText('鲁东大学')).toBeInTheDocument();
    expect(screen.getByText('民主测评系统')).toBeInTheDocument();
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
