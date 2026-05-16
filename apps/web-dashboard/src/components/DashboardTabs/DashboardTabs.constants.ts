/**
 * Constantes para DashboardTabs
 */

import { LayoutDashboard, Bell, Smartphone, Users, FileText, Settings, Gift } from 'lucide-react';
import type { TabValue } from '@/types/dashboard.types';

export interface TabConfig {
  value: TabValue;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  ariaLabel: string;
}

export const TABS: TabConfig[] = [
  {
    value: 'overview',
    label: 'Resumen',
    icon: LayoutDashboard,
    ariaLabel: 'Resumen del dashboard',
  },
  {
    value: 'notifications',
    label: 'Notificaciones',
    icon: Bell,
    ariaLabel: 'Notificaciones',
  },
  {
    value: 'devices',
    label: 'Dispositivos',
    icon: Smartphone,
    ariaLabel: 'Dispositivos',
  },
  {
    value: 'employees',
    label: 'Empleados',
    icon: Users,
    ariaLabel: 'Empleados',
  },
  {
    value: 'logs',
    label: 'Logs',
    icon: FileText,
    ariaLabel: 'Logs de dispositivos',
  },
  {
    value: 'settings',
    label: 'Configuracion',
    icon: Settings,
    ariaLabel: 'Configuracion',
  },
  {
    value: 'referrals',
    label: 'Referidos',
    icon: Gift,
    ariaLabel: 'Programa de Referidos',
  },
];

export const DEFAULT_TAB: TabValue = 'overview';

export const TAB_KEYS = {
  ARROW_LEFT: 'ArrowLeft',
  ARROW_RIGHT: 'ArrowRight',
  HOME: 'Home',
  END: 'End',
  ENTER: 'Enter',
  SPACE: ' ',
} as const;

