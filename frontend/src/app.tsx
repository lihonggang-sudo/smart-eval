import 'antd/dist/reset.css';
import { LogoutOutlined } from '@ant-design/icons';
import { ProLayout, PageContainer } from '@ant-design/pro-components';
import { Button, Typography } from 'antd';
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  type RouteObject,
  useLocation,
  useNavigate,
  useRoutes
} from 'react-router-dom';
import { getAdminNavigationRoutes, getRouteChildren, routes, type AppRoute } from './routes';
import AdminUsers from './pages/admin/AdminUsers';
import Departments from './pages/admin/Departments';
import AdminDashboard from './pages/admin/Dashboard';
import Evaluatees from './pages/admin/Evaluatees';
import EvaluatorUsers from './pages/admin/EvaluatorUsers';
import AdminLogin from './pages/admin/Login';
import ResultDetails from './pages/admin/ResultDetails';
import ResultSummary from './pages/admin/ResultSummary';
import CompletePage from './pages/mobile/CompletePage';
import ConfirmPage from './pages/mobile/ConfirmPage';
import EvaluationPage from './pages/mobile/EvaluationPage';
import MobileLogin from './pages/mobile/Login';
import { clearToken } from './services/session';

function AdminShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const adminRoutes = getAdminNavigationRoutes(routes);

  function handleLogout() {
    clearToken();
    navigate('/admin/login', { replace: true });
  }

  return (
    <div data-testid="admin-layout">
      <ProLayout
        actionsRender={() => [
          <Button
            key="logout"
            aria-label="退出系统"
            icon={<LogoutOutlined />}
            type="text"
            onClick={handleLogout}
          >
            退出系统
          </Button>
        ]}
        fixSiderbar
        location={{ pathname: location.pathname }}
        menuItemRender={(item, dom) => <Link to={item.path ?? '/admin'}>{dom}</Link>}
        route={{ routes: adminRoutes }}
        title="Smart Eval"
      >
        <PageContainer
          header={{
            title: false,
            breadcrumb: {}
          }}
        >
          <Outlet />
        </PageContainer>
      </ProLayout>
    </div>
  );
}

function renderElement(component?: AppRoute['component']) {
  switch (component) {
    case './pages/admin/Login':
      return <AdminLogin />;
    case './pages/admin/Dashboard':
      return <AdminDashboard />;
    case './pages/admin/Departments':
      return <Departments />;
    case './pages/admin/Evaluatees':
      return <Evaluatees />;
    case './pages/admin/EvaluatorUsers':
      return <EvaluatorUsers />;
    case './pages/admin/AdminUsers':
      return <AdminUsers />;
    case './pages/admin/ResultSummary':
      return <ResultSummary />;
    case './pages/admin/ResultDetails':
      return <ResultDetails />;
    case './pages/mobile/Login':
      return <MobileLogin />;
    case './pages/mobile/EvaluationPage':
      return <EvaluationPage />;
    case './pages/mobile/ConfirmPage':
      return <ConfirmPage />;
    case './pages/mobile/CompletePage':
      return <CompletePage />;
    default:
      return <Typography.Text>Route not implemented.</Typography.Text>;
  }
}

function buildRouteObjects(routeList: AppRoute[]): RouteObject[] {
  return routeList.map((route) => {
    const children = getRouteChildren(route);
    const element = route.redirect ? (
      <Navigate replace to={route.redirect} />
    ) : route.layout === 'admin' ? (
      <AdminShell />
    ) : (
      renderElement(route.component)
    );

    if (route.index) {
      return {
        index: true,
        element
      };
    }

    return {
      path: route.path,
      element,
      children: children ? buildRouteObjects(children) : undefined
    };
  });
}

export function AppRoutes() {
  const element = useRoutes(buildRouteObjects(routes));

  return element;
}

export default function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AppRoutes />
    </BrowserRouter>
  );
}
