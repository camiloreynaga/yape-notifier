// src/components/LoadingFallback/LoadingFallback.tsx
// Fallback component para Suspense con lazy loading

interface LoadingFallbackProps {
  message?: string;
  fullScreen?: boolean;
}

export default function LoadingFallback({
  message = 'Cargando...',
  fullScreen = false
}: LoadingFallbackProps) {
  const containerClass = fullScreen
    ? 'min-h-screen flex items-center justify-center bg-gray-50'
    : 'flex items-center justify-center h-64';

  return (
    <div className={containerClass}>
      <div className="flex flex-col items-center gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        <p className="text-gray-600 text-sm">{message}</p>
      </div>
    </div>
  );
}

/**
 * Loading fallback mínimo para Suspense de rutas
 */
export function RouteLoadingFallback() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>
  );
}
