import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import type { Device, AppInstance } from '@/types';
import { format } from 'date-fns';
import { Plus, Edit, Trash2, Power, PowerOff, Smartphone, QrCode, Battery, Wifi, WifiOff, CheckCircle, XCircle, AlertCircle, Package, ChevronDown, ChevronUp } from 'lucide-react';
import AppInstanceCard from '@/components/AppInstanceCard';

export default function DevicesPage() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingDevice, setEditingDevice] = useState<Device | null>(null);
  const [formData, setFormData] = useState({
    name: '',
  });
  const [expandedDevices, setExpandedDevices] = useState<Set<number>>(new Set());
  const [deviceInstances, setDeviceInstances] = useState<Record<number, AppInstance[]>>({});
  const [loadingInstances, setLoadingInstances] = useState<Set<number>>(new Set());

  useEffect(() => {
    loadDevices();
  }, []);

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

  const handleCreate = () => {
    setEditingDevice(null);
    setFormData({ name: '' });
    setShowModal(true);
  };

  const handleEdit = (device: Device) => {
    setEditingDevice(device);
    setFormData({ name: device.name });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // Platform is always 'android' for now
      const deviceData = {
        ...formData,
        platform: 'android',
      };
      
      if (editingDevice) {
        await apiService.updateDevice(editingDevice.id, deviceData);
      } else {
        await apiService.createDevice(deviceData);
      }
      setShowModal(false);
      loadDevices();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      const errorMessage = err.response?.data?.message || 
        Object.values(err.response?.data?.errors || {}).flat().join(', ') ||
        'Error al guardar dispositivo';
      alert(errorMessage);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('¿Estás seguro de eliminar este dispositivo?')) {
      return;
    }
    try {
      await apiService.deleteDevice(id);
      loadDevices();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al eliminar dispositivo');
    }
  };

  const handleToggleStatus = async (device: Device) => {
    try {
      await apiService.toggleDeviceStatus(device.id, !device.is_active);
      loadDevices();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al cambiar estado');
    }
  };

  const isDeviceOnline = (device: Device): boolean => {
    if (!device.last_heartbeat) {
      return false;
    }
    const heartbeatTime = new Date(device.last_heartbeat).getTime();
    const now = Date.now();
    const diffMinutes = (now - heartbeatTime) / (1000 * 60);
    return diffMinutes < 5;
  };

  const toggleDeviceExpanded = async (deviceId: number) => {
    const isExpanded = expandedDevices.has(deviceId);
    const newExpanded = new Set(expandedDevices);
    
    if (isExpanded) {
      newExpanded.delete(deviceId);
    } else {
      newExpanded.add(deviceId);
      // Cargar instancias si no están cargadas
      if (!deviceInstances[deviceId]) {
        await loadDeviceInstances(deviceId);
      }
    }
    
    setExpandedDevices(newExpanded);
  };

  const loadDeviceInstances = async (deviceId: number) => {
    setLoadingInstances((prev) => new Set(prev).add(deviceId));
    try {
      const instances = await apiService.getDeviceAppInstances(deviceId);
      setDeviceInstances((prev) => ({ ...prev, [deviceId]: instances }));
    } catch (error) {
      console.error('Error loading device instances:', error);
      setDeviceInstances((prev) => ({ ...prev, [deviceId]: [] }));
    } finally {
      setLoadingInstances((prev) => {
        const next = new Set(prev);
        next.delete(deviceId);
        return next;
      });
    }
  };

  const handleInstanceUpdate = (deviceId: number) => {
    loadDeviceInstances(deviceId);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Dispositivos</h1>
        <div className="flex gap-2">
          <button
            onClick={() => navigate('/devices/add')}
            className="btn btn-primary flex items-center gap-2"
          >
            <QrCode className="h-4 w-4" />
            Agregar Dispositivo
          </button>
          <button onClick={handleCreate} className="btn btn-secondary flex items-center gap-2">
            <Plus className="h-4 w-4" />
            Nuevo Dispositivo
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      ) : devices.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {devices.map((device) => (
            <div key={device.id} className="card">
              <div className="flex items-start justify-between">
                <div className="flex items-center">
                  <div className="flex-shrink-0 bg-primary-100 rounded-lg p-3">
                    <Smartphone className="h-6 w-6 text-primary-600" />
                  </div>
                  <div className="ml-4">
                    <h3 className="text-lg font-semibold text-gray-900">{device.name}</h3>
                    <p className="text-sm text-gray-500">{device.platform}</p>
                  </div>
                </div>
                <button
                  onClick={() => handleToggleStatus(device)}
                  className={`p-2 rounded-lg ${
                    device.is_active
                      ? 'bg-green-100 text-green-600 hover:bg-green-200'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                  title={device.is_active ? 'Desactivar' : 'Activar'}
                >
                  {device.is_active ? (
                    <Power className="h-5 w-5" />
                  ) : (
                    <PowerOff className="h-5 w-5" />
                  )}
                </button>
              </div>

              <div className="mt-4 space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">UUID:</span>
                  <span className="font-mono text-xs text-gray-700">{device.uuid}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">Estado:</span>
                  <span
                    className={`badge ${device.is_active ? 'badge-success' : 'badge-warning'}`}
                  >
                    {device.is_active ? 'Activo' : 'Inactivo'}
                  </span>
                </div>
                
                {/* Health Status */}
                <div className="pt-2 border-t border-gray-200 space-y-2">
                  {/* Online Status */}
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-500 flex items-center gap-1">
                      <Wifi className="h-3 w-3" />
                      Conexión:
                    </span>
                    {device.last_heartbeat && isDeviceOnline(device) ? (
                      <span className="flex items-center gap-1 text-green-600">
                        <Wifi className="h-3 w-3" />
                        <span>En línea</span>
                      </span>
                    ) : (
                      <span className="flex items-center gap-1 text-gray-400">
                        <WifiOff className="h-3 w-3" />
                        <span>Desconectado</span>
                      </span>
                    )}
                  </div>

                  {/* Battery Level */}
                  {device.battery_level !== null && (
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500 flex items-center gap-1">
                        <Battery className="h-3 w-3" />
                        Batería:
                      </span>
                      <div className="flex items-center gap-2">
                        <div className="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${
                              device.battery_level > 50
                                ? 'bg-green-500'
                                : device.battery_level > 20
                                ? 'bg-yellow-500'
                                : 'bg-red-500'
                            }`}
                            style={{ width: `${device.battery_level}%` }}
                          />
                        </div>
                        <span className="text-gray-700 font-medium">{device.battery_level}%</span>
                      </div>
                    </div>
                  )}

                  {/* Battery Optimization */}
                  {device.battery_optimization_disabled !== null && (
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">Optimización batería:</span>
                      {device.battery_optimization_disabled ? (
                        <span className="flex items-center gap-1 text-green-600">
                          <CheckCircle className="h-3 w-3" />
                          <span>Desactivada</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-orange-600">
                          <AlertCircle className="h-3 w-3" />
                          <span>Activada</span>
                        </span>
                      )}
                    </div>
                  )}

                  {/* Notification Permission */}
                  {device.notification_permission_enabled !== null && (
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">Permisos notificaciones:</span>
                      {device.notification_permission_enabled ? (
                        <span className="flex items-center gap-1 text-green-600">
                          <CheckCircle className="h-3 w-3" />
                          <span>Habilitado</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-red-600">
                          <XCircle className="h-3 w-3" />
                          <span>Deshabilitado</span>
                        </span>
                      )}
                    </div>
                  )}

                  {/* Last Heartbeat */}
                  {device.last_heartbeat && (
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">Último heartbeat:</span>
                      <span className="text-gray-700">
                        {format(new Date(device.last_heartbeat), 'dd/MM/yyyy HH:mm')}
                      </span>
                    </div>
                  )}

                  {/* Last Seen */}
                  {device.last_seen_at && (
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">Última actividad:</span>
                      <span className="text-gray-700">
                        {format(new Date(device.last_seen_at), 'dd/MM/yyyy HH:mm')}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Instancias de Apps */}
              <div className="mt-4 pt-4 border-t border-gray-200">
                <button
                  onClick={() => toggleDeviceExpanded(device.id)}
                  className="w-full flex items-center justify-between text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <Package className="h-4 w-4" />
                    <span>
                      Instancias de Apps
                      {deviceInstances[device.id] && (
                        <span className="ml-2 text-gray-500">
                          ({deviceInstances[device.id].length})
                        </span>
                      )}
                    </span>
                  </div>
                  {expandedDevices.has(device.id) ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </button>

                {expandedDevices.has(device.id) && (
                  <div className="mt-3 space-y-2">
                    {loadingInstances.has(device.id) ? (
                      <div className="flex items-center justify-center py-4">
                        <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
                      </div>
                    ) : deviceInstances[device.id] && deviceInstances[device.id].length > 0 ? (
                      <div className="space-y-2">
                        {deviceInstances[device.id].map((instance) => (
                          <div key={instance.id} className="bg-gray-50 rounded-lg p-3">
                            <AppInstanceCard
                              instance={instance}
                              onUpdate={() => handleInstanceUpdate(device.id)}
                            />
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-4 text-sm text-gray-500">
                        <Package className="h-6 w-6 mx-auto mb-2 text-gray-400" />
                        <p>No hay instancias de apps en este dispositivo</p>
                        <button
                          onClick={() => navigate('/app-instances')}
                          className="mt-2 text-primary-600 hover:text-primary-700 text-sm font-medium"
                        >
                          Ver todas las instancias
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="mt-4 flex gap-2">
                <button
                  onClick={() => handleEdit(device)}
                  className="btn btn-secondary flex-1 flex items-center justify-center gap-2"
                >
                  <Edit className="h-4 w-4" />
                  Editar
                </button>
                <button
                  onClick={() => handleDelete(device.id)}
                  className="btn btn-danger flex items-center justify-center gap-2"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card text-center py-12">
          <Smartphone className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500 mb-4">No hay dispositivos registrados</p>
          <div className="flex gap-2 justify-center">
            <button
              onClick={() => navigate('/devices/add')}
              className="btn btn-primary flex items-center gap-2"
            >
              <QrCode className="h-4 w-4" />
              Agregar Dispositivo
            </button>
            <button onClick={handleCreate} className="btn btn-secondary">
              Crear Dispositivo
            </button>
          </div>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">
              {editingDevice ? 'Editar Dispositivo' : 'Nuevo Dispositivo'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Nombre
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="input"
                  placeholder="Ej: Caja 1 - Yape"
                />
              </div>
              {editingDevice && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    UUID
                  </label>
                  <input
                    type="text"
                    value={editingDevice.uuid}
                    disabled
                    className="input"
                  />
                </div>
              )}
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingDevice ? 'Actualizar' : 'Crear'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

