import api from './client';

// ============ HCF PROFILE API ============

export interface HcfProfile {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  profilePhotoUrl: string | null;
  role: string;
  hcfName: string | null;
  hcfCode: string | null;
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProfileUpdateRequest {
  fullName?: string;
  email?: string;
  phone?: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ActivityLog {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string | null;
  timestamp: string;
}

export const getHcfProfile = async (): Promise<HcfProfile> => {
  const res = await api.get('/api/hcf/profile/me');
  return res.data;
};

export const updateHcfProfile = async (data: ProfileUpdateRequest): Promise<HcfProfile> => {
  const res = await api.put('/api/hcf/profile/me', data);
  return res.data;
};

export const uploadHcfPhoto = async (file: File): Promise<{ photoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await api.post('/api/hcf/profile/me/photo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const removeHcfPhoto = async (): Promise<{ message: string }> => {
  const res = await api.delete('/api/hcf/profile/me/photo');
  return res.data;
};

export const changeHcfPassword = async (data: PasswordChangeRequest): Promise<{ message: string }> => {
  const res = await api.post('/api/hcf/profile/me/password', data);
  return res.data;
};

export const getHcfActivityLogs = async (limit = 20): Promise<{ logs: ActivityLog[]; total: number }> => {
  const res = await api.get('/api/hcf/profile/me/logs', { params: { limit } });
  return res.data;
};
