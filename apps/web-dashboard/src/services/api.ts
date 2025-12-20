import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api';
import type { AuthResponse, User, Device, Notification, NotificationFilters, NotificationStatistics, PaginatedResponse, ApiError, Commerce, AppInstance, MonitorPackage } from '@/types';

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

  async updateDeviceHealth(
    id: number,
    data: {
      battery_level?: number;
      battery_optimization_disabled?: boolean;
      notification_permission_enabled?: boolean;
    }
  ): Promise<Device> {
    const response = await this.client.post<{ 
      device: Device; 
      health: {
        battery_level?: number;
        battery_optimization_disabled?: boolean;
        notification_permission_enabled?: boolean;
      }
    }>(
      API_ENDPOINTS.devices.updateHealth(id),
      data
    );
    return response.data.device;
  }

  /**
   * Genera un código de vinculación para vincular un dispositivo
   * @returns Objeto con el código y fecha de expiración
   */
  async generateLinkCode(): Promise<{ code: string; expires_at: string }> {
    const response = await this.client.post<{
      message: string;
      code: string;
      expires_at: string;
    }>(API_ENDPOINTS.devices.generateLinkCode);
    return {
      code: response.data.code,
      expires_at: response.data.expires_at,
    };
  }

  /**
   * Verifica el estado de un código de vinculación
   * @param code Código de vinculación a verificar
   * @returns Objeto con información de validez y commerce asociado
   */
  async checkLinkCode(code: string): Promise<{
    valid: boolean;
    message: string;
    commerce?: { id: number; name: string };
  }> {
    const response = await this.client.get<{
      valid: boolean;
      message: string;
      commerce?: { id: number; name: string };
    }>(API_ENDPOINTS.devices.checkLinkCode(code));
    return response.data;
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

  /**
   * Verifica si el usuario tiene un commerce asociado
   * Retorna el commerce si existe, o null si no existe
   * Lanza error si hay un problema de conexión o autenticación
   */
  async checkCommerce(): Promise<Commerce | null> {
    try {
      const response = await this.client.get<{ commerce: Commerce }>(API_ENDPOINTS.commerces.check);
      return response.data.commerce;
    } catch (error) {
      const axiosError = error as AxiosError<ApiError>;
      // Si el error es 404, significa que no hay commerce (no es un error crítico)
      if (axiosError.response?.status === 404) {
        return null;
      }
      // Para otros errores, lanzar excepción
      throw new Error(axiosError.response?.data?.message || 'Error al verificar commerce');
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

  // Monitor Package methods
  /**
   * Obtiene todos los paquetes monitoreados
   * @param activeOnly Si es true, solo retorna los activos
   */
  async getMonitorPackages(activeOnly = false): Promise<MonitorPackage[]> {
    const response = await this.client.get<{ packages: MonitorPackage[] }>(
      API_ENDPOINTS.monitorPackages.list,
      { params: { active_only: activeOnly } }
    );
    return response.data.packages;
  }

  /**
   * Obtiene un paquete monitoreado por ID
   */
  async getMonitorPackage(id: number): Promise<MonitorPackage> {
    const response = await this.client.get<{ package: MonitorPackage }>(
      API_ENDPOINTS.monitorPackages.show(id)
    );
    return response.data.package;
  }

  /**
   * Crea un nuevo paquete monitoreado
   */
  async createMonitorPackage(data: {
    package_name: string;
    app_name?: string;
    description?: string;
    priority?: number;
  }): Promise<MonitorPackage> {
    const response = await this.client.post<{ package: MonitorPackage; message: string }>(
      API_ENDPOINTS.monitorPackages.create,
      data
    );
    return response.data.package;
  }

  /**
   * Actualiza un paquete monitoreado
   */
  async updateMonitorPackage(
    id: number,
    data: {
      package_name?: string;
      app_name?: string;
      description?: string;
      priority?: number;
    }
  ): Promise<MonitorPackage> {
    const response = await this.client.put<{ package: MonitorPackage; message: string }>(
      API_ENDPOINTS.monitorPackages.update(id),
      data
    );
    return response.data.package;
  }

  /**
   * Elimina un paquete monitoreado
   */
  async deleteMonitorPackage(id: number): Promise<void> {
    await this.client.delete(API_ENDPOINTS.monitorPackages.delete(id));
  }

  /**
   * Activa o desactiva un paquete monitoreado
   */
  async toggleMonitorPackageStatus(id: number, isActive: boolean): Promise<MonitorPackage> {
    const response = await this.client.post<{ package: MonitorPackage; message: string }>(
      API_ENDPOINTS.monitorPackages.toggleStatus(id),
      { is_active: isActive }
    );
    return response.data.package;
  }

  /**
   * Crea múltiples paquetes a la vez
   */
  async bulkCreateMonitorPackages(packageNames: string[]): Promise<{
    created_count: number;
    packages: MonitorPackage[];
  }> {
    const response = await this.client.post<{
      message: string;
      created_count: number;
      packages: MonitorPackage[];
    }>(API_ENDPOINTS.monitorPackages.bulkCreate, {
      packages: packageNames,
    });
    return {
      created_count: response.data.created_count,
      packages: response.data.packages,
    };
  }
}

export const apiService = new ApiService();

