// src/hooks/useInfiniteNotifications.ts
// Hook para infinite scroll de notificaciones con React Query

import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useRef } from 'react';
import { apiService } from '@/services/api';
import { useWebSocket } from '@/hooks/useWebSocket';
import { getConnectionState } from '@/services/echo';
import { logger } from '@/services/logger';
import type { NotificationFilters, PaginatedResponse, Notification } from '@/types';

interface UseInfiniteNotificationsOptions {
  filters?: Omit<NotificationFilters, 'page' | 'per_page'>;
  enabled?: boolean;
  pageSize?: number;
  onNewNotification?: (notification: Notification) => void;
}

export function useInfiniteNotifications(options: UseInfiniteNotificationsOptions = {}) {
  const {
    filters = {},
    enabled = true,
    pageSize = 20,
    onNewNotification
  } = options;

  const queryClient = useQueryClient();

  // Query con infinite scroll
  const query = useInfiniteQuery<PaginatedResponse<Notification>>({
    queryKey: ['notifications', 'infinite', filters],
    queryFn: async ({ pageParam = 1 }) => {
      logger.debug('Fetching notifications page', { page: pageParam, pageSize });
      return await apiService.getNotifications({
        ...filters,
        page: pageParam as number,
        per_page: pageSize,
      });
    },
    getNextPageParam: (lastPage: PaginatedResponse<Notification>) => {
      // Si hay más páginas, retornar el número de la siguiente página
      if (lastPage.current_page < lastPage.last_page) {
        return lastPage.current_page + 1;
      }
      return undefined; // No hay más páginas
    },
    initialPageParam: 1,
    enabled,
    staleTime: 30000, // 30 segundos
    refetchOnWindowFocus: true,
  });

  // Handler para nuevas notificaciones vía WebSocket
  const handleNewNotification = useCallback(
    (notification: Notification) => {
      logger.debug('New notification received via WebSocket', {
        notificationId: notification.id,
      });

      // Llamar callback externo si existe
      if (onNewNotification) {
        onNewNotification(notification);
      }

      // Actualizar cache de forma optimista
      queryClient.setQueryData<{
        pages: PaginatedResponse<Notification>[];
        pageParams: unknown[];
      }>(['notifications', 'infinite', filters], (oldData: { pages: PaginatedResponse<Notification>[]; pageParams: unknown[] } | undefined) => {
        if (!oldData) {
          // Si no hay datos, hacer refetch
          queryClient.invalidateQueries({
            queryKey: ['notifications', 'infinite', filters]
          });
          return oldData;
        }

        // Clonar las páginas
        const newPages = [...oldData.pages];
        const firstPage = newPages[0];

        if (!firstPage) return oldData;

        // Verificar si la notificación ya existe
        const exists = firstPage.data.some((n: Notification) => n.id === notification.id);

        if (exists) {
          // Actualizar notificación existente
          newPages[0] = {
            ...firstPage,
            data: firstPage.data.map((n: Notification) =>
              n.id === notification.id ? notification : n
            ),
          };
        } else {
          // Agregar nueva notificación al inicio de la primera página
          newPages[0] = {
            ...firstPage,
            data: [notification, ...firstPage.data],
            total: firstPage.total + 1,
          };
        }

        return {
          ...oldData,
          pages: newPages,
        };
      });

      // Invalidar queries relacionadas
      queryClient.invalidateQueries({
        queryKey: ['notifications'],
        exact: false,
      });
    },
    [filters, onNewNotification, queryClient]
  );

  // Suscribirse a WebSocket
  useWebSocket(handleNewNotification, {
    enabled,
    listenerId: 'use-infinite-notifications',
  });

  // Flatten de todas las notificaciones de todas las páginas
  const notifications = query.data?.pages.flatMap((page: PaginatedResponse<Notification>) => page.data) ?? [];

  // Metadata
  const totalCount = query.data?.pages[0]?.total ?? 0;
  const hasNextPage = query.hasNextPage;
  const isFetchingNextPage = query.isFetchingNextPage;

  return {
    notifications,
    totalCount,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage: query.fetchNextPage,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    connectionState: getConnectionState(),
  };
}

/**
 * Hook para detectar cuando el usuario llega al final de la lista
 * Usado para trigger de infinite scroll
 */
export function useInfiniteScroll(
  callback: () => void,
  options: {
    threshold?: number;
    enabled?: boolean;
  } = {}
) {
  const { threshold = 200, enabled = true } = options;
  const observerRef = useRef<IntersectionObserver | null>(null);
  const elementRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!enabled) return;

    // Crear observer
    observerRef.current = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting) {
          logger.debug('Infinite scroll trigger', { threshold });
          callback();
        }
      },
      {
        rootMargin: `${threshold}px`,
      }
    );

    // Observar elemento
    const element = elementRef.current;
    if (element) {
      observerRef.current.observe(element);
    }

    // Cleanup
    return () => {
      if (observerRef.current && element) {
        observerRef.current.unobserve(element);
      }
    };
  }, [callback, threshold, enabled]);

  return elementRef;
}
