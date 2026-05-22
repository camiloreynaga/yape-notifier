// src/hooks/useNotifications.ts (optimizado con WebSocket Manager centralizado)
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { apiService } from "@/services/api";
import { getConnectionState } from "@/services/echo";
import { useWebSocket } from "@/hooks/useWebSocket";
import type {
  NotificationFilters,
  PaginatedResponse,
  Notification,
} from "@/types";

interface UseNotificationsOptions {
  filters?: NotificationFilters;
  enabled?: boolean;
  onNewNotification?: (notification: Notification) => void;
}

export function useNotifications(options: UseNotificationsOptions = {}) {
  const { filters = {}, enabled = true, onNewNotification } = options;
  const queryClient = useQueryClient();

  // Query inicial para cargar notificaciones
  const query = useQuery<PaginatedResponse<Notification>>({
    queryKey: ["notifications", filters],
    queryFn: () => apiService.getNotifications(filters),
    enabled: enabled,
    staleTime: 30000, // 30 segundos (WebSockets actualizará antes)
    refetchOnWindowFocus: true, // Refetch cuando ventana recupera foco
  });

  // Handler para nuevas notificaciones vía WebSocket
  const handleNewNotification = useCallback(
    (notification: Notification) => {
      // Llamar callback externo si existe
      if (onNewNotification) {
        onNewNotification(notification);
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
    },
    [filters, onNewNotification, queryClient]
  );

  // Suscribirse a WebSocket usando el manager centralizado
  useWebSocket(handleNewNotification, {
    enabled,
    listenerId: 'use-notifications',
  });

  return {
    ...query,
    notifications: query.data || null,
    loading: query.isLoading,
    error: query.error,
    refetch: query.refetch,
    connectionState: getConnectionState(),
  };
}
