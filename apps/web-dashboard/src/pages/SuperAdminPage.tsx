import { Outlet, NavLink } from 'react-router-dom';
import { Building2, CreditCard, Shield, Banknote } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

export default function SuperAdminPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-4" role="main" aria-label="Panel de Super Admin">
      <div className="rounded-2xl bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-600 p-6 shadow-xl text-white">
        <div className="flex items-center gap-3">
          <Shield className="h-7 w-7" />
          <div>
            <h1 className="text-2xl font-bold">Panel de Super Admin</h1>
            <p className="text-sm text-indigo-100">Bienvenido, {user?.name}</p>
          </div>
        </div>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-6" aria-label="Tabs">
          <NavLink
            to="/super-admin/commerces"
            className={({ isActive }) =>
              `flex items-center gap-2 border-b-2 py-3 px-1 text-sm font-medium ${isActive ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'}`
            }
          >
            <Building2 className="h-4 w-4" /> Comercios
          </NavLink>
          <NavLink
            to="/super-admin/plans"
            className={({ isActive }) =>
              `flex items-center gap-2 border-b-2 py-3 px-1 text-sm font-medium ${isActive ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'}`
            }
          >
            <CreditCard className="h-4 w-4" /> Planes
          </NavLink>
          <NavLink
            to="/super-admin/commissions"
            className={({ isActive }) =>
              `flex items-center gap-2 border-b-2 py-3 px-1 text-sm font-medium ${isActive ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'}`
            }
          >
            <Banknote className="h-4 w-4" /> Comisiones
          </NavLink>
        </nav>
      </div>

      <Outlet />
    </div>
  );
}
