import { useMutation, useQueryClient } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useChangePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, plan_slug }: { id: number; plan_slug: string }) =>
      superAdminApi.changePlan(id, plan_slug),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerces'] });
      qc.invalidateQueries({ queryKey: ['superAdmin', 'commerce', vars.id] });
    },
  });
}
