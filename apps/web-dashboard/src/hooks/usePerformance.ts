import { useEffect, useRef } from 'react';
import { createRenderMonitor, performanceMark, performanceMeasure } from '@/utils/performance';
import { logger } from '@/services/logger';

/**
 * Hook para monitorear renders de un componente.
 * Ayuda a detectar renders innecesarios y optimizar performance.
 *
 * @example
 * function MyComponent({ data, onUpdate }) {
 *   useRenderMonitor('MyComponent', { data, onUpdate });
 *
 *   return <div>{data}</div>;
 * }
 */
export function useRenderMonitor(componentName: string, props?: any): void {
  const monitorRef = useRef(createRenderMonitor(componentName));

  useEffect(() => {
    monitorRef.current.track(props);
  });
}

/**
 * Hook para medir el tiempo de render de un componente.
 *
 * @example
 * function HeavyComponent() {
 *   useRenderTime('HeavyComponent');
 *
 *   // Expensive rendering logic
 *   return <div>...</div>;
 * }
 */
export function useRenderTime(componentName: string): void {
  const renderCountRef = useRef(0);

  useEffect(() => {
    renderCountRef.current++;
    const renderNum = renderCountRef.current;
    const startMark = `${componentName}-render-${renderNum}-start`;
    const endMark = `${componentName}-render-${renderNum}-end`;

    performanceMark(endMark);

    // Medir desde el inicio del render (que marcamos antes)
    if (renderNum > 1) {
      const prevEndMark = `${componentName}-render-${renderNum - 1}-end`;
      const duration = performanceMeasure(
        `${componentName}-render-${renderNum}`,
        prevEndMark,
        endMark
      );

      if (duration && duration > 16) {
        logger.warn(`⚠️ Slow render: ${componentName} took ${duration.toFixed(2)}ms`, {
          componentName,
          renderNum,
          duration,
        });
      }
    }

    return () => {
      performanceMark(startMark);
    };
  });
}

/**
 * Hook para medir el tiempo de mount de un componente.
 *
 * @example
 * function MyComponent() {
 *   useMountTime('MyComponent');
 *   return <div>...</div>;
 * }
 */
export function useMountTime(componentName: string): void {
  useEffect(() => {
    const startMark = `${componentName}-mount-start`;
    const endMark = `${componentName}-mount-end`;

    performanceMark(endMark);

    // Si existe un mark de inicio (del render anterior)
    try {
      const duration = performanceMeasure(
        `${componentName}-mount`,
        startMark,
        endMark
      );

      if (duration) {
        logger.debug(`${componentName} mounted in ${duration.toFixed(2)}ms`, {
          componentName,
          duration,
        });

        if (duration > 100) {
          logger.warn(`⚠️ Slow mount: ${componentName} took ${duration.toFixed(2)}ms`, {
            componentName,
            duration,
          });
        }
      }
    } catch {
      // No hay mark de inicio, es el primer render
    }

    return () => {
      performanceMark(startMark);
    };
  }, [componentName]);
}

/**
 * Hook para detectar cuando un efecto se ejecuta innecesariamente.
 *
 * @example
 * function MyComponent({ userId, settings }) {
 *   useWhyDidYouUpdate('MyComponent', { userId, settings });
 *
 *   useEffect(() => {
 *     // Efecto que depende de userId y settings
 *   }, [userId, settings]);
 * }
 */
export function useWhyDidYouUpdate(name: string, props: Record<string, any>): void {
  const previousProps = useRef<Record<string, any>>();

  useEffect(() => {
    if (previousProps.current) {
      const allKeys = Object.keys({ ...previousProps.current, ...props });
      const changedProps: Record<string, { from: any; to: any }> = {};

      allKeys.forEach((key) => {
        if (previousProps.current![key] !== props[key]) {
          changedProps[key] = {
            from: previousProps.current![key],
            to: props[key],
          };
        }
      });

      if (Object.keys(changedProps).length > 0) {
        logger.debug(`[${name}] Props changed:`, changedProps);
      }
    }

    previousProps.current = props;
  });
}

/**
 * Hook para medir performance de efectos async.
 *
 * @example
 * function MyComponent() {
 *   const measureAsync = useAsyncPerformance('MyComponent');
 *
 *   useEffect(() => {
 *     measureAsync('fetchData', async () => {
 *       await apiService.getData();
 *     });
 *   }, []);
 * }
 */
export function useAsyncPerformance(componentName: string) {
  return async <T,>(label: string, fn: () => Promise<T>): Promise<T> => {
    const start = performance.now();
    try {
      const result = await fn();
      const duration = performance.now() - start;

      logger.debug(`[${componentName}] ${label} completed in ${duration.toFixed(2)}ms`, {
        componentName,
        label,
        duration,
      });

      if (duration > 1000) {
        logger.warn(
          `⚠️ [${componentName}] Slow async operation: ${label} took ${duration.toFixed(2)}ms`,
          {
            componentName,
            label,
            duration,
          }
        );
      }

      return result;
    } catch (error) {
      const duration = performance.now() - start;
      logger.error(
        `[${componentName}] ${label} failed after ${duration.toFixed(2)}ms`,
        error as Error,
        {
          componentName,
          label,
          duration,
        }
      );
      throw error;
    }
  };
}
