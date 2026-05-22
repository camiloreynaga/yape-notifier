/**
 * Contenedor para gestionar múltiples toasts de notificaciones
 * Optimizado con WebSocket Manager centralizado
 */

import { useState, useCallback } from "react";
import NotificationToast from "./NotificationToast";
import type { Notification } from "@/types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { logger } from "@/services/logger";

interface ToastNotification {
  notification: Notification;
  toastId: string; // ID único para el toast
}

interface NotificationToastContainerProps {
  maxToasts?: number;
}

export default function NotificationToastContainer({
  maxToasts = 5,
}: NotificationToastContainerProps) {
  const [toasts, setToasts] = useState<ToastNotification[]>([]);

  const addToast = useCallback(
    (notification: Notification) => {
      const toastId = `toast-${notification.id}-${Date.now()}`;
      const newToast: ToastNotification = {
        notification,
        toastId,
      };

      logger.debug("Adding notification toast", {
        notificationId: notification.id,
        toastId,
      });

      setToasts((prev) => {
        const updated = [newToast, ...prev].slice(0, maxToasts);
        return updated;
      });
    },
    [maxToasts]
  );

  const removeToast = useCallback((toastId: string) => {
    setToasts((prev) => prev.filter((toast) => toast.toastId !== toastId));
  }, []);

  // Suscribirse a WebSocket usando el manager centralizado
  useWebSocket(addToast, {
    listenerId: 'notification-toast-container',
  });

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div
      className="fixed top-4 right-4 z-50 space-y-2"
      role="region"
      aria-label="Notificaciones"
      aria-live="polite"
    >
      {toasts.map((toast) => (
        <NotificationToast
          key={toast.toastId}
          notification={toast.notification}
          onClose={() => removeToast(toast.toastId)}
        />
      ))}
    </div>
  );
}
