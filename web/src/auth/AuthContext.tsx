import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { JwtPayload, LoginRequest, UserRole } from '../types/api';
import { authApi } from '../api/auth';
import { tokenStorage, TOKEN_KEY } from '../api/client';
import { AuthContext, type AuthContextValue, type AuthState } from './authContextCore';

const initialState: AuthState = {
  isAuthenticated: false,
  isLoading: true,
  user: null,
  error: null,
};

const VALID_ROLES: readonly UserRole[] = [
  'SUPER_ADMIN',
  'CBWTF_ADMIN',
  'HCF_ADMIN',
  'TOP_MANAGEMENT',
  'DRIVER',
  'PLANT_OPERATOR',
];

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

const isValidRole = (value: unknown): value is UserRole =>
  typeof value === 'string' && VALID_ROLES.includes(value as UserRole);

const nullableString = (value: unknown): string | null =>
  typeof value === 'string' ? value : null;

function toJwtPayload(value: unknown): JwtPayload | null {
  if (!isRecord(value)) return null;
  if (typeof value.sub !== 'string' || value.sub.trim() === '') return null;
  if (!isValidRole(value.role)) return null;
  if (typeof value.iat !== 'number' || !Number.isFinite(value.iat)) return null;
  if (typeof value.exp !== 'number' || !Number.isFinite(value.exp)) return null;
  if (typeof value.must_change_password !== 'undefined' && typeof value.must_change_password !== 'boolean') {
    return null;
  }

  return {
    sub: value.sub,
    role: value.role,
    full_name: nullableString(value.full_name),
    profile_photo_url: nullableString(value.profile_photo_url),
    tenant_id: nullableString(value.tenant_id),
    hcf_id: nullableString(value.hcf_id),
    must_change_password: value.must_change_password,
    iat: value.iat,
    exp: value.exp,
  };
}

// Parse and validate JWT payload without trusting localStorage blindly.
function parseJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return toJwtPayload(JSON.parse(jsonPayload));
  } catch {
    return null;
  }
}

// Check if token is expired
function isTokenExpired(payload: JwtPayload): boolean {
  const now = Math.floor(Date.now() / 1000);
  return payload.exp <= now;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>(initialState);
  const queryClient = useQueryClient();

  const clearAuthenticatedState = useCallback(() => {
    tokenStorage.remove();
    queryClient.clear();
    setState({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      error: null,
    });
  }, [queryClient]);

  // Initialize auth state from stored token
  useEffect(() => {
    const token = tokenStorage.get();
    if (token) {
      const payload = parseJwt(token);
      if (payload && !isTokenExpired(payload)) {
        setState({
          isAuthenticated: true,
          isLoading: false,
          user: payload,
          error: null,
        });
      } else {
        clearAuthenticatedState();
      }
    } else {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, [clearAuthenticatedState]);

  // Multi-tab logout sync: listen for token removal in other tabs
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === TOKEN_KEY && e.newValue === null) {
        // Token removed in another tab → force logout this tab
        clearAuthenticatedState();
        // Hard redirect guarantees termination of privileged state
        window.location.href = '/login';
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [clearAuthenticatedState]);

  // Enforce session timeout client-side as soon as JWT expires.
  useEffect(() => {
    if (!state.user) return;
    const expiresAtMs = state.user.exp * 1000;
    const delayMs = expiresAtMs - Date.now();
    if (delayMs <= 0) {
      clearAuthenticatedState();
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
      return;
    }
    const timerId = window.setTimeout(() => {
      clearAuthenticatedState();
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }, delayMs);
    return () => window.clearTimeout(timerId);
  }, [state.user, clearAuthenticatedState]);

  // Login handler
  const login = useCallback(async (credentials: LoginRequest) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    try {
      const response = await authApi.login(credentials);
      const payload = parseJwt(response.accessToken);
      
      if (!payload) {
        throw new Error('Invalid token received');
      }

      tokenStorage.set(response.accessToken);
      setState({
        isAuthenticated: true,
        isLoading: false,
        user: payload,
        error: null,
      });
      return payload;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Login failed';
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: message,
      }));
      throw err;
    }
  }, []);

  // Logout handler - clears everything
  const logout = useCallback(() => {
    clearAuthenticatedState();
  }, [clearAuthenticatedState]);

  // Update user profile without re-login (for photo and name changes)
  const updateUserProfile = useCallback((updates: Partial<Pick<JwtPayload, 'full_name' | 'profile_photo_url'>>) => {
    setState((prev) => {
      if (!prev.user) return prev;
      return {
        ...prev,
        user: { ...prev.user, ...updates },
      };
    });
  }, []);

  // Role check helper
  const hasRole = useCallback(
    (role: UserRole | UserRole[]): boolean => {
      if (!state.user) return false;
      const roles = Array.isArray(role) ? role : [role];
      return roles.includes(state.user.role);
    },
    [state.user]
  );

  // Memoize context value
  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      logout,
      hasRole,
      tenantId: state.user?.tenant_id ?? null,
      hcfId: state.user?.hcf_id ?? null,
      mustChangePassword: state.user?.must_change_password ?? false,
      updateUserProfile,
    }),
    [state, login, logout, hasRole, updateUserProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
