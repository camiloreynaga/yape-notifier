# Landing Page Yape Notifier — Spec + Plan

**Fecha:** 2026-05-16
**Goal:** Página pública en `/` para captar clientes, con repaint del dashboard a la paleta oficial de Yape (morado + teal).

---

## Decisiones cerradas

| Decisión | Valor |
|---|---|
| Posicionamiento | "Capa de confianza entre dueño y trabajadores" (no productividad) |
| Headline | "Control total de tus Yapes, sin riesgos." |
| Paleta | Yape morado primario + teal accent + emerald solo para "validado" |
| Hero variant | **B — centrado con cards flotantes** |
| Sección estrella | "Un dashboard, todos tus Yapes" (multi-cuenta) |
| Calculadora | **Pérdida actual**, no ahorro (loss aversion) |
| Dashboard | Se repinta al mismo violet/teal (cohesión) |

---

## Paleta — tokens Tailwind

Reemplaza `primary` (Pharmly verde) y `accent` (lima) en [apps/web-dashboard/tailwind.config.js](apps/web-dashboard/tailwind.config.js). `cta` (emerald) se preserva — solo se usa en "validado/cobrado" (semántico, no de marca).

```js
primary: {  // Yape purple
  50:  '#F4EFFB',
  100: '#E5D6F7',
  200: '#CCB1EE',
  300: '#B08AE3',
  400: '#9265D6',
  500: '#7B3FCC',
  600: '#722EA8',  // base brand
  700: '#5B249A',
  800: '#4A1A7A',  // hero gradient bottom
  900: '#2E0F52',
},
accent: {  // Yape teal
  50:  '#E6FAFA',
  100: '#CCF7F7',
  200: '#99F0F0',
  300: '#5EE0E0',
  400: '#3DD9D9',
  500: '#1FD4D4',  // base accent
  600: '#16AEAE',
  700: '#0F8989',
  800: '#0A6868',
  900: '#054040',
},
// cta (emerald) se mantiene: 10B981 / 059669 — usado solo para "validado", "cobrado"
```

**Tipografía** sin cambio: Inter sans + JetBrains Mono.

---

## Routing

Cambio en `apps/web-dashboard/src/App.tsx`:

- Actual: `/` está bajo `<PrivateRoute requireCommerce={true}>` con `<Route index>` que redirige a `/dashboard`.
- Nuevo: `/` se vuelve **pública** (componente `LandingPage`). Si el usuario está autenticado, la nav del landing muestra "Ir al dashboard" en lugar de "Empezar".
- `/dashboard` queda como dashboard autenticado (sin cambio).

---

## Estructura de la página `/` (top → bottom)

1. **Nav** — Logo · Producto · Precios · FAQ · Login · Empezar
2. **Hero** (variante B, centrado) — headline + sub + 2 CTAs + mockup dashboard centrado con cards flotantes ("Pago validado S/49", "🚫 Falso detectado", "Sin comisión", "3 cuentas Yape")
3. **Beneficios** — 3 columnas con iconos: 🔒 Anti-fraude · 👁 Sin acceso a cuenta · 💸 Sin comisión
4. **Problema** — fondo violet oscuro, 3 escenarios:
   - "Le prestas tu celular para que cobren" → robo sistemático
   - "Aceptas capturas como prueba" → capturas falsas
   - "Pagas comisión por cada Yape" → margen erosionado
5. **Solución** — 3 pasos: conecta celular → captadores reciben en SU dispositivo (PIN) → validación automática
6. **★ Multi-Yape (sección estrella)** — "¿Tienes 2, 3 o 5 cuentas Yape? Las controlas todas desde la misma pantalla." 3 mockups de celular → 1 dashboard
7. **Loss calculator** — Sliders interactivos (Yapes/mes, ticket promedio, # trabajadores) → cálculo en vivo de:
   - Pérdida por pagos falsos: `yapes_mes × ticket × 0.02` (2% default)
   - Pérdida por gota a gota: `trabajadores × 100 × 12` (S/ 100/mes default)
   - Total anual perdido + punchline "Recuperas todo por S/ 49/mes"
8. **Features bento** — Multi-dispositivo · Roles · Detección duplicados · Reportes · Exportación · Multi-app
9. **Demo visual** — screenshot real anonimizado del Centro de Validación
10. **Precios** — 2-3 planes con "Tarifa fija mensual. Sin comisión por transacción."
11. **Afiliados (mini)** — "Gana 20% recurrente refiriendo otros comercios" con CTA "Conocer más"
12. **Testimonios** — placeholder reservado (3 cards vacías con texto "Próximamente")
13. **FAQ** — acordeón 6 preguntas: seguro, Android, cancelar, soporte, instalación, multi-Yape
14. **CTA final** — "Empieza hoy. Sin tarjeta. Sin riesgo."
15. **Footer** — Logo · Producto · Empresa · Legal · Contacto

---

## Plan de ejecución (4 tareas)

### Task 1 — Repaint de tokens + estructura de routing

- Actualizar `tailwind.config.js` con la nueva paleta.
- Refactor de routing en `App.tsx`: `/` pasa a ser pública (componente stub `LandingPage`), `/dashboard` queda autenticado.
- Stub mínimo de `LandingPage.tsx` ("Página en construcción") solo para verificar que rutas funcionan.
- Levantar dev server y verificar visualmente que el dashboard renderiza sin romperse con la paleta nueva (los componentes ya usan tokens `primary-*` y `accent-*`).
- Commit + push.

### Task 2 — Construir Hero + Nav + Sections 2-5

- Nav superior (responsive, hamburguesa en mobile).
- Hero variante B con composición: dashboard mockup centrado + cards flotantes alrededor.
- Beneficios row (sección 3).
- Problema (sección 4) — fondo `bg-primary-900` con cards oscuras.
- Solución (sección 5) — 3 pasos con números grandes en `accent-500`.
- Commit.

### Task 3 — Multi-Yape + Loss Calculator + Features

- Multi-Yape section (★): visualización de 3 mockups celular → dashboard único.
- Loss Calculator interactivo: 3 sliders + computación en vivo. Componente con `useState`.
- Features bento grid.
- Demo visual.
- Commit.

### Task 4 — Precios + Afiliados + Testimonios + FAQ + CTA + Footer

- Pricing cards (hardcoded por ahora, no leer de API en este sprint).
- Afiliados mini-card.
- Testimonios placeholder.
- FAQ acordeón.
- CTA final + Footer.
- Smoke completo: levantar dev server, scroll del landing, capturar 3-4 screenshots.
- Commit final + push.

---

## Notas de implementación

- **Stack**: el mismo del dashboard. Sin nuevas dependencias. `lucide-react` para iconos. `tailwindcss` puro.
- **Responsive**: mobile-first. Hero en mobile pasa de "cards flotantes" a stack vertical. Multi-Yape de 3 mockups → 1 mockup + texto.
- **Imágenes**: el dashboard mockup en el hero puede ser un componente React renderizado a escala (no imagen estática) — más mantenible.
- **Performance**: lazy load del landing en `App.tsx` igual que otras páginas. Bundle del landing debe ser independiente del dashboard.
- **Accesibilidad**: alt text en mockups, focus visible en CTAs, sliders con `aria-label`.
- **NO se toca el backend**. Cero endpoints nuevos. Cero migraciones.
