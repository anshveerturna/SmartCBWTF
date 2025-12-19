import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline, Box, CircularProgress } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import theme from './theme';
import { AuthProvider, ProtectedRoute, RoleGuard, SUPER_ADMIN_ONLY, CBWTF_ADMIN_ONLY, HCF_ADMIN_ONLY } from './auth';
import { DashboardShell } from './components/layout';

// Lazy-loaded pages
const Login = lazy(() => import('./pages/Login'));
const SuperAdminDashboard = lazy(() => import('./pages/superadmin/Dashboard'));
const CbwtfDashboard = lazy(() => import('./pages/cbwtf/Dashboard'));
const HcfDashboard = lazy(() => import('./pages/hcf/Dashboard'));

// Loading fallback
const PageLoader: React.FC = () => (
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

// Query client configuration
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthProvider>
          <BrowserRouter>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/login" element={<Login />} />

                {/* SuperAdmin Routes */}
                <Route
                  path="/admin"
                  element={
                    <ProtectedRoute>
                      <RoleGuard allowedRoles={SUPER_ADMIN_ONLY}>
                        <DashboardShell />
                      </RoleGuard>
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="dashboard" replace />} />
                  <Route path="dashboard" element={<SuperAdminDashboard />} />
                  <Route path="tenants" element={<div>Tenant Management (Coming Soon)</div>} />
                  <Route path="analytics" element={<div>Platform Analytics (Coming Soon)</div>} />
                  <Route path="settings" element={<div>System Settings (Coming Soon)</div>} />
                </Route>

                {/* CBWTF Admin Routes */}
                <Route
                  path="/cbwtf"
                  element={
                    <ProtectedRoute>
                      <RoleGuard allowedRoles={CBWTF_ADMIN_ONLY}>
                        <DashboardShell />
                      </RoleGuard>
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="dashboard" replace />} />
                  <Route path="dashboard" element={<CbwtfDashboard />} />
                  <Route path="hcfs" element={<div>HCF Management (Coming Soon)</div>} />
                  <Route path="labels" element={<div>QR Labels (Coming Soon)</div>} />
                  <Route path="operations" element={<div>Waste Operations (Coming Soon)</div>} />
                  <Route path="billing" element={<div>Billing & Invoicing (Coming Soon)</div>} />
                  <Route path="alerts" element={<div>Alerts (Coming Soon)</div>} />
                  <Route path="settings" element={<div>Settings (Coming Soon)</div>} />
                </Route>

                {/* HCF Admin Routes */}
                <Route
                  path="/hcf"
                  element={
                    <ProtectedRoute>
                      <RoleGuard allowedRoles={HCF_ADMIN_ONLY}>
                        <DashboardShell />
                      </RoleGuard>
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="dashboard" replace />} />
                  <Route path="dashboard" element={<HcfDashboard />} />
                  <Route path="pickups" element={<div>Pickup History (Coming Soon)</div>} />
                  <Route path="invoices" element={<div>Invoices (Coming Soon)</div>} />
                  <Route path="agreement" element={<div>Agreement (Coming Soon)</div>} />
                </Route>

                {/* Default redirect */}
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
