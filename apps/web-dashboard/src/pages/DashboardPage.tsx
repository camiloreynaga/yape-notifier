import DashboardTabs, { type TabValue } from '@/components/DashboardTabs';
import NotificationsPage from './NotificationsPage';
import DevicesPage from './DevicesPage';
import MonitoredAppsPage from './MonitoredAppsPage';

export default function DashboardPage() {
  const renderTabContent = (activeTab: TabValue) => {
    switch (activeTab) {
      case 'notifications':
        return <NotificationsPage />;
      case 'devices':
        return <DevicesPage />;
      case 'settings':
        return <MonitoredAppsPage />;
      default:
        return <NotificationsPage />;
    }
  };

  return <DashboardTabs defaultTab="notifications">{renderTabContent}</DashboardTabs>;
}
