import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '@/services/api';

export function useValidateNotification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'validated' | 'pending' | 'inconsistent' }) =>
      apiService.updateNotificationStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notifications', 'byInstance'] });
      qc.invalidateQueries({ queryKey: ['notifications', 'statistics'] });
    },
  });
}
