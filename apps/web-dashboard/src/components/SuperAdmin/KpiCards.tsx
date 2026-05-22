import { Building2, Clock, AlertTriangle, Ban } from 'lucide-react';
import { useSuperAdminKpis } from '@/hooks/useSuperAdminKpis';

interface KpiCardsProps {
  activeFilter: string | null;
  onFilterChange: (filter: string | null) => void;
}

interface CardConfig {
  key: string;
  label: string;
  count: number;
  icon: React.ComponentType<{ className?: string }>;
  bg: string;
  fg: string;
  ring: string;
}

export default function KpiCards({ activeFilter, onFilterChange }: KpiCardsProps) {
  const { data, isLoading } = useSuperAdminKpis();

  if (isLoading || !data) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-24 rounded-xl bg-gray-100 animate-pulse" />
        ))}
      </div>
    );
  }

  const cards: CardConfig[] = [
    { key: 'all',     label: 'Total',       count: data.total,                          icon: Building2,     bg: 'bg-gray-50',    fg: 'text-gray-700',    ring: 'ring-gray-200' },
    { key: 'pending', label: 'Pendientes',  count: data.pending,                        icon: Clock,         bg: 'bg-yellow-50',  fg: 'text-yellow-800',  ring: 'ring-yellow-200' },
    { key: 'expiring',label: 'Por vencer',  count: data.expiring_soon + data.in_grace,  icon: AlertTriangle, bg: 'bg-orange-50',  fg: 'text-orange-800',  ring: 'ring-orange-200' },
    { key: 'suspended',label: 'Suspendidos',count: data.suspended,                      icon: Ban,           bg: 'bg-red-50',     fg: 'text-red-800',     ring: 'ring-red-200' },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const isActive = activeFilter === card.key || (activeFilter === null && card.key === 'all');
        const Icon = card.icon;
        return (
          <button
            key={card.key}
            onClick={() => onFilterChange(card.key === 'all' ? null : card.key)}
            className={`text-left rounded-xl ${card.bg} p-5 ring-1 ring-inset ${card.ring} transition-all hover:scale-[1.02] hover:shadow-md ${isActive ? 'ring-2 ring-offset-2 ring-primary-500' : ''}`}
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
