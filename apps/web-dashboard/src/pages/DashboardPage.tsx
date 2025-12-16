import { useEffect, useState } from 'react';
import { apiService } from '@/services/api';
import type { NotificationStatistics } from '@/types';
import { 
  DollarSign, 
  Bell, 
  TrendingUp, 
  AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export default function DashboardPage() {
  const [statistics, setStatistics] = useState<NotificationStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({
    start_date: format(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), 'yyyy-MM-dd'),
    end_date: format(new Date(), 'yyyy-MM-dd'),
  });

  useEffect(() => {
    loadStatistics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dateRange]);

  const loadStatistics = async () => {
    setLoading(true);
    try {
      const stats = await apiService.getStatistics(dateRange);
      setStatistics(stats);
    } catch (error) {
      console.error('Error loading statistics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!statistics) {
    return (
      <div className="card">
        <p className="text-gray-500">No hay datos disponibles</p>
      </div>
    );
  }

  // Prepare chart data
  const dateChartData = Object.entries(statistics.by_date)
    .map(([date, data]) => ({
      date: format(new Date(date), 'dd/MM'),
      cantidad: data.count,
      monto: parseFloat(Number(data.total_amount).toFixed(2)),
    }))
    .sort((a, b) => a.date.localeCompare(b.date));

  const sourceAppData = Object.entries(statistics.by_source_app)
    .map(([source, data]) => ({
      source: source || 'Desconocido',
      cantidad: data.count,
      monto: parseFloat(Number(data.total_amount).toFixed(2)),
    }))
    .sort((a, b) => b.monto - a.monto);

  const statusData = Object.entries(statistics.by_status).map(([status, count]) => ({
    status: status === 'pending' ? 'Pendiente' : status === 'validated' ? 'Validado' : 'Inconsistente',
    cantidad: count,
  }));

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <div className="flex gap-2">
          <input
            type="date"
            value={dateRange.start_date}
            onChange={(e) => setDateRange({ ...dateRange, start_date: e.target.value })}
            className="input"
          />
          <input
            type="date"
            value={dateRange.end_date}
            onChange={(e) => setDateRange({ ...dateRange, end_date: e.target.value })}
            className="input"
          />
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-primary-100 rounded-lg p-3">
              <DollarSign className="h-6 w-6 text-primary-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Total Monto</p>
              <p className="text-2xl font-bold text-gray-900">
                S/ {Number(statistics.total_amount).toFixed(2)}
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-green-100 rounded-lg p-3">
              <Bell className="h-6 w-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Total Notificaciones</p>
              <p className="text-2xl font-bold text-gray-900">{statistics.total}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-yellow-100 rounded-lg p-3">
              <TrendingUp className="h-6 w-6 text-yellow-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Promedio por Día</p>
              <p className="text-2xl font-bold text-gray-900">
                {dateChartData.length > 0
                  ? (statistics.total / dateChartData.length).toFixed(1)
                  : 0}
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-red-100 rounded-lg p-3">
              <AlertCircle className="h-6 w-6 text-red-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Duplicados</p>
              <p className="text-2xl font-bold text-gray-900">{statistics.duplicates}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Notificaciones por Día</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={dateChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="cantidad" stroke="#0ea5e9" name="Cantidad" />
              <Line type="monotone" dataKey="monto" stroke="#10b981" name="Monto (S/)" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Por Aplicación</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={sourceAppData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="source" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="cantidad" fill="#0ea5e9" name="Cantidad" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Status Chart */}
      <div className="card">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Estado de Notificaciones</h2>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={statusData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="status" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="cantidad" fill="#8b5cf6" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Top Source Apps */}
      <div className="card">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Resumen por Aplicación</h2>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Aplicación
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Cantidad
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Total Monto
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {sourceAppData.map((item) => (
                <tr key={item.source}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {item.source}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {item.cantidad}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    S/ {Number(item.monto).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

