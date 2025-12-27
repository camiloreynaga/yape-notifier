// src/services/logger.ts
// Servicio de logging estructurado para producción

// Declarar tipo para Sentry (opcional, solo si está configurado)
declare global {
  interface Window {
    Sentry?: {
      captureException: (error: Error, options?: { extra?: LogContext }) => void;
      captureMessage: (message: string, level?: string) => void;
    };
  }
}

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogContext {
  [key: string]: unknown;
}

class Logger {
  private isDevelopment = import.meta.env.DEV;
  private isProduction = import.meta.env.MODE === 'production';

  private log(level: LogLevel, message: string, context?: LogContext) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message,
      ...context,
    };

    // En producción, solo loggear errores y warnings
    if (this.isProduction && (level === 'debug' || level === 'info')) {
      return;
    }

    // En desarrollo, usar console con colores
    if (this.isDevelopment) {
      const colors = {
        debug: 'color: #6B7280',
        info: 'color: #3B82F6',
        warn: 'color: #F59E0B',
        error: 'color: #EF4444',
      };
      // eslint-disable-next-line no-console
      console[level === 'debug' ? 'log' : level](
        `%c[${level.toUpperCase()}] ${message}`,
        colors[level],
        context || ''
      );
      return;
    }

    // En producción, estructurar logs para sistemas de logging
    // Aquí puedes integrar con Sentry, LogRocket, etc.
    if (level === 'error') {
      // Enviar a error tracking si está configurado
      this.sendToErrorTracking(message, context);
    }

    // Log estructurado para sistemas de logging (ELK, CloudWatch, etc.)
    // eslint-disable-next-line no-console
    console[level](JSON.stringify(logEntry));
  }

  private sendToErrorTracking(message: string, context?: LogContext) {
    // Placeholder para integración con Sentry u otro servicio
    // Se implementará cuando se configure error tracking
    if (window.Sentry) {
      window.Sentry.captureException(new Error(message), {
        extra: context,
      });
    }
  }

  debug(message: string, context?: LogContext) {
    this.log('debug', message, context);
  }

  info(message: string, context?: LogContext) {
    this.log('info', message, context);
  }

  warn(message: string, context?: LogContext) {
    this.log('warn', message, context);
  }

  error(message: string, error?: Error | unknown, context?: LogContext) {
    const errorMessage = error instanceof Error ? error.message : String(error || message);
    const errorContext = {
      ...context,
      error: error instanceof Error ? {
        message: error.message,
        stack: error.stack,
        name: error.name,
      } : error,
    };
    this.log('error', errorMessage, errorContext);
  }
}

export const logger = new Logger();

