/**
 * Componente para mostrar resumen de dispositivos
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { Wifi, WifiOff, Battery, CheckCircle, XCircle, AlertCircle, ExternalLink } from 'lucide-react';
import { apiService } from '@/services/api';
import type { Device } from '@/types';

export default function DevicesSummary() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDevices = async () => {
      setLoading(true);
      try {
        const deviceList = await apiService.getDevices();
        setDevices(deviceList);
      } catch (error) {
        console.error('Error loading devices:', error);
      } finally {
        setLoading(false);
      }
    };

    loadDevices();
  }, []);

  const isDeviceOnline = (device: Device): boolean => {
    if (!device.last_heartbeat) return false;
    const heartbeatTime = new Date(device.last_heartbeat).getTime();
    const now = Date.now();
    const diffMinutes = (now - heartbeatTime) / (1000 * 60);
    return diffMinutes < 5;
  };

  if (loading) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Resumen de Dispositivos</h3>
        <div className="animate-pulse space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 bg-gray-200 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (devices.length === 0) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Resumen de Dispositivos</h3>
        <div className="text-center py-8">
          <p className="text-gray-500 mb-4">No hay dispositivos registrados</p>
          <button
            onClick={() => navigate('/devices/add')}
            className="btn btn-primary"
          >
            Agregar Dispositivo
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Resumen de Dispositivos</h3>
        <button
          onClick={() => navigate('/dashboard?tab=devices')}
          className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
        >
          Ver todos
          <ExternalLink className="h-3 w-3" />
        </button>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Dispositivo
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Estado
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Última Actividad
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Salud
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {devices.slice(0, 5).map((device) => {
              const online = isDeviceOnline(device);
              return (
                <tr key={device.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{device.name}</div>
                    <div className="text-xs text-gray-500">{device.platform}</div>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      {online ? (
                        <>
                          <Wifi className="h-4 w-4 text-green-500" />
                          <span className="text-sm text-green-600">En línea</span>
                        </>
                      ) : (
                        <>
                          <WifiOff className="h-4 w-4 text-gray-400" />
                          <span className="text-sm text-gray-500">Desconectado</span>
                        </>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                    {device.last_seen_at
                      ? format(new Date(device.last_seen_at), 'dd/MM/yyyy HH:mm')
                      : 'Nunca'}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-3 text-xs">
                      {device.battery_level !== null && (
                        <div className="flex items-center gap-1">
                          <Battery
                            className={`h-4 w-4 ${
                              device.battery_level > 50
                                ? 'text-green-500'
                                : device.battery_level > 20
                                ? 'text-yellow-500'
                                : 'text-red-500'
                            }`}
                          />
                          <span>{device.battery_level}%</span>
                        </div>
                      )}
                      {device.notification_permission_enabled !== null && (
                        <div className="flex items-center gap-1">
                          {device.notification_permission_enabled ? (
                            <CheckCircle className="h-4 w-4 text-green-500" />
                          ) : (
                            <XCircle className="h-4 w-4 text-red-500" />
                          )}
                          <span>Permisos</span>
                        </div>
                      )}
                      {device.battery_optimization_disabled === false && (
                        <div className="flex items-center gap-1 text-yellow-600">
                          <AlertCircle className="h-4 w-4" />
                          <span>Optimización</span>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {devices.length > 5 && (
        <div className="mt-4 text-center">
          <button
            onClick={() => navigate('/dashboard?tab=devices')}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Ver {devices.length - 5} dispositivos más →
          </button>
        </div>
      )}
    </div>
  );
}

