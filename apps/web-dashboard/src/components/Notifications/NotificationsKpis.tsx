import { Bell, Clock, CheckCircle, AlertTriangle } from 'lucide-react';
import type { NotificationStatistics } from '@/types';

interface Props {
  stats?: NotificationStatistics;
  loading?: boolean;
  activeFilter: 'all' | 'pending' | 'validated' | 'inconsistent';
  onFilterChange: (filter: 'all' | 'pending' | 'validated' | 'inconsistent') => void;
}

interface CardConfig {
  key: 'all' | 'pending' | 'validated' | 'inconsistent';
  label: string;
  count: number;
  icon: React.ComponentType<{ className?: string }>;
  bg: string;
  fg: string;
  ring: string;
}

export default function NotificationsKpis({ stats, loading, activeFilter, onFilterChange }: Props) {
  if (loading || !stats) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 rounded-xl bg-gray-100 animate-pulse" />
        ))}
      </div>
    );
  }

  const total = stats.total ?? 0;
  // backend returns by_status as Record<string, number>, e.g. { pending: 100, validated: 80 }
  const byStatus = stats.by_status ?? {};
  const get = (s: string) => byStatus[s] ?? 0;

  const cards: CardConfig[] = [
    { key: 'all',          label: 'Total del periodo',     count: total,                  icon: Bell,           bg: 'bg-white',       fg: 'text-gray-800',   ring: 'ring-gray-200' },
    { key: 'pending',      label: 'Pendientes',            count: get('pending'),         icon: Clock,          bg: 'bg-yellow-50',   fg: 'text-yellow-800', ring: 'ring-yellow-200' },
    { key: 'validated',    label: 'Validadas',             count: get('validated'),       icon: CheckCircle,    bg: 'bg-green-50',    fg: 'text-green-800',  ring: 'ring-green-200' },
    { key: 'inconsistent', label: 'Inconsistentes',        count: get('inconsistent'),    icon: AlertTriangle,  bg: 'bg-red-50',      fg: 'text-red-800',    ring: 'ring-red-200' },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const isActive = activeFilter === card.key;
        const Icon = card.icon;
        return (
          <button
            key={card.key}
            onClick={() => onFilterChange(card.key)}
            className={`text-left rounded-xl ${card.bg} p-5 ring-1 ring-inset ${card.ring} transition-all hover:scale-[1.01] hover:shadow-sm ${isActive ? 'ring-2 ring-offset-2 ring-accent-300' : ''}`}
          >
            <div className="flex items-center justify-between mb-2">
              <Icon className={`h-5 w-5 ${card.fg}`} />
            </div>
            <div className={`text-3xl font-bold ${card.fg}`}>{card.count}</div>
            <div className={`text-sm ${card.fg} opacity-80`}>{card.label}</div>
          </button>
        );
      })}
    </div>
  );
}
