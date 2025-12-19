import React from 'react';
import { Navigate } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';
import { Block as BlockIcon } from '@mui/icons-material';
import { useAuth } from './AuthContext';
import type { UserRole } from '../types/api';

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
  fallback?: 'redirect' | 'forbidden';
  redirectTo?: string;
}

export const RoleGuard: React.FC<RoleGuardProps> = ({
  children,
  allowedRoles,
  fallback = 'forbidden',
  redirectTo = '/',
}) => {
  const { user, hasRole, logout } = useAuth();

  // Check if user has any of the allowed roles
  if (!hasRole(allowedRoles)) {
    if (fallback === 'redirect') {
      return <Navigate to={redirectTo} replace />;
    }

    // Forbidden UI
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          bgcolor: 'background.default',
          gap: 2,
          p: 3,
          textAlign: 'center',
        }}
      >
        <BlockIcon sx={{ fontSize: 80, color: 'error.main', opacity: 0.8 }} />
        <Typography variant="h4" color="text.primary">
          Access Denied
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 400 }}>
          You don't have permission to access this page. Your current role is{' '}
          <strong>{user?.role}</strong>.
        </Typography>
        <Button variant="outlined" onClick={logout} sx={{ mt: 2 }}>
          Sign Out
        </Button>
      </Box>
    );
  }

  return <>{children}</>;
};

// Role helper constants
export const SUPER_ADMIN_ONLY: UserRole[] = ['SUPER_ADMIN'];
export const CBWTF_ADMIN_ONLY: UserRole[] = ['CBWTF_ADMIN'];
export const HCF_ADMIN_ONLY: UserRole[] = ['HCF_ADMIN'];
export const ADMIN_ROLES: UserRole[] = ['SUPER_ADMIN', 'CBWTF_ADMIN'];
export const ALL_ADMIN_ROLES: UserRole[] = ['SUPER_ADMIN', 'CBWTF_ADMIN', 'HCF_ADMIN'];
