import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

export default function LandingPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-900 via-primary-800 to-primary-700 text-white flex flex-col items-center justify-center px-6">
      <div className="max-w-3xl text-center space-y-6">
        <p className="text-accent-400 text-sm uppercase tracking-widest">Yape Notifier</p>
        <h1 className="text-5xl md:text-6xl font-extrabold leading-tight">
          Control total de tus Yapes, sin riesgos.
        </h1>
        <p className="text-lg text-primary-100 max-w-xl mx-auto">
          Landing en construcción. Esta es la página pública raíz —
          los siguientes pasos del plan agregan hero completo, secciones, calculadora de pérdida, FAQ y footer.
        </p>
        <div className="flex gap-3 justify-center pt-4">
          {isAuthenticated ? (
            <Link
              to="/dashboard"
              className="rounded-full bg-accent-500 text-primary-900 px-6 py-3 font-semibold hover:bg-accent-400 transition"
            >
              Ir al dashboard
            </Link>
          ) : (
            <>
              <Link
                to="/register"
                className="rounded-full bg-accent-500 text-primary-900 px-6 py-3 font-semibold hover:bg-accent-400 transition"
              >
                Empezar gratis
              </Link>
              <Link
                to="/login"
                className="rounded-full border border-white/30 px-6 py-3 font-semibold hover:bg-white/10 transition"
              >
                Iniciar sesión
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
