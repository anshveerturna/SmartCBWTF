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
const CBWTFManagement = lazy(() => import('./pages/superadmin/CBWTFManagement'));
const OnboardCBWTF = lazy(() => import('./pages/superadmin/OnboardCBWTF'));
const CBWTFDetail = lazy(() => import('./pages/superadmin/CBWTFDetail'));
const AllUsers = lazy(() => import('./pages/superadmin/users/AllUsers'));
const CreateUser = lazy(() => import('./pages/superadmin/users/CreateUser'));
const UserDetail = lazy(() => import('./pages/superadmin/users/UserDetail'));
// Master Data pages
const MasterHcfs = lazy(() => import('./pages/superadmin/master/MasterHcfs'));
const MasterPickups = lazy(() => import('./pages/superadmin/master/MasterPickups'));
const MasterBags = lazy(() => import('./pages/superadmin/master/MasterBags'));
const MasterQrLabels = lazy(() => import('./pages/superadmin/master/MasterQrLabels'));
const MasterAttendance = lazy(() => import('./pages/superadmin/master/MasterAttendance'));
const MasterVehicles = lazy(() => import('./pages/superadmin/master/MasterVehicles'));
const MasterInvoices = lazy(() => import('./pages/superadmin/master/MasterInvoices'));
const MasterPayments = lazy(() => import('./pages/superadmin/master/MasterPayments'));
const MasterAuditLogs = lazy(() => import('./pages/superadmin/master/MasterAuditLogs'));
const PaymentGateway = lazy(() => import('./pages/superadmin/PaymentGateway'));
const SystemConfig = lazy(() => import('./pages/superadmin/SystemConfig'));
const ProfilePage = lazy(() => import('./pages/superadmin/profile/ProfilePage'));
const CbwtfDashboard = lazy(() => import('./pages/cbwtf/Dashboard'));
const HcfDashboard = lazy(() => import('./pages/hcf/Dashboard'));
// CBWTF Finance pages
const FinanceBankAccounts = lazy(() => import('./pages/cbwtf/finance/BankAccounts'));
const FinanceInvoices = lazy(() => import('./pages/cbwtf/finance/Invoices'));
const FinanceBills = lazy(() => import('./pages/cbwtf/finance/Bills'));
const FinanceRevenue = lazy(() => import('./pages/cbwtf/finance/Revenue'));

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
                  path="/superadmin"
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
                  {/* CBWTF Management */}
                  <Route path="cbwtfs" element={<CBWTFManagement />} />
                  <Route path="cbwtfs/new" element={<OnboardCBWTF />} />
                  <Route path="cbwtfs/:id" element={<CBWTFDetail />} />
                  {/* User Management */}
                  <Route path="users" element={<AllUsers />} />
                  <Route path="users/new" element={<CreateUser />} />
                  <Route path="users/:id" element={<UserDetail />} />
                  {/* Master Data */}
                  <Route path="master/hcfs" element={<MasterHcfs />} />
                  <Route path="master/pickups" element={<MasterPickups />} />
                  <Route path="master/bags" element={<MasterBags />} />
                  <Route path="master/qr-labels" element={<MasterQrLabels />} />
                  <Route path="master/attendance" element={<MasterAttendance />} />
                  <Route path="master/vehicles" element={<MasterVehicles />} />
                  <Route path="master/invoices" element={<MasterInvoices />} />
                  <Route path="master/payments" element={<MasterPayments />} />
                  <Route path="master/audit-logs" element={<MasterAuditLogs />} />
                  {/* Payment Gateway & Settings */}
                  <Route path="payment-gateway" element={<PaymentGateway />} />
                  <Route path="settings" element={<SystemConfig />} />
                  <Route path="profile" element={<ProfilePage />} />
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
                  {/* Finance Section */}
                  <Route path="finance/bank-accounts" element={<FinanceBankAccounts />} />
                  <Route path="finance/invoices" element={<FinanceInvoices />} />
                  <Route path="finance/bills" element={<FinanceBills />} />
                  <Route path="finance/revenue" element={<FinanceRevenue />} />
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
