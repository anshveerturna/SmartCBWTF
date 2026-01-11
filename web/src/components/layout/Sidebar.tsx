import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
  alpha,
  Chip,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  LocalHospital as HcfIcon,
  QrCode2 as QrCodeIcon,
  LocalShipping as OperationsIcon,
  Receipt as BillingIcon,
  NotificationsActive as AlertsIcon,
  Business as BusinessIcon,
  Settings as SettingsIcon,
  Analytics as AnalyticsIcon,
  People as PeopleIcon,
  PersonAdd as PersonAddIcon,
  Add as AddIcon,
  Storage as StorageIcon,
  History as HistoryIcon,
  DirectionsCar as VehicleIcon,
  Payment as PaymentIcon,
  Inventory as InventoryIcon,
  EventAvailable as AttendanceIcon,
  Route as RouteIcon,
  FactCheck as FactCheckIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useAuth } from '../../auth';
import type { UserRole } from '../../types/api';

export const DRAWER_WIDTH = 280;

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
  roles: UserRole[];
  badge?: number;
}

interface NavSection {
  title: string | null;
  items: NavItem[];
}

interface SidebarProps {
  onNavigate?: () => void;
}

// Navigation configuration by role
const getSuperAdminSections = (): NavSection[] => [
  {
    title: null,
    items: [
      { path: '/superadmin/dashboard', label: 'Platform Dashboard', icon: <DashboardIcon />, roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    title: 'CBWTF Management',
    items: [
      { path: '/superadmin/cbwtfs', label: 'All CBWTFs', icon: <BusinessIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/cbwtfs/new', label: 'Onboard CBWTF', icon: <AddIcon />, roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    title: 'User Management',
    items: [
      { path: '/superadmin/users', label: 'All Users', icon: <PeopleIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/users/new', label: 'Create User', icon: <PersonAddIcon />, roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    title: 'Master Data',
    items: [
      { path: '/superadmin/master/hcfs', label: 'HCFs', icon: <HcfIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/pickups', label: 'Waste Pickups', icon: <OperationsIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/bags', label: 'Waste Bags', icon: <InventoryIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/qr-labels', label: 'QR Labels', icon: <QrCodeIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/attendance', label: 'Attendance', icon: <AttendanceIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/vehicles', label: 'Vehicles', icon: <VehicleIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/bills', label: 'Bills', icon: <BillingIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/payments', label: 'Payments', icon: <PaymentIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/master/audit-logs', label: 'Audit Logs', icon: <HistoryIcon />, roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    title: 'System',
    items: [
      { path: '/superadmin/payment-gateway', label: 'Payment Gateway', icon: <AnalyticsIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/email-templates', label: 'Email Templates', icon: <AlertsIcon />, roles: ['SUPER_ADMIN'] },
      { path: '/superadmin/settings', label: 'Configuration', icon: <SettingsIcon />, roles: ['SUPER_ADMIN'] },
    ],
  },
];

const getCbwtfAdminItems = (): NavItem[] => [
  { path: '/cbwtf/dashboard', label: 'Dashboard', icon: <DashboardIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/analytics', label: 'Analytics', icon: <AnalyticsIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/vehicles', label: 'Vehicles', icon: <OperationsIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/staff', label: 'Staff', icon: <PeopleIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/attendance', label: 'Attendance', icon: <AttendanceIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/hcfs/small', label: 'HCFs (0–30 Beds)', icon: <HcfIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/hcfs/large', label: 'HCFs (Above 30 Beds)', icon: <HcfIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/labels', label: 'QR Labels', icon: <QrCodeIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/consumables', label: 'Consumables', icon: <InventoryIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/consumable-orders', label: 'Consumable Orders', icon: <InventoryIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/routes', label: 'Routes & Planning', icon: <RouteIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/compliance', label: 'Compliance & Reports', icon: <AnalyticsIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/dues-verification', label: 'Dues Verification', icon: <FactCheckIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/alerts', label: 'Alerts', icon: <AlertsIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/settings', label: 'Settings', icon: <SettingsIcon />, roles: ['CBWTF_ADMIN'] },
];

// Finance sub-navigation for CBWTF Admin
const getCbwtfFinanceItems = (): NavItem[] => [
  { path: '/cbwtf/payments', label: 'Payments', icon: <PaymentIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/billing', label: 'Bills', icon: <BillingIcon />, roles: ['CBWTF_ADMIN'] },
  { path: '/cbwtf/finance/revenue', label: 'Revenue', icon: <AnalyticsIcon />, roles: ['CBWTF_ADMIN'] },
];

const getHcfAdminItems = (): NavItem[] => [
  { path: '/hcf/dashboard', label: 'Dashboard', icon: <DashboardIcon />, roles: ['HCF_ADMIN'] },
  { path: '/hcf/compliance', label: 'Compliance Reports', icon: <AnalyticsIcon />, roles: ['HCF_ADMIN'] },
  { path: '/hcf/qr-labels', label: 'QR Labels', icon: <QrCodeIcon />, roles: ['HCF_ADMIN'] },
  { path: '/hcf/consumables/order', label: 'Order Consumables', icon: <InventoryIcon />, roles: ['HCF_ADMIN'] },
  { path: '/hcf/agreement', label: 'Agreement', icon: <StorageIcon />, roles: ['HCF_ADMIN'] },
];

const getTopManagementItems = (): NavItem[] => [
  { path: '/management/dues-approvals', label: 'Dues Approvals', icon: <CheckCircleIcon />, roles: ['TOP_MANAGEMENT'] },
];

export const Sidebar: React.FC<SidebarProps> = ({ onNavigate }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const handleNavigate = (path: string) => {
    navigate(path);
    onNavigate?.();
  };

  const getRoleBadgeColor = (role: UserRole | undefined): 'error' | 'primary' | 'secondary' => {
    if (role === 'SUPER_ADMIN') return 'error';
    if (role === 'CBWTF_ADMIN') return 'primary';
    return 'secondary';
  };

  const getRoleLabel = (role: UserRole | undefined): string => {
    if (role === 'SUPER_ADMIN') return 'Super Admin';
    if (role === 'CBWTF_ADMIN') return 'CBWTF Admin';
    if (role === 'HCF_ADMIN') return 'HCF Admin';
    return role || '';
  };

  const renderNavItem = (item: NavItem) => {
    // Special handling for HCF routes to match detail pages
    let isActive = location.pathname === item.path;
    
    // For HCF list routes, also match when viewing HCF detail pages
    // HCF detail pages are /cbwtf/hcfs/{uuid} and we need to check referring context
    // Since we don't have HCF bed count here, match /hcfs/ prefix but not /hcfs/small or /hcfs/large
    if (!isActive && item.path === '/cbwtf/hcfs/small') {
      // This is complex - for now, don't auto-match detail pages since we can't know bed count
      // The back navigation was already fixed to go to correct list
    }
    if (!isActive && item.path === '/cbwtf/hcfs/large') {
      // Same as above
    }
    
    // For other routes, use startsWith for nested page matching
    if (!isActive && location.pathname.startsWith(item.path) && item.path !== '/cbwtf/hcfs/small' && item.path !== '/cbwtf/hcfs/large') {
      isActive = true;
    }
    return (
      <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
        <ListItemButton
          onClick={() => handleNavigate(item.path)}
          sx={{
            borderRadius: 2,
            px: 2,
            py: 1,
            bgcolor: isActive ? (theme) => alpha(theme.palette.primary.main, 0.12) : 'transparent',
            color: isActive ? 'primary.main' : 'text.secondary',
            '&:hover': {
              bgcolor: (theme) => alpha(theme.palette.primary.main, 0.08),
            },
          }}
        >
          <ListItemIcon
            sx={{
              minWidth: 36,
              color: isActive ? 'primary.main' : 'text.secondary',
            }}
          >
            {item.icon}
          </ListItemIcon>
          <ListItemText
            primary={item.label}
            primaryTypographyProps={{
              fontSize: '0.85rem',
              fontWeight: isActive ? 600 : 400,
            }}
          />
          {item.badge !== undefined && item.badge > 0 && (
            <Chip
              label={item.badge}
              size="small"
              color="error"
              sx={{ height: 20, minWidth: 20, fontSize: '0.7rem' }}
            />
          )}
        </ListItemButton>
      </ListItem>
    );
  };

  const renderSuperAdminNav = () => {
    const sections = getSuperAdminSections();
    return (
      <>
        {sections.map((section, index) => (
          <Box key={section.title || `section-${index}`}>
            {section.title && (
              <Typography
                variant="overline"
                sx={{
                  px: 2,
                  py: 1,
                  display: 'block',
                  color: 'text.secondary',
                  fontSize: '0.65rem',
                  fontWeight: 700,
                  letterSpacing: 1.2,
                }}
              >
                {section.title}
              </Typography>
            )}
            <List sx={{ px: 1, py: 0 }}>
              {section.items.map(renderNavItem)}
            </List>
            {index < sections.length - 1 && <Divider sx={{ my: 1, mx: 2 }} />}
          </Box>
        ))}
      </>
    );
  };

  const renderFlatNav = (items: NavItem[]) => (
    <List sx={{ flex: 1, px: 2, py: 2 }}>
      {items.map(renderNavItem)}
    </List>
  );

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Logo / Brand */}
      <Box
        sx={{
          p: 3,
          display: 'flex',
          alignItems: 'center',
          gap: 2,
        }}
      >
        <Box
          component="img"
          src="/logo.svg"
          alt="SmartCBWTF"
          sx={{
            width: 40,
            height: 40,
          }}
        />
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            SmartCBWTF
          </Typography>
          <Chip
            label={getRoleLabel(user?.role)}
            size="small"
            color={getRoleBadgeColor(user?.role)}
            sx={{ height: 20, fontSize: '0.65rem', mt: 0.5 }}
          />
        </Box>
      </Box>

      <Divider sx={{ mx: 2 }} />

      {/* Navigation Links */}
      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        {user?.role === 'SUPER_ADMIN' && renderSuperAdminNav()}
        {user?.role === 'CBWTF_ADMIN' && (
          <>
            {renderFlatNav(getCbwtfAdminItems())}
            <Divider sx={{ mx: 2, my: 1 }} />
            <Typography variant="overline" sx={{ px: 3, py: 0.5, display: 'block', color: 'text.secondary', fontSize: '0.65rem' }}>
              Finance
            </Typography>
            {renderFlatNav(getCbwtfFinanceItems())}
          </>
        )}
        {user?.role === 'HCF_ADMIN' && renderFlatNav(getHcfAdminItems())}
        {user?.role === 'TOP_MANAGEMENT' && renderFlatNav(getTopManagementItems())}
      </Box>

      {/* Footer */}
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          © 2025 SmartCBWTF
        </Typography>
      </Box>
    </Box>
  );
};
