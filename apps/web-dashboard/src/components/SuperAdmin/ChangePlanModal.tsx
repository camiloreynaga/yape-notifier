import { useState } from 'react';
import { X } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import { useChangePlan } from '@/hooks/useChangePlan';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function ChangePlanModal({ commerce, onClose, onSuccess }: Props) {
  const { data: plans } = useSuperAdminPlans();
  const change = useChangePlan();
  const [planSlug, setPlanSlug] = useState(commerce.plan?.slug ?? '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await change.mutateAsync({ id: commerce.id, plan_slug: planSlug });
    onSuccess?.();
    onClose();
  };

  const activePlans = plans?.filter((p) => p.is_active) ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Cambiar plan</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <p className="text-sm text-gray-500">Plan actual</p>
            <p className="font-medium">{commerce.plan?.name ?? '—'} {commerce.plan ? `· S/${commerce.plan.price}` : ''}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo plan</label>
            <select value={planSlug} onChange={(e) => setPlanSlug(e.target.value)}
              className="w-full rounded-md border-gray-300" required>
              {activePlans.map((p) => (
                <option key={p.id} value={p.slug}>{p.name} — S/{p.price}</option>
              ))}
            </select>
          </div>
          <p className="text-sm text-gray-600">
            El cambio de plan no afecta la fecha de vencimiento.
          </p>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={!planSlug || change.isPending || planSlug === commerce.plan?.slug}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50">
              {change.isPending ? 'Cambiando...' : 'Cambiar plan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
