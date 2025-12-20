import { Link, useLocation } from 'react-router-dom';
import { AlertCircle, X } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';

/**
 * Componente que muestra un banner de alerta cuando el usuario no tiene commerce
 * Se muestra en todas las páginas excepto en /create-commerce
 * Usa AuthContext para evitar llamadas API duplicadas
 */
export default function CommerceBanner() {
  const location = useLocation();
  const { hasCommerce, loading } = useAuth();
  const [dismissed, setDismissed] = useState(false);

  // No mostrar si está en la página de crear commerce
  if (location.pathname === '/create-commerce') {
    return null;
  }

  // No mostrar si está cargando o si ya tiene commerce
  if (loading || hasCommerce) {
    return null;
  }

  // No mostrar si el usuario descartó el banner
  if (dismissed) {
    return null;
  }

  return (
    <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-4">
      <div className="flex items-start">
        <div className="flex-shrink-0">
          <AlertCircle className="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <div className="ml-3 flex-1">
          <h3 className="text-sm font-medium text-yellow-800">
            Necesitas crear un comercio
          </h3>
          <div className="mt-2 text-sm text-yellow-700">
            <p>
              Para usar todas las funcionalidades del sistema, necesitas crear un comercio.
              Esto te permitirá organizar tus dispositivos y notificaciones.
            </p>
          </div>
          <div className="mt-4">
            <div className="-mx-2 -my-1.5 flex">
              <Link
                to="/create-commerce"
                className="bg-yellow-50 px-2 py-1.5 rounded-md text-sm font-medium text-yellow-800 hover:bg-yellow-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-yellow-50 focus:ring-yellow-600"
              >
                Crear comercio ahora
              </Link>
              <button
                type="button"
                onClick={() => setDismissed(true)}
                className="ml-3 bg-yellow-50 px-2 py-1.5 rounded-md text-sm font-medium text-yellow-800 hover:bg-yellow-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-yellow-50 focus:ring-yellow-600"
              >
                Descartar
              </button>
            </div>
          </div>
        </div>
        <div className="ml-auto pl-3">
          <div className="-mx-1.5 -my-1.5">
            <button
              type="button"
              onClick={() => setDismissed(true)}
              className="inline-flex bg-yellow-50 rounded-md p-1.5 text-yellow-500 hover:bg-yellow-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-yellow-50 focus:ring-yellow-600"
              aria-label="Cerrar"
            >
              <X className="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

