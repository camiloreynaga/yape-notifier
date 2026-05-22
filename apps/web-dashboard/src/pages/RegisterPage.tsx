import { useState, FormEvent, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { UserPlus, Mail, Phone, Lock, Eye, EyeOff, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function RegisterPage() {
  const [searchParams] = useSearchParams();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirmation, setPasswordConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  // Capture referral code from URL parameter
  useEffect(() => {
    const ref = searchParams.get('ref');
    if (ref) {
      sessionStorage.setItem('referral_code', ref);
    }
  }, [searchParams]);

  // Real-time validations
  const passwordsMatch = password.length === 0 || password === passwordConfirmation;
  const passwordLongEnough = password.length === 0 || password.length >= 8;
  const phoneValid = phone.length === 0 || /^\+?\d[\d\s-]{6,18}\d$/.test(phone.trim());

  const canSubmit =
    name.trim().length > 0 &&
    email.trim().length > 0 &&
    password.length >= 8 &&
    password === passwordConfirmation &&
    !loading;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== passwordConfirmation) {
      setError('Las contraseñas no coinciden');
      return;
    }
    if (password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres');
      return;
    }
    if (phone && !phoneValid) {
      setError('El teléfono no tiene un formato válido');
      return;
    }

    setLoading(true);
    try {
      await register(name, email, password, phone || undefined);
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      if (error.response?.data?.errors) {
        const errorMessages = Object.values(error.response.data.errors).flat();
        setError(errorMessages.join('. ') || 'Error de validación');
      } else {
        setError(error.response?.data?.message || 'Error al registrar usuario');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-stretch bg-surface-warm">
      {/* Left side: brand panel (desktop only) */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary-800 via-primary-700 to-primary-900 text-white p-12 flex-col justify-between relative overflow-hidden">
        <div className="relative z-10">
          <h1 className="text-3xl font-bold tracking-tight">Yape Notifier</h1>
          <p className="text-primary-100 mt-2">Centraliza tus pagos digitales</p>
        </div>
        <div className="relative z-10 space-y-5">
          <h2 className="text-4xl font-extrabold leading-tight">
            Captura, valida y consolida<br />tus pagos en un solo lugar.
          </h2>
          <ul className="space-y-3 text-primary-100">
            <li className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 mt-0.5 text-accent-300 shrink-0" />
              <span>Notificaciones en tiempo real desde Yape, Plin, BCP y más.</span>
            </li>
            <li className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 mt-0.5 text-accent-300 shrink-0" />
              <span>Múltiples dispositivos y captadores por comercio.</span>
            </li>
            <li className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 mt-0.5 text-accent-300 shrink-0" />
              <span>Validación rápida y trazabilidad completa.</span>
            </li>
          </ul>
        </div>
        <div className="relative z-10 text-xs text-primary-200">
          © {new Date().getFullYear()} Yape Notifier
        </div>
        {/* Decorative blobs */}
        <div className="absolute -top-20 -right-20 w-72 h-72 rounded-full bg-accent-300/10 blur-3xl" />
        <div className="absolute -bottom-32 -left-10 w-80 h-80 rounded-full bg-accent-300/5 blur-3xl" />
      </div>

      {/* Right side: form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary-800 mb-4">
              <UserPlus className="h-7 w-7 text-accent-300" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Crear cuenta</h2>
            <p className="mt-2 text-sm text-gray-600">
              ¿Ya tienes cuenta?{' '}
              <Link to="/login" className="font-semibold text-primary-700 hover:text-primary-800">
                Inicia sesión
              </Link>
            </p>
          </div>

          <form className="space-y-4" onSubmit={handleSubmit}>
            {error && (
              <div className="flex items-start gap-2 rounded-lg bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1.5">
                Nombre completo
              </label>
              <input
                id="name"
                name="name"
                type="text"
                required
                autoComplete="name"
                className="w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm placeholder-gray-400 focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
                placeholder="Juan Pérez"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1.5">
                Correo electrónico
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  className="w-full rounded-lg border border-gray-300 bg-white pl-10 pr-3.5 py-2.5 text-sm placeholder-gray-400 focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
                  placeholder="tu@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1.5">
                Teléfono (WhatsApp) <span className="text-gray-400 text-xs font-normal">— opcional</span>
              </label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  autoComplete="tel"
                  className={`w-full rounded-lg border bg-white pl-10 pr-3.5 py-2.5 text-sm placeholder-gray-400 focus:ring-2 focus:outline-none ${
                    !phoneValid
                      ? 'border-red-300 focus:border-red-500 focus:ring-red-500/20'
                      : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500/20'
                  }`}
                  placeholder="+51 999 888 777"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
              </div>
              {!phoneValid && (
                <p className="text-xs text-red-600 mt-1">
                  Formato no válido. Ejemplo: +51 999 888 777
                </p>
              )}
              <p className="text-xs text-gray-500 mt-1">
                Lo usaremos para contactarte sobre tu cuenta y plan.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Contraseña
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    className={`w-full rounded-lg border bg-white pl-10 pr-10 py-2.5 text-sm placeholder-gray-400 focus:ring-2 focus:outline-none ${
                      !passwordLongEnough
                        ? 'border-red-300 focus:border-red-500 focus:ring-red-500/20'
                        : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500/20'
                    }`}
                    placeholder="Mín. 8 caracteres"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label htmlFor="password_confirmation" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Confirmar
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="password_confirmation"
                    name="password_confirmation"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    className={`w-full rounded-lg border bg-white pl-10 pr-3.5 py-2.5 text-sm placeholder-gray-400 focus:ring-2 focus:outline-none ${
                      !passwordsMatch
                        ? 'border-red-300 focus:border-red-500 focus:ring-red-500/20'
                        : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500/20'
                    }`}
                    placeholder="Repite la contraseña"
                    value={passwordConfirmation}
                    onChange={(e) => setPasswordConfirmation(e.target.value)}
                  />
                </div>
                {!passwordsMatch && (
                  <p className="text-xs text-red-600 mt-1">No coinciden</p>
                )}
              </div>
            </div>

            <button
              type="submit"
              disabled={!canSubmit}
              className="w-full rounded-lg bg-primary-800 hover:bg-primary-700 disabled:bg-primary-300 disabled:cursor-not-allowed text-white font-semibold py-2.5 transition-colors"
            >
              {loading ? 'Creando cuenta...' : 'Crear cuenta'}
            </button>

            <p className="text-xs text-center text-gray-500 mt-4">
              Al continuar aceptas que tu cuenta será asociada a un comercio nuevo.
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}
