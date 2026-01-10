// src/services/requestQueue.ts
// Sistema de cola para requests fallidos con reintentos inteligentes

import { logger } from './logger';

export interface QueuedRequest<T = unknown> {
  id: string;
  fn: () => Promise<T>;
  retries: number;
  maxRetries: number;
  priority: number;
  timestamp: number;
  name: string;
  onSuccess?: (result: T) => void;
  onError?: (error: Error) => void;
}

export class RequestQueue {
  private queue: QueuedRequest<unknown>[] = [];
  private processing = false;
  private maxQueueSize = 50;
  private processingDelay = 1000; // 1 segundo entre requests

  /**
   * Agrega un request a la cola
   */
  enqueue<T>(
    fn: () => Promise<T>,
    options: {
      name: string;
      maxRetries?: number;
      priority?: number;
      onSuccess?: (result: T) => void;
      onError?: (error: Error) => void;
    }
  ): string {
    const id = `${options.name}-${Date.now()}-${Math.random()}`;

    // Verificar tamaño de la cola
    if (this.queue.length >= this.maxQueueSize) {
      logger.warn('Request queue is full, removing oldest item', {
        queueSize: this.queue.length,
        maxSize: this.maxQueueSize,
      });
      this.queue.shift(); // Remover el más antiguo
    }

    const request: QueuedRequest<unknown> = {
      id,
      fn: fn as () => Promise<unknown>,
      retries: 0,
      maxRetries: options.maxRetries ?? 3,
      priority: options.priority ?? 0,
      timestamp: Date.now(),
      name: options.name,
      onSuccess: options.onSuccess ? ((result: unknown) => options.onSuccess!(result as T)) : undefined,
      onError: options.onError,
    };

    this.queue.push(request);

    // Ordenar por prioridad (mayor prioridad primero)
    this.queue.sort((a, b) => b.priority - a.priority);

    logger.debug(`Request queued: ${options.name}`, {
      id,
      queueSize: this.queue.length,
      priority: request.priority,
    });

    // Iniciar procesamiento si no está en curso
    if (!this.processing) {
      this.processQueue();
    }

    return id;
  }

  /**
   * Procesa la cola de requests
   */
  private async processQueue() {
    if (this.processing || this.queue.length === 0) {
      return;
    }

    this.processing = true;

    while (this.queue.length > 0) {
      const request = this.queue[0];

      try {
        logger.debug(`Processing queued request: ${request.name}`, {
          id: request.id,
          retries: request.retries,
          maxRetries: request.maxRetries,
        });

        // Ejecutar el request
        const result = await request.fn();

        // Éxito - remover de la cola
        this.queue.shift();

        logger.info(`Queued request succeeded: ${request.name}`, {
          id: request.id,
        });

        // Llamar callback de éxito
        if (request.onSuccess) {
          request.onSuccess(result);
        }

        // Notificar éxito
        this.notifySuccess(request.name);
      } catch (error) {
        // Error - incrementar retries
        request.retries++;

        logger.warn(`Queued request failed: ${request.name}`, {
          id: request.id,
          retries: request.retries,
          maxRetries: request.maxRetries,
          error: error instanceof Error ? error.message : String(error),
        });

        if (request.retries >= request.maxRetries) {
          // Máximo de reintentos alcanzado - remover de la cola
          this.queue.shift();

          logger.error(
            `Queued request exhausted retries: ${request.name}`,
            error as Error,
            {
              id: request.id,
              retries: request.retries,
            }
          );

          // Llamar callback de error
          if (request.onError) {
            request.onError(error as Error);
          }

          // Notificar fallo permanente
          this.notifyFailure(request.name);
        } else {
          // Mover al final de la cola para reintento posterior
          this.queue.shift();
          this.queue.push(request);
        }
      }

      // Delay entre requests para evitar sobrecargar
      await this.delay(this.processingDelay);
    }

    this.processing = false;
  }

  /**
   * Delay helper
   */
  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /**
   * Notifica éxito de un request en cola
   */
  private notifySuccess(name: string) {
    window.dispatchEvent(
      new CustomEvent('request-queue:success', {
        detail: { name },
      })
    );
  }

  /**
   * Notifica fallo permanente de un request en cola
   */
  private notifyFailure(name: string) {
    window.dispatchEvent(
      new CustomEvent('request-queue:failure', {
        detail: { name },
      })
    );
  }

  /**
   * Obtiene el estado actual de la cola
   */
  getQueueState() {
    return {
      size: this.queue.length,
      processing: this.processing,
      requests: this.queue.map((r) => ({
        id: r.id,
        name: r.name,
        retries: r.retries,
        maxRetries: r.maxRetries,
        priority: r.priority,
        age: Date.now() - r.timestamp,
      })),
    };
  }

  /**
   * Limpia la cola
   */
  clear() {
    const clearedCount = this.queue.length;
    this.queue = [];
    this.processing = false;

    logger.info(`Request queue cleared`, {
      clearedCount,
    });
  }

  /**
   * Remueve un request específico de la cola
   */
  remove(id: string): boolean {
    const index = this.queue.findIndex((r) => r.id === id);
    if (index !== -1) {
      const removed = this.queue.splice(index, 1)[0];
      logger.debug(`Request removed from queue: ${removed.name}`, {
        id,
      });
      return true;
    }
    return false;
  }

  /**
   * Obtiene el tamaño actual de la cola
   */
  get size(): number {
    return this.queue.length;
  }

  /**
   * Verifica si la cola está procesando
   */
  get isProcessing(): boolean {
    return this.processing;
  }
}

// Singleton instance
export const requestQueue = new RequestQueue();
