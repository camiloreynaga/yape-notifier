// src/components/NotificationList/NotificationList.tsx
// Componente optimizado de lista de notificaciones con infinite scroll

import { memo } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { Eye, Loader2 } from 'lucide-react';
import { useInfiniteScroll } from '@/hooks/useInfiniteNotifications';
import type { Notification } from '@/types';

interface NotificationListProps {
  notifications: Notification[];
  onStatusChange: (id: number, status: 'pending' | 'validated' | 'inconsistent') => void;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  onLoadMore: () => void;
  isLoading?: boolean;
}

/**
 * Componente de fila de notificación - Memoizado para performance
 */
const NotificationRow = memo(({
  notification,
  onStatusChange,
  onClick
}: {
  notification: Notification;
  onStatusChange: (id: number, status: 'pending' | 'validated' | 'inconsistent') => void;
  onClick: () => void;
}) => {
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'validated':
        return <span className="badge badge-success">Validado</span>;
      case 'inconsistent':
        return <span className="badge badge-danger">Inconsistente</span>;
      default:
        return <span className="badge badge-warning">Pendiente</span>;
    }
  };

  return (
    <tr
      className="hover:bg-gray-50 transition-colors cursor-pointer"
      onClick={onClick}
    >
      <td className="px-4 py-3 whitespace-nowrap">
        <div className="text-sm font-medium text-gray-900">
          {format(new Date(notification.received_at), 'dd/MM/yyyy')}
        </div>
        <div className="text-xs text-gray-500">
          {format(new Date(notification.received_at), 'HH:mm')}
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap">
        <div className="flex items-center">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 capitalize">
            {notification.source_app || 'N/A'}
          </span>
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 hidden lg:table-cell">
        <div className="max-w-[150px] truncate" title={notification.app_instance?.instance_label || (notification.android_user_id ? `${notification.package_name} (User ${notification.android_user_id})` : 'N/A')}>
          {notification.app_instance?.instance_label ||
           (notification.android_user_id ? `User ${notification.android_user_id}` : 'N/A')}
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 hidden md:table-cell">
        <div className="max-w-[120px] truncate" title={notification.device?.name || 'N/A'}>
          {notification.device?.name || 'N/A'}
        </div>
      </td>
      <td className="px-4 py-3">
        <div className="text-sm font-medium text-gray-900 max-w-[200px] truncate" title={notification.title}>
          {notification.title}
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-right">
        {notification.amount != null && !isNaN(Number(notification.amount)) ? (
          <div className="text-sm font-semibold text-gray-900">
            {notification.currency || 'S/'} {Number(notification.amount).toFixed(2)}
          </div>
        ) : (
          <span className="text-sm text-gray-400">N/A</span>
        )}
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 hidden xl:table-cell">
        <div className="max-w-[150px] truncate" title={notification.payer_name || 'N/A'}>
          {notification.payer_name || 'N/A'}
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-center">
        <div className="flex flex-col items-center gap-1">
          {getStatusBadge(notification.status)}
          {notification.is_duplicate && (
            <span className="badge badge-warning text-xs">Duplicado</span>
          )}
        </div>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-center" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onClick();
            }}
            className="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
            title="Ver detalle"
          >
            <Eye className="h-4 w-4" />
          </button>
          <select
            value={notification.status}
            onChange={(e) => {
              e.stopPropagation();
              onStatusChange(
                notification.id,
                e.target.value as 'pending' | 'validated' | 'inconsistent'
              );
            }}
            className="text-xs border border-gray-300 rounded-md px-2 py-1 bg-white hover:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-colors"
            onClick={(e) => e.stopPropagation()}
          >
            <option value="pending">Pendiente</option>
            <option value="validated">Validado</option>
            <option value="inconsistent">Inconsistente</option>
          </select>
        </div>
      </td>
    </tr>
  );
});

NotificationRow.displayName = 'NotificationRow';

/**
 * Componente principal de lista de notificaciones con infinite scroll
 */
export default function NotificationList({
  notifications,
  onStatusChange,
  hasNextPage,
  isFetchingNextPage,
  onLoadMore,
  isLoading,
}: NotificationListProps) {
  const navigate = useNavigate();

  // Infinite scroll trigger
  const loadMoreRef = useInfiniteScroll(
    () => {
      if (hasNextPage && !isFetchingNextPage) {
        onLoadMore();
      }
    },
    {
      threshold: 400, // Cargar cuando esté a 400px del final
      enabled: hasNextPage && !isFetchingNextPage,
    }
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (notifications.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No se encontraron notificaciones
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50 sticky top-0 z-10">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
              Fecha
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
              App
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider hidden lg:table-cell">
              Instancia
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider hidden md:table-cell">
              Dispositivo
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
              Título
            </th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-gray-700 uppercase tracking-wider">
              Monto
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider hidden xl:table-cell">
              Pagador
            </th>
            <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
              Estado
            </th>
            <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
              Acciones
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {notifications.map((notification) => (
            <NotificationRow
              key={notification.id}
              notification={notification}
              onStatusChange={onStatusChange}
              onClick={() => navigate(`/notifications/${notification.id}`)}
            />
          ))}
        </tbody>
      </table>

      {/* Load more trigger */}
      {hasNextPage && (
        <div
          ref={loadMoreRef}
          className="flex items-center justify-center py-6"
        >
          {isFetchingNextPage ? (
            <div className="flex items-center gap-2 text-gray-600">
              <Loader2 className="h-5 w-5 animate-spin" />
              <span>Cargando más...</span>
            </div>
          ) : (
            <button
              onClick={onLoadMore}
              className="btn btn-secondary"
            >
              Cargar más
            </button>
          )}
        </div>
      )}

      {/* End message */}
      {!hasNextPage && notifications.length > 0 && (
        <div className="text-center py-6 text-gray-500 text-sm">
          No hay más notificaciones
        </div>
      )}
    </div>
  );
}
