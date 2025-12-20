import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiError } from '../types/api';

// API Base URL - configurable via environment
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Token storage keys
const TOKEN_KEY = 'smartcbwtf_token';

// Token management
export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  remove: (): void => localStorage.removeItem(TOKEN_KEY),
};

// Request interceptor - attach JWT token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.get();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    // Handle 401 Unauthorized - redirect to login
    if (error.response?.status === 401) {
      tokenStorage.remove();
      // Only redirect if not already on login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }

    // Handle 503 Service Unavailable - maintenance mode or system disabled
    if (error.response?.status === 503) {
      const responseData = error.response?.data as unknown as Record<string, unknown>;
      if (responseData?.maintenance || responseData?.loginDisabled || responseData?.readonly) {
        // Store maintenance message for display
        sessionStorage.setItem('maintenance_message', responseData.message as string || 'System unavailable');
        sessionStorage.setItem('maintenance_mode', 'true');
        
        // Redirect to login if not already there
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login?maintenance=true';
        }
      }
    }

    // Extract error message
    const message = error.response?.data?.message 
      || error.message 
      || 'An unexpected error occurred';

    return Promise.reject({
      message,
      code: error.response?.data?.code,
      details: error.response?.data?.details,
      status: error.response?.status,
    });
  }
);

export default apiClient;
