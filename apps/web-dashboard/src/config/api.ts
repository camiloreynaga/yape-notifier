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
} as const;

