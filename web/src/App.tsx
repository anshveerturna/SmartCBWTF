import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { CssBaseline, Box, CircularProgress } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeContextProvider } from './theme';
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
const MasterPayments = lazy(() => import('./pages/superadmin/master/MasterPayments'));
const MasterAuditLogs = lazy(() => import('./pages/superadmin/master/MasterAuditLogs'));
const PaymentGateway = lazy(() => import('./pages/superadmin/PaymentGateway'));
const SystemConfig = lazy(() => import('./pages/superadmin/SystemConfig'));
const ProfilePage = lazy(() => import('./pages/superadmin/profile/ProfilePage'));
const EmailTemplates = lazy(() => import('./pages/superadmin/EmailTemplates'));
const CbwtfDashboard = lazy(() => import('./pages/cbwtf/Dashboard'));
const CbwtfAnalytics = lazy(() => import('./pages/cbwtf/Analytics'));
const CbwtfVehicles = lazy(() => import('./pages/cbwtf/Vehicles'));
const CbwtfVehicleLiveMap = lazy(() => import('./pages/cbwtf/VehicleLiveMap'));
const CbwtfStaff = lazy(() => import('./pages/cbwtf/Staff'));
const CbwtfStaffDetail = lazy(() => import('./pages/cbwtf/StaffDetail'));
const CbwtfAttendance = lazy(() => import('./pages/cbwtf/AttendanceList'));
const CbwtfProfile = lazy(() => import('./pages/cbwtf/Profile'));
const CbwtfHcfListSmall = lazy(() => import('./pages/cbwtf/HcfListSmall'));
const CbwtfHcfListLarge = lazy(() => import('./pages/cbwtf/HcfListLarge'));
const CbwtfHcfDetail = lazy(() => import('./pages/cbwtf/HcfDetail'));
const CbwtfHcfPendingApprovals = lazy(() => import('./pages/cbwtf/HcfPendingApprovals'));
const CbwtfHcfRegister = lazy(() => import('./pages/cbwtf/HcfRegister'));
const CbwtfQrLabels = lazy(() => import('./pages/cbwtf/QrLabels'));
const CbwtfConsumables = lazy(() => import('./pages/cbwtf/Consumables'));
const CbwtfConsumableNew = lazy(() => import('./pages/cbwtf/ConsumableNew'));
const CbwtfConsumableDetail = lazy(() => import('./pages/cbwtf/ConsumableDetail'));
const CbwtfBillingList = lazy(() => import('./pages/cbwtf/BillingList'));
const CbwtfBillDetail = lazy(() => import('./pages/cbwtf/BillDetail'));
const CbwtfBillingSettings = lazy(() => import('./pages/cbwtf/BillingSettings'));
const CbwtfComplianceReports = lazy(() => import('./pages/cbwtf/ComplianceReports'));
const CbwtfAlerts = lazy(() => import('./pages/cbwtf/Alerts'));
const CbwtfNotificationSettings = lazy(() => import('./pages/cbwtf/NotificationSettings'));
const CbwtfBankAccounts = lazy(() => import('./pages/cbwtf/BankAccounts'));
const CbwtfPayments = lazy(() => import('./pages/cbwtf/Payments'));
const HcfDashboard = lazy(() => import('./pages/hcf/Dashboard'));
// CBWTF Finance pages
const FinanceBankAccounts = lazy(() => import('./pages/cbwtf/finance/BankAccounts'));
const FinanceBills = lazy(() => import('./pages/cbwtf/finance/Bills'));
const CbwtfSettings = lazy(() => import('./pages/cbwtf/Settings'));
const FinanceRevenue = lazy(() => import('./pages/cbwtf/finance/Revenue'));
// Utility pages
const Blocked = lazy(() => import('./pages/Blocked'));
const ChangePassword = lazy(() => import('./pages/ChangePassword'));

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
      <ThemeContextProvider>
        <CssBaseline />
        <AuthProvider>
          <BrowserRouter>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/login" element={<Login />} />
                <Route path="/blocked" element={<Blocked />} />
                <Route path="/change-password" element={<ChangePassword />} />

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
                  <Route path="master/bills" element={<CbwtfBillingList />} />
                  <Route path="master/payments" element={<MasterPayments />} />
                  <Route path="master/audit-logs" element={<MasterAuditLogs />} />
                  {/* Payment Gateway & Settings */}
                  <Route path="payment-gateway" element={<PaymentGateway />} />
                  <Route path="settings" element={<SystemConfig />} />
                  <Route path="email-templates" element={<EmailTemplates />} />
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
                  <Route path="analytics" element={<CbwtfAnalytics />} />
                  <Route path="vehicles" element={<CbwtfVehicles />} />
                  <Route path="vehicles/live-map" element={<CbwtfVehicleLiveMap />} />
                  <Route path="staff" element={<CbwtfStaff />} />
                  <Route path="staff/:id" element={<CbwtfStaffDetail />} />
                  <Route path="attendance" element={<CbwtfAttendance />} />
                  <Route path="hcfs" element={<Navigate to="small" replace />} />
                  <Route path="hcfs/small" element={<CbwtfHcfListSmall />} />
                  <Route path="hcfs/large" element={<CbwtfHcfListLarge />} />
                  <Route path="hcfs/pending" element={<CbwtfHcfPendingApprovals />} />
                  <Route path="hcfs/register" element={<CbwtfHcfRegister />} />
                  <Route path="hcfs/:id" element={<CbwtfHcfDetail />} />
                  <Route path="labels" element={<CbwtfQrLabels />} />
                  <Route path="consumables" element={<CbwtfConsumables />} />
                  <Route path="consumables/new" element={<CbwtfConsumableNew />} />
                  <Route path="consumables/:id" element={<CbwtfConsumableDetail />} />
                  <Route path="operations" element={<div>Waste Operations (Coming Soon)</div>} />
                  {/* Finance Section */}
                  <Route path="finance/bank-accounts" element={<FinanceBankAccounts />} />
                  <Route path="finance/bills" element={<FinanceBills />} />
                  <Route path="finance/revenue" element={<FinanceRevenue />} />
                  <Route path="settings" element={<CbwtfSettings />} />
                  <Route path="billing" element={<CbwtfBillingList />} />
                  <Route path="billing/:billId" element={<CbwtfBillDetail />} />
                  <Route path="settings/billing" element={<CbwtfBillingSettings />} />
                  {/* Compliance & Reports */}
                  <Route path="compliance" element={<CbwtfComplianceReports />} />
                  <Route path="alerts" element={<CbwtfAlerts />} />
                  {/* Phase 10: Payments & Bank Accounts */}
                  <Route path="payments" element={<CbwtfPayments />} />
                  <Route path="settings/bank-accounts" element={<CbwtfBankAccounts />} />
                  <Route path="settings/notifications" element={<CbwtfNotificationSettings />} />
                  <Route path="settings" element={<div>Settings (Coming Soon)</div>} />
                  <Route path="profile" element={<CbwtfProfile />} />
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
                  <Route path="bills" element={<div>Bills (Coming Soon)</div>} />
                  <Route path="agreement" element={<div>Agreement (Coming Soon)</div>} />
                </Route>

                {/* Default redirect */}
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ThemeContextProvider>
    </QueryClientProvider>
  );
};

export default App;
