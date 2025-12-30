import React from 'react';
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
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  gradient,
  loading = false,
  glowColor,
}) => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  
  return (
    <Card
      sx={{
        position: 'relative',
        overflow: 'hidden',
        background: isDark 
          ? `linear-gradient(135deg, ${alpha(gradient[0], 0.15)} 0%, ${alpha(gradient[1], 0.08)} 100%)`
          : `linear-gradient(135deg, ${alpha(gradient[0], 0.08)} 0%, ${alpha(gradient[1], 0.03)} 100%)`,
        border: `1px solid ${alpha(gradient[0], isDark ? 0.3 : 0.2)}`,
        backdropFilter: 'blur(10px)',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: glowColor 
            ? `0 20px 40px ${alpha(glowColor, 0.3)}, 0 0 60px ${alpha(glowColor, 0.1)}`
            : `0 20px 40px ${alpha(gradient[0], 0.25)}`,
          border: `1px solid ${alpha(gradient[0], 0.5)}`,
        },
      }}
    >
      {/* Gradient accent line at top */}
      <Box
        sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 4,
          background: `linear-gradient(90deg, ${gradient[0]}, ${gradient[1]})`,
        }}
      />
      
      <CardContent sx={{ p: 3, pt: 3.5 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography 
              variant="body2" 
              sx={{ 
                color: 'text.secondary',
                fontWeight: 500,
                letterSpacing: '0.02em',
                textTransform: 'uppercase',
                fontSize: '0.7rem',
                mb: 1,
              }}
            >
              {title}
            </Typography>
            {loading ? (
              <Skeleton width={80} height={48} sx={{ borderRadius: 1 }} />
            ) : (
              <Typography 
                variant="h3" 
                sx={{ 
                  fontWeight: 800,
                  background: `linear-gradient(135deg, ${gradient[0]} 0%, ${gradient[1]} 100%)`,
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                  letterSpacing: '-0.02em',
                  lineHeight: 1,
                  mb: 0.5,
                }}
              >
                {value}
              </Typography>
            )}
            {subtitle && (
              <Typography 
                variant="caption" 
                sx={{ 
                  color: 'text.secondary',
                  fontWeight: 400,
                  display: 'block',
                  mt: 0.5,
                }}
              >
                {subtitle}
              </Typography>
            )}
            {trend && (
              <Box 
                sx={{ 
                  display: 'inline-flex', 
                  alignItems: 'center', 
                  mt: 1.5, 
                  gap: 0.5,
                  px: 1.5,
                  py: 0.5,
                  borderRadius: 2,
                  bgcolor: trend.value >= 0 
                    ? alpha('#10B981', 0.15) 
                    : alpha('#EF4444', 0.15),
                }}
              >
                {trend.value >= 0 ? (
                  <TrendingUp sx={{ fontSize: 16, color: '#10B981' }} />
                ) : (
                  <TrendingDown sx={{ fontSize: 16, color: '#EF4444' }} />
                )}
                <Typography
                  variant="caption"
                  sx={{ 
                    color: trend.value >= 0 ? '#10B981' : '#EF4444',
                    fontWeight: 600,
                  }}
                >
                  {Math.abs(trend.value)}% {trend.label}
                </Typography>
              </Box>
            )}
          </Box>
          <Box
            sx={{
              width: 56,
              height: 56,
              borderRadius: 3,
              background: `linear-gradient(135deg, ${gradient[0]} 0%, ${gradient[1]} 100%)`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              boxShadow: `0 8px 24px ${alpha(gradient[0], 0.4)}`,
              flexShrink: 0,
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

// Enhanced Risk Alert Component
const RiskAlertCard: React.FC<{ alerts: RiskAlert[] }> = ({ alerts }) => {
  if (alerts.length === 0) return null;

  return (
    <Card 
      sx={{ 
        mb: 4,
        background: 'linear-gradient(135deg, rgba(239,68,68,0.08) 0%, rgba(249,115,22,0.05) 100%)',
        border: '1px solid rgba(239,68,68,0.3)',
      }}
    >
      <CardContent sx={{ p: 3 }}>
        <Typography 
          variant="h6" 
          sx={{ 
            mb: 2, 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            fontWeight: 700,
          }}
        >
          <WarningIcon sx={{ color: '#F59E0B' }} />
          Risk Alerts ({alerts.length})
        </Typography>
        <Stack spacing={2}>
          {alerts.map((alert, index) => (
            <Alert 
              key={index} 
              severity={alert.severity === 'CRITICAL' ? 'error' : 'warning'}
              icon={<ErrorOutline />}
              sx={{
                borderRadius: 2,
                '& .MuiAlert-message': { width: '100%' },
              }}
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

// Chart data
const categoryData = [
  { name: 'Yellow', value: 45, color: '#FBBF24' },
  { name: 'Red', value: 25, color: '#EF4444' },
  { name: 'Blue', value: 20, color: '#3B82F6' },
  { name: 'White', value: 10, color: '#94A3B8' },
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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const { data: dashboard, isLoading, isError, error, refetch } = useQuery<CBWTFDashboardDTO>({
    queryKey: ['cbwtf-dashboard'],
    queryFn: cbwtfApi.getDashboard,
    refetchInterval: 60000,
  });

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
          <Typography 
            variant="h3" 
            sx={{ 
              fontWeight: 800, 
              letterSpacing: '-0.02em',
              background: 'linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #A855F7 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}
          >
            {isLoading ? <Skeleton width={250} /> : 'Welcome back'}
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, fontWeight: 400 }}>
            {isLoading ? <Skeleton width={350} /> : `Real-time overview of ${dashboard?.facilityName || 'your facility'}`}
          </Typography>
        </Box>
        {dashboard && (
          <Chip 
            label={`${dashboard.subscriptionPlan} • ${dashboard.subscriptionDaysLeft >= 0 ? `${dashboard.subscriptionDaysLeft} days left` : 'Unlimited'}`}
            sx={{
              background: dashboard.subscriptionDaysLeft < 7 && dashboard.subscriptionDaysLeft >= 0 
                ? 'linear-gradient(135deg, #EF4444 0%, #F97316 100%)'
                : 'linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%)',
              color: '#fff',
              fontWeight: 600,
              fontSize: '0.85rem',
              height: 36,
              px: 1,
            }}
            icon={<ScheduleIcon sx={{ color: '#fff !important' }} />}
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
          title="Bags Processed Today"
          value={dashboard?.bagsProcessedToday ?? '-'}
          subtitle={`${dashboard?.bagsProcessedThisWeek ?? 0} this week`}
          icon={<InventoryIcon sx={{ fontSize: 28 }} />}
          trend={{ value: 12, label: 'vs yesterday' }}
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
          subtitle="This week"
          icon={<ErrorOutline sx={{ fontSize: 26 }} />}
          gradient={['#F97316', '#EA580C']}
          glowColor="#F97316"
          loading={isLoading}
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
            <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
              Waste by Category
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Weekly distribution
            </Typography>
            <Box sx={{ height: 260 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={90}
                    paddingAngle={3}
                    strokeWidth={0}
                  >
                    {categoryData.map((entry, index) => (
                      <Cell 
                        key={`cell-${index}`} 
                        fill={entry.color}
                        style={{ filter: 'drop-shadow(0 4px 8px rgba(0,0,0,0.2))' }}
                      />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{
                      backgroundColor: isDark ? '#1E293B' : '#fff',
                      border: 'none',
                      borderRadius: 12,
                      boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
                    }}
                    formatter={(value: number) => [`${value}%`, 'Share']}
                  />
                  <Legend 
                    verticalAlign="bottom" 
                    height={36}
                    formatter={(value) => <span style={{ color: isDark ? '#94A3B8' : '#64748B' }}>{value}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            </Box>
            
            {/* Blue Waste Compliance */}
            <Box sx={{ mt: 2, p: 2, bgcolor: alpha('#3B82F6', 0.08), borderRadius: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, alignItems: 'center' }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Blue Waste Compliance
                </Typography>
                <Chip
                  label="20%"
                  size="small"
                  sx={{ 
                    background: 'linear-gradient(135deg, #F59E0B, #D97706)',
                    color: '#fff',
                    fontWeight: 600,
                    height: 24,
                  }}
                />
              </Box>
              <LinearProgress
                variant="determinate"
                value={36}
                sx={{
                  height: 10,
                  borderRadius: 5,
                  bgcolor: alpha('#3B82F6', 0.2),
                  '& .MuiLinearProgress-bar': {
                    background: 'linear-gradient(90deg, #3B82F6, #6366F1)',
                    borderRadius: 5,
                  },
                }}
              />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                Target: 55% | Current: 20%
              </Typography>
            </Box>
          </CardContent>
        </Card>

        {/* Weekly Trend Area Chart */}
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
            <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
              Weekly Collection Trend
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Bags processed by category
            </Typography>
            <Box sx={{ height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trendData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorYellow" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#FBBF24" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#FBBF24" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorRed" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#EF4444" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorBlue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorWhite" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#94A3B8" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#94A3B8" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#334155' : '#E2E8F0'} />
                  <XAxis 
                    dataKey="date" 
                    stroke={isDark ? '#64748B' : '#94A3B8'}
                    tick={{ fill: isDark ? '#94A3B8' : '#64748B' }}
                  />
                  <YAxis 
                    stroke={isDark ? '#64748B' : '#94A3B8'}
                    tick={{ fill: isDark ? '#94A3B8' : '#64748B' }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: isDark ? '#1E293B' : '#fff',
                      border: 'none',
                      borderRadius: 12,
                      boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
                    }}
                  />
                  <Legend 
                    verticalAlign="top" 
                    height={36}
                    formatter={(value) => <span style={{ color: isDark ? '#94A3B8' : '#64748B', textTransform: 'capitalize' }}>{value}</span>}
                  />
                  <Area type="monotone" dataKey="yellow" stroke="#FBBF24" strokeWidth={2} fill="url(#colorYellow)" />
                  <Area type="monotone" dataKey="red" stroke="#EF4444" strokeWidth={2} fill="url(#colorRed)" />
                  <Area type="monotone" dataKey="blue" stroke="#3B82F6" strokeWidth={2} fill="url(#colorBlue)" />
                  <Area type="monotone" dataKey="white" stroke="#94A3B8" strokeWidth={2} fill="url(#colorWhite)" />
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
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
              Recent Activity
            </Typography>
            {isLoading ? (
              <Stack spacing={2}>
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} height={50} sx={{ borderRadius: 2 }} />
                ))}
              </Stack>
            ) : dashboard?.recentBagEvents && dashboard.recentBagEvents.length > 0 ? (
              <Stack divider={<Divider sx={{ opacity: 0.5 }} />} spacing={0}>
                {dashboard.recentBagEvents.slice(0, 5).map((event, index) => (
                  <Box 
                    key={index} 
                    sx={{ 
                      py: 2,
                      '&:hover': { bgcolor: alpha('#6366F1', 0.05) },
                      borderRadius: 1,
                      px: 1,
                      mx: -1,
                    }}
                  >
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Typography variant="body2" fontWeight={600}>
                        {event.qrCode || 'Unknown QR'}
                      </Typography>
                      <Chip 
                        label={event.eventType.replace('_', ' ')} 
                        size="small" 
                        sx={{
                          background: event.anomalyState && event.anomalyState !== 'NONE' 
                            ? 'linear-gradient(135deg, #EF4444, #DC2626)'
                            : 'linear-gradient(135deg, #6366F1, #8B5CF6)',
                          color: '#fff',
                          fontWeight: 600,
                          fontSize: '0.7rem',
                        }}
                      />
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {event.hcfName || 'Unknown HCF'} • {new Date(event.eventTs).toLocaleString()}
                    </Typography>
                  </Box>
                ))}
              </Stack>
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
    </Box>
  );
};

export default CbwtfDashboard;
