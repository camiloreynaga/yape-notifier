import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '@/services/api';
import { logger } from '@/services/logger';
import type { Device } from '@/types';

/**
 * React Query hook para gestionar dispositivos con cache y optimización
 *
 * Características:
 * - Cache automático de 5 minutos
 * - Retry automático con exponential backoff (3 intentos)
 * - Deduplicación de requests
 * - Sincronización entre componentes
 * - Refetch automático cuando la ventana recupera foco
 *
 * @param activeOnly - Si es true, solo retorna dispositivos activos
 * @param enabled - Si es false, no ejecuta la query automáticamente
 */
export function useDevices(activeOnly = false, enabled = true) {
  return useQuery<Device[], Error>({
    queryKey: ['devices', { activeOnly }],
    queryFn: async () => {
      logger.debug('Fetching devices from API', { activeOnly });
      return await apiService.getDevices(activeOnly);
    },
    enabled,
    staleTime: 5 * 60 * 1000, // 5 minutos - los datos se consideran frescos
    gcTime: 10 * 60 * 1000, // 10 minutos - tiempo en cache después de no usarse
    refetchOnWindowFocus: true,
    retry: 3,
    retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff: 1s, 2s, 4s
  });
}

/**
 * Hook para crear un nuevo dispositivo
 * Invalida automáticamente el cache de devices después de crear
 */
export function useCreateDevice() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { name: string; platform: string }) =>
      apiService.createDevice(data),
    onSuccess: (newDevice: Device) => {
      logger.info('Device created successfully', { deviceId: newDevice.id });
      // Invalidar cache para refrescar la lista
      queryClient.invalidateQueries({ queryKey: ['devices'] });
    },
    onError: (error: Error) => {
      logger.error('Error creating device', error);
    },
  });
}

/**
 * Hook para actualizar un dispositivo
 * Actualiza optimísticamente el cache mientras espera la respuesta
 */
export function useUpdateDevice() {
  const queryClient = useQueryClient();

  return useMutation<
    Device,
    Error,
    { id: number; data: { name?: string; platform?: string } },
    { previousDevices: Device[] | undefined }
  >({
    mutationFn: ({ id, data }: { id: number; data: { name?: string; platform?: string } }) =>
      apiService.updateDevice(id, data),
    onMutate: async ({ id, data }: { id: number; data: { name?: string; platform?: string } }) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['devices'] });

      // Snapshot the previous value
      const previousDevices = queryClient.getQueryData<Device[]>(['devices', { activeOnly: false }]);

      // Optimistically update
      queryClient.setQueryData<Device[]>(['devices', { activeOnly: false }], (old: Device[] | undefined) => {
        if (!old) return old;
        return old.map((device: Device) =>
          device.id === id ? { ...device, ...data } : device
        );
      });

      return { previousDevices };
    },
    onError: (error: Error, variables: { id: number; data: { name?: string; platform?: string } }, context?: { previousDevices: Device[] | undefined }) => {
      // Rollback on error
      if (context?.previousDevices) {
        queryClient.setQueryData(['devices', { activeOnly: false }], context.previousDevices);
      }
      logger.error('Error updating device', error as Error, { deviceId: variables.id });
    },
    onSuccess: (updatedDevice: Device) => {
      logger.info('Device updated successfully', { deviceId: updatedDevice.id });
      // Invalidar cache para asegurar consistencia
      queryClient.invalidateQueries({ queryKey: ['devices'] });
    },
  });
}

/**
 * Hook para eliminar un dispositivo
 */
export function useDeleteDevice() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => apiService.deleteDevice(id),
    onSuccess: (_: void, deletedId: number) => {
      logger.info('Device deleted successfully', { deviceId: deletedId });
      // Invalidar cache para refrescar la lista
      queryClient.invalidateQueries({ queryKey: ['devices'] });
    },
    onError: (error: Error, deviceId: number) => {
      logger.error('Error deleting device', error, { deviceId });
    },
  });
}

/**
 * Hook para activar/desactivar un dispositivo
 */
export function useToggleDeviceStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, isActive }: { id: number; isActive: boolean }) =>
      apiService.toggleDeviceStatus(id, isActive),
    onSuccess: (updatedDevice: Device) => {
      logger.info('Device status toggled successfully', {
        deviceId: updatedDevice.id,
        isActive: updatedDevice.is_active
      });
      // Invalidar cache para refrescar la lista
      queryClient.invalidateQueries({ queryKey: ['devices'] });
    },
    onError: (error: Error, variables: { id: number; isActive: boolean }) => {
      logger.error('Error toggling device status', error, { deviceId: variables.id });
    },
  });
}

/**
 * Hook para actualizar el health de un dispositivo
 */
export function useUpdateDeviceHealth() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data
    }: {
      id: number;
      data: {
        battery_level?: number;
        battery_optimization_disabled?: boolean;
        notification_permission_enabled?: boolean;
      }
    }) => apiService.updateDeviceHealth(id, data),
    onSuccess: (updatedDevice: Device) => {
      logger.info('Device health updated successfully', { deviceId: updatedDevice.id });
      // Invalidar cache para refrescar
      queryClient.invalidateQueries({ queryKey: ['devices'] });
    },
    onError: (error: Error, variables: { id: number; data: { battery_level?: number; battery_optimization_disabled?: boolean; notification_permission_enabled?: boolean } }) => {
      logger.error('Error updating device health', error, { deviceId: variables.id });
    },
  });
}
