import api from './client';

export interface PendingHcfApproval {
  id: string;
  name: string;
  code: string;
  address: string;
  contactEmail: string;
  contactPhone: string;
  numberOfBeds?: number;
  monthlyCharges?: number;
  requestedAt: string;
}

export const getPendingHcfApprovals = async (): Promise<PendingHcfApproval[]> => {
  const { data } = await api.get('/api/top-mgmt/approvals/hcfs');
  return data;
};

export const approveHcfRegistration = async (id: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/hcfs/${id}/approve`);
};

export const rejectHcfRegistration = async (id: string, reason: string): Promise<void> => {
  await api.post(`/api/top-mgmt/approvals/hcfs/${id}/reject`, { reason });
};
