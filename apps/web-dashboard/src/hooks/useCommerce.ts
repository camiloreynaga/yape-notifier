import { useState, useEffect, useCallback } from 'react';
import { apiService } from '@/services/api';
import type { Commerce } from '@/types';

interface UseCommerceReturn {
  commerce: Commerce | null;
  loading: boolean;
  error: string | null;
  hasCommerce: boolean;
  checkCommerce: () => Promise<void>;
  refreshCommerce: () => Promise<void>;
}

/**
 * Hook personalizado para gestionar el estado de Commerce del usuario
 * 
 * @returns {UseCommerceReturn} Objeto con el estado de commerce y métodos para gestionarlo
 */
export function useCommerce(): UseCommerceReturn {
  const [commerce, setCommerce] = useState<Commerce | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /**
   * Verifica si el usuario tiene un commerce asociado
   */
  const checkCommerce = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const commerceData = await apiService.checkCommerce();
      setCommerce(commerceData);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Error al verificar commerce';
      setError(errorMessage);
      setCommerce(null);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Refresca el estado de commerce desde el servidor
   */
  const refreshCommerce = useCallback(async () => {
    await checkCommerce();
  }, [checkCommerce]);

  // Verificar commerce al montar el hook
  useEffect(() => {
    checkCommerce();
  }, [checkCommerce]);

  return {
    commerce,
    loading,
    error,
    hasCommerce: !!commerce,
    checkCommerce,
    refreshCommerce,
  };
}

