import apiClient from './client';

// Types for Admin API
export interface TenantDTO {
  id: string;
  code: string;
  name: string;
  address: string;
  contactEmail: string | null;
  contactPhone: string | null;
  gpsLat: number | null;
  gpsLon: number | null;
  subscriptionPlan: 'BASIC' | 'PRO' | 'ENTERPRISE' | 'TRIAL';
  subscriptionStatus: 'ACTIVE' | 'TRIAL' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED';
  subscriptionExpiresAt: string | null;
  onboardedAt: string | null;
  hcfCount: number;
  activeUserCount: number;
  features: Record<string, boolean>;
}

export interface OnboardTenantRequest {
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

export interface TenantAuditDTO {
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
  totalTenants: number;
  activeTenants: number;
  trialTenants: number;
  expiredTenants: number;
  suspendedTenants: number;
  totalHcfs: number;
  totalUsers: number;
  totalBagsProcessed: number;
  lastUpdated: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Admin API functions
export const adminApi = {
  // Tenant listing
  listTenants: async (params?: { 
    status?: string; 
    search?: string; 
    page?: number; 
    size?: number 
  }): Promise<PagedResponse<TenantDTO>> => {
    const response = await apiClient.get('/api/admin/tenants', { params });
    return response.data;
  },

  // Get single tenant
  getTenant: async (id: string): Promise<TenantDTO> => {
    const response = await apiClient.get(`/api/admin/tenants/${id}`);
    return response.data;
  },

  // Onboard new tenant
  onboardTenant: async (data: OnboardTenantRequest): Promise<TenantDTO> => {
    const response = await apiClient.post('/api/admin/tenants', data);
    return response.data;
  },

  // Update subscription
  updateSubscription: async (
    id: string, 
    data: UpdateSubscriptionRequest
  ): Promise<TenantDTO> => {
    const response = await apiClient.put(`/api/admin/tenants/${id}/subscription`, data);
    return response.data;
  },

  // Suspend tenant
  suspendTenant: async (id: string, reason: string): Promise<TenantDTO> => {
    const response = await apiClient.post(`/api/admin/tenants/${id}/suspend`, { reason });
    return response.data;
  },

  // Reactivate tenant
  reactivateTenant: async (
    id: string, 
    days: number = 365, 
    notes?: string
  ): Promise<TenantDTO> => {
    const response = await apiClient.post(`/api/admin/tenants/${id}/reactivate`, { 
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
  ): Promise<TenantDTO> => {
    const response = await apiClient.post(`/api/admin/tenants/${id}/temporary-access`, { 
      days, 
      reason 
    });
    return response.data;
  },

  // Feature flags
  getFeatures: async (id: string): Promise<Record<string, boolean>> => {
    const response = await apiClient.get(`/api/admin/tenants/${id}/features`);
    return response.data;
  },

  updateFeatures: async (
    id: string, 
    features: Record<string, boolean>
  ): Promise<Record<string, boolean>> => {
    const response = await apiClient.put(`/api/admin/tenants/${id}/features`, features);
    return response.data;
  },

  // Audit history
  getAuditHistory: async (
    id: string, 
    params?: { page?: number; size?: number }
  ): Promise<PagedResponse<TenantAuditDTO>> => {
    const response = await apiClient.get(`/api/admin/tenants/${id}/audit`, { params });
    return response.data;
  },

  // Platform stats
  getPlatformStats: async (): Promise<PlatformStatsDTO> => {
    const response = await apiClient.get('/api/admin/platform/stats');
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
