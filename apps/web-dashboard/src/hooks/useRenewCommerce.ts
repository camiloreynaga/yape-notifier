import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useRenewCommerce() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug, amount_paid, notes }: {
      id: number; plan_slug: string; amount_paid?: number; notes?: string;
    }) => superAdminApi.renewCommerce(id, { plan_slug, amount_paid, notes }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'kpis'] });
    },
  });
}
