import apiClient from './client';
import type { LoginRequest, LoginResponse } from '../types/api';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', credentials);
    return response.data;
  },

  // Token refresh (if implemented on backend)
  refresh: async (): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/refresh');
    return response.data;
  },
};
