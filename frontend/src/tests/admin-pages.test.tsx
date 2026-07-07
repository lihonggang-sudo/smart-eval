import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { message } from 'antd';
import { AppRoutes } from '../app';
import AdminUsers from '../pages/admin/AdminUsers';
import Departments from '../pages/admin/Departments';
import Evaluatees from '../pages/admin/Evaluatees';
import EvaluatorUsers from '../pages/admin/EvaluatorUsers';
import { deleteDepartment, listDepartments } from '../services/departments';
import { fetchEvaluatorUsersPrintHtml } from '../services/evaluatorUsers';
import { routes } from '../routes';

const originalGetComputedStyle = window.getComputedStyle;
const originalConsoleError = console.error;

Object.defineProperty(window, 'getComputedStyle', {
  configurable: true,
  value: () => ({
    getPropertyValue: () => '',
    overflow: 'auto'
  })
});

vi.mock('../services/adminUsers', () => ({
  listAdminUsers: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
  createAdminUser: vi.fn(),
  updateAdminUser: vi.fn(),
  setAdminUserEnabled: vi.fn(),
  resetAdminUserPassword: vi.fn()
}));

vi.mock('../services/departments', () => ({
  listDepartments: vi.fn().mockResolvedValue({
    items: [{ id: 1, name: '研发部', sortOrder: 1, createdAt: '2026-07-01T00:00:00Z' }],
    total: 1,
    page: 1,
    pageSize: 20
  }),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}));

vi.mock('../services/evaluatees', () => ({
  listEvaluatees: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
  createEvaluatee: vi.fn(),
  updateEvaluatee: vi.fn(),
  setEvaluateeEnabled: vi.fn()
}));

vi.mock('../services/evaluatorUsers', () => ({
  listEvaluatorUsers: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
  batchGenerateEvaluatorUsers: vi.fn(),
  resetEvaluatorPassword: vi.fn().mockResolvedValue({ passwordPlain: 'pw123456' }),
  setEvaluatorUserEnabled: vi.fn(),
  printEvaluatorUsersUrl: vi.fn().mockReturnValue('/api/admin/evaluator-users/print'),
  fetchEvaluatorUsersPrintHtml: vi.fn().mockResolvedValue('<html></html>')
}));

describe('admin management pages', () => {
  beforeAll(() => {
    vi.spyOn(console, 'error').mockImplementation((message?: unknown, ...args: unknown[]) => {
      const text = String(message);
      if (text.includes('findDOMNode is deprecated')) {
        return;
      }
      originalConsoleError(message, ...args);
    });
  });

  afterAll(() => {
    console.error = originalConsoleError;
    Object.defineProperty(window, 'getComputedStyle', {
      configurable: true,
      value: originalGetComputedStyle
    });
  });

  it('renders create action', async () => {
    render(<Departments />);
    expect(await screen.findByText('新增部门')).toBeInTheDocument();
  });

  it('shows backend message when deleting a department fails', async () => {
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => {
      return undefined as never;
    });

    vi.mocked(deleteDepartment).mockRejectedValueOnce(
      new Error('部门下存在关联数据，不能删除')
    );

    render(<Departments />);

    fireEvent.click(await screen.findByText('删除'));
    fireEvent.click(await screen.findByRole('button', { name: /确 定|确定|OK/i }));

    await waitFor(() => {
      expect(deleteDepartment).toHaveBeenCalledWith(1);
      expect(errorSpy).toHaveBeenCalledWith('部门下存在关联数据，不能删除');
    });

    errorSpy.mockRestore();
  });

  it('renders evaluatee management action', async () => {
    render(<Evaluatees />);
    expect(await screen.findByText('新增被测评人员')).toBeInTheDocument();
  });

  it('renders evaluator user management actions', async () => {
    render(<EvaluatorUsers />);
    expect(await screen.findByText('批量生成账号')).toBeInTheDocument();
    expect(await screen.findByText('打印账号')).toBeInTheDocument();
  });

  it('fetches authenticated print HTML before opening the print page', async () => {
    const originalCreateObjectUrl = URL.createObjectURL;
    const createObjectUrlSpy = vi.fn().mockReturnValue('blob:print');
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: createObjectUrlSpy
    });
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    render(<EvaluatorUsers />);

    const print = await screen.findByText('打印账号');
    print.click();

    await waitFor(() => {
      expect(fetchEvaluatorUsersPrintHtml).toHaveBeenCalledWith({});
      expect(createObjectUrlSpy).toHaveBeenCalledWith(expect.any(Blob));
      expect(openSpy).toHaveBeenCalledWith('blob:print', '_blank', 'noopener,noreferrer');
    });

    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: originalCreateObjectUrl
    });
    openSpy.mockRestore();
  });

  it('renders admin user management action', async () => {
    render(<AdminUsers />);
    expect(await screen.findByText('新增管理员')).toBeInTheDocument();
  });

  it('declares the exact admin route manifest entries from the brief', () => {
    expect(
      routes.filter((route) =>
        [
          '/admin/departments',
          '/admin/evaluatees',
          '/admin/evaluator-users',
          '/admin/admin-users'
        ].includes(route.path ?? '')
      )
    ).toEqual([
      {
        path: '/admin/departments',
        component: './pages/admin/Departments',
        layout: 'admin',
        name: '部门管理'
      },
      {
        path: '/admin/evaluatees',
        component: './pages/admin/Evaluatees',
        layout: 'admin',
        name: '被测评人员'
      },
      {
        path: '/admin/evaluator-users',
        component: './pages/admin/EvaluatorUsers',
        layout: 'admin',
        name: '测评用户'
      },
      {
        path: '/admin/admin-users',
        component: './pages/admin/AdminUsers',
        layout: 'admin',
        name: '管理员账号'
      }
    ]);
  });

  it.each([
    ['/admin/departments', '新增部门'],
    ['/admin/evaluatees', '新增被测评人员'],
    ['/admin/evaluator-users', '批量生成账号'],
    ['/admin/evaluator-users', '打印账号'],
    ['/admin/admin-users', '新增管理员']
  ])('maps %s to the correct admin page action %s', async (path, actionText) => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={[path]}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(await screen.findByText(actionText)).toBeInTheDocument();
    expect(screen.getByTestId('admin-layout')).toBeInTheDocument();
  });

  it.each([
    ['被测评人员', <Evaluatees />],
    ['测评用户', <EvaluatorUsers />]
  ])('shows a visible error when %s department options fail to load', async (_, page) => {
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => {
      return undefined as never;
    });

    vi.mocked(listDepartments).mockRejectedValueOnce(new Error('network error'));

    render(page);

    await screen.findByText(/新增|批量生成账号/);

    expect(errorSpy).toHaveBeenCalledTimes(1);
    errorSpy.mockRestore();
  });
});
