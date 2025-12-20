import { ReactNode } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Bell, Smartphone, Settings } from 'lucide-react';
import clsx from 'clsx';

export type TabValue = 'notifications' | 'devices' | 'settings';

interface Tab {
  value: TabValue;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const tabs: Tab[] = [
  { value: 'notifications', label: 'Notificaciones', icon: Bell },
  { value: 'devices', label: 'Dispositivos', icon: Smartphone },
  { value: 'settings', label: 'Configuración', icon: Settings },
];

interface DashboardTabsProps {
  children: (activeTab: TabValue) => ReactNode;
  defaultTab?: TabValue;
}

export default function DashboardTabs({ children, defaultTab = 'notifications' }: DashboardTabsProps) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabValue) || defaultTab;

  const handleTabChange = (tabValue: TabValue) => {
    navigate(`/dashboard?tab=${tabValue}`, { replace: true });
  };

  return (
    <div className="space-y-6">
      {/* Tabs Navigation */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.value;
            return (
              <button
                key={tab.value}
                onClick={() => handleTabChange(tab.value)}
                className={clsx(
                  'group inline-flex items-center py-4 px-1 border-b-2 font-medium text-sm transition-colors',
                  isActive
                    ? 'border-primary-500 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                )}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon
                  className={clsx(
                    '-ml-0.5 mr-2 h-5 w-5',
                    isActive ? 'text-primary-500' : 'text-gray-400 group-hover:text-gray-500'
                  )}
                />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="tab-content">{children(activeTab)}</div>
    </div>
  );
}

