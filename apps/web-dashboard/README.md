# Yape Notifier - Web Dashboard

Dashboard web para administrar el sistema de notificaciones de pagos Yape & Bank Notification Payment Validator.

## 🚀 Características

- **Autenticación**: Login y registro de usuarios
- **Dashboard Principal**: Estadísticas, gráficos y resumen de pagos
- **Gestión de Notificaciones**: Lista completa con filtros avanzados, paginación y exportación a CSV
- **Gestión de Dispositivos**: CRUD completo para administrar dispositivos Android
- **Interfaz Moderna**: Diseño responsive con Tailwind CSS

## 🛠️ Stack Tecnológico

- **React 18** con **TypeScript**
- **Vite** como build tool
- **React Router** para navegación
- **Axios** para llamadas a la API
- **Tailwind CSS** para estilos
- **Recharts** para gráficos
- **Lucide React** para iconos
- **date-fns** para manejo de fechas

## 📦 Instalación

### Opción 1: Docker (Recomendado)

El dashboard está integrado en el docker-compose del proyecto. Desde la raíz del proyecto:

```bash
# Modo producción (build estático)
cd ../../infra/docker
docker-compose up -d dashboard

# Modo desarrollo (con hot-reload)
docker-compose --profile dev up -d dashboard-dev
```

El dashboard estará disponible en:
- **Producción**: http://localhost:3000
- **Desarrollo**: http://localhost:3001

Ver más detalles en [DOCKER.md](./DOCKER.md)

### Opción 2: Instalación Local

```bash
# Instalar dependencias
npm install

# O con yarn
yarn install

# O con pnpm
pnpm install
```

## 🏃 Desarrollo

### Con Docker

```bash
# Desde infra/docker
docker-compose --profile dev up -d dashboard-dev

# Ver logs
docker-compose logs -f dashboard-dev
```

### Local

```bash
# Iniciar servidor de desarrollo
npm run dev

# El dashboard estará disponible en http://localhost:3000
```

## 📦 Build para Producción

### Con Docker

```bash
# Desde infra/docker
docker-compose build dashboard
docker-compose up -d dashboard
```

### Local

```bash
# Construir para producción
npm run build

# Los archivos estarán en la carpeta dist/
```

## 🔧 Configuración

### Variables de Entorno

En Docker, la URL de la API se configura automáticamente durante el build. Para desarrollo local, crea un archivo `.env` en la raíz del proyecto (opcional):

```env
VITE_API_BASE_URL=http://localhost:8000
```

Si no se especifica, por defecto usará `http://localhost:8000`.

### Configuración en Docker

El dashboard en Docker se conecta a la API usando la URL externa configurada durante el build (`http://localhost:8000/api` por defecto).

## 🎯 Estructura del Proyecto

```
src/
├── components/          # Componentes reutilizables
│   └── Layout.tsx      # Layout principal con navegación
├── contexts/           # Contextos de React
│   └── AuthContext.tsx # Contexto de autenticación
├── pages/              # Páginas de la aplicación
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── NotificationsPage.tsx
│   └── DevicesPage.tsx
├── services/           # Servicios
│   └── api.ts          # Cliente API con Axios
├── config/             # Configuración
│   └── api.ts          # Endpoints y configuración de API
├── types/              # Tipos TypeScript
│   └── index.ts
├── App.tsx             # Componente principal
├── main.tsx            # Punto de entrada
└── index.css           # Estilos globales
```

## 🔐 Autenticación

El dashboard utiliza Laravel Sanctum para autenticación. Los tokens se almacenan en `localStorage` y se incluyen automáticamente en todas las peticiones a la API.

## 📊 Funcionalidades

### Dashboard
- Estadísticas generales (total monto, notificaciones, promedio, duplicados)
- Gráficos de notificaciones por día
- Gráficos por aplicación fuente
- Gráficos de estado de notificaciones
- Tabla resumen por aplicación

### Notificaciones
- Lista paginada de todas las notificaciones
- Filtros avanzados:
  - Por dispositivo
  - Por aplicación fuente
  - Por estado (pendiente, validado, inconsistente)
  - Por rango de fechas
  - Excluir duplicados
- Cambio de estado de notificaciones
- Exportación a CSV

### Dispositivos
- Lista de dispositivos registrados
- Crear nuevo dispositivo
- Editar dispositivo
- Activar/desactivar dispositivo
- Eliminar dispositivo
- Visualización de UUID y última actividad

## 🌐 Integración con API

El dashboard se conecta a la API Laravel en `apps/api`. Asegúrate de que:

1. La API esté corriendo en `http://localhost:8000` (o la URL configurada)
2. CORS esté configurado correctamente en Laravel
3. Las rutas de la API coincidan con las definidas en `src/config/api.ts`

## 📝 Notas

- El dashboard está diseñado para ser responsive y funcionar en dispositivos móviles
- Los gráficos se generan usando Recharts
- La exportación a CSV incluye todos los datos visibles en la tabla actual
- En Docker, el dashboard se construye con la URL de la API configurada en tiempo de build

## 🐛 Troubleshooting

### Error de CORS
Si encuentras errores de CORS, verifica la configuración en `apps/api/config/cors.php` y asegúrate de que el origen del dashboard esté permitido.

### Token expirado
Si el token expira, el usuario será redirigido automáticamente a la página de login.

### API no disponible
Si la API no está disponible, verifica que:
- El servidor Laravel esté corriendo
- La URL en `.env` o `src/config/api.ts` sea correcta
- El puerto no esté bloqueado por firewall

### Problemas en Docker
Ver la guía completa en [DOCKER.md](./DOCKER.md)

## 📄 Licencia

MIT
