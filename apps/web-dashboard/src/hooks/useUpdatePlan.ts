import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';
import type { Plan } from '@/types';

export function useUpdatePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...payload }: {
      id: number;
    } & Partial<Pick<Plan, 'name' | 'max_devices' | 'max_notifications_per_day' | 'price' | 'is_active'>>) =>
      superAdminApi.updatePlan(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'plans'] });
    },
  });
}
