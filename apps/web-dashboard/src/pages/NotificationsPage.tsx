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

  // Client-side search by q across visible columns
  const filtered = useMemo(() => {
    if (!debouncedQ) return notificationsArray;
    const q = debouncedQ.toLowerCase();
    return notificationsArray.filter((n) =>
      [n.payer_name, n.security_code, String(n.amount), n.device?.alias, n.device?.name]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q))
    );
  }, [notificationsArray, debouncedQ]);

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

  return (
    <div className="space-y-6" onBlur={updateUrl}>
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Notificaciones</h1>
        <p className="text-sm text-gray-600">Visualiza y valida las notificaciones recibidas.</p>
      </div>

      <NotificationsKpis
        stats={stats}
        activeFilter={statusFilter}
        onFilterChange={setStatusFilter}
      />

      <NotificationsToolbar
        filters={toolbar}
        onChange={(next) => { setToolbar(next); updateUrl(); }}
        onRefresh={() => refetch()}
        onExport={onExport}
        exporting={exporting}
      />

      <InstancesBreakdown start_date={range.start_date} end_date={range.end_date} />

      <NotificationsTable
        notifications={filtered}
        loading={loading}
        validatingId={validate.isPending ? validate.variables?.id ?? null : null}
        onValidate={onValidate}
        onMarkInconsistent={onMarkInconsistent}
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
