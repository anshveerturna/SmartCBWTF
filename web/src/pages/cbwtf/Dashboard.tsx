import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
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
  useTheme,
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
} from '@mui/material';
import { Close as CloseIcon, OpenInNew as OpenInNewIcon } from '@mui/icons-material';
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
  QrCode2 as QrCodeIcon,
  Inventory as InventoryIcon,
  AccountBalanceWallet as WalletIcon,
  DirectionsCar as VehicleIcon,
  Badge as BadgeIcon,
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
  AreaChart,
  Area,
  Legend,
} from 'recharts';
import { cbwtfApi, type CBWTFDashboardDTO, type RiskAlert } from '../../api/cbwtf';

// Premium Metric Card Component
interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: { value: number; label: string };
  gradient: string[];
  loading?: boolean;
  glowColor?: string;
  onClick?: () => void;
  clickable?: boolean;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  gradient,
  loading = false,
  glowColor, // Unused in new design but kept for prop compatibility
  onClick,
  clickable = false,
}) => {
  const theme = useTheme();
  const primaryColor = gradient[0];

  return (
    <Card
      onClick={clickable ? onClick : undefined}
      sx={{
        height: '100%',
        cursor: clickable ? 'pointer' : 'default',
        transition: 'all 0.2s',
        '&:hover': {
          borderColor: primaryColor,
          transform: clickable ? 'translateY(-2px)' : 'none',
          boxShadow: clickable ? '0 4px 6px -1px rgba(0, 0, 0, 0.1)' : undefined,
        },
      }}
    >
      <CardContent sx={{ p: '20px !important' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: 1.5,
              bgcolor: alpha(primaryColor, 0.1),
              color: primaryColor,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {React.cloneElement(icon as any, { fontSize: 'small' })}
          </Box>
          {trend && (
            <Chip
              icon={trend.value >= 0 ? <TrendingUp /> : <TrendingDown />}
              label={`${Math.abs(trend.value)}%`}
              size="small"
              sx={{
                height: 24,
                bgcolor: trend.value >= 0 ? alpha(theme.palette.success.main, 0.1) : alpha(theme.palette.error.main, 0.1),
                color: trend.value >= 0 ? 'success.main' : 'error.main',
                fontWeight: 600,
                border: 'none',
                '& .MuiChip-icon': {
                  fontSize: '0.875rem',
                  color: 'inherit',
                },
              }}
            />
          )}
        </Box>

        <Box>
          <Typography 
            variant="body2" 
            color="text.secondary" 
            fontWeight={500}
            sx={{ mb: 0.5 }}
          >
            {title}
          </Typography>
          {loading ? (
            <Skeleton width={100} height={40} />
          ) : (
            <Typography variant="h4" fontWeight={600} color="text.primary">
              {value}
            </Typography>
          )}
          {subtitle && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              {subtitle}
            </Typography>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

// Enhanced Risk Alert Component
const RiskAlertCard: React.FC<{ alerts: RiskAlert[] }> = ({ alerts }) => {
  if (alerts.length === 0) return null;

  return (
    <Card sx={{ mb: 4, bgcolor: 'background.paper', borderColor: 'warning.light' }}>
      <CardContent sx={{ p: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <WarningIcon color="warning" />
          <Typography variant="h6" fontWeight={600}>
            Risk Alerts ({alerts.length})
          </Typography>
        </Box>
        <Stack spacing={1}>
          {alerts.map((alert, index) => (
            <Alert 
              key={index} 
              severity={alert.severity === 'CRITICAL' ? 'error' : 'warning'}
              icon={<ErrorOutline fontSize="small" />}
              sx={{ py: 0, alignItems: 'center' }}
            >
              <AlertTitle sx={{ fontSize: '0.875rem', fontWeight: 600, mb: 0 }}>{alert.title}</AlertTitle>
              {alert.description}
            </Alert>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
};

const CbwtfDashboard: React.FC = () => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [anomalyDialogOpen, setAnomalyDialogOpen] = useState(false);

  const { data: dashboard, isLoading, isError, error, refetch } = useQuery<CBWTFDashboardDTO>({
    queryKey: ['cbwtf-dashboard'],
    queryFn: cbwtfApi.getDashboard,
    refetchInterval: 60000,
  });

  // Fetch anomaly bags when dialog is opened
  const { data: anomalyBags, isLoading: anomalyLoading } = useQuery({
    queryKey: ['cbwtf-anomaly-bags'],
    queryFn: cbwtfApi.getAnomalyBags,
    enabled: anomalyDialogOpen,
  });

  // Fetch chart data from backend
  const { data: categoryData } = useQuery({
    queryKey: ['cbwtf-category-breakdown'],
    queryFn: cbwtfApi.getCategoryBreakdown,
    refetchInterval: 60000,
  });

  const { data: trendData } = useQuery({
    queryKey: ['cbwtf-weekly-trend'],
    queryFn: cbwtfApi.getWeeklyTrend,
    refetchInterval: 60000,
  });

  const { data: trendComparison } = useQuery({
    queryKey: ['cbwtf-trend-comparison'],
    queryFn: cbwtfApi.getTrendComparison,
    refetchInterval: 60000,
  });

  const chartCategoryData = categoryData || [];
  const chartTrendData = trendData || [];
  const totalCategoryCount = chartCategoryData.reduce((sum, item) => sum + (item.value || 0), 0);
  const blueCategoryCount = chartCategoryData.find((item) => item.name?.toLowerCase() === 'blue')?.value || 0;
  const bluePercent = totalCategoryCount > 0 ? (blueCategoryCount / totalCategoryCount) * 100 : 0;

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
    <Box sx={{ minHeight: '100vh' }}>
      {/* Hero Header */}
      <Box 
        sx={{ 
          mb: 4, 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box>
          <Typography variant="h4" fontWeight={700} color="text.primary" gutterBottom>
            {isLoading ? <Skeleton width={200} /> : 'Welcome back'}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {isLoading ? <Skeleton width={300} /> : `Overview for ${dashboard?.facilityName || 'your facility'}`}
          </Typography>
        </Box>
        {dashboard && (
          <Chip 
            label={dashboard.subscriptionDaysLeft >= 0 ? `${dashboard.subscriptionDaysLeft} days left` : 'Unlimited'}
            color={dashboard.subscriptionDaysLeft < 7 && dashboard.subscriptionDaysLeft >= 0 ? 'error' : 'primary'}
            variant="filled"
            icon={<ScheduleIcon />}
            sx={{ fontWeight: 600 }}
          />
        )}
      </Box>

      {/* Risk Alerts */}
      {dashboard?.riskAlerts && dashboard.riskAlerts.length > 0 && (
        <RiskAlertCard alerts={dashboard.riskAlerts} />
      )}

      {/* Primary Metrics Grid - 2x3 */}
      <Box 
        sx={{ 
          display: 'grid', 
          gridTemplateColumns: { 
            xs: '1fr', 
            sm: 'repeat(2, 1fr)', 
            lg: 'repeat(3, 1fr)' 
          }, 
          gap: 3, 
          mb: 4 
        }}
      >
        <MetricCard
          title="Active HCFs"
          value={dashboard?.activeHcfs ?? '-'}
          subtitle={`of ${dashboard?.totalAgreements ?? 0} total agreements`}
          icon={<PeopleIcon sx={{ fontSize: 28 }} />}
          gradient={['#10B981', '#059669']}
          glowColor="#10B981"
          loading={isLoading}
        />
        
        <MetricCard
          title="Bags Verified Today"
          value={dashboard?.bagsProcessedToday ?? '-'}
          subtitle={`${dashboard?.bagsProcessedThisWeek ?? 0} this week`}
          icon={<InventoryIcon sx={{ fontSize: 28 }} />}
          trend={trendComparison ? { value: trendComparison.percentChange, label: 'vs yesterday' } : { value: 0, label: 'vs yesterday' }}
          gradient={['#6366F1', '#8B5CF6']}
          glowColor="#6366F1"
          loading={isLoading}
        />

        <MetricCard
          title="Vehicles Online"
          value={`${dashboard?.vehiclesOnline ?? 0}/${dashboard?.totalVehicles ?? 0}`}
          subtitle="GPS active < 15 min"
          icon={<VehicleIcon sx={{ fontSize: 28 }} />}
          gradient={['#8B5CF6', '#A855F7']}
          glowColor="#8B5CF6"
          loading={isLoading}
        />

        <MetricCard
          title="Staff Present"
          value={`${dashboard?.staffPresentToday ?? 0}/${dashboard?.totalStaff ?? 0}`}
          subtitle="Attendance today"
          icon={<BadgeIcon sx={{ fontSize: 28 }} />}
          gradient={['#06B6D4', '#0891B2']}
          glowColor="#06B6D4"
          loading={isLoading}
        />

        <MetricCard
          title="Unpaid Invoices"
          value={dashboard ? formatCurrency(dashboard.pendingInvoiceAmount) : '-'}
          subtitle={`${dashboard?.pendingInvoiceCount ?? 0} pending`}
          icon={<WalletIcon sx={{ fontSize: 28 }} />}
          gradient={['#F59E0B', '#D97706']}
          glowColor="#F59E0B"
          loading={isLoading}
        />

        <MetricCard
          title="Subscription"
          value={dashboard?.subscriptionDaysLeft ?? '-'}
          subtitle={dashboard?.subscriptionDaysLeft !== undefined && dashboard.subscriptionDaysLeft >= 0 ? 'days remaining' : 'Unlimited'}
          icon={<ScheduleIcon sx={{ fontSize: 28 }} />}
          gradient={dashboard?.subscriptionDaysLeft !== undefined && dashboard.subscriptionDaysLeft < 7 
            ? ['#EF4444', '#DC2626'] 
            : ['#22C55E', '#16A34A']}
          glowColor={dashboard?.subscriptionDaysLeft !== undefined && dashboard.subscriptionDaysLeft < 7 
            ? '#EF4444' : '#22C55E'}
          loading={isLoading}
        />
      </Box>

      {/* Secondary Metrics - 4 columns */}
      <Box 
        sx={{ 
          display: 'grid', 
          gridTemplateColumns: { 
            xs: '1fr', 
            sm: 'repeat(2, 1fr)', 
            lg: 'repeat(4, 1fr)' 
          }, 
          gap: 3, 
          mb: 4 
        }}
      >
        <MetricCard
          title="Agreements Expiring"
          value={dashboard?.agreementsExpiringSoon ?? '-'}
          subtitle="Within 30 days"
          icon={<WarningIcon sx={{ fontSize: 26 }} />}
          gradient={['#EF4444', '#DC2626']}
          glowColor="#EF4444"
          loading={isLoading}
        />
        <MetricCard
          title="Anomaly Bags"
          value={dashboard?.anomalyBagsThisWeek ?? '-'}
          subtitle="This week • Click for details"
          icon={<ErrorOutline sx={{ fontSize: 26 }} />}
          gradient={['#F97316', '#EA580C']}
          glowColor="#F97316"
          loading={isLoading}
          clickable={true}
          onClick={() => setAnomalyDialogOpen(true)}
        />
        <MetricCard
          title="Total Revenue"
          value={dashboard ? formatCurrency(dashboard.totalRevenueAllTime) : '-'}
          subtitle="All time"
          icon={<AttachMoney sx={{ fontSize: 26 }} />}
          gradient={['#22C55E', '#16A34A']}
          glowColor="#22C55E"
          loading={isLoading}
        />
        <MetricCard
          title="QR Labels Issued"
          value={dashboard?.totalBagLabelsIssued ?? '-'}
          subtitle="Total generated"
          icon={<QrCodeIcon sx={{ fontSize: 26 }} />}
          gradient={['#3B82F6', '#2563EB']}
          glowColor="#3B82F6"
          loading={isLoading}
        />
      </Box>

      {/* Charts Row */}
      <Box 
        sx={{ 
          display: 'grid', 
          gridTemplateColumns: { xs: '1fr', lg: '1fr 2fr' }, 
          gap: 3, 
          mb: 4 
        }}
      >
        {/* Category Breakdown Pie Chart */}
        {/* Category Breakdown Pie Chart */}
        <Card sx={{ height: '100%' }}>
          <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6" fontWeight={700}>
                Waste by Category
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Weekly distribution
              </Typography>
            </Box>
            
            <Box sx={{ flexGrow: 1, minHeight: 260, position: 'relative' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartCategoryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={2}
                  >
                    {chartCategoryData.map((entry, index) => (
                      <Cell 
                        key={`cell-${index}`} 
                        fill={entry.color}
                        strokeWidth={0}
                      />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{
                      backgroundColor: theme.palette.background.paper,
                      borderColor: theme.palette.divider,
                      borderRadius: 8,
                      boxShadow: theme.shadows[2],
                      color: theme.palette.text.primary,
                    }}
                  />
                  <Legend verticalAlign="bottom" height={36} />
                </PieChart>
              </ResponsiveContainer>
            </Box>
            
            {/* Blue Waste Compliance */}
            <Box sx={{ mt: 3, p: 2, bgcolor: alpha('#3B82F6', 0.04), borderRadius: 2, border: `1px solid ${alpha('#3B82F6', 0.1)}` }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, alignItems: 'center' }}>
                <Typography variant="body2" fontWeight={600} color="primary">
                  Blue Waste Compliance
                </Typography>
                <Chip
                  label={`${bluePercent.toFixed(1)}%`}
                  size="small"
                  color="primary"
                  sx={{ height: 24, fontWeight: 700 }}
                />
              </Box>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(100, Math.max(0, bluePercent))}
                  sx={{
                    height: 8,
                    borderRadius: 4,
                  bgcolor: alpha(theme.palette.primary.main, 0.1),
                }}
              />
            </Box>
          </CardContent>
        </Card>

        {/* Weekly Trend Area Chart */}
        <Card sx={{ height: '100%' }}>
          <CardContent sx={{ p: 3 }}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6" fontWeight={700}>
                Weekly Collection Trend
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Bags processed by category
              </Typography>
            </Box>
            <Box sx={{ height: 320, minHeight: 320, position: 'relative' }}>
              <ResponsiveContainer width="100%" height={320}>
                <AreaChart data={chartTrendData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} vertical={false} />
                  <XAxis 
                    dataKey="date" 
                    stroke={theme.palette.text.secondary}
                    tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis 
                    stroke={theme.palette.text.secondary}
                    tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: theme.palette.background.paper,
                      borderColor: theme.palette.divider,
                      borderRadius: 8,
                      boxShadow: theme.shadows[2],
                    }}
                  />
                  <Legend verticalAlign="top" height={36} />
                  <Area type="monotone" dataKey="yellow" stroke="#FBBF24" fill="#FBBF24" fillOpacity={0.1} strokeWidth={2} />
                  <Area type="monotone" dataKey="red" stroke="#EF4444" fill="#EF4444" fillOpacity={0.1} strokeWidth={2} />
                  <Area type="monotone" dataKey="blue" stroke="#3B82F6" fill="#3B82F6" fillOpacity={0.1} strokeWidth={2} />
                  <Area type="monotone" dataKey="white" stroke="#94A3B8" fill="#94A3B8" fillOpacity={0.1} strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </Box>
          </CardContent>
        </Card>
      </Box>

      {/* Recent Activity & Expiring Agreements */}
      <Box 
        sx={{ 
          display: 'grid', 
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, 
          gap: 3 
        }}
      >
        {/* Recent Activity */}
        <Card 
          sx={{ 
            overflow: 'hidden',
            background: isDark 
              ? 'linear-gradient(135deg, rgba(99,102,241,0.08) 0%, rgba(139,92,246,0.05) 100%)'
              : undefined,
            border: isDark ? '1px solid rgba(99,102,241,0.2)' : undefined,
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                Recent Activity
              </Typography>
            </Box>

            {isLoading ? (
              <Stack spacing={2}>
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} height={60} sx={{ borderRadius: 2 }} />
                ))}
              </Stack>
            ) : dashboard?.recentBagEvents && dashboard.recentBagEvents.length > 0 ? (
              <Box sx={{ maxHeight: 400, overflowY: 'auto', pr: 1 }}>
                <Stack spacing={1.5}>
                  {dashboard.recentBagEvents.map((event, index) => {
                    const category = (event.wasteCategory || 'UNKNOWN').toUpperCase();
                    
                    // Determine color based on category
                    // Default: Purple for dark mode, Green for light mode
                    let color = isDark ? '#6366F1' : '#10B981'; 
                    let bgColor = alpha(color, 0.1);
                    
                    if (category === 'YELLOW') {
                      color = '#F59E0B';
                      bgColor = alpha('#F59E0B', 0.1);
                    } else if (category === 'RED') {
                      color = '#EF4444';
                      bgColor = alpha('#EF4444', 0.1);
                    } else if (category === 'BLUE') {
                      color = '#3B82F6';
                      bgColor = alpha('#3B82F6', 0.1);
                    } else if (category === 'WHITE') {
                      color = '#64748B'; // Slate
                      bgColor = alpha('#64748B', 0.1);
                    }

                    // Explicit timestamp format as requested
                    const timestamp = event.eventTs ? new Date(event.eventTs).toLocaleString('en-US', {
                      month: 'short', day: 'numeric', year: 'numeric',
                      hour: 'numeric', minute: 'numeric', hour12: true
                    }) : '';

                    return (
                      <Box 
                        key={index} 
                        sx={{ 
                          p: 1.5,
                          borderRadius: 2,
                          bgcolor: isDark ? alpha(color, 0.05) : alpha(color, 0.02),
                          border: `1px solid ${alpha(color, 0.1)}`,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 2,
                          transition: 'all 0.2s',
                          '&:hover': {
                            bgcolor: isDark ? alpha(color, 0.1) : alpha(color, 0.05),
                            transform: 'translateX(4px)',
                          }
                        }}
                      >
                        {/* Icon Box */}
                        <Box 
                          sx={{ 
                            width: 40, 
                            height: 40, 
                            borderRadius: '50%', 
                            bgcolor: bgColor,
                            display: 'flex', 
                            alignItems: 'center', 
                            justifyContent: 'center',
                            color: color,
                            flexShrink: 0,
                          }}
                        >
                         {event.eventType === 'PICKUP_COMPLETED' ? (
                           <LocalShipping fontSize="small" />
                         ) : event.eventType === 'BAG_SCANNED' ? (
                           <QrCodeIcon fontSize="small" />
                         ) : (
                           <InventoryIcon fontSize="small" />
                         )}
                        </Box>

                        {/* Content */}
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <Typography variant="body2" fontWeight={600} noWrap>
                               {event.eventType.replace(/_/g, ' ')}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', ml: 1 }}>
                              {timestamp}
                            </Typography>
                          </Box>
                          
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                            <Chip 
                              label={category} 
                              size="small" 
                              sx={{ 
                                height: 20, 
                                fontSize: '0.65rem', 
                                fontWeight: 700,
                                bgcolor: color,
                                color: '#fff',
                              }} 
                            />
                            <Typography variant="caption" color="text.secondary" noWrap>
                               • {event.hcfName || 'Unknown HCF'}
                            </Typography>
                          </Box>
                        </Box>
                      </Box>
                    );
                  })}
                </Stack>
              </Box>
            ) : (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  No recent activity
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>

        {/* Expiring Agreements */}
        <Card 
          sx={{ 
            overflow: 'hidden',
            background: isDark 
              ? 'linear-gradient(135deg, rgba(239,68,68,0.08) 0%, rgba(249,115,22,0.05) 100%)'
              : undefined,
            border: isDark ? '1px solid rgba(239,68,68,0.2)' : undefined,
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
              Agreements Expiring Soon
            </Typography>
            {isLoading ? (
              <Stack spacing={2}>
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} height={50} sx={{ borderRadius: 2 }} />
                ))}
              </Stack>
            ) : dashboard?.expiringAgreements && dashboard.expiringAgreements.length > 0 ? (
              <Stack divider={<Divider sx={{ opacity: 0.5 }} />} spacing={0}>
                {dashboard.expiringAgreements.slice(0, 5).map((agreement, index) => (
                  <Box 
                    key={index} 
                    sx={{ 
                      py: 2,
                      '&:hover': { bgcolor: alpha('#EF4444', 0.05) },
                      borderRadius: 1,
                      px: 1,
                      mx: -1,
                    }}
                  >
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Typography variant="body2" fontWeight={600}>
                        {agreement.hcfName || agreement.agreementNumber}
                      </Typography>
                      <Chip 
                        label={`${agreement.daysUntilExpiry} days`}
                        size="small" 
                        sx={{
                          background: agreement.daysUntilExpiry < 7 
                            ? 'linear-gradient(135deg, #EF4444, #DC2626)'
                            : 'linear-gradient(135deg, #F59E0B, #D97706)',
                          color: '#fff',
                          fontWeight: 600,
                          fontSize: '0.75rem',
                        }}
                      />
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {agreement.agreementNumber} • Expires: {agreement.endDate ? new Date(agreement.endDate).toLocaleDateString() : 'N/A'}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            ) : (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  No agreements expiring soon
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </Box>

      {/* Anomaly Bags Dialog */}
      <Dialog 
        open={anomalyDialogOpen} 
        onClose={() => setAnomalyDialogOpen(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          background: 'linear-gradient(135deg, rgba(249,115,22,0.1) 0%, rgba(234,88,12,0.05) 100%)',
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <ErrorOutline sx={{ color: '#F97316' }} />
            <Typography variant="h6" fontWeight={700}>Anomaly Bags - This Week</Typography>
          </Box>
          <IconButton onClick={() => setAnomalyDialogOpen(false)}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ p: 0 }}>
          {anomalyLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : anomalyBags && anomalyBags.length > 0 ? (
            <TableContainer component={Paper} elevation={0}>
              <Table>
                <TableHead>
                  <TableRow sx={{ bgcolor: alpha('#F97316', 0.08) }}>
                    <TableCell sx={{ fontWeight: 700 }}>Timestamp</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>HCF</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Category</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Anomaly Type</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Weight (kg)</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Staff</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Location</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {anomalyBags.map((bag: any, index: number) => (
                    <TableRow 
                      key={bag.id || index}
                      sx={{ 
                        '&:hover': { bgcolor: alpha('#F97316', 0.05) },
                        '&:nth-of-type(odd)': { bgcolor: alpha('#F97316', 0.02) },
                      }}
                    >
                      <TableCell>
                        <Typography variant="body2">
                          {new Date(bag.eventTs).toLocaleDateString()}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(bag.eventTs).toLocaleTimeString()}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={500}>
                          {bag.hcfName || 'Unknown HCF'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={bag.category || 'Unknown'} 
                          size="small"
                          sx={{
                            bgcolor: bag.category === 'RED' ? alpha('#EF4444', 0.2) :
                                    bag.category === 'YELLOW' ? alpha('#FBBF24', 0.2) :
                                    bag.category === 'BLUE' ? alpha('#3B82F6', 0.2) :
                                    alpha('#94A3B8', 0.2),
                            color: bag.category === 'RED' ? '#DC2626' :
                                   bag.category === 'YELLOW' ? '#B45309' :
                                   bag.category === 'BLUE' ? '#2563EB' :
                                   '#475569',
                            fontWeight: 600,
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={bag.anomalyState?.replace(/_/g, ' ') || 'Unknown'} 
                          size="small"
                          color="warning"
                          variant="outlined"
                          sx={{ fontWeight: 600 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={500}>
                          {bag.weightKg?.toFixed(2) || '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {bag.staffName || bag.collectedByUserId?.substring(0, 8) || 'Unknown'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {bag.gpsLat && bag.gpsLon ? (
                          <IconButton 
                            size="small" 
                            onClick={() => window.open(`https://www.google.com/maps?q=${bag.gpsLat},${bag.gpsLon}`, '_blank')}
                            sx={{ color: '#3B82F6' }}
                          >
                            <OpenInNewIcon fontSize="small" />
                          </IconButton>
                        ) : (
                          <Typography variant="caption" color="text.secondary">N/A</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Box sx={{ py: 8, textAlign: 'center' }}>
              <Typography variant="body1" color="text.secondary">
                🎉 No anomaly bags this week!
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                All bag collections and verifications are within normal parameters.
              </Typography>
            </Box>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default CbwtfDashboard;
