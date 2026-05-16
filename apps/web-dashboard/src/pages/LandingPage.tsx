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
  Check,
  TrendingDown,
  AlertTriangle,
  Smartphone,
  Users,
  Copy,
  BarChart3,
  Layers,
  LockKeyhole,
  Quote,
  ChevronDown,
  MessageCircle,
  Gift,
  Instagram,
  Facebook,
  Linkedin,
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

// ─── Multi-Yape Section ──────────────────────────────────────────────────────

function MultiYapeSection() {
  const cuentas = [
    { label: 'Bodega', amount: 'S/ 2,340', yapes: '47 Yapes hoy', borderColor: 'border-l-primary-400' },
    { label: 'Farmacia', amount: 'S/ 5,890', yapes: '112 Yapes hoy', borderColor: 'border-l-accent-500' },
    { label: 'Delivery', amount: 'S/ 980', yapes: '23 Yapes hoy', borderColor: 'border-l-violet-400' },
  ];

  const bullets = [
    'Soporta Yape personal + Yape negocio en el mismo celular (Android multi-user)',
    'Detecta automáticamente de qué cuenta vino cada pago',
    'Reportes consolidados o filtrados por cuenta',
  ];

  return (
    <section id="multi-yape" className="py-24 bg-gradient-to-br from-primary-50 via-white to-accent-50">
      <div className="max-w-7xl mx-auto px-6">
        {/* Header */}
        <p className="text-primary-700 uppercase tracking-widest text-sm font-medium">El feature que nadie más tiene</p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3 max-w-3xl leading-tight">
          ¿Tienes 2, 3 o 5 cuentas Yape? Las controlas todas desde la misma pantalla.
        </h2>
        <p className="text-lg text-slate-600 mt-4 max-w-2xl leading-relaxed">
          Yape de la bodega + Yape de la farmacia + Yape de delivery. En un solo lugar,
          separados por instancia, con KPIs individuales.
        </p>

        {/* Diagram */}
        <div className="mt-16 flex flex-col lg:flex-row items-center gap-10 lg:gap-8">
          {/* Left: stacked phones */}
          <div className="relative flex items-center justify-center w-72 h-64 shrink-0">
            {/* Phone 3 — back */}
            <div className="absolute top-0 left-4 w-36 h-60 bg-slate-900 rounded-[22px] shadow-lg ring-1 ring-white/10 transform rotate-[-6deg] translate-y-2">
              <div className="m-1.5 h-full bg-white rounded-[18px] overflow-hidden">
                <div className="bg-primary-700 px-3 py-2">
                  <p className="text-white text-[10px] font-bold leading-tight">Yape · Delivery</p>
                </div>
              </div>
            </div>
            {/* Phone 2 — middle */}
            <div className="absolute top-3 left-14 w-36 h-60 bg-slate-900 rounded-[22px] shadow-xl ring-1 ring-white/10 transform rotate-[-2deg]">
              <div className="m-1.5 h-full bg-white rounded-[18px] overflow-hidden">
                <div className="bg-primary-700 px-3 py-2">
                  <p className="text-white text-[10px] font-bold leading-tight">Yape · Farmacia</p>
                </div>
              </div>
            </div>
            {/* Phone 1 — front */}
            <div className="absolute top-6 left-24 w-36 h-60 bg-slate-900 rounded-[22px] shadow-2xl ring-1 ring-white/10 transform rotate-[3deg]">
              <div className="m-1.5 h-full bg-white rounded-[18px] overflow-hidden">
                <div className="bg-primary-700 px-3 py-2">
                  <p className="text-white text-[10px] font-bold leading-tight">Yape · Bodega</p>
                </div>
              </div>
            </div>
          </div>

          {/* Arrow */}
          <div className="flex flex-col lg:flex-row items-center gap-2 shrink-0">
            <ArrowRight className="h-12 w-12 text-primary-500 hidden lg:block" />
            <ArrowRight className="h-8 w-8 text-primary-500 rotate-90 lg:hidden" />
          </div>

          {/* Right: dashboard card */}
          <div className="flex-1 bg-white rounded-3xl shadow-xl ring-1 ring-slate-200 p-6 min-w-0">
            <div className="flex items-center justify-between mb-5">
              <p className="text-xs text-slate-400 uppercase tracking-widest font-medium">Dashboard Yape Notifier</p>
              <span className="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-700 text-xs font-semibold px-2.5 py-1 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                En vivo
              </span>
            </div>
            <div className="grid grid-cols-3 gap-4">
              {cuentas.map((c) => (
                <div
                  key={c.label}
                  className={`border-l-4 ${c.borderColor} pl-4 py-2`}
                >
                  <p className="text-xs text-slate-500 font-medium uppercase tracking-wide">{c.label}</p>
                  <p className="text-2xl font-extrabold text-slate-900 mt-1 font-mono tabular-nums">{c.amount}</p>
                  <p className="text-xs text-slate-500 mt-1">{c.yapes}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Bullet points */}
        <ul className="mt-12 flex flex-col sm:flex-row gap-4 sm:gap-8">
          {bullets.map((b) => (
            <li key={b} className="flex items-start gap-2.5">
              <Check className="w-5 h-5 text-accent-500 shrink-0 mt-0.5" />
              <span className="text-slate-700 text-sm leading-relaxed">{b}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

// ─── Loss Calculator Section ─────────────────────────────────────────────────

function LossCalculatorSection() {
  const [yapesPerMonth, setYapesPerMonth] = useState(300);
  const [avgTicket, setAvgTicket] = useState(50);
  const [employees, setEmployees] = useState(3);

  const annualYapes = yapesPerMonth * 12;
  const falsosLoss = annualYapes * avgTicket * 0.02;
  const skimmingLoss = employees * 100 * 12;
  const totalLoss = falsosLoss + skimmingLoss;

  const fmt = (n: number) =>
    new Intl.NumberFormat('es-PE', { maximumFractionDigits: 0 }).format(n);

  type SliderProps = {
    label: string;
    value: number;
    min: number;
    max: number;
    step: number;
    onChange: (v: number) => void;
    suffix?: string;
  };

  function Slider({ label, value, min, max, step, onChange, suffix = '' }: SliderProps) {
    return (
      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <span className="text-sm text-primary-100/80">{label}</span>
          <span className="bg-white/10 text-white text-sm font-semibold px-3 py-0.5 rounded-full tabular-nums">
            {value}{suffix}
          </span>
        </div>
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="w-full h-2 rounded-full appearance-none cursor-pointer bg-white/10"
          style={{ accentColor: '#1FD4D4' }}
        />
        <div className="flex justify-between text-xs text-primary-100/40">
          <span>{min}{suffix}</span>
          <span>{max}{suffix}</span>
        </div>
      </div>
    );
  }

  return (
    <section id="calculadora" className="py-24 bg-gradient-to-b from-primary-900 to-[#1E0840] text-white">
      <div className="max-w-7xl mx-auto px-6">
        {/* Header */}
        <p className="text-accent-400 uppercase tracking-widest text-sm font-medium">El costo de no controlarlo</p>
        <h2 className="text-4xl md:text-5xl font-bold mt-3 max-w-3xl leading-tight">
          ¿Cuánto te está costando ahora mismo?
        </h2>
        <p className="text-primary-100/70 mt-4 max-w-2xl text-lg leading-relaxed">
          Ajusta los valores y mira cuánto pierdes al año por pagos falsos y robo sistemático.
          Spoiler: más que la suscripción.
        </p>

        {/* Two-column layout */}
        <div className="mt-12 grid lg:grid-cols-2 gap-12 items-start">
          {/* Left: sliders */}
          <div className="flex flex-col gap-8">
            <Slider
              label="Yapes que recibes al mes"
              value={yapesPerMonth}
              min={50}
              max={2000}
              step={50}
              onChange={setYapesPerMonth}
            />
            <Slider
              label="Ticket promedio (S/)"
              value={avgTicket}
              min={10}
              max={500}
              step={5}
              onChange={setAvgTicket}
              suffix=" S/"
            />
            <Slider
              label="Trabajadores que manejan Yape"
              value={employees}
              min={1}
              max={15}
              step={1}
              onChange={setEmployees}
            />
          </div>

          {/* Right: results card */}
          <div className="bg-white/5 ring-1 ring-white/10 rounded-3xl p-8 backdrop-blur-sm">
            {/* Row 1: pagos falsos */}
            <div className="flex items-start gap-4 pb-5 border-b border-white/10">
              <div className="w-10 h-10 rounded-xl bg-rose-500/20 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-rose-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-primary-100/70">Pagos falsos sin detectar (2% est.)</p>
                <p className="text-2xl font-bold text-rose-300 mt-1 tabular-nums">
                  S/ {fmt(falsosLoss)}/año
                </p>
              </div>
            </div>

            {/* Row 2: skimming */}
            <div className="flex items-start gap-4 py-5 border-b border-white/10">
              <div className="w-10 h-10 rounded-xl bg-orange-500/20 flex items-center justify-center shrink-0">
                <TrendingDown className="w-5 h-5 text-orange-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-primary-100/70">Gota a gota de cajeros (~S/ 100/mes por trabajador)</p>
                <p className="text-2xl font-bold text-orange-300 mt-1 tabular-nums">
                  S/ {fmt(skimmingLoss)}/año
                </p>
              </div>
            </div>

            {/* Total */}
            <div className="pt-6">
              <p className="text-xs text-primary-100/40 uppercase tracking-widest font-medium">Total que estás perdiendo</p>
              <p className="text-5xl md:text-6xl font-extrabold text-rose-400 mt-2 tabular-nums leading-none">
                S/ {fmt(totalLoss)}/año
              </p>
              <p className="text-accent-300 mt-6 text-lg">
                Recuperas todo eso por solo{' '}
                <strong className="text-accent-200">S/ 49 al mes</strong>.
              </p>
              <Link
                to="/register"
                className="bg-accent-500 text-primary-900 px-6 py-3 rounded-full font-semibold mt-6 inline-flex items-center gap-2 hover:bg-accent-400 transition"
              >
                Cerrar la fuga ahora
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Features Bento Grid ─────────────────────────────────────────────────────

function FeaturesBentoSection() {
  return (
    <section id="producto" className="py-24 bg-surface-warm">
      <div className="max-w-7xl mx-auto px-6">
        {/* Header */}
        <p className="text-primary-700 uppercase tracking-widest text-sm font-medium">
          Todo lo que necesitas, nada que estorbe
        </p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3 leading-tight">
          Diseñado para negocios reales.
        </h2>

        {/* Bento grid */}
        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Cell 1 — 2 cols */}
          <div className="md:col-span-2 bg-white rounded-3xl p-8 ring-1 ring-slate-200 flex flex-col justify-between">
            <div>
              <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center">
                <Smartphone className="w-5 h-5" />
              </div>
              <h3 className="text-xl font-semibold text-slate-900 mt-4">Multi-dispositivo Android</h3>
              <p className="text-slate-600 mt-2 text-sm leading-relaxed">
                Conecta 1, 5 o 50 celulares al mismo dashboard. Cada uno reporta sus Yapes en
                tiempo real. Sin límite de captadores.
              </p>
            </div>
            {/* Mini visual: 3 phone icons */}
            <div className="flex items-end gap-3 mt-8 opacity-80">
              {[0, 1, 2].map((i) => (
                <div
                  key={i}
                  className="bg-primary-50 rounded-xl p-3 ring-1 ring-primary-100 flex flex-col items-center gap-1"
                  style={{ transform: `scale(${0.85 + i * 0.075})` }}
                >
                  <Smartphone className="w-5 h-5 text-primary-600" />
                  <span className="text-[10px] text-primary-500 font-medium">Captador {i + 1}</span>
                </div>
              ))}
              <div className="ml-auto text-xs text-slate-400 italic self-center">+ más</div>
            </div>
          </div>

          {/* Cell 2 */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200">
            <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
            <h3 className="text-xl font-semibold text-slate-900 mt-4">Roles y permisos</h3>
            <p className="text-slate-600 mt-2 text-sm leading-relaxed">
              Admin gestiona, captador solo valida. PIN de 4 dígitos, sin contraseñas que se filtren.
            </p>
          </div>

          {/* Cell 3 */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200">
            <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center">
              <Copy className="w-5 h-5" />
            </div>
            <h3 className="text-xl font-semibold text-slate-900 mt-4">Detección de duplicados</h3>
            <p className="text-slate-600 mt-2 text-sm leading-relaxed">
              Si el mismo Yape llega dos veces, lo marcamos. Si una captura no coincide con un Yape
              real, también.
            </p>
          </div>

          {/* Cell 4 */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200">
            <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center">
              <BarChart3 className="w-5 h-5" />
            </div>
            <h3 className="text-xl font-semibold text-slate-900 mt-4">Reportes en tiempo real</h3>
            <p className="text-slate-600 mt-2 text-sm leading-relaxed">
              KPIs por día, semana, mes. Por captador. Por cuenta Yape. Exportables a Excel.
            </p>
          </div>

          {/* Cell 5 */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200">
            <div className="w-10 h-10 rounded-xl bg-primary-50 text-primary-700 flex items-center justify-center">
              <Layers className="w-5 h-5" />
            </div>
            <h3 className="text-xl font-semibold text-slate-900 mt-4">Multi-app Yape</h3>
            <p className="text-slate-600 mt-2 text-sm leading-relaxed">
              Yape Personal + Yape Negocio en el mismo celular (Android multi-user). Cada Yape se
              diferencia automáticamente.
            </p>
          </div>

          {/* Cell 6 — 3 cols, feature destacado */}
          <div className="md:col-span-3 bg-gradient-to-br from-primary-50 to-accent-50 rounded-3xl p-8 ring-1 ring-primary-100 flex flex-col md:flex-row items-start md:items-center gap-6">
            <div className="w-12 h-12 rounded-2xl bg-primary-100 text-primary-700 flex items-center justify-center shrink-0">
              <LockKeyhole className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-xl font-semibold text-slate-900">Tu cuenta Yape nunca sale de tus manos</h3>
              <p className="text-slate-600 mt-2 text-sm leading-relaxed max-w-3xl">
                Tus trabajadores ven solo lo necesario para validar un cobro. Cero acceso a saldos,
                transferencias o contactos. Cuando un empleado renuncia, simplemente desactivas su
                PIN — no tienes que cambiar nada en tu app de Yape.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Demo Visual Section ──────────────────────────────────────────────────────

function DemoSection() {
  type TxStatus = 'Validado' | 'Duplicado' | 'Pendiente' | 'Inconsistente';

  const statusStyle: Record<TxStatus, string> = {
    Validado: 'bg-emerald-50 text-emerald-700',
    Duplicado: 'bg-rose-50 text-rose-700',
    Pendiente: 'bg-amber-50 text-amber-700',
    Inconsistente: 'bg-violet-50 text-violet-700',
  };

  const rows: { name: string; amount: string; time: string; status: TxStatus }[] = [
    { name: 'Karol Mendoza',       amount: 'S/ 49.00',  time: '10:03',  status: 'Validado' },
    { name: 'José Pérez',          amount: 'S/ 18.50',  time: '10:07',  status: 'Validado' },
    { name: 'Cliente anónimo',     amount: 'S/ 200.00', time: '10:12',  status: 'Duplicado' },
    { name: 'Ana Torres',          amount: 'S/ 35.00',  time: '10:15',  status: 'Validado' },
    { name: 'Luis Quispe',         amount: 'S/ 80.00',  time: '10:21',  status: 'Pendiente' },
    { name: 'María Flores',        amount: 'S/ 120.00', time: '10:28',  status: 'Validado' },
    { name: 'Cliente anónimo',     amount: 'S/ 50.00',  time: '10:34',  status: 'Inconsistente' },
    { name: 'Roberto Ccopa',       amount: 'S/ 25.00',  time: '10:41',  status: 'Validado' },
  ];

  return (
    <section className="py-24 bg-white">
      <div className="max-w-7xl mx-auto px-6">
        {/* Header */}
        <p className="text-primary-700 uppercase tracking-widest text-sm font-medium">Así se ve</p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3 leading-tight">
          El Centro de Validación, en acción.
        </h2>
        <p className="text-slate-600 mt-4 max-w-2xl text-lg leading-relaxed">
          Cada pago, validado o sospechoso, en una sola pantalla. Tus captadores trabajan sin
          fricción. Tú duermes tranquilo.
        </p>

        {/* Big mockup */}
        <div className="mt-12 max-w-5xl mx-auto bg-white rounded-3xl shadow-2xl ring-1 ring-slate-200 p-6 overflow-x-auto">
          {/* Toolbar header */}
          <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
            <div>
              <p className="text-xs text-slate-400 uppercase tracking-widest font-medium">Centro de Validación</p>
              <p className="text-sm font-semibold text-slate-700 mt-0.5">
                Hoy · 47 operaciones
              </p>
            </div>
            <div className="flex items-center gap-3">
              <span className="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-700 text-xs font-semibold px-2.5 py-1 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                En vivo
              </span>
              <span className="text-xs text-slate-400 bg-slate-100 px-3 py-1 rounded-full">Hoy</span>
            </div>
          </div>

          {/* Table */}
          <div className="min-w-[480px]">
            {/* Header row */}
            <div className="grid grid-cols-4 px-4 py-2 bg-slate-50 rounded-xl mb-1">
              <span className="text-xs text-slate-400 font-medium uppercase tracking-wider">Enviado por</span>
              <span className="text-xs text-slate-400 font-medium uppercase tracking-wider text-center">Monto</span>
              <span className="text-xs text-slate-400 font-medium uppercase tracking-wider text-center">Hora</span>
              <span className="text-xs text-slate-400 font-medium uppercase tracking-wider text-right">Estado</span>
            </div>
            {/* Data rows */}
            {rows.map((row, i) => (
              <div
                key={i}
                className={`grid grid-cols-4 px-4 py-3 items-center rounded-lg ${
                  i % 2 === 0 ? 'bg-white' : 'bg-slate-50/60'
                } hover:bg-primary-50/30 transition-colors`}
              >
                <span className="text-sm font-medium text-slate-800 truncate pr-2">{row.name}</span>
                <span className="text-sm font-mono tabular-nums text-slate-700 text-center">{row.amount}</span>
                <span className="text-xs text-slate-400 text-center font-mono">{row.time}</span>
                <div className="flex justify-end">
                  <span
                    className={`inline-flex text-xs font-semibold px-2.5 py-1 rounded-full ${
                      statusStyle[row.status]
                    }`}
                  >
                    {row.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Captions */}
        <div className="mt-8 flex flex-col sm:flex-row gap-3 text-sm text-slate-600 max-w-5xl mx-auto">
          <span className="bg-emerald-50 text-emerald-700 px-4 py-2 rounded-full">
            🟢 Verde = validado y registrado.
          </span>
          <span className="bg-rose-50 text-rose-700 px-4 py-2 rounded-full">
            🌸 Rosa = duplicado o falso detectado automáticamente.
          </span>
        </div>
      </div>
    </section>
  );
}

// ─── Pricing Section ─────────────────────────────────────────────────────────

function PricingSection() {
  return (
    <section id="precios" className="py-24 bg-gradient-to-b from-white to-primary-50">
      <div className="max-w-6xl mx-auto px-6 text-center">
        <p className="text-primary-700 uppercase tracking-widest text-sm font-semibold">Precios</p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3">
          Tarifa fija. Sin comisión por transacción. Para siempre.
        </h2>
        <p className="text-lg text-slate-600 mt-4 max-w-2xl mx-auto">
          Elige el plan según cuántos dispositivos necesites conectar. Cancela cuando quieras.
        </p>

        <div className="mt-12 grid md:grid-cols-3 gap-6 max-w-5xl mx-auto">
          {/* Starter */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200 flex flex-col">
            <h3 className="text-xl font-bold text-slate-900">Starter</h3>
            <p className="text-slate-500 text-sm mt-1">Para empezar</p>
            <div className="flex items-baseline mt-6">
              <span className="text-5xl font-extrabold text-slate-900">S/ 49</span>
              <span className="text-slate-500 ml-2 text-sm">/mes</span>
            </div>
            <div className="border-t border-slate-200 my-6" />
            <ul className="space-y-3 flex-1 text-left">
              {[
                '1 dispositivo Android conectado',
                'Hasta 500 Yapes/mes',
                '2 captadores',
                'Multi-cuenta Yape',
                'Reportes básicos',
                'Soporte por email',
              ].map((f) => (
                <li key={f} className="flex items-center gap-2">
                  <Check className="h-4 w-4 text-accent-500 shrink-0" />
                  <span className="text-sm text-slate-700">{f}</span>
                </li>
              ))}
            </ul>
            <Link
              to="/register"
              className="mt-6 w-full py-3 rounded-full font-semibold text-center border border-primary-600 text-primary-700 hover:bg-primary-50 transition block"
            >
              Empezar gratis 7 días
            </Link>
          </div>

          {/* Pro */}
          <div className="bg-white rounded-3xl p-8 ring-2 ring-primary-600 lg:scale-105 relative flex flex-col">
            <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary-600 text-white text-xs font-semibold px-3 py-1 rounded-full">
              MÁS POPULAR
            </span>
            <h3 className="text-xl font-bold text-slate-900">Pro</h3>
            <p className="text-slate-500 text-sm mt-1">El más usado</p>
            <div className="flex items-baseline mt-6">
              <span className="text-5xl font-extrabold text-slate-900">S/ 99</span>
              <span className="text-slate-500 ml-2 text-sm">/mes</span>
            </div>
            <div className="border-t border-slate-200 my-6" />
            <ul className="space-y-3 flex-1 text-left">
              {[
                'Hasta 5 dispositivos',
                'Yapes ilimitados',
                'Captadores ilimitados',
                'Multi-cuenta Yape',
                'Reportes avanzados + exportación',
                'Programa de afiliados activo',
                'Soporte prioritario',
              ].map((f) => (
                <li key={f} className="flex items-center gap-2">
                  <Check className="h-4 w-4 text-accent-500 shrink-0" />
                  <span className="text-sm text-slate-700">{f}</span>
                </li>
              ))}
            </ul>
            <Link
              to="/register"
              className="mt-6 w-full py-3 rounded-full font-semibold text-center bg-accent-500 text-primary-900 hover:bg-accent-400 transition block"
            >
              Probar Pro 7 días
            </Link>
          </div>

          {/* Empresa */}
          <div className="bg-white rounded-3xl p-8 ring-1 ring-slate-200 flex flex-col">
            <h3 className="text-xl font-bold text-slate-900">Empresa</h3>
            <p className="text-slate-500 text-sm mt-1">Para cadenas</p>
            <div className="flex items-baseline mt-6">
              <span className="text-5xl font-extrabold text-slate-900">A medida</span>
            </div>
            <div className="border-t border-slate-200 my-6" />
            <ul className="space-y-3 flex-1 text-left">
              {[
                'Dispositivos ilimitados',
                'Yapes ilimitados',
                'Reportes consolidados multi-sede',
                'API + integraciones',
                'SLA dedicado',
                'Onboarding personalizado',
              ].map((f) => (
                <li key={f} className="flex items-center gap-2">
                  <Check className="h-4 w-4 text-accent-500 shrink-0" />
                  <span className="text-sm text-slate-700">{f}</span>
                </li>
              ))}
            </ul>
            <a
              href="mailto:ventas@yape-notifier.com"
              className="mt-6 w-full py-3 rounded-full font-semibold text-center bg-primary-700 text-white hover:bg-primary-800 transition block"
            >
              Hablar con ventas
            </a>
          </div>
        </div>

        <p className="mt-8 text-center text-sm text-slate-500">
          Sin tarjeta de crédito durante el trial. Cancela en 1 click.
        </p>
      </div>
    </section>
  );
}

// ─── Afiliados Section ────────────────────────────────────────────────────────

function AfiliadosSection() {
  return (
    <section id="afiliados" className="py-20">
      <div className="max-w-4xl mx-auto px-6">
        <div className="bg-gradient-to-br from-primary-700 via-primary-600 to-accent-700 text-white rounded-3xl p-10 md:p-14 relative overflow-hidden">
          <Gift className="absolute top-6 right-6 opacity-10 w-32 h-32" />
          <p className="text-sm font-semibold uppercase tracking-widest text-primary-100">
            Programa de Afiliados
          </p>
          <h3 className="text-3xl md:text-4xl font-bold mt-2 max-w-2xl">
            Refiere otros comercios y gana 20% recurrente.
          </h3>
          <p className="text-primary-100/80 mt-4 max-w-2xl">
            Por cada negocio que se registre con tu link, recibes el 20% de su tarifa mensual
            mientras siga activo. Sin tope. Sin letra chica.
          </p>
          <Link
            to="/register"
            className="mt-6 inline-flex items-center gap-2 bg-accent-500 text-primary-900 px-6 py-3 rounded-full font-semibold hover:bg-accent-400 transition"
          >
            Conocer el programa
          </Link>
        </div>
      </div>
    </section>
  );
}

// ─── Testimonios Section ──────────────────────────────────────────────────────

function TestimoniosSection() {
  const testimonios = [
    {
      text: 'Antes pagaba 4% en comisiones por las pasarelas. Ahora pago una tarifa fija y duermo tranquilo.',
      name: 'Próximamente',
      business: '—',
    },
    {
      text: 'Cambié 3 cajas registradoras por Yape Notifier. Mis 4 captadores nunca tocan mi celular.',
      name: 'Próximamente',
      business: '—',
    },
    {
      text: 'Detectamos S/ 1,200 en pagos falsos el primer mes. La inversión se justificó sola.',
      name: 'Próximamente',
      business: '—',
    },
  ];

  return (
    <section className="py-24 bg-surface-warm">
      <div className="max-w-6xl mx-auto px-6 text-center">
        <p className="text-primary-700 uppercase tracking-widest text-sm font-semibold">Lo que dicen</p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3">
          Negocios reales que ya cerraron la fuga.
        </h2>
        <p className="mt-2 text-sm text-slate-500 max-w-xl mx-auto">
          Estamos preparando testimonios reales de los primeros usuarios. Mientras tanto, los casos
          abajo son ejemplos representativos del valor que el producto entrega.
        </p>

        <div className="mt-12 grid md:grid-cols-3 gap-6 max-w-5xl mx-auto">
          {testimonios.map((t, i) => (
            <div key={i} className="bg-white rounded-2xl p-6 ring-1 ring-slate-200 flex flex-col text-left">
              <Quote className="text-primary-300 h-8 w-8" />
              <p className="mt-4 text-slate-700 text-sm leading-relaxed flex-1">"{t.text}"</p>
              <div className="mt-6 flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-slate-200 shrink-0" />
                <div>
                  <p className="text-sm font-semibold text-slate-900">{t.name}</p>
                  <p className="text-xs text-slate-500">{t.business}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── FAQ Section ──────────────────────────────────────────────────────────────

function FAQSection() {
  const faqs = [
    {
      q: '¿Es seguro? ¿Pueden ver mi cuenta de Yape?',
      a: 'No. Tus captadores nunca tienen acceso a tu app de Yape ni a tus saldos. Solo ven las notificaciones de pagos entrantes en una vista limitada, autenticada con un PIN de 4 dígitos. Tu celular con Yape se queda contigo.',
    },
    {
      q: '¿Funciona con cualquier celular Android?',
      a: 'Sí. Yape Notifier funciona en cualquier Android con la app de Yape instalada (versión 6.0 o superior). No requiere root, ni permisos especiales de banco.',
    },
    {
      q: '¿Cuántas cuentas Yape puedo conectar?',
      a: 'Todas las que necesites. Puedes tener Yape personal + Yape negocio en el mismo celular (Android multi-user) y el sistema las diferencia automáticamente. Si tienes varios celulares con Yape, los conectas todos al mismo dashboard.',
    },
    {
      q: '¿Qué pasa si un captador renuncia?',
      a: 'Desactivas su PIN en 2 clicks desde tu dashboard. No tienes que cambiar contraseñas, recuperar dispositivos ni preocuparte por accesos olvidados. Es así de simple.',
    },
    {
      q: '¿Puedo cancelar cuando quiera?',
      a: 'Sí. No hay permanencia ni costos por cancelar. Si decides irte, exportas todos tus datos en CSV y cierras la cuenta en 1 click. Sin preguntas.',
    },
    {
      q: '¿Cuánto demora la instalación?',
      a: 'Menos de 10 minutos. Te enviamos el instructivo paso a paso al registrarte. Si necesitas ayuda, nuestro equipo te guía por WhatsApp sin costo durante el trial.',
    },
  ];

  return (
    <section id="faq" className="py-24 bg-white">
      <div className="max-w-6xl mx-auto px-6 text-center">
        <p className="text-primary-700 uppercase tracking-widest text-sm font-semibold">
          Preguntas frecuentes
        </p>
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mt-3">
          Lo que la gente se pregunta antes de empezar.
        </h2>

        <div className="mt-10 max-w-3xl mx-auto space-y-3 text-left">
          {faqs.map((item, i) => (
            <details key={i} className="group rounded-2xl bg-slate-50 ring-1 ring-slate-200 px-6 py-4">
              <summary className="cursor-pointer list-none flex items-center justify-between font-semibold text-slate-900">
                <span>{item.q}</span>
                <ChevronDown className="h-5 w-5 transition-transform group-open:rotate-180 shrink-0 ml-4" />
              </summary>
              <p className="mt-3 text-slate-600 text-sm leading-relaxed">{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── CTA Final Section ────────────────────────────────────────────────────────

function CTAFinalSection() {
  return (
    <section className="py-32 bg-gradient-to-br from-primary-900 via-primary-800 to-accent-900 text-white text-center relative overflow-hidden">
      <LockKeyhole className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 opacity-5 w-[500px] h-[500px]" />
      <div className="relative z-10 max-w-6xl mx-auto px-6">
        <h2 className="text-5xl md:text-7xl font-extrabold leading-[1.05] max-w-3xl mx-auto">
          Empieza hoy. Sin tarjeta. Sin riesgo.
        </h2>
        <p className="text-lg text-primary-100/80 mt-6 max-w-xl mx-auto">
          7 días gratis. Cancela cuando quieras. Sin tarjeta de crédito durante el trial.
        </p>
        <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            to="/register"
            className="bg-accent-500 text-primary-900 px-8 py-4 rounded-full font-bold text-lg hover:bg-accent-400 transition inline-flex items-center gap-2 justify-center"
          >
            Empezar mi prueba gratis
            <ArrowRight className="h-5 w-5" />
          </Link>
          <a
            href="mailto:soporte@yape-notifier.com"
            className="text-primary-100 hover:text-white px-6 py-4 inline-flex items-center gap-2 justify-center transition"
          >
            Hablar con un humano
            <MessageCircle className="h-5 w-5" />
          </a>
        </div>
      </div>
    </section>
  );
}

// ─── Footer ───────────────────────────────────────────────────────────────────

function Footer() {
  return (
    <footer className="text-primary-200 py-16" style={{ backgroundColor: '#1E0840' }}>
      <div className="max-w-6xl mx-auto px-6">
        <div className="grid md:grid-cols-4 gap-12">
          {/* Brand */}
          <div>
            <p className="text-white font-bold text-2xl">Yape Notifier</p>
            <p className="mt-3 text-sm text-primary-300 max-w-xs">
              El control de tus Yapes, sin riesgos. Una herramienta para negocios que quieren crecer
              sin perder en el camino.
            </p>
          </div>

          {/* Producto */}
          <div>
            <h4 className="text-white font-semibold mb-4 text-sm uppercase tracking-wider">Producto</h4>
            <ul className="space-y-2">
              {[
                { label: 'Características', href: '#producto' },
                { label: 'Precios', href: '#precios' },
                { label: 'Multi-Yape', href: '#multi-yape' },
                { label: 'Programa de Afiliados', href: '#afiliados' },
                { label: 'FAQ', href: '#faq' },
              ].map((l) => (
                <li key={l.label}>
                  <a href={l.href} className="text-sm hover:text-white transition">
                    {l.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Empresa */}
          <div>
            <h4 className="text-white font-semibold mb-4 text-sm uppercase tracking-wider">Empresa</h4>
            <ul className="space-y-2">
              <li><a href="#" className="text-sm hover:text-white transition">Sobre nosotros</a></li>
              <li>
                <a href="mailto:hola@yape-notifier.com" className="text-sm hover:text-white transition">
                  Contacto
                </a>
              </li>
              <li><a href="#" className="text-sm hover:text-white transition">Blog</a></li>
              <li><a href="#" className="text-sm hover:text-white transition">Trabaja con nosotros</a></li>
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h4 className="text-white font-semibold mb-4 text-sm uppercase tracking-wider">Legal</h4>
            <ul className="space-y-2">
              <li><a href="#" className="text-sm hover:text-white transition">Términos y condiciones</a></li>
              <li><a href="#" className="text-sm hover:text-white transition">Política de privacidad</a></li>
              <li><a href="#" className="text-sm hover:text-white transition">Política de cookies</a></li>
            </ul>
          </div>
        </div>

        {/* Bottom row */}
        <div className="mt-12 pt-8 border-t border-primary-700/50 flex flex-col sm:flex-row justify-between items-center gap-4">
          <p className="text-xs text-primary-400">
            © {new Date().getFullYear()} Yape Notifier. Todos los derechos reservados.
          </p>
          <div className="flex items-center gap-4">
            <a href="#" aria-label="Instagram" className="hover:text-white transition">
              <Instagram className="w-5 h-5" />
            </a>
            <a href="#" aria-label="Facebook" className="hover:text-white transition">
              <Facebook className="w-5 h-5" />
            </a>
            <a href="#" aria-label="LinkedIn" className="hover:text-white transition">
              <Linkedin className="w-5 h-5" />
            </a>
          </div>
        </div>

        <p className="mt-6 text-xs text-primary-500 text-center">
          Yape es una marca registrada de BCP. Yape Notifier no está afiliado oficialmente con BCP ni
          con Yape. Esta herramienta opera leyendo notificaciones del sistema operativo Android.
        </p>
      </div>
    </footer>
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
      <MultiYapeSection />
      <LossCalculatorSection />
      <FeaturesBentoSection />
      <DemoSection />
      <PricingSection />
      <AfiliadosSection />
      <TestimoniosSection />
      <FAQSection />
      <CTAFinalSection />
      <Footer />
    </div>
  );
}
