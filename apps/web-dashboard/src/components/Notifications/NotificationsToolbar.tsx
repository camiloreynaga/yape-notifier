import { useState } from 'react';
import { Search, RefreshCw, Download, Filter, X } from 'lucide-react';
import Button from '@/components/UI/Button';
import { useDevices } from '@/hooks/useDevices';
import { useAppInstances } from '@/hooks/useAppInstances';

export type Period = 'today' | 'yesterday' | 'last7' | 'last30' | 'thisMonth' | 'lastMonth' | 'custom';
export type StatusFilter = 'all' | 'pending' | 'validated' | 'inconsistent';

export interface ToolbarFilters {
  q: string;
  instance_ids: number[];
  device_ids: number[];
  period: Period;
  min_amount?: number;
  max_amount?: number;
}

interface Props {
  filters: ToolbarFilters;
  status: StatusFilter;
  onChange: (next: ToolbarFilters) => void;
  onStatusChange: (s: StatusFilter) => void;
  onRefresh: () => void;
  onExport: () => void;
  exporting?: boolean;
  pendingCount?: number;
  validatedCount?: number;
}

const PERIODS: Array<{ key: Period; label: string }> = [
  { key: 'today',      label: 'Hoy' },
  { key: 'yesterday',  label: 'Ayer' },
  { key: 'last7',      label: 'Últimos 7 días' },
  { key: 'last30',     label: 'Últimos 30 días' },
  { key: 'thisMonth',  label: 'Este mes' },
  { key: 'lastMonth',  label: 'Mes pasado' },
];

const STATUS_CHIPS: Array<{ key: StatusFilter; label: string }> = [
  { key: 'all', label: 'Todos' },
  { key: 'pending', label: 'Pendientes' },
  { key: 'validated', label: 'Validados' },
  { key: 'inconsistent', label: 'Observados' },
];

export default function NotificationsToolbar({
  filters,
  status,
  onChange,
  onStatusChange,
  onRefresh,
  onExport,
  exporting,
  pendingCount,
  validatedCount,
}: Props) {
  const { data: devices = [] } = useDevices();
  const { data: instances = [] } = useAppInstances();
  const [showAdvanced, setShowAdvanced] = useState<boolean>(
    Boolean(filters.min_amount || filters.max_amount)
  );

  const hasAdvanced = Boolean(filters.min_amount || filters.max_amount);

  const chipCount = (k: StatusFilter): number | undefined => {
    if (k === 'pending') return pendingCount;
    if (k === 'validated') return validatedCount;
    return undefined;
  };

  return (
    <div className="space-y-3">
      {/* Row 1: Status chips + Action buttons */}
      <div className="flex flex-col sm:flex-row gap-3 sm:items-center">
        <div className="flex gap-1.5 flex-wrap">
          {STATUS_CHIPS.map((c) => {
            const isActive = status === c.key;
            const count = chipCount(c.key);
            return (
              <button
                key={c.key}
                type="button"
                onClick={() => onStatusChange(c.key)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
                  isActive
                    ? 'bg-primary-800 text-white'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                {c.label}
                {count !== undefined && count > 0 && (
                  <span
                    className={`inline-flex items-center justify-center min-w-[18px] h-[18px] rounded-full text-[10px] font-bold tabular-nums ${
                      isActive ? 'bg-accent-300 text-primary-900' : 'bg-slate-100 text-slate-700'
                    }`}
                  >
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        <div className="flex gap-2 sm:ml-auto">
          <Button
            variant="ghost"
            size="md"
            icon={<RefreshCw className="h-4 w-4" />}
            onClick={onRefresh}
          >
            <span className="hidden sm:inline">Actualizar</span>
          </Button>
          <Button
            variant="outline"
            size="md"
            icon={<Download className="h-4 w-4" />}
            onClick={onExport}
            loading={exporting}
          >
            <span className="hidden sm:inline">Exportar</span>
          </Button>
        </div>
      </div>

      {/* Row 2: Search + Period + Instance + Device */}
      <div className="flex flex-col lg:flex-row gap-2 items-stretch lg:items-center">
        <div className="relative flex-1 lg:max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            value={filters.q}
            onChange={(e) => onChange({ ...filters, q: e.target.value })}
            placeholder="Buscar código, monto o pagador..."
            className="w-full rounded-lg border border-slate-200 bg-white pl-9 pr-3 py-2 text-sm placeholder-slate-400 focus:border-primary-500 focus:ring-2 focus:ring-primary-500/15 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <select
            value={filters.period}
            onChange={(e) => onChange({ ...filters, period: e.target.value as Period })}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 focus:border-primary-500 focus:outline-none"
          >
            {PERIODS.map((p) => (
              <option key={p.key} value={p.key}>{p.label}</option>
            ))}
          </select>

          <select
            value={filters.instance_ids[0] ?? ''}
            onChange={(e) => {
              const v = e.target.value ? Number(e.target.value) : null;
              onChange({ ...filters, instance_ids: v ? [v] : [] });
            }}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 focus:border-primary-500 focus:outline-none"
          >
            <option value="">Todas las instancias</option>
            {instances.map((i) => (
              <option key={i.id} value={i.id}>{i.instance_label ?? `Instancia #${i.id}`}</option>
            ))}
          </select>

          <select
            value={filters.device_ids[0] ?? ''}
            onChange={(e) => {
              const v = e.target.value ? Number(e.target.value) : null;
              onChange({ ...filters, device_ids: v ? [v] : [] });
            }}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 focus:border-primary-500 focus:outline-none"
          >
            <option value="">Todos los dispositivos</option>
            {devices.map((d) => (
              <option key={d.id} value={d.id}>{d.alias ?? d.name}</option>
            ))}
          </select>

          <button
            type="button"
            onClick={() => setShowAdvanced((v) => !v)}
            className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
              showAdvanced || hasAdvanced
                ? 'border-primary-600 bg-primary-50 text-primary-800'
                : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
            }`}
          >
            <Filter className="h-3.5 w-3.5" />
            Más filtros
            {hasAdvanced && <span className="ml-1 inline-block w-1.5 h-1.5 rounded-full bg-primary-600" />}
          </button>
        </div>
      </div>

      {/* Row 3: Advanced filters (collapsible) */}
      {showAdvanced && (
        <div className="flex flex-wrap gap-2 items-center bg-slate-50 border border-slate-200 rounded-lg p-3">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider mr-1">Monto</span>
          <input
            type="number"
            min="0"
            step="0.01"
            placeholder="Mín. S/"
            value={filters.min_amount ?? ''}
            onChange={(e) => onChange({ ...filters, min_amount: e.target.value ? Number(e.target.value) : undefined })}
            className="w-24 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-sm tabular-nums focus:border-primary-500 focus:outline-none"
          />
          <span className="text-slate-400">—</span>
          <input
            type="number"
            min="0"
            step="0.01"
            placeholder="Máx. S/"
            value={filters.max_amount ?? ''}
            onChange={(e) => onChange({ ...filters, max_amount: e.target.value ? Number(e.target.value) : undefined })}
            className="w-24 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-sm tabular-nums focus:border-primary-500 focus:outline-none"
          />
          {hasAdvanced && (
            <button
              type="button"
              onClick={() => onChange({ ...filters, min_amount: undefined, max_amount: undefined })}
              className="ml-auto inline-flex items-center gap-1 text-xs text-slate-500 hover:text-slate-700"
            >
              <X className="h-3 w-3" /> Limpiar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

// Helper for parent to convert Period → start_date/end_date timestamps.
// Returns full datetime strings (YYYY-MM-DD HH:MM:SS) so backend SQL comparisons
// include the entire end day. Using YYYY-MM-DD only breaks because PostgreSQL
// interprets it as 00:00:00, excluding rows from the end day's afternoon.
export function periodToRange(period: Period): { start_date?: string; end_date?: string } {
  const today = new Date();
  // Format as 'YYYY-MM-DD HH:MM:SS' in local time (backend stores in same TZ)
  const fmt = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };
  const startOfDay = (d: Date) => { d.setHours(0, 0, 0, 0); return d; };
  const endOfDay = (d: Date) => { d.setHours(23, 59, 59, 999); return d; };

  switch (period) {
    case 'today':
      return { start_date: fmt(startOfDay(new Date(today))), end_date: fmt(endOfDay(new Date(today))) };
    case 'yesterday': {
      const y = new Date(today); y.setDate(y.getDate() - 1);
      return { start_date: fmt(startOfDay(new Date(y))), end_date: fmt(endOfDay(new Date(y))) };
    }
    case 'last7': {
      const s = new Date(today); s.setDate(s.getDate() - 6);
      return { start_date: fmt(startOfDay(s)), end_date: fmt(endOfDay(new Date(today))) };
    }
    case 'last30': {
      const s = new Date(today); s.setDate(s.getDate() - 29);
      return { start_date: fmt(startOfDay(s)), end_date: fmt(endOfDay(new Date(today))) };
    }
    case 'thisMonth': {
      const s = new Date(today.getFullYear(), today.getMonth(), 1);
      return { start_date: fmt(startOfDay(s)), end_date: fmt(endOfDay(new Date(today))) };
    }
    case 'lastMonth': {
      const s = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      const e = new Date(today.getFullYear(), today.getMonth(), 0);
      return { start_date: fmt(startOfDay(s)), end_date: fmt(endOfDay(e)) };
    }
    default:
      return {};
  }
}
