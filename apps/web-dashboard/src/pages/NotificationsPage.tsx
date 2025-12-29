import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import { useNotifications } from '@/hooks/useNotifications';
import { useToast } from '@/hooks/useToast';
import { logger } from '@/services/logger';
import type { Notification, NotificationFilters, Device, AppInstance } from '@/types';
import { format } from 'date-fns';
import { Download, Filter, Eye, RefreshCw, Calendar, X, Inbox, Search } from 'lucide-react';
import WebSocketStatus from '@/components/WebSocketStatus';
import EmptyState from '@/components/EmptyState';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [filters, setFilters] = useState<NotificationFilters>({
    per_page: 50,
    page: 1,
  });
  const [showFilters, setShowFilters] = useState(false);
  const [devices, setDevices] = useState<Device[]>([]);
  const [appInstances, setAppInstances] = useState<AppInstance[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  // Callback para manejar nuevas notificaciones (ya manejado por WebSocket)
  const handleNewNotification = useCallback((notification: Notification) => {
    logger.debug('Nueva notificación recibida en página', { notificationId: notification.id });
    // El toast se maneja automáticamente por NotificationToastContainer
  }, []);

  // Usar el hook con WebSockets
  const {
    notifications,
    loading,
    error,
    refetch,
  } = useNotifications({
    filters,
    enabled: true,
    onNewNotification: handleNewNotification,
  });

  useEffect(() => {
    loadDevices();
    loadAppInstances();
  }, [loadDevices, loadAppInstances]);

  const loadDevices = useCallback(async () => {
    try {
      const deviceList = await apiService.getDevices();
      setDevices(deviceList);
    } catch (error) {
      logger.error('Error loading devices', error as Error, { source: 'NotificationsPage' });
      toast.showError('Error al cargar dispositivos');
    }
  }, [toast]);

  const loadAppInstances = useCallback(async () => {
    try {
      const instances = await apiService.getAppInstances();
      setAppInstances(instances);
    } catch (error) {
      logger.error('Error loading app instances', error as Error, { source: 'NotificationsPage' });
      // No mostrar toast para errores silenciosos de carga inicial
    }
  }, []);

  const handleFilterChange = (key: keyof NotificationFilters, value: string | number | boolean | undefined) => {
    setFilters({ ...filters, [key]: value, page: 1 });
  };

  const clearFilters = () => {
    setFilters({ per_page: 50, page: 1 });
    setSearchQuery('');
  };

  const handleStatusChange = async (id: number, status: 'pending' | 'validated' | 'inconsistent') => {
    try {
      await apiService.updateNotificationStatus(id, status);
      refetch();
      toast.showSuccess('Estado actualizado correctamente');
    } catch (error: unknown) {
      logger.error('Error updating notification status', error as Error, { notificationId: id, status });
      toast.showError('Error al actualizar el estado');
    }
  };

  const exportToCSV = () => {
    if (!notifications || notifications.data.length === 0) {
      toast.showWarning('No hay datos para exportar');
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

    const rows = notifications.data.map((n: Notification) => [
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
      ...rows.map((row: (string | number)[]) => row.map((cell: string | number) => `"${cell}"`).join(',')),
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

  // Filtros rápidos tipo chips
  const quickFilters = [
    { key: 'all', label: 'Todos', value: undefined },
    { key: 'today', label: 'Hoy', value: format(new Date(), 'yyyy-MM-dd') },
    { key: 'pending', label: 'Pendientes', value: 'pending' },
  ];

  const handleQuickFilter = (filterKey: string) => {
    if (filterKey === 'all') {
      clearFilters();
    } else if (filterKey === 'today') {
      setFilters({ ...filters, start_date: format(new Date(), 'yyyy-MM-dd'), page: 1 });
    } else if (filterKey === 'pending') {
      setFilters({ ...filters, status: 'pending', page: 1 });
    }
  };

  const activeQuickFilter = 
    !filters.status && !filters.start_date ? 'all' :
    filters.start_date === format(new Date(), 'yyyy-MM-dd') ? 'today' :
    filters.status === 'pending' ? 'pending' : '';

  return (
    <div className="space-y-4">
      {/* Header compacto y responsive */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Notificaciones</h1>
          <WebSocketStatus />
        </div>
        <div className="flex flex-wrap gap-2 w-full sm:w-auto">
          <button
            onClick={() => refetch()}
            className="btn btn-secondary flex items-center gap-2 text-sm"
            title="Actualizar manualmente"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">Actualizar</span>
          </button>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`btn btn-secondary flex items-center gap-2 text-sm ${showFilters ? 'bg-primary-100 text-primary-700' : ''}`}
          >
            <Filter className="h-4 w-4" />
            <span className="hidden sm:inline">Filtros</span>
          </button>
          <button
            onClick={exportToCSV}
            className="btn btn-primary flex items-center gap-2 text-sm"
          >
            <Download className="h-4 w-4" />
            <span className="hidden sm:inline">Exportar</span>
          </button>
        </div>
      </div>

      {/* Filtros rápidos y búsqueda en una sola fila */}
      <div className="flex flex-col sm:flex-row gap-3">
        {/* Filtros rápidos tipo chips */}
        <div className="flex gap-2 overflow-x-auto pb-2 flex-1">
          {quickFilters.map((filter) => (
            <button
              key={filter.key}
              onClick={() => handleQuickFilter(filter.key)}
              className={`
                px-3 sm:px-4 py-1.5 sm:py-2 rounded-full flex items-center gap-2 whitespace-nowrap text-sm
                transition-colors flex-shrink-0
                ${
                  activeQuickFilter === filter.key
                    ? 'bg-primary-600 text-white shadow-sm'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }
              `}
            >
              {filter.key === 'today' && <Calendar className="w-3.5 h-3.5 sm:w-4 sm:h-4" />}
              {filter.label}
            </button>
          ))}
        </div>

        {/* Búsqueda compacta */}
        <div className="relative flex-shrink-0 sm:w-64">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
            }}
            placeholder="Buscar..."
            className="w-full pl-9 pr-9 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          {searchQuery && (
            <button
              onClick={() => {
                setSearchQuery('');
              }}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <X className="w-4 h-4" />
            </button>
          )}
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

      {/* Error message */}
      {error && (
        <div className="card bg-red-50 border-l-4 border-red-400">
          <div className="flex">
            <div className="ml-3">
              <p className="text-sm text-red-700">
                Error al cargar notificaciones: {error.message}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Notifications Table - Mejorado */}
      <div className="card p-0 overflow-hidden">
        {loading && !notifications ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          </div>
        ) : notifications && notifications.data.length > 0 ? (
          <>
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
                  {notifications.data.map((notification: Notification) => (
                    <tr 
                      key={notification.id} 
                      className="hover:bg-gray-50 transition-colors cursor-pointer"
                      onClick={() => navigate(`/notifications/${notification.id}`)}
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
                            onClick={() => navigate(`/notifications/${notification.id}`)}
                            className="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
                            title="Ver detalle"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          <select
                            value={notification.status}
                            onChange={(e) =>
                              handleStatusChange(
                                notification.id,
                                e.target.value as 'pending' | 'validated' | 'inconsistent'
                              )
                            }
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
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination - Mejorado */}
            {notifications.last_page > 1 && (
              <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="text-sm text-gray-700">
                  Mostrando <span className="font-medium">{notifications.from}</span> a{' '}
                  <span className="font-medium">{notifications.to}</span> de{' '}
                  <span className="font-medium">{notifications.total}</span> resultados
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleFilterChange('page', notifications.current_page - 1)}
                    disabled={notifications.current_page === 1}
                    className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    Anterior
                  </button>
                  <span className="px-4 py-1.5 text-sm text-gray-700">
                    Página <span className="font-semibold">{notifications.current_page}</span> de{' '}
                    <span className="font-semibold">{notifications.last_page}</span>
                  </span>
                  <button
                    onClick={() => handleFilterChange('page', notifications.current_page + 1)}
                    disabled={notifications.current_page === notifications.last_page}
                    className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    Siguiente
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <EmptyState
            icon={<Inbox className="w-16 h-16 text-gray-400" />}
            title="No se encontraron notificaciones"
            message={
              Object.keys(filters).length > 2
                ? "No hay notificaciones que coincidan con los filtros seleccionados. Intenta ajustar los filtros."
                : "Aún no has recibido notificaciones. Las notificaciones aparecerán aquí cuando lleguen."
            }
            action={
              Object.keys(filters).length > 2
                ? {
                    label: "Limpiar filtros",
                    onClick: clearFilters,
                  }
                : undefined
            }
          />
        )}
      </div>
    </div>
  );
}
