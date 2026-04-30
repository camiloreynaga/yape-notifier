import { Bell, Clock, CheckCircle, AlertTriangle, DollarSign } from 'lucide-react';
import type { NotificationStatistics } from '@/types';

interface Props {
  stats?: NotificationStatistics;
  loading?: boolean;
  activeFilter: 'all' | 'pending' | 'validated' | 'inconsistent';
  onFilterChange: (filter: 'all' | 'pending' | 'validated' | 'inconsistent') => void;
}

interface CardConfig {
  key: 'all' | 'pending' | 'validated' | 'inconsistent' | 'amount';
  label: string;
  value: string;
  icon: React.ComponentType<{ className?: string }>;
  bg: string;
  fg: string;
  ring: string;
  clickable?: boolean;
}

export default function NotificationsKpis({ stats, loading, activeFilter, onFilterChange }: Props) {
  if (loading || !stats) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        {[0, 1, 2, 3, 4].map((i) => (
          <div key={i} className="h-24 rounded-xl bg-gray-200 animate-pulse" />
        ))}
      </div>
    );
  }

  const total = stats.total ?? 0;
  const totalAmount = stats.total_amount ?? 0;
  // backend returns by_status as Record<string, number>, e.g. { pending: 100, validated: 80 }
  const byStatus = stats.by_status ?? {};
  const get = (s: string) => byStatus[s] ?? 0;

  const fmt = (n: number) => n.toLocaleString('es-PE');
  const fmtAmount = (n: number) =>
    'S/ ' + n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const cards: CardConfig[] = [
    { key: 'all',          label: 'Total del periodo', value: fmt(total),          icon: Bell,          bg: 'bg-white',     fg: 'text-gray-800',   ring: 'ring-gray-200',   clickable: true },
    { key: 'amount',       label: 'Monto total',       value: fmtAmount(totalAmount), icon: DollarSign,  bg: 'bg-emerald-50', fg: 'text-emerald-800', ring: 'ring-emerald-200', clickable: false },
    { key: 'pending',      label: 'Pendientes',        value: fmt(get('pending')),    icon: Clock,        bg: 'bg-yellow-50',  fg: 'text-yellow-800', ring: 'ring-yellow-200', clickable: true },
    { key: 'validated',    label: 'Validadas',         value: fmt(get('validated')),  icon: CheckCircle,  bg: 'bg-green-50',   fg: 'text-green-800',  ring: 'ring-green-200',  clickable: true },
    { key: 'inconsistent', label: 'Inconsistentes',    value: fmt(get('inconsistent')), icon: AlertTriangle, bg: 'bg-red-50',  fg: 'text-red-800',    ring: 'ring-red-200',    clickable: true },
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
      {cards.map((card) => {
        const isFilterCard = card.clickable !== false;
        const isActive = isFilterCard && activeFilter === card.key;
        const Icon = card.icon;
        const baseClass = `text-left rounded-xl ${card.bg} p-4 ring-1 ring-inset ${card.ring} transition-all`;
        const interactClass = isFilterCard
          ? `cursor-pointer hover:shadow-sm ${isActive ? 'ring-2 ring-offset-2 ring-accent-300' : ''}`
          : 'cursor-default';
        return (
          <button
            key={card.key}
            type="button"
            onClick={() => {
              if (isFilterCard && card.key !== 'amount') {
                onFilterChange(card.key as Props['activeFilter']);
              }
            }}
            disabled={!isFilterCard}
            className={`${baseClass} ${interactClass}`}
          >
            <div className="flex items-center justify-between mb-1">
              <Icon className={`h-4 w-4 ${card.fg}`} />
            </div>
            <div className={`text-2xl font-bold ${card.fg} truncate`}>{card.value}</div>
            <div className={`text-xs ${card.fg} opacity-80`}>{card.label}</div>
          </button>
        );
      })}
    </div>
  );
}
