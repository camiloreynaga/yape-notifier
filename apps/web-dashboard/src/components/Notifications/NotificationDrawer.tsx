import { X, Check, AlertTriangle, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';
import Button from '@/components/UI/Button';
import StatusBadge from '@/components/UI/StatusBadge';
import type { Notification } from '@/types';

interface Props {
  notification: Notification | null;
  onClose: () => void;
  onValidate: (n: Notification) => void;
  onMarkInconsistent: (n: Notification) => void;
  onRevert: (n: Notification) => void;
  busy?: boolean;
}

export default function NotificationDrawer({ notification, onClose, onValidate, onMarkInconsistent, onRevert, busy }: Props) {
  if (!notification) return null;

  return (
    <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose}>
      <div
        className="fixed top-0 right-0 h-full w-full sm:w-[480px] bg-white shadow-xl overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold">Detalle de notificacion</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-gray-500 hover:text-gray-700" /></button>
        </div>

        <div className="p-5 space-y-5">
          <section>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Monto</p>
            <p className="text-3xl font-bold text-gray-900">S/ {Number(notification.amount ?? 0).toFixed(2)}</p>
            <div className="mt-2"><StatusBadge status={notification.status} /></div>
          </section>

          {notification.security_code && (
            <section>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Codigo de seguridad</p>
              <p className="font-mono text-2xl text-primary-800 bg-accent-100 inline-block px-3 py-1 rounded-md mt-1">
                {notification.security_code}
              </p>
            </section>
          )}

          <section className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs text-gray-500">Pagador</p>
              <p className="font-medium">{notification.payer_name ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Fecha</p>
              <p className="font-medium">{format(new Date(notification.created_at), 'dd MMM yyyy HH:mm')}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Dispositivo</p>
              <p className="font-medium">{notification.device?.alias ?? notification.device?.name ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Instancia</p>
              <p className="font-medium">{notification.app_instance?.instance_label ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">App origen</p>
              <p className="font-medium">{notification.source_app ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Paquete</p>
              <p className="font-medium font-mono text-xs">{notification.package_name ?? '—'}</p>
            </div>
          </section>

          {notification.body && (
            <section>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">Mensaje original</p>
              <pre className="text-xs whitespace-pre-wrap bg-gray-50 rounded-md p-3 border border-gray-200">{notification.body}</pre>
            </section>
          )}

          <section className="space-y-2 pt-2">
            {notification.status === 'pending' && (
              <>
                <Button variant="success" className="w-full" icon={<Check className="h-4 w-4" />}
                  onClick={() => onValidate(notification)} loading={busy}>
                  Validar notificacion
                </Button>
                <Button variant="outline" className="w-full" icon={<AlertTriangle className="h-4 w-4" />}
                  onClick={() => onMarkInconsistent(notification)}>
                  Marcar inconsistente
                </Button>
              </>
            )}
            {notification.status === 'inconsistent' && (
              <Button variant="outline" className="w-full" icon={<RotateCcw className="h-4 w-4" />}
                onClick={() => onRevert(notification)}>
                Revertir a pendiente
              </Button>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
