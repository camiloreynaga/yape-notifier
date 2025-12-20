/**
 * Componente reutilizable para mostrar cards de estadísticas (KPIs)
 */

import { LucideIcon } from 'lucide-react';
import clsx from 'clsx';

export interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  iconColor?: 'blue' | 'green' | 'yellow' | 'red' | 'purple' | 'indigo';
  trend?: {
    value: number;
    label: string;
    isPositive?: boolean;
  };
  link?: {
    href: string;
    label: string;
  };
  loading?: boolean;
  className?: string;
}

const iconColorClasses = {
  blue: 'bg-blue-100 text-blue-600',
  green: 'bg-green-100 text-green-600',
  yellow: 'bg-yellow-100 text-yellow-600',
  red: 'bg-red-100 text-red-600',
  purple: 'bg-purple-100 text-purple-600',
  indigo: 'bg-indigo-100 text-indigo-600',
};

export default function StatCard({
  title,
  value,
  icon: Icon,
  iconColor = 'blue',
  trend,
  link,
  loading = false,
  className,
}: StatCardProps) {
  if (loading) {
    return (
      <div className={clsx('card', className)}>
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
          <div className="h-8 bg-gray-200 rounded w-1/2 mb-2"></div>
          <div className="h-3 bg-gray-200 rounded w-1/3"></div>
        </div>
      </div>
    );
  }

  return (
    <div
      className={clsx(
        'card relative overflow-hidden transition-all duration-300 hover:shadow-lg',
        'bg-gradient-to-br from-white to-gray-50',
        className
      )}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mb-2">{value}</p>
          {trend && (
            <div className="flex items-center gap-1 text-sm">
              <span
                className={clsx(
                  'font-medium',
                  trend.isPositive !== false ? 'text-green-600' : 'text-red-600'
                )}
              >
                {trend.isPositive !== false ? '↑' : '↓'} {Math.abs(trend.value)}%
              </span>
              <span className="text-gray-500">{trend.label}</span>
            </div>
          )}
          {link && (
            <a
              href={link.href}
              className="mt-3 inline-flex items-center text-sm font-medium text-primary-600 hover:text-primary-700 transition-colors"
            >
              {link.label} →
            </a>
          )}
        </div>
        <div className={clsx('flex-shrink-0 p-3 rounded-lg', iconColorClasses[iconColor])}>
          <Icon className="h-6 w-6" aria-hidden="true" />
        </div>
      </div>
    </div>
  );
}

