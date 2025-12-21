/// <reference types="vite/client" />
/// <reference types="vitest/globals" />
/// <reference types="@testing-library/jest-dom" />

import type { Notification } from './types';

declare global {
  interface Window {
    addNotificationToast?: (notification: Notification) => void;
  }
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  // more env variables...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
