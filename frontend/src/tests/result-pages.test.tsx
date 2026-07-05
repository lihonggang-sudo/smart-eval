import React, { type ReactNode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from '../app';
import AdminDashboard from '../pages/admin/Dashboard';
import ResultDetails from '../pages/admin/ResultDetails';
import ResultSummary from '../pages/admin/ResultSummary';
import { routes } from '../routes';
import { listDepartments } from '../services/departments';
import { listEvaluatees } from '../services/evaluatees';
import { listEvaluatorUsers } from '../services/evaluatorUsers';
import { getResultDetails, getResultSummary } from '../services/results';

type TableRequestParams = {
  current?: number;
  pageSize?: number;
  [key: string]: unknown;
};

type TableColumn = {
  title?: ReactNode;
  hideInTable?: boolean;
};

type ProTableProps = {
  columns?: TableColumn[];
  headerTitle?: ReactNode;
  request?: (params: TableRequestParams) => Promise<unknown> | unknown;
};

const proTableHarness = vi.hoisted(() => ({
  requestParams: [{ current: 1, pageSize: 20 }] as TableRequestParams[]
}));

const originalGetComputedStyle = window.getComputedStyle;
const originalConsoleError = console.error;

Object.defineProperty(window, 'getComputedStyle', {
  configurable: true,
  value: () => ({
    getPropertyValue: () => '',
    overflow: 'auto'
  })
});

vi.mock('@ant-design/pro-components', async () => {
  const ReactModule = await vi.importActual<typeof import('react')>('react');

  return {
    PageContainer: ({ children }: { children: ReactNode }) =>
      ReactModule.createElement('main', null, children),
    ProLayout: ({ children }: { children: ReactNode }) =>
      ReactModule.createElement('section', null, children),
    ProTable: ({ columns = [], headerTitle, request }: ProTableProps) => {
      const requested = ReactModule.useRef(false);

      ReactModule.useEffect(() => {
        if (requested.current) {
          return;
        }
        requested.current = true;
        proTableHarness.requestParams.forEach((params) => {
          void request?.(params);
        });
      }, [request]);

      return ReactModule.createElement(
        'section',
        null,
        headerTitle ? ReactModule.createElement('h1', null, headerTitle) : null,
        ReactModule.createElement(
          'div',
          null,
          columns
            .filter((column) => !column.hideInTable)
            .map((column, index) =>
              ReactModule.createElement('span', { key: index }, column.title)
            )
        )
      );
    }
  };
});

vi.mock('../services/departments', () => ({
  listDepartments: vi.fn().mockResolvedValue({
    items: [{ id: 1, name: '研发部', sortOrder: 1, createdAt: '2026-07-01T00:00:00Z' }],
    total: 7,
    page: 1,
    pageSize: 200
  })
}));

vi.mock('../services/evaluatees', () => ({
  listEvaluatees: vi.fn().mockResolvedValue({
    items: [{ id: 3, name: '张三', departmentId: 1, departmentName: '研发部', enabled: true }],
    total: 11,
    page: 1,
    pageSize: 200
  })
}));

vi.mock('../services/evaluatorUsers', () => ({
  listEvaluatorUsers: vi.fn().mockImplementation((params?: { submitted?: boolean }) => {
    if (params?.submitted === true) {
      return Promise.resolve({ items: [], total: 13, page: 1, pageSize: 1 });
    }
    if (params?.submitted === false) {
      return Promise.resolve({ items: [], total: 17, page: 1, pageSize: 1 });
    }
    return Promise.resolve({
      items: [
        {
          id: 5,
          username: 'u0001',
          passwordPlain: 'pw',
          departmentId: 1,
          departmentName: '研发部',
          enabled: true,
          submitted: false,
          submittedAt: null,
          createdAt: '2026-07-01T00:00:00Z'
        }
      ],
      total: 19,
      page: 1,
      pageSize: 200
    });
  })
}));

vi.mock('../services/results', () => ({
  getResultSummary: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 }),
  getResultDetails: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 20 })
}));

describe('result pages', () => {
  beforeAll(() => {
    vi.spyOn(console, 'error').mockImplementation((message?: unknown, ...args: unknown[]) => {
      const text = String(message);
      if (
        text.includes('findDOMNode is deprecated') ||
        text.includes("Not implemented: window.getComputedStyle")
      ) {
        return;
      }
      originalConsoleError(message, ...args);
    });
  });

  beforeEach(() => {
    vi.clearAllMocks();
    proTableHarness.requestParams = [{ current: 1, pageSize: 20 }];
  });

  afterAll(() => {
    console.error = originalConsoleError;
    Object.defineProperty(window, 'getComputedStyle', {
      configurable: true,
      value: originalGetComputedStyle
    });
  });

  it('renders result summary columns and passes request params', async () => {
    proTableHarness.requestParams = [
      {
        current: 2,
        pageSize: 50,
        departmentId: '1',
        evaluateeName: '张三'
      }
    ];

    render(<ResultSummary />);

    expect(await screen.findByText('结果汇总')).toBeInTheDocument();
    expect(screen.getByText('被测评人员')).toBeInTheDocument();
    expect(screen.getByText('部门')).toBeInTheDocument();
    expect(screen.getByText('优秀')).toBeInTheDocument();
    expect(screen.getByText('称职')).toBeInTheDocument();
    expect(screen.getByText('基本称职')).toBeInTheDocument();
    expect(screen.getByText('不称职')).toBeInTheDocument();
    expect(screen.getByText('合计')).toBeInTheDocument();

    await waitFor(() => {
      expect(getResultSummary).toHaveBeenCalledWith({
        page: 2,
        pageSize: 50,
        departmentId: 1,
        evaluateeName: '张三'
      });
    });
  });

  it('does not pass invalid summary numeric filters as NaN', async () => {
    proTableHarness.requestParams = [
      {
        current: 1,
        pageSize: 20,
        departmentId: 'not-a-number'
      }
    ];

    render(<ResultSummary />);

    await waitFor(() => {
      expect(getResultSummary).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
        departmentId: undefined,
        evaluateeName: undefined
      });
    });
  });

  it('renders result detail columns and passes request params', async () => {
    proTableHarness.requestParams = [
      {
        current: 3,
        pageSize: 30,
        departmentId: '1',
        evaluateeId: '3',
        evaluatorUserId: '5',
        grade: 'EXCELLENT'
      }
    ];

    render(<ResultDetails />);

    expect(await screen.findByText('评价明细')).toBeInTheDocument();
    expect(screen.getByText('测评人')).toBeInTheDocument();
    expect(screen.getByText('测评人部门')).toBeInTheDocument();
    expect(screen.getByText('被测评人员')).toBeInTheDocument();
    expect(screen.getByText('评价结果')).toBeInTheDocument();
    expect(screen.getByText('问题及表现')).toBeInTheDocument();
    expect(screen.getByText('意见或建议')).toBeInTheDocument();
    expect(screen.getByText('提交时间')).toBeInTheDocument();

    await waitFor(() => {
      expect(getResultDetails).toHaveBeenCalledWith({
        page: 3,
        pageSize: 30,
        departmentId: 1,
        evaluateeId: 3,
        evaluatorUserId: 5,
        grade: 'EXCELLENT'
      });
    });
  });

  it('does not pass invalid detail numeric filters as NaN', async () => {
    proTableHarness.requestParams = [
      {
        current: 1,
        pageSize: 20,
        departmentId: 'bad-department',
        evaluateeId: 'bad-evaluatee',
        evaluatorUserId: 'bad-evaluator'
      }
    ];

    render(<ResultDetails />);

    await waitFor(() => {
      expect(getResultDetails).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
        departmentId: undefined,
        evaluateeId: undefined,
        evaluatorUserId: undefined,
        grade: undefined
      });
    });
  });

  it('loads dashboard statistics from list services with submitted filters', async () => {
    render(<AdminDashboard />);

    await waitFor(() => {
      expect(listDepartments).toHaveBeenCalledWith({ page: 1, pageSize: 1 });
      expect(listEvaluatees).toHaveBeenCalledWith({ page: 1, pageSize: 1 });
      expect(listEvaluatorUsers).toHaveBeenCalledWith({ page: 1, pageSize: 1 });
      expect(listEvaluatorUsers).toHaveBeenCalledWith({
        page: 1,
        pageSize: 1,
        submitted: true
      });
      expect(listEvaluatorUsers).toHaveBeenCalledWith({
        page: 1,
        pageSize: 1,
        submitted: false
      });
    });

    expect(screen.getByText('部门总数')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByText('被测评人员')).toBeInTheDocument();
    expect(screen.getByText('11')).toBeInTheDocument();
    expect(screen.getByText('测评用户')).toBeInTheDocument();
    expect(screen.getByText('19')).toBeInTheDocument();
    expect(screen.getByText('已提交')).toBeInTheDocument();
    expect(screen.getByText('13')).toBeInTheDocument();
    expect(screen.getByText('未提交')).toBeInTheDocument();
    expect(screen.getByText('17')).toBeInTheDocument();
  });

  it('declares the exact result route manifest entries from the brief', () => {
    expect(
      routes.filter((route) =>
        ['/admin/results/summary', '/admin/results/details'].includes(route.path ?? '')
      )
    ).toEqual([
      {
        path: '/admin/results/summary',
        component: './pages/admin/ResultSummary',
        layout: 'admin',
        name: '结果汇总'
      },
      {
        path: '/admin/results/details',
        component: './pages/admin/ResultDetails',
        layout: 'admin',
        name: '评价明细'
      }
    ]);
  });

  it.each([
    ['/admin/results/summary', '结果汇总'],
    ['/admin/results/details', '评价明细']
  ])('maps %s to the correct result page %s', async (path, title) => {
    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={[path]}
      >
        <AppRoutes />
      </MemoryRouter>
    );

    expect(await screen.findByText(title)).toBeInTheDocument();
    expect(screen.getByTestId('admin-layout')).toBeInTheDocument();
  });
});
