import { useAuth } from '@/contexts/AuthContext';
import { Building2, CreditCard, Shield } from 'lucide-react';

export default function SuperAdminPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6" role="main" aria-label="Panel de Super Admin">
      <div className="rounded-2xl bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-600 p-8 shadow-xl text-white">
        <div className="flex items-center gap-3 mb-2">
          <Shield className="h-8 w-8" />
          <h1 className="text-3xl font-bold">Panel de Super Admin</h1>
        </div>
        <p className="text-indigo-100">
          Bienvenido, {user?.name}. Desde aqui puedes administrar comercios y planes de la plataforma.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3 mb-3">
            <div className="rounded-lg bg-indigo-100 p-2">
              <Building2 className="h-6 w-6 text-indigo-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">Comercios</h2>
          </div>
          <p className="text-sm text-gray-600 mb-4">
            Aprobar, suspender o reactivar comercios. Cambiar planes asignados.
          </p>
          <span className="inline-flex items-center rounded-full bg-yellow-100 px-3 py-1 text-xs font-medium text-yellow-800">
            Proximamente
          </span>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3 mb-3">
            <div className="rounded-lg bg-purple-100 p-2">
              <CreditCard className="h-6 w-6 text-purple-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">Planes</h2>
          </div>
          <p className="text-sm text-gray-600 mb-4">
            Crear y editar planes con sus limites de dispositivos y notificaciones.
          </p>
          <span className="inline-flex items-center rounded-full bg-yellow-100 px-3 py-1 text-xs font-medium text-yellow-800">
            Proximamente
          </span>
        </div>
      </div>
    </div>
  );
}
