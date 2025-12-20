import DashboardTabs, { type TabValue } from '@/components/DashboardTabs';
import DashboardOverview from '@/components/DashboardOverview';
import ErrorBoundary from '@/components/ErrorBoundary';
import NotificationsPage from './NotificationsPage';
import DevicesPage from './DevicesPage';
import MonitoredAppsPage from './MonitoredAppsPage';

export default function DashboardPage() {
  const renderTabContent = (activeTab: TabValue) => {
    switch (activeTab) {
      case 'overview':
        return (
          <ErrorBoundary>
            <DashboardOverview />
          </ErrorBoundary>
        );
      case 'notifications':
        return (
          <ErrorBoundary>
            <NotificationsPage />
          </ErrorBoundary>
        );
      case 'devices':
        return (
          <ErrorBoundary>
            <DevicesPage />
          </ErrorBoundary>
        );
      case 'settings':
        return (
          <ErrorBoundary>
            <MonitoredAppsPage />
          </ErrorBoundary>
        );
      default:
        return (
          <ErrorBoundary>
            <DashboardOverview />
          </ErrorBoundary>
        );
    }
  };

  return <DashboardTabs defaultTab="overview">{renderTabContent}</DashboardTabs>;
}
