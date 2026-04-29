import { Search, RefreshCw, Download } from 'lucide-react';
import Button from '@/components/UI/Button';
import { useDevices } from '@/hooks/useDevices';
import { useAppInstances } from '@/hooks/useAppInstances';

export type Period = 'today' | 'yesterday' | 'last7' | 'last30' | 'thisMonth' | 'lastMonth' | 'custom';

export interface ToolbarFilters {
  q: string;
  instance_ids: number[];
  device_ids: number[];
  period: Period;
}

interface Props {
  filters: ToolbarFilters;
  onChange: (next: ToolbarFilters) => void;
  onRefresh: () => void;
  onExport: () => void;
  exporting?: boolean;
}

const PERIODS: Array<{ key: Period; label: string }> = [
  { key: 'today',      label: 'Hoy' },
  { key: 'yesterday',  label: 'Ayer' },
  { key: 'last7',      label: 'Ultimos 7 dias' },
  { key: 'last30',     label: 'Ultimos 30 dias' },
  { key: 'thisMonth',  label: 'Este mes' },
  { key: 'lastMonth',  label: 'Mes pasado' },
];

export default function NotificationsToolbar({ filters, onChange, onRefresh, onExport, exporting }: Props) {
  const { data: devices = [] } = useDevices();
  const { data: instances = [] } = useAppInstances();

  return (
    <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div className="flex flex-wrap gap-2">
        <select
          value={filters.instance_ids[0] ?? ''}
          onChange={(e) => {
            const v = e.target.value ? Number(e.target.value) : null;
            onChange({ ...filters, instance_ids: v ? [v] : [] });
          }}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
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
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
        >
          <option value="">Todos los dispositivos</option>
          {devices.map((d) => (
            <option key={d.id} value={d.id}>{d.alias ?? d.name}</option>
          ))}
        </select>

        <select
          value={filters.period}
          onChange={(e) => onChange({ ...filters, period: e.target.value as Period })}
          className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm"
        >
          {PERIODS.map((p) => <option key={p.key} value={p.key}>{p.label}</option>)}
        </select>
      </div>

      <div className="relative flex-1 max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          value={filters.q}
          onChange={(e) => onChange({ ...filters, q: e.target.value })}
          placeholder="Buscar codigo, monto o pagador..."
          className="w-full rounded-lg border border-gray-300 bg-white pl-9 pr-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
        />
      </div>

      <div className="flex gap-2 lg:ml-auto">
        <Button variant="outline" size="md" icon={<RefreshCw className="h-4 w-4" />} onClick={onRefresh}>
          Actualizar
        </Button>
        <Button variant="dark" size="md" icon={<Download className="h-4 w-4" />} onClick={onExport} loading={exporting}>
          Exportar
        </Button>
      </div>
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
