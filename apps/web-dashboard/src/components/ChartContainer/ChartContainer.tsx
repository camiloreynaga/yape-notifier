/**
 * Wrapper para gráficos con estados de loading y error
 */

import { ReactNode } from 'react';
import { AlertCircle } from 'lucide-react';
import clsx from 'clsx';

export interface ChartContainerProps {
  title: string;
  description?: string;
  loading?: boolean;
  error?: Error | string | null;
  empty?: boolean;
  emptyMessage?: string;
  children: ReactNode;
  className?: string;
  height?: number;
}

export default function ChartContainer({
  title,
  description,
  loading = false,
  error = null,
  empty = false,
  emptyMessage = 'No hay datos para mostrar',
  children,
  className,
  height = 300,
}: ChartContainerProps) {
  return (
    <div className={clsx('card', className)}>
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
        {description && <p className="text-sm text-gray-500 mt-1">{description}</p>}
      </div>

      <div style={{ minHeight: `${height}px` }} className="relative">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="animate-pulse space-y-4 w-full">
              <div className="h-4 bg-gray-200 rounded w-3/4"></div>
              <div className="h-4 bg-gray-200 rounded w-1/2"></div>
              <div className="h-32 bg-gray-200 rounded"></div>
            </div>
          </div>
        ) : error ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <AlertCircle className="h-12 w-12 text-red-400 mx-auto mb-2" />
              <p className="text-sm text-gray-600">
                {typeof error === 'string' ? error : error?.message || 'Error al cargar datos'}
              </p>
            </div>
          </div>
        ) : empty ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <p className="text-sm text-gray-500">{emptyMessage}</p>
            </div>
          </div>
        ) : (
          children
        )}
      </div>
    </div>
  );
}

