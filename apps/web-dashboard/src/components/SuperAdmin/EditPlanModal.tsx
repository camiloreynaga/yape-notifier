import { useState } from 'react';
import { X } from 'lucide-react';
import { useUpdatePlan } from '@/hooks/useUpdatePlan';
import type { Plan } from '@/types';

interface Props {
  plan: Plan;
  onClose: () => void;
}

export default function EditPlanModal({ plan, onClose }: Props) {
  const update = useUpdatePlan();
  const [price, setPrice] = useState(plan.price.toString());
  const [maxDevices, setMaxDevices] = useState(plan.max_devices?.toString() ?? '');
  const [maxNotifs, setMaxNotifs] = useState(plan.max_notifications_per_day?.toString() ?? '');
  const [isActive, setIsActive] = useState(plan.is_active);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await update.mutateAsync({
      id: plan.id,
      price: parseFloat(price),
      max_devices: maxDevices ? parseInt(maxDevices, 10) : null,
      max_notifications_per_day: maxNotifs ? parseInt(maxNotifs, 10) : null,
      is_active: isActive,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Editar plan: {plan.name}</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Precio (S/)</label>
            <input type="number" step="0.01" min="0" value={price} onChange={(e) => setPrice(e.target.value)}
              className="w-full rounded-md border-gray-300" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Maximo dispositivos (vacio = ilimitado)</label>
            <input type="number" min="1" value={maxDevices} onChange={(e) => setMaxDevices(e.target.value)}
              className="w-full rounded-md border-gray-300" placeholder="∞" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notificaciones por dia (vacio = ilimitado)</label>
            <input type="number" min="1" value={maxNotifs} onChange={(e) => setMaxNotifs(e.target.value)}
              className="w-full rounded-md border-gray-300" placeholder="∞" />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)}
              className="rounded border-gray-300 text-primary-600" />
            <span>Plan activo (visible al renovar)</span>
          </label>
          <p className="text-xs text-gray-500">Nombre y slug no se pueden editar para evitar romper relaciones existentes.</p>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={update.isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50">
              {update.isPending ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
