// src/hooks/useWebSocket.ts
// Hook centralizado para WebSocket subscriptions

import { useEffect, useRef, useCallback } from 'react';
import { websocketManager } from '@/services/websocketManager';
import { logger } from '@/services/logger';
import { useAuth } from '@/contexts/AuthContext';
import type { Notification } from '@/types';

/**
 * Hook para suscribirse a eventos de WebSocket de forma centralizada
 *
 * Beneficios:
 * - Deduplicación automática de subscriptions
 * - Limpieza automática al desmontar
 * - Una sola conexión compartida entre componentes
 * - Manejo robusto de reconexiones
 *
 * @param onNotificationCreated - Callback cuando se crea una notificación
 * @param options - Opciones de configuración
 */
export function useWebSocket(
  onNotificationCreated?: (notification: Notification) => void,
  options: {
    enabled?: boolean;
    listenerId?: string;
  } = {}
) {
  const { enabled = true, listenerId: customListenerId } = options;
  const { user } = useAuth();
  const commerceId = user?.commerce_id;

  // Generar listener ID único
  const listenerIdRef = useRef(
    customListenerId || `listener-${Math.random().toString(36).substr(2, 9)}`
  );

  // Mantener callback actualizado sin causar re-suscripciones
  const onNotificationCreatedRef = useRef(onNotificationCreated);
  useEffect(() => {
    onNotificationCreatedRef.current = onNotificationCreated;
  }, [onNotificationCreated]);

  // Wrapper del callback para usar la ref
  const wrappedCallback = useCallback((notification: Notification) => {
    if (onNotificationCreatedRef.current) {
      onNotificationCreatedRef.current(notification);
    }
  }, []);

  // Suscribirse a eventos
  useEffect(() => {
    if (!enabled || !commerceId) {
      logger.debug('WebSocket subscription skipped', {
        enabled,
        hasCommerceId: !!commerceId,
        listenerId: listenerIdRef.current,
      });
      return;
    }

    logger.debug('Setting up WebSocket subscription', {
      commerceId,
      listenerId: listenerIdRef.current,
    });

    // Suscribirse usando el manager
    const unsubscribe = websocketManager.subscribe(
      commerceId,
      'notification.created',
      wrappedCallback,
      listenerIdRef.current
    );

    // Cleanup al desmontar
    return () => {
      logger.debug('Cleaning up WebSocket subscription', {
        commerceId,
        listenerId: listenerIdRef.current,
      });
      unsubscribe();
    };
  }, [enabled, commerceId, wrappedCallback]);

  return {
    listenerId: listenerIdRef.current,
  };
}

/**
 * Hook para obtener métricas de WebSocket (útil para debugging)
 */
export function useWebSocketMetrics() {
  const getMetrics = useCallback(() => {
    return websocketManager.getMetrics();
  }, []);

  return { getMetrics };
}
