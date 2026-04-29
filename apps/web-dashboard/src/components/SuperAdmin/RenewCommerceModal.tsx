import { useState, useMemo } from 'react';
import { X } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import { useRenewCommerce } from '@/hooks/useRenewCommerce';
import type { CommerceListItem, CommerceDetail } from '@/types';

interface Props {
  commerce: CommerceListItem | CommerceDetail;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function RenewCommerceModal({ commerce, onClose, onSuccess }: Props) {
  const { data: plans } = useSuperAdminPlans();
  const renew = useRenewCommerce();
  const [planSlug, setPlanSlug] = useState(commerce.plan?.slug ?? '');
  const [amount, setAmount] = useState<string>(commerce.plan?.price?.toString() ?? '');
  const [notes, setNotes] = useState('');
  const [confirmed, setConfirmed] = useState(false);

  const newExpiresAt = useMemo(() => {
    const base = commerce.plan_expires_at && new Date(commerce.plan_expires_at) > new Date()
      ? new Date(commerce.plan_expires_at)
      : new Date();
    base.setDate(base.getDate() + 30);
    return base.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
  }, [commerce.plan_expires_at]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await renew.mutateAsync({
      id: commerce.id,
      plan_slug: planSlug,
      amount_paid: amount ? parseFloat(amount) : undefined,
      notes: notes || undefined,
    });
    onSuccess?.();
    onClose();
  };

  const activePlans = plans?.filter((p) => p.is_active) ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Renovar comercio</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <p className="text-sm text-gray-500">Comercio</p>
            <p className="font-medium">{commerce.name}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Plan</label>
            <select
              value={planSlug}
              onChange={(e) => {
                setPlanSlug(e.target.value);
                const p = activePlans.find((pp) => pp.slug === e.target.value);
                if (p) setAmount(p.price.toString());
              }}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500"
              required
            >
              <option value="">Selecciona un plan</option>
              {activePlans.map((p) => (
                <option key={p.id} value={p.slug}>{p.name} — S/{p.price}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Monto recibido (opcional)</label>
            <input type="number" step="0.01" min="0" value={amount} onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm" placeholder="0.00" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nota (opcional)</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2}
              className="w-full rounded-md border-gray-300 shadow-sm" placeholder="Ej: Yape Juan Perez" />
          </div>
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)}
              className="mt-0.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
            <span>Confirmo que el cliente ya pago</span>
          </label>
          <div className="rounded-lg bg-blue-50 p-3 text-sm text-blue-900">
            Nuevo vencimiento: <strong>{newExpiresAt}</strong>
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md">
              Cancelar
            </button>
            <button type="submit" disabled={!confirmed || !planSlug || renew.isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed">
              {renew.isPending ? 'Renovando...' : 'Confirmar y renovar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
