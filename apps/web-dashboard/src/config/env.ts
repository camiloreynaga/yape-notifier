// src/config/env.ts
// Validación de variables de entorno
// IMPORTANTE: En producción, las variables deben estar disponibles en build time
// Vite las inyecta en el bundle, no en runtime

const requiredEnvVars = [
  "VITE_API_URL",
  "VITE_REVERB_APP_KEY",
  "VITE_REVERB_HOST",
  "VITE_REVERB_PORT",
] as const;

export function validateEnvVars(): { valid: boolean; missing: string[] } {
  const missing: string[] = [];

  requiredEnvVars.forEach((varName) => {
    if (!import.meta.env[varName]) {
      missing.push(varName);
    }
  });

  return {
    valid: missing.length === 0,
    missing,
  };
}

// Validar al importar (solo en desarrollo)
// En producción, si faltan variables, el build fallará o la app no funcionará
// pero no crasheará la app en runtime
if (import.meta.env.DEV) {
  const validation = validateEnvVars();
  if (!validation.valid) {
    console.warn(
      `⚠️ Faltan variables de entorno requeridas: ${validation.missing.join(
        ", "
      )}\n` + `Por favor, crea un archivo .env con estas variables.`
    );
  }
}

// Exportar variables tipadas
export const env = {
  API_URL:
    import.meta.env.VITE_API_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8000",
  REVERB_APP_KEY: import.meta.env.VITE_REVERB_APP_KEY || "",
  REVERB_HOST: import.meta.env.VITE_REVERB_HOST || window.location.hostname,
  REVERB_PORT: parseInt(import.meta.env.VITE_REVERB_PORT || "8080"),
  REVERB_SCHEME:
    import.meta.env.VITE_REVERB_SCHEME ||
    (window.location.protocol === "https:" ? "https" : "http"),
} as const;
