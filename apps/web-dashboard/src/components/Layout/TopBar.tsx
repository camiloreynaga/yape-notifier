import { Menu } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface Props {
  onMenuClick: () => void;
}

export default function TopBar({ onMenuClick }: Props) {
  const { user } = useAuth();
  const isSuperAdmin = user?.role === 'super_admin';

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-gray-200 bg-white px-4 md:px-6">
      <div className="flex items-center gap-3">
        <button
          aria-label="Abrir menu"
          onClick={onMenuClick}
          className="md:hidden rounded-md p-2 hover:bg-gray-100"
        >
          <Menu className="h-5 w-5" />
        </button>
        {isSuperAdmin && (
          <span className="inline-flex items-center rounded-full bg-accent-100 px-2.5 py-0.5 text-xs font-bold text-primary-800 ring-1 ring-accent-300">
            SUPER ADMIN
          </span>
        )}
      </div>
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium text-gray-900">{user?.name ?? '—'}</p>
          <p className="text-xs text-gray-500">{user?.email ?? ''}</p>
        </div>
      </div>
    </header>
  );
}
