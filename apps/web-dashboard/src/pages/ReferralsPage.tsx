import { useEffect, useState, useCallback } from 'react';
import { apiService } from '@/services/api';
import type {
  ReferralStats,
  ReferralCommerceRow,
  ReferralCommission,
  PayoutAccount,
  CommissionStatus,
  PaginatedResponse,
} from '@/types';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { Copy, Check, Share2, Banknote, CheckCircle2, AlertCircle } from 'lucide-react';

const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
});

function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return currencyFormatter.format(value);
}

function StatusPill({ status }: { status: ReferralCommerceRow['status'] }) {
  const map: Record<ReferralCommerceRow['status'], string> = {
    active: 'bg-green-100 text-green-800',
    pending: 'bg-amber-100 text-amber-800',
    suspended: 'bg-red-100 text-red-800',
  };
  const labels: Record<ReferralCommerceRow['status'], string> = {
    active: 'Activo',
    pending: 'Pendiente',
    suspended: 'Suspendido',
  };
  return (
    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${map[status]}`}>
      {labels[status]}
    </span>
  );
}

function CommissionStatusPill({ status }: { status: CommissionStatus }) {
  const map: Record<CommissionStatus, string> = {
    pending: 'bg-amber-100 text-amber-800',
    approved: 'bg-blue-100 text-blue-800',
    paid: 'bg-green-100 text-green-800',
    void: 'bg-gray-100 text-gray-500',
  };
  const labels: Record<CommissionStatus, string> = {
    pending: 'Pendiente',
    approved: 'Aprobada',
    paid: 'Pagada',
    void: 'Anulada',
  };
  return (
    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${map[status]}`}>
      {labels[status]}
    </span>
  );
}

function SkeletonCard() {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 animate-pulse">
      <div className="h-4 bg-slate-200 rounded w-1/3 mb-3" />
      <div className="h-8 bg-slate-200 rounded w-1/2" />
    </div>
  );
}

export default function ReferralsPage() {
  // Data state
  const [stats, setStats] = useState<ReferralStats | null>(null);
  const [referrals, setReferrals] = useState<ReferralCommerceRow[]>([]);
  const [payout, setPayout] = useState<PayoutAccount | null>(null);
  const [commissions, setCommissions] = useState<PaginatedResponse<ReferralCommission> | null>(null);

  // Loading state
  const [loadingMain, setLoadingMain] = useState(true);
  const [loadingCommissions, setLoadingCommissions] = useState(false);

  // Error state
  const [mainError, setMainError] = useState<string | null>(null);
  const [commissionsError, setCommissionsError] = useState<string | null>(null);
  const [payoutError, setPayoutError] = useState<string | null>(null);

  // UI state
  const [copiedLink, setCopiedLink] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(1);

  // Payout form state
  const [payoutForm, setPayoutForm] = useState({
    payout_bank: '',
    payout_account_type: '' as '' | 'corriente' | 'ahorros' | 'cci',
    payout_account_number: '',
    payout_account_holder: '',
    payout_account_holder_doc: '',
  });
  const [savingPayout, setSavingPayout] = useState(false);
  const [savedPayout, setSavedPayout] = useState(false);

  // Load main data (stats, referrals, payout) in parallel on mount
  const loadMain = useCallback(async () => {
    setLoadingMain(true);
    setMainError(null);

    const [statsResult, referralsResult, payoutResult] = await Promise.allSettled([
      apiService.getReferralStats(),
      apiService.getReferrals(),
      apiService.getPayoutAccount(),
    ]);

    if (statsResult.status === 'fulfilled') {
      setStats(statsResult.value);
    } else {
      setMainError('No se pudieron cargar los datos de referidos.');
    }

    if (referralsResult.status === 'fulfilled') {
      setReferrals(referralsResult.value.data);
    }

    if (payoutResult.status === 'fulfilled') {
      const p = payoutResult.value;
      setPayout(p);
      setPayoutForm({
        payout_bank: p.payout_bank ?? '',
        payout_account_type: (p.payout_account_type as '' | 'corriente' | 'ahorros' | 'cci') ?? '',
        payout_account_number: p.payout_account_number ?? '',
        payout_account_holder: p.payout_account_holder ?? '',
        payout_account_holder_doc: p.payout_account_holder_doc ?? '',
      });
    }

    setLoadingMain(false);
  }, []);

  // Load commissions separately; refetch when filter or page changes
  const loadCommissions = useCallback(async () => {
    setLoadingCommissions(true);
    setCommissionsError(null);
    try {
      const params: { status?: string; page?: number } = { page };
      if (statusFilter) params.status = statusFilter;
      const result = await apiService.getMyCommissions(params);
      setCommissions(result);
    } catch {
      setCommissionsError('No se pudieron cargar las comisiones.');
    } finally {
      setLoadingCommissions(false);
    }
  }, [statusFilter, page]);

  useEffect(() => {
    loadMain();
  }, [loadMain]);

  useEffect(() => {
    loadCommissions();
  }, [loadCommissions]);

  // Copy share link to clipboard
  const shareUrl = stats
    ? `${window.location.origin}/register?ref=${stats.referral_code}`
    : '';

  const handleCopyLink = () => {
    if (!shareUrl) return;
    navigator.clipboard.writeText(shareUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 1500);
  };

  const handleWhatsApp = () => {
    const msg = `Únete con mi código de referido y empieza a usar Yape Notifier. Regístrate aquí: ${shareUrl}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank');
  };

  // Save payout account
  const handleSavePayout = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingPayout(true);
    setPayoutError(null);
    try {
      const result = await apiService.updatePayoutAccount({
        payout_bank: payoutForm.payout_bank,
        payout_account_type: (payoutForm.payout_account_type || 'corriente') as 'corriente' | 'ahorros' | 'cci',
        payout_account_number: payoutForm.payout_account_number,
        payout_account_holder: payoutForm.payout_account_holder,
        payout_account_holder_doc: payoutForm.payout_account_holder_doc,
      });
      setSavedPayout(true);
      setTimeout(() => setSavedPayout(false), 2000);
      // Refetch to update badge
      const [updatedPayout, updatedStats] = await Promise.allSettled([
        apiService.getPayoutAccount(),
        apiService.getReferralStats(),
      ]);
      if (updatedPayout.status === 'fulfilled') setPayout(updatedPayout.value);
      if (updatedStats.status === 'fulfilled') setStats(updatedStats.value);
      void result;
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      const msg =
        e.response?.data?.message ||
        Object.values(e.response?.data?.errors ?? {}).flat().join(', ') ||
        'Error al guardar la cuenta de pago.';
      setPayoutError(msg);
    } finally {
      setSavingPayout(false);
    }
  };

  // --- RENDER ---

  if (loadingMain) {
    return (
      <div className="space-y-6 p-6">
        <SkeletonCard />
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[0, 1, 2, 3].map((i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      {/* Top error banner */}
      {mainError && (
        <div className="bg-red-50 border border-red-200 text-red-800 p-3 rounded flex items-center gap-2">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          {mainError}
        </div>
      )}

      {/* ================================================================
          SECTION 1 — Share card
      ================================================================ */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6">
        <div className="flex items-start gap-3 mb-4">
          <div className="bg-primary-50 rounded-xl p-2">
            <Share2 className="h-6 w-6 text-primary-600" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-gray-900">Tu código de referido</h2>
            <p className="text-sm text-gray-500 mt-0.5">
              Comparte tu código y gana comisiones por cada comercio que se registre.
            </p>
          </div>
        </div>

        {stats ? (
          <>
            <div className="mb-4">
              <span className="font-mono text-2xl tracking-wider font-bold text-primary-700 bg-primary-50 px-4 py-2 rounded-lg inline-block">
                {stats.referral_code}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={shareUrl}
                className="flex-1 rounded-md border border-gray-300 bg-gray-50 px-3 py-2 text-sm text-gray-700 focus:outline-none"
              />
              <button
                onClick={handleCopyLink}
                className="flex items-center gap-1.5 px-4 py-2 rounded-md bg-primary-600 text-white text-sm font-medium hover:bg-primary-700 transition-colors"
              >
                {copiedLink ? (
                  <>
                    <Check className="h-4 w-4" />
                    Copiado
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4" />
                    Copiar
                  </>
                )}
              </button>
              <button
                onClick={handleWhatsApp}
                className="flex items-center gap-1.5 px-4 py-2 rounded-md border border-green-600 text-green-700 text-sm font-medium hover:bg-green-50 transition-colors"
              >
                <Share2 className="h-4 w-4" />
                WhatsApp
              </button>
            </div>
          </>
        ) : (
          <p className="text-sm text-gray-400">No se pudo cargar el código de referido.</p>
        )}
      </div>

      {/* ================================================================
          SECTION 2 — KPI cards
      ================================================================ */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200 p-5">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Comisiones este mes
          </p>
          <p className="text-2xl font-bold text-gray-900">
            {stats ? formatCurrency(stats.month_earnings) : '—'}
          </p>
        </div>
        <div className="bg-white rounded-2xl border border-slate-200 p-5">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Saldo pendiente
          </p>
          <p className="text-2xl font-bold text-amber-600">
            {stats ? formatCurrency(stats.pending_balance) : '—'}
          </p>
        </div>
        <div className="bg-white rounded-2xl border border-slate-200 p-5">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Total pagado
          </p>
          <p className="text-2xl font-bold text-green-700">
            {stats ? formatCurrency(stats.lifetime_paid) : '—'}
          </p>
        </div>
        <div className="bg-white rounded-2xl border border-slate-200 p-5">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Referidos activos
          </p>
          <p className="text-2xl font-bold text-gray-900">
            {stats ? stats.active_referrals_count : '—'}
          </p>
        </div>
      </div>

      {/* ================================================================
          SECTION 3 — Payout account form
      ================================================================ */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="bg-slate-50 rounded-xl p-2">
              <Banknote className="h-6 w-6 text-slate-600" />
            </div>
            <h2 className="text-xl font-bold text-gray-900">Cuenta para pagos</h2>
          </div>
          {payout && (
            payout.is_complete ? (
              <span className="flex items-center gap-1 px-3 py-1 rounded-full bg-green-100 text-green-800 text-xs font-semibold">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Completa
              </span>
            ) : (
              <span className="flex items-center gap-1 px-3 py-1 rounded-full bg-amber-100 text-amber-800 text-xs font-semibold">
                <AlertCircle className="h-3.5 w-3.5" />
                Falta configurar
              </span>
            )
          )}
        </div>

        <p className="text-sm text-gray-500 mb-5">
          Esta cuenta se usará para depositar tus comisiones aprobadas.
        </p>

        {payoutError && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-800 p-3 rounded text-sm">
            {payoutError}
          </div>
        )}

        <form onSubmit={handleSavePayout}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Banco</label>
              <input
                type="text"
                value={payoutForm.payout_bank}
                onChange={(e) => setPayoutForm({ ...payoutForm, payout_bank: e.target.value })}
                placeholder="Ej. BCP, Interbank, BBVA..."
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de cuenta</label>
              <select
                value={payoutForm.payout_account_type}
                onChange={(e) =>
                  setPayoutForm({
                    ...payoutForm,
                    payout_account_type: e.target.value as '' | 'corriente' | 'ahorros' | 'cci',
                  })
                }
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
              >
                <option value="">Seleccionar...</option>
                <option value="corriente">Corriente</option>
                <option value="ahorros">Ahorros</option>
                <option value="cci">CCI</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Número de cuenta</label>
              <input
                type="text"
                value={payoutForm.payout_account_number}
                onChange={(e) =>
                  setPayoutForm({ ...payoutForm, payout_account_number: e.target.value })
                }
                placeholder="Ej. 19512345678901"
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Titular</label>
              <input
                type="text"
                value={payoutForm.payout_account_holder}
                onChange={(e) =>
                  setPayoutForm({ ...payoutForm, payout_account_holder: e.target.value })
                }
                placeholder="Nombre completo del titular"
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                DNI / RUC del titular
              </label>
              <input
                type="text"
                value={payoutForm.payout_account_holder_doc}
                onChange={(e) =>
                  setPayoutForm({ ...payoutForm, payout_account_holder_doc: e.target.value })
                }
                placeholder="Número de documento"
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
              />
            </div>
          </div>

          <div className="flex justify-end items-center gap-3">
            {savedPayout && (
              <span className="flex items-center gap-1 text-sm text-green-700 font-medium">
                <CheckCircle2 className="h-4 w-4" />
                Guardado
              </span>
            )}
            <button
              type="submit"
              disabled={savingPayout}
              className="px-5 py-2 rounded-md bg-primary-600 text-white text-sm font-medium hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {savingPayout ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        </form>
      </div>

      {/* ================================================================
          SECTION 4 — My referrals table
      ================================================================ */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <h2 className="text-xl font-bold text-gray-900">
            Mis ahijados
            {referrals.length > 0 && (
              <span className="ml-2 text-sm font-normal text-gray-500">
                ({referrals.length} en total)
              </span>
            )}
          </h2>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Nombre
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Plan
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Estado
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Alta
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Pagado total
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {referrals.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-gray-500 text-sm">
                    Aún no tienes referidos. Comparte tu código para empezar a ganar comisiones.
                  </td>
                </tr>
              ) : (
                referrals.map((r) => (
                  <tr key={r.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {r.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {r.plan?.name ?? '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusPill status={r.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(r.created_at), 'dd/MM/yyyy', { locale: es })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {r.total_paid !== null ? formatCurrency(r.total_paid) : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ================================================================
          SECTION 5 — My commissions table
      ================================================================ */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between flex-wrap gap-3">
          <h2 className="text-xl font-bold text-gray-900">Mis comisiones</h2>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
            className="rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 text-sm"
          >
            <option value="">Todas</option>
            <option value="pending">Pendientes</option>
            <option value="approved">Aprobadas</option>
            <option value="paid">Pagadas</option>
            <option value="void">Anuladas</option>
          </select>
        </div>

        {commissionsError && (
          <div className="mx-6 mt-4 bg-red-50 border border-red-200 text-red-800 p-3 rounded text-sm">
            {commissionsError}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Fecha
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Ahijado
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Monto base
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  %
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Comisión
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Estado
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Referencia
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Fecha pago
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loadingCommissions ? (
                <tr>
                  <td colSpan={8} className="px-6 py-10 text-center">
                    <div className="flex justify-center">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
                    </div>
                  </td>
                </tr>
              ) : !commissions || commissions.data.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-10 text-center text-gray-500 text-sm">
                    No hay comisiones para mostrar.
                  </td>
                </tr>
              ) : (
                commissions.data.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(c.created_at), 'dd/MM/yyyy', { locale: es })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {c.referred?.name ?? '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {formatCurrency(parseFloat(c.base_amount))}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {(parseFloat(c.commission_rate) * 100).toFixed(0)}%
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                      {formatCurrency(parseFloat(c.amount))}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <CommissionStatusPill status={c.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {c.payout_reference ?? '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {c.paid_at
                        ? format(new Date(c.paid_at), 'dd/MM/yyyy', { locale: es })
                        : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {commissions && commissions.last_page > 1 && (
          <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between">
            <p className="text-sm text-gray-500">
              Página {commissions.current_page} de {commissions.last_page} ({commissions.total} resultados)
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={commissions.current_page <= 1}
                className="px-3 py-1.5 rounded-md border border-gray-300 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Anterior
              </button>
              <button
                onClick={() => setPage((p) => Math.min(commissions.last_page, p + 1))}
                disabled={commissions.current_page >= commissions.last_page}
                className="px-3 py-1.5 rounded-md border border-gray-300 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
