import apiClient from './client';
import type { Hcf, PaginatedResponse } from '../types/api';

export interface HcfListParams {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

export interface HcfApprovalRequest {
  approved: boolean;
  remarks?: string;
  ratePerBedPerDay?: number;
}

export const hcfApi = {
  // List HCFs (paginated)
  list: async (params: HcfListParams = {}): Promise<PaginatedResponse<Hcf>> => {
    const response = await apiClient.get<PaginatedResponse<Hcf>>('/api/hcfs', { params });
    return response.data;
  },

  // Get pending approvals
  getPending: async (): Promise<Hcf[]> => {
    const response = await apiClient.get<Hcf[]>('/api/hcfs/pending');
    return response.data;
  },

  // Get single HCF by ID
  getById: async (hcfId: string): Promise<Hcf> => {
    const response = await apiClient.get<Hcf>(`/api/hcfs/${hcfId}`);
    return response.data;
  },

  // Approve or reject HCF
  approve: async (hcfId: string, request: HcfApprovalRequest): Promise<Hcf> => {
    const response = await apiClient.post<Hcf>(`/api/hcfs/${hcfId}/approve`, request);
    return response.data;
  },

  // Get HCFs near a location (for drivers)
  getNearest: async (lat: number, lon: number, radiusM: number = 500): Promise<Hcf[]> => {
    const response = await apiClient.get<Hcf[]>('/api/hcfs/nearest', {
      params: { lat, lon, radiusM },
    });
    return response.data;
  },
};
