import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import type { JwtPayload, UserRole, LoginRequest } from '../types/api';
import { authApi } from '../api/auth';
import { tokenStorage } from '../api/client';

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
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

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
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isLoading: true,
    user: null,
    error: null,
  });

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

  // Logout handler
  const logout = useCallback(() => {
    tokenStorage.remove();
    setState({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      error: null,
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
    }),
    [state, login, logout, hasRole]
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
