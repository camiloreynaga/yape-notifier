import { useEffect, useState } from 'react';
import { apiService } from '@/services/api';
import type { Device } from '@/types';
import { format } from 'date-fns';
import { Plus, Edit, Trash2, Power, PowerOff, Smartphone } from 'lucide-react';

export default function DevicesPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingDevice, setEditingDevice] = useState<Device | null>(null);
  const [formData, setFormData] = useState({
    name: '',
  });

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

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Dispositivos</h1>
        <button onClick={handleCreate} className="btn btn-primary flex items-center gap-2">
          <Plus className="h-4 w-4" />
          Nuevo Dispositivo
        </button>
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
                {device.last_seen_at && (
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-500">Última actividad:</span>
                    <span className="text-gray-700">
                      {format(new Date(device.last_seen_at), 'dd/MM/yyyy HH:mm')}
                    </span>
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
          <button onClick={handleCreate} className="btn btn-primary">
            Crear primer dispositivo
          </button>
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

