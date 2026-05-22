import { useState, useMemo } from 'react';
import { useDevices } from '@/hooks/useDevices';
import { useAppInstances } from '@/hooks/useAppInstances';
import { useDebouncedValue } from '@/hooks/useDebounce';
import AppInstanceCard from '@/components/AppInstanceCard';
import { Smartphone, Filter, Search, Package } from 'lucide-react';

export default function AppInstancesPage() {
  const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Debounced search term (espera 300ms después de que el usuario deja de escribir)
  const { debouncedValue: debouncedSearchTerm, isDebouncing } = useDebouncedValue(searchTerm, 300);

  // Usar React Query hooks para devices y app instances
  const {
    data: devices = [],
    isLoading: devicesLoading,
    error: devicesError
  } = useDevices(false);

  const {
    data: instances = [],
    isLoading: instancesLoading,
    error: instancesError,
    refetch: refetchInstances
  } = useAppInstances(selectedDeviceId || undefined);

  // Estado de carga combinado
  const loading = devicesLoading || instancesLoading;

  const handleUpdate = () => {
    refetchInstances();
  };

  // Filtrar instancias usando useMemo para optimización
  // Usa debouncedSearchTerm en lugar de searchTerm para evitar filtrados innecesarios
  const filteredInstances = useMemo(() => {
    return instances.filter((instance: typeof instances[0]) => {
      const matchesDevice = !selectedDeviceId || instance.device_id === selectedDeviceId;
      const matchesSearch =
        !debouncedSearchTerm ||
        instance.package_name.toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
        (instance.instance_label &&
          instance.instance_label.toLowerCase().includes(debouncedSearchTerm.toLowerCase())) ||
        instance.android_user_id.toString().includes(debouncedSearchTerm);
      return matchesDevice && matchesSearch;
    });
  }, [instances, selectedDeviceId, debouncedSearchTerm]);

  // Separar instancias asignadas y sin asignar
  const assignedInstances = useMemo(() =>
    filteredInstances.filter((i: typeof filteredInstances[0]) => i.instance_label),
    [filteredInstances]
  );
  const unassignedInstances = useMemo(() =>
    filteredInstances.filter((i: typeof filteredInstances[0]) => !i.instance_label),
    [filteredInstances]
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  // Mostrar errores si existen
  const hasError = devicesError || instancesError;

  return (
    <div className="space-y-6">
      {/* Error message */}
      {hasError && (
        <div className="card bg-red-50 border-l-4 border-red-400">
          <div className="flex">
            <div className="ml-3">
              <p className="text-sm text-red-700">
                {devicesError && `Error al cargar dispositivos: ${devicesError.message}`}
                {devicesError && instancesError && ' | '}
                {instancesError && `Error al cargar instancias: ${instancesError.message}`}
              </p>
            </div>
          </div>
        </div>
      )}
      {/* Header con navegación */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
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
              {devices.map((device: typeof devices[0]) => (
                <option key={device.id} value={device.id}>
                  {device.name}
                </option>
              ))}
            </select>
          </div>

          {/* Búsqueda con indicador de debouncing */}
          <div>
            <label htmlFor="search-instance" className="block text-sm font-medium text-gray-700 mb-2">
              <Search className="h-4 w-4 inline mr-1" />
              Buscar
              {isDebouncing && (
                <span className="ml-2 text-xs text-primary-600">(buscando...)</span>
              )}
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
            {assignedInstances.map((instance: typeof assignedInstances[0]) => (
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
            {unassignedInstances.map((instance: typeof unassignedInstances[0]) => (
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

