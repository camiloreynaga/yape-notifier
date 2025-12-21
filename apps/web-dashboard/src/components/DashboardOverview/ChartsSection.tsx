/**
 * Componente para mostrar los gráficos del dashboard
 */

import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { format, parseISO } from 'date-fns';
import ChartContainer from '@/components/ChartContainer';
import { useDashboardStatistics } from '@/hooks/useDashboardStatistics';
import { usePeriodFilter } from '@/hooks/usePeriodFilter';
import { useMemo } from 'react';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

export default function ChartsSection() {
  const { dateRange } = usePeriodFilter();
  const { statistics, loading } = useDashboardStatistics(dateRange);

  // Preparar datos para gráfico de líneas (notificaciones por día)
  const notificationsByDate = useMemo(() => {
    if (!statistics?.by_date) return [];
    const entries = Object.entries(statistics.by_date).map(([date, data]) => {
      try {
        // Intentar parsear la fecha, si falla usar la fecha original
        const parsedDate = parseISO(date);
        return {
          date: format(parsedDate, 'dd/MM'),
          count: data.count,
          amount: data.total_amount,
          sortKey: parsedDate.getTime(),
        };
      } catch {
        return {
          date: date,
          count: data.count,
          amount: data.total_amount,
          sortKey: 0,
        };
      }
    });
    
    return entries
      .sort((a, b) => a.sortKey - b.sortKey)
      .map((entry) => ({
        date: entry.date,
        count: entry.count,
        amount: entry.amount,
      }))
      .slice(-30); // Últimos 30 días
  }, [statistics]);

  // Preparar datos para gráfico de barras (notificaciones por app)
  const notificationsByApp = useMemo(() => {
    if (!statistics?.by_source_app) return [];
    return Object.entries(statistics.by_source_app)
      .map(([app, data]) => ({
        name: app.charAt(0).toUpperCase() + app.slice(1),
        count: data.count,
        amount: data.total_amount,
      }))
      .sort((a, b) => b.count - a.count);
  }, [statistics]);

  // Preparar datos para gráfico de dona (distribución por estado)
  const notificationsByStatus = useMemo(() => {
    if (!statistics?.by_status) return [];
    const statusLabels: Record<string, string> = {
      pending: 'Pendiente',
      validated: 'Validado',
      inconsistent: 'Inconsistente',
    };
    return Object.entries(statistics.by_status).map(([status, count]) => ({
      name: statusLabels[status] || status,
      value: count,
    }));
  }, [statistics]);

  // Preparar datos para gráfico de barras horizontales (top dispositivos)
  const topDevices = useMemo(() => {
    if (!statistics?.by_device) return [];
    return Object.entries(statistics.by_device)
      .map(([deviceId, data]) => ({
        name: `Dispositivo ${deviceId}`,
        count: data.count,
        amount: data.total_amount,
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);
  }, [statistics]);

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold text-gray-900">Visualizaciones</h2>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Gráfico de líneas: Notificaciones por día */}
        <ChartContainer
          title="Notificaciones por Día"
          description="Evolución de notificaciones en el período seleccionado"
          loading={loading}
          empty={notificationsByDate.length === 0}
        >
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={notificationsByDate}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="count" stroke="#3b82f6" name="Cantidad" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </ChartContainer>

        {/* Gráfico de barras: Notificaciones por app */}
        <ChartContainer
          title="Notificaciones por Aplicación"
          description="Distribución de notificaciones según la aplicación de origen"
          loading={loading}
          empty={notificationsByApp.length === 0}
        >
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={notificationsByApp}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="count" fill="#3b82f6" name="Cantidad" />
            </BarChart>
          </ResponsiveContainer>
        </ChartContainer>

        {/* Gráfico de barras: Monto total por app */}
        <ChartContainer
          title="Monto Total por Aplicación"
          description="Monto acumulado por aplicación de origen"
          loading={loading}
          empty={notificationsByApp.length === 0}
        >
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={notificationsByApp}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip formatter={(value) => `S/ ${Number(value).toFixed(2)}`} />
              <Legend />
              <Bar dataKey="amount" fill="#10b981" name="Monto (S/)" />
            </BarChart>
          </ResponsiveContainer>
        </ChartContainer>

        {/* Gráfico de dona: Distribución por estado */}
        <ChartContainer
          title="Distribución por Estado"
          description="Proporción de notificaciones según su estado"
          loading={loading}
          empty={notificationsByStatus.length === 0}
        >
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={notificationsByStatus}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {notificationsByStatus.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </ChartContainer>
      </div>

      {/* Gráfico de barras horizontales: Top dispositivos */}
      <ChartContainer
        title="Top 5 Dispositivos por Notificaciones"
        description="Dispositivos con mayor cantidad de notificaciones"
        loading={loading}
        empty={topDevices.length === 0}
      >
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={topDevices} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" />
            <YAxis dataKey="name" type="category" width={100} />
            <Tooltip />
            <Legend />
            <Bar dataKey="count" fill="#8b5cf6" name="Cantidad" />
          </BarChart>
        </ResponsiveContainer>
      </ChartContainer>
    </div>
  );
}

