/**
 * Throttle utility - Limita la ejecución de una función a una vez por período.
 *
 * Diferencia con debounce:
 * - Debounce: espera que el usuario TERMINE (útil para búsquedas)
 * - Throttle: ejecuta PERIÓDICAMENTE mientras el usuario actúa (útil para scroll)
 *
 * Útil para:
 * - Eventos de scroll (actualizar navbar, lazy loading)
 * - Eventos de resize (responsive adjustments)
 * - Movimiento del mouse (drag & drop, tooltips)
 * - Actualización de gráficos en tiempo real
 *
 * @example
 * const handleScroll = throttle(() => {
 *   console.log('Scroll position:', window.scrollY);
 * }, 100);
 *
 * window.addEventListener('scroll', handleScroll);
 * // Se ejecutará máximo 1 vez cada 100ms, sin importar cuántas veces scrollees
 */

export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle = false;
  let lastArgs: Parameters<T> | null = null;

  return function throttled(...args: Parameters<T>) {
    lastArgs = args;

    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      lastArgs = null;

      setTimeout(() => {
        inThrottle = false;
        // Si hubo llamadas mientras estábamos en throttle, ejecutar con los últimos args
        if (lastArgs !== null) {
          func(...lastArgs);
          lastArgs = null;
        }
      }, limit);
    }
  };
}

/**
 * Throttle con leading y trailing - Ejecuta tanto al inicio como al final.
 *
 * @example
 * const updatePosition = throttleLeadingTrailing((x, y) => {
 *   console.log('Position:', x, y);
 * }, 200);
 *
 * // Primera llamada: ejecuta inmediatamente (leading)
 * // Llamadas durante 200ms: throttled
 * // Última llamada después del throttle: ejecuta (trailing)
 */
export function throttleLeadingTrailing<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  let lastCallTime = 0;

  return function throttled(...args: Parameters<T>) {
    const now = Date.now();
    const timeSinceLastCall = now - lastCallTime;

    // Leading: ejecutar inmediatamente si ha pasado suficiente tiempo
    if (timeSinceLastCall >= limit) {
      func(...args);
      lastCallTime = now;
    } else {
      // Trailing: programar ejecución al final del período
      if (timeoutId !== null) {
        clearTimeout(timeoutId);
      }

      timeoutId = setTimeout(
        () => {
          func(...args);
          lastCallTime = Date.now();
          timeoutId = null;
        },
        limit - timeSinceLastCall
      );
    }
  };
}

/**
 * Cancelable throttle - Throttle que retorna función de cancelación.
 *
 * @example
 * const [throttledScroll, cancelScroll] = throttleCancelable(
 *   () => updateScrollPosition(),
 *   100
 * );
 *
 * window.addEventListener('scroll', throttledScroll);
 *
 * // Limpiar en unmount
 * useEffect(() => {
 *   return () => cancelScroll();
 * }, []);
 */
export function throttleCancelable<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): [(...args: Parameters<T>) => void, () => void] {
  let inThrottle = false;
  let lastArgs: Parameters<T> | null = null;
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  const throttled = function (...args: Parameters<T>) {
    lastArgs = args;

    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      lastArgs = null;

      timeoutId = setTimeout(() => {
        inThrottle = false;
        if (lastArgs !== null) {
          func(...lastArgs);
          lastArgs = null;
        }
        timeoutId = null;
      }, limit);
    }
  };

  const cancel = () => {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
      timeoutId = null;
    }
    inThrottle = false;
    lastArgs = null;
  };

  return [throttled, cancel];
}

/**
 * Request Animation Frame throttle - Optimizado para operaciones visuales.
 * Usa requestAnimationFrame en vez de setTimeout para mejor sincronización
 * con el rendering del navegador.
 *
 * Útil para:
 * - Animaciones
 * - Actualización de posiciones visuales
 * - Canvas/WebGL rendering
 * - Drag & drop visual
 *
 * @example
 * const updatePosition = rafThrottle((x: number, y: number) => {
 *   element.style.transform = `translate(${x}px, ${y}px)`;
 * });
 *
 * element.addEventListener('pointermove', (e) => {
 *   updatePosition(e.clientX, e.clientY);
 * });
 */
export function rafThrottle<T extends (...args: any[]) => any>(
  func: T
): (...args: Parameters<T>) => void {
  let rafId: number | null = null;
  let lastArgs: Parameters<T> | null = null;

  return function throttled(...args: Parameters<T>) {
    lastArgs = args;

    if (rafId === null) {
      rafId = requestAnimationFrame(() => {
        if (lastArgs !== null) {
          func(...lastArgs);
        }
        rafId = null;
        lastArgs = null;
      });
    }
  };
}

/**
 * Cancelable RAF throttle.
 *
 * @example
 * const [updateCanvas, cancelUpdate] = rafThrottleCancelable(
 *   () => drawCanvas()
 * );
 *
 * useEffect(() => {
 *   const interval = setInterval(updateCanvas, 16);
 *   return () => {
 *     clearInterval(interval);
 *     cancelUpdate();
 *   };
 * }, []);
 */
export function rafThrottleCancelable<T extends (...args: any[]) => any>(
  func: T
): [(...args: Parameters<T>) => void, () => void] {
  let rafId: number | null = null;
  let lastArgs: Parameters<T> | null = null;

  const throttled = function (...args: Parameters<T>) {
    lastArgs = args;

    if (rafId === null) {
      rafId = requestAnimationFrame(() => {
        if (lastArgs !== null) {
          func(...lastArgs);
        }
        rafId = null;
        lastArgs = null;
      });
    }
  };

  const cancel = () => {
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    lastArgs = null;
  };

  return [throttled, cancel];
}
