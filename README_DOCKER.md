# 🐳 Docker - Yape Notifier

## 📍 Ubicación

**Toda la configuración Docker está en `infra/docker/`**

```
infra/docker/
├── docker-compose.yml              # ✅ Producción
├── docker-compose.staging.yml     # ✅ Staging
├── deploy.sh                       # ✅ Script producción
├── deploy-staging.sh               # ✅ Script staging
├── dockerfiles/                    # ✅ Dockerfiles
├── nginx/                          # ✅ Configuraciones Nginx
├── caddy/                          # ✅ Configuraciones Caddy
└── README.md                       # ✅ Documentación
```

## 🚀 Inicio Rápido

### Staging

```bash
cd infra/docker
cp .env.staging.example .env.staging
nano .env.staging  # Configurar DB_PASSWORD
chmod +x deploy-staging.sh
./deploy-staging.sh
```

### Producción

```bash
cd infra/docker
cp .env.production.example .env.production
nano .env.production  # Configurar variables
chmod +x deploy.sh
./deploy.sh
```

## 📚 Documentación

- **`docs/DEPLOYMENT.md`** - Guía completa
- **`docs/QUICKSTART.md`** - Inicio rápido
- **`infra/docker/README.md`** - Documentación técnica

## ✅ Arquitectura

```
Caddy (HTTPS) → Nginx API → PHP-FPM (Laravel)
              → Dashboard (React)
              → PostgreSQL (interno)
```

**Todo centralizado en `infra/docker/`** 🎯


