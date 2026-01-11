import { useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import { useNotifications } from '@/hooks/useNotifications';
import { useDevices } from '@/hooks/useDevices';
import { useAppInstances } from '@/hooks/useAppInstances';
import { useToast } from '@/hooks/useToast';
import { useDebouncedValue } from '@/hooks/useDebounce';
import { logger } from '@/services/logger';
import type { Notification, NotificationFilters } from '@/types';
import { format } from 'date-fns';
import { Download, Eye, RefreshCw, Calendar, X, Inbox, Search, Grid3x3, List, SlidersHorizontal, Loader2 } from 'lucide-react';
import WebSocketStatus from '@/components/WebSocketStatus';
import EmptyState from '@/components/EmptyState';
import NotificationCard from '@/components/NotificationCard';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [filters, setFilters] = useState<NotificationFilters>({
    per_page: 50,
    page: 1,
  });
  const [showFilters, setShowFilters] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [updatingStatus, setUpdatingStatus] = useState<number | null>(null);
  const [exporting, setExporting] = useState(false);

  // Debounced search query (espera 300ms después de que el usuario deja de escribir)
  const { debouncedValue: debouncedSearchQuery, isDebouncing } = useDebouncedValue(searchQuery, 300);

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

  // Usar React Query hooks para devices y app instances
  // Solo cargar cuando se necesitan (cuando se muestran los filtros)
  const {
    data: devices = [],
    isLoading: devicesLoading,
    error: devicesError
  } = useDevices(false, showFilters);

  const {
    data: appInstances = [],
    isLoading: appInstancesLoading,
    error: appInstancesError
  } = useAppInstances(undefined, showFilters);

  // Mostrar errores de carga de filtros si existen
  if (devicesError) {
    logger.error('Error loading devices', devicesError, { source: 'NotificationsPage' });
  }
  if (appInstancesError) {
    logger.error('Error loading app instances', appInstancesError, { source: 'NotificationsPage' });
  }

  const handleFilterChange = useCallback((key: keyof NotificationFilters, value: string | number | boolean | undefined) => {
    setFilters((prev) => ({ ...prev, [key]: value, page: 1 }));
  }, []);

  const clearFilters = useCallback(() => {
    setFilters({ per_page: 50, page: 1 });
    setSearchQuery('');
  }, []);

  // Filtrar notificaciones en cliente usando el search query debounced
  // Memoizado para no recalcular en cada render
  const filteredNotifications = useMemo(() => {
    if (!notifications?.data || !debouncedSearchQuery.trim()) {
      return notifications?.data || [];
    }

    const searchLower = debouncedSearchQuery.toLowerCase().trim();

    return notifications.data.filter((notification: Notification) => {
      // Buscar en título
      if (notification.title?.toLowerCase().includes(searchLower)) return true;

      // Buscar en monto
      if (notification.amount?.toString().includes(searchLower)) return true;

      // Buscar en nombre del pagador
      if (notification.payer_name?.toLowerCase().includes(searchLower)) return true;

      // Buscar en app
      if (notification.source_app?.toLowerCase().includes(searchLower)) return true;

      // Buscar en dispositivo
      if (notification.device?.name?.toLowerCase().includes(searchLower)) return true;

      return false;
    });
  }, [notifications?.data, debouncedSearchQuery]);

  const handleStatusChange = async (id: number, status: 'pending' | 'validated' | 'inconsistent') => {
    setUpdatingStatus(id);
    try {
      await apiService.updateNotificationStatus(id, status);
      refetch();
      const statusLabels = { pending: 'pendiente', validated: 'validado', inconsistent: 'inconsistente' };
      toast.showSuccess(`Estado actualizado a "${statusLabels[status]}"`);
    } catch (error: unknown) {
      logger.error('Error updating notification status', error as Error, { notificationId: id, status });
      toast.showError('Error al actualizar el estado');
    } finally {
      setUpdatingStatus(null);
    }
  };

  const exportToCSV = useCallback(() => {
    if (!notifications || notifications.data.length === 0) {
      toast.showWarning('No hay datos para exportar');
      return;
    }

    setExporting(true);

    // Use setTimeout to allow UI to update before processing
    setTimeout(() => {
      try {
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
        toast.showSuccess(`Exportadas ${notifications.data.length} notificaciones`);
      } catch {
        toast.showError('Error al exportar los datos');
      } finally {
        setExporting(false);
      }
    }, 100);
  }, [notifications, toast]);

  const getStatusBadge = useCallback((status: string) => {
    switch (status) {
      case 'validated':
        return <span className="badge badge-success">Validado</span>;
      case 'inconsistent':
        return <span className="badge badge-danger">Inconsistente</span>;
      default:
        return <span className="badge badge-warning">Pendiente</span>;
    }
  }, []);

  // Filtros rápidos tipo chips
  const quickFilters = [
    { key: 'all', label: 'Todos', value: undefined },
    { key: 'today', label: 'Hoy', value: format(new Date(), 'yyyy-MM-dd') },
    { key: 'pending', label: 'Pendientes', value: 'pending' },
  ];

  const handleQuickFilter = useCallback((filterKey: string) => {
    if (filterKey === 'all') {
      clearFilters();
    } else if (filterKey === 'today') {
      setFilters((prev) => ({ ...prev, start_date: format(new Date(), 'yyyy-MM-dd'), page: 1 }));
    } else if (filterKey === 'pending') {
      setFilters((prev) => ({ ...prev, status: 'pending', page: 1 }));
    }
  }, [clearFilters]);

  const activeQuickFilter = useMemo(() =>
    !filters.status && !filters.start_date ? 'all' :
    filters.start_date === format(new Date(), 'yyyy-MM-dd') ? 'today' :
    filters.status === 'pending' ? 'pending' : ''
  , [filters.status, filters.start_date]);

  // Calcular estadísticas para el header
  const stats = useMemo(() => {
    const data = filteredNotifications;
    return {
      total: data.length,
      pending: data.filter((n: Notification) => n.status === 'pending').length,
      validated: data.filter((n: Notification) => n.status === 'validated').length,
      inconsistent: data.filter((n: Notification) => n.status === 'inconsistent').length,
    };
  }, [filteredNotifications]);

  return (
    <div className="space-y-6">
      {/* Header moderno con estadísticas */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary-500 via-primary-600 to-indigo-600 p-6 shadow-xl">
        {/* Patrón decorativo de fondo */}
        <div className="absolute inset-0 bg-grid-white/10 [mask-image:linear-gradient(0deg,white,rgba(255,255,255,0.5))]" />
        <div className="absolute -right-4 -top-4 h-24 w-24 rounded-full bg-white/10 blur-2xl" />
        <div className="absolute -left-4 -bottom-4 h-32 w-32 rounded-full bg-white/10 blur-3xl" />

        <div className="relative z-10">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
            {/* Título y estadísticas */}
            <div className="space-y-4 flex-1">
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-bold text-white">Notificaciones</h1>
                <WebSocketStatus />
              </div>

              {/* Estadísticas rápidas */}
              <div className="flex flex-wrap gap-3">
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-primary-100 font-medium">Total</div>
                  <div className="text-2xl font-bold text-white">{stats.total}</div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-primary-100 font-medium">Pendientes</div>
                  <div className="text-2xl font-bold text-yellow-200">{stats.pending}</div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-primary-100 font-medium">Validadas</div>
                  <div className="text-2xl font-bold text-green-200">{stats.validated}</div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-primary-100 font-medium">Inconsistentes</div>
                  <div className="text-2xl font-bold text-red-200">{stats.inconsistent}</div>
                </div>
              </div>
            </div>

            {/* Acciones y controles */}
            <div className="flex flex-wrap gap-2">
              {/* Toggle de vista */}
              <div className="flex gap-1 bg-white/10 backdrop-blur-sm border border-white/20 rounded-lg p-1">
                <button
                  onClick={() => setViewMode('grid')}
                  className={`px-3 py-2 rounded-md flex items-center gap-2 text-sm font-medium transition-all duration-200 ${
                    viewMode === 'grid'
                      ? 'bg-white text-primary-600 shadow-sm'
                      : 'text-white hover:bg-white/10'
                  }`}
                  title="Vista de cuadrícula"
                >
                  <Grid3x3 className="h-4 w-4" />
                  <span className="hidden sm:inline">Grid</span>
                </button>
                <button
                  onClick={() => setViewMode('list')}
                  className={`px-3 py-2 rounded-md flex items-center gap-2 text-sm font-medium transition-all duration-200 ${
                    viewMode === 'list'
                      ? 'bg-white text-primary-600 shadow-sm'
                      : 'text-white hover:bg-white/10'
                  }`}
                  title="Vista de lista"
                >
                  <List className="h-4 w-4" />
                  <span className="hidden sm:inline">Lista</span>
                </button>
              </div>

              <button
                onClick={() => refetch()}
                className="px-4 py-2 rounded-lg bg-white/10 backdrop-blur-sm border border-white/20 text-white hover:bg-white/20 transition-all duration-200 flex items-center gap-2 text-sm font-medium"
                title="Actualizar manualmente"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                <span className="hidden sm:inline">Actualizar</span>
              </button>
              <button
                onClick={() => setShowFilters(!showFilters)}
                className={`px-4 py-2 rounded-lg backdrop-blur-sm border text-sm font-medium transition-all duration-200 flex items-center gap-2 ${
                  showFilters
                    ? 'bg-white text-primary-600 border-white shadow-sm'
                    : 'bg-white/10 border-white/20 text-white hover:bg-white/20'
                }`}
              >
                <SlidersHorizontal className="h-4 w-4" />
                <span className="hidden sm:inline">Filtros</span>
              </button>
              <button
                onClick={exportToCSV}
                disabled={exporting}
                className="px-4 py-2 rounded-lg bg-white text-primary-600 hover:bg-white/90 transition-all duration-200 flex items-center gap-2 text-sm font-medium shadow-sm disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {exporting ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Download className="h-4 w-4" />
                )}
                <span className="hidden sm:inline">{exporting ? 'Exportando...' : 'Exportar'}</span>
              </button>
            </div>
          </div>
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

        {/* Búsqueda compacta con indicador de debouncing */}
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
          {isDebouncing ? (
            <div className="absolute left-3 top-1/2 transform -translate-y-1/2">
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-primary-600 border-t-transparent"></div>
            </div>
          ) : (
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          )}
          {searchQuery && (
            <button
              onClick={() => {
                setSearchQuery('');
              }}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              title="Limpiar búsqueda"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Filters - Mejorado */}
      {showFilters && (
        <div className="card bg-gradient-to-br from-white via-white to-gray-50/50 border-2 border-primary-100 shadow-lg">
          <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary-100">
                <SlidersHorizontal className="h-5 w-5 text-primary-600" />
              </div>
              <h2 className="text-xl font-bold text-gray-900">Filtros Avanzados</h2>
            </div>
            <button
              onClick={clearFilters}
              className="px-4 py-2 text-sm font-medium text-primary-600 hover:text-primary-700 hover:bg-primary-50 rounded-lg transition-all duration-200 flex items-center gap-2"
            >
              <X className="h-4 w-4" />
              Limpiar filtros
            </button>
          </div>
          {(devicesLoading || appInstancesLoading) && (
            <div className="mb-4 text-sm text-gray-600 flex items-center gap-2">
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-600"></div>
              Cargando opciones de filtros...
            </div>
          )}
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
                disabled={devicesLoading}
              >
                <option value="">Todos</option>
                {devices.map((device: typeof devices[0]) => (
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
                disabled={appInstancesLoading}
              >
                <option value="">Todas</option>
                {appInstances.map((instance: typeof appInstances[0]) => (
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

      {/* Notifications - Grid o Lista */}
      {loading && !notifications ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      ) : filteredNotifications.length > 0 ? (
        <>
          {/* Vista de Grid */}
          {viewMode === 'grid' ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-fade-in">
              {filteredNotifications.map((notification: Notification) => (
                <NotificationCard
                  key={notification.id}
                  notification={notification}
                  onStatusChange={handleStatusChange}
                  onClick={() => navigate(`/notifications/${notification.id}`)}
                  isUpdating={updatingStatus === notification.id}
                />
              ))}
            </div>
          ) : (
            /* Vista de Lista (tabla) */
            <div className="card p-0 overflow-hidden">
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
                    <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider hidden 2xl:table-cell">
                      Código
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
                  {filteredNotifications.map((notification: Notification) => (
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
                      <td className="px-4 py-3 whitespace-nowrap text-center hidden 2xl:table-cell">
                        {notification.security_code ? (
                          <span className="inline-flex items-center px-3 py-1 rounded-lg text-sm font-bold bg-gradient-to-br from-purple-100 to-indigo-100 text-purple-700 border border-purple-200">
                            {notification.security_code}
                          </span>
                        ) : (
                          <span className="text-xs text-gray-400">-</span>
                        )}
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
              {notifications && notifications.last_page > 1 && (
                <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="text-sm text-gray-700">
                  {debouncedSearchQuery ? (
                    <>
                      Mostrando <span className="font-medium">{filteredNotifications.length}</span> de{' '}
                      <span className="font-medium">{notifications.total}</span> resultados
                      <span className="text-primary-600 ml-1">(búsqueda activa)</span>
                    </>
                  ) : (
                    <>
                      Mostrando <span className="font-medium">{notifications.from}</span> a{' '}
                      <span className="font-medium">{notifications.to}</span> de{' '}
                      <span className="font-medium">{notifications.total}</span> resultados
                    </>
                  )}
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
            </div>
          )}

          {/* Paginación para vista de Grid */}
          {viewMode === 'grid' && notifications && notifications.last_page > 1 && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mt-6 p-6 bg-white rounded-xl shadow-sm border border-gray-100">
              <div className="text-sm text-gray-700">
                {debouncedSearchQuery ? (
                  <>
                    Mostrando <span className="font-medium">{filteredNotifications.length}</span> de{' '}
                    <span className="font-medium">{notifications.total}</span> resultados
                    <span className="text-primary-600 ml-1">(búsqueda activa)</span>
                  </>
                ) : (
                  <>
                    Mostrando <span className="font-medium">{notifications.from}</span> a{' '}
                    <span className="font-medium">{notifications.to}</span> de{' '}
                    <span className="font-medium">{notifications.total}</span> resultados
                  </>
                )}
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleFilterChange('page', notifications.current_page - 1)}
                  disabled={notifications.current_page === 1}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
                >
                  Anterior
                </button>
                <span className="px-4 py-2 text-sm text-gray-700">
                  Página <span className="font-semibold">{notifications.current_page}</span> de{' '}
                  <span className="font-semibold">{notifications.last_page}</span>
                </span>
                <button
                  onClick={() => handleFilterChange('page', notifications.current_page + 1)}
                  disabled={notifications.current_page === notifications.last_page}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
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
            debouncedSearchQuery
              ? `No hay notificaciones que coincidan con "${debouncedSearchQuery}". Intenta con otro término de búsqueda.`
              : Object.keys(filters).length > 2
              ? "No hay notificaciones que coincidan con los filtros seleccionados. Intenta ajustar los filtros."
              : "Aún no has recibido notificaciones. Las notificaciones aparecerán aquí cuando lleguen."
          }
          action={
            debouncedSearchQuery || Object.keys(filters).length > 2
              ? {
                  label: debouncedSearchQuery ? "Limpiar búsqueda" : "Limpiar filtros",
                  onClick: clearFilters,
                }
              : undefined
          }
        />
      )}
    </div>
  );
}
