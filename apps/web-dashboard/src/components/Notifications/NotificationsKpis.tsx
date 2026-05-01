import { Clock, CheckCircle2, DollarSign, AlertTriangle, ArrowUpRight } from 'lucide-react';
import type { NotificationStatistics, Notification } from '@/types';

interface Props {
  stats?: NotificationStatistics;
  loading?: boolean;
  notificationsForDuplicates?: Notification[];
  activeFilter: 'all' | 'pending' | 'validated' | 'inconsistent';
  onFilterChange: (filter: 'all' | 'pending' | 'validated' | 'inconsistent') => void;
}

interface CardConfig {
  key: 'pending' | 'validated' | 'amount' | 'duplicate';
  label: string;
  value: string;
  delta?: string;
  deltaTone?: 'good' | 'warn' | 'neutral';
  icon: React.ComponentType<{ className?: string }>;
  filterKey?: 'pending' | 'validated' | null;
  emphasize?: boolean;
}

const fmtNum = (n: number) => n.toLocaleString('es-PE');
const fmtAmount = (n: number) =>
  'S/ ' + n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function detectDuplicates(notifications: Notification[]): number {
  // count notifications that have at least one near-duplicate within 60s window
  let count = 0;
  for (let i = 0; i < notifications.length; i++) {
    const a = notifications[i];
    if (!a.security_code) continue;
    const aTime = new Date(a.created_at).getTime();
    for (let j = i + 1; j < notifications.length; j++) {
      const b = notifications[j];
      if (
        b.security_code === a.security_code &&
        Number(b.amount) === Number(a.amount) &&
        Math.abs(new Date(b.created_at).getTime() - aTime) < 60_000
      ) {
        count++;
        break;
      }
    }
  }
  return count;
}

export default function NotificationsKpis({
  stats,
  loading,
  notificationsForDuplicates = [],
  activeFilter,
  onFilterChange,
}: Props) {
  const isLoading = loading === true && !stats;

  // Robust defaults: render with zeros if stats not loaded yet so cards
  // are not stuck as empty skeletons forever.
  const total = stats?.total ?? 0;
  const totalAmount = stats?.total_amount ?? 0;
  const byStatus = stats?.by_status ?? {};
  const pendingCount = byStatus['pending'] ?? 0;
  const validatedCount = byStatus['validated'] ?? 0;
  const duplicateCount = detectDuplicates(notificationsForDuplicates);

  const pendingTone: 'good' | 'warn' = pendingCount > 5 ? 'warn' : 'good';
  const duplicateTone: 'good' | 'warn' = duplicateCount > 0 ? 'warn' : 'good';

  const cards: CardConfig[] = [
    {
      key: 'pending',
      label: 'Pendientes hoy',
      value: fmtNum(pendingCount),
      delta: pendingCount > 0 ? 'requieren validación' : 'al día',
      deltaTone: pendingTone,
      icon: Clock,
      filterKey: 'pending',
      emphasize: pendingCount > 0,
    },
    {
      key: 'validated',
      label: 'Validadas hoy',
      value: fmtNum(validatedCount),
      delta: total > 0 ? `${Math.round((validatedCount / total) * 100)}% del total del día` : '0% del total',
      deltaTone: 'neutral',
      icon: CheckCircle2,
      filterKey: 'validated',
    },
    {
      key: 'amount',
      label: 'Monto total de hoy',
      value: fmtAmount(totalAmount),
      delta: `${total} operacion${total === 1 ? '' : 'es'} del día`,
      deltaTone: 'neutral',
      icon: DollarSign,
      filterKey: null,
    },
    {
      key: 'duplicate',
      label: 'Posibles duplicados',
      value: fmtNum(duplicateCount),
      delta: duplicateCount > 0 ? 'revisar antes de validar' : 'sin coincidencias',
      deltaTone: duplicateTone,
      icon: AlertTriangle,
      filterKey: null,
    },
  ];

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-28 rounded-xl bg-white border border-slate-200 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const Icon = card.icon;
        const isActive = card.filterKey != null && activeFilter === card.filterKey;
        const isClickable = card.filterKey != null;

        const baseClass =
          'group text-left rounded-xl bg-white border p-5 transition-all relative overflow-hidden';
        const borderClass = card.emphasize
          ? 'border-amber-300'
          : isActive
          ? 'border-cta-600'
          : 'border-slate-200';
        const interactClass = isClickable
          ? 'cursor-pointer hover:border-slate-300 hover:shadow-sm'
          : 'cursor-default';

        const deltaColor =
          card.deltaTone === 'good'
            ? 'text-emerald-700'
            : card.deltaTone === 'warn'
            ? 'text-amber-700'
            : 'text-slate-500';

        return (
          <button
            key={card.key}
            type="button"
            disabled={!isClickable}
            onClick={() => {
              if (card.filterKey) onFilterChange(card.filterKey);
            }}
            className={`${baseClass} ${borderClass} ${interactClass}`}
          >
            {card.emphasize && (
              <span className="absolute top-0 left-0 right-0 h-0.5 bg-amber-400" aria-hidden />
            )}
            <div className="flex items-center justify-between mb-3">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                {card.label}
              </span>
              <Icon className="h-4 w-4 text-slate-400" />
            </div>
            <div className="text-3xl font-bold text-slate-900 tracking-tight tabular-nums">
              {card.value}
            </div>
            {card.delta && (
              <div className={`mt-2 text-xs font-medium flex items-center gap-1 ${deltaColor}`}>
                {card.deltaTone === 'good' && <ArrowUpRight className="h-3 w-3" />}
                {card.delta}
              </div>
            )}
          </button>
        );
      })}
    </div>
  );
}
