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
    <div className="space-y-8" role="main" aria-label="Resumen del dashboard">
      {/* Filtro de período */}
      <PeriodFilter />

      {/* Cards de KPIs */}
      <KPICards />

      {/* Acciones rápidas - Diseño mejorado */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary-500 via-primary-600 to-indigo-600 p-8 shadow-xl">
        {/* Patrón decorativo de fondo */}
        <div className="absolute inset-0 bg-grid-white/10 [mask-image:linear-gradient(0deg,white,rgba(255,255,255,0.5))]" />
        <div className="absolute -right-4 -top-4 h-24 w-24 rounded-full bg-white/10 blur-2xl" />
        <div className="absolute -left-4 -bottom-4 h-32 w-32 rounded-full bg-white/10 blur-3xl" />

        <div className="relative z-10">
          <div className="mb-6">
            <h3 className="text-2xl font-bold text-white mb-2">Acciones Rápidas</h3>
            <p className="text-primary-100 text-sm">Accede rápidamente a las funciones más usadas</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <button
              onClick={() => navigate('/devices/add')}
              className="group relative overflow-hidden rounded-xl bg-white/10 backdrop-blur-sm border border-white/20 p-4 text-white hover:bg-white/20 hover:scale-105 transition-all duration-300 hover:shadow-2xl"
            >
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0 p-2 rounded-lg bg-white/20 group-hover:bg-white/30 group-hover:rotate-6 transition-all duration-300">
                  <Plus className="h-5 w-5" />
                </div>
                <span className="font-medium text-sm text-left">Agregar Dispositivo</span>
              </div>
            </button>

            <button
              onClick={() => navigate('/dashboard?tab=notifications')}
              className="group relative overflow-hidden rounded-xl bg-white/10 backdrop-blur-sm border border-white/20 p-4 text-white hover:bg-white/20 hover:scale-105 transition-all duration-300 hover:shadow-2xl"
            >
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0 p-2 rounded-lg bg-white/20 group-hover:bg-white/30 group-hover:rotate-6 transition-all duration-300">
                  <Bell className="h-5 w-5" />
                </div>
                <span className="font-medium text-sm text-left">Ver Notificaciones</span>
              </div>
            </button>

            <button
              onClick={() => navigate('/app-instances')}
              className="group relative overflow-hidden rounded-xl bg-white/10 backdrop-blur-sm border border-white/20 p-4 text-white hover:bg-white/20 hover:scale-105 transition-all duration-300 hover:shadow-2xl"
            >
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0 p-2 rounded-lg bg-white/20 group-hover:bg-white/30 group-hover:rotate-6 transition-all duration-300">
                  <Package className="h-5 w-5" />
                </div>
                <span className="font-medium text-sm text-left">Gestionar Instancias</span>
              </div>
            </button>

            <button
              onClick={() => navigate('/dashboard?tab=settings')}
              className="group relative overflow-hidden rounded-xl bg-white/10 backdrop-blur-sm border border-white/20 p-4 text-white hover:bg-white/20 hover:scale-105 transition-all duration-300 hover:shadow-2xl"
            >
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0 p-2 rounded-lg bg-white/20 group-hover:bg-white/30 group-hover:rotate-6 transition-all duration-300">
                  <Settings className="h-5 w-5" />
                </div>
                <span className="font-medium text-sm text-left">Configurar Apps</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      {/* Gráficos */}
      <ChartsSection />

      {/* Tablas de resumen - Grid Bento */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <RecentNotifications />
        </div>
        <div className="lg:col-span-1">
          <DevicesSummary />
        </div>
      </div>
    </div>
  );
}

