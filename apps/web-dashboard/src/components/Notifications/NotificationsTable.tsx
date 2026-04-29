import { Eye, Check, MoreVertical, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';
import Button from '@/components/UI/Button';
import StatusBadge from '@/components/UI/StatusBadge';
import Badge from '@/components/UI/Badge';
import PossibleDuplicateBadge, { isPossibleDuplicate } from './PossibleDuplicateBadge';
import type { Notification } from '@/types';

interface Props {
  notifications: Notification[];
  loading?: boolean;
  validatingId?: number | null;
  onValidate: (n: Notification) => void;
  onMarkInconsistent: (n: Notification) => void;
  onRevert: (n: Notification) => void;
  onView: (n: Notification) => void;
}

const APP_TONE: Record<string, 'purple' | 'blue' | 'red' | 'gray'> = {
  yape: 'purple',
  plin: 'blue',
  bcp: 'red',
};

function appBadge(sourceApp?: string | null) {
  const key = (sourceApp ?? '').toLowerCase();
  const tone = APP_TONE[key] ?? 'gray';
  return <Badge tone={tone}>{(sourceApp ?? 'OTRA').toUpperCase()}</Badge>;
}

export default function NotificationsTable({
  notifications, loading, validatingId, onValidate, onMarkInconsistent, onRevert, onView,
}: Props) {
  if (loading) {
    return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando notificaciones...</div>;
  }
  if (notifications.length === 0) {
    return (
      <div className="rounded-xl bg-white p-12 text-center">
        <p className="text-gray-700 font-medium">No hay notificaciones</p>
        <p className="text-sm text-gray-500 mt-1">Ajusta filtros o periodo para ver mas resultados.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Fecha</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">App</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Instancia</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Dispositivo</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Pagador</th>
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600">Monto</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Codigo</th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600">Acciones</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {notifications.map((n) => {
            const dup = isPossibleDuplicate(n, notifications);
            return (
              <tr key={n.id} className="hover:bg-gray-50">
                <td className="px-3 py-3 text-sm text-gray-700 whitespace-nowrap">
                  {format(new Date(n.created_at), 'dd/MM HH:mm')}
                </td>
                <td className="px-3 py-3">{appBadge(n.source_app)}</td>
                <td className="px-3 py-3 text-sm text-gray-700">{n.app_instance?.instance_label ?? '—'}</td>
                <td className="px-3 py-3 text-sm text-gray-700">{n.device?.alias ?? n.device?.name ?? '—'}</td>
                <td className="px-3 py-3 text-sm text-gray-700 max-w-[180px] truncate" title={n.payer_name ?? ''}>
                  {n.payer_name ?? '—'}
                </td>
                <td className="px-3 py-3 text-right text-sm font-semibold text-gray-900 whitespace-nowrap">
                  S/ {Number(n.amount ?? 0).toFixed(2)}
                </td>
                <td className="px-3 py-3">
                  <span className="font-mono text-xs rounded-md bg-accent-100 text-primary-800 px-2 py-1">
                    {n.security_code ?? '—'}
                  </span>
                  {dup && <div className="mt-1"><PossibleDuplicateBadge /></div>}
                </td>
                <td className="px-3 py-3"><StatusBadge status={n.status} /></td>
                <td className="px-3 py-3 text-right">
                  <div className="flex justify-end gap-1.5">
                    {n.status === 'pending' && (
                      <Button
                        size="sm"
                        variant="success"
                        icon={<Check className="h-3.5 w-3.5" />}
                        onClick={() => onValidate(n)}
                        loading={validatingId === n.id}
                      >
                        Validar
                      </Button>
                    )}
                    {n.status === 'inconsistent' && (
                      <Button
                        size="sm"
                        variant="outline"
                        icon={<RotateCcw className="h-3.5 w-3.5" />}
                        onClick={() => onRevert(n)}
                      >
                        Revertir
                      </Button>
                    )}
                    <Button size="sm" variant="ghost" icon={<Eye className="h-3.5 w-3.5" />} onClick={() => onView(n)}>
                      Ver
                    </Button>
                    {n.status === 'pending' && (
                      <button
                        onClick={() => onMarkInconsistent(n)}
                        className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                        title="Marcar inconsistente"
                      >
                        <MoreVertical className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
