/**
 * Componente principal del Dashboard Overview
 * Muestra estadísticas, gráficos y resúmenes del sistema
 */

import { Plus, Bell, Package, Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import PeriodFilter from './PeriodFilter';
import KPICards from './KPICards';
import ChartsSection from './ChartsSection';
import DevicesSummary from './DevicesSummary';
import RecentNotifications from './RecentNotifications';

export default function DashboardOverview() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6" role="main" aria-label="Resumen del dashboard">
      {/* Filtro de período */}
      <PeriodFilter />

      {/* Cards de KPIs */}
      <KPICards />

      {/* Acciones rápidas */}
      <div className="card bg-gradient-to-r from-primary-50 to-indigo-50">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Acciones Rápidas</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <button
            onClick={() => navigate('/devices/add')}
            className="btn btn-primary flex items-center justify-center gap-2"
          >
            <Plus className="h-4 w-4" />
            Agregar Dispositivo
          </button>
          <button
            onClick={() => navigate('/dashboard?tab=notifications')}
            className="btn btn-secondary flex items-center justify-center gap-2"
          >
            <Bell className="h-4 w-4" />
            Ver Todas las Notificaciones
          </button>
          <button
            onClick={() => navigate('/app-instances')}
            className="btn btn-secondary flex items-center justify-center gap-2"
          >
            <Package className="h-4 w-4" />
            Gestionar Instancias
          </button>
          <button
            onClick={() => navigate('/dashboard?tab=settings')}
            className="btn btn-secondary flex items-center justify-center gap-2"
          >
            <Settings className="h-4 w-4" />
            Configurar Apps
          </button>
        </div>
      </div>

      {/* Gráficos */}
      <ChartsSection />

      {/* Tablas de resumen */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <DevicesSummary />
        <RecentNotifications />
      </div>
    </div>
  );
}

