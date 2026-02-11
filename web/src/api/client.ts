import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiError } from '../types/api';

// API Base URL - configurable via environment
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Token storage keys
export const TOKEN_KEY = 'smartcbwtf_token';

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
    const status = error.response?.status;
    const data = error.response?.data as Record<string, unknown> | undefined;
    const details = data?.details as Record<string, unknown> | undefined;
    const requestUrl = error.config?.url || '';
    
    // Handle 401 Unauthorized
    if (status === 401) {
      // If this is a login attempt, pass the backend error message
      if (requestUrl.includes('/auth/login')) {
         const msg = (data?.message as string) || 'Invalid username or password.';
         const err = new Error(msg);
         // Attach extra props if needed
         (err as any).code = data?.code || 'INVALID_CREDENTIALS';
         (err as any).status = 401;
         return Promise.reject(err);
      }

      // Session expired handling for other requests
      tokenStorage.remove();
      // Only redirect if not already on login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
      const sessErr = new Error('Session expired. Please login again.');
      (sessErr as any).code = 'UNAUTHORIZED';
      (sessErr as any).status = 401;
      return Promise.reject(sessErr);
    }

    // Handle 403 Forbidden - differentiate by error type
    if (status === 403) {
      const errorType = details?.error as string | undefined;
      
      // FEATURE_DISABLED: Let component handle with toast/message
      // DO NOT logout, DO NOT redirect
      if (errorType === 'FEATURE_DISABLED') {
        const msg = (data?.message as string) || 'This feature is not enabled';
        const err = new Error(msg);
        (err as any).code = 'FEATURE_DISABLED';
        (err as any).feature = details?.feature;
        (err as any).status = 403;
        return Promise.reject(err);
      }
      
      // SUBSCRIPTION_INACTIVE: Hard block - redirect to blocked page
      if (errorType === 'SUBSCRIPTION_INACTIVE' || errorType === 'SUBSCRIPTION_EXPIRED') {
        tokenStorage.remove();
        sessionStorage.setItem('blocked_reason', data?.message as string || 'Subscription inactive');
        window.location.href = '/blocked';
        const err = new Error((data?.message as string) || 'Subscription inactive');
        (err as any).code = errorType;
        (err as any).status = 403;
        return Promise.reject(err);
      }
      
      // Other 403 errors - access denied
      const msg = (data?.message as string) || 'Access denied';
      const err = new Error(msg);
      (err as any).code = 'ACCESS_DENIED';
      (err as any).details = details;
      (err as any).status = 403;
      return Promise.reject(err);
    }

    // Handle 503 Service Unavailable - maintenance mode or system disabled
    if (status === 503) {
      if (data?.maintenance || data?.loginDisabled || data?.readonly) {
        // Store maintenance message for display
        sessionStorage.setItem('maintenance_message', data.message as string || 'System unavailable');
        sessionStorage.setItem('maintenance_mode', 'true');
        
        // Redirect to login if not already there
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login?maintenance=true';
        }
      }
       const msg = (data?.message as string) || 'Service unavailable. Please try again later.';
       const err = new Error(msg);
       (err as any).status = 503;
       return Promise.reject(err);
    }
    
    // Handle 500 Server Error
    if (status && status >= 500) {
        const msg = 'System error. Please contact support or try again later.';
        const err = new Error(msg);
        (err as any).status = status;
        return Promise.reject(err);
    }

    // Extract error message for all other errors
    const message = (data?.message as string)
      || error.message 
      || 'An unexpected error occurred';

    const fallbackErr = new Error(message);
    (fallbackErr as any).code = data?.code;
    (fallbackErr as any).details = data?.details;
    (fallbackErr as any).status = status;
    return Promise.reject(fallbackErr);
  }
);

export default apiClient;
