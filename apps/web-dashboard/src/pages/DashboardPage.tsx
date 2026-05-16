import DashboardTabs, { type TabValue } from '@/components/DashboardTabs';
import DashboardOverview from '@/components/DashboardOverview';
import ErrorBoundary from '@/components/ErrorBoundary';
import NotificationsPage from './NotificationsPage';
import DevicesPage from './DevicesPage';
import EmployeesPage from './EmployeesPage';
import DeviceLogsPage from './DeviceLogsPage';
import MonitoredAppsPage from './MonitoredAppsPage';
import ReferralsPage from './ReferralsPage';

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
      case 'employees':
        return (
          <ErrorBoundary>
            <EmployeesPage />
          </ErrorBoundary>
        );
      case 'logs':
        return (
          <ErrorBoundary>
            <DeviceLogsPage />
          </ErrorBoundary>
        );
      case 'settings':
        return (
          <ErrorBoundary>
            <MonitoredAppsPage />
          </ErrorBoundary>
        );
      case 'referrals':
        return (
          <ErrorBoundary>
            <ReferralsPage />
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
