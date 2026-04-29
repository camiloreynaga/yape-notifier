# Frontend Redesign — Fase 1: Design System + Notificaciones

**Fecha**: 2026-04-29
**Estado**: Pendiente de aprobacion del usuario
**Alcance**: Web dashboard (apps/web-dashboard) — Android queda fuera de scope
**Fases siguientes**: 2 (Resumen), 3 (Dispositivos), 4 (Empleados), 5 (Logs + Settings)

---

## 1. Objetivo

Establecer el sistema de diseno visual de la app (paleta, sidebar, tipografia, densidad) usando como referente Pharmly y aplicarlo al modulo de Notificaciones, que es el flujo mas usado y tiene los pain points mas urgentes (boton de validar enterrado, vista Grid innecesaria, detail page con bug 404).

## 2. Contexto

- App: dashboard de un sistema que recibe notificaciones financieras de billeteras peruanas (Yape, Plin, BCP, etc.)
- Usuarios: dueno del comercio (admin) + captadores (operativos)
- Multi-tenant: cada comercio tiene sus dispositivos, instancias y captadores
- Vista actual: tabs horizontales (Resumen, Notificaciones, Dispositivos, Empleados, Logs, Configuracion)
- Pain points reportados:
  - Vista Grid de notificaciones es decorativa, agrega nada operativo
  - Validar requiere abrir un dropdown y elegir — deberia ser 1 click
  - Click en una notificacion lleva a `/notifications/:id` que da 404
  - KPIs por instancia/captador no existen (importante para usuarios con multiples instancias)
  - Headers gradient gigantes ocupan mucho espacio vertical

## 3. Sistema de diseno (transversal)

### 3.1 Paleta

```
PRIMARY (verde oscuro Pharmly)
  primary-50    #F0F4F1
  primary-100   #DCE6DE
  primary-200   #B8CCBC
  primary-300   #8FAD96
  primary-400   #648E6F
  primary-500   #406E4F
  primary-600   #2A5238
  primary-700   #1F3D2A
  primary-800   #1A2E2A   <- sidebar
  primary-900   #14211F

ACCENT (lime brillante)
  accent-50     #F4FBE3
  accent-100   #E8F5C4
  accent-200   #DAEDA0
  accent-300   #C5E865   <- principal (botones, activos)
  accent-400   #B0D850
  accent-500   #94BC34

STATES
  success      #4CAF50 / bg #E8F5E9
  warning      #F4C430 / bg #FFF8E1
  danger       #E94545 / bg #FDECEC
  info         #4A90E2 / bg #E3F2FD

NEUTRALS
  bg            #F7F7F2 (warm off-white para body)
  surface       #FFFFFF (cards)
  surface-dark  #1A2E2A (sidebar, cards highlighted)
  border        #E5E5E0
  text-primary  #1A1A1A
  text-secondary #6B6B6B
  text-tertiary #A0A0A0
```

### 3.2 Layout: sidebar lateral + content

Cambio estructural mayor. El layout actual es:

```
┌─────────────────────────────────┐
│   Header horizontal              │
├─────────────────────────────────┤
│   Tabs                           │
├─────────────────────────────────┤
│   Content                        │
└─────────────────────────────────┘
```

Cambia a:

```
┌──────┬──────────────────────────┐
│      │  Top bar (search + user) │
│ Side │──────────────────────────│
│ bar  │                          │
│      │  Page content            │
│ dark │                          │
└──────┴──────────────────────────┘
```

**Sidebar (~220px en desktop, colapsable a iconos en mobile):**
- Background `primary-800` con texto blanco
- Logo "Yape Notifier" arriba
- Links: Resumen, Notificaciones, Dispositivos, Empleados, Logs, Configuracion
- Item activo: background `accent-300`, texto `primary-800`
- Item inactivo: texto blanco con opacity 80%, hover blanco 100%
- Footer del sidebar: nombre del comercio + role del user, boton logout

**Top bar:**
- Background blanco
- Search global a la izquierda
- Avatar + nombre del user a la derecha + dropdown con logout
- En super_admin se muestra el badge "SUPER ADMIN"

**Mobile (< 768px):**
- Sidebar se convierte en drawer toggleable (icono hamburguesa en topbar)

### 3.3 Componentes base reusables

**StatCard** (ya existe, refresca):
```
┌──────────────────────────────┐
│  [icon]              ⋮       │
│                              │
│  120                         │
│  ↗ +2%   Pain Relievers      │
└──────────────────────────────┘
```
- Variante `highlighted` con bg `primary-800`, texto blanco, icono lime
- Variante `default` con bg blanco, icono coloreado segun categoria

**StatusBadge:** pill con punto + label
- Validado: bg `success-bg`, texto `success`
- Pendiente: bg `warning-bg`, texto `warning`
- Inconsistente: bg `danger-bg`, texto `danger`

**Button:**
- `primary`: bg `accent-300`, texto `primary-900` (lime button del referente)
- `dark`: bg `primary-800`, texto blanco
- `outline`: border `border`, texto `text-primary`
- `ghost`: sin bg, texto `text-primary`, hover bg `border`
- `danger`: bg `danger`, texto blanco

**Table:**
- Header bg `bg`, texto secundario, peso 600
- Filas blancas con border-bottom `border` ligero
- Hover bg `bg`
- Densidad: 48px row height (no 64+)

### 3.4 Tipografia y espaciado

- Font: Inter (ya en uso) — sin cambios
- H1 page title: 24px, weight 700
- H2 section title: 18px, weight 600
- Body: 14px
- Caption / labels: 12px
- Espaciado vertical entre secciones: 24px
- Padding interno de cards: 16-20px

## 4. Modulo Notificaciones — cambios

### 4.1 Estructura de la pagina

```
[Top bar global ya cubierto por el layout]

┌─────────────────────────────────────────────────────────────┐
│  Notificaciones                            [Exportar]       │
│  Visualiza y valida las notificaciones recibidas            │
├─────────────────────────────────────────────────────────────┤
│  KPIs (4 cards)                                             │
│  Total / Pendientes / Validadas / Inconsistentes            │
├─────────────────────────────────────────────────────────────┤
│  Toolbar:                                                   │
│  [Filtros: Instancia ▼] [Dispositivo ▼] [Periodo ▼]         │
│  [Buscar codigo, monto, pagador]               [Filtros +]  │
├─────────────────────────────────────────────────────────────┤
│  TABLA (lista)                                              │
│  Fecha · Instancia · Dispositivo · Pagador · Monto · Codigo │
│  · Estado · Acciones                                        │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 KPI cards (top de la pagina)

| Card | Titulo | Valor | Subtitulo |
|------|--------|-------|-----------|
| Total | Total | conteo | "del periodo" |
| Pendientes | Pendientes | conteo | "esperan validacion" |
| Validadas | Validadas | conteo | "del periodo" |
| Inconsistentes | Inconsistentes | conteo | "requieren revision" |

Cada card es clickeable y filtra la tabla. La activa muestra ring con `accent-300`.

### 4.3 Toolbar de filtros

| Control | Comportamiento |
|---------|---------------|
| Dropdown Instancia | Lista de instancias del comercio, multi-select |
| Dropdown Dispositivo | Lista de devices del comercio, multi-select |
| Dropdown Periodo | Hoy / Ayer / Ultimos 7 / Ultimos 30 / Este mes / Mes pasado / Personalizado |
| Buscar | Busca en codigo, monto, nombre del pagador, dispositivo |
| Boton "Filtros +" | Abre panel con filtros avanzados (estado, source_app) |

**Persistencia:** filtros se guardan en URL query string (`?instance=12&period=last30&q=ABC`) para compartir/marcar.

### 4.4 Tabla de notificaciones (eliminar Grid, solo Lista)

Columnas:

| Columna | Contenido |
|---------|-----------|
| Fecha | DD/MM hh:mm — formato compacto |
| App | Badge pequeno: Yape (morado), Plin (azul), BCP (rojo), etc. |
| Instancia | Nombre de la instancia (label custom) |
| Dispositivo | Alias del dispositivo o ID corto |
| Pagador | Nombre del pagador (truncado a 18 char con tooltip) |
| Monto | Negrita, formato `S/ 70.00` |
| Codigo | Pill con bg lime claro y texto monoespaciado |
| Estado | StatusBadge (pendiente/validada/inconsistente) |
| Acciones | (ver abajo) |

**Acciones por fila — el cambio mas critico:**

Si la notificacion esta `pendiente`:
```
[✓ Validar] [Ver] [⋮]
```
- `✓ Validar`: boton verde primario que cambia status a `validada` con 1 click
- `Ver`: boton outline pequeno, abre el detail (drawer o pagina, ver 4.5)
- `⋮`: menu con "Marcar inconsistente", "Eliminar"

Si la notificacion esta `validada`:
```
[Ver]
```

Si la notificacion esta `inconsistente`:
```
[Ver] [↶ Revertir]
```

**Densidad:** 56px de altura por fila para que quepan los botones sin apretarse.

**Posible duplicado:** badge sutil amarillo "POSIBLE DUPLICADO" debajo del codigo si hay match exacto (mismo monto + mismo codigo + delta < 60s) en otras filas del periodo.

### 4.5 Detail view: drawer en lugar de pagina

Click en `Ver` → abre drawer lateral derecho (480px desktop, full mobile) con todo el detalle. Reemplaza la actual ruta `/notifications/:id` que da 404.

**Razones:**
- El usuario no pierde el contexto de la lista
- Eliminar la pagina elimina el bug 404 sin tener que arreglarlo
- Consistente con el drawer de comercio en super admin

**Contenido del drawer:**
- Header: monto grande + estado
- Codigo de seguridad en pill grande
- Pagador, dispositivo, instancia, fecha completa
- Body original de la notificacion (texto raw recibido)
- Acciones al fondo: Validar / Marcar inconsistente / Revertir / Eliminar segun estado

### 4.6 Eliminar la ruta `/notifications/:id`

- Quitar la ruta en App.tsx
- Mantener `NotificationDetailPage.tsx` por ahora pero sin referenciar (cleanup en fase posterior)
- Cualquier link viejo a `/notifications/:id` se debe redirigir a `/dashboard?tab=notifications&open=:id` y abrir el drawer automaticamente

### 4.7 Validacion rapida — flujo

1. User ve fila pendiente → click `✓ Validar`
2. Boton muestra spinner + se deshabilita
3. PATCH /api/notifications/{id}/status `{ status: 'validated' }`
4. Toast verde "Notificacion validada" arriba a la derecha
5. La fila refresca: badge cambia a "Validada", botones pasan a solo `[Ver]`
6. KPI "Pendientes" baja en 1, "Validadas" sube en 1

Si falla: toast rojo con error, fila vuelve al estado anterior.

## 5. KPIs por instancia y captador (operativos para multi-tenant)

Aunque el dueno actual no las usa, se incluyen porque otros usuarios las necesitaran. Van en una tabla compacta debajo de los KPIs principales:

```
┌────────────────────────────────────────────────────────┐
│  Operaciones por instancia (periodo seleccionado)      │
├──────────────┬─────┬───────────┬───────────┬───────────┤
│ Instancia    │ Ops │ Validadas │ Pendientes│ Monto S/  │
├──────────────┼─────┼───────────┼───────────┼───────────┤
│ Katty - Yape │ 25  │ 11        │ 14        │ 1,840.00  │
│ Erika - Yape │ 13  │ 5         │ 8         │ 720.00    │
└──────────────┴─────┴───────────┴───────────┴───────────┘
```

Esta tabla se muestra **plegable**: cerrada por defecto, abre con click en "Ver desglose por instancia →".

Mismo concepto se replica en Fase 2 (Resumen) con vista expandida.

## 6. Cambios en backend (minimos)

| Cambio | Razon |
|--------|-------|
| Endpoint `GET /api/notifications/by-instance?period=...` | Para la tabla de KPIs por instancia |
| Filtro `instance_id[]` y `device_id[]` en `GET /api/notifications` | Para los multi-select |
| Nada mas | Los endpoints `show` y `updateStatus` ya existen |

Si el endpoint `show` falla con 404, queda fuera de scope (lo eliminamos del frontend, no se repara).

## 7. Componentes a crear / modificar

### 7.1 Crear nuevos
- `src/components/Layout/AppLayout.tsx` — nuevo layout sidebar + topbar
- `src/components/Layout/Sidebar.tsx`
- `src/components/Layout/TopBar.tsx`
- `src/components/Notifications/NotificationsKpis.tsx` — cards de Total/Pendientes/Validadas/Inconsistentes
- `src/components/Notifications/NotificationsToolbar.tsx` — filtros + busqueda
- `src/components/Notifications/NotificationsTable.tsx` — tabla con acciones inline
- `src/components/Notifications/NotificationDrawer.tsx` — detalle lateral (reemplaza pagina)
- `src/components/Notifications/InstancesBreakdown.tsx` — tabla plegable de KPIs por instancia
- `src/components/UI/Button.tsx` — boton consistente con variantes
- `src/components/UI/Badge.tsx` — badge consistente

### 7.2 Modificar
- `tailwind.config.js` — paleta nueva
- `src/App.tsx` — usar AppLayout, eliminar ruta `/notifications/:id`
- `src/pages/NotificationsPage.tsx` — refactor completo con los nuevos componentes
- `src/components/Layout.tsx` — quedara legacy o se elimina (decidir en implementacion)
- `src/components/CommerceBanner.tsx` — adaptar a paleta nueva

### 7.3 Eliminar (al final, una vez verificado)
- `src/pages/NotificationDetailPage.tsx`
- Ruta `notifications/:id` en App.tsx

## 8. Decisiones de implementacion

- **Migracion de paleta es disruptiva** — todas las clases `primary-600` etc. cambian de color azul a verde oscuro. Componentes existentes pueden verse "raros" hasta que se ajusten en fases posteriores. Aceptable: es lo esperado en un rediseno.
- **No vamos a soportar mobile responsive perfecto en esta fase** — sidebar colapsa, pero la tabla densa se ve apretada. Mobile completo lo dejamos para una fase posterior si hace falta.
- **El drawer de notificaciones reemplaza la pagina** — no arreglamos el endpoint si esta roto, simplemente eliminamos esa pagina.
- **InstancesBreakdown** se incluye aqui porque depende del nuevo endpoint backend; no requiere fase aparte.

## 9. Casos borde

| Caso | Comportamiento |
|------|----------------|
| Sin notificaciones en el periodo | Empty state con ilustracion suave + "No hay notificaciones en este periodo" |
| Validar mientras otra request en flight | Boton deshabilitado, spinner visible |
| Filtro sin resultados | Mensaje "No hay resultados con los filtros actuales" + boton "Limpiar filtros" |
| Drawer abierto + cambio de filtro | Drawer se cierra (refresca lista) |
| Endpoint show 404 | Si llega a llamarse, drawer muestra "Notificacion no disponible" |

## 10. Fuera de alcance (proximas fases)

- Resumen / Dashboard (Fase 2)
- Dispositivos (Fase 3)
- Empleados (Fase 4)
- Logs + Configuracion (Fase 5)
- Mobile responsive completo
- Modo oscuro
- Dark mode toggle
- Personalizacion de paleta por commerce

## 11. Estimacion

~30-40 tareas granulares (similar al super admin panel) ejecutables por subagentes con TDD donde aplique. Total ~2-3 dias de trabajo.
