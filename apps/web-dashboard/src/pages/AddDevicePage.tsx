import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '@/services/api';
import QRCodeDisplay from '@/components/QRCodeDisplay';
import { Loader2, Copy, Check, AlertCircle, Smartphone } from 'lucide-react';
import { format } from 'date-fns';

type LinkStatus = 'generating' | 'waiting' | 'linked' | 'error' | 'expired';

export default function AddDevicePage() {
  const navigate = useNavigate();
  const [linkCode, setLinkCode] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<Date | null>(null);
  const [status, setStatus] = useState<LinkStatus>('generating');
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const pollingIntervalRef = useRef<number | null>(null);

  useEffect(() => {
    generateLinkCode();

    // Cleanup: detener polling al desmontar
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, []);

  const generateLinkCode = async () => {
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
  };

  const startPolling = (code: string) => {
    // Limpiar intervalo anterior si existe
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
    }

    // Polling cada 2 segundos
    pollingIntervalRef.current = window.setInterval(async () => {
      try {
        const result = await apiService.checkLinkCode(code);

        if (result.valid && result.commerce) {
          // Código vinculado exitosamente
          setStatus('linked');
          if (pollingIntervalRef.current) {
            clearInterval(pollingIntervalRef.current);
          }
          // Redirigir después de 1 segundo
          setTimeout(() => {
            navigate('/devices', { replace: true });
          }, 1000);
        } else if (!result.valid) {
          // Código inválido o expirado
          if (result.message.includes('expirado') || result.message.includes('expired')) {
            setStatus('expired');
            if (pollingIntervalRef.current) {
              clearInterval(pollingIntervalRef.current);
            }
          }
        }
      } catch (err) {
        // Error en polling, continuar intentando
        console.error('Error checking link code:', err);
      }
    }, 2000);
  };

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

  // Construir URL completa para el QR (el dispositivo Android escaneará esto)
  const qrValue = linkCode || '';

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
              <div>
                <p className="text-sm font-medium text-gray-900">
                  Esperando vinculación...
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {expiresAt && (
                    <>Expira el {format(expiresAt, 'dd/MM/yyyy HH:mm')}</>
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
                  Dispositivo vinculado exitosamente
                </h3>
                <p className="text-sm text-green-700 mt-1">
                  Redirigiendo a la lista de dispositivos...
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

