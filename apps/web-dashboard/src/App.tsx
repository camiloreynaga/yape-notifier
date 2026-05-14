import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect, lazy, Suspense } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import AppLayout from './components/Layout/AppLayout';
import { NotificationToastContainer } from './components/NotificationToast';
import { ErrorNotificationContainer } from './components/ErrorNotification';
import ToastContainer from './components/Toast/ToastContainer';
import { RouteLoadingFallback } from './components/LoadingFallback';
import { updateAuthToken } from './services/echo';
import { logger } from './services/logger';

// Lazy loading de páginas para code splitting
// Esto reduce el bundle inicial y carga cada página solo cuando se necesita
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const AddDevicePage = lazy(() => import('./pages/AddDevicePage'));
const AppInstancesPage = lazy(() => import('./pages/AppInstancesPage'));
const CreateCommercePage = lazy(() => import('./pages/CreateCommercePage'));
const CommerceStatusPage = lazy(() => import('./pages/CommerceStatusPage'));
const SuperAdminPage = lazy(() => import('./pages/SuperAdminPage'));
const SuperAdminCommercesTab = lazy(() => import('./pages/SuperAdminCommercesTab'));
const SuperAdminPlansTab = lazy(() => import('./pages/SuperAdminPlansTab'));

// Configurar React Query Client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: true,
      retry: 1,
      staleTime: 30000, // 30 segundos
    },
  },
});

interface PrivateRouteProps {
  children: React.ReactNode;
  requireCommerce?: boolean;
}

function PrivateRoute({ children, requireCommerce = false }: PrivateRouteProps) {
  const { isAuthenticated, loading, hasCommerce, commerce, user } = useAuth();
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

  const isSuperAdmin = user?.role === 'super_admin';

  // Super admins tienen su propia vista. Cualquier ruta del dashboard
  // de comercio (que asume commerce_id) los manda a /super-admin.
  if (isSuperAdmin && !location.pathname.startsWith('/super-admin')) {
    return <Navigate to="/super-admin" replace />;
  }

  if (requireCommerce) {
    // No commerce at all → create flow
    if (!hasCommerce && location.pathname !== '/create-commerce') {
      return <Navigate to="/create-commerce" replace />;
    }
    // Has a commerce but it's pending/suspended → status screen.
    // This is the fix for users getting stuck re-creating a pending commerce.
    if (
      hasCommerce &&
      commerce &&
      commerce.status !== 'active' &&
      location.pathname !== '/commerce-status'
    ) {
      return <Navigate to="/commerce-status" replace />;
    }
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
    <Suspense fallback={<RouteLoadingFallback />}>
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
        <Route path="/commerce-status" element={
          <PrivateRoute requireCommerce={false}>
            <CommerceStatusPage />
          </PrivateRoute>
        } />
        <Route path="/super-admin" element={
          <PrivateRoute requireCommerce={false}>
            <AppLayout />
          </PrivateRoute>
        }>
          <Route element={<SuperAdminPage />}>
            <Route index element={<Navigate to="/super-admin/commerces" replace />} />
            <Route path="commerces" element={<SuperAdminCommercesTab />} />
            <Route path="plans" element={<SuperAdminPlansTab />} />
          </Route>
        </Route>
        <Route path="/" element={
          <PrivateRoute requireCommerce={true}>
            <AppLayout />
          </PrivateRoute>
        }>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          {/* Rutas individuales para deep linking */}
          <Route path="notifications" element={<Navigate to="/dashboard?tab=notifications" replace />} />
          <Route path="devices" element={<Navigate to="/dashboard?tab=devices" replace />} />
          <Route path="devices/add" element={<AddDevicePage />} />
          <Route path="employees" element={<Navigate to="/dashboard?tab=employees" replace />} />
          <Route path="logs" element={<Navigate to="/dashboard?tab=logs" replace />} />
          <Route path="app-instances" element={<AppInstancesPage />} />
          <Route path="settings/monitored-apps" element={<Navigate to="/dashboard?tab=settings" replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
}

// Componente para manejar eventos de WebSocket a nivel de app
function WebSocketErrorHandler() {
  const { logout } = useAuth();

  useEffect(() => {
    const handleTokenExpired = () => {
      logger.warn("Token expirado, cerrando sesión");
      logout();
    };

    const handleAuthError = (event: CustomEvent) => {
      logger.error("Error de autenticación en WebSocket", event.detail);
      // Opcional: mostrar notificación al usuario
      // Por ahora solo cerramos sesión
      logout();
    };

    window.addEventListener("echo:token-expired", handleTokenExpired);
    window.addEventListener("echo:auth-error", handleAuthError as EventListener);

    return () => {
      window.removeEventListener("echo:token-expired", handleTokenExpired);
      window.removeEventListener("echo:auth-error", handleAuthError as EventListener);
    };
  }, [logout]);

  return null;
}

// Componente para actualizar token en Echo
function EchoTokenUpdater() {
  const { token } = useAuth();

  // Actualizar token en Echo cuando cambia
  useEffect(() => {
    if (token) {
      updateAuthToken(token);
    }
  }, [token]);

  return null;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
    <AuthProvider>
        <WebSocketErrorHandler />
        <EchoTokenUpdater />
      <Router>
        <AppRoutes />
        <NotificationToastContainer />
        <ErrorNotificationContainer />
        <ToastContainer />
      </Router>
    </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;

