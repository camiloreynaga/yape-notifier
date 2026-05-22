/**
 * Componente para mostrar las cards de KPIs principales
 * Layout Bento Grid mejorado con animaciones stagger
 */

import { Bell, DollarSign, Smartphone, AlertCircle, Copy, Package, RefreshCw } from 'lucide-react';
import StatCard from '@/components/StatCard';
import { useDashboardStatistics } from '@/hooks/useDashboardStatistics';
import { usePeriodFilter } from '@/hooks/usePeriodFilter';
import { apiService } from '@/services/api';
import { useState, useEffect, useMemo } from 'react';
import type { Device, AppInstance } from '@/types';

export default function KPICards() {
  const { dateRange } = usePeriodFilter();
  const { statistics, loading: statsLoading, refresh } = useDashboardStatistics(dateRange);
  const [devices, setDevices] = useState<Device[]>([]);
  const [appInstances, setAppInstances] = useState<AppInstance[]>([]);
  const [loadingDevices, setLoadingDevices] = useState(true);

  useEffect(() => {
    const loadDevices = async () => {
      try {
        const deviceList = await apiService.getDevices();
        setDevices(deviceList);
      } catch (error) {
        console.error('Error loading devices:', error);
      } finally {
        setLoadingDevices(false);
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

    loadDevices();
    loadAppInstances();
  }, []);

  const activeDevices = devices.filter((d) => d.is_active).length;
  const totalDevices = devices.length;
  const activeAppInstances = appInstances.length;
  const pendingNotifications = statistics?.by_status?.pending || 0;
  const duplicateNotifications = statistics?.duplicates || 0;

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  // Generar datos de sparkline simulados (en producción vendrían del API)
  const generateSparklineData = (base: number, variance: number = 0.2) => {
    return Array.from({ length: 7 }, () =>
      base * (1 + (Math.random() - 0.5) * variance)
    );
  };

  const notificationSparkline = useMemo(() =>
    generateSparklineData(statistics?.total || 100),
    [statistics?.total]
  );

  const amountSparkline = useMemo(() =>
    generateSparklineData(statistics?.total_amount || 1000),
    [statistics?.total_amount]
  );

  return (
    <div className="mb-8">
      {/* Header con animación */}
      <div className="flex items-center justify-between mb-6 animate-fade-in">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text">
            Resumen Principal
          </h2>
          <p className="text-sm text-gray-500 mt-1">Métricas clave de tu negocio</p>
        </div>
        <button
          onClick={refresh}
          className="btn btn-secondary flex items-center gap-2 group"
          aria-label="Actualizar estadísticas"
        >
          <RefreshCw className="h-4 w-4 group-hover:rotate-180 transition-transform duration-500" />
          <span className="hidden sm:inline">Actualizar</span>
        </button>
      </div>

      {/* Bento Grid Layout - Asimétrico y moderno */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 auto-rows-fr">
        {/* Card Principal - Notificaciones (Grande, 2 filas) */}
        <div className="lg:row-span-2 animate-fade-in" style={{ animationDelay: '0ms' }}>
          <StatCard
            title="Notificaciones Totales"
            value={statistics?.total || 0}
            icon={Bell}
            iconColor="blue"
            loading={statsLoading}
            size="large"
            sparklineData={notificationSparkline}
            trend={{
              value: 15,
              label: 'vs. mes anterior',
              isPositive: true,
            }}
            link={{
              href: '/dashboard?tab=notifications',
              label: 'Ver todas',
            }}
          />
        </div>

        {/* Card Monto Total (Grande, 2 filas) */}
        <div className="lg:row-span-2 animate-fade-in" style={{ animationDelay: '100ms' }}>
          <StatCard
            title="Monto Total"
            value={formatCurrency(statistics?.total_amount || 0)}
            icon={DollarSign}
            iconColor="green"
            loading={statsLoading}
            size="large"
            sparklineData={amountSparkline}
            trend={{
              value: 8,
              label: 'vs. mes anterior',
              isPositive: true,
            }}
          />
        </div>

        {/* Cards normales */}
        <div className="animate-fade-in" style={{ animationDelay: '200ms' }}>
          <StatCard
            title="Dispositivos Activos"
            value={`${activeDevices}/${totalDevices}`}
            icon={Smartphone}
            iconColor="indigo"
            loading={loadingDevices}
            link={{
              href: '/dashboard?tab=devices',
              label: 'Gestionar',
            }}
          />
        </div>

        <div className="animate-fade-in" style={{ animationDelay: '250ms' }}>
          <StatCard
            title="Pendientes"
            value={pendingNotifications}
            icon={AlertCircle}
            iconColor="yellow"
            loading={statsLoading}
            link={{
              href: '/dashboard?tab=notifications&status=pending',
              label: 'Ver pendientes',
            }}
          />
        </div>

        <div className="animate-fade-in" style={{ animationDelay: '300ms' }}>
          <StatCard
            title="Duplicados"
            value={duplicateNotifications}
            icon={Copy}
            iconColor="red"
            loading={statsLoading}
            trend={{
              value: 12,
              label: 'vs. mes anterior',
              isPositive: false,
            }}
          />
        </div>

        <div className="animate-fade-in" style={{ animationDelay: '350ms' }}>
          <StatCard
            title="Instancias"
            value={activeAppInstances}
            icon={Package}
            iconColor="purple"
            loading={loadingDevices}
            link={{
              href: '/app-instances',
              label: 'Gestionar',
            }}
          />
        </div>
      </div>
    </div>
  );
}
