// API Base URL configuration
// In Docker production, this will be set via environment variable
// In development, it defaults to localhost
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';

export const API_ENDPOINTS = {
  auth: {
    register: '/api/register',
    login: '/api/login',
    logout: '/api/logout',
    me: '/api/me',
  },
  devices: {
    list: '/api/devices',
    create: '/api/devices',
    show: (id: number) => `/api/devices/${id}`,
    update: (id: number) => `/api/devices/${id}`,
    delete: (id: number) => `/api/devices/${id}`,
    toggleStatus: (id: number) => `/api/devices/${id}/toggle-status`,
  },
  notifications: {
    list: '/api/notifications',
    create: '/api/notifications',
    show: (id: number) => `/api/notifications/${id}`,
    statistics: '/api/notifications/statistics',
    updateStatus: (id: number) => `/api/notifications/${id}/status`,
  },
  monitorPackages: {
    list: '/api/monitor-packages',
    create: '/api/monitor-packages',
    show: (id: number) => `/api/monitor-packages/${id}`,
    update: (id: number) => `/api/monitor-packages/${id}`,
    delete: (id: number) => `/api/monitor-packages/${id}`,
    toggleStatus: (id: number) => `/api/monitor-packages/${id}/toggle-status`,
    bulkCreate: '/api/monitor-packages/bulk-create',
    // Public endpoint for clients
    getActive: '/api/settings/monitored-packages',
  },
  commerces: {
    create: '/api/commerces',
    show: '/api/commerces/me',
  },
  appInstances: {
    list: '/api/app-instances',
    getDeviceInstances: (deviceId: number) => `/api/devices/${deviceId}/app-instances`,
    updateLabel: (id: number) => `/api/app-instances/${id}/label`,
  },
} as const;

