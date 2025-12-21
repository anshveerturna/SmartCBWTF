import api from './client';

// ============ MY PROFILE ============

export interface SuperAdminProfile {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  profilePhotoUrl: string | null;
  role: string;
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

export const getMyProfile = async (): Promise<SuperAdminProfile> => {
  const res = await api.get('/api/superadmin/profile/me');
  return res.data;
};

export const updateMyProfile = async (data: ProfileUpdateRequest): Promise<SuperAdminProfile> => {
  const res = await api.put('/api/superadmin/profile/me', data);
  return res.data;
};

export const uploadMyPhoto = async (file: File): Promise<{ photoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await api.post('/api/superadmin/profile/me/photo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const changePassword = async (data: PasswordChangeRequest): Promise<{ message: string }> => {
  const res = await api.post('/api/superadmin/profile/me/password', data);
  return res.data;
};

export const removeMyPhoto = async (): Promise<{ message: string }> => {
  const res = await api.delete('/api/superadmin/profile/me/photo');
  return res.data;
};

// ============ SUPERADMIN USER MANAGEMENT ============

export interface SuperAdminUser {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  profilePhotoUrl: string | null;
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreateSuperAdminRequest {
  username: string;
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface UpdateSuperAdminRequest {
  fullName?: string;
  email?: string;
  phone?: string;
}

export interface SuperAdminUsersResponse {
  content: SuperAdminUser[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const listSuperAdminUsers = async (params: {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<SuperAdminUsersResponse> => {
  const res = await api.get('/api/superadmin/users', { params });
  return res.data;
};

export const getSuperAdminUser = async (id: string): Promise<SuperAdminUser> => {
  const res = await api.get(`/api/superadmin/users/${id}`);
  return res.data;
};

export const createSuperAdmin = async (data: CreateSuperAdminRequest): Promise<SuperAdminUser> => {
  const res = await api.post('/api/superadmin/users', data);
  return res.data;
};

export const updateSuperAdmin = async (id: string, data: UpdateSuperAdminRequest): Promise<SuperAdminUser> => {
  const res = await api.put(`/api/superadmin/users/${id}`, data);
  return res.data;
};

export const uploadUserPhoto = async (userId: string, file: File): Promise<{ photoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await api.post(`/api/superadmin/users/${userId}/photo`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const disableSuperAdmin = async (id: string): Promise<{ message: string }> => {
  const res = await api.put(`/api/superadmin/users/${id}/disable`);
  return res.data;
};

export const enableSuperAdmin = async (id: string): Promise<{ message: string }> => {
  const res = await api.put(`/api/superadmin/users/${id}/enable`);
  return res.data;
};

export const resetSuperAdminPassword = async (id: string, newPassword?: string): Promise<{
  message: string;
  temporaryPassword: string;
  mustChangeOnLogin: boolean;
}> => {
  const res = await api.post(`/api/superadmin/users/${id}/reset-password`, 
    newPassword ? { newPassword } : {});
  return res.data;
};

// ============ AUDIT LOGS ============

export interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  actorId: string | null;
  actorUsername: string | null;
  actorRole: string | null;
  notes: string | null;
  createdAt: string;
}

export interface AuditLogsResponse {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const getAuditLogs = async (params: {
  entityType?: string;
  action?: string;
  actorId?: string;
  page?: number;
  size?: number;
}): Promise<AuditLogsResponse> => {
  const res = await api.get('/api/superadmin/audit-logs', { params });
  return res.data;
};

export const getSuperAdminAuditLogs = async (params: {
  page?: number;
  size?: number;
}): Promise<AuditLogsResponse> => {
  const res = await api.get('/api/superadmin/audit-logs/superadmin-actions', { params });
  return res.data;
};
