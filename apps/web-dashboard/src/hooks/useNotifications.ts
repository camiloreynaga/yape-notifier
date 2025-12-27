// src/hooks/useNotifications.ts (modificado completamente)
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { apiService } from "@/services/api";
import { echo, getConnectionState } from "@/services/echo";
import type {
  NotificationFilters,
  PaginatedResponse,
  Notification,
} from "@/types";
import { useAuth } from "@/contexts/AuthContext";

interface UseNotificationsOptions {
  filters?: NotificationFilters;
  enabled?: boolean;
  onNewNotification?: (notification: Notification) => void;
}

export function useNotifications(options: UseNotificationsOptions = {}) {
  const { filters = {}, enabled = true, onNewNotification } = options;
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const commerceId = user?.commerce_id;

  const channelRef = useRef<ReturnType<typeof echo.private> | null>(null);
  const onNewNotificationRef = useRef(onNewNotification);

  // Actualizar ref cuando cambia el callback
  useEffect(() => {
    onNewNotificationRef.current = onNewNotification;
  }, [onNewNotification]);

  // Query inicial para cargar notificaciones
  const query = useQuery<PaginatedResponse<Notification>>({
    queryKey: ["notifications", filters],
    queryFn: () => apiService.getNotifications(filters),
    enabled: enabled && !!commerceId,
    staleTime: 30000, // 30 segundos (WebSockets actualizará antes)
    refetchOnWindowFocus: true, // Refetch cuando ventana recupera foco
  });

  // Escuchar eventos de WebSocket
  useEffect(() => {
    if (!commerceId || !enabled || !echo) {
      return;
    }

    // Verificar estado de conexión
    const connectionState = getConnectionState();
    if (connectionState !== "connected") {
      console.warn("⚠️ WebSocket no está conectado, estado:", connectionState);
    }

    // Suscribirse al canal privado
    const channelName = `commerce.${commerceId}`;
    channelRef.current = echo.private(channelName);

    // Escuchar evento de notificación creada
    // IMPORTANTE: El backend envía el evento como 'notification.created'
    channelRef.current.listen(
      ".notification.created",
      (data: { notification: Notification }) => {
        const notification = data.notification;
        // eslint-disable-next-line no-console
        console.log(
          "🔔 Nueva notificación recibida vía WebSocket:",
          notification
        );

        // Llamar callback si existe
        if (onNewNotificationRef.current) {
          onNewNotificationRef.current(notification);
        }

        // Actualizar cache de React Query de forma optimista
        queryClient.setQueryData<PaginatedResponse<Notification>>(
          ["notifications", filters],
          (oldData: PaginatedResponse<Notification> | undefined) => {
            if (!oldData) {
              // Si no hay datos, hacer refetch
              queryClient.invalidateQueries({ queryKey: ["notifications"] });
              return oldData;
            }

            // Verificar si la notificación ya existe (evitar duplicados)
            const exists = oldData.data.some((n: Notification) => n.id === notification.id);
            if (exists) {
              // Actualizar notificación existente si cambió
              return {
                ...oldData,
                data: oldData.data.map((n: Notification) =>
                  n.id === notification.id ? notification : n
                ),
              };
            }

            // Agregar nueva notificación al inicio
            return {
              ...oldData,
              data: [notification, ...oldData.data],
              total: oldData.total + 1,
            };
          }
        );

        // Invalidar queries relacionadas para mantener consistencia
        queryClient.invalidateQueries({
          queryKey: ["notifications"],
          exact: false, // Invalidar todas las variaciones de filtros
        });
      }
    );

    // Manejar errores de suscripción
    channelRef.current.error((error: Error) => {
      console.error("❌ Error en canal WebSocket:", error);
      
      // Si es error de autenticación, notificar
      const errorMessage = error?.message || String(error);
      if (errorMessage.includes("401") || errorMessage.includes("403") || errorMessage.includes("authentication")) {
        window.dispatchEvent(new CustomEvent("echo:auth-error", { detail: error }));
      }
    });

    // Limpiar suscripción al desmontar
    return () => {
      if (channelRef.current) {
        channelRef.current.stopListening(".notification.created");
        echo.leave(channelName);
        channelRef.current = null;
      }
    };
  }, [commerceId, enabled, filters, queryClient]);

  // Escuchar cambios en estado de conexión
  useEffect(() => {
    const handleConnected = () => {
      // eslint-disable-next-line no-console
      console.log("✅ WebSocket reconectado, refrescando notificaciones...");
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    };

    const handleDisconnected = () => {
      console.warn("⚠️ WebSocket desconectado");
    };

    window.addEventListener("echo:connected", handleConnected);
    window.addEventListener("echo:disconnected", handleDisconnected);

    return () => {
      window.removeEventListener("echo:connected", handleConnected);
      window.removeEventListener("echo:disconnected", handleDisconnected);
    };
  }, [queryClient]);

  return {
    ...query,
    notifications: query.data || null,
    loading: query.isLoading,
    error: query.error,
    refetch: query.refetch,
    connectionState: getConnectionState(),
  };
}
