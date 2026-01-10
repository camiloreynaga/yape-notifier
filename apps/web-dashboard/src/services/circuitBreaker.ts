// src/services/circuitBreaker.ts
// Implementación del patrón Circuit Breaker para prevenir cascadas de errores

import { logger } from './logger';

/**
 * Estados del Circuit Breaker:
 * - CLOSED: Funcionando normalmente, todas las requests pasan
 * - OPEN: Demasiados errores, todas las requests son rechazadas inmediatamente
 * - HALF_OPEN: Probando si el servicio se recuperó, permite algunas requests
 */
export enum CircuitState {
  CLOSED = 'CLOSED',
  OPEN = 'OPEN',
  HALF_OPEN = 'HALF_OPEN',
}

export interface CircuitBreakerConfig {
  /** Nombre del circuit breaker (para logging) */
  name: string;
  /** Threshold de errores consecutivos antes de abrir el circuito */
  failureThreshold: number;
  /** Tiempo en ms antes de intentar recuperación (pasar a HALF_OPEN) */
  resetTimeout: number;
  /** Número de requests exitosos en HALF_OPEN antes de cerrar el circuito */
  successThreshold: number;
  /** Timeout de requests en ms */
  timeout?: number;
}

export class CircuitBreaker {
  private state: CircuitState = CircuitState.CLOSED;
  private failureCount = 0;
  private successCount = 0;
  private nextAttempt = Date.now();
  private readonly config: CircuitBreakerConfig;

  constructor(config: CircuitBreakerConfig) {
    this.config = {
      failureThreshold: 5,
      resetTimeout: 60000, // 1 minuto
      successThreshold: 2,
      ...config,
    };

    logger.info(`Circuit Breaker initialized: ${this.config.name}`, {
      config: this.config,
    });
  }

  /**
   * Ejecuta una función con protección de circuit breaker
   * @param fn - Función async a ejecutar
   * @returns Resultado de la función o lanza error si el circuito está abierto
   */
  async execute<T>(fn: () => Promise<T>): Promise<T> {
    // Verificar estado del circuito
    if (this.state === CircuitState.OPEN) {
      // Verificar si es momento de intentar recuperación
      if (Date.now() < this.nextAttempt) {
        const error = new Error(
          `Circuit breaker is OPEN for ${this.config.name}. Service unavailable.`
        );
        logger.warn(`Circuit breaker rejected request: ${this.config.name}`, {
          state: this.state,
          nextAttempt: new Date(this.nextAttempt).toISOString(),
        });
        throw error;
      }

      // Pasar a HALF_OPEN para probar
      this.state = CircuitState.HALF_OPEN;
      this.successCount = 0;
      logger.info(`Circuit breaker transitioning to HALF_OPEN: ${this.config.name}`);
    }

    try {
      // Ejecutar con timeout si está configurado
      const result = this.config.timeout
        ? await this.executeWithTimeout(fn, this.config.timeout)
        : await fn();

      // Éxito
      this.onSuccess();
      return result;
    } catch (error) {
      // Error
      this.onFailure();
      throw error;
    }
  }

  /**
   * Ejecuta una función con timeout
   */
  private async executeWithTimeout<T>(
    fn: () => Promise<T>,
    timeout: number
  ): Promise<T> {
    return Promise.race([
      fn(),
      new Promise<T>((_, reject) =>
        setTimeout(
          () => reject(new Error(`Request timeout after ${timeout}ms`)),
          timeout
        )
      ),
    ]);
  }

  /**
   * Maneja éxito de request
   */
  private onSuccess() {
    this.failureCount = 0;

    if (this.state === CircuitState.HALF_OPEN) {
      this.successCount++;

      if (this.successCount >= this.config.successThreshold) {
        // Cerrar circuito - servicio recuperado
        this.state = CircuitState.CLOSED;
        this.successCount = 0;
        logger.info(`Circuit breaker closed: ${this.config.name}`, {
          state: this.state,
        });

        // Notificar recuperación
        this.notifyRecovery();
      }
    }
  }

  /**
   * Maneja fallo de request
   */
  private onFailure() {
    this.failureCount++;

    logger.warn(`Circuit breaker failure count increased: ${this.config.name}`, {
      failureCount: this.failureCount,
      threshold: this.config.failureThreshold,
      state: this.state,
    });

    if (this.state === CircuitState.HALF_OPEN) {
      // En HALF_OPEN, cualquier fallo abre el circuito de nuevo
      this.openCircuit();
    } else if (this.failureCount >= this.config.failureThreshold) {
      // En CLOSED, abrir si se alcanza el threshold
      this.openCircuit();
    }
  }

  /**
   * Abre el circuito
   */
  private openCircuit() {
    this.state = CircuitState.OPEN;
    this.nextAttempt = Date.now() + this.config.resetTimeout;

    logger.error(`Circuit breaker opened: ${this.config.name}`, undefined, {
      failureCount: this.failureCount,
      resetTimeout: this.config.resetTimeout,
      nextAttempt: new Date(this.nextAttempt).toISOString(),
    });

    // Notificar que el servicio está caído
    this.notifyCircuitOpen();
  }

  /**
   * Notifica que el circuito se abrió (servicio caído)
   */
  private notifyCircuitOpen() {
    window.dispatchEvent(
      new CustomEvent('circuit-breaker:open', {
        detail: {
          name: this.config.name,
          nextAttempt: this.nextAttempt,
        },
      })
    );
  }

  /**
   * Notifica que el servicio se recuperó
   */
  private notifyRecovery() {
    window.dispatchEvent(
      new CustomEvent('circuit-breaker:recovered', {
        detail: {
          name: this.config.name,
        },
      })
    );
  }

  /**
   * Resetea manualmente el circuit breaker
   */
  reset() {
    this.state = CircuitState.CLOSED;
    this.failureCount = 0;
    this.successCount = 0;
    this.nextAttempt = Date.now();

    logger.info(`Circuit breaker manually reset: ${this.config.name}`);
  }

  /**
   * Obtiene el estado actual del circuito
   */
  getState() {
    return {
      state: this.state,
      failureCount: this.failureCount,
      successCount: this.successCount,
      nextAttempt: this.nextAttempt,
    };
  }

  /**
   * Verifica si el circuito está disponible para requests
   */
  isAvailable(): boolean {
    return this.state === CircuitState.CLOSED ||
           (this.state === CircuitState.HALF_OPEN && Date.now() >= this.nextAttempt);
  }
}

/**
 * Manager para múltiples circuit breakers
 */
class CircuitBreakerManager {
  private breakers = new Map<string, CircuitBreaker>();

  /**
   * Crea o obtiene un circuit breaker
   */
  getOrCreate(config: CircuitBreakerConfig): CircuitBreaker {
    if (!this.breakers.has(config.name)) {
      this.breakers.set(config.name, new CircuitBreaker(config));
    }
    return this.breakers.get(config.name)!;
  }

  /**
   * Obtiene un circuit breaker existente
   */
  get(name: string): CircuitBreaker | undefined {
    return this.breakers.get(name);
  }

  /**
   * Resetea todos los circuit breakers
   */
  resetAll() {
    logger.info('Resetting all circuit breakers');
    this.breakers.forEach((breaker) => breaker.reset());
  }

  /**
   * Obtiene el estado de todos los circuit breakers
   */
  getAllStates() {
    const states: Record<string, ReturnType<CircuitBreaker['getState']>> = {};
    this.breakers.forEach((breaker, name) => {
      states[name] = breaker.getState();
    });
    return states;
  }

  /**
   * Verifica si algún circuit breaker está abierto
   */
  hasOpenCircuits(): boolean {
    for (const breaker of this.breakers.values()) {
      if (breaker.getState().state === CircuitState.OPEN) {
        return true;
      }
    }
    return false;
  }
}

// Singleton instance
export const circuitBreakerManager = new CircuitBreakerManager();

// Circuit breakers pre-configurados para endpoints comunes
export const apiCircuitBreaker = circuitBreakerManager.getOrCreate({
  name: 'api-general',
  failureThreshold: 5,
  resetTimeout: 60000, // 1 minuto
  successThreshold: 2,
  timeout: 30000, // 30 segundos
});

export const devicesCircuitBreaker = circuitBreakerManager.getOrCreate({
  name: 'api-devices',
  failureThreshold: 3,
  resetTimeout: 30000, // 30 segundos
  successThreshold: 2,
  timeout: 10000, // 10 segundos
});

export const appInstancesCircuitBreaker = circuitBreakerManager.getOrCreate({
  name: 'api-app-instances',
  failureThreshold: 3,
  resetTimeout: 30000, // 30 segundos
  successThreshold: 2,
  timeout: 10000, // 10 segundos
});
