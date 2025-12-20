/**
 * Constantes para DashboardTabs
 */

import { LayoutDashboard, Bell, Smartphone, Settings } from 'lucide-react';
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
    value: 'settings',
    label: 'Configuración',
    icon: Settings,
    ariaLabel: 'Configuración',
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

