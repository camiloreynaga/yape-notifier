/**
 * Componente para mostrar las cards de KPIs principales
 */

import { Bell, DollarSign, Smartphone, AlertCircle, Copy, Package, RefreshCw } from 'lucide-react';
import StatCard from '@/components/StatCard';
import { useDashboardStatistics } from '@/hooks/useDashboardStatistics';
import { usePeriodFilter } from '@/hooks/usePeriodFilter';
import { apiService } from '@/services/api';
import { useState, useEffect } from 'react';
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

  return (
    <div className="mb-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-gray-900">Resumen Principal</h2>
        <button
          onClick={refresh}
          className="btn btn-secondary flex items-center gap-2"
          aria-label="Actualizar estadísticas"
        >
          <RefreshCw className="h-4 w-4" />
          Actualizar
        </button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <StatCard
          title="Notificaciones Totales"
          value={statistics?.total || 0}
          icon={Bell}
          iconColor="blue"
          loading={statsLoading}
          link={{
            href: '/dashboard?tab=notifications',
            label: 'Ver todas',
          }}
        />
        <StatCard
          title="Monto Total"
          value={formatCurrency(statistics?.total_amount || 0)}
          icon={DollarSign}
          iconColor="green"
          loading={statsLoading}
        />
        <StatCard
          title="Dispositivos"
          value={`${activeDevices}/${totalDevices}`}
          icon={Smartphone}
          iconColor="indigo"
          loading={loadingDevices}
          link={{
            href: '/dashboard?tab=devices',
            label: 'Gestionar',
          }}
        />
        <StatCard
          title="Pendientes de Validar"
          value={pendingNotifications}
          icon={AlertCircle}
          iconColor="yellow"
          loading={statsLoading}
          link={{
            href: '/dashboard?tab=notifications&status=pending',
            label: 'Ver pendientes',
          }}
        />
        <StatCard
          title="Duplicados Detectados"
          value={duplicateNotifications}
          icon={Copy}
          iconColor="red"
          loading={statsLoading}
        />
        <StatCard
          title="Instancias Activas"
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
  );
}

