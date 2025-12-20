/**
 * Hook personalizado para manejar la lógica de tabs del dashboard
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { TabValue } from '@/types/dashboard.types';
import { DEFAULT_TAB, TAB_KEYS } from '@/components/DashboardTabs/DashboardTabs.constants';

export interface UseDashboardTabsReturn {
  activeTab: TabValue;
  setActiveTab: (tab: TabValue) => void;
  handleKeyDown: (event: React.KeyboardEvent<HTMLButtonElement>, tabIndex: number) => void;
  tabRefs: React.MutableRefObject<(HTMLButtonElement | null)[]>;
}

/**
 * Valida si un string es un TabValue válido
 */
function isValidTab(tab: string | null): tab is TabValue {
  return tab === 'overview' || tab === 'notifications' || tab === 'devices' || tab === 'settings';
}

export function useDashboardTabs(defaultTab: TabValue = DEFAULT_TAB): UseDashboardTabsReturn {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const tabFromUrl = searchParams.get('tab');
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const [activeTab, setActiveTabState] = useState<TabValue>(() => {
    return isValidTab(tabFromUrl) ? tabFromUrl : defaultTab;
  });

  // Sincronizar con URL
  useEffect(() => {
    if (isValidTab(tabFromUrl)) {
      setActiveTabState(tabFromUrl);
    }
  }, [tabFromUrl]);

  const setActiveTab = useCallback(
    (tab: TabValue) => {
      setActiveTabState(tab);
      const params = new URLSearchParams(searchParams);
      params.set('tab', tab);
      navigate(`/dashboard?${params.toString()}`, { replace: true });
    },
    [navigate, searchParams]
  );

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLButtonElement>, tabIndex: number) => {
      const tabs: TabValue[] = ['overview', 'notifications', 'devices', 'settings'];
      let newIndex = tabIndex;

      switch (event.key) {
        case TAB_KEYS.ARROW_LEFT:
          event.preventDefault();
          newIndex = tabIndex > 0 ? tabIndex - 1 : tabs.length - 1;
          break;
        case TAB_KEYS.ARROW_RIGHT:
          event.preventDefault();
          newIndex = tabIndex < tabs.length - 1 ? tabIndex + 1 : 0;
          break;
        case TAB_KEYS.HOME:
          event.preventDefault();
          newIndex = 0;
          break;
        case TAB_KEYS.END:
          event.preventDefault();
          newIndex = tabs.length - 1;
          break;
        case TAB_KEYS.ENTER:
        case TAB_KEYS.SPACE:
          event.preventDefault();
          setActiveTab(tabs[tabIndex]);
          return;
        default:
          return;
      }

      if (newIndex !== tabIndex) {
        setActiveTab(tabs[newIndex]);
        // Focus en el nuevo tab después de un breve delay para asegurar que el DOM se actualizó
        setTimeout(() => {
          tabRefs.current[newIndex]?.focus();
        }, 0);
      }
    },
    [setActiveTab]
  );

  return {
    activeTab,
    setActiveTab,
    handleKeyDown,
    tabRefs,
  };
}

