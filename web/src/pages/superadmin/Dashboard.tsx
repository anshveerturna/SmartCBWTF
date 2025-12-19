import React from 'react';
import { Box, Typography, Card, CardContent, Grid, Chip, alpha, Button } from '@mui/material';
import {
  TrendingUp,
  LocalShipping,
  Business,
  AttachMoney,
  Add as AddIcon,
} from '@mui/icons-material';

// Metric Card (can be extracted to shared component)
interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  color = '#6366F1',
}) => (
  <Card>
    <CardContent sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {title}
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
            {value}
          </Typography>
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
        <Button variant="contained" startIcon={<AddIcon />}>
          Onboard Tenant
        </Button>
      </Box>

      {/* Platform Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Active Tenants"
            value="24"
            subtitle="CBWTFs onboarded"
            icon={<Business />}
            color="#6366F1"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total HCFs"
            value="1,248"
            subtitle="Across all tenants"
            icon={<LocalShipping />}
            color="#10B981"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Monthly Waste"
            value="45.2T"
            subtitle="December 2025"
            icon={<TrendingUp />}
            color="#F59E0B"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Platform Revenue"
            value="₹12.5L"
            subtitle="This month"
            icon={<AttachMoney />}
            color="#EF4444"
          />
        </Grid>
      </Grid>

      {/* Tenant Status */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 3 }}>
            Tenant Status
          </Typography>
          <Grid container spacing={2}>
            {['Active', 'Trial', 'Expired', 'Suspended'].map((status, index) => (
              <Grid key={status} size={{ xs: 6, md: 3 }}>
                <Box
                  sx={{
                    p: 2,
                    borderRadius: 2,
                    bgcolor: 'background.default',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="h5" sx={{ fontWeight: 700 }}>
                    {[18, 4, 1, 1][index]}
                  </Typography>
                  <Chip
                    label={status}
                    size="small"
                    color={
                      status === 'Active'
                        ? 'success'
                        : status === 'Trial'
                        ? 'primary'
                        : status === 'Expired'
                        ? 'warning'
                        : 'error'
                    }
                    sx={{ mt: 1 }}
                  />
                </Box>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SuperAdminDashboard;
