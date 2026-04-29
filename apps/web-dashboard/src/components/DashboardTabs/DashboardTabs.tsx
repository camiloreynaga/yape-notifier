/**
 * Componente mejorado de DashboardTabs con accesibilidad, navegación por teclado,
 * badges, animaciones y lazy loading
 */

import { ReactNode, Suspense } from 'react';
import { useDashboardTabs } from '@/hooks/useDashboardTabs';
import { DEFAULT_TAB } from './DashboardTabs.constants';
import type { TabValue } from '@/types/dashboard.types';

interface DashboardTabsProps {
  children: (activeTab: TabValue) => ReactNode;
  defaultTab?: TabValue;
}

// Componente de skeleton para loading
function TabContentSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="h-8 bg-gray-200 rounded w-1/4"></div>
      <div className="h-64 bg-gray-200 rounded"></div>
      <div className="h-32 bg-gray-200 rounded"></div>
    </div>
  );
}

export default function DashboardTabs({ children, defaultTab = DEFAULT_TAB }: DashboardTabsProps) {
  // Reads active tab from URL but no longer renders the visual tab buttons —
  // the Sidebar component now handles navigation between tabs.
  const { activeTab } = useDashboardTabs(defaultTab);

  return (
    <div className="space-y-6">
      {/* Skip link para accesibilidad */}
      <a
        href="#tab-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary-600 focus:text-white focus:rounded-lg"
      >
        Saltar al contenido principal
      </a>

      {/*
        Visual horizontal tabs removed — navigation is now handled by the
        Sidebar (Layout/Sidebar.tsx). This component still reads the active
        tab from the URL via useDashboardTabs and renders the right content,
        but the tab buttons themselves are hidden to avoid duplicate nav.
      */}

      {/* Tab Content con animación y lazy loading */}
      <div
        id="tab-content"
        role="tabpanel"
        aria-labelledby={`tab-${activeTab}`}
        className="tab-content"
      >
        <div
          key={activeTab}
          className="animate-fade-in"
          style={{
            animation: 'fadeIn 0.3s ease-in-out',
          }}
        >
          <Suspense fallback={<TabContentSkeleton />}>{children(activeTab)}</Suspense>
        </div>
      </div>

      {/* Region para actualizaciones dinámicas (accesibilidad) */}
      <div
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
        id="dashboard-updates"
      >
        {activeTab === 'overview' && 'Resumen del dashboard cargado'}
      </div>
    </div>
  );
}

