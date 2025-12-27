// src/services/echo.ts
import Echo from "laravel-echo";
import Pusher from "pusher-js";
import { env } from "@/config/env";
import { logger } from "./logger";

// Declarar tipos globales
declare global {
  interface Window {
    Pusher: typeof Pusher;
    Echo: Echo;
  }
}

// Configurar Pusher como global (requerido por Laravel Echo)
window.Pusher = Pusher;

// Obtener token de autenticación (desde localStorage)
const getAuthToken = (): string => {
  return localStorage.getItem("auth_token") || "";
};

// Verificar si el token es válido (no expirado)
const isTokenValid = (): boolean => {
  const token = getAuthToken();
  if (!token) return false;
  
  try {
    // Decodificar JWT básico (sin verificar firma, solo para verificar expiración)
    const payload = JSON.parse(atob(token.split('.')[1]));
    const exp = payload.exp;
    if (!exp) return true; // Si no tiene exp, asumimos válido
    
    // Verificar si expiró (con margen de 5 minutos)
    const now = Math.floor(Date.now() / 1000);
    return exp > (now + 300);
  } catch {
    // Si no es JWT válido, asumimos válido (puede ser otro formato)
    return true;
  }
};

// Estado de conexión
let connectionState: "connected" | "disconnected" | "connecting" | "error" =
  "disconnected";
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;

// Crear instancia de Echo con configuración profesional
export const echo = new Echo({
  broadcaster: "reverb",
  key: env.REVERB_APP_KEY,
  wsHost: env.REVERB_HOST,
  wsPort: env.REVERB_PORT,
  wssPort: env.REVERB_PORT,
  forceTLS: env.REVERB_SCHEME === "https",
  enabledTransports: ["ws", "wss"],
  authEndpoint: `${env.API_URL}/api/broadcasting/auth`,
  auth: {
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
      Accept: "application/json",
    },
  },
  // Configuración de reconexión automática
  cluster: undefined, // No necesario para Reverb
});

// Manejo de eventos de conexión
if (echo.connector?.pusher?.connection) {
  echo.connector.pusher.connection.bind("connected", () => {
    logger.info("WebSocket conectado", { state: "connected" });
    connectionState = "connected";
    reconnectAttempts = 0;

    // Notificar a listeners
    window.dispatchEvent(new CustomEvent("echo:connected"));
  });

  echo.connector.pusher.connection.bind("disconnected", () => {
    logger.warn("WebSocket desconectado", { state: "disconnected" });
    connectionState = "disconnected";

    // Notificar a listeners
    window.dispatchEvent(new CustomEvent("echo:disconnected"));

    // Verificar token antes de reconectar
    if (!isTokenValid()) {
      logger.error("Token expirado, no se intentará reconexión", {
        action: "token_expired",
      });
      window.dispatchEvent(new CustomEvent("echo:token-expired"));
      return;
    }

    // Intentar reconexión automática
    attemptReconnect();
  });

  echo.connector.pusher.connection.bind("error", (error: Error) => {
    logger.error("Error de WebSocket", error, { state: "error" });
    connectionState = "error";

    // Verificar si es error de autenticación (401/403)
    const errorMessage = error?.message || String(error);
    if (errorMessage.includes("401") || errorMessage.includes("403") || errorMessage.includes("authentication")) {
      logger.error("Error de autenticación en WebSocket", error, {
        action: "auth_error",
      });
      window.dispatchEvent(new CustomEvent("echo:auth-error", { detail: error }));
      return; // No intentar reconectar si es error de auth
    }

    // Notificar a listeners
    window.dispatchEvent(new CustomEvent("echo:error", { detail: error }));

    // Intentar reconexión si no se ha excedido el límite
    if (reconnectAttempts < maxReconnectAttempts) {
      attemptReconnect();
    }
  });
}

// Función de reconexión con backoff exponencial
function attemptReconnect() {
  if (reconnectAttempts >= maxReconnectAttempts) {
    logger.error("Máximo de intentos de reconexión alcanzado", {
      attempts: reconnectAttempts,
      maxAttempts: maxReconnectAttempts,
    });
    window.dispatchEvent(new CustomEvent("echo:max-reconnect-attempts"));
    return;
  }

  // Verificar token antes de reconectar
  if (!isTokenValid()) {
    logger.error("Token expirado, cancelando reconexión", {
      action: "token_expired",
    });
    window.dispatchEvent(new CustomEvent("echo:token-expired"));
    return;
  }

  reconnectAttempts++;
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts - 1), 30000); // Max 30s

  logger.info(`Intentando reconectar en ${delay}ms`, {
    attempt: reconnectAttempts,
    maxAttempts: maxReconnectAttempts,
    delay,
  });

  reconnectTimeout = setTimeout(() => {
    connectionState = "connecting";
    if (echo.connector?.pusher?.connect) {
      echo.connector.pusher.connect();
    }
  }, delay);
}

// Función para resetear intentos de reconexión (útil después de login)
export function resetReconnectAttempts() {
  reconnectAttempts = 0;
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
}

// Función para obtener estado de conexión
export function getConnectionState() {
  return connectionState;
}

// Función para desconectar manualmente
export function disconnect() {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
  echo.disconnect();
}

// Función para actualizar token de autenticación (útil después de login)
export function updateAuthToken(token: string) {
  localStorage.setItem("auth_token", token);
  logger.info("Token de autenticación actualizado", { action: "token_updated" });
  
  // Actualizar headers de autenticación en Echo
  if (echo.connector?.pusher?.config?.auth?.headers) {
    echo.connector.pusher.config.auth.headers.Authorization = `Bearer ${token}`;
  }
  
  // Reconectar con nuevo token
  if (connectionState === "disconnected" || connectionState === "error") {
    resetReconnectAttempts();
    if (echo.connector?.pusher?.connect) {
      logger.info("Reconectando con nuevo token");
      echo.connector.pusher.connect();
    }
  }
}

// Exportar instancia global
window.Echo = echo;

export default echo;

