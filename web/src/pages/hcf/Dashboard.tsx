import React from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  alpha,
  List,
  ListItem,
  ListItemText,
  Divider,
  Chip,
  CircularProgress,
  LinearProgress,
} from '@mui/material';
import {
  LocalShipping,
  Delete as WasteIcon,
  Receipt,
  CheckCircle,
} from '@mui/icons-material';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../api/client';

// Metric Card
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

interface DashboardStats {
  todayWaste: number;
  monthPickups: number;
  duesStatus: string;
  duesMessage: string;
  complianceScore: number;
  recentPickups: {
    date: string;
    bags: number;
    weight: string;
    status: string;
  }[];
  categorySplit: Record<string, number>;
  dailyTrend: Array<Record<string, string | number>>;
  blueCompliance: number;
}

const COLORS = {
  BLUE: '#3B82F6',
  RED: '#EF4444',
  WHITE: '#94A3B8',
  YELLOW: '#EAB308',
};

const CATEGORY_COLORS: Record<string, string> = {
  BLUE: COLORS.BLUE,
  RED: COLORS.RED,
  WHITE: COLORS.WHITE,
  YELLOW: COLORS.YELLOW,
};

interface ChartTooltipEntry {
  name: string;
  value: number;
  color?: string;
  payload: {
    fullDate?: string;
  };
}

interface ChartTooltipProps {
  active?: boolean;
  payload?: ChartTooltipEntry[];
  label?: string;
}

const CustomTooltip = ({ active, payload, label }: ChartTooltipProps) => {
  if (active && payload && payload.length) {
    return (
      <Box sx={{ bgcolor: '#1E293B', p: 2, borderRadius: 2, border: '1px solid #334155' }}>
        <Typography variant="subtitle2" sx={{ color: '#F8FAFC', mb: 1 }}>{payload[0].payload.fullDate || label}</Typography>
        {payload.map((entry) => (
          <Box key={entry.name} sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
            <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: entry.color, mr: 1 }} />
            <Typography variant="caption" sx={{ color: '#CBD5E1', minWidth: 60 }}>
              {entry.name}:
            </Typography>
            <Typography variant="caption" sx={{ color: '#F8FAFC', fontWeight: 600 }}>
              {entry.value.toFixed(1)} kg
            </Typography>
          </Box>
        ))}
      </Box>
    );
  }
  return null;
};

const HcfDashboard: React.FC = () => {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['hcf-dashboard'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/dashboard');
      return res.data as DashboardStats;
    },
    refetchInterval: 30000, 
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  const recentList = stats?.recentPickups || [];
  
  // Transform category map to array for Pie
  const pieData = stats?.categorySplit 
    ? Object.entries(stats.categorySplit).map(([name, value]) => ({ name, value }))
    : [];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Your Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Real-time waste analytics
        </Typography>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Today's Waste"
            value={`${stats?.todayWaste?.toFixed(1) || '0.0'} kg`}
            subtitle={new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'short' })}
            icon={<WasteIcon />}
            color="#10B981"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Total Bags"
            value={stats?.monthPickups || 0}
            subtitle="This month"
            icon={<LocalShipping />}
            color="#6366F1"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Dues Status"
            value={stats?.duesStatus || 'Unknown'}
            subtitle={stats?.duesMessage}
            icon={<Receipt />}
            color={stats?.duesStatus === 'Dues Clear' ? '#10B981' : '#F59E0B'}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard
            title="Compliance Score"
            value={`${stats?.complianceScore || 100}%`}
            subtitle="Overall"
            icon={<CheckCircle />}
            color={stats?.complianceScore && stats.complianceScore < 80 ? '#F59E0B' : '#10B981'}
          />
        </Grid>
      </Grid>

      {/* Advanced Charts */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Waste by Category (Donut) */}
        <Grid item xs={12} md={5}>
          <Card sx={{ height: '100%', minHeight: 420, display: 'flex', flexDirection: 'column' }}>
            <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Typography variant="h6" gutterBottom>Waste by Category</Typography>
              <Typography variant="caption" color="text.secondary">Last 30 days distribution</Typography>
              
              <Box sx={{ height: 260, position: 'relative' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="40%"
                      innerRadius={55}
                      outerRadius={75}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={CATEGORY_COLORS[entry.name] || '#888'} />
                      ))}
                    </Pie>
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: 8 }}
                      itemStyle={{ color: '#F8FAFC' }}
                    />
                    <Legend verticalAlign="bottom" height={36} />
                  </PieChart>
                </ResponsiveContainer>
              </Box>

              {/* Blue Waste Compliance */}
              <Box sx={{ mt: 2, bgcolor: alpha(COLORS.BLUE, 0.1), p: 2, borderRadius: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Blue Waste Compliance</Typography>
                      <Chip label={`${stats?.blueCompliance || 0}%`} size="small" color="primary" />
                  </Box>
                  <LinearProgress 
                    variant="determinate" 
                    value={stats?.blueCompliance || 0} 
                    sx={{ height: 8, borderRadius: 4, bgcolor: alpha(COLORS.BLUE, 0.2), '& .MuiLinearProgress-bar': { bgcolor: COLORS.BLUE } }}
                  />
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                      Target: 95% | Current: {stats?.blueCompliance || 0}%
                  </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Weekly Trend (Stacked Area) */}
        <Grid item xs={12} md={7}>
          <Card sx={{ height: '100%', minHeight: 420 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Weekly Collection Trend</Typography>
              <Typography variant="caption" color="text.secondary">Bags processed by category (Last 7 days)</Typography>
              
              <Box sx={{ height: 340, mt: 2 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={stats?.dailyTrend || []} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorBlue" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={COLORS.BLUE} stopOpacity={0.3}/>
                        <stop offset="95%" stopColor={COLORS.BLUE} stopOpacity={0}/>
                      </linearGradient>
                      <linearGradient id="colorRed" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={COLORS.RED} stopOpacity={0.3}/>
                        <stop offset="95%" stopColor={COLORS.RED} stopOpacity={0}/>
                      </linearGradient>
                      <linearGradient id="colorYellow" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={COLORS.YELLOW} stopOpacity={0.3}/>
                        <stop offset="95%" stopColor={COLORS.YELLOW} stopOpacity={0}/>
                      </linearGradient>
                      <linearGradient id="colorWhite" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={COLORS.WHITE} stopOpacity={0.3}/>
                        <stop offset="95%" stopColor={COLORS.WHITE} stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={true} />
                    <XAxis dataKey="day" stroke="#94A3B8" />
                    <YAxis stroke="#94A3B8" />
                    <Tooltip content={<CustomTooltip />} />
                    <Area type="monotone" dataKey="YELLOW" stackId="1" stroke={COLORS.YELLOW} fill="url(#colorYellow)" strokeWidth={2} />
                    <Area type="monotone" dataKey="RED" stackId="1" stroke={COLORS.RED} fill="url(#colorRed)" strokeWidth={2} />
                    <Area type="monotone" dataKey="WHITE" stackId="1" stroke={COLORS.WHITE} fill="url(#colorWhite)" strokeWidth={2} />
                    <Area type="monotone" dataKey="BLUE" stackId="1" stroke={COLORS.BLUE} fill="url(#colorBlue)" strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Pickups List */}
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Recent Pickups History
              </Typography>
              {recentList.length === 0 ? (
                 <Box sx={{ display: 'flex', height: '100px', alignItems: 'center', justifyContent: 'center' }}>
                    <Typography color="text.secondary">No recent pickups</Typography>
                 </Box>
              ) : (
                <List>
                  {recentList.map((pickup, index) => (
                    <React.Fragment key={index}>
                      <ListItem>
                        <ListItemText
                          primary={new Date(pickup.date).toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' })}
                          secondary={`${pickup.bags} bags collected • Total Weight: ${pickup.weight}`}
                        />
                         <Chip
                          label={pickup.status}
                          color={pickup.status === 'Verified' ? 'success' : 'warning'}
                          variant="outlined"
                        />
                      </ListItem>
                      {index < recentList.length - 1 && <Divider component="li" />}
                    </React.Fragment>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default HcfDashboard;
