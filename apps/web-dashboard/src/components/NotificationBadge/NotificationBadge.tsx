/**
 * Componente para mostrar el badge de notificaciones no leídas
 */

import { Bell } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useUnreadNotifications } from '@/hooks/useUnreadNotifications';

interface NotificationBadgeProps {
  className?: string;
  showLabel?: boolean;
}

export default function NotificationBadge({ className = '', showLabel = false }: NotificationBadgeProps) {
  const { unreadCount, markAllAsRead } = useUnreadNotifications();

  const handleClick = () => {
    // Marcar todas como leídas al hacer clic
    markAllAsRead();
  };

  if (unreadCount === 0) {
    return (
      <Link
        to="/dashboard?tab=notifications"
        className={`flex items-center gap-2 ${className}`}
        aria-label="Notificaciones"
      >
        <Bell className="h-5 w-5 text-gray-400" />
        {showLabel && <span className="text-sm text-gray-700">Notificaciones</span>}
      </Link>
    );
  }

  return (
    <Link
      to="/dashboard?tab=notifications"
      onClick={handleClick}
      className={`relative flex items-center gap-2 ${className}`}
      aria-label={`${unreadCount} notificaciones no leídas`}
    >
      <Bell className="h-5 w-5 text-primary-600" />
      {showLabel && <span className="text-sm text-gray-700">Notificaciones</span>}
      <span
        className="absolute -top-1 -right-1 flex items-center justify-center min-w-[20px] h-5 px-1.5 text-xs font-bold text-white bg-red-500 rounded-full animate-pulse"
        aria-live="polite"
        aria-atomic="true"
      >
        {unreadCount > 99 ? '99+' : unreadCount}
      </span>
    </Link>
  );
}

