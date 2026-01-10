import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '@/services/api';
import { logger } from '@/services/logger';
import type { AppInstance } from '@/types';

/**
 * React Query hook para gestionar instancias de aplicaciones con cache y optimización
 *
 * Características:
 * - Cache automático de 5 minutos
 * - Retry automático con exponential backoff (3 intentos)
 * - Deduplicación de requests
 * - Sincronización entre componentes
 * - Refetch automático cuando la ventana recupera foco
 *
 * @param deviceId - ID del dispositivo (opcional). Si se proporciona, filtra por dispositivo
 * @param enabled - Si es false, no ejecuta la query automáticamente
 */
export function useAppInstances(deviceId?: number, enabled = true) {
  return useQuery<AppInstance[], Error>({
    queryKey: ['appInstances', { deviceId }],
    queryFn: async () => {
      logger.debug('Fetching app instances from API', { deviceId });
      return await apiService.getAppInstances(deviceId);
    },
    enabled,
    staleTime: 5 * 60 * 1000, // 5 minutos - los datos se consideran frescos
    gcTime: 10 * 60 * 1000, // 10 minutos - tiempo en cache después de no usarse
    refetchOnWindowFocus: true,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff
  });
}

/**
 * Hook para obtener instancias de un dispositivo específico
 * Útil cuando necesitas las instancias de un dispositivo particular
 *
 * @param deviceId - ID del dispositivo
 * @param enabled - Si es false, no ejecuta la query automáticamente
 */
export function useDeviceAppInstances(deviceId: number, enabled = true) {
  return useQuery<AppInstance[], Error>({
    queryKey: ['appInstances', 'device', deviceId],
    queryFn: async () => {
      logger.debug('Fetching device app instances from API', { deviceId });
      return await apiService.getDeviceAppInstances(deviceId);
    },
    enabled: enabled && !!deviceId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    refetchOnWindowFocus: true,
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });
}

/**
 * Hook para actualizar el label de una instancia de app
 * Actualiza optimísticamente el cache mientras espera la respuesta
 */
export function useUpdateAppInstanceLabel() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, label }: { id: number; label: string }) =>
      apiService.updateAppInstanceLabel(id, label),
    onMutate: async ({ id, label }) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['appInstances'] });

      // Snapshot the previous values
      const previousInstances: Map<string, AppInstance[] | undefined> = new Map();

      // Get all cached queries for appInstances
      queryClient
        .getQueriesData<AppInstance[]>({ queryKey: ['appInstances'] })
        .forEach(([queryKey, data]) => {
          previousInstances.set(JSON.stringify(queryKey), data);
        });

      // Optimistically update all related queries
      queryClient.setQueriesData<AppInstance[]>(
        { queryKey: ['appInstances'] },
        (old) => {
          if (!old) return old;
          return old.map((instance) =>
            instance.id === id ? { ...instance, instance_label: label } : instance
          );
        }
      );

      return { previousInstances };
    },
    onError: (error, variables, context) => {
      // Rollback on error
      if (context?.previousInstances) {
        context.previousInstances.forEach((data, queryKey) => {
          queryClient.setQueryData(JSON.parse(queryKey), data);
        });
      }
      logger.error('Error updating app instance label', error as Error, {
        instanceId: variables.id,
      });
    },
    onSuccess: (updatedInstance) => {
      logger.info('App instance label updated successfully', {
        instanceId: updatedInstance.id,
        label: updatedInstance.instance_label,
      });
      // Invalidar todas las queries de appInstances para asegurar consistencia
      queryClient.invalidateQueries({ queryKey: ['appInstances'] });
    },
  });
}

/**
 * Hook helper para obtener una instancia específica del cache
 * Útil para acceder a datos ya cargados sin hacer otra request
 *
 * @param instanceId - ID de la instancia a buscar
 * @returns La instancia si está en cache, undefined si no
 */
export function useAppInstanceFromCache(instanceId: number): AppInstance | undefined {
  const queryClient = useQueryClient();

  // Buscar en todas las queries de appInstances cacheadas
  const queries = queryClient.getQueriesData<AppInstance[]>({
    queryKey: ['appInstances'],
  });

  for (const [, instances] of queries) {
    if (instances) {
      const found = instances.find((instance) => instance.id === instanceId);
      if (found) return found;
    }
  }

  return undefined;
}

/**
 * Hook para invalidar manualmente el cache de app instances
 * Útil cuando necesitas forzar un refresh desde el servidor
 */
export function useInvalidateAppInstances() {
  const queryClient = useQueryClient();

  return () => {
    logger.debug('Manually invalidating app instances cache');
    queryClient.invalidateQueries({ queryKey: ['appInstances'] });
  };
}
