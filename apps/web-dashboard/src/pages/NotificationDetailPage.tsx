import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiService } from '@/services/api';
import type { Notification } from '@/types';
import { format } from 'date-fns';
import {
  ArrowLeft,
  Smartphone,
  Package,
  Calendar,
  DollarSign,
  User,
  FileText,
  Code,
  CheckCircle,
  XCircle,
  AlertCircle,
  Copy,
  Check,
} from 'lucide-react';

export default function NotificationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [notification, setNotification] = useState<Notification | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (id) {
      loadNotification(parseInt(id));
    }
  }, [id]);

  const loadNotification = async (notificationId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getNotification(notificationId);
      setNotification(data);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : 'Error al cargar la notificación';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (status: 'pending' | 'validated' | 'inconsistent') => {
    if (!notification) return;

    setStatusLoading(true);
    try {
      const updated = await apiService.updateNotificationStatus(notification.id, status);
      setNotification(updated);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : 'Error al actualizar el estado';
      alert(errorMessage);
    } finally {
      setStatusLoading(false);
    }
  };

  const handleCopyJson = async () => {
    if (!notification?.raw_json) return;

    try {
      await navigator.clipboard.writeText(JSON.stringify(notification.raw_json, null, 2));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Error copying to clipboard:', err);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'validated':
        return (
          <span className="badge badge-success flex items-center gap-1">
            <CheckCircle className="h-3 w-3" />
            Validado
          </span>
        );
      case 'inconsistent':
        return (
          <span className="badge badge-danger flex items-center gap-1">
            <XCircle className="h-3 w-3" />
            Inconsistente
          </span>
        );
      default:
        return (
          <span className="badge badge-warning flex items-center gap-1">
            <AlertCircle className="h-3 w-3" />
            Pendiente
          </span>
        );
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error || !notification) {
    return (
      <div className="space-y-6">
        <Link
          to="/notifications"
          className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver a notificaciones
        </Link>
        <div className="card">
          <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
            <div className="flex">
              <AlertCircle className="h-5 w-5 text-red-400" />
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Error</h3>
                <p className="mt-1 text-sm text-red-700">
                  {error || 'No se pudo cargar la notificación'}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link
            to="/notifications"
            className="btn btn-secondary flex items-center gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Volver
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Detalle de Notificación</h1>
            <p className="mt-1 text-sm text-gray-600">ID: #{notification.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {getStatusBadge(notification.status)}
          {notification.is_duplicate && (
            <span className="badge badge-warning">Duplicado</span>
          )}
        </div>
      </div>

      {/* Información Principal */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Columna Izquierda */}
        <div className="space-y-6">
          {/* Información de Pago */}
          <div className="card">
            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <DollarSign className="h-5 w-5 text-primary-600" />
              Información de Pago
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Título
                </label>
                <p className="text-base text-gray-900">{notification.title}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Mensaje
                </label>
                <p className="text-base text-gray-900">{notification.body}</p>
              </div>
              {notification.amount != null && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Monto
                  </label>
                  <p className="text-2xl font-bold text-gray-900">
                    {notification.currency || 'S/'} {Number(notification.amount).toFixed(2)}
                  </p>
                </div>
              )}
              {notification.payer_name && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Pagador
                  </label>
                  <p className="text-base text-gray-900 flex items-center gap-2">
                    <User className="h-4 w-4 text-gray-400" />
                    {notification.payer_name}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Información de Origen */}
          <div className="card">
            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Package className="h-5 w-5 text-primary-600" />
              Origen
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Aplicación
                </label>
                <p className="text-base text-gray-900">{notification.source_app || 'N/A'}</p>
              </div>
              {notification.package_name && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Package Name
                  </label>
                  <p className="text-sm font-mono text-gray-700 bg-gray-50 p-2 rounded">
                    {notification.package_name}
                  </p>
                </div>
              )}
              {notification.app_instance ? (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Instancia de App
                  </label>
                  <p className="text-base text-gray-900">
                    {notification.app_instance.instance_label || 'Sin nombre'}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Usuario Android: {notification.app_instance.android_user_id}
                  </p>
                </div>
              ) : notification.android_user_id && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Usuario Android
                  </label>
                  <p className="text-base text-gray-900">{notification.android_user_id}</p>
                </div>
              )}
              {notification.android_uid && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Android UID
                  </label>
                  <p className="text-sm font-mono text-gray-700">{notification.android_uid}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Columna Derecha */}
        <div className="space-y-6">
          {/* Dispositivo */}
          <div className="card">
            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Smartphone className="h-5 w-5 text-primary-600" />
              Dispositivo
            </h2>
            {notification.device ? (
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Nombre
                  </label>
                  <p className="text-base text-gray-900">{notification.device.name}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    UUID
                  </label>
                  <p className="text-sm font-mono text-gray-700 bg-gray-50 p-2 rounded">
                    {notification.device.uuid}
                  </p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Plataforma
                  </label>
                  <p className="text-base text-gray-900">{notification.device.platform}</p>
                </div>
                <Link
                  to={`/devices`}
                  className="text-sm text-primary-600 hover:text-primary-700"
                >
                  Ver dispositivo →
                </Link>
              </div>
            ) : (
              <p className="text-gray-500">Dispositivo no disponible</p>
            )}
          </div>

          {/* Fechas */}
          <div className="card">
            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Calendar className="h-5 w-5 text-primary-600" />
              Fechas
            </h2>
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Recibida
                </label>
                <p className="text-base text-gray-900">
                  {format(new Date(notification.received_at), 'dd/MM/yyyy HH:mm:ss')}
                </p>
              </div>
              {notification.posted_at && (
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Publicada
                  </label>
                  <p className="text-base text-gray-900">
                    {format(new Date(notification.posted_at), 'dd/MM/yyyy HH:mm:ss')}
                  </p>
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Creada
                </label>
                <p className="text-sm text-gray-500">
                  {format(new Date(notification.created_at), 'dd/MM/yyyy HH:mm:ss')}
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Actualizada
                </label>
                <p className="text-sm text-gray-500">
                  {format(new Date(notification.updated_at), 'dd/MM/yyyy HH:mm:ss')}
                </p>
              </div>
            </div>
          </div>

          {/* Cambiar Estado */}
          <div className="card">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Cambiar Estado</h2>
            <div className="space-y-2">
              <select
                value={notification.status}
                onChange={(e) =>
                  handleStatusChange(e.target.value as 'pending' | 'validated' | 'inconsistent')
                }
                disabled={statusLoading}
                className="input w-full"
              >
                <option value="pending">Pendiente</option>
                <option value="validated">Validado</option>
                <option value="inconsistent">Inconsistente</option>
              </select>
              {statusLoading && (
                <p className="text-sm text-gray-500">Actualizando...</p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Raw JSON */}
      {notification.raw_json && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
              <Code className="h-5 w-5 text-primary-600" />
              Datos JSON (Raw)
            </h2>
            <button
              onClick={handleCopyJson}
              className="btn btn-secondary btn-sm flex items-center gap-2"
            >
              {copied ? (
                <>
                  <Check className="h-4 w-4" />
                  Copiado
                </>
              ) : (
                <>
                  <Copy className="h-4 w-4" />
                  Copiar JSON
                </>
              )}
            </button>
          </div>
          <div className="bg-gray-900 rounded-lg p-4 overflow-x-auto">
            <pre className="text-sm text-gray-100 font-mono">
              {JSON.stringify(notification.raw_json, null, 2)}
            </pre>
          </div>
        </div>
      )}

      {/* Información Adicional */}
      <div className="card">
        <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <FileText className="h-5 w-5 text-primary-600" />
          Información Adicional
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              ID de Notificación
            </label>
            <p className="text-sm font-mono text-gray-700">{notification.id}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              ID de Usuario
            </label>
            <p className="text-sm text-gray-700">{notification.user_id}</p>
          </div>
          {notification.commerce_id && (
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                ID de Comercio
              </label>
              <p className="text-sm text-gray-700">{notification.commerce_id}</p>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              ID de Dispositivo
            </label>
            <p className="text-sm text-gray-700">{notification.device_id}</p>
          </div>
          {notification.app_instance_id && (
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                ID de Instancia de App
              </label>
              <p className="text-sm text-gray-700">{notification.app_instance_id}</p>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Es Duplicado
            </label>
            <p className="text-sm text-gray-700">
              {notification.is_duplicate ? 'Sí' : 'No'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

