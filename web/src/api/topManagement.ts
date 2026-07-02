import api from './client';
import type { HcfDetail } from './cbwtf';

export interface PendingHcfApproval {
  id: string;
  name: string;
  code: string;
  address: string;
  contactEmail: string;
  contactPhone: string;
  numberOfBeds?: number;
  monthlyCharges?: number;
  agreementNumber?: string;
  agreementStartDate?: string;
  agreementEndDate?: string;
  requestedAt: string;
}

export const getPendingHcfApprovals = async (): Promise<PendingHcfApproval[]> => {
  const { data } = await api.get('/api/top-mgmt/approvals/hcfs');
  return data;
};

export const getHcfApprovalDetail = async (id: string): Promise<HcfDetail> => {
  const { data } = await api.get(`/api/top-mgmt/approvals/hcfs/${id}`);
  return data;
};

export const downloadHcfApprovalRentAgreement = async (id: string): Promise<Blob> => {
  const { data } = await api.get(`/api/top-mgmt/approvals/hcfs/${id}/rent-agreement`, {
    responseType: 'blob',
  });
  return data;
};

export const approveHcfRegistration = async (id: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/hcfs/${id}/approve`);
};

export const rejectHcfRegistration = async (id: string, reason: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/hcfs/${id}/reject`, { reason });
};

// ==================== CORRECTION REQUESTS ====================

export interface PendingCorrection {
  id: string;
  hcfName: string;
  hcfCode: string;
  doctorName: string;
  address: string;
  contactPhone: string;
  agreementNumber: string;
  fieldName: string;
  currentValue: string;
  requestedValue: string;
  reason: string;
  requestedAt: string;
}

export const getPendingCorrections = async (): Promise<PendingCorrection[]> => {
  const { data } = await api.get('/api/top-mgmt/approvals/corrections');
  return data;
};

export const approveCorrection = async (id: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/corrections/${id}/approve`);
};

export const rejectCorrection = async (id: string, reason: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/corrections/${id}/reject`, { reason });
};
