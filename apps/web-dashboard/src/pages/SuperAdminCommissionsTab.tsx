import { useState, useEffect, useCallback } from 'react';
import { format, parseISO } from 'date-fns';
import { es } from 'date-fns/locale';
import { apiService } from '@/services/api';
import type { ReferralCommission, CommissionStatus, PaginatedResponse } from '@/types';

// ── Formatters ────────────────────────────────────────────────────────────────

const currencyFmt = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

function fmt(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  const n = typeof value === 'string' ? parseFloat(value) : value;
  return isNaN(n) ? '—' : currencyFmt.format(n);
}

function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    return format(parseISO(iso), 'dd/MM/yyyy', { locale: es });
  } catch {
    return iso;
  }
}

// ── Status pill ───────────────────────────────────────────────────────────────

const STATUS_CLASSES: Record<CommissionStatus, string> = {
  pending: 'bg-amber-100 text-amber-800',
  approved: 'bg-blue-100 text-blue-800',
  paid: 'bg-green-100 text-green-800',
  void: 'bg-slate-100 text-slate-600',
};

const STATUS_LABELS: Record<CommissionStatus, string> = {
  pending: 'Pendiente',
  approved: 'Aprobada',
  paid: 'Pagada',
  void: 'Anulada',
};

function StatusPill({ status }: { status: CommissionStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASSES[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  );
}

// ── KPI Cards ─────────────────────────────────────────────────────────────────

interface KpiCardsProps {
  commissions: ReferralCommission[];
}

function KpiCards({ commissions }: KpiCardsProps) {
  const sum = (status: CommissionStatus) =>
    commissions
      .filter((c) => c.status === status)
      .reduce((acc, c) => acc + parseFloat(c.amount), 0);

  const pending = sum('pending');
  const approved = sum('approved');
  const paid = sum('paid');

  const cards = [
    { label: 'Pendientes (pág.)', value: fmt(pending), color: 'text-amber-700 bg-amber-50 border-amber-200' },
    { label: 'Aprobadas (pág.)', value: fmt(approved), color: 'text-blue-700 bg-blue-50 border-blue-200' },
    { label: 'Pagadas (pág.)', value: fmt(paid), color: 'text-green-700 bg-green-50 border-green-200' },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      {cards.map((c) => (
        <div key={c.label} className={`rounded-xl border p-4 ${c.color}`}>
          <p className="text-xs font-medium uppercase tracking-wide opacity-70">{c.label}</p>
          <p className="mt-1 text-xl font-bold">{c.value}</p>
        </div>
      ))}
    </div>
  );
}

// ── Pay Modal ─────────────────────────────────────────────────────────────────

interface PayModalProps {
  commission: ReferralCommission;
  onClose: () => void;
  onDone: () => void;
}

function PayModal({ commission, onClose, onDone }: PayModalProps) {
  const [ref, setRef] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const referrer = commission.referrer;
  const hasPayout = !!(referrer?.payout_bank);

  const handleSubmit = async () => {
    if (!ref.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await apiService.payCommission(commission.id, ref.trim());
      onDone();
      onClose();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err?.response?.data?.message ?? 'Error al registrar el pago.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-xl">
        <div className="border-b border-gray-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-900">Pagar comisión #{commission.id}</h2>
        </div>
        <div className="px-6 py-4 space-y-4">
          {/* Referrer payout info */}
          {hasPayout ? (
            <div className="rounded-lg bg-gray-50 p-4 space-y-1 text-sm">
              <p className="font-medium text-gray-700">Cuenta de pago del referidor</p>
              <p className="text-gray-600">Banco: <span className="font-medium">{referrer?.payout_bank}</span></p>
              {referrer?.payout_account_type && (
                <p className="text-gray-600">Tipo: <span className="font-medium">{referrer.payout_account_type}</span></p>
              )}
              {referrer?.payout_account_holder && (
                <p className="text-gray-600">Titular: <span className="font-medium">{referrer.payout_account_holder}</span></p>
              )}
              {referrer?.payout_account_holder_doc && (
                <p className="text-gray-600">Doc: <span className="font-medium">{referrer.payout_account_holder_doc}</span></p>
              )}
              <p className="mt-2 text-base font-semibold text-gray-900">Monto: {fmt(commission.amount)}</p>
            </div>
          ) : (
            <div className="rounded-lg bg-amber-50 border border-amber-200 p-4 text-sm text-amber-800">
              El comercio no tiene cuenta de pago configurada. No se puede registrar el pago.
            </div>
          )}

          {hasPayout && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Número de operación / referencia <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={ref}
                onChange={(e) => setRef(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
                placeholder="Ej. OP-20240115-001"
              />
            </div>
          )}

          {error && (
            <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">{error}</p>
          )}
        </div>
        <div className="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={!hasPayout || !ref.trim() || loading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Procesando...' : 'Confirmar pago'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Void Modal ────────────────────────────────────────────────────────────────

interface VoidModalProps {
  commission: ReferralCommission;
  onClose: () => void;
  onDone: () => void;
}

function VoidModal({ commission, onClose, onDone }: VoidModalProps) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!reason.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await apiService.voidCommission(commission.id, reason.trim());
      onDone();
      onClose();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err?.response?.data?.message ?? 'Error al anular la comisión.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-xl">
        <div className="border-b border-gray-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-900">Anular comisión #{commission.id}</h2>
        </div>
        <div className="px-6 py-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Motivo de anulación <span className="text-red-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value.slice(0, 500))}
              rows={4}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
              placeholder="Describe el motivo de la anulación..."
            />
            <p className="mt-1 text-xs text-gray-400 text-right">{reason.length}/500</p>
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">{error}</p>
          )}
        </div>
        <div className="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={!reason.trim() || loading}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Anulando...' : 'Confirmar anulación'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function SuperAdminCommissionsTab() {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [month, setMonth] = useState<string>('');
  const [page, setPage] = useState(1);
  const [data, setData] = useState<PaginatedResponse<ReferralCommission> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal state
  const [payModal, setPayModal] = useState<ReferralCommission | null>(null);
  const [voidModal, setVoidModal] = useState<ReferralCommission | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiService.listAllCommissions({
        status: statusFilter || undefined,
        month: month || undefined,
        page,
      });
      setData(result);
    } catch {
      setError('No se pudo cargar la lista de comisiones.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, month, page]);

  useEffect(() => {
    reload();
  }, [reload]);

  // Reset page when filters change
  const handleStatusChange = (val: string) => {
    setPage(1);
    setStatusFilter(val);
  };

  const handleMonthChange = (val: string) => {
    setPage(1);
    setMonth(val);
  };

  const handleApprove = async (commission: ReferralCommission) => {
    setError(null);
    try {
      await apiService.approveCommission(commission.id);
      reload();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err?.response?.data?.message ?? 'Error al aprobar la comisión.');
    }
  };

  const commissions = data?.data ?? [];
  const currentPage = data?.current_page ?? 1;
  const lastPage = data?.last_page ?? 1;

  return (
    <div className="space-y-6">
      {/* Header + filters */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-900 flex-1">Comisiones de referidos</h2>
        <div className="flex flex-wrap gap-3">
          <select
            value={statusFilter}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
          >
            <option value="">Todas</option>
            <option value="pending">Pendientes</option>
            <option value="approved">Aprobadas</option>
            <option value="paid">Pagadas</option>
            <option value="void">Anuladas</option>
          </select>
          <div className="flex items-center gap-2">
            <input
              type="month"
              value={month}
              onChange={(e) => handleMonthChange(e.target.value)}
              className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-primary-500 focus:ring-primary-500"
            />
            {month && (
              <button
                onClick={() => handleMonthChange('')}
                className="text-xs text-gray-500 hover:text-gray-700 underline"
              >
                Todos
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Error banner */}
      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* KPI cards */}
      <KpiCards commissions={commissions} />

      {/* Loading spinner */}
      {loading && (
        <div className="flex justify-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      )}

      {/* Table */}
      {!loading && (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Fecha</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Referidor</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Referido</th>
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-500">Base</th>
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-500">%</th>
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-500">Comisión</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Referencia</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">Pagado</th>
                <th className="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-500">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {commissions.length === 0 ? (
                <tr>
                  <td colSpan={11} className="px-4 py-10 text-center text-gray-400">
                    No hay comisiones para los filtros seleccionados.
                  </td>
                </tr>
              ) : (
                commissions.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">#{c.id}</td>
                    <td className="px-4 py-3 text-gray-700 whitespace-nowrap">{fmtDate(c.created_at)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-0.5">
                        <span className="font-medium text-gray-900">{c.referrer?.name ?? `#${c.referrer_commerce_id}`}</span>
                        {c.referrer?.payout_bank ? (
                          <span className="text-xs text-green-700">✓ cuenta</span>
                        ) : (
                          <span className="text-xs text-amber-600">⚠ sin cuenta</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-gray-700">{c.referred?.name ?? `#${c.referred_commerce_id}`}</td>
                    <td className="px-4 py-3 text-right text-gray-700 whitespace-nowrap">{fmt(c.base_amount)}</td>
                    <td className="px-4 py-3 text-right text-gray-500 whitespace-nowrap">
                      {parseFloat(c.commission_rate) * 100}%
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-900 whitespace-nowrap">{fmt(c.amount)}</td>
                    <td className="px-4 py-3">
                      <StatusPill status={c.status} />
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{c.payout_reference ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{fmtDate(c.paid_at)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-center gap-2">
                        {c.status === 'pending' && (
                          <button
                            onClick={() => handleApprove(c)}
                            className="rounded-md bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700"
                          >
                            Aprobar
                          </button>
                        )}
                        {c.status === 'approved' && (
                          <button
                            onClick={() => setPayModal(c)}
                            className="rounded-md bg-blue-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-blue-700"
                          >
                            Pagar
                          </button>
                        )}
                        {c.status !== 'void' && (
                          <button
                            onClick={() => setVoidModal(c)}
                            className="rounded-md border border-red-300 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                          >
                            Anular
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {!loading && lastPage > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-500">
            Página {currentPage} de {lastPage}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={currentPage <= 1}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => Math.min(lastPage, p + 1))}
              disabled={currentPage >= lastPage}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      {/* Modals */}
      {payModal && (
        <PayModal
          commission={payModal}
          onClose={() => setPayModal(null)}
          onDone={reload}
        />
      )}
      {voidModal && (
        <VoidModal
          commission={voidModal}
          onClose={() => setVoidModal(null)}
          onDone={reload}
        />
      )}
    </div>
  );
}
