/**
 * Contenedor para mostrar todos los toasts activos
 */

import { useToastStore } from '@/hooks/useToast';
import Toast from './Toast';

export default function ToastContainer() {
  const { toasts, removeToast } = useToastStore();

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div
      className="fixed top-20 right-4 z-[100] space-y-2 max-w-md w-full"
      role="region"
      aria-label="Notificaciones del sistema"
      aria-live="polite"
    >
      {toasts.map((toast) => (
        <Toast key={toast.id} toast={toast} onClose={removeToast} />
      ))}
    </div>
  );
}

