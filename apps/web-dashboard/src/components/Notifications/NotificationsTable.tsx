import { Eye, Check, RotateCcw, Sparkles } from 'lucide-react';
import { format, isAfter, subSeconds } from 'date-fns';
import Button from '@/components/UI/Button';
import StatusBadge from '@/components/UI/StatusBadge';
import Badge from '@/components/UI/Badge';
import { isPossibleDuplicate } from './PossibleDuplicateBadge';
import type { Notification } from '@/types';

interface Props {
  notifications: Notification[];
  loading?: boolean;
  validatingId?: number | null;
  onValidate: (n: Notification) => void;
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

// Soft pastel palette — assigns a stable color per instance so the user
// can quickly tell rows from the same instance apart at a glance.
const INSTANCE_TONES: Array<'pink' | 'sky' | 'amber' | 'emerald' | 'violet' | 'rose' | 'cyan' | 'lime'> = [
  'pink', 'sky', 'amber', 'emerald', 'violet', 'rose', 'cyan', 'lime',
];

const INSTANCE_TONE_CLASSES: Record<string, string> = {
  pink:    'bg-pink-50 text-pink-800 ring-pink-200',
  sky:     'bg-sky-50 text-sky-800 ring-sky-200',
  amber:   'bg-amber-50 text-amber-800 ring-amber-200',
  emerald: 'bg-emerald-50 text-emerald-800 ring-emerald-200',
  violet:  'bg-violet-50 text-violet-800 ring-violet-200',
  rose:    'bg-rose-50 text-rose-800 ring-rose-200',
  cyan:    'bg-cyan-50 text-cyan-800 ring-cyan-200',
  lime:    'bg-lime-50 text-lime-800 ring-lime-200',
};

function instanceTone(id: number): string {
  return INSTANCE_TONES[id % INSTANCE_TONES.length];
}

function instanceBadge(label: string | null | undefined, id: number | null | undefined) {
  if (id == null) return <span className="text-slate-400 text-xs">—</span>;
  const tone = instanceTone(id);
  const classes = INSTANCE_TONE_CLASSES[tone];
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset whitespace-nowrap ${classes}`}>
      {label ?? `#${id}`}
    </span>
  );
}

function isRecent(createdAt: string): boolean {
  // notifications received in the last 60s are highlighted as "new"
  return isAfter(new Date(createdAt), subSeconds(new Date(), 60));
}

export default function NotificationsTable({
  notifications, loading, validatingId, onValidate, onRevert, onView,
}: Props) {
  if (loading) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-slate-500">
        Cargando notificaciones...
      </div>
    );
  }
  if (notifications.length === 0) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-12 text-center">
        <p className="text-slate-700 font-medium">No hay notificaciones</p>
        <p className="text-sm text-slate-500 mt-1">Ajusta filtros o periodo para ver mas resultados.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr className="text-left">
            <th className="w-1 px-0" aria-hidden></th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Hora</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">App</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Instancia</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Pagador</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500 text-right">Monto</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Código</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Estado</th>
            <th className="px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500 text-right">Acción</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {notifications.map((n) => {
            const dup = isPossibleDuplicate(n, notifications);
            const isValidated = n.status === 'validated';
            const isPending = n.status === 'pending';
            const recent = isRecent(n.created_at);

            // Left indicator color: lime for new, amber for duplicate, none otherwise
            const indicatorClass = recent
              ? 'bg-accent-300'
              : dup
              ? 'bg-amber-400'
              : '';

            // Row background based on state
            const rowClass = isValidated
              ? 'bg-cta-50/40 opacity-70 hover:bg-cta-50/60'
              : recent
              ? 'bg-accent-50/40 hover:bg-accent-50'
              : 'hover:bg-slate-50';

            return (
              <tr key={n.id} className={`group ${rowClass}`}>
                <td className="relative w-1 p-0">
                  {indicatorClass && (
                    <span className={`absolute inset-y-1 left-0 w-1 rounded-r ${indicatorClass}`} aria-hidden />
                  )}
                </td>
                <td className="px-3 py-3 text-xs font-mono text-slate-600 whitespace-nowrap tabular-nums">
                  {format(new Date(n.created_at), 'dd/MM HH:mm')}
                  {recent && (
                    <div className="mt-0.5 inline-flex items-center gap-1 text-[10px] font-bold uppercase text-emerald-700">
                      <Sparkles className="h-3 w-3" /> Nueva
                    </div>
                  )}
                </td>
                <td className="px-3 py-3">{appBadge(n.source_app)}</td>
                <td className="px-3 py-3">
                  {instanceBadge(n.app_instance?.instance_label, n.app_instance?.id ?? n.app_instance_id)}
                </td>
                <td className="px-3 py-3 max-w-[200px]">
                  <div className="text-sm text-slate-900 truncate" title={n.payer_name ?? ''}>
                    {n.payer_name ?? '—'}
                  </div>
                  <div className="text-[11px] text-slate-500 truncate">
                    {n.device?.alias ?? n.device?.name ?? ''}
                  </div>
                </td>
                <td className="px-3 py-3 text-right whitespace-nowrap">
                  <div className="text-base font-semibold text-slate-900 tabular-nums">
                    S/ {Number(n.amount ?? 0).toFixed(2)}
                  </div>
                </td>
                <td className="px-3 py-3">
                  {n.security_code ? (
                    <span className="inline-flex items-center font-mono text-sm font-bold rounded-md bg-accent-100 text-primary-800 px-2.5 py-1 tabular-nums">
                      {n.security_code}
                    </span>
                  ) : (
                    <span className="inline-flex items-center text-[10px] font-semibold uppercase rounded-md bg-slate-100 text-slate-500 px-2 py-1">
                      Sin código
                    </span>
                  )}
                  {dup && (
                    <div className="mt-1 text-[10px] font-bold uppercase tracking-wider text-amber-700">
                      ⚠ Posible duplicado
                    </div>
                  )}
                </td>
                <td className="px-3 py-3"><StatusBadge status={n.status} /></td>
                <td className="px-3 py-3 text-right whitespace-nowrap">
                  <div className="inline-flex gap-1.5">
                    {isPending && (
                      <Button
                        size="sm"
                        variant="cta"
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
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Eye className="h-3.5 w-3.5" />}
                      onClick={() => onView(n)}
                    >
                      Ver
                    </Button>
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
