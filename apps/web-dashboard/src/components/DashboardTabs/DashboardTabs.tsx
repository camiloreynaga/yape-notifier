/**
 * Componente mejorado de DashboardTabs con accesibilidad, navegación por teclado,
 * badges, animaciones y lazy loading
 */

import { ReactNode, Suspense, useMemo } from 'react';
import clsx from 'clsx';
import { useDashboardTabs } from '@/hooks/useDashboardTabs';
import { useTabBadges } from '@/hooks/useTabBadges';
import { TABS, DEFAULT_TAB } from './DashboardTabs.constants';
import TabBadge from '@/components/TabBadge';
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
  const { activeTab, setActiveTab, handleKeyDown, tabRefs } = useDashboardTabs(defaultTab);
  const badges = useTabBadges();

  // Mapear badges a tabs
  const tabBadges = useMemo(() => {
    const badgeMap: Record<TabValue, typeof badges.notifications> = {
      overview: null,
      notifications: badges.notifications,
      devices: badges.devices,
      settings: badges.settings,
    };
    return badgeMap;
  }, [badges]);

  return (
    <div className="space-y-6">
      {/* Skip link para accesibilidad */}
      <a
        href="#tab-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary-600 focus:text-white focus:rounded-lg"
      >
        Saltar al contenido principal
      </a>

      {/* Tabs Navigation */}
      <div className="border-b border-gray-200" role="tablist" aria-label="Navegación del dashboard">
        <nav
          className="-mb-px flex space-x-4 sm:space-x-8 overflow-x-auto scrollbar-hide"
          aria-label="Tabs"
          style={{
            scrollbarWidth: 'none',
            msOverflowStyle: 'none',
          }}
        >
          {TABS.map((tab, index) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.value;
            const badge = tabBadges[tab.value];

            return (
              <button
                key={tab.value}
                ref={(el) => {
                  tabRefs.current[index] = el;
                }}
                onClick={() => setActiveTab(tab.value)}
                onKeyDown={(e) => handleKeyDown(e, index)}
                className={clsx(
                  'group inline-flex items-center py-3 sm:py-4 px-2 sm:px-1 border-b-2 font-medium text-xs sm:text-sm transition-all duration-300',
                  'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 focus:rounded-t-lg',
                  'relative whitespace-nowrap flex-shrink-0',
                  isActive
                    ? 'border-primary-500 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                )}
                role="tab"
                aria-selected={isActive}
                aria-controls={`tabpanel-${tab.value}`}
                aria-label={tab.ariaLabel}
                id={`tab-${tab.value}`}
                tabIndex={isActive ? 0 : -1}
              >
                <Icon
                  className={clsx(
                    '-ml-0.5 mr-2 h-5 w-5 transition-colors duration-200',
                    isActive ? 'text-primary-500' : 'text-gray-400 group-hover:text-gray-500'
                  )}
                  aria-hidden="true"
                />
                <span>{tab.label}</span>
                <TabBadge badge={badge} />
                {/* Indicador animado de tab activo */}
                {isActive && (
                  <span
                    className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary-500 animate-pulse"
                    aria-hidden="true"
                  />
                )}
              </button>
            );
          })}
        </nav>
      </div>

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

