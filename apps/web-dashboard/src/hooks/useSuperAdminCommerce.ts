import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export function useSuperAdminCommerce(id: number | null) {
  return useQuery({
    queryKey: ['superAdmin', 'commerce', id],
    queryFn: () => superAdminApi.getCommerce(id as number),
    enabled: id !== null,
    staleTime: 30_000,
  });
}
