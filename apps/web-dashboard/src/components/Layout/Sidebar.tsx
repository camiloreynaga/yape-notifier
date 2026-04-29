import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Bell,
  Smartphone,
  Users,
  ScrollText,
  Settings,
  LogOut,
  Shield,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const NAV: NavItem[] = [
  { to: '/dashboard',                       label: 'Resumen',        icon: LayoutDashboard },
  { to: '/dashboard?tab=notifications',     label: 'Notificaciones', icon: Bell },
  { to: '/dashboard?tab=devices',           label: 'Dispositivos',   icon: Smartphone },
  { to: '/dashboard?tab=employees',         label: 'Empleados',      icon: Users },
  { to: '/dashboard?tab=logs',              label: 'Logs',           icon: ScrollText },
  { to: '/dashboard?tab=settings',          label: 'Configuracion',  icon: Settings },
];

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user, logout } = useAuth();
  const isSuperAdmin = user?.role === 'super_admin';

  return (
    <>
      {open && (
        <button
          aria-label="Cerrar menu"
          onClick={onClose}
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
        />
      )}
      <aside
        className={`fixed md:static z-40 h-full w-56 shrink-0 bg-primary-800 text-white transition-transform ${open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
      >
        <div className="flex h-16 items-center gap-2 px-5 border-b border-primary-700">
          <span className="text-xl font-bold">Yape Notifier</span>
        </div>

        {isSuperAdmin && (
          <div className="px-4 py-3 border-b border-primary-700">
            <NavLink
              to="/super-admin"
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`
              }
            >
              <Shield className="h-4 w-4" /> Panel Super Admin
            </NavLink>
          </div>
        )}

        <nav className="px-4 py-4 space-y-1">
          {NAV.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/dashboard'}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`
              }
            >
              <Icon className="h-4 w-4" /> {label}
            </NavLink>
          ))}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 border-t border-primary-700 p-4">
          <div className="text-xs text-white/60 mb-2 truncate">{user?.email ?? ''}</div>
          <button
            onClick={logout}
            className="flex items-center gap-2 text-sm text-white/80 hover:text-white"
          >
            <LogOut className="h-4 w-4" /> Cerrar sesion
          </button>
        </div>
      </aside>
    </>
  );
}
