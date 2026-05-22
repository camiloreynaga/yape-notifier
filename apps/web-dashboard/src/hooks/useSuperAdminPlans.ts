import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminPlans() {
  return useQuery({
    queryKey: ['superAdmin', 'plans'],
    queryFn: () => superAdminApi.listPlans(),
    staleTime: 60_000,
  });
}
