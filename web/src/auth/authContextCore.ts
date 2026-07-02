import { createContext, useContext } from 'react';
import type { JwtPayload, LoginRequest, UserRole } from '../types/api';

export interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: JwtPayload | null;
  error: string | null;
}

export interface AuthContextValue extends AuthState {
  login: (credentials: LoginRequest) => Promise<JwtPayload>;
  logout: () => void;
  hasRole: (role: UserRole | UserRole[]) => boolean;
  tenantId: string | null;
  hcfId: string | null;
  mustChangePassword: boolean;
  updateUserProfile: (updates: Partial<Pick<JwtPayload, 'full_name' | 'profile_photo_url'>>) => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
