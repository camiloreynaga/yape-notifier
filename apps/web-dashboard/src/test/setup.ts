/**
 * Setup file for Vitest
 * This file runs before all tests
 */

import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

// Extend Vitest's expect with jest-dom matchers
expect.extend(matchers);

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock window.matchMedia (used by some components)
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock IntersectionObserver (used by some components)
(window as unknown as { IntersectionObserver: typeof IntersectionObserver }).IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  takeRecords() {
    return [];
  }
  unobserve() {}
} as unknown as typeof IntersectionObserver;

// Mock ResizeObserver (used by Recharts)
(window as unknown as { ResizeObserver: typeof ResizeObserver }).ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
} as unknown as typeof ResizeObserver;

// Mock laravel-echo and pusher-js for tests
// These modules are not installed in the host (only in Docker), so we need to mock them
vi.mock('laravel-echo', () => {
  const mockChannel = {
    listen: vi.fn(),
    stopListening: vi.fn(),
    error: vi.fn(),
  };

  return {
    default: vi.fn(() => ({
      private: vi.fn(() => mockChannel),
      leave: vi.fn(),
      disconnect: vi.fn(),
      connector: {
        pusher: {
          connection: {
            bind: vi.fn(),
            connect: vi.fn(),
          },
        },
      },
    })),
  };
});

vi.mock('pusher-js', () => {
  return {
    default: vi.fn(() => ({
      connection: {
        bind: vi.fn(),
        connect: vi.fn(),
        state: 'connected',
      },
    })),
  };
});

// Suppress console errors in tests (optional, uncomment if needed)
// global.console = {
//   ...console,
//   error: vi.fn(),
//   warn: vi.fn(),
// };

