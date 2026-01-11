/**
 * Componente moderno para mostrar una notificación individual
 * Diseño tipo card con glassmorphism y microinteracciones
 */

import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { Eye, Copy, Check, X, AlertCircle, DollarSign, Shield, Loader2 } from 'lucide-react';
import clsx from 'clsx';
import type { Notification } from '@/types';

interface NotificationCardProps {
  notification: Notification;
  onStatusChange: (id: number, status: 'pending' | 'validated' | 'inconsistent') => void;
  onClick: () => void;
  isUpdating?: boolean;
}

// Colores por app
const appColors = {
  yape: 'from-purple-500 to-purple-600',
  plin: 'from-blue-500 to-blue-600',
  bcp: 'from-orange-500 to-orange-600',
  interbank: 'from-green-500 to-green-600',
  bbva: 'from-blue-600 to-blue-700',
  scotiabank: 'from-red-500 to-red-600',
} as const;

export default function NotificationCard({
  notification,
  onStatusChange,
  onClick,
  isUpdating = false,
}: NotificationCardProps) {
  const appColor = appColors[notification.source_app as keyof typeof appColors] || 'from-gray-500 to-gray-600';

  const getStatusConfig = (status: string) => {
    switch (status) {
      case 'validated':
        return {
          icon: Check,
          label: 'Validado',
          color: 'bg-green-50 text-green-700 ring-green-600/20',
          iconColor: 'text-green-600',
        };
      case 'inconsistent':
        return {
          icon: X,
          label: 'Inconsistente',
          color: 'bg-red-50 text-red-700 ring-red-600/20',
          iconColor: 'text-red-600',
        };
      default:
        return {
          icon: AlertCircle,
          label: 'Pendiente',
          color: 'bg-yellow-50 text-yellow-700 ring-yellow-600/20',
          iconColor: 'text-yellow-600',
        };
    }
  };

  const statusConfig = getStatusConfig(notification.status);
  const StatusIcon = statusConfig.icon;

  return (
    <div
      className="group relative bg-white rounded-xl shadow-sm hover:shadow-xl border border-gray-100 hover:border-gray-200 transition-all duration-300 overflow-hidden cursor-pointer hover:-translate-y-1"
      onClick={onClick}
    >
      {/* Barra de color por app */}
      <div className={clsx('absolute top-0 left-0 right-0 h-1 bg-gradient-to-r', appColor)} />

      {/* Badge de duplicado */}
      {notification.is_duplicate && (
        <div className="absolute top-3 right-3 z-10">
          <div className="flex items-center gap-1 px-2 py-1 rounded-lg bg-orange-50 text-orange-700 ring-1 ring-orange-600/20 text-xs font-semibold">
            <Copy className="h-3 w-3" />
            Duplicado
          </div>
        </div>
      )}

      <div className="p-5">
        {/* Header: Fecha y App */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <span className={clsx(
                'inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold capitalize',
                'bg-gradient-to-r text-white shadow-sm',
                appColor
              )}>
                {notification.source_app || 'N/A'}
              </span>
              <span className="text-xs text-gray-500">
                {format(new Date(notification.received_at), 'HH:mm', { locale: es })}
              </span>
            </div>
            <p className="text-sm text-gray-600">
              {format(new Date(notification.received_at), "d 'de' MMMM, yyyy", { locale: es })}
            </p>
          </div>
        </div>

        {/* Título */}
        <h3 className="text-lg font-semibold text-gray-900 mb-3 line-clamp-2 group-hover:text-primary-600 transition-colors">
          {notification.title}
        </h3>

        {/* Monto */}
        {notification.amount != null && !isNaN(Number(notification.amount)) && (
          <div className="flex items-center gap-2 mb-4 p-3 rounded-lg bg-gradient-to-br from-green-50 to-emerald-50 border border-green-100">
            <div className="flex-shrink-0 p-2 rounded-lg bg-green-100">
              <DollarSign className="h-4 w-4 text-green-600" />
            </div>
            <div>
              <p className="text-xs text-green-600 font-medium">Monto</p>
              <p className="text-xl font-bold text-green-700">
                {notification.currency || 'S/'} {Number(notification.amount).toFixed(2)}
              </p>
            </div>
          </div>
        )}

        {/* Código de seguridad (solo Yape -> Yape) */}
        {notification.security_code && (
          <div className="mb-4 p-3 rounded-lg bg-gradient-to-br from-purple-50 to-indigo-50 border border-purple-200">
            <div className="flex items-center gap-2">
              <div className="flex-shrink-0 p-2 rounded-lg bg-purple-100">
                <Shield className="h-4 w-4 text-purple-600" />
              </div>
              <div>
                <p className="text-xs text-purple-600 font-medium">Código de Seguridad</p>
                <p className="text-2xl font-bold text-purple-700 tracking-wider">
                  {notification.security_code}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Información adicional */}
        <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
          {notification.payer_name && (
            <div>
              <p className="text-xs text-gray-500 mb-0.5">Pagador</p>
              <p className="font-medium text-gray-900 truncate" title={notification.payer_name}>
                {notification.payer_name}
              </p>
            </div>
          )}
          {notification.device?.name && (
            <div>
              <p className="text-xs text-gray-500 mb-0.5">Dispositivo</p>
              <p className="font-medium text-gray-900 truncate" title={notification.device.name}>
                {notification.device.name}
              </p>
            </div>
          )}
        </div>

        {/* Footer: Estado y Acciones */}
        <div className="flex items-center justify-between pt-4 border-t border-gray-100">
          {/* Estado */}
          <div className={clsx(
            'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold ring-1',
            statusConfig.color
          )}>
            <StatusIcon className="h-3.5 w-3.5" />
            {statusConfig.label}
          </div>

          {/* Acciones */}
          <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={onClick}
              className="p-2 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50 transition-colors"
              title="Ver detalle"
            >
              <Eye className="h-4 w-4" />
            </button>

            {/* Selector de estado compacto con indicador de carga */}
            <div className="relative">
              {isUpdating && (
                <div className="absolute inset-0 flex items-center justify-center bg-white/80 rounded-lg z-10">
                  <Loader2 className="h-4 w-4 animate-spin text-primary-600" />
                </div>
              )}
              <select
                value={notification.status}
                onChange={(e) => {
                  e.stopPropagation();
                  onStatusChange(
                    notification.id,
                    e.target.value as 'pending' | 'validated' | 'inconsistent'
                  );
                }}
                disabled={isUpdating}
                className="text-xs px-2 py-1.5 rounded-lg border border-gray-200 bg-white hover:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all font-medium text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={(e) => e.stopPropagation()}
              >
                <option value="pending">Pendiente</option>
                <option value="validated">Validar</option>
                <option value="inconsistent">Inconsistente</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Efecto de hover */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary-500/0 to-primary-500/0 group-hover:from-primary-500/5 group-hover:to-transparent transition-all duration-300 pointer-events-none" />
    </div>
  );
}
