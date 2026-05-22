import { useState, useCallback, useMemo, useEffect } from 'react';
import { apiService } from '@/services/api';
import { useDevices } from '@/hooks/useDevices';
import { useToast } from '@/hooks/useToast';
import type { DeviceLog, DeviceLogsSummary, PaginatedResponse } from '@/types';
import { format, formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import {
  FileText,
  RefreshCw,
  AlertTriangle,
  AlertCircle,
  Info,
  XCircle,
  Smartphone,
  Clock,
  Filter,
  ChevronDown,
  ChevronUp,
  Wifi,
  WifiOff,
} from 'lucide-react';
import EmptyState from '@/components/EmptyState';
import Pagination from '@/components/Pagination';

interface LogFilters {
  device_id?: number;
  category?: string;
  level?: string;
  hours?: number;
  per_page: number;
  page: number;
}

const CATEGORIES = [
  { value: '', label: 'Todas' },
  { value: 'SERVICE', label: 'Servicio' },
  { value: 'DISCONNECT', label: 'Desconexion' },
  { value: 'RECONNECT', label: 'Reconexion' },
  { value: 'ERROR', label: 'Error' },
  { value: 'HEALTH', label: 'Salud' },
  { value: 'SYSTEM', label: 'Sistema' },
];

const LEVELS = [
  { value: '', label: 'Todos' },
  { value: 'debug', label: 'Debug' },
  { value: 'info', label: 'Info' },
  { value: 'warning', label: 'Warning' },
  { value: 'error', label: 'Error' },
  { value: 'critical', label: 'Critical' },
];

const TIME_RANGES = [
  { value: 1, label: 'Ultima hora' },
  { value: 6, label: 'Ultimas 6 horas' },
  { value: 24, label: 'Ultimas 24 horas' },
  { value: 48, label: 'Ultimos 2 dias' },
  { value: 168, label: 'Ultima semana' },
];

export default function DeviceLogsPage() {
  const toast = useToast();
  const [filters, setFilters] = useState<LogFilters>({
    hours: 24,
    per_page: 50,
    page: 1,
  });
  const [showFilters, setShowFilters] = useState(false);
  const [logs, setLogs] = useState<PaginatedResponse<DeviceLog> | null>(null);
  const [summary, setSummary] = useState<DeviceLogsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [expandedLogs, setExpandedLogs] = useState<Set<number>>(new Set());

  const { data: devices = [] } = useDevices(false, true);

  const loadLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getCommerceLogs({
        ...filters,
        category: filters.category || undefined,
        level: filters.level || undefined,
        device_id: filters.device_id || undefined,
      });
      setLogs(data);
    } catch (err) {
      setError(err as Error);
      toast.showError('Error al cargar los logs');
    } finally {
      setLoading(false);
    }
  }, [filters, toast]);

  const loadSummary = useCallback(async () => {
    setLoadingSummary(true);
    try {
      const data = await apiService.getDeviceLogsSummary(filters.hours || 24);
      setSummary(data);
    } catch {
      // Silently fail for summary
    } finally {
      setLoadingSummary(false);
    }
  }, [filters.hours]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  const handleFilterChange = useCallback(
    (key: keyof LogFilters, value: string | number | undefined) => {
      setFilters((prev) => ({
        ...prev,
        [key]: value,
        page: key !== 'page' ? 1 : (value as number),
      }));
    },
    []
  );

  const clearFilters = useCallback(() => {
    setFilters({ hours: 24, per_page: 50, page: 1 });
  }, []);

  const toggleLogExpanded = (logId: number) => {
    setExpandedLogs((prev) => {
      const next = new Set(prev);
      if (next.has(logId)) {
        next.delete(logId);
      } else {
        next.add(logId);
      }
      return next;
    });
  };

  const getLevelIcon = (level: string) => {
    switch (level) {
      case 'critical':
        return <XCircle className="h-4 w-4 text-red-600" />;
      case 'error':
        return <AlertCircle className="h-4 w-4 text-red-500" />;
      case 'warning':
        return <AlertTriangle className="h-4 w-4 text-yellow-500" />;
      case 'info':
        return <Info className="h-4 w-4 text-blue-500" />;
      default:
        return <Info className="h-4 w-4 text-gray-400" />;
    }
  };

  const getLevelBadge = (level: string) => {
    const styles: Record<string, string> = {
      critical: 'bg-red-100 text-red-800 border-red-200',
      error: 'bg-red-50 text-red-700 border-red-100',
      warning: 'bg-yellow-50 text-yellow-700 border-yellow-100',
      info: 'bg-blue-50 text-blue-700 border-blue-100',
      debug: 'bg-gray-50 text-gray-600 border-gray-100',
    };
    return (
      <span
        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${styles[level] || styles.debug}`}
      >
        {level}
      </span>
    );
  };

  const getCategoryBadge = (category: string) => {
    const styles: Record<string, string> = {
      SERVICE: 'bg-purple-100 text-purple-800',
      DISCONNECT: 'bg-red-100 text-red-800',
      RECONNECT: 'bg-green-100 text-green-800',
      ERROR: 'bg-red-100 text-red-800',
      HEALTH: 'bg-blue-100 text-blue-800',
      SYSTEM: 'bg-gray-100 text-gray-800',
    };
    return (
      <span
        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${styles[category] || 'bg-gray-100 text-gray-800'}`}
      >
        {category}
      </span>
    );
  };

  const getRelativeTime = (date: string | null): string => {
    if (!date) return 'N/A';
    try {
      return formatDistanceToNow(new Date(date), {
        addSuffix: true,
        locale: es,
      });
    } catch {
      return format(new Date(date), 'dd/MM/yyyy HH:mm');
    }
  };

  // Stats from summary
  const stats = useMemo(() => {
    if (!summary) {
      return { total: 0, devicesWithIssues: 0, disconnects: 0, errors: 0 };
    }
    const devicesWithIssues = summary.devices.filter(
      (d) => d.has_disconnects || d.has_errors
    ).length;
    const totalLogs = summary.devices.reduce((acc, d) => acc + d.total, 0);
    const disconnects = summary.devices.reduce(
      (acc, d) => acc + (d.counts['DISCONNECT'] || 0),
      0
    );
    const errors = summary.devices.reduce(
      (acc, d) => acc + (d.counts['ERROR'] || 0),
      0
    );
    return { total: totalLogs, devicesWithIssues, disconnects, errors };
  }, [summary]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-700 via-slate-800 to-gray-900 p-6 shadow-xl">
        <div className="absolute inset-0 bg-grid-white/5 [mask-image:linear-gradient(0deg,white,rgba(255,255,255,0.5))]" />
        <div className="absolute -right-4 -top-4 h-24 w-24 rounded-full bg-white/10 blur-2xl" />
        <div className="absolute -left-4 -bottom-4 h-32 w-32 rounded-full bg-white/10 blur-3xl" />

        <div className="relative z-10">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
            <div className="space-y-4 flex-1">
              <div className="flex items-center gap-3">
                <FileText className="h-8 w-8 text-white" />
                <h1 className="text-3xl font-bold text-white">
                  Logs de Dispositivos
                </h1>
              </div>

              {/* Stats */}
              <div className="flex flex-wrap gap-3">
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-gray-300 font-medium">
                    Total Logs
                  </div>
                  <div className="text-2xl font-bold text-white">
                    {loadingSummary ? '...' : stats.total}
                  </div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-gray-300 font-medium">
                    Dispositivos con Alertas
                  </div>
                  <div className="text-2xl font-bold text-red-300">
                    {loadingSummary ? '...' : stats.devicesWithIssues}
                  </div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-gray-300 font-medium">
                    Desconexiones
                  </div>
                  <div className="text-2xl font-bold text-yellow-300">
                    {loadingSummary ? '...' : stats.disconnects}
                  </div>
                </div>
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-sm border border-white/20">
                  <div className="text-xs text-gray-300 font-medium">
                    Errores
                  </div>
                  <div className="text-2xl font-bold text-red-400">
                    {loadingSummary ? '...' : stats.errors}
                  </div>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => {
                  loadLogs();
                  loadSummary();
                }}
                className="px-4 py-2 rounded-lg bg-white/10 backdrop-blur-sm border border-white/20 text-white hover:bg-white/20 transition-all duration-200 flex items-center gap-2 text-sm font-medium"
              >
                <RefreshCw
                  className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`}
                />
                <span className="hidden sm:inline">Actualizar</span>
              </button>
              <button
                onClick={() => setShowFilters(!showFilters)}
                className={`px-4 py-2 rounded-lg backdrop-blur-sm border text-sm font-medium transition-all duration-200 flex items-center gap-2 ${
                  showFilters
                    ? 'bg-white text-slate-700 border-white shadow-sm'
                    : 'bg-white/10 border-white/20 text-white hover:bg-white/20'
                }`}
              >
                <Filter className="h-4 w-4" />
                <span className="hidden sm:inline">Filtros</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Device Summary Cards */}
      {summary && summary.devices.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {summary.devices.map((deviceSummary) => (
            <div
              key={deviceSummary.device.id}
              className={`card p-4 cursor-pointer hover:shadow-md transition-shadow ${
                deviceSummary.has_errors || deviceSummary.has_disconnects
                  ? 'border-l-4 border-l-red-500'
                  : 'border-l-4 border-l-green-500'
              }`}
              onClick={() =>
                handleFilterChange('device_id', deviceSummary.device.id)
              }
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Smartphone className="h-4 w-4 text-gray-500" />
                  <span className="font-medium text-gray-900 truncate max-w-[120px]">
                    {deviceSummary.device.alias || deviceSummary.device.name}
                  </span>
                </div>
                {deviceSummary.has_disconnects || deviceSummary.has_errors ? (
                  <WifiOff className="h-4 w-4 text-red-500" />
                ) : (
                  <Wifi className="h-4 w-4 text-green-500" />
                )}
              </div>
              <div className="text-sm text-gray-600">
                <span className="font-semibold">{deviceSummary.total}</span>{' '}
                logs
              </div>
              <div className="flex flex-wrap gap-1 mt-2">
                {Object.entries(deviceSummary.counts).map(([cat, count]) => (
                  <span
                    key={cat}
                    className={`text-xs px-1.5 py-0.5 rounded ${
                      cat === 'DISCONNECT' || cat === 'ERROR'
                        ? 'bg-red-100 text-red-700'
                        : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {cat}: {count}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Quick Filters */}
      <div className="flex flex-wrap gap-2">
        {TIME_RANGES.map((range) => (
          <button
            key={range.value}
            onClick={() => handleFilterChange('hours', range.value)}
            className={`px-3 py-1.5 rounded-full text-sm transition-colors ${
              filters.hours === range.value
                ? 'bg-slate-700 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <Clock className="h-3 w-3 inline mr-1" />
            {range.label}
          </button>
        ))}
        {filters.device_id && (
          <button
            onClick={() => handleFilterChange('device_id', undefined)}
            className="px-3 py-1.5 rounded-full text-sm bg-blue-100 text-blue-700 hover:bg-blue-200 flex items-center gap-1"
          >
            <Smartphone className="h-3 w-3" />
            Dispositivo filtrado
            <XCircle className="h-3 w-3 ml-1" />
          </button>
        )}
      </div>

      {/* Advanced Filters */}
      {showFilters && (
        <div className="card bg-white border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-gray-900">Filtros Avanzados</h3>
            <button
              onClick={clearFilters}
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              Limpiar filtros
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Dispositivo
              </label>
              <select
                value={filters.device_id || ''}
                onChange={(e) =>
                  handleFilterChange(
                    'device_id',
                    e.target.value ? parseInt(e.target.value) : undefined
                  )
                }
                className="input"
              >
                <option value="">Todos</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.alias || device.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Categoria
              </label>
              <select
                value={filters.category || ''}
                onChange={(e) =>
                  handleFilterChange('category', e.target.value || undefined)
                }
                className="input"
              >
                {CATEGORIES.map((cat) => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Nivel
              </label>
              <select
                value={filters.level || ''}
                onChange={(e) =>
                  handleFilterChange('level', e.target.value || undefined)
                }
                className="input"
              >
                {LEVELS.map((lvl) => (
                  <option key={lvl.value} value={lvl.value}>
                    {lvl.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rango de Tiempo
              </label>
              <select
                value={filters.hours || 24}
                onChange={(e) =>
                  handleFilterChange('hours', parseInt(e.target.value))
                }
                className="input"
              >
                {TIME_RANGES.map((range) => (
                  <option key={range.value} value={range.value}>
                    {range.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="card bg-red-50 border-l-4 border-red-400 p-4">
          <p className="text-sm text-red-700">
            Error al cargar logs: {error.message}
          </p>
        </div>
      )}

      {/* Logs Table */}
      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-600"></div>
        </div>
      ) : logs && logs.data.length > 0 ? (
        <div className="card p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">
                    Fecha
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">
                    Dispositivo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">
                    Categoria
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">
                    Nivel
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-700 uppercase">
                    Mensaje
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-semibold text-gray-700 uppercase">
                    Detalles
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {logs.data.map((log) => (
                  <>
                    <tr
                      key={log.id}
                      className={`hover:bg-gray-50 transition-colors ${
                        log.level === 'critical' || log.level === 'error'
                          ? 'bg-red-50/50'
                          : ''
                      }`}
                    >
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {format(new Date(log.created_at), 'dd/MM HH:mm:ss')}
                        </div>
                        <div className="text-xs text-gray-500">
                          {getRelativeTime(log.created_at)}
                        </div>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <Smartphone className="h-4 w-4 text-gray-400" />
                          <span className="text-sm text-gray-900">
                            {log.device?.alias || log.device?.name || 'N/A'}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        {getCategoryBadge(log.category)}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="flex items-center gap-1">
                          {getLevelIcon(log.level)}
                          {getLevelBadge(log.level)}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="text-sm text-gray-900 max-w-md truncate">
                          {log.message}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {log.details && (
                          <button
                            onClick={() => toggleLogExpanded(log.id)}
                            className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                          >
                            {expandedLogs.has(log.id) ? (
                              <ChevronUp className="h-4 w-4" />
                            ) : (
                              <ChevronDown className="h-4 w-4" />
                            )}
                          </button>
                        )}
                      </td>
                    </tr>
                    {expandedLogs.has(log.id) && log.details && (
                      <tr key={`${log.id}-details`}>
                        <td colSpan={6} className="px-4 py-3 bg-gray-50">
                          <div className="text-xs font-mono text-gray-700 whitespace-pre-wrap bg-gray-100 p-3 rounded-lg overflow-x-auto">
                            {log.details}
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {logs.last_page > 1 && (
            <Pagination
              currentPage={logs.current_page}
              totalPages={logs.last_page}
              onPageChange={(page) => handleFilterChange('page', page)}
              from={logs.from}
              to={logs.to}
              total={logs.total}
              className="border-t border-gray-200"
            />
          )}
        </div>
      ) : (
        <EmptyState
          icon={<FileText className="w-16 h-16 text-gray-400" />}
          title="No hay logs disponibles"
          message={
            filters.device_id || filters.category || filters.level
              ? 'No se encontraron logs con los filtros seleccionados.'
              : 'Los logs de dispositivos apareceran aqui cuando los dispositivos envien eventos.'
          }
          action={
            filters.device_id || filters.category || filters.level
              ? { label: 'Limpiar filtros', onClick: clearFilters }
              : undefined
          }
        />
      )}
    </div>
  );
}
