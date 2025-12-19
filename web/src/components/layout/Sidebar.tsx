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
  Business as TenantsIcon,
  Settings as SettingsIcon,
  Analytics as AnalyticsIcon,
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

interface SidebarProps {
  onNavigate?: () => void;
}

// Navigation items configuration
const getNavItems = (role: UserRole | undefined): NavItem[] => {
  const superAdminItems: NavItem[] = [
    { path: '/superadmin/dashboard', label: 'Platform Dashboard', icon: <DashboardIcon />, roles: ['SUPER_ADMIN'] },
    { path: '/superadmin/tenants', label: 'Tenant Management', icon: <TenantsIcon />, roles: ['SUPER_ADMIN'] },
    { path: '/superadmin/analytics', label: 'Platform Analytics', icon: <AnalyticsIcon />, roles: ['SUPER_ADMIN'] },
    { path: '/superadmin/settings', label: 'System Settings', icon: <SettingsIcon />, roles: ['SUPER_ADMIN'] },
  ];

  const cbwtfAdminItems: NavItem[] = [
    { path: '/cbwtf/dashboard', label: 'Dashboard', icon: <DashboardIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/hcfs', label: 'HCF Management', icon: <HcfIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/labels', label: 'QR Labels', icon: <QrCodeIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/operations', label: 'Waste Operations', icon: <OperationsIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/billing', label: 'Billing & Invoicing', icon: <BillingIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/alerts', label: 'Alerts', icon: <AlertsIcon />, roles: ['CBWTF_ADMIN'] },
    { path: '/cbwtf/settings', label: 'Settings', icon: <SettingsIcon />, roles: ['CBWTF_ADMIN'] },
  ];

  const hcfAdminItems: NavItem[] = [
    { path: '/hcf/dashboard', label: 'Dashboard', icon: <DashboardIcon />, roles: ['HCF_ADMIN'] },
    { path: '/hcf/pickups', label: 'Pickup History', icon: <OperationsIcon />, roles: ['HCF_ADMIN'] },
    { path: '/hcf/invoices', label: 'Invoices', icon: <BillingIcon />, roles: ['HCF_ADMIN'] },
    { path: '/hcf/agreement', label: 'Agreement', icon: <TenantsIcon />, roles: ['HCF_ADMIN'] },
  ];

  if (role === 'SUPER_ADMIN') return superAdminItems;
  if (role === 'CBWTF_ADMIN') return cbwtfAdminItems;
  if (role === 'HCF_ADMIN') return hcfAdminItems;
  return [];
};

export const Sidebar: React.FC<SidebarProps> = ({ onNavigate }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const navItems = getNavItems(user?.role);

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
          sx={{
            width: 40,
            height: 40,
            borderRadius: 2,
            background: 'linear-gradient(135deg, #6366F1 0%, #10B981 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography variant="h6" sx={{ color: 'white', fontWeight: 700 }}>
            S
          </Typography>
        </Box>
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
      <List sx={{ flex: 1, px: 2, py: 2 }}>
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                onClick={() => handleNavigate(item.path)}
                sx={{
                  borderRadius: 2,
                  px: 2,
                  py: 1.25,
                  bgcolor: isActive ? (theme) => alpha(theme.palette.primary.main, 0.12) : 'transparent',
                  color: isActive ? 'primary.main' : 'text.secondary',
                  '&:hover': {
                    bgcolor: (theme) => alpha(theme.palette.primary.main, 0.08),
                  },
                }}
              >
                <ListItemIcon
                  sx={{
                    minWidth: 40,
                    color: isActive ? 'primary.main' : 'text.secondary',
                  }}
                >
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{
                    fontSize: '0.875rem',
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
        })}
      </List>

      {/* Footer */}
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          © 2025 SmartCBWTF
        </Typography>
      </Box>
    </Box>
  );
};
