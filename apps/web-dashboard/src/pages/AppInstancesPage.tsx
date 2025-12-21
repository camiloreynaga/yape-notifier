import { useEffect, useState, useCallback } from 'react';
import { apiService } from '@/services/api';
import type { AppInstance, Device } from '@/types';
import AppInstanceCard from '@/components/AppInstanceCard';
import { Smartphone, Filter, Search, Package } from 'lucide-react';

export default function AppInstancesPage() {
  const [instances, setInstances] = useState<AppInstance[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const loadInstances = useCallback(async () => {
    try {
      const instancesData = await apiService.getAppInstances(
        selectedDeviceId || undefined
      );
      setInstances(instancesData);
    } catch (error) {
      console.error('Error loading instances:', error);
    }
  }, [selectedDeviceId]);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    loadInstances();
  }, [loadInstances]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [instancesData, devicesData] = await Promise.all([
        apiService.getAppInstances(),
        apiService.getDevices(),
      ]);
      setInstances(instancesData);
      setDevices(devicesData);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = () => {
    loadInstances();
  };

  // Filtrar instancias
  const filteredInstances = instances.filter((instance) => {
    const matchesDevice = !selectedDeviceId || instance.device_id === selectedDeviceId;
    const matchesSearch =
      !searchTerm ||
      instance.package_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (instance.instance_label &&
        instance.instance_label.toLowerCase().includes(searchTerm.toLowerCase())) ||
      instance.android_user_id.toString().includes(searchTerm);
    return matchesDevice && matchesSearch;
  });

  // Separar instancias asignadas y sin asignar
  const assignedInstances = filteredInstances.filter((i) => i.instance_label);
  const unassignedInstances = filteredInstances.filter((i) => !i.instance_label);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Instancias de Apps</h1>
          <p className="mt-1 text-sm text-gray-600">
            Gestiona las instancias de aplicaciones en tus dispositivos
          </p>
        </div>
      </div>

      {/* Filtros */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Filtro por dispositivo */}
          <div>
            <label htmlFor="filter-device" className="block text-sm font-medium text-gray-700 mb-2">
              <Filter className="h-4 w-4 inline mr-1" />
              Filtrar por dispositivo
            </label>
            <select
              id="filter-device"
              value={selectedDeviceId || ''}
              onChange={(e) =>
                setSelectedDeviceId(e.target.value ? Number(e.target.value) : null)
              }
              className="input w-full"
            >
              <option value="">Todos los dispositivos</option>
              {devices.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.name}
                </option>
              ))}
            </select>
          </div>

          {/* Búsqueda */}
          <div>
            <label htmlFor="search-instance" className="block text-sm font-medium text-gray-700 mb-2">
              <Search className="h-4 w-4 inline mr-1" />
              Buscar
            </label>
            <input
              id="search-instance"
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Buscar por package, nombre o usuario Android..."
              className="input w-full"
            />
          </div>
        </div>
      </div>

      {/* Estadísticas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card bg-blue-50 border-blue-200">
          <div className="flex items-center gap-3">
            <Package className="h-8 w-8 text-blue-600" />
            <div>
              <p className="text-sm text-blue-600 font-medium">Total de instancias</p>
              <p className="text-2xl font-bold text-blue-900">{filteredInstances.length}</p>
            </div>
          </div>
        </div>
        <div className="card bg-green-50 border-green-200">
          <div className="flex items-center gap-3">
            <Smartphone className="h-8 w-8 text-green-600" />
            <div>
              <p className="text-sm text-green-600 font-medium">Asignadas</p>
              <p className="text-2xl font-bold text-green-900">
                {assignedInstances.length}
              </p>
            </div>
          </div>
        </div>
        <div className="card bg-yellow-50 border-yellow-200">
          <div className="flex items-center gap-3">
            <Package className="h-8 w-8 text-yellow-600" />
            <div>
              <p className="text-sm text-yellow-600 font-medium">Sin asignar</p>
              <p className="text-2xl font-bold text-yellow-900">
                {unassignedInstances.length}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Instancias asignadas */}
      {assignedInstances.length > 0 && (
        <div>
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Instancias Asignadas ({assignedInstances.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {assignedInstances.map((instance) => (
              <AppInstanceCard
                key={instance.id}
                instance={instance}
                onUpdate={handleUpdate}
              />
            ))}
          </div>
        </div>
      )}

      {/* Instancias sin asignar */}
      {unassignedInstances.length > 0 && (
        <div>
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Instancias Sin Asignar ({unassignedInstances.length})
          </h2>
          <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-4 rounded">
            <p className="text-sm text-yellow-700">
              Estas instancias no tienen un nombre asignado. Asigna un nombre para
              identificarlas fácilmente (ej: &quot;Yape Principal&quot;, &quot;Yape Secundario&quot;).
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {unassignedInstances.map((instance) => (
              <AppInstanceCard
                key={instance.id}
                instance={instance}
                onUpdate={handleUpdate}
              />
            ))}
          </div>
        </div>
      )}

      {/* Estado vacío */}
      {filteredInstances.length === 0 && (
        <div className="card text-center py-12">
          <Package className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500 mb-2">
            {selectedDeviceId || searchTerm
              ? 'No se encontraron instancias con los filtros aplicados'
              : 'No hay instancias de apps registradas'}
          </p>
          {selectedDeviceId || searchTerm ? (
            <button
              onClick={() => {
                setSelectedDeviceId(null);
                setSearchTerm('');
              }}
              className="btn btn-secondary mt-4"
            >
              Limpiar filtros
            </button>
          ) : null}
        </div>
      )}
    </div>
  );
}

