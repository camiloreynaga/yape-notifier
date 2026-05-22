/**
 * Tests for usePeriodFilter hook
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { usePeriodFilter } from './usePeriodFilter';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';

// Wrapper para el hook con router
function Wrapper({ children }: { children: ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>;
}

describe('usePeriodFilter', () => {
  beforeEach(() => {
    // Reset URL params before each test
    window.history.replaceState({}, '', '/dashboard');
  });

  it('returns default period when no URL param', () => {
    const { result } = renderHook(() => usePeriodFilter(), {
      wrapper: Wrapper,
    });

    expect(result.current.period).toBe('week');
    expect(result.current.label).toBe('Esta semana');
  });

  it('can set period programmatically', async () => {
    const { result } = renderHook(() => usePeriodFilter(), {
      wrapper: Wrapper,
    });

    expect(result.current.period).toBe('week');

    result.current.setPeriod('today');

    await waitFor(() => {
      expect(result.current.period).toBe('today');
      expect(result.current.label).toBe('Hoy');
    });
  });

  it('provides date range', () => {
    const { result } = renderHook(() => usePeriodFilter(), {
      wrapper: Wrapper,
    });

    expect(result.current.dateRange).toHaveProperty('start_date');
    expect(result.current.dateRange).toHaveProperty('end_date');
    expect(result.current.dateRange.start_date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(result.current.dateRange.end_date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('updates period when setPeriod is called', async () => {
    const { result } = renderHook(() => usePeriodFilter(), {
      wrapper: Wrapper,
    });

    expect(result.current.period).toBe('week');

    result.current.setPeriod('month');

    await waitFor(() => {
      expect(result.current.period).toBe('month');
      expect(result.current.label).toBe('Este mes');
    });
  });
});

