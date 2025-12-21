import apiClient from './client';

// Types for Admin API — CBWTF Management
export interface CBWTFDTO {
  id: string;
  code: string;
  name: string;
  address: string;
  ownerName: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  gpsLat: number | null;
  gpsLon: number | null;
  panNumber: string | null;
  gstNumber: string | null;
  aadharNumber: string | null;
  subscriptionPlan: 'BASIC' | 'PRO' | 'ENTERPRISE' | 'TRIAL';
  subscriptionStatus: 'ACTIVE' | 'TRIAL' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED';
  subscriptionExpiresAt: string | null;
  onboardedAt: string | null;
  hcfCount: number;
  activeUserCount: number;
  features: Record<string, boolean>;
}

export interface OnboardCBWTFRequest {
  code: string;
  name: string;
  address: string;
  contactEmail: string;
  contactPhone?: string;
  gpsLat?: number;
  gpsLon?: number;
  geofenceRadiusM?: number;
  subscriptionPlan: 'BASIC' | 'PRO' | 'ENTERPRISE' | 'TRIAL';
  trialDays?: number;
  adminEmail: string;
  adminName: string;
}

export interface UpdateSubscriptionRequest {
  plan: 'BASIC' | 'PRO' | 'ENTERPRISE' | 'TRIAL';
  expiresAt: string; // ISO date
  notes?: string;
}

export interface CBWTFAuditDTO {
  id: string;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  performedBy: string | null;
  performedByRole: string | null;
  performedAt: string;
  notes: string | null;
}

export interface PlatformStatsDTO {
  totalCBWTFs: number;
  activeCBWTFs: number;
  trialCBWTFs: number;
  expiredCBWTFs: number;
  suspendedCBWTFs: number;
  totalHcfs: number;
  totalUsers: number;
  totalBagsProcessed: number;
  totalRevenue: number;
  pendingErrors: number;
  recentErrors: SystemErrorDTO[];
  lastUpdated: string;
}

export interface SystemErrorDTO {
  id: string;
  timestamp: string;
  severity: 'CRITICAL' | 'ERROR' | 'WARNING' | 'INFO';
  component: string;
  message: string;
  cbwtfCode: string;
  resolved: boolean;
}

// Detailed error for management page
export interface SystemErrorDetailDTO {
  id: string;
  errorCode: string | null;
  severity: 'CRITICAL' | 'ERROR' | 'WARNING' | 'INFO';
  source: 'USER_REPORTED' | 'AUTO_DETECTED' | 'API_ERROR' | 'MOBILE_APP';
  component: string | null;
  cbwtfCode: string | null;
  hcfCode: string | null;
  reportedBy: string | null;
  title: string;
  description: string | null;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'IGNORED';
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  createdAt: string;
}

export interface ErrorStatsDTO {
  open: number;
  inProgress: number;
  critical: number;
  errors: number;
  warnings: number;
}

// System Configuration Types
export interface SystemConfigDTO {
  id: string;
  key: string;
  value: string;
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';
  category: string;
  displayName: string;
  description: string | null;
  isSensitive: boolean;
  requiresConfirmation: boolean;
  isReadonly: boolean;
  validationRules: { min?: number; max?: number } | null;
  version: number;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface ConfigAuditDTO {
  id: string;
  key: string;
  oldValue: string | null;
  newValue: string;
  changedBy: string | null;
  changedAt: string;
  reason: string | null;
  ipAddress: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// User Management Types
export interface UserDTO {
  id: string;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  role: string;
  cbwtfId: string | null;
  cbwtfCode: string | null;
  cbwtfName: string | null;
  hcfId: string | null;
  hcfName: string | null;
  active: boolean;
  forcePasswordChange: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  email: string;
  phone?: string;
  role: string;
  cbwtfId?: string;
  hcfId?: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  phone?: string;
  role?: string;
  cbwtfId?: string;
  hcfId?: string;
  active?: boolean;
}

// Admin API functions
export const adminApi = {
  // ==================== CBWTF Management ====================
  
  // List all CBWTFs
  listCBWTFs: async (params?: { 
    status?: string; 
    search?: string; 
    page?: number; 
    size?: number 
  }): Promise<PagedResponse<CBWTFDTO>> => {
    const response = await apiClient.get('/api/admin/cbwtfs', { params });
    return response.data;
  },

  // Get single CBWTF
  getCBWTF: async (id: string): Promise<CBWTFDTO> => {
    const response = await apiClient.get(`/api/admin/cbwtfs/${id}`);
    return response.data;
  },

  // Onboard new CBWTF
  onboardCBWTF: async (data: OnboardCBWTFRequest): Promise<CBWTFDTO> => {
    const response = await apiClient.post('/api/admin/cbwtfs', data);
    return response.data;
  },

  // Update subscription
  updateSubscription: async (
    id: string, 
    data: UpdateSubscriptionRequest
  ): Promise<CBWTFDTO> => {
    const response = await apiClient.put(`/api/admin/cbwtfs/${id}/subscription`, data);
    return response.data;
  },

  // Suspend CBWTF
  suspendCBWTF: async (id: string, reason: string): Promise<CBWTFDTO> => {
    const response = await apiClient.post(`/api/admin/cbwtfs/${id}/suspend`, { reason });
    return response.data;
  },

  // Reactivate CBWTF
  reactivateCBWTF: async (
    id: string, 
    days: number = 365, 
    notes?: string
  ): Promise<CBWTFDTO> => {
    const response = await apiClient.post(`/api/admin/cbwtfs/${id}/reactivate`, { 
      days, 
      notes 
    });
    return response.data;
  },

  // Grant temporary access
  grantTemporaryAccess: async (
    id: string, 
    days: number, 
    reason: string
  ): Promise<CBWTFDTO> => {
    const response = await apiClient.post(`/api/admin/cbwtfs/${id}/temporary-access`, { 
      days, 
      reason 
    });
    return response.data;
  },

  // Feature flags
  getFeatures: async (id: string): Promise<Record<string, boolean>> => {
    const response = await apiClient.get(`/api/admin/cbwtfs/${id}/features`);
    return response.data;
  },

  updateFeatures: async (
    id: string, 
    features: Record<string, boolean>
  ): Promise<Record<string, boolean>> => {
    const response = await apiClient.put(`/api/admin/cbwtfs/${id}/features`, features);
    return response.data;
  },

  // Audit history
  getAuditHistory: async (
    id: string, 
    params?: { page?: number; size?: number }
  ): Promise<PagedResponse<CBWTFAuditDTO>> => {
    const response = await apiClient.get(`/api/admin/cbwtfs/${id}/audit`, { params });
    return response.data;
  },

  // Platform stats
  getPlatformStats: async (): Promise<PlatformStatsDTO> => {
    const response = await apiClient.get('/api/admin/platform/stats');
    return response.data;
  },

  // ==================== User Management ====================

  // List all users across all CBWTFs
  listUsers: async (params?: {
    cbwtfId?: string;
    role?: string;
    active?: boolean;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<UserDTO>> => {
    const response = await apiClient.get('/api/admin/users', { params });
    return response.data;
  },

  // Get single user
  getUser: async (id: string): Promise<UserDTO> => {
    const response = await apiClient.get(`/api/admin/users/${id}`);
    return response.data;
  },

  // Create new user
  createUser: async (data: CreateUserRequest): Promise<UserDTO> => {
    const response = await apiClient.post('/api/admin/users', data);
    return response.data;
  },

  // Update user
  updateUser: async (id: string, data: UpdateUserRequest): Promise<UserDTO> => {
    const response = await apiClient.put(`/api/admin/users/${id}`, data);
    return response.data;
  },

  // Disable user
  disableUser: async (id: string, reason: string): Promise<UserDTO> => {
    const response = await apiClient.post(`/api/admin/users/${id}/disable`, { reason });
    return response.data;
  },

  // Enable user
  enableUser: async (id: string): Promise<UserDTO> => {
    const response = await apiClient.post(`/api/admin/users/${id}/enable`);
    return response.data;
  },

  // Force password reset
  forcePasswordReset: async (id: string): Promise<UserDTO> => {
    const response = await apiClient.post(`/api/admin/users/${id}/force-password-reset`);
    return response.data;
  },

  // Change user password (SuperAdmin sets new password)
  changeUserPassword: async (id: string, newPassword: string): Promise<UserDTO> => {
    const response = await apiClient.post(`/api/admin/users/${id}/change-password`, { newPassword });
    return response.data;
  },

  // Update CBWTF details
  updateCBWTF: async (id: string, data: {
    name: string;
    address: string;
    ownerName?: string;
    contactEmail?: string;
    contactPhone?: string;
    gpsLat?: number;
    gpsLon?: number;
    geofenceRadiusM?: number;
    panNumber?: string;
    gstNumber?: string;
    aadharNumber?: string;
  }): Promise<CBWTFDTO> => {
    const response = await apiClient.put(`/api/admin/cbwtfs/${id}`, data);
    return response.data;
  },

  // Get CBWTF admin user info
  getCBWTFAdmin: async (id: string): Promise<{ 
    hasAdmin: boolean; 
    id?: string;
    username?: string; 
    email?: string;
    fullName?: string;
    active?: boolean;
    lastLoginAt?: string;
  }> => {
    const response = await apiClient.get(`/api/admin/cbwtfs/${id}/admin`);
    return response.data;
  },

  // Change CBWTF admin credentials
  changeCBWTFCredentials: async (id: string, newUsername: string, newPassword: string): Promise<{ message: string; username: string }> => {
    const response = await apiClient.post(`/api/admin/cbwtfs/${id}/change-credentials`, { newUsername, newPassword });
    return response.data;
  },

  // Revoke all access immediately
  revokeAccess: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/admin/users/${id}/revoke`);
  },

  // ==================== Master Data ====================

  // List HCFs globally
  listMasterHcfs: async (params?: {
    cbwtfId?: string;
    status?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/hcfs', { params });
    return response.data;
  },

  // List Waste Pickups (BagEvents)
  listMasterPickups: async (params?: {
    cbwtfId?: string;
    eventType?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/pickups', { params });
    return response.data;
  },

  // List Waste Bags (BagLabels)
  listMasterBags: async (params?: {
    cbwtfId?: string;
    status?: string;
    category?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/bags', { params });
    return response.data;
  },

  // List QR Labels
  listMasterQrLabels: async (params?: {
    cbwtfId?: string;
    status?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/qr-labels', { params });
    return response.data;
  },

  // List Attendance
  listMasterAttendance: async (params?: {
    cbwtfId?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/attendance', { params });
    return response.data;
  },

  // List Vehicles
  listMasterVehicles: async (params?: {
    cbwtfId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/vehicles', { params });
    return response.data;
  },

  // List Invoices
  listMasterInvoices: async (params?: {
    cbwtfId?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/invoices', { params });
    return response.data;
  },

  // List Payments
  listMasterPayments: async (params?: {
    cbwtfId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/payments', { params });
    return response.data;
  },

  // List Audit Logs
  listMasterAuditLogs: async (params?: {
    cbwtfId?: string;
    action?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Record<string, unknown>>> => {
    const response = await apiClient.get('/api/admin/master-data/audit-logs', { params });
    return response.data;
  },

  // ==================== System Errors ====================

  // List all system errors
  listSystemErrors: async (params?: {
    status?: string;
    severity?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<SystemErrorDetailDTO>> => {
    const response = await apiClient.get('/api/admin/errors', { params });
    return response.data;
  },

  // Get error stats for dashboard
  getErrorStats: async (): Promise<ErrorStatsDTO> => {
    const response = await apiClient.get('/api/admin/errors/stats');
    return response.data;
  },

  // Resolve an error
  resolveError: async (id: string, notes?: string): Promise<SystemErrorDetailDTO> => {
    const response = await apiClient.put(`/api/admin/errors/${id}/resolve`, { notes });
    return response.data;
  },

  // Update error status
  updateErrorStatus: async (id: string, status: string): Promise<SystemErrorDetailDTO> => {
    const response = await apiClient.put(`/api/admin/errors/${id}/status`, { status });
    return response.data;
  },

  // Report an error (any user)
  reportError: async (data: { title: string; description?: string; component?: string; severity?: string }): Promise<{ id: string; message: string }> => {
    const response = await apiClient.post('/api/errors/report', data);
    return response.data;
  },

  // ==================== System Configuration ====================

  // Get all configs grouped by category
  getAllSystemConfigs: async (): Promise<Record<string, SystemConfigDTO[]>> => {
    const response = await apiClient.get('/api/admin/system-config');
    return response.data;
  },

  // Get configs for a specific category
  getSystemConfigsByCategory: async (category: string): Promise<SystemConfigDTO[]> => {
    const response = await apiClient.get(`/api/admin/system-config/category/${category}`);
    return response.data;
  },

  // Update a single config
  updateSystemConfig: async (key: string, value: string, reason?: string): Promise<SystemConfigDTO> => {
    const response = await apiClient.put(`/api/admin/system-config/key/${key}`, { value, reason });
    return response.data;
  },

  // Bulk update configs for a category
  bulkUpdateSystemConfig: async (category: string, updates: Record<string, string>, reason?: string): Promise<{ updatedCount: number; message: string }> => {
    const response = await apiClient.put(`/api/admin/system-config/category/${category}`, { updates, reason });
    return response.data;
  },

  // Get audit history for a config key
  getConfigAuditHistory: async (key: string): Promise<ConfigAuditDTO[]> => {
    const response = await apiClient.get(`/api/admin/system-config/key/${key}/audit`);
    return response.data;
  },

  // Get recent config changes
  getRecentConfigChanges: async (): Promise<ConfigAuditDTO[]> => {
    const response = await apiClient.get('/api/admin/system-config/audit/recent');
    return response.data;
  },

  // Refresh config cache
  refreshConfigCache: async (): Promise<{ message: string }> => {
    const response = await apiClient.post('/api/admin/system-config/refresh-cache');
    return response.data;
  },
};

// Feature flag constants
export const FEATURE_FLAGS = {
  ADVANCED_ANALYTICS: 'ADVANCED_ANALYTICS',
  ROUTE_OPTIMIZATION: 'ROUTE_OPTIMIZATION',
  CPCB_REPORTING: 'CPCB_REPORTING',
  INVOICE_AUTO_SEND: 'INVOICE_AUTO_SEND',
  PAYMENT_GATEWAY: 'PAYMENT_GATEWAY',
  ATTENDANCE_ENFORCEMENT: 'ATTENDANCE_ENFORCEMENT',
  VEHICLE_TRACKING: 'VEHICLE_TRACKING',
  AI_INSIGHTS: 'AI_INSIGHTS',
} as const;

export type FeatureFlag = keyof typeof FEATURE_FLAGS;
