import apiClient from './client';
import type { DashboardMetrics, TrendDataPoint } from '../types/api';

export interface AnalyticsParams {
  start: string; // ISO date
  end: string;
  hcfId?: string;
}

export const analyticsApi = {
  // Dashboard metrics for CBWTF Admin
  getDashboard: async (facilityId: string, params: AnalyticsParams): Promise<DashboardMetrics> => {
    const response = await apiClient.get<DashboardMetrics>(
      `/api/analytics/facility/${facilityId}`,
      { params }
    );
    return response.data;
  },

  // HCF-specific analytics
  getHcfAnalytics: async (hcfId: string, params: AnalyticsParams): Promise<DashboardMetrics> => {
    const response = await apiClient.get<DashboardMetrics>(
      `/api/analytics/hcf/${hcfId}`,
      { params }
    );
    return response.data;
  },

  // Trend data for charts
  getTrends: async (facilityId: string, params: AnalyticsParams): Promise<TrendDataPoint[]> => {
    const response = await apiClient.get<TrendDataPoint[]>(
      `/api/analytics/facility/${facilityId}/trends`,
      { params }
    );
    return response.data;
  },

  // Platform-wide analytics (SUPER_ADMIN only)
  getPlatformAnalytics: async (params: AnalyticsParams): Promise<DashboardMetrics> => {
    const response = await apiClient.get<DashboardMetrics>(
      '/api/admin/analytics/platform',
      { params }
    );
    return response.data;
  },
};
