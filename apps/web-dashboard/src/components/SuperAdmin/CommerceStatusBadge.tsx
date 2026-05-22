import type { ExpiryStatus } from '@/types';

const STATUS_CONFIG: Record<ExpiryStatus, { label: string; classes: string; dot: string }> = {
  pending:       { label: 'Pendiente',  classes: 'bg-yellow-50 text-yellow-800 ring-yellow-600/20',   dot: 'bg-yellow-500' },
  active:        { label: 'Activo',     classes: 'bg-green-50 text-green-700 ring-green-600/20',      dot: 'bg-green-500'  },
  expiring_soon: { label: 'Por vencer', classes: 'bg-orange-50 text-orange-800 ring-orange-600/20',   dot: 'bg-orange-500' },
  in_grace:      { label: 'En gracia',  classes: 'bg-rose-50 text-rose-800 ring-rose-600/20',         dot: 'bg-rose-500'   },
  expired:       { label: 'Vencido',    classes: 'bg-red-50 text-red-800 ring-red-600/20',            dot: 'bg-red-600'    },
  suspended:     { label: 'Suspendido', classes: 'bg-gray-100 text-gray-700 ring-gray-500/20',        dot: 'bg-gray-500'   },
};

export default function CommerceStatusBadge({ status }: { status: ExpiryStatus }) {
  const cfg = STATUS_CONFIG[status];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${cfg.classes}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
}
