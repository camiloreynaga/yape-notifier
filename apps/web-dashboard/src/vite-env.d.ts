/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_REVERB_APP_KEY?: string;
  readonly VITE_REVERB_HOST?: string;
  readonly VITE_REVERB_PORT?: string;
  readonly VITE_REVERB_SCHEME?: string;
  readonly VITE_SENTRY_DSN?: string;
  readonly MODE: string;
  readonly DEV: boolean;
  readonly PROD: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Declaración global para Sentry (cuando se integre)
declare global {
  interface Window {
    Sentry?: {
      captureException: (error: Error, context?: Record<string, unknown>) => void;
      captureMessage: (message: string, level?: string) => void;
    };
  }
}
