export interface User {
  id: number;
  name: string;
  email: string;
  commerce_id: number | null;
  role: 'super_admin' | 'admin' | 'captador' | 'system';
  pin?: string | null;
  password_visible?: string | null;
  is_active?: boolean;
  created_at: string;
  updated_at: string;
}

export interface Device {
  id: number;
  user_id: number;
  uuid: string;
  name: string;
  alias: string | null;
  platform: string;
  is_active: boolean;
  commerce_id: number | null;
  last_seen_at: string | null;
  battery_level: number | null;
  battery_optimization_disabled: boolean | null;
  notification_permission_enabled: boolean | null;
  // NEW: Real service connection state (not just permission)
  notification_service_connected: boolean | null;
  // NEW: Count of pending notifications in local DB
  pending_notifications_count: number | null;
  // NEW: Timestamp of last captured notification
  last_notification_captured_at: string | null;
  last_heartbeat: string | null;
  created_at: string;
  updated_at: string;
}

export interface AppInstance {
  id: number;
  commerce_id: number;
  device_id: number;
  package_name: string;
  android_user_id: number;
  instance_label: string | null;
  created_at: string;
  updated_at: string;
  device?: Device;
}

export interface Notification {
  id: number;
  user_id: number;
  commerce_id: number | null;
  device_id: number;
  source_app: string;
  package_name: string | null;
  android_user_id: number | null;
  android_uid: number | null;
  app_instance_id: number | null;
  title: string;
  body: string;
  amount: number | null;
  currency: string | null;
  payer_name: string | null;
  security_code: string | null;
  posted_at: string | null;
  received_at: string;
  raw_json: Record<string, unknown> | null;
  status: 'pending' | 'validated' | 'inconsistent';
  is_duplicate: boolean;
  created_at: string;
  updated_at: string;
  device?: Device;
  app_instance?: AppInstance;
}

export interface Commerce {
  id: number;
  name: string;
  owner_user_id: number;
  created_at: string;
  updated_at: string;
}

export interface NotificationFilters {
  device_id?: number | number[];
  source_app?: string;
  package_name?: string;
  app_instance_id?: number;
  instance_id?: number | number[]; // alias accepted by backend
  start_date?: string;
  end_date?: string;
  status?: 'pending' | 'validated' | 'inconsistent';
  exclude_duplicates?: boolean;
  per_page?: number;
  page?: number;
  q?: string;
}

export interface NotificationStatistics {
  total: number;
  total_amount: number;
  by_source_app: Record<string, { count: number; total_amount: number }>;
  by_device: Record<string, { count: number; total_amount: number }>;
  by_date: Record<string, { count: number; total_amount: number }>;
  by_status: Record<string, number>;
  duplicates: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  current_page: number;
  last_page: number;
  per_page: number;
  total: number;
  from: number;
  to: number;
}

export interface AuthResponse {
  message: string;
  user: User;
  token: string;
}

export interface MonitorPackage {
  id: number;
  commerce_id: number;
  package_name: string;
  app_name: string | null;
  description: string | null;
  is_active: boolean;
  enabled_default?: boolean;
  priority: number;
  created_at: string;
  updated_at: string;
}

export interface ApiError {
  message: string;
  errors?: Record<string, string[]>;
}

// Device Logs types
export interface DeviceLog {
  id: number;
  device_id: number;
  commerce_id: number | null;
  category: 'SERVICE' | 'DISCONNECT' | 'RECONNECT' | 'ERROR' | 'HEALTH' | 'SYSTEM';
  level: 'debug' | 'info' | 'warning' | 'error' | 'critical';
  message: string;
  details: string | null;
  device_timestamp: string | null;
  created_at: string;
  device?: {
    id: number;
    name: string;
    alias: string | null;
    uuid: string;
  };
}

export interface DeviceLogsSummary {
  hours: number;
  devices: Array<{
    device: {
      id: number;
      name: string;
      alias: string | null;
      uuid: string;
    };
    counts: Record<string, number>;
    total: number;
    has_disconnects: boolean;
    has_errors: boolean;
  }>;
}

export type ExpiryStatus =
  | 'pending'
  | 'active'
  | 'expiring_soon'
  | 'in_grace'
  | 'expired'
  | 'suspended';

export interface Plan {
  id: number;
  name: string;
  slug: string;
  max_devices: number | null;
  max_notifications_per_day: number | null;
  price: number;
  is_active: boolean;
  commerces_count?: number;
}

export interface CommerceRenewal {
  id: number;
  commerce_id: number;
  plan_id: number;
  renewed_by_user_id: number;
  previous_expires_at: string | null;
  new_expires_at: string;
  amount_paid: number | null;
  notes: string | null;
  created_at: string;
  plan?: Plan;
  renewedBy?: { id: number; name: string };
}

export interface CommerceListItem {
  id: number;
  name: string;
  status: 'pending' | 'active' | 'suspended';
  plan_id: number | null;
  plan_expires_at: string | null;
  expiry_status: ExpiryStatus;
  days_until_expiry: number | null;
  captadores_count: number;
  devices_count: number;
  notifications_count: number;
  owner?: { id: number; name: string; email: string; phone: string | null };
  plan?: Plan;
  approved_at: string | null;
  created_at: string;
}

export interface CommerceDetail extends CommerceListItem {
  captadores: Array<{ id: number; name: string; pin: string | null }>;
  renewals: CommerceRenewal[];
}

export interface SuperAdminKpis {
  total: number;
  pending: number;
  active: number;
  expiring_soon: number;
  in_grace: number;
  expired: number;
  suspended: number;
}

export interface NotificationsByInstanceRow {
  instance_id: number;
  instance_label: string;
  total: number;
  validated: number;
  pending: number;
  inconsistent: number;
  amount_total: string; // backend returns formatted decimal as string
}
