// src/components/ErrorNotification/ErrorNotification.tsx
// Sistema de notificaciones de error mejorado

import { useEffect, useState } from 'react';
import { AlertCircle, WifiOff, RefreshCw, X } from 'lucide-react';

interface ErrorNotificationProps {
  id: string;
  title: string;
  message: string;
  type: 'error' | 'warning' | 'circuit-open' | 'recovery';
  duration?: number;
  onClose: (id: string) => void;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function ErrorNotification({
  id,
  title,
  message,
  type,
  duration = 5000,
  onClose,
  action,
}: ErrorNotificationProps) {
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    if (duration && duration > 0) {
      const timer = setTimeout(() => {
        handleClose();
      }, duration);

      return () => clearTimeout(timer);
    }
  }, [duration]);

  const handleClose = () => {
    setIsExiting(true);
    setTimeout(() => {
      onClose(id);
    }, 300); // Duración de la animación
  };

  const config = {
    error: {
      icon: AlertCircle,
      bgColor: 'bg-red-50',
      borderColor: 'border-red-500',
      iconColor: 'text-red-600',
      textColor: 'text-red-800',
    },
    warning: {
      icon: AlertCircle,
      bgColor: 'bg-yellow-50',
      borderColor: 'border-yellow-500',
      iconColor: 'text-yellow-600',
      textColor: 'text-yellow-800',
    },
    'circuit-open': {
      icon: WifiOff,
      bgColor: 'bg-orange-50',
      borderColor: 'border-orange-500',
      iconColor: 'text-orange-600',
      textColor: 'text-orange-800',
    },
    recovery: {
      icon: RefreshCw,
      bgColor: 'bg-green-50',
      borderColor: 'border-green-500',
      iconColor: 'text-green-600',
      textColor: 'text-green-800',
    },
  }[type];

  const Icon = config.icon;

  return (
    <div
      className={`
        ${config.bgColor} ${config.borderColor} ${config.textColor}
        border-l-4 p-4 rounded shadow-lg mb-3 transition-all duration-300
        ${isExiting ? 'opacity-0 translate-x-full' : 'opacity-100 translate-x-0'}
      `}
      role="alert"
    >
      <div className="flex items-start">
        <Icon className={`h-5 w-5 ${config.iconColor} flex-shrink-0 mt-0.5`} />
        <div className="ml-3 flex-1">
          <h3 className="text-sm font-semibold">{title}</h3>
          <p className="text-sm mt-1">{message}</p>
          {action && (
            <button
              onClick={action.onClick}
              className={`
                mt-2 text-sm font-medium underline hover:no-underline
                ${config.iconColor}
              `}
            >
              {action.label}
            </button>
          )}
        </div>
        <button
          onClick={handleClose}
          className={`ml-3 flex-shrink-0 ${config.iconColor} hover:opacity-70 transition-opacity`}
          aria-label="Cerrar notificación"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

interface ErrorNotification {
  id: string;
  title: string;
  message: string;
  type: 'error' | 'warning' | 'circuit-open' | 'recovery';
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function ErrorNotificationContainer() {
  const [notifications, setNotifications] = useState<ErrorNotification[]>([]);

  useEffect(() => {
    // Escuchar eventos de error de red
    const handleNetworkError = (event: Event) => {
      const customEvent = event as CustomEvent<{ message: string; endpoint?: string }>;
      addNotification({
        id: `network-error-${Date.now()}`,
        title: 'Error de conexión',
        message: customEvent.detail.message || 'No se pudo conectar con el servidor',
        type: 'error',
        duration: 5000,
      });
    };

    // Escuchar eventos de circuit breaker abierto
    const handleCircuitOpen = (event: Event) => {
      const customEvent = event as CustomEvent<{ name: string; nextAttempt: number }>;
      const timeUntilRetry = Math.round((customEvent.detail.nextAttempt - Date.now()) / 1000);

      addNotification({
        id: `circuit-open-${customEvent.detail.name}`,
        title: 'Servicio temporalmente no disponible',
        message: `El servicio ${customEvent.detail.name} no está respondiendo. Reintentando en ${timeUntilRetry} segundos.`,
        type: 'circuit-open',
        duration: 10000,
      });
    };

    // Escuchar eventos de recuperación de circuit breaker
    const handleCircuitRecovered = (event: Event) => {
      const customEvent = event as CustomEvent<{ name: string }>;

      // Remover notificación de circuit open si existe
      setNotifications((prev) =>
        prev.filter((n) => n.id !== `circuit-open-${customEvent.detail.name}`)
      );

      addNotification({
        id: `circuit-recovered-${customEvent.detail.name}`,
        title: 'Servicio recuperado',
        message: `El servicio ${customEvent.detail.name} está funcionando nuevamente.`,
        type: 'recovery',
        duration: 3000,
      });
    };

    // Escuchar error de autenticación de WebSocket
    const handleAuthError = () => {
      addNotification({
        id: 'auth-error',
        title: 'Error de autenticación',
        message: 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.',
        type: 'error',
        duration: 0, // No auto-cerrar
        action: {
          label: 'Iniciar sesión',
          onClick: () => {
            window.location.href = '/login';
          },
        },
      });
    };

    window.addEventListener('network-error', handleNetworkError);
    window.addEventListener('circuit-breaker:open', handleCircuitOpen);
    window.addEventListener('circuit-breaker:recovered', handleCircuitRecovered);
    window.addEventListener('echo:auth-error', handleAuthError);

    return () => {
      window.removeEventListener('network-error', handleNetworkError);
      window.removeEventListener('circuit-breaker:open', handleCircuitOpen);
      window.removeEventListener('circuit-breaker:recovered', handleCircuitRecovered);
      window.removeEventListener('echo:auth-error', handleAuthError);
    };
  }, []);

  const addNotification = (notification: ErrorNotification) => {
    // Evitar duplicados
    setNotifications((prev) => {
      const exists = prev.some((n) => n.id === notification.id);
      if (exists) {
        return prev;
      }
      return [...prev, notification];
    });
  };

  const removeNotification = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  if (notifications.length === 0) {
    return null;
  }

  return (
    <div
      className="fixed top-4 right-4 z-50 max-w-md w-full pointer-events-none"
      aria-live="assertive"
      aria-atomic="true"
    >
      <div className="space-y-2 pointer-events-auto">
        {notifications.map((notification) => (
          <ErrorNotification
            key={notification.id}
            {...notification}
            onClose={removeNotification}
          />
        ))}
      </div>
    </div>
  );
}
