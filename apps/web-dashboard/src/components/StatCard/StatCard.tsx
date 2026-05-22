/**
 * Componente reutilizable para mostrar cards de estadísticas (KPIs)
 * Versión mejorada con glassmorphism, sparklines y animaciones
 */

import { LucideIcon, TrendingUp, TrendingDown } from 'lucide-react';
import clsx from 'clsx';
import { useMemo } from 'react';

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
  sparklineData?: number[]; // Datos para mini gráfico
  size?: 'normal' | 'large'; // Tamaño de la card
}

const iconColorClasses = {
  blue: 'bg-gradient-to-br from-blue-400 to-blue-600 text-white shadow-lg shadow-blue-500/50',
  green: 'bg-gradient-to-br from-green-400 to-green-600 text-white shadow-lg shadow-green-500/50',
  yellow: 'bg-gradient-to-br from-yellow-400 to-yellow-600 text-white shadow-lg shadow-yellow-500/50',
  red: 'bg-gradient-to-br from-red-400 to-red-600 text-white shadow-lg shadow-red-500/50',
  purple: 'bg-gradient-to-br from-purple-400 to-purple-600 text-white shadow-lg shadow-purple-500/50',
  indigo: 'bg-gradient-to-br from-indigo-400 to-indigo-600 text-white shadow-lg shadow-indigo-500/50',
};

const sparklineColorClasses = {
  blue: 'stroke-blue-500',
  green: 'stroke-green-500',
  yellow: 'stroke-yellow-500',
  red: 'stroke-red-500',
  purple: 'stroke-purple-500',
  indigo: 'stroke-indigo-500',
};

// Componente Sparkline (mini gráfico)
function Sparkline({ data, color }: { data: number[]; color: string }) {
  const points = useMemo(() => {
    if (!data || data.length === 0) return '';

    const max = Math.max(...data);
    const min = Math.min(...data);
    const range = max - min || 1;
    const width = 100;
    const height = 24;

    return data
      .map((value, index) => {
        const x = (index / (data.length - 1)) * width;
        const y = height - ((value - min) / range) * height;
        return `${x},${y}`;
      })
      .join(' ');
  }, [data]);

  if (!data || data.length === 0) return null;

  return (
    <svg
      className="w-full h-6 opacity-50 hover:opacity-100 transition-opacity"
      viewBox="0 0 100 24"
      preserveAspectRatio="none"
    >
      <polyline
        points={points}
        fill="none"
        className={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default function StatCard({
  title,
  value,
  icon: Icon,
  iconColor = 'blue',
  trend,
  link,
  loading = false,
  className,
  sparklineData,
  size = 'normal',
}: StatCardProps) {
  if (loading) {
    return (
      <div className={clsx('card', size === 'large' && 'row-span-2', className)}>
        <div className="animate-pulse space-y-3">
          <div className="h-4 bg-gray-200 rounded w-3/4"></div>
          <div className="h-10 bg-gray-200 rounded w-1/2"></div>
          <div className="h-6 bg-gray-200 rounded w-full"></div>
          <div className="h-3 bg-gray-200 rounded w-1/3"></div>
        </div>
      </div>
    );
  }

  const TrendIcon = trend?.isPositive !== false ? TrendingUp : TrendingDown;

  return (
    <div
      className={clsx(
        'card relative overflow-hidden group',
        'bg-gradient-to-br from-white via-white to-gray-50/50',
        'border border-gray-100',
        'transition-all duration-500 ease-out',
        'hover:shadow-2xl hover:shadow-gray-200/50',
        'hover:-translate-y-1 hover:border-gray-200',
        'cursor-pointer',
        size === 'large' && 'row-span-2',
        className
      )}
    >
      {/* Efecto de brillo en hover */}
      <div className="absolute inset-0 bg-gradient-to-br from-transparent via-white/0 to-transparent opacity-0 group-hover:opacity-10 transition-opacity duration-500" />

      {/* Decoración de fondo */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-gray-50 to-transparent rounded-full blur-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />

      <div className="relative z-10">
        {/* Header con icono */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">
              {title}
            </p>
          </div>
          <div
            className={clsx(
              'flex-shrink-0 p-3 rounded-xl transition-all duration-300',
              'group-hover:scale-110 group-hover:rotate-3',
              iconColorClasses[iconColor]
            )}
          >
            <Icon className="h-5 w-5" aria-hidden="true" />
          </div>
        </div>

        {/* Valor principal */}
        <div className="mb-3">
          <p className={clsx(
            'font-bold text-gray-900 transition-all duration-300',
            size === 'large' ? 'text-4xl sm:text-5xl' : 'text-2xl sm:text-3xl'
          )}>
            {value}
          </p>
        </div>

        {/* Sparkline */}
        {sparklineData && (
          <div className="mb-3">
            <Sparkline data={sparklineData} color={sparklineColorClasses[iconColor]} />
          </div>
        )}

        {/* Trend */}
        {trend && (
          <div className="flex items-center gap-2 mb-3">
            <div
              className={clsx(
                'flex items-center gap-1 px-2 py-1 rounded-lg text-xs font-semibold',
                trend.isPositive !== false
                  ? 'bg-green-50 text-green-700 ring-1 ring-green-600/20'
                  : 'bg-red-50 text-red-700 ring-1 ring-red-600/20'
              )}
            >
              <TrendIcon className="h-3 w-3" />
              {Math.abs(trend.value)}%
            </div>
            <span className="text-xs text-gray-500">{trend.label}</span>
          </div>
        )}

        {/* Link */}
        {link && (
          <a
            href={link.href}
            className="inline-flex items-center gap-1 text-xs font-semibold text-primary-600 hover:text-primary-700 transition-colors group/link"
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            {link.label}
            <span className="group-hover/link:translate-x-1 transition-transform duration-200">→</span>
          </a>
        )}
      </div>
    </div>
  );
}

