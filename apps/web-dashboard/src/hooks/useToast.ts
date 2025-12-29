/**
 * Hook para mostrar notificaciones toast al usuario
 * Reemplaza el uso de alert() y proporciona una mejor UX
 */

import { useCallback, useState, useEffect } from 'react';
import { logger } from '@/services/logger';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface ToastCallbacks {
  showToast: (message: string, type?: ToastType, duration?: number) => void;
  showSuccess: (message: string, duration?: number) => void;
  showError: (message: string, duration?: number) => void;
  showWarning: (message: string, duration?: number) => void;
  showInfo: (message: string, duration?: number) => void;
}

// Store global para toasts (patrón singleton)
class ToastStore {
  private listeners: Set<(toasts: Toast[]) => void> = new Set();
  private toasts: Toast[] = [];

  subscribe(listener: (toasts: Toast[]) => void) {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify() {
    this.listeners.forEach((listener) => listener([...this.toasts]));
  }

  add(message: string, type: ToastType = 'info', duration = 5000): string {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const toast: Toast = { id, message, type, duration };

    this.toasts = [...this.toasts, toast];
    this.notify();

    // Auto-remove después de la duración
    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }

    return id;
  }

  remove(id: string) {
    this.toasts = this.toasts.filter((toast) => toast.id !== id);
    this.notify();
  }

  clear() {
    this.toasts = [];
    this.notify();
  }

  getToasts(): Toast[] {
    return [...this.toasts];
  }
}

const toastStore = new ToastStore();

/**
 * Hook para mostrar toasts
 */
export function useToast(): ToastCallbacks {
  const showToast = useCallback((message: string, type: ToastType = 'info', duration = 5000) => {
    // Log para debugging
    logger.debug('Toast shown', { message, type, duration });
    
    // Log errores también al logger
    if (type === 'error') {
      logger.error('User-facing error', new Error(message), { source: 'toast' });
    }

    return toastStore.add(message, type, duration);
  }, []);

  const showSuccess = useCallback((message: string, duration = 3000) => {
    return showToast(message, 'success', duration);
  }, [showToast]);

  const showError = useCallback((message: string, duration = 6000) => {
    return showToast(message, 'error', duration);
  }, [showToast]);

  const showWarning = useCallback((message: string, duration = 5000) => {
    return showToast(message, 'warning', duration);
  }, [showToast]);

  const showInfo = useCallback((message: string, duration = 4000) => {
    return showToast(message, 'info', duration);
  }, [showToast]);

  return {
    showToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
  };
}

/**
 * Hook para suscribirse a cambios en los toasts (para componentes de UI)
 */
export function useToastStore() {
  const [toasts, setToasts] = useState<Toast[]>(toastStore.getToasts());

  useEffect(() => {
    const unsubscribe = toastStore.subscribe(setToasts);
    return unsubscribe;
  }, []);

  return {
    toasts,
    removeToast: useCallback((id: string) => {
      toastStore.remove(id);
    }, []),
    clearToasts: useCallback(() => {
      toastStore.clear();
    }, []),
  };
}

