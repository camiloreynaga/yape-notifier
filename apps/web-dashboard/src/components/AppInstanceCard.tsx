import { useState } from 'react';
import { Edit, Save, X, Smartphone, Package, User } from 'lucide-react';
import type { AppInstance } from '@/types';
import { apiService } from '@/services/api';

interface AppInstanceCardProps {
  instance: AppInstance;
  onUpdate: () => void;
}

/**
 * Componente para mostrar y editar una instancia de app
 */
export default function AppInstanceCard({ instance, onUpdate }: AppInstanceCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [label, setLabel] = useState(instance.instance_label || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleEdit = () => {
    setIsEditing(true);
    setLabel(instance.instance_label || '');
    setError(null);
  };

  const handleCancel = () => {
    setIsEditing(false);
    setLabel(instance.instance_label || '');
    setError(null);
  };

  const handleSave = async () => {
    if (label.trim() === instance.instance_label) {
      setIsEditing(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await apiService.updateAppInstanceLabel(instance.id, label.trim() || '');
      setIsEditing(false);
      onUpdate();
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : 'Error al actualizar el nombre';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSave();
    } else if (e.key === 'Escape') {
      handleCancel();
    }
  };

  const displayLabel = instance.instance_label || 'Sin asignar';
  const isUnassigned = !instance.instance_label;

  return (
    <div className="card hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          {/* Nombre de la instancia */}
          {isEditing ? (
            <div className="space-y-2">
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                onKeyDown={handleKeyPress}
                disabled={loading}
                className="input w-full"
                placeholder="Nombre de la instancia"
                autoFocus
              />
              {error && (
                <p className="text-sm text-red-600">{error}</p>
              )}
              <div className="flex gap-2">
                <button
                  onClick={handleSave}
                  disabled={loading}
                  className="btn btn-primary btn-sm flex items-center gap-1"
                >
                  <Save className="h-3 w-3" />
                  Guardar
                </button>
                <button
                  onClick={handleCancel}
                  disabled={loading}
                  className="btn btn-secondary btn-sm flex items-center gap-1"
                >
                  <X className="h-3 w-3" />
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            <div>
              <h3
                className={`text-lg font-semibold ${
                  isUnassigned ? 'text-gray-400 italic' : 'text-gray-900'
                }`}
              >
                {displayLabel}
              </h3>
              {isUnassigned && (
                <p className="text-xs text-gray-500 mt-1">
                  Asigna un nombre para identificar esta instancia
                </p>
              )}
            </div>
          )}

          {/* Información de la instancia */}
          <div className="mt-3 space-y-2">
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <Package className="h-4 w-4 text-gray-400" />
              <span className="font-mono text-xs">{instance.package_name}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <User className="h-4 w-4 text-gray-400" />
              <span>Usuario Android: {instance.android_user_id}</span>
            </div>
            {instance.device && (
              <div className="flex items-center gap-2 text-sm text-gray-600">
                <Smartphone className="h-4 w-4 text-gray-400" />
                <span>{instance.device.name}</span>
              </div>
            )}
          </div>
        </div>

        {/* Botón de editar */}
        {!isEditing && (
          <button
            onClick={handleEdit}
            className="btn btn-secondary btn-sm flex items-center gap-1"
            title="Editar nombre"
          >
            <Edit className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}

