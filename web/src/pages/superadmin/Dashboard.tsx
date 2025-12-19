import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Chip,
  alpha,
  Button,
  Skeleton,
  Alert,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  TrendingUp,
  LocalShipping,
  Business,
  People,
  Add as AddIcon,
} from '@mui/icons-material';
import { adminApi } from '../../api/admin';

// Metric Card
interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
  loading?: boolean;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  color = '#6366F1',
  loading = false,
}) => (
  <Card>
    <CardContent sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {title}
          </Typography>
          {loading ? (
            <Skeleton width={80} height={40} />
          ) : (
            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
              {value}
            </Typography>
          )}
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        <Box
          sx={{
            width: 48,
            height: 48,
            borderRadius: 2,
            bgcolor: alpha(color, 0.12),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: color,
          }}
        >
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

const SuperAdminDashboard: React.FC = () => {
  const navigate = useNavigate();

  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['platform-stats'],
    queryFn: adminApi.getPlatformStats,
  });

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Platform Overview
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Enterprise-wide metrics and tenant management
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/superadmin/tenants/new')}
        >
          Onboard Tenant
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load platform statistics. Using cached data.
        </Alert>
      )}

      {/* Platform Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Active Tenants"
            value={stats?.activeTenants ?? 0}
            subtitle="CBWTFs onboarded"
            icon={<Business />}
            color="#6366F1"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total HCFs"
            value={stats?.totalHcfs.toLocaleString() ?? '0'}
            subtitle="Across all tenants"
            icon={<LocalShipping />}
            color="#10B981"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total Users"
            value={stats?.totalUsers.toLocaleString() ?? '0'}
            subtitle="Platform-wide"
            icon={<People />}
            color="#F59E0B"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total Tenants"
            value={stats?.totalTenants ?? 0}
            subtitle="All statuses"
            icon={<TrendingUp />}
            color="#EF4444"
            loading={isLoading}
          />
        </Grid>
      </Grid>

      {/* Tenant Status */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 3 }}>
            Tenant Status Breakdown
          </Typography>
          <Grid container spacing={2}>
            {[
              { status: 'Active', count: stats?.activeTenants ?? 0, color: 'success' as const },
              { status: 'Trial', count: stats?.trialTenants ?? 0, color: 'info' as const },
              { status: 'Expired', count: stats?.expiredTenants ?? 0, color: 'warning' as const },
              { status: 'Suspended', count: stats?.suspendedTenants ?? 0, color: 'error' as const },
            ].map(({ status, count, color }) => (
              <Grid key={status} size={{ xs: 6, md: 3 }}>
                <Box
                  sx={{
                    p: 2,
                    borderRadius: 2,
                    bgcolor: 'background.default',
                    textAlign: 'center',
                    cursor: 'pointer',
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                  onClick={() => navigate(`/superadmin/tenants?status=${status.toUpperCase()}`)}
                >
                  {isLoading ? (
                    <Skeleton width={40} height={32} sx={{ mx: 'auto' }} />
                  ) : (
                    <Typography variant="h5" sx={{ fontWeight: 700 }}>
                      {count}
                  </Typography>
                  )}
                  <Chip
                    label={status}
                    size="small"
                    color={color}
                    sx={{ mt: 1 }}
                  />
                </Box>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Quick Actions
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              onClick={() => navigate('/superadmin/tenants')}
            >
              Manage Tenants
            </Button>
            <Button
              variant="outlined"
              onClick={() => navigate('/superadmin/tenants/new')}
            >
              Onboard New Tenant
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SuperAdminDashboard;
