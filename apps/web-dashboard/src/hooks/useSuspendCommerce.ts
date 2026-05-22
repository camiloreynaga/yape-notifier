import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuspendCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => superAdminApi.suspendCommerce(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
