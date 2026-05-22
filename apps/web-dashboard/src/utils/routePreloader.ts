// src/utils/routePreloader.ts
// Utilidades para precargar rutas lazy-loaded

import { logger } from '@/services/logger';

/**
 * Precarga un componente lazy-loaded
 * Útil para mejorar la experiencia en navegaciones predecibles
 */
export async function preloadRoute(importFn: () => Promise<any>): Promise<void> {
  try {
    logger.debug('Preloading route');
    await importFn();
    logger.debug('Route preloaded successfully');
  } catch (error) {
    logger.error('Failed to preload route', error as Error);
  }
}

/**
 * Precarga múltiples rutas en paralelo
 */
export async function preloadRoutes(importFns: Array<() => Promise<any>>): Promise<void> {
  try {
    logger.debug(`Preloading ${importFns.length} routes`);
    await Promise.all(importFns.map((fn) => fn()));
    logger.debug('All routes preloaded successfully');
  } catch (error) {
    logger.error('Failed to preload routes', error as Error);
  }
}

/**
 * Hook para precargar una ruta al hacer hover sobre un link
 */
export function prefetchOnHover(importFn: () => Promise<any>) {
  return {
    onMouseEnter: () => {
      preloadRoute(importFn);
    },
    onFocus: () => {
      preloadRoute(importFn);
    },
  };
}

/**
 * Precarga con delay (útil para rutas frecuentes después del login)
 */
export function preloadWithDelay(
  importFn: () => Promise<any>,
  delayMs: number = 2000
): void {
  setTimeout(() => {
    preloadRoute(importFn);
  }, delayMs);
}
