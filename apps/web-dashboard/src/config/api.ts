// API Base URL configuration
// In Docker production, this will be set via environment variable
// In development, it defaults to localhost
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8000";

export const API_ENDPOINTS = {
  auth: {
    register: "/api/register",
    login: "/api/login",
    logout: "/api/logout",
    me: "/api/me",
  },
  devices: {
    list: "/api/devices",
    create: "/api/devices",
    show: (id: number) => `/api/devices/${id}`,
    update: (id: number) => `/api/devices/${id}`,
    delete: (id: number) => `/api/devices/${id}`,
    toggleStatus: (id: number) => `/api/devices/${id}/toggle-status`,
    updateHealth: (id: number) => `/api/devices/${id}/health`,
    generateLinkCode: "/api/devices/generate-link-code",
    checkLinkCode: (code: string) => `/api/devices/link-code/${code}`,
  },
  notifications: {
    list: "/api/notifications",
    create: "/api/notifications",
    show: (id: number) => `/api/notifications/${id}`,
    statistics: "/api/notifications/statistics",
    updateStatus: (id: number) => `/api/notifications/${id}/status`,
    byInstance: "/api/notifications/by-instance",
  },
  monitorPackages: {
    list: "/api/monitor-packages",
    create: "/api/monitor-packages",
    show: (id: number) => `/api/monitor-packages/${id}`,
    update: (id: number) => `/api/monitor-packages/${id}`,
    delete: (id: number) => `/api/monitor-packages/${id}`,
    toggleStatus: (id: number) => `/api/monitor-packages/${id}/toggle-status`,
    bulkCreate: "/api/monitor-packages/bulk-create",
    detected: "/api/monitor-packages/detected",
    // Public endpoint for clients
    getActive: "/api/settings/monitored-packages",
  },
  commerces: {
    create: "/api/commerces",
    show: "/api/commerces/me",
    check: "/api/commerces/me", // Usa /me para obtener el commerce completo, /check solo retorna boolean
  },
  appInstances: {
    list: "/api/app-instances",
    getDeviceInstances: (deviceId: number) =>
      `/api/devices/${deviceId}/app-instances`,
    updateLabel: (id: number) => `/api/app-instances/${id}/label`,
  },
  users: {
    list: "/api/users",
    create: "/api/users",
    show: (id: number) => `/api/users/${id}`,
    update: (id: number) => `/api/users/${id}`,
    delete: (id: number) => `/api/users/${id}`,
    regeneratePin: (id: number) => `/api/users/${id}/regenerate-pin`,
    regeneratePassword: (id: number) => `/api/users/${id}/regenerate-password`,
  },
  deviceLogs: {
    commerce: "/api/device-logs/commerce",
    summary: "/api/device-logs/summary",
    device: (deviceId: number) => `/api/devices/${deviceId}/logs`,
  },
  referrals: {
    stats: '/api/referrals/stats',
    list: '/api/referrals/referrals',
    commissions: '/api/referrals/commissions',
    payoutAccount: '/api/commerces/me/payout-account',
  },
  admin: {
    commerces: '/api/admin/commerces',
    commerce: (id: number) => `/api/admin/commerces/${id}`,
    approve: (id: number) => `/api/admin/commerces/${id}/approve`,
    suspend: (id: number) => `/api/admin/commerces/${id}/suspend`,
    reactivate: (id: number) => `/api/admin/commerces/${id}/reactivate`,
    changePlan: (id: number) => `/api/admin/commerces/${id}/plan`,
    renew: (id: number) => `/api/admin/commerces/${id}/renew`,
    plans: '/api/admin/plans',
    plan: (id: number) => `/api/admin/plans/${id}`,
    dashboardKpis: '/api/admin/dashboard/kpis',
    commissions: '/api/admin/commissions',
    commissionApprove: (id: number) => `/api/admin/commissions/${id}/approve`,
    commissionPay: (id: number) => `/api/admin/commissions/${id}/pay`,
    commissionVoid: (id: number) => `/api/admin/commissions/${id}/void`,
  },
} as const;
