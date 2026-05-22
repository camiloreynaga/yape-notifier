import axios, { AxiosInstance } from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api';
import type {
  CommerceListItem,
  CommerceDetail,
  Plan,
  SuperAdminKpis,
} from '@/types';

interface PaginatedResponse<T> {
  data: T[];
  current_page: number;
  last_page: number;
  per_page: number;
  total: number;
}

class SuperAdminApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      timeout: 30000,
    });
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('auth_token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  async listCommerces(params: { status?: string; q?: string; page?: number } = {}): Promise<PaginatedResponse<CommerceListItem>> {
    const { data } = await this.client.get<PaginatedResponse<CommerceListItem>>(
      API_ENDPOINTS.admin.commerces, { params }
    );
    return data;
  }

  async getCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.get<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.commerce(id));
    return data.commerce;
  }

  async approveCommerce(id: number, planSlug: string): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.approve(id), { plan_slug: planSlug }
    );
    return data.commerce;
  }

  async renewCommerce(
    id: number,
    payload: { plan_slug: string; amount_paid?: number; notes?: string }
  ): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.renew(id), payload
    );
    return data.commerce;
  }

  async changePlan(id: number, planSlug: string): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(
      API_ENDPOINTS.admin.changePlan(id), { plan_slug: planSlug }
    );
    return data.commerce;
  }

  async suspendCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.suspend(id));
    return data.commerce;
  }

  async reactivateCommerce(id: number): Promise<CommerceDetail> {
    const { data } = await this.client.patch<{ commerce: CommerceDetail }>(API_ENDPOINTS.admin.reactivate(id));
    return data.commerce;
  }

  async getKpis(): Promise<SuperAdminKpis> {
    const { data } = await this.client.get<SuperAdminKpis>(API_ENDPOINTS.admin.dashboardKpis);
    return data;
  }

  async listPlans(): Promise<Plan[]> {
    const { data } = await this.client.get<{ plans: Plan[] }>(API_ENDPOINTS.admin.plans);
    return data.plans;
  }

  async updatePlan(
    id: number,
    payload: Partial<Pick<Plan, 'name' | 'max_devices' | 'max_notifications_per_day' | 'price' | 'is_active'>>
  ): Promise<Plan> {
    const { data } = await this.client.patch<{ plan: Plan }>(API_ENDPOINTS.admin.plan(id), payload);
    return data.plan;
  }
}

export const superAdminApi = new SuperAdminApiService();
