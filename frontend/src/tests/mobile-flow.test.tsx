import '@testing-library/jest-dom/vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from '../app';
import { routes } from '../routes';
import type { EvaluationItemsResult, SubmitEvaluationRecord } from '../services/mobileEvaluation';
import { getEvaluationItems, submitEvaluation } from '../services/mobileEvaluation';
import { mobileLogin } from '../services/auth';
import { saveToken } from '../services/session';

const defaultEvaluationItems: EvaluationItemsResult = {
  departmentId: 1,
  departmentName: '组织部',
  submitted: false,
  items: [
    { evaluateeId: 1, name: '张三' },
    { evaluateeId: 2, name: '李四' }
  ],
  grades: [
    { value: 'EXCELLENT', label: '优秀' },
    { value: 'COMPETENT', label: '称职' },
    { value: 'BASIC_COMPETENT', label: '基本称职' },
    { value: 'INCOMPETENT', label: '不称职' }
  ]
};

vi.mock('../services/mobileEvaluation', () => ({
  getEvaluationItems: vi.fn(),
  submitEvaluation: vi.fn()
}));

vi.mock('../services/auth', () => ({
  mobileLogin: vi.fn()
}));

vi.mock('../services/session', () => ({
  saveToken: vi.fn()
}));

function renderMobileRoute(initialEntry: string) {
  return render(
    <MemoryRouter
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      initialEntries={[initialEntry]}
    >
      <AppRoutes />
    </MemoryRouter>
  );
}

describe('mobile evaluation flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    vi.mocked(getEvaluationItems).mockResolvedValue(structuredClone(defaultEvaluationItems));
    vi.mocked(submitEvaluation).mockResolvedValue({
      submitted: true,
      submittedAt: '2026-07-04T00:00:00.000Z'
    });
    vi.mocked(mobileLogin).mockResolvedValue({
      token: 'mobile-token',
      user: {
        id: 1,
        username: 'mobile-user',
        departmentId: 1,
        departmentName: '组织部',
        submitted: false
      }
    });
  });

  it('declares the exact mobile route manifest entries from the brief', () => {
    expect(
      routes.filter((route) =>
        [
          '/mobile/login',
          '/mobile/evaluation',
          '/mobile/confirm',
          '/mobile/complete'
        ].includes(route.path ?? '')
      )
    ).toEqual([
      { path: '/mobile/login', component: './pages/mobile/Login', layout: false },
      { path: '/mobile/evaluation', component: './pages/mobile/EvaluationPage', layout: false },
      { path: '/mobile/confirm', component: './pages/mobile/ConfirmPage', layout: false },
      { path: '/mobile/complete', component: './pages/mobile/CompletePage', layout: false }
    ]);
  });

  it('saves the mobile token and navigates to evaluation after login success', async () => {
    renderMobileRoute('/mobile/login');

    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'mobile-user' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'secret' } });
    fireEvent.click(screen.getByRole('button', { name: /进入测评/ }));

    await waitFor(() => {
      expect(mobileLogin).toHaveBeenCalledWith('mobile-user', 'secret');
      expect(saveToken).toHaveBeenCalledWith('mobile-token');
    });

    expect(await screen.findByText('组织部')).toBeInTheDocument();
  });

  it('shows the mobile login API error message on failure', async () => {
    vi.mocked(mobileLogin).mockRejectedValueOnce(new Error('账号或密码错误'));

    renderMobileRoute('/mobile/login');

    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'mobile-user' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /进入测评/ }));

    expect(await screen.findByText('账号或密码错误')).toBeInTheDocument();
  });

  it('requires all evaluatees to be graded before submit', async () => {
    renderMobileRoute('/mobile/evaluation');

    const submit = await screen.findByRole('button', { name: /提\s*交/ });
    expect(submit).toBeDisabled();

    fireEvent.click(screen.getAllByText('优秀')[0]);
    expect(submit).toBeDisabled();

    fireEvent.click(screen.getAllByText('称职')[1]);
    expect(submit).toBeEnabled();
  });

  it('renders the formal mobile evaluation form structure with progress and item status', async () => {
    renderMobileRoute('/mobile/evaluation');

    expect(await screen.findByTestId('mobile-evaluation-summary')).toHaveTextContent('部门：组织部');
    expect(screen.getByTestId('mobile-evaluation-summary')).toHaveTextContent('已评 0 / 2');
    expect(screen.getByTestId('mobile-evaluation-summary')).toHaveTextContent(
      '请逐项完成评分，提交前可返回确认。'
    );
    expect(screen.getByTestId('mobile-evaluatee-card-1')).toHaveTextContent('01');
    expect(screen.getByTestId('mobile-evaluatee-card-1')).toHaveTextContent('未评分');
    expect(screen.getByTestId('mobile-evaluatee-card-2')).toHaveTextContent('02');
    expect(screen.getAllByText('综合评价')).toHaveLength(2);
    expect(screen.getAllByText('文字意见')).toHaveLength(2);
    expect(screen.getByTestId('mobile-evaluation-footer-progress')).toHaveTextContent('已完成 0/2');

    fireEvent.click(screen.getAllByText('优秀')[0]);

    expect(screen.getByTestId('mobile-evaluation-summary')).toHaveTextContent('已评 1 / 2');
    expect(screen.getByTestId('mobile-evaluatee-card-1')).toHaveTextContent('已评分');
    expect(screen.getByTestId('mobile-evaluation-footer-progress')).toHaveTextContent('已完成 1/2');
  });

  it('preserves selections through confirm and submits before navigating to completion', async () => {
    renderMobileRoute('/mobile/evaluation');

    const submit = await screen.findByRole('button', { name: /提\s*交/ });
    fireEvent.click(screen.getAllByText('优秀')[0]);
    fireEvent.click(screen.getAllByText('称职')[1]);
    fireEvent.click(submit);

    expect(await screen.findByText('提交确认')).toBeInTheDocument();
    expect(screen.getByText(/已完成/)).toHaveTextContent('已完成 2 人评分。');

    fireEvent.click(screen.getByRole('button', { name: /确认提交/ }));

    await waitFor(() => {
      expect(submitEvaluation).toHaveBeenCalledWith([
        { evaluateeId: 1, grade: 'EXCELLENT' },
        { evaluateeId: 2, grade: 'COMPETENT' }
      ] satisfies SubmitEvaluationRecord[]);
    });

    expect(await screen.findByText('提交完成')).toBeInTheDocument();
  });

  it('submits per-evaluatee comments with the selected grades', async () => {
    renderMobileRoute('/mobile/evaluation');

    const submit = await screen.findByRole('button', { name: /提\s*交/ });
    fireEvent.click(screen.getAllByText('优秀')[0]);
    fireEvent.click(screen.getAllByText('称职')[1]);

    const problemInputs = screen.getAllByLabelText('是否存在慵懒散、不担当、不作为等问题及表现');
    const suggestionInputs = screen.getAllByLabelText('意见或建议');
    fireEvent.change(problemInputs[0], { target: { value: '张三存在推进慢的问题' } });
    fireEvent.change(suggestionInputs[0], { target: { value: '建议加强跟踪督办' } });
    fireEvent.change(problemInputs[1], { target: { value: '李四暂无明显问题' } });
    fireEvent.change(suggestionInputs[1], { target: { value: '建议保持工作节奏' } });

    fireEvent.click(submit);
    fireEvent.click(await screen.findByRole('button', { name: /确认提交/ }));

    await waitFor(() => {
      expect(submitEvaluation).toHaveBeenCalledWith([
        {
          evaluateeId: 1,
          grade: 'EXCELLENT',
          comment1: '张三存在推进慢的问题',
          comment2: '建议加强跟踪督办'
        },
        {
          evaluateeId: 2,
          grade: 'COMPETENT',
          comment1: '李四暂无明显问题',
          comment2: '建议保持工作节奏'
        }
      ] satisfies SubmitEvaluationRecord[]);
    });
  });

  it('clears any stored draft when the backend reports submitted already', async () => {
    sessionStorage.setItem(
      'smart_eval_mobile_draft',
      JSON.stringify({
        departmentId: 1,
        departmentName: '组织部',
        items: defaultEvaluationItems.items,
        grades: defaultEvaluationItems.grades,
        selections: {
          '1': 'EXCELLENT',
          '2': 'COMPETENT'
        }
      })
    );
    vi.mocked(getEvaluationItems).mockResolvedValueOnce({
      departmentId: 1,
      departmentName: '组织部',
      submitted: true,
      items: [],
      grades: []
    });

    renderMobileRoute('/mobile/evaluation');

    expect(await screen.findByText('提交完成')).toBeInTheDocument();
    expect(sessionStorage.getItem('smart_eval_mobile_draft')).toBeNull();
  });

  it('redirects back to evaluation instead of submitting a stale or invalid confirm draft', async () => {
    sessionStorage.setItem(
      'smart_eval_mobile_draft',
      JSON.stringify({
        departmentId: 1,
        departmentName: '组织部',
        items: defaultEvaluationItems.items,
        grades: defaultEvaluationItems.grades,
        selections: {
          '1': 'EXCELLENT',
          '2': 'HACKED'
        }
      })
    );

    renderMobileRoute('/mobile/confirm');

    expect(await screen.findByText('组织部')).toBeInTheDocument();
    expect(submitEvaluation).not.toHaveBeenCalled();
  });

  it('clears malformed confirm drafts and routes back to evaluation without submitting', async () => {
    const malformedDraft = JSON.stringify({
      departmentId: 1,
      departmentName: '组织部',
      items: { broken: true },
      grades: 'EXCELLENT',
      selections: []
    });

    sessionStorage.setItem(
      'smart_eval_mobile_draft',
      malformedDraft
    );

    renderMobileRoute('/mobile/confirm');

    expect(await screen.findByText('组织部')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /提\s*交/ })).toBeDisabled();
    expect(submitEvaluation).not.toHaveBeenCalled();
    expect(sessionStorage.getItem('smart_eval_mobile_draft')).not.toBe(malformedDraft);
  });

  it('ignores malformed saved drafts while loading fresh evaluation items', async () => {
    sessionStorage.setItem(
      'smart_eval_mobile_draft',
      JSON.stringify({
        departmentId: 1,
        departmentName: '组织部',
        items: defaultEvaluationItems.items,
        grades: defaultEvaluationItems.grades,
        selections: null
      })
    );

    renderMobileRoute('/mobile/evaluation');

    expect(await screen.findByText('张三')).toBeInTheDocument();
    expect(screen.queryByText('加载失败')).not.toBeInTheDocument();
    expect(sessionStorage.getItem('smart_eval_mobile_draft')).not.toBeNull();
  });
});
