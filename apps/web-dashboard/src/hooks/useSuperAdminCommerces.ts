import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '@/services/superAdminApi';

export interface CommercesQueryParams {
  status?: string;
  q?: string;
  page?: number;
}

export function useSuperAdminCommerces(params: CommercesQueryParams) {
  return useQuery({
    queryKey: ['superAdmin', 'commerces', params],
    queryFn: () => superAdminApi.listCommerces(params),
    staleTime: 30_000,
  });
}
