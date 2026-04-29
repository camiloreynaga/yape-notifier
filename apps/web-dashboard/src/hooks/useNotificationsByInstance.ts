import { useQuery } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useNotificationsByInstance(params: { start_date?: string; end_date?: string } = {}) {
  return useQuery({
    queryKey: ['notifications', 'byInstance', params],
    queryFn: () => apiService.getNotificationsByInstance(params),
    staleTime: 30_000,
  });
}
