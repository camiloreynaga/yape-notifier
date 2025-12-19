import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import { useAuth } from '@/contexts/AuthContext';
import { Building2, AlertCircle } from 'lucide-react';
import { AxiosError } from 'axios';
import type { ApiError } from '@/types';

export default function CreateCommercePage() {
  const navigate = useNavigate();
  const { checkCommerce, hasCommerce } = useAuth();
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  // Redirigir si ya tiene commerce
  useEffect(() => {
    if (hasCommerce) {
      navigate('/dashboard', { replace: true });
    }
  }, [hasCommerce, navigate]);

  const validateName = (value: string): string | null => {
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      return 'El nombre del comercio es requerido';
    }
    if (trimmed.length < 3) {
      return 'El nombre debe tener al menos 3 caracteres';
    }
    if (trimmed.length > 100) {
      return 'El nombre no puede exceder 100 caracteres';
    }
    return null;
  };

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setName(value);
    setValidationError(null);
    
    // Validación en tiempo real
    if (value.trim().length > 0) {
      const validation = validateName(value);
      setValidationError(validation);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setValidationError(null);

    // Validar antes de enviar
    const validation = validateName(name);
    if (validation) {
      setValidationError(validation);
      return;
    }

    setLoading(true);

    try {
      await apiService.createCommerce(name.trim());
      // Actualizar el estado de commerce en AuthContext
      await checkCommerce();
      // Redirigir al dashboard
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const axiosError = err as AxiosError<ApiError>;
      let errorMessage = 'Error al crear comercio';
      
      if (axiosError.response?.data) {
        const apiError = axiosError.response.data;
        if (apiError.message) {
          errorMessage = apiError.message;
        } else if (apiError.errors) {
          // Si hay errores de validación del backend
          const firstError = Object.values(apiError.errors)[0];
          errorMessage = Array.isArray(firstError) ? firstError[0] : firstError;
        }
      } else if (axiosError.message) {
        errorMessage = axiosError.message;
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <div className="flex justify-center">
            <div className="rounded-full bg-primary-100 p-3">
              <Building2 className="h-8 w-8 text-primary-600" />
            </div>
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Crear Comercio
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Crea un nuevo comercio para organizar tus dispositivos y notificaciones.
            El comercio te permitirá gestionar múltiples dispositivos y recibir notificaciones de pagos.
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertCircle className="h-5 w-5 text-red-400" aria-hidden="true" />
                </div>
                <div className="ml-3">
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              </div>
            </div>
          )}
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Nombre del Comercio
            </label>
            <input
              id="name"
              name="name"
              type="text"
              required
              value={name}
              onChange={handleNameChange}
              disabled={loading}
              className={`
                appearance-none relative block w-full px-3 py-2 border rounded-md
                placeholder-gray-400 text-gray-900 focus:outline-none focus:ring-primary-500 
                focus:border-primary-500 focus:z-10 sm:text-sm
                ${validationError || error
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                  : 'border-gray-300'
                }
                ${loading ? 'bg-gray-100 cursor-not-allowed' : 'bg-white'}
              `}
              placeholder="Ej: Mi Tienda, Restaurante El Buen Sabor"
              aria-invalid={!!validationError}
              aria-describedby={validationError ? 'name-error' : undefined}
            />
            {validationError && (
              <p className="mt-1 text-sm text-red-600" id="name-error" role="alert">
                {validationError}
              </p>
            )}
            <p className="mt-1 text-xs text-gray-500">
              Mínimo 3 caracteres, máximo 100 caracteres
            </p>
          </div>
          <div>
            <button
              type="submit"
              disabled={loading || !!validationError || name.trim().length === 0}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? (
                <span className="flex items-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Creando...
                </span>
              ) : (
                'Crear Comercio'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}



