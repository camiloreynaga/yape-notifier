/**
 * Componente Toast para mostrar nuevas notificaciones
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Bell, DollarSign } from 'lucide-react';
import type { Notification } from '@/types';
import { format } from 'date-fns';

interface NotificationToastProps {
  notification: Notification;
  onClose: () => void;
  autoDismiss?: boolean;
  dismissAfter?: number; // en milisegundos
}

export default function NotificationToast({
  notification,
  onClose,
  autoDismiss = true,
  dismissAfter = 5000,
}: NotificationToastProps) {
  const navigate = useNavigate();
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    if (autoDismiss) {
      const timer = setTimeout(() => {
        setIsVisible(false);
        setTimeout(onClose, 300); // Esperar animación
      }, dismissAfter);

      return () => clearTimeout(timer);
    }
  }, [autoDismiss, dismissAfter, onClose]);

  const handleClick = () => {
    navigate(`/notifications/${notification.id}`);
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  const handleClose = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsVisible(false);
    setTimeout(onClose, 300);
  };

  if (!isVisible) {
    return null;
  }

  return (
    <div
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`Notificación: ${notification.title}. Click para ver detalles`}
      className="bg-white rounded-lg shadow-lg border border-gray-200 p-4 mb-3 cursor-pointer hover:shadow-xl transition-all duration-300 animate-slide-in-right max-w-md focus:outline-none focus:ring-2 focus:ring-primary-500"
    >
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0">
          <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
            {notification.amount ? (
              <DollarSign className="h-5 w-5 text-primary-600" />
            ) : (
              <Bell className="h-5 w-5 text-primary-600" />
            )}
          </div>
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1">
              <p className="text-sm font-semibold text-gray-900 truncate">
                {notification.title}
              </p>
              <p className="text-xs text-gray-600 mt-1 line-clamp-2">
                {notification.body}
              </p>
              {notification.amount && (
                <p className="text-sm font-bold text-primary-600 mt-1">
                  {notification.currency || 'S/'} {Number(notification.amount).toFixed(2)}
                </p>
              )}
              <p className="text-xs text-gray-500 mt-1">
                {format(new Date(notification.received_at), 'HH:mm:ss')}
              </p>
            </div>
            <button
              onClick={handleClose}
              className="flex-shrink-0 p-1 text-gray-400 hover:text-gray-600 rounded"
              aria-label="Cerrar notificación"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

