import { useState } from 'react';
import { Search } from 'lucide-react';
import KpiCards from '@/components/SuperAdmin/KpiCards';
import CommercesTable from '@/components/SuperAdmin/CommercesTable';
import type { CommerceListItem } from '@/types';

const FILTER_TO_STATUS: Record<string, string | undefined> = {
  pending: 'pending',
  expiring: undefined, // backend treats as 'active' but we filter client-side
  suspended: 'suspended',
};

export default function SuperAdminCommercesTab() {
  const [filter, setFilter] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [drawerCommerceId, setDrawerCommerceId] = useState<number | null>(null);
  const [pendingAction, setPendingAction] = useState<{
    action: 'approve' | 'renew' | 'reactivate' | 'change_plan' | 'suspend';
    commerce: CommerceListItem;
  } | null>(null);

  const queryParams = {
    status: filter ? FILTER_TO_STATUS[filter] : undefined,
    q: search || undefined,
  };

  return (
    <div className="space-y-6">
      <KpiCards activeFilter={filter} onFilterChange={setFilter} />

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="flex gap-2 flex-wrap">
          {[
            { key: null, label: 'Todos' },
            { key: 'pending', label: 'Pendientes' },
            { key: 'expiring', label: 'Por vencer' },
            { key: 'suspended', label: 'Suspendidos' },
          ].map((f) => (
            <button
              key={f.key ?? 'all'}
              onClick={() => setFilter(f.key)}
              className={`rounded-full px-3 py-1.5 text-sm font-medium ${filter === f.key ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="relative flex-1 sm:max-w-xs sm:ml-auto">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar comercio o email..."
            className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
          />
        </div>
      </div>

      <CommercesTable
        filters={queryParams}
        onRowClick={(id) => setDrawerCommerceId(id)}
        onAction={(action, commerce) => setPendingAction({ action, commerce })}
      />

      {/* Drawer + modals are wired in Task 28 — for now, both are placeholders */}
      {drawerCommerceId !== null && (
        <div className="hidden">{drawerCommerceId}</div>
      )}
      {pendingAction !== null && (
        <div className="hidden">{pendingAction.action}</div>
      )}
    </div>
  );
}
