import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Chip,
  LinearProgress,
  alpha,
  Alert,
  AlertTitle,
  Skeleton,
  Stack,
  Divider,
} from '@mui/material';
import {
  TrendingUp,
  TrendingDown,
  LocalShipping,
  Delete as WasteIcon,
  Warning as WarningIcon,
  AttachMoney,
  People as PeopleIcon,
  Speed as SpeedIcon,
  Schedule as ScheduleIcon,
  ErrorOutline,
  Autorenew,
} from '@mui/icons-material';
import {

  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  Legend,
} from 'recharts';
import { cbwtfApi, type CBWTFDashboardDTO, type RiskAlert } from '../../api/cbwtf';

// Metric Card Component
interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: { value: number; label: string };
  color?: string;
  loading?: boolean;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  color = '#6366F1',
  loading = false,
}) => (
  <Card>
    <CardContent sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box sx={{ flex: 1 }}>
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
          {trend && (
            <Box sx={{ display: 'flex', alignItems: 'center', mt: 1, gap: 0.5 }}>
              {trend.value >= 0 ? (
                <TrendingUp sx={{ fontSize: 16, color: 'success.main' }} />
              ) : (
                <TrendingDown sx={{ fontSize: 16, color: 'error.main' }} />
              )}
              <Typography
                variant="caption"
                sx={{ color: trend.value >= 0 ? 'success.main' : 'error.main' }}
              >
                {Math.abs(trend.value)}% {trend.label}
              </Typography>
            </Box>
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

// Risk Alert Component
const RiskAlertCard: React.FC<{ alerts: RiskAlert[] }> = ({ alerts }) => {
  if (alerts.length === 0) return null;

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      default: return 'info';
    }
  };

  return (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
          <WarningIcon color="warning" />
          Risk Alerts ({alerts.length})
        </Typography>
        <Stack spacing={2}>
          {alerts.map((alert, index) => (
            <Alert 
              key={index} 
              severity={getSeverityColor(alert.severity) as 'error' | 'warning' | 'info'}
              icon={<ErrorOutline />}
            >
              <AlertTitle sx={{ fontWeight: 600 }}>{alert.title}</AlertTitle>
              {alert.description}
            </Alert>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
};

// Mock data for charts (keep for visualization - would need real data from API)
const categoryData = [
  { name: 'Yellow', value: 45, color: '#FBBF24' },
  { name: 'Red', value: 25, color: '#EF4444' },
  { name: 'Blue', value: 20, color: '#3B82F6' },
  { name: 'White', value: 10, color: '#E2E8F0' },
];

const trendData = [
  { date: 'Mon', yellow: 120, red: 80, blue: 60, white: 40 },
  { date: 'Tue', yellow: 150, red: 90, blue: 70, white: 35 },
  { date: 'Wed', yellow: 135, red: 85, blue: 75, white: 45 },
  { date: 'Thu', yellow: 160, red: 95, blue: 65, white: 50 },
  { date: 'Fri', yellow: 180, red: 100, blue: 80, white: 55 },
  { date: 'Sat', yellow: 90, red: 60, blue: 40, white: 30 },
  { date: 'Sun', yellow: 70, red: 45, blue: 35, white: 25 },
];

const CbwtfDashboard: React.FC = () => {
  // Fetch dashboard data from API
  const { data: dashboard, isLoading, isError, error, refetch } = useQuery<CBWTFDashboardDTO>({
    queryKey: ['cbwtf-dashboard'],
    queryFn: cbwtfApi.getDashboard,
    refetchInterval: 60000, // Refresh every minute
  });

  // Format currency
  const formatCurrency = (amount: number) => {
    if (amount >= 100000) {
      return `₹${(amount / 100000).toFixed(1)}L`;
    } else if (amount >= 1000) {
      return `₹${(amount / 1000).toFixed(1)}K`;
    }
    return `₹${amount.toFixed(0)}`;
  };

  if (isError) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert 
          severity="error" 
          action={
            <Chip 
              icon={<Autorenew />} 
              label="Retry" 
              onClick={() => refetch()} 
              size="small" 
            />
          }
        >
          Failed to load dashboard: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Welcome Header */}
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            {isLoading ? <Skeleton width={200} /> : `Welcome back`}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {isLoading ? <Skeleton width={300} /> : `Here's what's happening with ${dashboard?.facilityName || 'your facility'} today.`}
          </Typography>
        </Box>
        {dashboard && (
          <Chip 
            label={`${dashboard.subscriptionPlan} • ${dashboard.subscriptionDaysLeft >= 0 ? `${dashboard.subscriptionDaysLeft} days left` : 'Unlimited'}`}
            color={dashboard.subscriptionDaysLeft < 7 && dashboard.subscriptionDaysLeft >= 0 ? 'error' : 'primary'}
            icon={<ScheduleIcon />}
          />
        )}
      </Box>

      {/* Risk Alerts */}
      {dashboard?.riskAlerts && dashboard.riskAlerts.length > 0 && (
        <RiskAlertCard alerts={dashboard.riskAlerts} />
      )}

      {/* Phase 2 Metric Cards - All 6 Required Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* 1. Active HCFs */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Active HCFs"
            value={dashboard?.activeHcfs ?? '-'}
            subtitle={`of ${dashboard?.totalAgreements ?? 0} total agreements`}
            icon={<PeopleIcon />}
            color="#10B981"
            loading={isLoading}
          />
        </Grid>
        
        {/* 2. Total Waste Today */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Bags Processed Today"
            value={dashboard?.bagsProcessedToday ?? '-'}
            subtitle={`${dashboard?.bagsProcessedThisWeek ?? 0} this week`}
            icon={<WasteIcon />}
            trend={{ value: 12, label: 'vs yesterday' }}
            color="#6366F1"
            loading={isLoading}
          />
        </Grid>

        {/* 3. Vehicles Online */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Vehicles Online"
            value={`${dashboard?.vehiclesOnline ?? 0}/${dashboard?.totalVehicles ?? 0}`}
            subtitle="GPS active < 15 min"
            icon={<LocalShipping />}
            color="#8B5CF6"
            loading={isLoading}
          />
        </Grid>

        {/* 4. Staff Attendance */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Staff Present"
            value={`${dashboard?.staffPresentToday ?? 0}/${dashboard?.totalStaff ?? 0}`}
            subtitle="Attendance today"
            icon={<SpeedIcon />}
            color="#06B6D4"
            loading={isLoading}
          />
        </Grid>

        {/* 5. Unpaid Invoices */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Unpaid Invoices"
            value={dashboard ? formatCurrency(dashboard.pendingInvoiceAmount) : '-'}
            subtitle={`${dashboard?.pendingInvoiceCount ?? 0} pending`}
            icon={<AttachMoney />}
            color="#F59E0B"
            loading={isLoading}
          />
        </Grid>

        {/* 6. Subscription Days Left */}
        <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
          <MetricCard
            title="Subscription"
            value={dashboard?.subscriptionDaysLeft ?? '-'}
            subtitle={dashboard?.subscriptionDaysLeft !== undefined && dashboard.subscriptionDaysLeft >= 0 ? 'days remaining' : 'Unlimited'}
            icon={<ScheduleIcon />}
            color={dashboard?.subscriptionDaysLeft !== undefined && dashboard.subscriptionDaysLeft < 7 ? '#EF4444' : '#22C55E'}
            loading={isLoading}
          />
        </Grid>
      </Grid>

      {/* Additional Metrics Row */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Agreements Expiring"
            value={dashboard?.agreementsExpiringSoon ?? '-'}
            subtitle="Within 30 days"
            icon={<WarningIcon />}
            color="#EF4444"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Anomaly Bags"
            value={dashboard?.anomalyBagsThisWeek ?? '-'}
            subtitle="This week"
            icon={<ErrorOutline />}
            color="#F97316"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total Revenue"
            value={dashboard ? formatCurrency(dashboard.totalRevenueAllTime) : '-'}
            subtitle="All time"
            icon={<AttachMoney />}
            color="#22C55E"
            loading={isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="QR Labels Issued"
            value={dashboard?.totalBagLabelsIssued ?? '-'}
            subtitle="Total generated"
            icon={<WasteIcon />}
            color="#3B82F6"
            loading={isLoading}
          />
        </Grid>
      </Grid>

      {/* Charts Row */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Category Breakdown Pie Chart */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ height: 400 }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Waste by Category
              </Typography>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={categoryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={2}
                    label={({ name, percent }) =>
                      `${name} ${((percent ?? 0) * 100).toFixed(0)}%`
                    }
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
              {/* Blue Waste Compliance */}
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">
                    Blue Waste Compliance
                  </Typography>
                  <Chip
                    label="20%"
                    size="small"
                    color="warning"
                    sx={{ height: 20, fontSize: '0.7rem' }}
                  />
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={36}
                  sx={{
                    height: 8,
                    borderRadius: 4,
                    bgcolor: alpha('#3B82F6', 0.2),
                    '& .MuiLinearProgress-bar': {
                      bgcolor: '#3B82F6',
                      borderRadius: 4,
                    },
                  }}
                />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  Target: 55% | Current: 20%
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Weekly Trend Line Chart */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ height: 400 }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Weekly Collection Trend
              </Typography>
              <ResponsiveContainer width="100%" height={320}>
                <LineChart data={trendData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="date" stroke="#94A3B8" />
                  <YAxis stroke="#94A3B8" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      border: '1px solid #334155',
                      borderRadius: 8,
                    }}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="yellow"
                    stroke="#FBBF24"
                    strokeWidth={2}
                    dot={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="red"
                    stroke="#EF4444"
                    strokeWidth={2}
                    dot={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="blue"
                    stroke="#3B82F6"
                    strokeWidth={2}
                    dot={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="white"
                    stroke="#E2E8F0"
                    strokeWidth={2}
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Activity & Expiring Agreements */}
      <Grid container spacing={3}>
        {/* Recent Bag Events */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Recent Activity
              </Typography>
              {isLoading ? (
                <Stack spacing={2}>
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} height={40} />
                  ))}
                </Stack>
              ) : dashboard?.recentBagEvents && dashboard.recentBagEvents.length > 0 ? (
                <Stack divider={<Divider />} spacing={1}>
                  {dashboard.recentBagEvents.slice(0, 5).map((event, index) => (
                    <Box key={index} sx={{ py: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" fontWeight={500}>
                          {event.qrCode || 'Unknown QR'}
                        </Typography>
                        <Chip 
                          label={event.eventType} 
                          size="small" 
                          color={event.anomalyState && event.anomalyState !== 'NONE' ? 'error' : 'default'}
                        />
                      </Box>
                      <Typography variant="caption" color="text.secondary">
                        {event.hcfName || 'Unknown HCF'} • {new Date(event.eventTs).toLocaleString()}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No recent activity
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Expiring Agreements */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Agreements Expiring Soon
              </Typography>
              {isLoading ? (
                <Stack spacing={2}>
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} height={40} />
                  ))}
                </Stack>
              ) : dashboard?.expiringAgreements && dashboard.expiringAgreements.length > 0 ? (
                <Stack divider={<Divider />} spacing={1}>
                  {dashboard.expiringAgreements.slice(0, 5).map((agreement, index) => (
                    <Box key={index} sx={{ py: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" fontWeight={500}>
                          {agreement.hcfName || agreement.agreementNumber}
                        </Typography>
                        <Chip 
                          label={`${agreement.daysUntilExpiry} days`}
                          size="small" 
                          color={agreement.daysUntilExpiry < 7 ? 'error' : 'warning'}
                        />
                      </Box>
                      <Typography variant="caption" color="text.secondary">
                        {agreement.agreementNumber} • Expires: {agreement.endDate ? new Date(agreement.endDate).toLocaleDateString() : 'N/A'}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No agreements expiring soon
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default CbwtfDashboard;
