import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import type { Notification, NotificationFilters, PaginatedResponse, Device, AppInstance } from '@/types';
import { format } from 'date-fns';
import { Download, Filter, Eye } from 'lucide-react';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<PaginatedResponse<Notification> | null>(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<NotificationFilters>({
    per_page: 50,
    page: 1,
  });
  const [showFilters, setShowFilters] = useState(false);
  const [devices, setDevices] = useState<Device[]>([]);
  const [appInstances, setAppInstances] = useState<AppInstance[]>([]);

  useEffect(() => {
    loadDevices();
    loadAppInstances();
  }, []);

  useEffect(() => {
    loadNotifications();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  const loadDevices = async () => {
    try {
      const deviceList = await apiService.getDevices();
      setDevices(deviceList);
    } catch (error) {
      console.error('Error loading devices:', error);
    }
  };

  const loadAppInstances = async () => {
    try {
      const instances = await apiService.getAppInstances();
      setAppInstances(instances);
    } catch (error) {
      console.error('Error loading app instances:', error);
    }
  };

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await apiService.getNotifications(filters);
      setNotifications(data);
    } catch (error: unknown) {
      console.error('Error loading notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key: keyof NotificationFilters, value: string | number | boolean | undefined) => {
    setFilters({ ...filters, [key]: value, page: 1 });
  };

  const clearFilters = () => {
    setFilters({ per_page: 50, page: 1 });
  };

  const handleStatusChange = async (id: number, status: 'pending' | 'validated' | 'inconsistent') => {
    try {
      await apiService.updateNotificationStatus(id, status);
      loadNotifications();
    } catch (error: unknown) {
      console.error('Error updating status:', error);
      alert('Error al actualizar el estado');
    }
  };

  const exportToCSV = () => {
    if (!notifications || notifications.data.length === 0) {
      alert('No hay datos para exportar');
      return;
    }

    const headers = [
      'ID',
      'Fecha',
      'Aplicación',
      'Instancia',
      'Dispositivo',
      'Título',
      'Monto',
      'Moneda',
      'Pagador',
      'Estado',
      'Duplicado',
    ];

    const rows = notifications.data.map((n) => [
      n.id,
      format(new Date(n.received_at), 'yyyy-MM-dd HH:mm:ss'),
      n.source_app || 'N/A',
      n.app_instance?.instance_label || (n.android_user_id ? `${n.package_name} (User ${n.android_user_id})` : 'N/A'),
      n.device?.name || 'N/A',
      n.title,
      n.amount || '0',
      n.currency || 'PEN',
      n.payer_name || 'N/A',
      n.status,
      n.is_duplicate ? 'Sí' : 'No',
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map((row) => row.map((cell) => `"${cell}"`).join(',')),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `notificaciones_${format(new Date(), 'yyyy-MM-dd')}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

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
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Notificaciones</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="btn btn-secondary flex items-center gap-2"
          >
            <Filter className="h-4 w-4" />
            Filtros
          </button>
          <button
            onClick={exportToCSV}
            className="btn btn-primary flex items-center gap-2"
          >
            <Download className="h-4 w-4" />
            Exportar CSV
          </button>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="card">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold">Filtros</h2>
            <button
              onClick={clearFilters}
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              Limpiar filtros
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="filter-device" className="block text-sm font-medium text-gray-700 mb-2">
                Dispositivo
              </label>
              <select
                id="filter-device"
                value={filters.device_id || ''}
                onChange={(e) =>
                  handleFilterChange('device_id', e.target.value ? parseInt(e.target.value) : undefined)
                }
                className="input"
              >
                <option value="">Todos</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="filter-app" className="block text-sm font-medium text-gray-700 mb-2">
                Aplicación
              </label>
              <select
                id="filter-app"
                value={filters.source_app || ''}
                onChange={(e) => handleFilterChange('source_app', e.target.value || undefined)}
                className="input"
              >
                <option value="">Todas</option>
                <option value="yape">Yape</option>
                <option value="plin">Plin</option>
                <option value="bcp">BCP</option>
                <option value="interbank">Interbank</option>
                <option value="bbva">BBVA</option>
                <option value="scotiabank">Scotiabank</option>
              </select>
            </div>
            <div>
              <label htmlFor="filter-instance" className="block text-sm font-medium text-gray-700 mb-2">
                Instancia (Dual Apps)
              </label>
              <select
                id="filter-instance"
                value={filters.app_instance_id || ''}
                onChange={(e) =>
                  handleFilterChange('app_instance_id', e.target.value ? parseInt(e.target.value) : undefined)
                }
                className="input"
              >
                <option value="">Todas</option>
                {appInstances.map((instance) => (
                  <option key={instance.id} value={instance.id}>
                    {instance.instance_label || `${instance.package_name} (User ${instance.android_user_id})`}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="filter-status" className="block text-sm font-medium text-gray-700 mb-2">
                Estado
              </label>
              <select
                id="filter-status"
                value={filters.status || ''}
                onChange={(e) =>
                  handleFilterChange('status', e.target.value || undefined)
                }
                className="input"
              >
                <option value="">Todos</option>
                <option value="pending">Pendiente</option>
                <option value="validated">Validado</option>
                <option value="inconsistent">Inconsistente</option>
              </select>
            </div>
            <div>
              <label htmlFor="filter-start-date" className="block text-sm font-medium text-gray-700 mb-2">
                Fecha Inicio
              </label>
              <input
                id="filter-start-date"
                type="date"
                value={filters.start_date || ''}
                onChange={(e) => handleFilterChange('start_date', e.target.value || undefined)}
                className="input"
              />
            </div>
            <div>
              <label htmlFor="filter-end-date" className="block text-sm font-medium text-gray-700 mb-2">
                Fecha Fin
              </label>
              <input
                id="filter-end-date"
                type="date"
                value={filters.end_date || ''}
                onChange={(e) => handleFilterChange('end_date', e.target.value || undefined)}
                className="input"
              />
            </div>
            <div className="flex items-end">
              <label htmlFor="filter-exclude-duplicates" className="flex items-center">
                <input
                  id="filter-exclude-duplicates"
                  type="checkbox"
                  checked={filters.exclude_duplicates || false}
                  onChange={(e) =>
                    handleFilterChange('exclude_duplicates', e.target.checked || undefined)
                  }
                  className="mr-2"
                />
                <span className="text-sm text-gray-700">Excluir duplicados</span>
              </label>
            </div>
          </div>
        </div>
      )}

      {/* Notifications Table */}
      <div className="card">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          </div>
        ) : notifications && notifications.data.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Fecha
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Aplicación
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Instancia
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Dispositivo
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Título
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Monto
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Pagador
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Estado
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {notifications.data.map((notification) => (
                    <tr key={notification.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {format(new Date(notification.received_at), 'dd/MM/yyyy HH:mm')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {notification.source_app || 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {notification.app_instance?.instance_label || 
                         (notification.android_user_id ? `${notification.package_name} (User ${notification.android_user_id})` : 'N/A')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {notification.device?.name || 'N/A'}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900 max-w-xs truncate">
                        {notification.title}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {notification.amount != null && !isNaN(Number(notification.amount))
                          ? `${notification.currency || 'S/'} ${Number(notification.amount).toFixed(2)}`
                          : 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {notification.payer_name || 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(notification.status)}
                        {notification.is_duplicate && (
                          <span className="ml-2 badge badge-warning">Duplicado</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => navigate(`/notifications/${notification.id}`)}
                            className="btn btn-secondary btn-sm flex items-center gap-1"
                            title="Ver detalle"
                          >
                            <Eye className="h-3 w-3" />
                            Ver
                          </button>
                          <select
                            value={notification.status}
                            onChange={(e) =>
                              handleStatusChange(
                                notification.id,
                                e.target.value as 'pending' | 'validated' | 'inconsistent'
                              )
                            }
                            className="text-xs border border-gray-300 rounded px-2 py-1"
                          >
                            <option value="pending">Pendiente</option>
                            <option value="validated">Validado</option>
                            <option value="inconsistent">Inconsistente</option>
                          </select>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {notifications.last_page > 1 && (
              <div className="mt-4 flex items-center justify-between">
                <div className="text-sm text-gray-700">
                  Mostrando {notifications.from} a {notifications.to} de {notifications.total} resultados
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleFilterChange('page', notifications.current_page - 1)}
                    disabled={notifications.current_page === 1}
                    className="btn btn-secondary disabled:opacity-50"
                  >
                    Anterior
                  </button>
                  <span className="flex items-center px-4">
                    Página {notifications.current_page} de {notifications.last_page}
                  </span>
                  <button
                    onClick={() => handleFilterChange('page', notifications.current_page + 1)}
                    disabled={notifications.current_page === notifications.last_page}
                    className="btn btn-secondary disabled:opacity-50"
                  >
                    Siguiente
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-12">
            <p className="text-gray-500">No se encontraron notificaciones</p>
          </div>
        )}
      </div>
    </div>
  );
}

