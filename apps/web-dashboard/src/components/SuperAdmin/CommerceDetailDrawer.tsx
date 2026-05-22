import { X, Mail, Phone, Copy, Users, Smartphone, BarChart, History } from 'lucide-react';
import { useSuperAdminCommerce } from '@/hooks/useSuperAdminCommerce';
import CommerceStatusBadge from './CommerceStatusBadge';
import type { CommerceDetail } from '@/types';

interface Props {
  commerceId: number | null;
  onClose: () => void;
  onAction: (action: 'renew' | 'change_plan' | 'suspend' | 'reactivate' | 'approve', commerce: CommerceDetail) => void;
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).catch(() => {});
}

export default function CommerceDetailDrawer({ commerceId, onClose, onAction }: Props) {
  const { data: commerce, isLoading } = useSuperAdminCommerce(commerceId);

  if (commerceId === null) return null;

  return (
    <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose}>
      <div
        className="fixed top-0 right-0 h-full w-full sm:w-[480px] bg-white shadow-xl overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold">Detalle del comercio</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>

        {isLoading || !commerce ? (
          <div className="p-6 text-center text-gray-500">Cargando...</div>
        ) : (
          <div className="p-4 space-y-5">
            <section>
              <h3 className="text-xl font-bold">{commerce.name}</h3>
              <div className="flex items-center gap-2 mt-1">
                <CommerceStatusBadge status={commerce.expiry_status} />
                {commerce.plan && <span className="text-sm text-gray-600">· {commerce.plan.name}</span>}
              </div>
            </section>

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Dueño</h4>
              <p className="font-medium">{commerce.owner?.name ?? '—'}</p>
              <div className="flex items-center gap-2 mt-1 text-sm">
                <Mail className="h-4 w-4 text-gray-400" />
                <span className="flex-1 truncate">{commerce.owner?.email ?? '—'}</span>
                {commerce.owner?.email && (
                  <button onClick={() => copyToClipboard(commerce.owner!.email)} className="text-gray-400 hover:text-gray-700" title="Copiar email">
                    <Copy className="h-4 w-4" />
                  </button>
                )}
              </div>
              <div className="flex items-center gap-2 mt-1 text-sm">
                <Phone className="h-4 w-4 text-gray-400" />
                <span className="flex-1">{commerce.owner?.phone ?? 'Sin telefono'}</span>
                {commerce.owner?.phone && (
                  <button onClick={() => copyToClipboard(commerce.owner!.phone!)} className="text-gray-400 hover:text-gray-700" title="Copiar telefono">
                    <Copy className="h-4 w-4" />
                  </button>
                )}
              </div>
            </section>

            {commerce.plan && (
              <section className="rounded-lg border p-3">
                <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Plan actual</h4>
                <div className="flex justify-between mb-2">
                  <span className="font-medium">{commerce.plan.name}</span>
                  <span className="text-sm">S/{commerce.plan.price}/mes</span>
                </div>
                <p className="text-sm">Dispositivos: {commerce.plan.max_devices ?? '∞'}</p>
                <p className="text-sm">Notificaciones/dia: {commerce.plan.max_notifications_per_day ?? '∞'}</p>
                {commerce.plan_expires_at && (
                  <p className="text-sm mt-2">
                    Vence: {new Date(commerce.plan_expires_at).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })}
                    {commerce.days_until_expiry !== null && (
                      <span className="text-gray-500"> ({commerce.days_until_expiry >= 0 ? `en ${commerce.days_until_expiry} dias` : `vencio hace ${Math.abs(commerce.days_until_expiry)} dias`})</span>
                    )}
                  </p>
                )}
              </section>
            )}

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                <Users className="h-4 w-4" /> Captadores ({commerce.captadores.length})
              </h4>
              {commerce.captadores.length === 0 ? (
                <p className="text-sm text-gray-500">Sin captadores</p>
              ) : (
                <ul className="text-sm space-y-1">
                  {commerce.captadores.map((c) => (
                    <li key={c.id} className="flex justify-between">
                      <span>{c.name}</span>
                      <span className="text-gray-500 font-mono">PIN {c.pin ?? '—'}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-lg border p-3">
              <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                <BarChart className="h-4 w-4" /> Uso
              </h4>
              <p className="text-sm flex items-center gap-2"><Smartphone className="h-3.5 w-3.5 text-gray-400" /> {commerce.devices_count} dispositivos</p>
            </section>

            {commerce.renewals.length > 0 && (
              <section className="rounded-lg border p-3">
                <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center gap-2">
                  <History className="h-4 w-4" /> Renovaciones recientes
                </h4>
                <ul className="text-sm space-y-2">
                  {commerce.renewals.slice(0, 5).map((r) => (
                    <li key={r.id} className="border-l-2 border-gray-200 pl-2">
                      <div className="flex justify-between text-xs text-gray-500">
                        <span>{new Date(r.created_at).toLocaleDateString('es-PE')}</span>
                        <span>{r.plan?.name}</span>
                      </div>
                      <div>{r.amount_paid !== null ? `S/${r.amount_paid}` : 'Sin monto'} · {r.renewedBy?.name}</div>
                      {r.notes && <div className="text-xs text-gray-500">{r.notes}</div>}
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="space-y-2">
              {commerce.expiry_status === 'pending' && (
                <button onClick={() => onAction('approve', commerce)} className="w-full rounded-md bg-primary-600 text-white py-2.5 font-medium hover:bg-primary-700">
                  Aprobar comercio
                </button>
              )}
              {commerce.expiry_status !== 'pending' && commerce.status !== 'suspended' && (
                <>
                  <button onClick={() => onAction('renew', commerce)} className="w-full rounded-md bg-primary-600 text-white py-2.5 font-medium hover:bg-primary-700">
                    Renovar 30 dias
                  </button>
                  <button onClick={() => onAction('change_plan', commerce)} className="w-full rounded-md bg-gray-100 text-gray-700 py-2.5 font-medium hover:bg-gray-200">
                    Cambiar plan
                  </button>
                  <button onClick={() => onAction('suspend', commerce)} className="w-full rounded-md text-red-700 py-2.5 font-medium hover:bg-red-50">
                    Suspender comercio
                  </button>
                </>
              )}
              {commerce.status === 'suspended' && (
                <button onClick={() => onAction('reactivate', commerce)} className="w-full rounded-md bg-blue-600 text-white py-2.5 font-medium hover:bg-blue-700">
                  Reactivar (renovar)
                </button>
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
