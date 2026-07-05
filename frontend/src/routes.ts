export type AppRoute = {
  path?: string;
  index?: boolean;
  redirect?: string;
  component?:
    | './pages/admin/Login'
    | './pages/admin/Dashboard'
    | './pages/admin/Departments'
    | './pages/admin/Evaluatees'
    | './pages/admin/EvaluatorUsers'
    | './pages/admin/AdminUsers'
    | './pages/admin/ResultSummary'
    | './pages/admin/ResultDetails'
    | './pages/mobile/Login'
    | './pages/mobile/EvaluationPage'
    | './pages/mobile/ConfirmPage'
    | './pages/mobile/CompletePage';
  name?: string;
  layout?: 'admin' | false;
  children?: AppRoute[];
};

export const routes: AppRoute[] = [
  { path: '/', redirect: '/admin/login' },
  { path: '/admin/login', component: './pages/admin/Login', layout: false },
  {
    path: '/admin',
    component: './pages/admin/Dashboard',
    layout: 'admin',
    name: '首页'
  },
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
  },
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
  },
  { path: '/mobile', redirect: '/mobile/login', layout: false },
  { path: '/mobile/login', component: './pages/mobile/Login', layout: false },
  { path: '/mobile/evaluation', component: './pages/mobile/EvaluationPage', layout: false },
  { path: '/mobile/confirm', component: './pages/mobile/ConfirmPage', layout: false },
  { path: '/mobile/complete', component: './pages/mobile/CompletePage', layout: false }
];

export function getAdminNavigationRoutes(routeList: AppRoute[]): Array<{ path?: string; name?: string }> {
  return routeList
    .filter((route) => route.layout === 'admin')
    .map((route) => ({
      path: route.path,
      name: route.name
    }));
}

export function getRouteChildren(route: AppRoute): AppRoute[] | undefined {
  const children = [...(route.children ?? [])];

  if (route.layout === 'admin' && route.component) {
    children.unshift({
      index: true,
      component: route.component
    });
  }

  return children.length > 0 ? children : undefined;
}
