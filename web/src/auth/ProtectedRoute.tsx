import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from './AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

// Routes allowed when password change is required
const PASSWORD_CHANGE_ALLOWED_ROUTES = ['/change-password', '/logout'];

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, isLoading, mustChangePassword } = useAuth();
  const location = useLocation();

  // FLASH FIX: Block ALL rendering while auth is loading
  // This prevents sidebar/content from briefly appearing
  if (isLoading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <CircularProgress size={48} />
      </Box>
    );
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // STRICT LOCK: Force password change if required
  // Only /change-password and /logout are allowed
  if (mustChangePassword) {
    const isAllowedRoute = PASSWORD_CHANGE_ALLOWED_ROUTES.some(
      route => location.pathname.includes(route)
    );
    if (!isAllowedRoute) {
      return <Navigate to="/change-password" replace />;
    }
  }

  return <>{children}</>;
};

