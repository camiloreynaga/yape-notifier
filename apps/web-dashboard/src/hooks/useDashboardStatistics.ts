/**
 * Hook personalizado para cargar y gestionar estadísticas del dashboard
 */

import { useState, useEffect, useCallback } from 'react';
import { apiService } from '@/services/api';
import type { NotificationStatistics } from '@/types';
import type { PeriodRange } from '@/types/dashboard.types';

export interface UseDashboardStatisticsReturn {
  statistics: NotificationStatistics | null;
  loading: boolean;
  error: Error | null;
  refresh: () => Promise<void>;
  lastUpdated: Date | null;
}

export function useDashboardStatistics(dateRange?: PeriodRange): UseDashboardStatisticsReturn {
  const [statistics, setStatistics] = useState<NotificationStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const loadStatistics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = dateRange
        ? {
            start_date: dateRange.start_date,
            end_date: dateRange.end_date,
          }
        : undefined;
      const data = await apiService.getStatistics(filters);
      setStatistics(data);
      setLastUpdated(new Date());
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Error al cargar estadísticas');
      setError(error);
      console.error('Error loading statistics:', err);
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => {
    loadStatistics();
  }, [loadStatistics]);

  const refresh = useCallback(async () => {
    await loadStatistics();
  }, [loadStatistics]);

  return {
    statistics,
    loading,
    error,
    refresh,
    lastUpdated,
  };
}

