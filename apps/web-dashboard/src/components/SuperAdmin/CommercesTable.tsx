import { useSuperAdminCommerces, type CommercesQueryParams } from '@/hooks/useSuperAdminCommerces';
import CommerceStatusBadge from './CommerceStatusBadge';
import type { CommerceListItem, ExpiryStatus } from '@/types';

interface Props {
  filters: CommercesQueryParams;
  onRowClick: (id: number) => void;
  onAction: (action: 'approve' | 'renew' | 'reactivate' | 'change_plan' | 'suspend', commerce: CommerceListItem) => void;
}

function formatExpiry(item: CommerceListItem): { primary: string; secondary: string } {
  if (item.status === 'pending') return { primary: '—', secondary: 'No aprobado aun' };
  if (!item.plan_expires_at) return { primary: '—', secondary: '' };
  const days = item.days_until_expiry ?? 0;
  const date = new Date(item.plan_expires_at).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
  if (days < 0) return { primary: `Vencio hace ${Math.abs(days)} dias`, secondary: date };
  if (days === 0) return { primary: 'Vence hoy', secondary: date };
  return { primary: `En ${days} dias`, secondary: date };
}

function ActionButton({ commerce, onAction }: { commerce: CommerceListItem; onAction: Props['onAction'] }) {
  const status = commerce.expiry_status as ExpiryStatus;
  const stop = (e: React.MouseEvent) => e.stopPropagation();

  if (status === 'pending') {
    return (
      <button onClick={(e) => { stop(e); onAction('approve', commerce); }}
        className="rounded-md bg-primary-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-primary-700">
        Aprobar
      </button>
    );
  }
  if (status === 'suspended') {
    return (
      <button onClick={(e) => { stop(e); onAction('reactivate', commerce); }}
        className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700">
        Reactivar
      </button>
    );
  }
  if (status === 'in_grace' || status === 'expired') {
    return (
      <button onClick={(e) => { stop(e); onAction('renew', commerce); }}
        className="rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700">
        Renovar
      </button>
    );
  }
  if (status === 'expiring_soon') {
    return (
      <button onClick={(e) => { stop(e); onAction('renew', commerce); }}
        className="rounded-md bg-orange-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-orange-600">
        Renovar
      </button>
    );
  }
  // active
  return (
    <button onClick={(e) => { stop(e); onAction('change_plan', commerce); }}
      className="rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200">
      Cambiar plan
    </button>
  );
}

export default function CommercesTable({ filters, onRowClick, onAction }: Props) {
  const { data, isLoading } = useSuperAdminCommerces(filters);

  if (isLoading) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando comercios...</div>;
  }
  if (!data || data.data.length === 0) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">No hay comercios con los filtros actuales</div>;
  }

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Comercio · Dueño</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Plan</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Vencimiento</th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-gray-600">Accion</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.data.map((commerce) => {
            const expiry = formatExpiry(commerce);
            return (
              <tr key={commerce.id} onClick={() => onRowClick(commerce.id)} className="cursor-pointer hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="font-medium text-gray-900">{commerce.name}</div>
                  <div className="text-sm text-gray-500">{commerce.owner?.name ?? '—'}</div>
                </td>
                <td className="px-4 py-3 text-sm text-gray-700">
                  {commerce.plan?.name ?? '—'}
                </td>
                <td className="px-4 py-3">
                  <CommerceStatusBadge status={commerce.expiry_status} />
                </td>
                <td className="px-4 py-3">
                  <div className="text-sm text-gray-900">{expiry.primary}</div>
                  <div className="text-xs text-gray-500">{expiry.secondary}</div>
                </td>
                <td className="px-4 py-3 text-right">
                  <ActionButton commerce={commerce} onAction={onAction} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
