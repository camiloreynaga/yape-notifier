/**
 * Componente para mostrar badges en tabs
 */

import clsx from 'clsx';
import type { TabBadge as TabBadgeType } from '@/types/dashboard.types';

export interface TabBadgeProps {
  badge: TabBadgeType | null;
  className?: string;
}

const variantClasses = {
  default: 'bg-gray-500',
  warning: 'bg-yellow-500',
  danger: 'bg-red-500',
  success: 'bg-green-500',
};

export default function TabBadge({ badge, className }: TabBadgeProps) {
  if (!badge || badge.count === 0) {
    return null;
  }

  return (
    <span
      className={clsx(
        'ml-2 inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold text-white rounded-full',
        variantClasses[badge.variant || 'default'],
        className
      )}
      aria-label={`${badge.count} elementos`}
    >
      {badge.count > 99 ? '99+' : badge.count}
    </span>
  );
}

