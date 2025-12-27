// src/hooks/useUnreadNotifications.ts (actualizado para usar WebSockets)
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useCallback, useState } from "react";
import { apiService } from "@/services/api";
import { echo } from "@/services/echo";
import { useAuth } from "@/contexts/AuthContext";

const STORAGE_KEY = "yape_notifier_read_notifications";

interface UseUnreadNotificationsReturn {
  unreadCount: number;
  markAsRead: (notificationIds: number[]) => void;
  markAllAsRead: () => void;
  refresh: () => Promise<void>;
}

export function useUnreadNotifications(): UseUnreadNotificationsReturn {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const commerceId = user?.commerce_id;

  // Query inicial para obtener contador
  const query = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: async () => {
      const response = await apiService.getNotifications({
        per_page: 1,
        page: 1,
        status: "pending",
      });
      return response.total; // Total de notificaciones pendientes
    },
    enabled: !!commerceId,
    staleTime: Infinity, // No hacer refetch automático, WebSocket actualizará
  });

  // Escuchar eventos WebSocket para actualizar contador
  useEffect(() => {
    if (!commerceId || !echo) return;

    const channel = echo.private(`commerce.${commerceId}`);

    channel.listen(".notification.created", () => {
      // Incrementar contador cuando llega nueva notificación
      queryClient.setQueryData<number>(
        ["notifications", "unread-count"],
        (oldCount: number | undefined) => (oldCount ?? 0) + 1
      );
    });

    return () => {
      channel.stopListening(".notification.created");
    };
  }, [commerceId, queryClient]);

  // Cargar IDs leídos desde localStorage
  const [readNotificationIds, setReadNotificationIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        setReadNotificationIds(new Set(JSON.parse(stored) as number[]));
      }
    } catch (error) {
      console.error("Error loading read notifications:", error);
    }
  }, []);

  // Guardar IDs leídos en localStorage
  const saveReadNotifications = useCallback((ids: Set<number>) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(ids)));
      setReadNotificationIds(ids);
    } catch (error) {
      console.error("Error saving read notifications:", error);
    }
  }, []);

  // Marcar notificaciones como leídas
  const markAsRead = useCallback(
    (notificationIds: number[]) => {
      const newReadIds = new Set(readNotificationIds);
      notificationIds.forEach((id) => newReadIds.add(id));
      saveReadNotifications(newReadIds);
      // Recalcular contador
      queryClient.invalidateQueries({ queryKey: ["notifications", "unread-count"] });
    },
    [readNotificationIds, saveReadNotifications, queryClient]
  );

  // Marcar todas como leídas
  const markAllAsRead = useCallback(async () => {
    try {
      const response = await apiService.getNotifications({
        status: "pending",
        per_page: 1000,
        page: 1,
      });

      const allIds = response.data.map((n) => n.id);
      const newReadIds = new Set([...readNotificationIds, ...allIds]);
      saveReadNotifications(newReadIds);
      queryClient.setQueryData<number>(["notifications", "unread-count"], 0);
    } catch (error) {
      console.error("Error marking all as read:", error);
    }
  }, [readNotificationIds, saveReadNotifications, queryClient]);

  // Refrescar contador
  const refresh = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ["notifications", "unread-count"] });
  }, [queryClient]);

  return {
    unreadCount: query.data ?? 0,
    markAsRead,
    markAllAsRead,
    refresh,
  };
}
