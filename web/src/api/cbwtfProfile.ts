import api from './client';

// ============ CBWTF PROFILE API ============

export interface CbwtfProfile {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  profilePhotoUrl: string | null;
  role: string;
  facilityName: string | null;
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

export const getCbwtfProfile = async (): Promise<CbwtfProfile> => {
  const res = await api.get('/api/cbwtf/profile/me');
  return res.data;
};

export const updateCbwtfProfile = async (data: ProfileUpdateRequest): Promise<CbwtfProfile> => {
  const res = await api.put('/api/cbwtf/profile/me', data);
  return res.data;
};

export const uploadCbwtfPhoto = async (file: File): Promise<{ photoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await api.post('/api/cbwtf/profile/me/photo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const changeCbwtfPassword = async (data: PasswordChangeRequest): Promise<{ message: string }> => {
  const res = await api.post('/api/cbwtf/profile/me/password', data);
  return res.data;
};

export const removeCbwtfPhoto = async (): Promise<{ message: string }> => {
  const res = await api.delete('/api/cbwtf/profile/me/photo');
  return res.data;
};
