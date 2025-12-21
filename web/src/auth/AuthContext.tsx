import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { JwtPayload, UserRole, LoginRequest } from '../types/api';
import { authApi } from '../api/auth';
import { tokenStorage, TOKEN_KEY } from '../api/client';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: JwtPayload | null;
  error: string | null;
}

interface AuthContextValue extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  hasRole: (role: UserRole | UserRole[]) => boolean;
  tenantId: string | null;
  hcfId: string | null;
  mustChangePassword: boolean;
  updateUserProfile: (updates: Partial<Pick<JwtPayload, 'full_name' | 'profile_photo_url'>>) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const initialState: AuthState = {
  isAuthenticated: false,
  isLoading: true,
  user: null,
  error: null,
};

// Parse JWT token without library
function parseJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

// Check if token is expired
function isTokenExpired(payload: JwtPayload): boolean {
  const now = Math.floor(Date.now() / 1000);
  return payload.exp < now;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>(initialState);
  const queryClient = useQueryClient();

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
        tokenStorage.remove();
        setState({
          isAuthenticated: false,
          isLoading: false,
          user: null,
          error: null,
        });
      }
    } else {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, []);

  // Multi-tab logout sync: listen for token removal in other tabs
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === TOKEN_KEY && e.newValue === null) {
        // Token removed in another tab → force logout this tab
        queryClient.clear(); // Clear all React Query cache
        setState({
          isAuthenticated: false,
          isLoading: false,
          user: null,
          error: null,
        });
        // Hard redirect guarantees termination of privileged state
        window.location.href = '/login';
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [queryClient]);

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
    tokenStorage.remove();
    queryClient.clear(); // Clear ALL React Query cached data
    setState({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      error: null,
    });
  }, [queryClient]);

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

// Hook to use auth context
export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

