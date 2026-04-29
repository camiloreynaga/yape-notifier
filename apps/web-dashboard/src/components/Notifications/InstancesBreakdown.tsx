import { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { useNotificationsByInstance } from '@/hooks/useNotificationsByInstance';

interface Props {
  start_date?: string;
  end_date?: string;
}

export default function InstancesBreakdown({ start_date, end_date }: Props) {
  const [open, setOpen] = useState(false);
  const { data, isLoading } = useNotificationsByInstance({ start_date, end_date });
  const rows = data?.data ?? [];

  return (
    <div className="rounded-xl border border-gray-200 bg-white">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-4 py-3 text-sm font-medium text-gray-800 hover:bg-gray-50"
      >
        <span className="flex items-center gap-2">
          {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          Operaciones por instancia
        </span>
        <span className="text-xs text-gray-500">{rows.length} instancias</span>
      </button>
      {open && (
        <div className="border-t border-gray-200 overflow-x-auto">
          {isLoading ? (
            <div className="p-6 text-center text-sm text-gray-500">Cargando...</div>
          ) : rows.length === 0 ? (
            <div className="p-6 text-center text-sm text-gray-500">Sin datos en el periodo</div>
          ) : (
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left font-semibold text-gray-600">Instancia</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Ops</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Validadas</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Pendientes</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Inconsistentes</th>
                  <th className="px-3 py-2 text-right font-semibold text-gray-600">Monto S/</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((r) => (
                  <tr key={r.instance_id}>
                    <td className="px-3 py-2 font-medium text-gray-900">{r.instance_label}</td>
                    <td className="px-3 py-2 text-right">{r.total}</td>
                    <td className="px-3 py-2 text-right text-green-700">{r.validated}</td>
                    <td className="px-3 py-2 text-right text-yellow-700">{r.pending}</td>
                    <td className="px-3 py-2 text-right text-red-700">{r.inconsistent}</td>
                    <td className="px-3 py-2 text-right font-semibold">S/ {r.amount_total}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
