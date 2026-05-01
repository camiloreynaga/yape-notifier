import { useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiService } from '@/services/api';
import { useNotifications } from '@/hooks/useNotifications';
import { useToast } from '@/hooks/useToast';
import { useDebouncedValue } from '@/hooks/useDebounce';
import { useValidateNotification } from '@/hooks/useValidateNotification';
import { logger } from '@/services/logger';
import type { Notification, NotificationFilters, NotificationStatistics } from '@/types';
import NotificationsKpis from '@/components/Notifications/NotificationsKpis';
import NotificationsToolbar, {
  periodToRange,
  type ToolbarFilters,
  type Period,
} from '@/components/Notifications/NotificationsToolbar';
import NotificationsTable from '@/components/Notifications/NotificationsTable';
import NotificationDrawer from '@/components/Notifications/NotificationDrawer';
import InstancesBreakdown from '@/components/Notifications/InstancesBreakdown';
import { useQuery } from '@tanstack/react-query';

type StatusFilter = 'all' | 'pending' | 'validated' | 'inconsistent';

export default function NotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { showSuccess, showError } = useToast();

  // Toolbar state synced to URL
  const [toolbar, setToolbar] = useState<ToolbarFilters>({
    q: searchParams.get('q') ?? '',
    instance_ids: searchParams.get('instance') ? [Number(searchParams.get('instance'))] : [],
    device_ids: searchParams.get('device') ? [Number(searchParams.get('device'))] : [],
    period: (searchParams.get('period') as Period) || 'last7',
  });
  const [statusFilter, setStatusFilter] = useState<StatusFilter>(
    (searchParams.get('status') as StatusFilter) || 'all'
  );

  const { debouncedValue: debouncedQ } = useDebouncedValue(toolbar.q, 300);

  // Build API filters
  const range = useMemo(() => periodToRange(toolbar.period), [toolbar.period]);
  const apiFilters: NotificationFilters = useMemo(() => ({
    per_page: 50,
    page: 1,
    status: statusFilter === 'all' ? undefined : statusFilter,
    instance_id: toolbar.instance_ids.length > 0 ? toolbar.instance_ids : undefined,
    device_id: toolbar.device_ids.length > 0 ? toolbar.device_ids : undefined,
    start_date: range.start_date,
    end_date: range.end_date,
  }), [statusFilter, toolbar.instance_ids, toolbar.device_ids, range]);

  // Sync URL with state
  const updateUrl = useCallback(() => {
    const next = new URLSearchParams();
    if (toolbar.q) next.set('q', toolbar.q);
    if (toolbar.instance_ids[0]) next.set('instance', String(toolbar.instance_ids[0]));
    if (toolbar.device_ids[0]) next.set('device', String(toolbar.device_ids[0]));
    if (toolbar.period !== 'last7') next.set('period', toolbar.period);
    if (statusFilter !== 'all') next.set('status', statusFilter);
    setSearchParams(next, { replace: true });
  }, [toolbar, statusFilter, setSearchParams]);

  // Notifications data via existing hook
  // useNotifications returns { notifications: PaginatedResponse<Notification> | null, loading, refetch }
  const { notifications: notificationsPage, loading, refetch } = useNotifications({
    filters: apiFilters,
    enabled: true,
    onNewNotification: useCallback((n: Notification) => {
      logger.debug('Nueva notificacion', { id: n.id });
    }, []),
  });

  // Flatten paginated response to array
  const notificationsArray: Notification[] = notificationsPage?.data ?? [];

  // Statistics for KPI cards
  const { data: stats } = useQuery<NotificationStatistics>({
    queryKey: ['notifications', 'statistics', range],
    queryFn: () => apiService.getStatistics({ start_date: range.start_date, end_date: range.end_date }),
    staleTime: 30_000,
  });

  // Client-side filtering: search query + amount range
  const filtered = useMemo(() => {
    let result = notificationsArray;
    if (debouncedQ) {
      const q = debouncedQ.toLowerCase();
      result = result.filter((n) =>
        [n.payer_name, n.security_code, String(n.amount), n.device?.alias, n.device?.name]
          .filter(Boolean)
          .some((v) => String(v).toLowerCase().includes(q))
      );
    }
    if (toolbar.min_amount != null) {
      result = result.filter((n) => Number(n.amount ?? 0) >= toolbar.min_amount!);
    }
    if (toolbar.max_amount != null) {
      result = result.filter((n) => Number(n.amount ?? 0) <= toolbar.max_amount!);
    }
    return result;
  }, [notificationsArray, debouncedQ, toolbar.min_amount, toolbar.max_amount]);

  // Validate / mark inconsistent / revert mutations
  const validate = useValidateNotification();
  const [drawerNotif, setDrawerNotif] = useState<Notification | null>(null);
  const [exporting, setExporting] = useState(false);

  const onValidate = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'validated' },
      {
        onSuccess: () => showSuccess('Notificacion validada'),
        onError: (e) => showError('Error al validar: ' + (e as Error).message),
      }
    );
  };
  const onMarkInconsistent = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'inconsistent' },
      {
        onSuccess: () => showSuccess('Marcada como inconsistente'),
      }
    );
  };
  const onRevert = (n: Notification) => {
    validate.mutate(
      { id: n.id, status: 'pending' },
      {
        onSuccess: () => showSuccess('Revertida a pendiente'),
      }
    );
  };

  const onExport = async () => {
    setExporting(true);
    try {
      const data = filtered;
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `notificaciones-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  };

  // Counts for status chip badges (from stats so they reflect period totals)
  const pendingChipCount = stats?.by_status?.pending ?? 0;
  const validatedChipCount = stats?.by_status?.validated ?? 0;

  return (
    <div className="space-y-5" onBlur={updateUrl}>
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Centro de Validación</h1>
          <p className="text-sm text-slate-500 mt-1">
            {filtered.length > 0
              ? `${filtered.length} operacion${filtered.length === 1 ? '' : 'es'} en el período seleccionado`
              : 'Sin operaciones en el período seleccionado'}
          </p>
        </div>
        <div className="flex items-center gap-1.5 text-xs text-slate-500">
          <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          Conexión en tiempo real
        </div>
      </div>

      <NotificationsKpis
        stats={stats}
        notificationsForDuplicates={filtered}
        activeFilter={statusFilter}
        onFilterChange={setStatusFilter}
      />

      <NotificationsToolbar
        filters={toolbar}
        status={statusFilter}
        onChange={(next) => { setToolbar(next); updateUrl(); }}
        onStatusChange={setStatusFilter}
        onRefresh={() => refetch()}
        onExport={onExport}
        exporting={exporting}
        pendingCount={pendingChipCount}
        validatedCount={validatedChipCount}
      />

      <InstancesBreakdown start_date={range.start_date} end_date={range.end_date} />

      <NotificationsTable
        notifications={filtered}
        loading={loading}
        validatingId={validate.isPending ? validate.variables?.id ?? null : null}
        onValidate={onValidate}
        onRevert={onRevert}
        onView={(n) => setDrawerNotif(n)}
      />

      <NotificationDrawer
        notification={drawerNotif}
        onClose={() => setDrawerNotif(null)}
        onValidate={(n) => { onValidate(n); setDrawerNotif(null); }}
        onMarkInconsistent={(n) => { onMarkInconsistent(n); setDrawerNotif(null); }}
        onRevert={(n) => { onRevert(n); setDrawerNotif(null); }}
        busy={validate.isPending}
      />
    </div>
  );
}
