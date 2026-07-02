import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiError } from '../types/api';

const DEFAULT_API_BASE_URL = import.meta.env.DEV ? 'http://localhost:8080' : 'https://api.smartcbwtf.com';

// API Base URL - configurable via environment
const resolveApiBaseUrl = (): string => {
  const configuredUrl = (import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL).replace(/\/+$/, '');
  const parsedUrl = new URL(configuredUrl);
  if (import.meta.env.PROD && parsedUrl.protocol !== 'https:') {
    throw new Error('VITE_API_BASE_URL must use HTTPS in production builds.');
  }
  return configuredUrl;
};

export const API_BASE_URL = resolveApiBaseUrl();

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

type ApiClientError = Error & {
  code?: unknown;
  status?: number;
  details?: unknown;
  feature?: unknown;
};

const createApiClientError = (
  message: string,
  metadata: Omit<ApiClientError, keyof Error> = {}
): ApiClientError => Object.assign(new Error(message), metadata);

// Token storage keys
export const TOKEN_KEY = 'smartcbwtf_token';

// Token management
export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  remove: (): void => localStorage.removeItem(TOKEN_KEY),
};

export const saveBlob = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const apiAssetUrl = (pathOrUrl: string | null | undefined, cacheKey?: string | number): string | undefined => {
  if (!pathOrUrl) return undefined;
  let base: string;
  if (pathOrUrl.startsWith('blob:')) {
    base = pathOrUrl;
  } else {
    try {
      const apiOrigin = new URL(API_BASE_URL).origin;
      const resolvedUrl = new URL(pathOrUrl, `${API_BASE_URL.replace(/\/$/, '')}/`);
      if (resolvedUrl.origin !== apiOrigin) {
        return undefined;
      }
      base = resolvedUrl.toString();
    } catch {
      return undefined;
    }
  }
  if (cacheKey === undefined || cacheKey === null || cacheKey === '') {
    return base;
  }
  const separator = base.includes('?') ? '&' : '?';
  return `${base}${separator}t=${encodeURIComponent(String(cacheKey))}`;
};

const isConfiguredApiOrigin = (requestUrl: string | undefined, baseUrl: string | undefined): boolean => {
  if (!requestUrl) return true;
  try {
    const apiOrigin = new URL(API_BASE_URL).origin;
    const resolvedUrl = new URL(requestUrl, `${baseUrl || API_BASE_URL}/`);
    return resolvedUrl.origin === apiOrigin;
  } catch {
    return false;
  }
};

// Request interceptor - attach JWT token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.get();
    if (token && config.headers && isConfiguredApiOrigin(config.url, config.baseURL)) {
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
         return Promise.reject(createApiClientError(msg, {
           code: data?.code || 'INVALID_CREDENTIALS',
           status: 401,
         }));
      }

      // Session expired handling for other requests
      tokenStorage.remove();
      // Only redirect if not already on login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
      return Promise.reject(createApiClientError('Session expired. Please login again.', {
        code: 'UNAUTHORIZED',
        status: 401,
      }));
    }

    // Handle 403 Forbidden - differentiate by error type
    if (status === 403) {
      const errorType = details?.error as string | undefined;
      
      // FEATURE_DISABLED: Let component handle with toast/message
      // DO NOT logout, DO NOT redirect
      if (errorType === 'FEATURE_DISABLED') {
        const msg = (data?.message as string) || 'This feature is not enabled';
        return Promise.reject(createApiClientError(msg, {
          code: 'FEATURE_DISABLED',
          feature: details?.feature,
          status: 403,
        }));
      }
      
      // SUBSCRIPTION_INACTIVE: Hard block - redirect to blocked page
      if (errorType === 'SUBSCRIPTION_INACTIVE' || errorType === 'SUBSCRIPTION_EXPIRED') {
        tokenStorage.remove();
        sessionStorage.setItem('blocked_reason', data?.message as string || 'Subscription inactive');
        window.location.href = '/blocked';
        return Promise.reject(createApiClientError((data?.message as string) || 'Subscription inactive', {
          code: errorType,
          status: 403,
        }));
      }
      
      // Other 403 errors - access denied
      const msg = (data?.message as string) || 'Access denied';
      return Promise.reject(createApiClientError(msg, {
        code: 'ACCESS_DENIED',
        details,
        status: 403,
      }));
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
       return Promise.reject(createApiClientError(msg, { status: 503 }));
    }
    
    // Handle 500 Server Error
    if (status && status >= 500) {
        const msg = 'System error. Please contact support or try again later.';
        return Promise.reject(createApiClientError(msg, { status }));
    }

    // Extract error message for all other errors
    const message = (data?.message as string)
      || error.message 
      || 'An unexpected error occurred';

    return Promise.reject(createApiClientError(message, {
      code: data?.code,
      details: data?.details,
      status,
    }));
  }
);

export default apiClient;
