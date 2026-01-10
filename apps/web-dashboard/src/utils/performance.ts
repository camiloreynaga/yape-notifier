/**
 * Performance monitoring utilities
 * Para medir y optimizar el rendimiento de la aplicación
 */

import { logger } from '@/services/logger';

/**
 * Medir el tiempo de ejecución de una función
 *
 * @example
 * const result = measureTime('Heavy computation', () => {
 *   // Código pesado aquí
 *   return complexCalculation();
 * });
 */
export function measureTime<T>(label: string, fn: () => T): T {
  const start = performance.now();
  try {
    const result = fn();
    const duration = performance.now() - start;

    if (duration > 100) {
      logger.warn(`⚠️ Slow operation: ${label} took ${duration.toFixed(2)}ms`, {
        label,
        duration,
      });
    } else if (duration > 16) {
      logger.debug(`Performance: ${label} took ${duration.toFixed(2)}ms`, {
        label,
        duration,
      });
    }

    return result;
  } catch (error) {
    const duration = performance.now() - start;
    logger.error(`Error in ${label} after ${duration.toFixed(2)}ms`, error as Error, {
      label,
      duration,
    });
    throw error;
  }
}

/**
 * Versión async de measureTime
 *
 * @example
 * const data = await measureTimeAsync('API call', async () => {
 *   return await apiService.getData();
 * });
 */
export async function measureTimeAsync<T>(
  label: string,
  fn: () => Promise<T>
): Promise<T> {
  const start = performance.now();
  try {
    const result = await fn();
    const duration = performance.now() - start;

    if (duration > 1000) {
      logger.warn(`⚠️ Slow async operation: ${label} took ${duration.toFixed(2)}ms`, {
        label,
        duration,
      });
    } else if (duration > 100) {
      logger.debug(`Performance: ${label} took ${duration.toFixed(2)}ms`, {
        label,
        duration,
      });
    }

    return result;
  } catch (error) {
    const duration = performance.now() - start;
    logger.error(`Error in ${label} after ${duration.toFixed(2)}ms`, error as Error, {
      label,
      duration,
    });
    throw error;
  }
}

/**
 * Performance marks para Web Vitals
 * Marca puntos importantes en el ciclo de vida de la app
 *
 * @example
 * performanceMark('app-init-start');
 * // ... código de inicialización
 * performanceMark('app-init-end');
 * const duration = performanceMeasure('app-init', 'app-init-start', 'app-init-end');
 */
export function performanceMark(name: string): void {
  if (typeof performance !== 'undefined' && performance.mark) {
    performance.mark(name);
  }
}

/**
 * Medir tiempo entre dos marks
 */
export function performanceMeasure(
  name: string,
  startMark: string,
  endMark: string
): number | null {
  if (typeof performance !== 'undefined' && performance.measure) {
    try {
      performance.measure(name, startMark, endMark);
      const measure = performance.getEntriesByName(name)[0];
      return measure ? measure.duration : null;
    } catch (error) {
      logger.error('Performance measure failed', error as Error, {
        name,
        startMark,
        endMark,
      });
      return null;
    }
  }
  return null;
}

/**
 * Limpiar marks y measures
 */
export function clearPerformanceMarks(): void {
  if (typeof performance !== 'undefined') {
    performance.clearMarks();
    performance.clearMeasures();
  }
}

/**
 * Obtener métricas de Web Vitals
 *
 * @example
 * const vitals = getWebVitals();
 * console.log('LCP:', vitals.lcp, 'ms');
 */
export interface WebVitals {
  fcp: number | null; // First Contentful Paint
  lcp: number | null; // Largest Contentful Paint
  fid: number | null; // First Input Delay
  cls: number | null; // Cumulative Layout Shift
  ttfb: number | null; // Time to First Byte
}

export function getWebVitals(): WebVitals {
  const vitals: WebVitals = {
    fcp: null,
    lcp: null,
    fid: null,
    cls: null,
    ttfb: null,
  };

  if (typeof performance === 'undefined') {
    return vitals;
  }

  // First Contentful Paint
  const fcpEntry = performance.getEntriesByName('first-contentful-paint')[0];
  if (fcpEntry) {
    vitals.fcp = fcpEntry.startTime;
  }

  // Largest Contentful Paint
  const lcpEntries = performance.getEntriesByType('largest-contentful-paint');
  if (lcpEntries.length > 0) {
    const lcpEntry = lcpEntries[lcpEntries.length - 1] as PerformanceEntry & {
      renderTime: number;
      loadTime: number;
    };
    vitals.lcp = lcpEntry.renderTime || lcpEntry.loadTime;
  }

  // Time to First Byte
  const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
  if (navEntry) {
    vitals.ttfb = navEntry.responseStart - navEntry.requestStart;
  }

  return vitals;
}

/**
 * Monitor de renders - detecta renders innecesarios
 *
 * @example
 * const renderMonitor = createRenderMonitor('MyComponent');
 *
 * function MyComponent({ data }) {
 *   renderMonitor.track({ data });
 *
 *   return <div>{data}</div>;
 * }
 */
export function createRenderMonitor(componentName: string) {
  let renderCount = 0;
  let lastProps: any = null;

  return {
    track(props?: any) {
      renderCount++;

      if (renderCount > 10) {
        logger.warn(`⚠️ Component ${componentName} rendered ${renderCount} times`, {
          componentName,
          renderCount,
        });
      }

      if (lastProps && props) {
        const changedProps = Object.keys(props).filter(
          (key) => props[key] !== lastProps[key]
        );

        if (changedProps.length === 0) {
          logger.warn(
            `⚠️ Component ${componentName} re-rendered without prop changes`,
            {
              componentName,
              renderCount,
            }
          );
        } else {
          logger.debug(
            `Component ${componentName} re-rendered. Changed props: ${changedProps.join(', ')}`,
            {
              componentName,
              changedProps,
              renderCount,
            }
          );
        }
      }

      lastProps = props;
    },

    reset() {
      renderCount = 0;
      lastProps = null;
    },

    getCount() {
      return renderCount;
    },
  };
}

/**
 * Detect long tasks (> 50ms) que bloquean el main thread
 *
 * @example
 * const observer = observeLongTasks((task) => {
 *   console.warn('Long task detected:', task.duration, 'ms');
 * });
 *
 * // Cleanup
 * observer.disconnect();
 */
export function observeLongTasks(
  callback: (task: PerformanceEntry) => void
): PerformanceObserver | null {
  if (typeof PerformanceObserver === 'undefined') {
    return null;
  }

  try {
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.duration > 50) {
          callback(entry);
          logger.warn('⚠️ Long task detected', {
            name: entry.name,
            duration: entry.duration,
            startTime: entry.startTime,
          });
        }
      }
    });

    observer.observe({ entryTypes: ['longtask'] });
    return observer;
  } catch (error) {
    logger.error('Failed to observe long tasks', error as Error);
    return null;
  }
}

/**
 * Medir el tamaño de memoria usado (solo Chrome)
 *
 * @example
 * const memory = getMemoryUsage();
 * console.log('Used:', memory.usedJSHeapSize, 'MB');
 */
export function getMemoryUsage(): {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
} | null {
  const perf = performance as any;

  if (perf.memory) {
    return {
      usedJSHeapSize: Math.round(perf.memory.usedJSHeapSize / 1024 / 1024),
      totalJSHeapSize: Math.round(perf.memory.totalJSHeapSize / 1024 / 1024),
      jsHeapSizeLimit: Math.round(perf.memory.jsHeapSizeLimit / 1024 / 1024),
    };
  }

  return null;
}

/**
 * Log de performance summary
 * Útil para debugging de performance
 */
export function logPerformanceSummary(): void {
  logger.info('=== Performance Summary ===');

  const vitals = getWebVitals();
  logger.info('Web Vitals:', vitals);

  const memory = getMemoryUsage();
  if (memory) {
    logger.info('Memory Usage:', memory);
  }

  const navigationEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
  if (navigationEntry) {
    logger.info('Navigation Timing:', {
      domContentLoaded: navigationEntry.domContentLoadedEventEnd - navigationEntry.domContentLoadedEventStart,
      loadComplete: navigationEntry.loadEventEnd - navigationEntry.loadEventStart,
      domInteractive: navigationEntry.domInteractive,
    });
  }

  logger.info('=== End Performance Summary ===');
}
