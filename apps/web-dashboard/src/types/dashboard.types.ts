/**
 * Tipos TypeScript para el Dashboard
 */

export type TabValue = 'overview' | 'notifications' | 'devices' | 'settings';

export type PeriodFilter = 'today' | 'week' | 'month' | 'custom';

export interface PeriodRange {
  start_date: string;
  end_date: string;
}

export interface DashboardKPIs {
  notificationsToday: number;
  notificationsWeek: number;
  notificationsMonth: number;
  amountToday: number;
  amountWeek: number;
  amountMonth: number;
  activeDevices: number;
  totalDevices: number;
  pendingNotifications: number;
  duplicateNotifications: number;
  activeAppInstances: number;
}

export interface ChartDataPoint {
  name: string;
  value: number;
  [key: string]: string | number;
}

export interface NotificationByDate {
  date: string;
  count: number;
  amount: number;
}

export interface TabBadge {
  count: number;
  variant?: 'default' | 'warning' | 'danger' | 'success';
}

