/**
 * Hook para obtener notificaciones con polling automático
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { apiService } from '@/services/api';
import type { Notification, NotificationFilters, PaginatedResponse } from '@/types';

interface UseNotificationsOptions {
  filters?: NotificationFilters;
  enabled?: boolean;
  refetchInterval?: number; // en milisegundos
  onNewNotifications?: (newNotifications: Notification[]) => void;
}

interface UseNotificationsReturn {
  notifications: PaginatedResponse<Notification> | null;
  loading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
  isPolling: boolean;
}

export function useNotifications({
  filters = {},
  enabled = true,
  refetchInterval = 10000, // 10 segundos por defecto
  onNewNotifications,
}: UseNotificationsOptions = {}): UseNotificationsReturn {
  const [notifications, setNotifications] = useState<PaginatedResponse<Notification> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  
  const intervalRef = useRef<number | null>(null);
  const lastNotificationIdsRef = useRef<Set<number>>(new Set());
  const isVisibleRef = useRef(true);

  const fetchNotifications = useCallback(async () => {
    // No hacer fetch si el tab no está visible
    if (!isVisibleRef.current || !enabled) {
      return;
    }

    setIsPolling(true);
    setLoading(true);
    try {
      const data = await apiService.getNotifications(filters);
      
      // Detectar nuevas notificaciones comparando con el estado anterior
      setNotifications((prevNotifications) => {
        if (onNewNotifications && prevNotifications) {
          const currentIds = new Set(prevNotifications.data.map(n => n.id));
          const newNotifications = data.data.filter(n => !currentIds.has(n.id));
          
          if (newNotifications.length > 0) {
            onNewNotifications(newNotifications);
          }
        }
        
        return data;
      });
      
      setError(null);
      setLoading(false);
      
      // Actualizar el set de IDs conocidos
      lastNotificationIdsRef.current = new Set(data.data.map(n => n.id));
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Error al cargar notificaciones');
      setError(error);
      setLoading(false);
      console.error('Error fetching notifications:', err);
    } finally {
      setIsPolling(false);
    }
  }, [filters, enabled, onNewNotifications]);

  // Manejar visibilidad del tab
  useEffect(() => {
    const handleVisibilityChange = () => {
      isVisibleRef.current = !document.hidden;
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  // Fetch inicial
  useEffect(() => {
    if (enabled) {
      fetchNotifications();
    }
  }, [enabled, fetchNotifications]);

  // Configurar polling
  useEffect(() => {
    if (!enabled || refetchInterval <= 0) {
      return;
    }

    intervalRef.current = setInterval(() => {
      if (isVisibleRef.current) {
        fetchNotifications();
      }
    }, refetchInterval);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [enabled, refetchInterval, fetchNotifications]);

  const refetch = useCallback(async () => {
    await fetchNotifications();
  }, [fetchNotifications]);

  return {
    notifications,
    loading,
    error,
    refetch,
    isPolling,
  };
}

