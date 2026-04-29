import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { LogOut, ArrowLeft } from 'lucide-react';
import CommerceBanner from './CommerceBanner';

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
  };

  // Determinar si estamos en una página standalone (no dashboard)
  const isStandalonePage = 
    !location.pathname.includes('/dashboard') && 
    location.pathname !== '/' &&
    location.pathname !== '/login' && 
    location.pathname !== '/register';

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Top header - Sticky */}
      <header className="sticky top-0 z-50 bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-[1920px] mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-4">
              <h1 className="text-2xl font-bold text-primary-600">Yape Notifier</h1>
              {user?.role === 'super_admin' && (
                <span className="inline-flex items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-bold text-indigo-700 ring-1 ring-indigo-200">
                  SUPER ADMIN
                </span>
              )}
              {isStandalonePage && (
                <button
                  onClick={() => navigate('/dashboard')}
                  className="flex items-center gap-2 px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                  title="Volver al dashboard"
                >
                  <ArrowLeft className="h-4 w-4" />
                  <span className="hidden sm:inline">Volver</span>
                </button>
              )}
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium text-gray-900">
                  {user?.name}
                </p>
                <p className="text-xs text-gray-500">
                  {user?.email}
                </p>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-gray-500 rounded-lg hover:bg-gray-100 transition-colors"
                title="Cerrar sesión"
              >
                <LogOut className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main content - Full width, mejor aprovechamiento del espacio */}
      <main className="flex-1">
        <div className="py-6">
          <div className="max-w-[1920px] mx-auto px-4 sm:px-6 lg:px-8">
            {isStandalonePage && <CommerceBanner />}
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}

