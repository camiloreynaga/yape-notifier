import { AlertTriangle } from 'lucide-react';
import { useSuspendCommerce } from '@/hooks/useSuspendCommerce';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function SuspendConfirmDialog({ commerce, onClose, onSuccess }: Props) {
  const suspend = useSuspendCommerce();

  const handleConfirm = async () => {
    await suspend.mutateAsync(commerce.id);
    onSuccess?.();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start gap-3 mb-4">
          <div className="flex-shrink-0 rounded-full bg-red-100 p-2">
            <AlertTriangle className="h-5 w-5 text-red-600" />
          </div>
          <div>
            <h2 className="text-lg font-semibold">Suspender comercio</h2>
            <p className="text-sm text-gray-600 mt-1">
              ¿Seguro que quieres suspender <strong>{commerce.name}</strong>? Sus dispositivos dejaran de aceptar notificaciones inmediatamente.
            </p>
          </div>
        </div>
        <div className="flex gap-2 justify-end mt-4">
          <button onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
            Cancelar
          </button>
          <button onClick={handleConfirm} disabled={suspend.isPending}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md disabled:opacity-50">
            {suspend.isPending ? 'Suspendiendo...' : 'Si, suspender'}
          </button>
        </div>
      </div>
    </div>
  );
}
