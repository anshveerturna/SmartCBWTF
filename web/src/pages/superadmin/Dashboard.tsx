import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Stack,
  IconButton,
  Tooltip,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  TrendingUp,
  LocalShipping,
  Business,
  People,
  Add as AddIcon,
  AttachMoney,
  Warning as WarningIcon,
  CheckCircle as ResolvedIcon,
  Check as CheckIcon,
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
  const queryClient = useQueryClient();

  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['platform-stats'],
    queryFn: adminApi.getPlatformStats,
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  // Mutation for resolving errors
  const resolveMutation = useMutation({
    mutationFn: (id: string) => adminApi.resolveError(id, 'Manually resolved from dashboard'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['platform-stats'] });
    },
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

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
          onClick={() => navigate('/superadmin/cbwtfs/new')}
        >
          Onboard CBWTF
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load platform statistics. Using cached data.
        </Alert>
      )}

      {/* Platform Metrics - Row 1 */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Active CBWTFs"
            value={stats?.activeCBWTFs ?? 0}
            subtitle="Currently operating"
            icon={<Business />}
            color="#6366F1"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Total HCFs"
            value={stats?.totalHcfs?.toLocaleString() ?? '0'}
            subtitle="Across all CBWTFs"
            icon={<LocalShipping />}
            color="#10B981"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Total Users"
            value={stats?.totalUsers?.toLocaleString() ?? '0'}
            subtitle="Platform-wide"
            icon={<People />}
            color="#F59E0B"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Total Revenue"
            value={formatCurrency(stats?.totalRevenue ?? 0)}
            subtitle="From paid invoices"
            icon={<AttachMoney />}
            color="#10B981"
            loading={isLoading}
          />
        </Grid>
      </Grid>

      {/* Row 2 - Status + Errors */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {/* CBWTF Status Breakdown */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 3 }}>
                CBWTF Status Breakdown
              </Typography>
              <Grid container spacing={2}>
                {[
                  { status: 'Active', count: stats?.activeCBWTFs ?? 0, color: 'success' as const },
                  { status: 'Trial', count: stats?.trialCBWTFs ?? 0, color: 'info' as const },
                  { status: 'Expired', count: stats?.expiredCBWTFs ?? 0, color: 'warning' as const },
                  { status: 'Suspended', count: stats?.suspendedCBWTFs ?? 0, color: 'error' as const },
                ].map(({ status, count, color }) => (
                  <Grid item key={status} xs={6} md={3}>
                    <Box
                      sx={{
                        p: 2,
                        borderRadius: 2,
                        bgcolor: 'background.default',
                        textAlign: 'center',
                        cursor: 'pointer',
                        '&:hover': { bgcolor: 'action.hover' },
                      }}
                      onClick={() => navigate(`/superadmin/cbwtfs?status=${status.toUpperCase()}`)}
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
              <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Total: {stats?.totalCBWTFs ?? 0} CBWTFs
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* System Errors */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="h6">
                    System Errors
                  </Typography>
                  <Button 
                    size="small" 
                    onClick={() => navigate('/superadmin/errors')}
                  >
                    View All
                  </Button>
                </Box>
                {(stats?.pendingErrors ?? 0) > 0 && (
                  <Chip
                    icon={<WarningIcon />}
                    label={`${stats?.pendingErrors} open`}
                    color="error"
                    size="small"
                  />
                )}
              </Box>
              {isLoading ? (
                <Stack spacing={1}>
                  {[1, 2, 3].map(i => <Skeleton key={i} height={40} />)}
                </Stack>
              ) : (stats?.recentErrors?.length ?? 0) > 0 ? (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Component</TableCell>
                      <TableCell>Message</TableCell>
                      <TableCell align="center">Status</TableCell>
                      <TableCell align="center">Action</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {stats?.recentErrors?.slice(0, 5).map((err) => (
                      <TableRow key={err.id} hover>
                        <TableCell>
                          <Typography variant="body2" fontFamily="monospace" fontSize="0.75rem">
                            {err.component}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>
                            {err.message}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          {err.resolved ? (
                            <ResolvedIcon color="success" fontSize="small" />
                          ) : (
                            <WarningIcon color="warning" fontSize="small" />
                          )}
                        </TableCell>
                        <TableCell align="center">
                          {!err.resolved && (
                            <Tooltip title="Resolve this error">
                              <IconButton
                                size="small"
                                color="success"
                                onClick={() => resolveMutation.mutate(err.id)}
                                disabled={resolveMutation.isPending}
                              >
                                <CheckIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <ResolvedIcon sx={{ fontSize: 48, color: 'success.main', mb: 1 }} />
                  <Typography color="text.secondary">
                    No pending issues
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Quick Actions */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Quick Actions
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              startIcon={<Business />}
              onClick={() => navigate('/superadmin/cbwtfs')}
            >
              Manage CBWTFs
            </Button>
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={() => navigate('/superadmin/cbwtfs/new')}
            >
              Onboard New CBWTF
            </Button>
            <Button
              variant="outlined"
              startIcon={<People />}
              onClick={() => navigate('/superadmin/users')}
            >
              Manage Users
            </Button>
            <Button
              variant="outlined"
              startIcon={<TrendingUp />}
              onClick={() => navigate('/superadmin/master/audit-logs')}
            >
              View Audit Logs
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SuperAdminDashboard;
