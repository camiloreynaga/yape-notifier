/**
 * Componente para mostrar las últimas notificaciones
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { ExternalLink, Eye } from 'lucide-react';
import { apiService } from '@/services/api';
import type { Notification } from '@/types';

export default function RecentNotifications() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadNotifications = async () => {
      setLoading(true);
      try {
        const data = await apiService.getNotifications({
          per_page: 10,
          page: 1,
        });
        setNotifications(data.data);
      } catch (error) {
        console.error('Error loading notifications:', error);
      } finally {
        setLoading(false);
      }
    };

    loadNotifications();
  }, []);

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

  if (loading) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Últimas Notificaciones</h3>
        <div className="animate-pulse space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-16 bg-gray-200 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (notifications.length === 0) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Últimas Notificaciones</h3>
        <div className="text-center py-8">
          <p className="text-gray-500">No hay notificaciones recientes</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Últimas Notificaciones</h3>
        <button
          onClick={() => navigate('/dashboard?tab=notifications')}
          className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
        >
          Ver todas
          <ExternalLink className="h-3 w-3" />
        </button>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Fecha/Hora
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                App
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Instancia
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Dispositivo
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Monto
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Estado
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Acciones
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {notifications.map((notification) => (
              <tr key={notification.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                  {format(new Date(notification.received_at), 'dd/MM/yyyy HH:mm')}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                  {notification.source_app || 'N/A'}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                  {notification.app_instance?.instance_label ||
                    (notification.android_user_id
                      ? `${notification.package_name} (User ${notification.android_user_id})`
                      : 'N/A')}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                  {notification.device?.name || 'N/A'}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                  {notification.amount != null && !isNaN(Number(notification.amount))
                    ? `${notification.currency || 'S/'} ${Number(notification.amount).toFixed(2)}`
                    : 'N/A'}
                </td>
                <td className="px-4 py-3 whitespace-nowrap">
                  {getStatusBadge(notification.status)}
                  {notification.is_duplicate && (
                    <span className="ml-2 badge badge-warning">Duplicado</span>
                  )}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => navigate(`/notifications/${notification.id}`)}
                    className="btn btn-secondary btn-sm flex items-center gap-1"
                    title="Ver detalle"
                  >
                    <Eye className="h-3 w-3" />
                    Ver
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

