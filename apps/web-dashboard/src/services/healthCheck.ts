// src/services/healthCheck.ts
// Health check para verificar estado de servicios

import { apiService } from './api';
import { getConnectionState } from './echo';

export interface HealthStatus {
  api: {
    status: 'ok' | 'error';
    message?: string;
  };
  websocket: {
    status: 'connected' | 'disconnected' | 'error';
    message?: string;
  };
  environment: {
    status: 'ok' | 'warning' | 'error';
    missingVars?: string[];
    message?: string;
  };
  overall: 'healthy' | 'degraded' | 'unhealthy';
}

export async function checkHealth(): Promise<HealthStatus> {
  const health: HealthStatus = {
    api: { status: 'error' },
    websocket: { status: 'disconnected' },
    environment: { status: 'ok' },
    overall: 'unhealthy',
  };

  // 1. Verificar variables de entorno
  const requiredVars = ['VITE_API_URL', 'VITE_REVERB_APP_KEY', 'VITE_REVERB_HOST', 'VITE_REVERB_PORT'];
  const missingVars = requiredVars.filter((varName) => !import.meta.env[varName]);

  if (missingVars.length > 0) {
    health.environment = {
      status: import.meta.env.MODE === 'production' ? 'error' : 'warning',
      missingVars,
      message: `Faltan variables: ${missingVars.join(', ')}`,
    };
  }

  // 2. Verificar API
  try {
    await apiService.getCurrentUser();
    health.api = { status: 'ok' };
  } catch (error) {
    health.api = {
      status: 'error',
      message: error instanceof Error ? error.message : 'Error desconocido',
    };
  }

  // 3. Verificar WebSocket
  const wsState = getConnectionState();
  health.websocket = {
    status: wsState === 'connected' ? 'connected' : wsState === 'error' ? 'error' : 'disconnected',
    message: wsState === 'connected' ? undefined : `Estado: ${wsState}`,
  };

  // 4. Determinar estado general
  if (health.api.status === 'ok' && health.websocket.status === 'connected' && health.environment.status === 'ok') {
    health.overall = 'healthy';
  } else if (health.api.status === 'ok' || health.websocket.status === 'connected') {
    health.overall = 'degraded';
  } else {
    health.overall = 'unhealthy';
  }

  return health;
}

