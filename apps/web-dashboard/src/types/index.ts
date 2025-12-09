export interface User {
  id: number;
  name: string;
  email: string;
  created_at: string;
  updated_at: string;
}

export interface Device {
  id: number;
  user_id: number;
  uuid: string;
  name: string;
  platform: string;
  is_active: boolean;
  last_seen_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface Notification {
  id: number;
  user_id: number;
  device_id: number;
  source_app: string;
  title: string;
  body: string;
  amount: number | null;
  currency: string | null;
  payer_name: string | null;
  received_at: string;
  raw_json: Record<string, unknown> | null;
  status: 'pending' | 'validated' | 'inconsistent';
  is_duplicate: boolean;
  created_at: string;
  updated_at: string;
  device?: Device;
}

export interface NotificationFilters {
  device_id?: number;
  source_app?: string;
  start_date?: string;
  end_date?: string;
  status?: 'pending' | 'validated' | 'inconsistent';
  exclude_duplicates?: boolean;
  per_page?: number;
  page?: number;
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

export interface ApiError {
  message: string;
  errors?: Record<string, string[]>;
}

