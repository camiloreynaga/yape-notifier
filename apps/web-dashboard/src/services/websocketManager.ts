// src/services/websocketManager.ts
// Manager centralizado para subscripciones WebSocket con deduplicación y heartbeat

import { echo, getConnectionState } from './echo';
import { logger } from './logger';
import type { Notification } from '@/types';

/**
 * Tipos de eventos que pueden emitirse
 */
type WebSocketEventType = 'notification.created' | 'notification.updated' | 'device.status';

/**
 * Callback para eventos de notificaciones
 */
type NotificationCallback = (notification: Notification) => void;

/**
 * Información de un canal suscrito
 */
interface ChannelSubscription {
  channelName: string;
  channel: ReturnType<typeof echo.private> | null;
  listeners: Map<string, Set<NotificationCallback>>;
  subscriberCount: number;
  lastActivity: number;
}

/**
 * WebSocket Manager - Gestiona suscripciones de forma centralizada
 *
 * Beneficios:
 * - Una sola conexión por canal (sin duplicados)
 * - Múltiples listeners pueden escuchar el mismo canal
 * - Limpieza automática cuando no hay listeners
 * - Heartbeat para detectar conexiones muertas
 * - Métricas de uso
 */
class WebSocketManager {
  private subscriptions = new Map<string, ChannelSubscription>();
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
  private heartbeatIntervalMs = 30000; // 30 segundos
  private maxInactivityMs = 120000; // 2 minutos

  constructor() {
    this.startHeartbeat();
    this.setupGlobalEventListeners();
  }

  /**
   * Suscribirse a un canal de commerce
   * Retorna una función de cleanup para desuscribirse
   */
  subscribe(
    commerceId: number,
    eventType: WebSocketEventType,
    callback: NotificationCallback,
    listenerId: string
  ): () => void {
    const channelName = `commerce.${commerceId}`;

    logger.debug('WebSocket subscription requested', {
      channelName,
      eventType,
      listenerId,
    });

    // Obtener o crear subscription
    let subscription = this.subscriptions.get(channelName);

    if (!subscription) {
      // Primera subscription a este canal - crear canal
      logger.info('Creating new WebSocket channel', { channelName });

      subscription = {
        channelName,
        channel: null,
        listeners: new Map(),
        subscriberCount: 0,
        lastActivity: Date.now(),
      };

      this.subscriptions.set(channelName, subscription);
      this.connectChannel(subscription);
    }

    // Agregar listener
    if (!subscription.listeners.has(eventType)) {
      subscription.listeners.set(eventType, new Set());
    }

    const eventListeners = subscription.listeners.get(eventType)!;
    eventListeners.add(callback);
    subscription.subscriberCount++;
    subscription.lastActivity = Date.now();

    logger.debug('WebSocket listener added', {
      channelName,
      eventType,
      listenerId,
      totalSubscribers: subscription.subscriberCount,
      eventListeners: eventListeners.size,
    });

    // Retornar función de cleanup
    return () => this.unsubscribe(channelName, eventType, callback, listenerId);
  }

  /**
   * Desuscribirse de un canal
   */
  private unsubscribe(
    channelName: string,
    eventType: WebSocketEventType,
    callback: NotificationCallback,
    listenerId: string
  ): void {
    const subscription = this.subscriptions.get(channelName);
    if (!subscription) return;

    logger.debug('WebSocket unsubscribe requested', {
      channelName,
      eventType,
      listenerId,
    });

    // Remover listener
    const eventListeners = subscription.listeners.get(eventType);
    if (eventListeners) {
      eventListeners.delete(callback);

      // Si no quedan listeners para este evento, remover el Set
      if (eventListeners.size === 0) {
        subscription.listeners.delete(eventType);
      }
    }

    subscription.subscriberCount--;

    logger.debug('WebSocket listener removed', {
      channelName,
      eventType,
      listenerId,
      remainingSubscribers: subscription.subscriberCount,
    });

    // Si no quedan listeners, desconectar canal
    if (subscription.subscriberCount === 0) {
      this.disconnectChannel(channelName);
    }
  }

  /**
   * Conectar a un canal
   */
  private connectChannel(subscription: ChannelSubscription): void {
    if (subscription.channel) {
      logger.warn('Channel already connected', { channelName: subscription.channelName });
      return;
    }

    try {
      const channel = echo.private(subscription.channelName);
      subscription.channel = channel;

      // Escuchar eventos relevantes
      // Notification created
      channel.listen('.notification.created', (data: { notification: Notification }) => {
        this.handleEvent(subscription, 'notification.created', data.notification);
      });

      // Manejar errores de canal
      channel.error((error: Error) => {
        logger.error('WebSocket channel error', error, {
          channelName: subscription.channelName,
        });

        // Si es error de autenticación, notificar
        const errorMessage = error?.message || String(error);
        if (
          errorMessage.includes('401') ||
          errorMessage.includes('403') ||
          errorMessage.includes('authentication')
        ) {
          window.dispatchEvent(new CustomEvent('echo:auth-error', { detail: error }));
        }
      });

      logger.info('WebSocket channel connected', {
        channelName: subscription.channelName,
        connectionState: getConnectionState(),
      });
    } catch (error) {
      logger.error('Failed to connect WebSocket channel', error as Error, {
        channelName: subscription.channelName,
      });
    }
  }

  /**
   * Desconectar de un canal
   */
  private disconnectChannel(channelName: string): void {
    const subscription = this.subscriptions.get(channelName);
    if (!subscription) return;

    logger.info('Disconnecting WebSocket channel', {
      channelName,
      reason: 'no_subscribers',
    });

    try {
      if (subscription.channel) {
        subscription.channel.stopListening('.notification.created');
        echo.leave(channelName);
        subscription.channel = null;
      }

      this.subscriptions.delete(channelName);
    } catch (error) {
      logger.error('Error disconnecting channel', error as Error, { channelName });
    }
  }

  /**
   * Manejar evento recibido y notificar a listeners
   */
  private handleEvent(
    subscription: ChannelSubscription,
    eventType: WebSocketEventType,
    data: Notification
  ): void {
    subscription.lastActivity = Date.now();

    const listeners = subscription.listeners.get(eventType);
    if (!listeners || listeners.size === 0) {
      logger.debug('No listeners for event', { eventType, channelName: subscription.channelName });
      return;
    }

    logger.debug('Broadcasting event to listeners', {
      eventType,
      channelName: subscription.channelName,
      listenerCount: listeners.size,
      notificationId: data.id,
    });

    // Notificar a todos los listeners
    listeners.forEach((callback) => {
      try {
        callback(data);
      } catch (error) {
        logger.error('Error in listener callback', error as Error, {
          eventType,
          channelName: subscription.channelName,
        });
      }
    });
  }

  /**
   * Heartbeat - Verificar conexiones y limpiar inactivas
   */
  private startHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    this.heartbeatInterval = setInterval(() => {
      this.performHeartbeat();
    }, this.heartbeatIntervalMs);

    logger.info('WebSocket heartbeat started', {
      intervalMs: this.heartbeatIntervalMs,
    });
  }

  /**
   * Ejecutar heartbeat check
   */
  private performHeartbeat(): void {
    const now = Date.now();
    const connectionState = getConnectionState();

    logger.debug('WebSocket heartbeat check', {
      activeChannels: this.subscriptions.size,
      connectionState,
    });

    // Verificar estado de conexión
    if (connectionState !== 'connected') {
      logger.warn('WebSocket not connected during heartbeat', {
        state: connectionState,
        activeChannels: this.subscriptions.size,
      });
      return;
    }

    // Limpiar canales inactivos (sin actividad reciente)
    const inactiveChannels: string[] = [];

    this.subscriptions.forEach((subscription, channelName) => {
      const inactiveTime = now - subscription.lastActivity;

      if (inactiveTime > this.maxInactivityMs && subscription.subscriberCount === 0) {
        inactiveChannels.push(channelName);
      }
    });

    // Desconectar canales inactivos
    if (inactiveChannels.length > 0) {
      logger.info('Cleaning up inactive WebSocket channels', {
        count: inactiveChannels.length,
        channels: inactiveChannels,
      });

      inactiveChannels.forEach((channelName) => {
        this.disconnectChannel(channelName);
      });
    }

    // Emitir evento de heartbeat para monitoreo
    window.dispatchEvent(
      new CustomEvent('websocket:heartbeat', {
        detail: {
          activeChannels: this.subscriptions.size,
          connectionState,
          timestamp: now,
        },
      })
    );
  }

  /**
   * Setup global event listeners
   */
  private setupGlobalEventListeners(): void {
    // Reconectar canales cuando WebSocket se reconecta
    window.addEventListener('echo:connected', () => {
      logger.info('WebSocket reconnected, resubscribing channels');

      this.subscriptions.forEach((subscription) => {
        if (!subscription.channel && subscription.subscriberCount > 0) {
          logger.info('Reconnecting channel', { channelName: subscription.channelName });
          this.connectChannel(subscription);
        }
      });
    });

    // Log cuando se desconecta
    window.addEventListener('echo:disconnected', () => {
      logger.warn('WebSocket disconnected', {
        activeChannels: this.subscriptions.size,
      });
    });
  }

  /**
   * Obtener métricas del manager
   */
  getMetrics() {
    const metrics = {
      totalChannels: this.subscriptions.size,
      totalSubscribers: 0,
      channels: [] as Array<{
        name: string;
        subscribers: number;
        eventTypes: string[];
        lastActivity: Date;
        connected: boolean;
      }>,
      connectionState: getConnectionState(),
    };

    this.subscriptions.forEach((subscription) => {
      metrics.totalSubscribers += subscription.subscriberCount;
      metrics.channels.push({
        name: subscription.channelName,
        subscribers: subscription.subscriberCount,
        eventTypes: Array.from(subscription.listeners.keys()),
        lastActivity: new Date(subscription.lastActivity),
        connected: !!subscription.channel,
      });
    });

    return metrics;
  }

  /**
   * Cleanup - desconectar todos los canales
   */
  cleanup(): void {
    logger.info('WebSocket manager cleanup', {
      activeChannels: this.subscriptions.size,
    });

    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }

    this.subscriptions.forEach((subscription) => {
      if (subscription.channel) {
        try {
          subscription.channel.stopListening('.notification.created');
          echo.leave(subscription.channelName);
        } catch (error) {
          logger.error('Error during cleanup', error as Error, {
            channelName: subscription.channelName,
          });
        }
      }
    });

    this.subscriptions.clear();
  }
}

// Singleton instance
export const websocketManager = new WebSocketManager();

// Cleanup on window unload
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    websocketManager.cleanup();
  });
}
