import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import QRCodeDisplay from '@/components/QRCodeDisplay';
import { Loader2, Copy, Check, AlertCircle, Smartphone, Clock } from 'lucide-react';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';

type LinkStatus = 'generating' | 'waiting' | 'linked' | 'error' | 'expired' | 'used';

export default function AddDevicePage() {
  const navigate = useNavigate();
  const [linkCode, setLinkCode] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<Date | null>(null);
  const [status, setStatus] = useState<LinkStatus>('generating');
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const pollingIntervalRef = useRef<number | null>(null);
  const timeUpdateIntervalRef = useRef<number | null>(null);

  // Actualizar tiempo restante cada segundo
  useEffect(() => {
    if (expiresAt && status === 'waiting') {
      const updateTime = () => {
        const now = new Date();
        const diff = expiresAt.getTime() - now.getTime();
        
        if (diff <= 0) {
          setStatus('expired');
          if (timeUpdateIntervalRef.current) {
            clearInterval(timeUpdateIntervalRef.current);
          }
          return;
        }
        
        const minutes = Math.floor(diff / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        
        if (minutes > 0) {
          setTimeRemaining(`${minutes}m ${seconds}s`);
        } else {
          setTimeRemaining(`${seconds}s`);
        }
      };
      
      updateTime();
      timeUpdateIntervalRef.current = window.setInterval(updateTime, 1000);
      
      return () => {
        if (timeUpdateIntervalRef.current) {
          clearInterval(timeUpdateIntervalRef.current);
        }
      };
    }
  }, [expiresAt, status]);

  const startPolling = useCallback((code: string) => {
    // Limpiar intervalo anterior si existe
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
    }

    // Polling cada 2 segundos
    pollingIntervalRef.current = window.setInterval(async () => {
      try {
        const result = await apiService.checkLinkCode(code);

        // Verificar si el código fue usado (dispositivo vinculado)
        if (!result.valid) {
          const message = result.message.toLowerCase();
          
          if (message.includes('expirado') || message.includes('expired')) {
            setStatus('expired');
            if (pollingIntervalRef.current) {
              clearInterval(pollingIntervalRef.current);
            }
            if (timeUpdateIntervalRef.current) {
              clearInterval(timeUpdateIntervalRef.current);
            }
            return;
          }
          
          // Detectar si el código fue usado (dispositivo vinculado)
          if (
            message.includes('utilizado') || 
            message.includes('used') || 
            message.includes('ya usado') ||
            message.includes('usado')
          ) {
            // Código fue usado, verificar que el dispositivo esté vinculado
            setStatus('used');
            if (pollingIntervalRef.current) {
              clearInterval(pollingIntervalRef.current);
            }
            
            // Esperar un momento para que el backend procese la vinculación
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Verificar que el dispositivo esté realmente vinculado
            try {
              const devices = await apiService.getDevices(false);
              // Buscar el dispositivo más reciente que tenga commerce_id
              const linkedDevice = devices
                .filter((d) => d.commerce_id !== null)
                .sort((a, b) => {
                  const aTime = a.updated_at ? new Date(a.updated_at).getTime() : 0;
                  const bTime = b.updated_at ? new Date(b.updated_at).getTime() : 0;
                  return bTime - aTime;
                })[0];
              
              if (linkedDevice) {
                setStatus('linked');
                // Redirigir después de 5 segundos para que el usuario vea el mensaje
                setTimeout(() => {
                  navigate('/devices', { replace: true });
                }, 5000);
              } else {
                // Dispositivo no encontrado, puede ser un error
                setError('El código fue usado pero no se encontró el dispositivo vinculado. Por favor, verifica en la lista de dispositivos.');
                setStatus('error');
              }
            } catch (err) {
              console.error('Error verificando dispositivo vinculado:', err);
              // Aún así, asumir que fue exitoso y redirigir
              setStatus('linked');
              setTimeout(() => {
                navigate('/devices', { replace: true });
              }, 5000);
            }
            return;
          }
        }

        // Si el código es válido y tiene commerce, puede estar esperando vinculación
        if (result.valid && result.commerce) {
          // El código es válido pero aún no ha sido usado
          // Continuar esperando
        }
      } catch (err) {
        // Error en polling, continuar intentando
        console.error('Error checking link code:', err);
      }
    }, 2000);
  }, [navigate]);

  const generateLinkCode = useCallback(async () => {
    try {
      setStatus('generating');
      setError(null);
      const result = await apiService.generateLinkCode();
      setLinkCode(result.code);
      setExpiresAt(new Date(result.expires_at));
      setStatus('waiting');
      startPolling(result.code);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : 'Error al generar código de vinculación';
      setError(errorMessage);
      setStatus('error');
    }
  }, [startPolling]);

  useEffect(() => {
    generateLinkCode();

    // Cleanup: detener polling y actualización de tiempo al desmontar
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
      if (timeUpdateIntervalRef.current) {
        clearInterval(timeUpdateIntervalRef.current);
      }
    };
  }, [generateLinkCode]);

  const handleCopyCode = async () => {
    if (!linkCode) return;

    try {
      await navigator.clipboard.writeText(linkCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Error copying to clipboard:', err);
    }
  };

  const handleRegenerate = () => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
    }
    generateLinkCode();
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-gray-900">Agregar Dispositivo</h1>
        <p className="mt-2 text-sm text-gray-600">
          Escanea el código QR con la aplicación Android para vincular tu dispositivo
        </p>
      </div>

      {status === 'generating' && (
        <div className="card text-center py-12">
          <Loader2 className="h-12 w-12 text-primary-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Generando código de vinculación...</p>
        </div>
      )}

      {status === 'error' && (
        <div className="card">
          <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-red-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Error</h3>
                <p className="mt-1 text-sm text-red-700">{error || 'Error desconocido'}</p>
                <button
                  onClick={handleRegenerate}
                  className="mt-3 btn btn-primary"
                >
                  Intentar de nuevo
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {status === 'expired' && (
        <div className="card">
          <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-yellow-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">Código Expirado</h3>
                <p className="mt-1 text-sm text-yellow-700">
                  El código de vinculación ha expirado. Genera uno nuevo.
                </p>
                <button
                  onClick={handleRegenerate}
                  className="mt-3 btn btn-primary"
                >
                  Generar nuevo código
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {status === 'waiting' && linkCode && (
        <div className="space-y-6">
          {/* QR Code */}
          <div className="card">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">
                Escanea este código QR
              </h2>
              <QRCodeDisplay value={linkCode} size={300} />
            </div>
          </div>

          {/* Código numérico alternativo */}
          <div className="card">
            <div className="text-center">
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                O ingresa este código manualmente
              </h3>
              <div className="flex items-center justify-center gap-3">
                <div className="bg-gray-50 border-2 border-gray-300 rounded-lg px-6 py-4">
                  <span className="text-3xl font-mono font-bold text-gray-900 tracking-wider">
                    {linkCode}
                  </span>
                </div>
                <button
                  onClick={handleCopyCode}
                  className="btn btn-secondary flex items-center gap-2"
                  title="Copiar código"
                >
                  {copied ? (
                    <>
                      <Check className="h-4 w-4" />
                      Copiado
                    </>
                  ) : (
                    <>
                      <Copy className="h-4 w-4" />
                      Copiar
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Estado de espera */}
          <div className="card">
            <div className="flex items-center justify-center gap-3 py-4">
              <Loader2 className="h-5 w-5 text-primary-600 animate-spin" />
              <div className="text-center">
                <p className="text-sm font-medium text-gray-900">
                  Esperando vinculación...
                </p>
                <div className="flex items-center justify-center gap-2 mt-2">
                  <Clock className="h-4 w-4 text-gray-400" />
                  {timeRemaining && (
                    <p className="text-xs font-mono text-gray-600">
                      Tiempo restante: <span className="font-bold text-primary-600">{timeRemaining}</span>
                    </p>
                  )}
                </div>
                <p className="text-xs text-gray-500 mt-1">
                  {expiresAt && (
                    <>Expira el {format(expiresAt, 'dd/MM/yyyy HH:mm', { locale: es })}</>
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* Instrucciones */}
          <div className="card bg-blue-50 border border-blue-200">
            <div className="flex items-start gap-3">
              <Smartphone className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1">Instrucciones:</p>
                <ol className="list-decimal list-inside space-y-1 text-blue-700">
                  <li>Abre la aplicación Android en tu dispositivo</li>
                  <li>Ve a la sección de vincular dispositivo</li>
                  <li>Escanea el código QR o ingresa el código manualmente</li>
                  <li>Espera a que se complete la vinculación</li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      )}

      {status === 'linked' && (
        <div className="card">
          <div className="bg-green-50 border-l-4 border-green-400 p-4 rounded text-center">
            <div className="flex items-center justify-center gap-3">
              <Check className="h-6 w-6 text-green-600" />
              <div>
                <h3 className="text-lg font-medium text-green-800">
                  ¡Dispositivo vinculado exitosamente!
                </h3>
                <p className="text-sm text-green-700 mt-1">
                  El dispositivo ha sido vinculado correctamente a tu negocio.
                </p>
                <p className="text-xs text-green-600 mt-2">
                  Redirigiendo a la lista de dispositivos en unos segundos...
                </p>
                <button
                  onClick={() => navigate('/devices', { replace: true })}
                  className="mt-4 btn btn-primary"
                >
                  Ir a dispositivos ahora
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {status === 'used' && (
        <div className="card">
          <div className="bg-blue-50 border-l-4 border-blue-400 p-4 rounded text-center">
            <div className="flex items-center justify-center gap-3">
              <Loader2 className="h-5 w-5 text-blue-600 animate-spin" />
              <div>
                <h3 className="text-lg font-medium text-blue-800">
                  Verificando vinculación...
                </h3>
                <p className="text-sm text-blue-700 mt-1">
                  El código fue usado. Verificando que el dispositivo esté correctamente vinculado...
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Botón cancelar */}
      {status !== 'linked' && (
        <div className="text-center">
          <button
            onClick={() => navigate('/devices', { replace: true })}
            className="btn btn-secondary"
          >
            Cancelar
          </button>
        </div>
      )}
    </div>
  );
}

