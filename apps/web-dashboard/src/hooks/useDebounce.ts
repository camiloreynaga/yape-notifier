import { useState, useEffect, useRef, useCallback } from 'react';
import { debounce } from '@/utils/debounce';

/**
 * Hook para debouncing de valores.
 * Actualiza el valor después de un delay desde el último cambio.
 *
 * Útil para:
 * - Búsquedas en tiempo real
 * - Validaciones de formularios
 * - Cualquier input que necesite esperar que el usuario termine
 *
 * @example
 * function SearchComponent() {
 *   const [searchTerm, setSearchTerm] = useState('');
 *   const debouncedSearchTerm = useDebounce(searchTerm, 300);
 *
 *   // Solo hace request cuando el usuario para de escribir por 300ms
 *   useEffect(() => {
 *     if (debouncedSearchTerm) {
 *       apiService.search(debouncedSearchTerm);
 *     }
 *   }, [debouncedSearchTerm]);
 *
 *   return (
 *     <input
 *       value={searchTerm}
 *       onChange={(e) => setSearchTerm(e.target.value)}
 *     />
 *   );
 * }
 */
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    // Crear timeout para actualizar el valor después del delay
    const timeoutId = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    // Cleanup: cancelar timeout si value cambia antes del delay
    return () => {
      clearTimeout(timeoutId);
    };
  }, [value, delay]);

  return debouncedValue;
}

/**
 * Hook para debouncing de funciones.
 * Retorna versión debounced de la función que se auto-limpia en unmount.
 *
 * Útil para:
 * - Event handlers que no deben ejecutarse en cada evento
 * - Callbacks que disparan API calls
 *
 * @example
 * function SearchComponent() {
 *   const [results, setResults] = useState([]);
 *
 *   const handleSearch = useDebouncedCallback(
 *     async (query: string) => {
 *       const data = await apiService.search(query);
 *       setResults(data);
 *     },
 *     300
 *   );
 *
 *   return (
 *     <input onChange={(e) => handleSearch(e.target.value)} />
 *   );
 * }
 */
export function useDebouncedCallback<T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): (...args: Parameters<T>) => void {
  // Usar ref para mantener la última versión del callback
  const callbackRef = useRef(callback);

  // Actualizar ref cuando callback cambia
  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  // Crear versión debounced que usa la ref
  const debouncedCallback = useCallback(
    debounce((...args: Parameters<T>) => {
      callbackRef.current(...args);
    }, delay),
    [delay]
  );

  return debouncedCallback;
}

/**
 * Hook combinado: debounce de valor + loading state.
 * Útil para mostrar indicadores de "buscando..." mientras se espera.
 *
 * @example
 * function SearchComponent() {
 *   const [searchTerm, setSearchTerm] = useState('');
 *   const { debouncedValue, isDebouncing } = useDebouncedValue(searchTerm, 300);
 *
 *   return (
 *     <>
 *       <input value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
 *       {isDebouncing && <span>Buscando...</span>}
 *     </>
 *   );
 * }
 */
export function useDebouncedValue<T>(
  value: T,
  delay: number
): { debouncedValue: T; isDebouncing: boolean } {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);
  const [isDebouncing, setIsDebouncing] = useState(false);

  useEffect(() => {
    // Marcar como debouncing si el valor es diferente
    if (value !== debouncedValue) {
      setIsDebouncing(true);
    }

    const timeoutId = setTimeout(() => {
      setDebouncedValue(value);
      setIsDebouncing(false);
    }, delay);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [value, delay]);

  return { debouncedValue, isDebouncing };
}
