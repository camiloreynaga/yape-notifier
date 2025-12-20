/**
 * Componente para filtrar por período
 */

import { useState, useEffect } from 'react';
import { Calendar, CalendarDays, CalendarRange } from 'lucide-react';
import { usePeriodFilter } from '@/hooks/usePeriodFilter';
import type { PeriodFilter } from '@/types/dashboard.types';

export default function PeriodFilter() {
  const { period, setPeriod, setCustomDateRange, dateRange, label } = usePeriodFilter();
  const [customStart, setCustomStart] = useState(dateRange.start_date);
  const [customEnd, setCustomEnd] = useState(dateRange.end_date);

  useEffect(() => {
    if (period === 'custom') {
      setCustomStart(dateRange.start_date);
      setCustomEnd(dateRange.end_date);
    }
  }, [dateRange, period]);

  const handlePeriodChange = (newPeriod: PeriodFilter) => {
    setPeriod(newPeriod);
  };

  const handleCustomDateChange = (e: React.ChangeEvent<HTMLInputElement>, type: 'start' | 'end') => {
    const value = e.target.value;
    
    if (type === 'start') {
      setCustomStart(value);
      setCustomDateRange(value, customEnd || value);
    } else {
      setCustomEnd(value);
      setCustomDateRange(customStart || value, value);
    }
  };

  return (
    <div className="card mb-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">Período de análisis</h2>
          <p className="text-sm text-gray-500 mt-1">Selecciona el rango de fechas para las estadísticas</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => handlePeriodChange('today')}
            className={`btn btn-secondary flex items-center gap-2 ${
              period === 'today' ? 'bg-primary-100 text-primary-700 border-primary-500' : ''
            }`}
            aria-pressed={period === 'today'}
          >
            <Calendar className="h-4 w-4" />
            Hoy
          </button>
          <button
            onClick={() => handlePeriodChange('week')}
            className={`btn btn-secondary flex items-center gap-2 ${
              period === 'week' ? 'bg-primary-100 text-primary-700 border-primary-500' : ''
            }`}
            aria-pressed={period === 'week'}
          >
            <CalendarDays className="h-4 w-4" />
            Esta semana
          </button>
          <button
            onClick={() => handlePeriodChange('month')}
            className={`btn btn-secondary flex items-center gap-2 ${
              period === 'month' ? 'bg-primary-100 text-primary-700 border-primary-500' : ''
            }`}
            aria-pressed={period === 'month'}
          >
            <CalendarRange className="h-4 w-4" />
            Este mes
          </button>
          {period === 'custom' && (
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={customStart}
                onChange={(e) => handleCustomDateChange(e, 'start')}
                className="input text-sm"
                placeholder="Desde"
              />
              <span className="text-gray-500">-</span>
              <input
                type="date"
                value={customEnd}
                onChange={(e) => handleCustomDateChange(e, 'end')}
                className="input text-sm"
                placeholder="Hasta"
              />
            </div>
          )}
          <button
            onClick={() => handlePeriodChange('custom')}
            className={`btn btn-secondary flex items-center gap-2 ${
              period === 'custom' ? 'bg-primary-100 text-primary-700 border-primary-500' : ''
            }`}
            aria-pressed={period === 'custom'}
          >
            <CalendarRange className="h-4 w-4" />
            Personalizado
          </button>
        </div>
      </div>
      {period !== 'custom' && (
        <div className="mt-4 text-sm text-gray-600">
          <span className="font-medium">Período seleccionado:</span> {label}
        </div>
      )}
    </div>
  );
}

