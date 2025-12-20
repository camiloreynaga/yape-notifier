import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import NotificationsPage from './pages/NotificationsPage';
import DevicesPage from './pages/DevicesPage';
import AddDevicePage from './pages/AddDevicePage';
import AppInstancesPage from './pages/AppInstancesPage';
import MonitoredAppsPage from './pages/MonitoredAppsPage';
import CreateCommercePage from './pages/CreateCommercePage';
import Layout from './components/Layout';

interface PrivateRouteProps {
  children: React.ReactNode;
  requireCommerce?: boolean;
}

function PrivateRoute({ children, requireCommerce = false }: PrivateRouteProps) {
  const { isAuthenticated, loading, hasCommerce } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // Si requiere commerce y no lo tiene, redirigir a crear commerce
  // Excepto si ya está en la página de crear commerce
  if (requireCommerce && !hasCommerce && location.pathname !== '/create-commerce') {
    return <Navigate to="/create-commerce" replace />;
  }

  return <>{children}</>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return !isAuthenticated ? <>{children}</> : <Navigate to="/dashboard" replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={
        <PublicRoute>
          <LoginPage />
        </PublicRoute>
      } />
      <Route path="/register" element={
        <PublicRoute>
          <RegisterPage />
        </PublicRoute>
      } />
      <Route path="/create-commerce" element={
        <PrivateRoute requireCommerce={false}>
          <CreateCommercePage />
        </PrivateRoute>
      } />
      <Route path="/" element={
        <PrivateRoute requireCommerce={true}>
          <Layout />
        </PrivateRoute>
      }>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="devices" element={<DevicesPage />} />
        <Route path="devices/add" element={<AddDevicePage />} />
        <Route path="app-instances" element={<AppInstancesPage />} />
        <Route path="settings/monitored-apps" element={<MonitoredAppsPage />} />
      </Route>
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;

