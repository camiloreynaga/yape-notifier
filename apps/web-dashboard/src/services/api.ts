import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api';
import type { AuthResponse, User, Device, Notification, NotificationFilters, NotificationStatistics, PaginatedResponse, ApiError, Commerce, AppInstance } from '@/types';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('auth_token');
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor to handle errors
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiError>) => {
        if (error.response?.status === 401) {
          // Unauthorized - clear token and redirect to login
          localStorage.removeItem('auth_token');
          localStorage.removeItem('user');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth methods
  async register(name: string, email: string, password: string): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>(API_ENDPOINTS.auth.register, {
      name,
      email,
      password,
      password_confirmation: password,
    });
    return response.data;
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>(API_ENDPOINTS.auth.login, {
      email,
      password,
    });
    return response.data;
  }

  async logout(): Promise<void> {
    await this.client.post(API_ENDPOINTS.auth.logout);
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get<{ user: User }>(API_ENDPOINTS.auth.me);
    return response.data.user;
  }

  // Device methods
  async getDevices(activeOnly = false): Promise<Device[]> {
    const response = await this.client.get<{ devices: Device[] }>(API_ENDPOINTS.devices.list, {
      params: { active_only: activeOnly },
    });
    return response.data.devices;
  }

  async createDevice(data: { name: string; platform: string }): Promise<Device> {
    const response = await this.client.post<{ device: Device }>(API_ENDPOINTS.devices.create, data);
    return response.data.device;
  }

  async updateDevice(id: number, data: { name?: string; platform?: string }): Promise<Device> {
    const response = await this.client.put<{ device: Device }>(API_ENDPOINTS.devices.update(id), data);
    return response.data.device;
  }

  async deleteDevice(id: number): Promise<void> {
    await this.client.delete(API_ENDPOINTS.devices.delete(id));
  }

  async toggleDeviceStatus(id: number, isActive: boolean): Promise<Device> {
    const response = await this.client.post<{ device: Device }>(
      API_ENDPOINTS.devices.toggleStatus(id),
      { is_active: isActive }
    );
    return response.data.device;
  }

  // Notification methods
  async getNotifications(filters?: NotificationFilters): Promise<PaginatedResponse<Notification>> {
    const response = await this.client.get<PaginatedResponse<Notification>>(
      API_ENDPOINTS.notifications.list,
      { params: filters }
    );
    return response.data;
  }

  async getNotification(id: number): Promise<Notification> {
    const response = await this.client.get<{ notification: Notification }>(
      API_ENDPOINTS.notifications.show(id)
    );
    return response.data.notification;
  }

  async getStatistics(filters?: { start_date?: string; end_date?: string }): Promise<NotificationStatistics> {
    const response = await this.client.get<NotificationStatistics>(
      API_ENDPOINTS.notifications.statistics,
      { params: filters }
    );
    return response.data;
  }

  async updateNotificationStatus(id: number, status: 'pending' | 'validated' | 'inconsistent'): Promise<Notification> {
    const response = await this.client.patch<{ notification: Notification }>(
      API_ENDPOINTS.notifications.updateStatus(id),
      { status }
    );
    return response.data.notification;
  }

  // Commerce methods
  async createCommerce(name: string): Promise<Commerce> {
    const response = await this.client.post<{ commerce: Commerce }>(
      API_ENDPOINTS.commerces.create,
      { name }
    );
    return response.data.commerce;
  }

  async getCommerce(): Promise<Commerce | null> {
    try {
      const response = await this.client.get<{ commerce: Commerce }>(API_ENDPOINTS.commerces.show);
      return response.data.commerce;
    } catch (error) {
      return null;
    }
  }

  // App Instance methods
  async getAppInstances(deviceId?: number): Promise<AppInstance[]> {
    const response = await this.client.get<{ instances: AppInstance[] }>(
      API_ENDPOINTS.appInstances.list,
      { params: deviceId ? { device_id: deviceId } : {} }
    );
    return response.data.instances;
  }

  async getDeviceAppInstances(deviceId: number): Promise<AppInstance[]> {
    const response = await this.client.get<{ instances: AppInstance[] }>(
      API_ENDPOINTS.appInstances.getDeviceInstances(deviceId)
    );
    return response.data.instances;
  }

  async updateAppInstanceLabel(id: number, label: string): Promise<AppInstance> {
    const response = await this.client.patch<{ instance: AppInstance }>(
      API_ENDPOINTS.appInstances.updateLabel(id),
      { instance_label: label }
    );
    return response.data.instance;
  }
}

export const apiService = new ApiService();

