import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminKpis() {
  return useQuery({
    queryKey: ['superAdmin', 'kpis'],
    queryFn: () => superAdminApi.getKpis(),
    staleTime: 30_000,
  });
}
