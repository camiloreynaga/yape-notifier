/**
 * Hook personalizado para obtener badges de tabs (contadores)
 */

import { useState, useEffect } from 'react';
import { apiService } from '@/services/api';
import type { TabBadge } from '@/types/dashboard.types';

export interface TabBadges {
  notifications: TabBadge | null;
  devices: TabBadge | null;
  settings: TabBadge | null;
}

export function useTabBadges(): TabBadges {
  const [badges, setBadges] = useState<TabBadges>({
    notifications: null,
    devices: null,
    settings: null,
  });

  useEffect(() => {
    const loadBadges = async () => {
      try {
        // Cargar notificaciones pendientes
        const notificationsData = await apiService.getNotifications({
          status: 'pending',
          per_page: 1,
        });
        const pendingCount = notificationsData.total || 0;

        // Cargar dispositivos offline
        const devices = await apiService.getDevices();
        const offlineCount = devices.filter((device) => {
          if (!device.last_heartbeat) return true;
          const heartbeatTime = new Date(device.last_heartbeat).getTime();
          const now = Date.now();
          const diffMinutes = (now - heartbeatTime) / (1000 * 60);
          return diffMinutes >= 5;
        }).length;

        setBadges({
          notifications: pendingCount > 0 ? { count: pendingCount, variant: 'warning' } : null,
          devices: offlineCount > 0 ? { count: offlineCount, variant: 'danger' } : null,
          settings: null,
        });
      } catch (error) {
        console.error('Error loading tab badges:', error);
      }
    };

    loadBadges();
    // Actualizar badges cada 30 segundos
    const interval = setInterval(loadBadges, 30000);
    return () => clearInterval(interval);
  }, []);

  return badges;
}

