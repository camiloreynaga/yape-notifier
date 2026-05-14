import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Clock, Ban, LogOut, RefreshCw } from 'lucide-react';

/**
 * Shown when the user has a commerce that is NOT active (pending or suspended).
 * Replaces the old broken flow where a pending user was bounced to
 * /create-commerce forever and kept re-submitting the create form.
 */
export default function CommerceStatusPage() {
  const navigate = useNavigate();
  const { commerce, hasCommerce, checkCommerce, logout } = useAuth();

  // If there's no commerce at all → the create flow. If it became active →
  // go to the dashboard. This page only handles pending/suspended.
  useEffect(() => {
    if (!hasCommerce) {
      navigate('/create-commerce', { replace: true });
    } else if (commerce && commerce.status === 'active') {
      navigate('/dashboard', { replace: true });
    }
  }, [hasCommerce, commerce, navigate]);

  const isSuspended = commerce?.status === 'suspended';

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-warm px-4">
      <div className="max-w-md w-full bg-white rounded-2xl border border-slate-200 shadow-sm p-8 text-center">
        <div
          className={`mx-auto w-14 h-14 rounded-2xl flex items-center justify-center mb-5 ${
            isSuspended ? 'bg-red-50' : 'bg-amber-50'
          }`}
        >
          {isSuspended ? (
            <Ban className="h-7 w-7 text-red-600" />
          ) : (
            <Clock className="h-7 w-7 text-amber-600" />
          )}
        </div>

        {isSuspended ? (
          <>
            <h1 className="text-xl font-bold text-slate-900">Comercio suspendido</h1>
            <p className="text-sm text-slate-600 mt-2">
              El comercio <strong>{commerce?.name}</strong> ha sido suspendido.
              Contacta a soporte para reactivar tu cuenta.
            </p>
          </>
        ) : (
          <>
            <h1 className="text-xl font-bold text-slate-900">Comercio en revisión</h1>
            <p className="text-sm text-slate-600 mt-2">
              Tu comercio <strong>{commerce?.name}</strong> ya está registrado y
              está <strong>pendiente de aprobación</strong>. Un administrador lo
              revisará pronto — no necesitas volver a crearlo.
            </p>
            <div className="mt-4 rounded-lg bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800">
              Te avisaremos cuando esté activo. Mientras tanto puedes cerrar sesión
              y volver más tarde.
            </div>
          </>
        )}

        <div className="mt-6 flex flex-col gap-2">
          <button
            onClick={() => checkCommerce()}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-primary-800 text-white py-2.5 text-sm font-medium hover:bg-primary-700"
          >
            <RefreshCw className="h-4 w-4" />
            Verificar estado
          </button>
          <button
            onClick={logout}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 text-slate-700 py-2.5 text-sm font-medium hover:bg-slate-50"
          >
            <LogOut className="h-4 w-4" />
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  );
}
