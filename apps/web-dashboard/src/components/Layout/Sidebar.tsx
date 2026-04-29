import { useNavigate, useLocation } from 'react-router-dom';
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
  tab: string | null; // tab query value, null for "no tab" (overview)
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const NAV: NavItem[] = [
  { to: '/dashboard',                       tab: null,             label: 'Resumen',        icon: LayoutDashboard },
  { to: '/dashboard?tab=notifications',     tab: 'notifications',  label: 'Notificaciones', icon: Bell },
  { to: '/dashboard?tab=devices',           tab: 'devices',        label: 'Dispositivos',   icon: Smartphone },
  { to: '/dashboard?tab=employees',         tab: 'employees',      label: 'Empleados',      icon: Users },
  { to: '/dashboard?tab=logs',              tab: 'logs',           label: 'Logs',           icon: ScrollText },
  { to: '/dashboard?tab=settings',          tab: 'settings',       label: 'Configuracion',  icon: Settings },
];

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const isSuperAdmin = user?.role === 'super_admin';

  const currentTab = new URLSearchParams(location.search).get('tab');
  const onDashboard = location.pathname === '/dashboard';
  const onSuperAdmin = location.pathname.startsWith('/super-admin');

  const isActive = (item: NavItem) => onDashboard && currentTab === item.tab;

  const handleClick = (to: string) => {
    navigate(to);
    onClose();
  };

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
        className={`fixed md:sticky md:top-0 z-40 h-screen w-56 shrink-0 bg-primary-800 text-white transition-transform flex flex-col ${open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
      >
        {/* Brand */}
        <div className="flex h-16 items-center gap-2 px-5 border-b border-primary-700 shrink-0">
          <span className="text-xl font-bold">Yape Notifier</span>
        </div>

        {/* Super admin shortcut */}
        {isSuperAdmin && (
          <div className="px-4 py-3 border-b border-primary-700 shrink-0">
            <button
              onClick={() => handleClick('/super-admin')}
              className={`w-full flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${onSuperAdmin ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`}
            >
              <Shield className="h-4 w-4" /> Panel Super Admin
            </button>
          </div>
        )}

        {/* Main nav */}
        <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = isActive(item);
            return (
              <button
                key={item.to}
                onClick={() => handleClick(item.to)}
                className={`w-full flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${active ? 'bg-accent-300 text-primary-900' : 'text-white/80 hover:bg-primary-700 hover:text-white'}`}
              >
                <Icon className="h-4 w-4" /> {item.label}
              </button>
            );
          })}
        </nav>

        {/* Footer with user info + logout */}
        <div className="border-t border-primary-700 p-4 shrink-0">
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
