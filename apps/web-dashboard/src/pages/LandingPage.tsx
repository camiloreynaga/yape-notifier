import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Shield,
  KeyRound,
  Banknote,
  Play,
  ArrowRight,
  Menu,
  X,
  CheckCircle2,
  XCircle,
  Coins,
  LayoutGrid,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

// ─── Top Nav ────────────────────────────────────────────────────────────────

function TopNav() {
  const { isAuthenticated } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const navLinks = [
    { label: 'Producto', href: '#producto' },
    { label: 'Precios', href: '#precios' },
    { label: 'Programa de Afiliados', href: '#afiliados' },
    { label: 'FAQ', href: '#faq' },
  ];

  return (
    <header
      className={`sticky top-0 z-50 transition-colors duration-300 ${
        scrolled ? 'bg-primary-900/95 backdrop-blur-sm shadow-lg' : 'bg-transparent'
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
        {/* Wordmark */}
        <Link to="/" className="font-extrabold text-xl text-white tracking-tight">
          Yape Notifier
        </Link>

        {/* Desktop center links */}
        <nav className="hidden md:flex items-center gap-8">
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm text-primary-100/80 hover:text-white transition-colors"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* Desktop right CTA */}
        <div className="hidden md:flex items-center gap-4">
          {isAuthenticated ? (
            <Link
              to="/dashboard"
              className="bg-accent-500 text-primary-900 font-semibold px-5 py-2 rounded-full text-sm hover:bg-accent-400 transition"
            >
              Ir al dashboard
            </Link>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm text-primary-100/80 hover:text-white transition-colors"
              >
                Iniciar sesión
              </Link>
              <Link
                to="/register"
                className="bg-accent-500 text-primary-900 font-semibold px-5 py-2 rounded-full text-sm hover:bg-accent-400 transition"
              >
                Empezar gratis
              </Link>
            </>
          )}
        </div>

        {/* Mobile hamburger */}
        <button
          className="md:hidden text-white p-1"
          onClick={() => setMobileOpen(true)}
          aria-label="Abrir menú"
        >
          <Menu className="w-6 h-6" />
        </button>
      </div>

      {/* Mobile slide-over */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 flex">
          <div
            className="flex-1 bg-black/50"
            onClick={() => setMobileOpen(false)}
          />
          <div className="w-72 bg-primary-900 flex flex-col p-6 gap-6">
            <div className="flex justify-between items-center">
              <span className="font-extrabold text-xl text-white">Yape Notifier</span>
              <button onClick={() => setMobileOpen(false)} aria-label="Cerrar menú">
                <X className="w-5 h-5 text-white" />
              </button>
            </div>
            <nav className="flex flex-col gap-5 mt-2">
              {navLinks.map((link) => (
                <a
                  key={link.href}
                  href={link.href}
                  className="text-primary-100/80 hover:text-white transition-colors text-base"
                  onClick={() => setMobileOpen(false)}
                >
                  {link.label}
                </a>
              ))}
            </nav>
            <div className="flex flex-col gap-3 mt-auto">
              {isAuthenticated ? (
                <Link
                  to="/dashboard"
                  className="bg-accent-500 text-primary-900 font-semibold px-5 py-3 rounded-full text-sm text-center hover:bg-accent-400 transition"
                  onClick={() => setMobileOpen(false)}
                >
                  Ir al dashboard
                </Link>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="text-primary-100/80 hover:text-white text-sm text-center transition-colors"
                    onClick={() => setMobileOpen(false)}
                  >
                    Iniciar sesión
                  </Link>
                  <Link
                    to="/register"
                    className="bg-accent-500 text-primary-900 font-semibold px-5 py-3 rounded-full text-sm text-center hover:bg-accent-400 transition"
                    onClick={() => setMobileOpen(false)}
                  >
                    Empezar gratis
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

// ─── Dashboard Mockup ────────────────────────────────────────────────────────

function DashboardMockup() {
  const transactions = [
    { name: 'Karol', amount: 'S/ 49.00', status: 'Validado', statusColor: 'bg-emerald-50 text-emerald-700' },
    { name: 'Pepe', amount: 'S/ 18.50', status: 'Validado', statusColor: 'bg-emerald-50 text-emerald-700' },
    { name: 'Anónimo', amount: 'S/ 200.00', status: 'Duplicado detectado', statusColor: 'bg-rose-50 text-rose-700' },
  ];

  return (
    <div className="bg-white rounded-2xl shadow-2xl ring-1 ring-black/5 max-w-2xl mx-auto overflow-hidden">
      {/* Card header */}
      <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 uppercase tracking-widest font-medium">Centro de Validación</p>
          <p className="text-sm text-slate-500 mt-0.5">
            <span className="font-semibold text-slate-700">Hoy: 47 Yapes</span>
            <span className="mx-2 text-slate-300">·</span>
            <span className="font-semibold text-slate-700">S/ 2,340</span>
          </p>
        </div>
        <span className="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-700 text-xs font-semibold px-2.5 py-1 rounded-full">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          En vivo
        </span>
      </div>

      {/* Table */}
      <div className="divide-y divide-slate-50">
        <div className="grid grid-cols-3 px-5 py-2 bg-slate-50">
          <span className="text-xs text-slate-400 font-medium uppercase tracking-wider">Enviado por</span>
          <span className="text-xs text-slate-400 font-medium uppercase tracking-wider text-center">Monto</span>
          <span className="text-xs text-slate-400 font-medium uppercase tracking-wider text-right">Estado</span>
        </div>
        {transactions.map((tx, i) => (
          <div key={i} className="grid grid-cols-3 px-5 py-3 items-center hover:bg-slate-50/50 transition-colors">
            <span className="text-sm font-medium text-slate-800">{tx.name}</span>
            <span className="text-sm font-mono tabular-nums text-slate-700 text-center">{tx.amount}</span>
            <div className="flex justify-end">
              <span className={`inline-flex text-xs font-semibold px-2.5 py-1 rounded-full ${tx.statusColor}`}>
                {tx.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Hero Section ────────────────────────────────────────────────────────────

function HeroSection() {
  return (
    <section className="bg-gradient-to-b from-primary-900 via-primary-800 to-primary-700 pt-4 pb-24 min-h-[85vh] flex flex-col items-center justify-center px-6">
      <div className="w-full max-w-7xl mx-auto flex flex-col items-center text-center">
        {/* Pill */}
        <span className="bg-white/10 text-accent-300 px-3 py-1 rounded-full text-xs uppercase tracking-widest border border-white/15 inline-block">
          ✨ Compatible con todas las cuentas Yape
        </span>

        {/* H1 */}
        <h1 className="text-5xl md:text-7xl font-extrabold leading-[1.05] tracking-tight text-white max-w-4xl mt-6">
          Control total de tus Yapes, sin{' '}
          <span className="text-accent-300 underline decoration-accent-500 decoration-4 underline-offset-8">
            riesgos
          </span>
          .
        </h1>

        {/* Subheadline */}
        <p className="text-lg md:text-xl text-primary-100/80 max-w-2xl mt-6 leading-relaxed">
          Tus trabajadores cobran sin tener acceso a tu cuenta. Pagos falsos y
          duplicados se detectan al instante.{' '}
          <strong className="text-white font-semibold">Sin comisión por transacción.</strong>
        </p>

        {/* CTAs */}
        <div className="flex flex-wrap items-center justify-center gap-4 mt-8">
          <Link
            to="/register"
            className="bg-accent-500 text-primary-900 font-semibold px-6 py-3 rounded-full hover:bg-accent-400 transition flex items-center gap-2"
          >
            Empezar gratis
            <ArrowRight className="w-4 h-4" />
          </Link>
          <a
            href="#demo"
            className="text-white border border-white/20 px-6 py-3 rounded-full hover:bg-white/10 transition flex items-center gap-2"
          >
            <Play className="w-4 h-4" />
            Ver demo de 60 seg
          </a>
        </div>

        {/* Trust line */}
        <p className="mt-6 text-xs text-primary-200/70">
          Sin tarjeta · Cancelas cuando quieras · 7 días gratis
        </p>

        {/* Mockup stage */}
        <div className="relative mt-16 w-full max-w-2xl mx-auto">
          {/* Floating card: top-left — pago validado */}
          <div className="hidden lg:block absolute -top-6 -left-10 rotate-[-3deg] bg-white rounded-xl shadow-lg p-3 ring-1 ring-black/5 w-44 z-10">
            <div className="flex items-center gap-2 mb-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span className="text-xs font-semibold text-slate-700">Pago validado</span>
            </div>
            <p className="text-lg font-bold text-slate-900 font-mono tabular-nums">S/ 49.00</p>
            <p className="text-xs text-slate-400 mt-0.5">hace 2 seg</p>
          </div>

          {/* Floating card: bottom-left — falso detectado */}
          <div className="hidden lg:block absolute -bottom-6 -left-12 rotate-[2deg] bg-white rounded-xl shadow-lg p-3 ring-1 ring-black/5 w-44 z-10">
            <div className="flex items-center gap-2 mb-1">
              <XCircle className="w-4 h-4 text-rose-500 shrink-0" />
              <span className="text-xs font-semibold text-slate-700">Falso detectado</span>
            </div>
            <p className="text-xs text-rose-600 font-medium mt-1">Captura no real</p>
          </div>

          {/* Floating card: top-right — comisión 0% */}
          <div className="hidden lg:block absolute -top-8 -right-8 rotate-[2deg] bg-white rounded-xl shadow-lg p-3 ring-1 ring-black/5 w-44 z-10">
            <div className="flex items-center gap-2 mb-1">
              <Coins className="w-4 h-4 text-accent-600 shrink-0" />
              <span className="text-xs font-semibold text-slate-700">0% comisión</span>
            </div>
            <p className="text-xs text-slate-500 mt-1">Tarifa fija mensual</p>
          </div>

          {/* Floating card: bottom-right — cuentas controladas */}
          <div className="hidden lg:block absolute -bottom-4 -right-14 rotate-[-3deg] bg-white rounded-xl shadow-lg p-3 ring-1 ring-black/5 w-44 z-10">
            <div className="flex items-center gap-2 mb-1">
              <LayoutGrid className="w-4 h-4 text-primary-600 shrink-0" />
              <span className="text-xs font-semibold text-slate-700">3 cuentas Yape</span>
            </div>
            <p className="text-xs text-slate-500 mt-1">controladas</p>
          </div>

          {/* Central mockup */}
          <div className="relative z-0">
            <DashboardMockup />
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Beneficios Row ──────────────────────────────────────────────────────────

function BeneficiosSection() {
  const items = [
    {
      Icon: Shield,
      title: 'Anti-fraude automático',
      body: 'Cada pago se compara contra la notificación real de Yape. Las capturas falsas y los duplicados se marcan al instante.',
    },
    {
      Icon: KeyRound,
      title: 'Tus trabajadores no ven tu cuenta',
      body: 'Acceden con PIN a una vista limitada. Tu app de Yape nunca sale de tu celular. Cero riesgo de filtraciones bancarias.',
    },
    {
      Icon: Banknote,
      title: 'Sin comisión por transacción',
      body: 'Tarifa fija mensual. Recibe 1, 100 o 10,000 Yapes — pagas lo mismo. Tu margen se queda contigo.',
    },
  ];

  return (
    <section className="py-20 bg-surface-warm" id="producto">
      <div className="max-w-7xl mx-auto px-6">
        <div className="grid md:grid-cols-3 gap-8">
          {items.map(({ Icon, title, body }) => (
            <div key={title} className="flex flex-col gap-4">
              <div className="w-12 h-12 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center shrink-0">
                <Icon className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-xl text-slate-900">{title}</h3>
                <p className="text-slate-600 mt-2 leading-relaxed">{body}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Problema Section ────────────────────────────────────────────────────────

function ProblemaSection() {
  const problems = [
    {
      num: '01',
      title: 'Le prestas tu celular para que cobren',
      body: 'Tus trabajadores acceden a todo: saldos, transferencias, contactos. El robo sistemático (gota a gota) se vuelve invisible.',
    },
    {
      num: '02',
      title: 'Aceptas capturas como prueba de pago',
      body: 'Cualquiera puede fabricar una captura realista en 30 segundos. Sin verificación contra la app real, tu negocio queda expuesto.',
    },
    {
      num: '03',
      title: 'Pagas comisión por cada Yape',
      body: 'Las pasarelas cobran 2-4% por operación. Si recibes 300 Yapes/mes de S/ 50 cada uno, eso son S/ 360-720 al mes drenados directo de tu margen.',
    },
  ];

  return (
    <section className="py-24 bg-gradient-to-b from-primary-900 to-[#1E0840] text-white">
      <div className="max-w-7xl mx-auto px-6">
        <p className="text-accent-400 uppercase tracking-widest text-sm font-medium">El problema oculto</p>
        <h2 className="text-4xl md:text-5xl font-bold mt-3 max-w-3xl leading-tight">
          Tres formas en las que estás perdiendo plata. Hoy.
        </h2>

        <div className="grid md:grid-cols-3 gap-6 mt-16">
          {problems.map(({ num, title, body }) => (
            <div
              key={num}
              className="bg-white/5 ring-1 ring-white/10 rounded-2xl p-6 backdrop-blur-sm"
            >
              <p className="text-accent-400 text-sm font-mono">{num}</p>
              <h3 className="text-xl font-semibold mt-2 leading-snug">{title}</h3>
              <p className="text-primary-100/70 mt-3 text-sm leading-relaxed">{body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Solución Section ────────────────────────────────────────────────────────

function SolucionSection() {
  const steps = [
    {
      n: '1',
      title: 'Conecta tu celular con Yape al sistema',
      body: 'Un dispositivo Android donde está instalado Yape se vincula al dashboard. Listo en 5 minutos.',
    },
    {
      n: '2',
      title: 'Tus captadores entran con su propio PIN',
      body: 'Desde sus dispositivos personales. No tienen acceso al saldo, transferencias ni datos bancarios. Solo validan los pagos que llegan.',
    },
    {
      n: '3',
      title: 'Cada Yape se confirma o se rechaza automáticamente',
      body: 'Pagos validados quedan registrados. Duplicados y falsos se detectan al instante. Tienes el control 24/7.',
    },
  ];

  return (
    <section className="py-24 bg-surface-warm">
      <div className="max-w-7xl mx-auto px-6">
        <p className="text-primary-700 uppercase tracking-widest text-sm font-medium">La solución</p>
        <h2 className="text-4xl md:text-5xl font-bold mt-3 text-slate-900 max-w-3xl leading-tight">
          Cobra Yapes sin compartir nada que importe.
        </h2>

        <div className="flex flex-col md:flex-row items-start md:items-center gap-8 mt-16">
          {steps.map((step, i) => (
            <>
              <div key={step.n} className="flex-1 min-w-0">
                <p className="text-7xl font-extrabold text-accent-500 leading-none">{step.n}</p>
                <h3 className="text-xl font-semibold mt-2 text-slate-900 leading-snug">{step.title}</h3>
                <p className="text-slate-600 mt-2 text-sm leading-relaxed">{step.body}</p>
              </div>
              {i < steps.length - 1 && (
                <ArrowRight
                  key={`arrow-${i}`}
                  className="hidden md:block w-8 h-8 text-primary-300 shrink-0 mt-6"
                />
              )}
            </>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export default function LandingPage() {
  return (
    <div className="min-h-screen">
      <TopNav />
      <HeroSection />
      <BeneficiosSection />
      <ProblemaSection />
      <SolucionSection />
    </div>
  );
}
