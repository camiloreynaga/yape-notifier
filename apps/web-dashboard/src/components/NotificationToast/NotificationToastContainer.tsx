/**
 * Contenedor para gestionar múltiples toasts de notificaciones
 * Actualizado para usar WebSockets directamente
 */

import { useState, useEffect, useCallback } from "react";
import NotificationToast from "./NotificationToast";
import type { Notification } from "@/types";
import { echo } from "@/services/echo";
import { useAuth } from "@/contexts/AuthContext";

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
  const { user } = useAuth();
  const commerceId = user?.commerce_id;

  const addToast = useCallback(
    (notification: Notification) => {
      const toastId = `toast-${notification.id}-${Date.now()}`;
      const newToast: ToastNotification = {
        notification,
        toastId,
      };

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

  // Escuchar eventos WebSocket para nuevas notificaciones
  useEffect(() => {
    if (!commerceId || !echo) return;

    const channel = echo.private(`commerce.${commerceId}`);

    // Escuchar evento de notificación creada
    channel.listen(
      ".notification.created",
      (data: { notification: Notification }) => {
        // eslint-disable-next-line no-console
        console.log("🔔 Nueva notificación recibida vía WebSocket para toast:", data.notification);
        addToast(data.notification);
      }
    );

    return () => {
      channel.stopListening(".notification.created");
    };
  }, [commerceId, addToast]);

  // Exponer addToast globalmente (para compatibilidad con código existente)
  useEffect(() => {
    (window as Window & {
      addNotificationToast?: (notification: Notification) => void;
    }).addNotificationToast = addToast;

    return () => {
      delete (window as Window & {
        addNotificationToast?: (notification: Notification) => void;
      }).addNotificationToast;
    };
  }, [addToast]);

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
