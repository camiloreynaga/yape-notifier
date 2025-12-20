/**
 * Hook personalizado para manejar filtros de período
 */

import { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { format, startOfDay, endOfDay, startOfWeek, endOfWeek, startOfMonth, endOfMonth, subMonths } from 'date-fns';
import type { PeriodFilter, PeriodRange } from '@/types/dashboard.types';

export interface UsePeriodFilterReturn {
  period: PeriodFilter;
  setPeriod: (period: PeriodFilter) => void;
  dateRange: PeriodRange;
  setCustomDateRange: (start: string, end: string) => void;
  label: string;
}

/**
 * Calcula el rango de fechas para un período dado
 */
function getDateRangeForPeriod(period: PeriodFilter): PeriodRange {
  const now = new Date();

  switch (period) {
    case 'today':
      return {
        start_date: format(startOfDay(now), 'yyyy-MM-dd'),
        end_date: format(endOfDay(now), 'yyyy-MM-dd'),
      };
    case 'week':
      return {
        start_date: format(startOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd'),
        end_date: format(endOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd'),
      };
    case 'month':
      return {
        start_date: format(startOfMonth(now), 'yyyy-MM-dd'),
        end_date: format(endOfMonth(now), 'yyyy-MM-dd'),
      };
    case 'custom':
      // Por defecto, último mes
      return {
        start_date: format(startOfMonth(subMonths(now, 1)), 'yyyy-MM-dd'),
        end_date: format(endOfMonth(now), 'yyyy-MM-dd'),
      };
    default:
      return {
        start_date: format(startOfDay(now), 'yyyy-MM-dd'),
        end_date: format(endOfDay(now), 'yyyy-MM-dd'),
      };
  }
}

/**
 * Obtiene la etiqueta descriptiva del período
 */
function getPeriodLabel(period: PeriodFilter): string {
  switch (period) {
    case 'today':
      return 'Hoy';
    case 'week':
      return 'Esta semana';
    case 'month':
      return 'Este mes';
    case 'custom':
      return 'Personalizado';
    default:
      return 'Hoy';
  }
}

export function usePeriodFilter(): UsePeriodFilterReturn {
  const [searchParams, setSearchParams] = useSearchParams();
  const periodParam = searchParams.get('period') as PeriodFilter | null;
  const startDateParam = searchParams.get('start_date');
  const endDateParam = searchParams.get('end_date');

  const [period, setPeriodState] = useState<PeriodFilter>(
    (periodParam && ['today', 'week', 'month', 'custom'].includes(periodParam)) ? periodParam : 'week'
  );

  const [customDateRange, setCustomDateRangeState] = useState<PeriodRange | null>(
    startDateParam && endDateParam
      ? { start_date: startDateParam, end_date: endDateParam }
      : null
  );

  // Sincronizar con URL params
  useEffect(() => {
    if (periodParam && ['today', 'week', 'month', 'custom'].includes(periodParam)) {
      setPeriodState(periodParam);
    }
  }, [periodParam]);

  useEffect(() => {
    if (startDateParam && endDateParam) {
      setCustomDateRangeState({ start_date: startDateParam, end_date: endDateParam });
    }
  }, [startDateParam, endDateParam]);

  const dateRange = useMemo(() => {
    if (period === 'custom' && customDateRange) {
      return customDateRange;
    }
    return getDateRangeForPeriod(period);
  }, [period, customDateRange]);

  const setPeriod = (newPeriod: PeriodFilter) => {
    setPeriodState(newPeriod);
    const params = new URLSearchParams(searchParams);
    params.set('period', newPeriod);

    if (newPeriod === 'custom' && customDateRange) {
      params.set('start_date', customDateRange.start_date);
      params.set('end_date', customDateRange.end_date);
    } else {
      params.delete('start_date');
      params.delete('end_date');
    }

    setSearchParams(params, { replace: true });
  };

  const setCustomDateRange = (start: string, end: string) => {
    const range = { start_date: start, end_date: end };
    setCustomDateRangeState(range);
    const params = new URLSearchParams(searchParams);
    params.set('period', 'custom');
    params.set('start_date', start);
    params.set('end_date', end);
    setSearchParams(params, { replace: true });
  };

  return {
    period,
    setPeriod,
    dateRange,
    setCustomDateRange,
    label: getPeriodLabel(period),
  };
}

