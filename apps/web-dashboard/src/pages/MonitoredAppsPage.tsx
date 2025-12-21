import { useEffect, useState, useCallback } from 'react';
import { apiService } from '@/services/api';
import { useAuth } from '@/contexts/AuthContext';
import type { MonitorPackage } from '@/types';
import {
  Package,
  Plus,
  Edit,
  Trash2,
  Power,
  PowerOff,
  Search,
  Settings,
  AlertCircle,
  CheckCircle,
  Upload,
  FileText,
} from 'lucide-react';

export default function MonitoredAppsPage() {
  const { user } = useAuth();
  const [packages, setPackages] = useState<MonitorPackage[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showActiveOnly, setShowActiveOnly] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingPackage, setEditingPackage] = useState<MonitorPackage | null>(null);
  const [formData, setFormData] = useState({
    package_name: '',
    app_name: '',
    description: '',
    priority: 0,
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [formLoading, setFormLoading] = useState(false);
  const [showBulkModal, setShowBulkModal] = useState(false);
  const [bulkPackages, setBulkPackages] = useState('');
  const [bulkLoading, setBulkLoading] = useState(false);
  const [bulkError, setBulkError] = useState<string | null>(null);

  const isAdmin = user?.role === 'admin';

  const loadPackages = useCallback(async () => {
    setLoading(true);
    try {
      const packagesData = await apiService.getMonitorPackages(showActiveOnly);
      setPackages(packagesData);
    } catch (error) {
      console.error('Error loading monitor packages:', error);
    } finally {
      setLoading(false);
    }
  }, [showActiveOnly]);

  useEffect(() => {
    loadPackages();
  }, [loadPackages]);

  const handleCreate = () => {
    setEditingPackage(null);
    setFormData({
      package_name: '',
      app_name: '',
      description: '',
      priority: 0,
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleEdit = (pkg: MonitorPackage) => {
    setEditingPackage(pkg);
    setFormData({
      package_name: pkg.package_name,
      app_name: pkg.app_name || '',
      description: pkg.description || '',
      priority: pkg.priority,
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError(null);

    try {
      if (editingPackage) {
        await apiService.updateMonitorPackage(editingPackage.id, formData);
      } else {
        await apiService.createMonitorPackage(formData);
      }
      setShowModal(false);
      loadPackages();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      const errorMessage =
        error.response?.data?.message ||
        Object.values(error.response?.data?.errors || {}).flat().join(', ') ||
        'Error al guardar paquete';
      setFormError(errorMessage);
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('¿Estás seguro de eliminar este paquete monitoreado?')) {
      return;
    }
    try {
      await apiService.deleteMonitorPackage(id);
      loadPackages();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al eliminar paquete');
    }
  };

  const handleToggleStatus = async (pkg: MonitorPackage) => {
    try {
      await apiService.toggleMonitorPackageStatus(pkg.id, !pkg.is_active);
      loadPackages();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al cambiar estado');
    }
  };

  const handleBulkCreate = async () => {
    setBulkLoading(true);
    setBulkError(null);

    try {
      // Parsear packages (uno por línea)
      const packageNames = bulkPackages
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0 && line.match(/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/));

      if (packageNames.length === 0) {
        setBulkError('No se encontraron packages válidos. Formato: com.example.app (uno por línea)');
        setBulkLoading(false);
        return;
      }

      const result = await apiService.bulkCreateMonitorPackages(packageNames);
      setShowBulkModal(false);
      setBulkPackages('');
      alert(`Se crearon ${result.created_count} packages exitosamente`);
      loadPackages();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      const errorMessage =
        error.response?.data?.message ||
        Object.values(error.response?.data?.errors || {}).flat().join(', ') ||
        'Error al crear packages en lote';
      setBulkError(errorMessage);
    } finally {
      setBulkLoading(false);
    }
  };

  // Filtrar paquetes por búsqueda
  const filteredPackages = packages.filter((pkg) => {
    const matchesSearch =
      !searchTerm ||
      pkg.package_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (pkg.app_name && pkg.app_name.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (pkg.description && pkg.description.toLowerCase().includes(searchTerm.toLowerCase()));
    return matchesSearch;
  });

  // Separar activos e inactivos
  const activePackages = filteredPackages.filter((pkg) => pkg.is_active);
  const inactivePackages = filteredPackages.filter((pkg) => !pkg.is_active);

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
          <h1 className="text-3xl font-bold text-gray-900">Apps Monitoreadas</h1>
          <p className="mt-1 text-sm text-gray-600">
            Configura qué aplicaciones Android deben ser monitoreadas para recibir notificaciones
          </p>
        </div>
        {isAdmin && (
          <div className="flex gap-2">
            <button
              onClick={() => setShowBulkModal(true)}
              className="btn btn-secondary flex items-center gap-2"
            >
              <Upload className="h-4 w-4" />
              Crear en Lote
            </button>
            <button onClick={handleCreate} className="btn btn-primary flex items-center gap-2">
              <Plus className="h-4 w-4" />
              Agregar App
            </button>
          </div>
        )}
      </div>

      {/* Filtros */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Búsqueda */}
          <div>
            <label htmlFor="search-packages" className="block text-sm font-medium text-gray-700 mb-2">
              <Search className="h-4 w-4 inline mr-1" />
              Buscar
            </label>
            <input
              id="search-packages"
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Buscar por package, nombre o descripción..."
              className="input w-full"
            />
          </div>

          {/* Filtro activos */}
          <div>
            <span className="block text-sm font-medium text-gray-700 mb-2">
              <Settings className="h-4 w-4 inline mr-1" />
              Filtro
            </span>
            <label htmlFor="filter-active-only" className="flex items-center gap-2 cursor-pointer">
              <input
                id="filter-active-only"
                type="checkbox"
                checked={showActiveOnly}
                onChange={(e) => setShowActiveOnly(e.target.checked)}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <span className="text-sm text-gray-700">Mostrar solo activos</span>
            </label>
          </div>
        </div>
      </div>

      {/* Estadísticas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card bg-blue-50 border-blue-200">
          <div className="flex items-center gap-3">
            <Package className="h-8 w-8 text-blue-600" />
            <div>
              <p className="text-sm text-blue-600 font-medium">Total</p>
              <p className="text-2xl font-bold text-blue-900">{filteredPackages.length}</p>
            </div>
          </div>
        </div>
        <div className="card bg-green-50 border-green-200">
          <div className="flex items-center gap-3">
            <CheckCircle className="h-8 w-8 text-green-600" />
            <div>
              <p className="text-sm text-green-600 font-medium">Activas</p>
              <p className="text-2xl font-bold text-green-900">{activePackages.length}</p>
            </div>
          </div>
        </div>
        <div className="card bg-gray-50 border-gray-200">
          <div className="flex items-center gap-3">
            <PowerOff className="h-8 w-8 text-gray-600" />
            <div>
              <p className="text-sm text-gray-600 font-medium">Inactivas</p>
              <p className="text-2xl font-bold text-gray-900">{inactivePackages.length}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Lista de paquetes activos */}
      {activePackages.length > 0 && (
        <div>
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Apps Activas ({activePackages.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {activePackages.map((pkg) => (
              <PackageCard
                key={pkg.id}
                pkg={pkg}
                onEdit={isAdmin ? () => handleEdit(pkg) : undefined}
                onDelete={isAdmin ? () => handleDelete(pkg.id) : undefined}
                onToggleStatus={() => handleToggleStatus(pkg)}
              />
            ))}
          </div>
        </div>
      )}

      {/* Lista de paquetes inactivos */}
      {!showActiveOnly && inactivePackages.length > 0 && (
        <div>
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Apps Inactivas ({inactivePackages.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {inactivePackages.map((pkg) => (
              <PackageCard
                key={pkg.id}
                pkg={pkg}
                onEdit={isAdmin ? () => handleEdit(pkg) : undefined}
                onDelete={isAdmin ? () => handleDelete(pkg.id) : undefined}
                onToggleStatus={() => handleToggleStatus(pkg)}
              />
            ))}
          </div>
        </div>
      )}

      {/* Estado vacío */}
      {filteredPackages.length === 0 && (
        <div className="card text-center py-12">
          <Package className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500 mb-4">
            {searchTerm || showActiveOnly
              ? 'No se encontraron apps con los filtros aplicados'
              : 'No hay apps monitoreadas configuradas'}
          </p>
          {isAdmin && !searchTerm && !showActiveOnly && (
            <button onClick={handleCreate} className="btn btn-primary">
              Agregar primera app
            </button>
          )}
        </div>
      )}

      {/* Modal para crear/editar */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">
              {editingPackage ? 'Editar App Monitoreada' : 'Nueva App Monitoreada'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              {formError && (
                <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
                  <div className="flex">
                    <AlertCircle className="h-5 w-5 text-red-400" />
                    <p className="ml-3 text-sm text-red-700">{formError}</p>
                  </div>
                </div>
              )}

              <div>
                <label htmlFor="package-name" className="block text-sm font-medium text-gray-700 mb-2">
                  Package Name <span className="text-red-500">*</span>
                </label>
                <input
                  id="package-name"
                  type="text"
                  required
                  disabled={!!editingPackage}
                  value={formData.package_name}
                  onChange={(e) => setFormData({ ...formData, package_name: e.target.value })}
                  className="input"
                  placeholder="com.example.app"
                  pattern="[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+"
                  title="Formato: com.example.app"
                />
                <p className="mt-1 text-xs text-gray-500">
                  Formato: com.example.app (no se puede cambiar después de crear)
                </p>
              </div>

              <div>
                <label htmlFor="app-name" className="block text-sm font-medium text-gray-700 mb-2">
                  Nombre de la App
                </label>
                <input
                  id="app-name"
                  type="text"
                  value={formData.app_name}
                  onChange={(e) => setFormData({ ...formData, app_name: e.target.value })}
                  className="input"
                  placeholder="Ej: Yape, Plin, BCP"
                />
              </div>

              <div>
                <label htmlFor="app-description" className="block text-sm font-medium text-gray-700 mb-2">
                  Descripción
                </label>
                <textarea
                  id="app-description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="input"
                  rows={3}
                  placeholder="Descripción opcional de la aplicación..."
                />
              </div>

              <div>
                <label htmlFor="app-priority" className="block text-sm font-medium text-gray-700 mb-2">
                  Prioridad
                </label>
                <input
                  id="app-priority"
                  type="number"
                  min="0"
                  max="100"
                  value={formData.priority}
                  onChange={(e) =>
                    setFormData({ ...formData, priority: parseInt(e.target.value) || 0 })
                  }
                  className="input"
                />
                <p className="mt-1 text-xs text-gray-500">
                  Prioridad para ordenamiento (0-100, mayor = más prioridad)
                </p>
              </div>

              <div className="flex gap-2 justify-end pt-4">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn btn-secondary"
                  disabled={formLoading}
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={formLoading}>
                  {formLoading ? 'Guardando...' : editingPackage ? 'Actualizar' : 'Crear'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal para bulk create */}
      {showBulkModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Crear Packages en Lote</h2>
            <div className="space-y-4">
              {bulkError && (
                <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
                  <div className="flex">
                    <AlertCircle className="h-5 w-5 text-red-400" />
                    <p className="ml-3 text-sm text-red-700">{bulkError}</p>
                  </div>
                </div>
              )}

              <div>
                <label htmlFor="bulk-packages" className="block text-sm font-medium text-gray-700 mb-2">
                  <FileText className="h-4 w-4 inline mr-1" />
                  Packages (uno por línea)
                </label>
                <textarea
                  id="bulk-packages"
                  value={bulkPackages}
                  onChange={(e) => setBulkPackages(e.target.value)}
                  className="input font-mono text-sm"
                  rows={10}
                  placeholder="com.yape.android&#10;com.plin.android&#10;com.bcp.bancadigital&#10;pe.com.interbank.mobilebanking"
                />
                <p className="mt-2 text-xs text-gray-500">
                  Ingresa los package names, uno por línea. Formato: com.example.app
                </p>
                <p className="mt-1 text-xs text-gray-500">
                  Los packages que ya existen se omitirán automáticamente.
                </p>
              </div>

              <div className="flex gap-2 justify-end pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowBulkModal(false);
                    setBulkPackages('');
                    setBulkError(null);
                  }}
                  className="btn btn-secondary"
                  disabled={bulkLoading}
                >
                  Cancelar
                </button>
                <button
                  onClick={handleBulkCreate}
                  className="btn btn-primary flex items-center gap-2"
                  disabled={bulkLoading}
                >
                  {bulkLoading ? 'Creando...' : (
                    <>
                      <Upload className="h-4 w-4" />
                      Crear Packages
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface PackageCardProps {
  pkg: MonitorPackage;
  onEdit?: () => void;
  onDelete?: () => void;
  onToggleStatus: () => void;
}

function PackageCard({ pkg, onEdit, onDelete, onToggleStatus }: PackageCardProps) {
  return (
    <div className="card hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <Package className="h-5 w-5 text-primary-600" />
            <h3 className="text-lg font-semibold text-gray-900">
              {pkg.app_name || pkg.package_name}
            </h3>
            <button
              onClick={onToggleStatus}
              className={`p-1 rounded ${
                pkg.is_active
                  ? 'bg-green-100 text-green-600 hover:bg-green-200'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
              title={pkg.is_active ? 'Desactivar' : 'Activar'}
            >
              {pkg.is_active ? (
                <Power className="h-4 w-4" />
              ) : (
                <PowerOff className="h-4 w-4" />
              )}
            </button>
          </div>

          <div className="space-y-2">
            <div>
              <p className="text-xs text-gray-500">Package:</p>
              <p className="text-sm font-mono text-gray-700">{pkg.package_name}</p>
            </div>

            {pkg.description && (
              <div>
                <p className="text-xs text-gray-500">Descripción:</p>
                <p className="text-sm text-gray-700">{pkg.description}</p>
              </div>
            )}

            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Prioridad:</span>
              <span className="font-medium text-gray-700">{pkg.priority}</span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Estado:</span>
              <span
                className={`badge ${pkg.is_active ? 'badge-success' : 'badge-warning'}`}
              >
                {pkg.is_active ? 'Activo' : 'Inactivo'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {(onEdit || onDelete) && (
        <div className="mt-4 flex gap-2 pt-4 border-t border-gray-200">
          {onEdit && (
            <button
              onClick={onEdit}
              className="btn btn-secondary flex-1 flex items-center justify-center gap-2"
            >
              <Edit className="h-4 w-4" />
              Editar
            </button>
          )}
          {onDelete && (
            <button
              onClick={onDelete}
              className="btn btn-danger flex items-center justify-center gap-2"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      )}
    </div>
  );
}

