/**
 * Debounce utility - Retrasa la ejecución de una función hasta que haya pasado
 * un tiempo determinado desde la última invocación.
 *
 * Útil para:
 * - Búsquedas en tiempo real (esperar que el usuario termine de escribir)
 * - Validaciones de formularios
 * - Auto-guardado
 *
 * @example
 * const debouncedSearch = debounce((query: string) => {
 *   apiService.search(query);
 * }, 300);
 *
 * // Solo ejecutará después de 300ms sin nuevas llamadas
 * debouncedSearch('test');
 * debouncedSearch('test 2'); // Cancela la anterior
 */

export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return function debounced(...args: Parameters<T>) {
    // Cancelar timeout anterior si existe
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    // Crear nuevo timeout
    timeoutId = setTimeout(() => {
      func(...args);
      timeoutId = null;
    }, wait);
  };
}

/**
 * Debounce con leading edge - Ejecuta inmediatamente en la primera llamada,
 * luego espera el delay antes de permitir otra ejecución.
 *
 * Útil para:
 * - Botones de envío (evitar doble click)
 * - Acciones que deben ejecutarse inmediatamente pero no repetirse
 *
 * @example
 * const handleSubmit = debounceLeading(async () => {
 *   await apiService.submitForm();
 * }, 1000);
 *
 * // Primera llamada: ejecuta inmediatamente
 * // Llamadas siguientes en <1s: ignoradas
 * handleSubmit(); // ✓ Ejecuta
 * handleSubmit(); // ✗ Ignorada (dentro de 1s)
 */
export function debounceLeading<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  let lastCallTime = 0;

  return function debouncedLeading(...args: Parameters<T>) {
    const now = Date.now();
    const timeSinceLastCall = now - lastCallTime;

    // Si es la primera llamada o ha pasado suficiente tiempo
    if (timeoutId === null && timeSinceLastCall >= wait) {
      func(...args);
      lastCallTime = now;
    }

    // Limpiar timeout anterior
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    // Resetear después del wait
    timeoutId = setTimeout(() => {
      timeoutId = null;
    }, wait);
  };
}

/**
 * Cancelable debounce - Debounce que retorna función de cancelación.
 *
 * Útil para:
 * - Limpiezas en unmount de componentes
 * - Cancelar búsquedas cuando el usuario navega
 *
 * @example
 * const [debouncedSearch, cancelSearch] = debounceCancelable(
 *   (query: string) => apiService.search(query),
 *   300
 * );
 *
 * useEffect(() => {
 *   debouncedSearch('query');
 *   return () => cancelSearch(); // Cancelar en unmount
 * }, []);
 */
export function debounceCancelable<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): [(...args: Parameters<T>) => void, () => void] {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  const debounced = function (...args: Parameters<T>) {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      func(...args);
      timeoutId = null;
    }, wait);
  };

  const cancel = () => {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
      timeoutId = null;
    }
  };

  return [debounced, cancel];
}
