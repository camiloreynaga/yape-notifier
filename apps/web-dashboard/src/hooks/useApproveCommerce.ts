import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useApproveCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug }: { id: number; plan_slug: string }) =>
      superAdminApi.approveCommerce(id, plan_slug),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
