/**
 * Hook para manejar el contador de notificaciones no leídas
 */

import { useEffect, useState, useCallback } from 'react';
import { apiService } from '@/services/api';

const STORAGE_KEY = 'yape_notifier_read_notifications';

interface UseUnreadNotificationsReturn {
  unreadCount: number;
  markAsRead: (notificationIds: number[]) => void;
  markAllAsRead: () => void;
  refresh: () => Promise<void>;
}

export function useUnreadNotifications(): UseUnreadNotificationsReturn {
  const [unreadCount, setUnreadCount] = useState(0);
  const [readNotificationIds, setReadNotificationIds] = useState<Set<number>>(new Set());

  // Cargar IDs leídos desde localStorage
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const ids = JSON.parse(stored) as number[];
        setReadNotificationIds(new Set(ids));
      }
    } catch (error) {
      console.error('Error loading read notifications:', error);
    }
  }, []);

  // Guardar IDs leídos en localStorage
  const saveReadNotifications = useCallback((ids: Set<number>) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(ids)));
      setReadNotificationIds(ids);
    } catch (error) {
      console.error('Error saving read notifications:', error);
    }
  }, []);

  // Calcular notificaciones no leídas
  const calculateUnreadCount = useCallback(async () => {
    try {
      // Obtener notificaciones pendientes (las más recientes)
      const response = await apiService.getNotifications({
        status: 'pending',
        per_page: 100,
        page: 1,
      });

      // Filtrar las que no están marcadas como leídas
      const unread = response.data.filter(
        (notification) => !readNotificationIds.has(notification.id)
      );

      setUnreadCount(unread.length);
    } catch (error) {
      console.error('Error calculating unread count:', error);
    }
  }, [readNotificationIds]);

  // Refrescar contador
  const refresh = useCallback(async () => {
    await calculateUnreadCount();
  }, [calculateUnreadCount]);

  // Marcar notificaciones como leídas
  const markAsRead = useCallback(
    (notificationIds: number[]) => {
      const newReadIds = new Set(readNotificationIds);
      notificationIds.forEach((id) => newReadIds.add(id));
      saveReadNotifications(newReadIds);
      // Recalcular contador
      calculateUnreadCount();
    },
    [readNotificationIds, saveReadNotifications, calculateUnreadCount]
  );

  // Marcar todas como leídas
  const markAllAsRead = useCallback(async () => {
    try {
      const response = await apiService.getNotifications({
        status: 'pending',
        per_page: 1000,
        page: 1,
      });

      const allIds = response.data.map((n) => n.id);
      const newReadIds = new Set([...readNotificationIds, ...allIds]);
      saveReadNotifications(newReadIds);
      setUnreadCount(0);
    } catch (error) {
      console.error('Error marking all as read:', error);
    }
  }, [readNotificationIds, saveReadNotifications]);

  // Refrescar contador periódicamente
  useEffect(() => {
    calculateUnreadCount();
    const interval = setInterval(() => {
      calculateUnreadCount();
    }, 10000); // Cada 10 segundos

    return () => clearInterval(interval);
  }, [calculateUnreadCount]);

  return {
    unreadCount,
    markAsRead,
    markAllAsRead,
    refresh,
  };
}

