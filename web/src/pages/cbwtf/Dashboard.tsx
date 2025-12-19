import React from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Chip,
  LinearProgress,
  alpha,
} from '@mui/material';
import {
  TrendingUp,
  TrendingDown,
  LocalShipping,
  Delete as WasteIcon,
  Warning as WarningIcon,
  AttachMoney,
} from '@mui/icons-material';
import {
  BarChart,
  Bar,
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

// Metric Card Component
interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: { value: number; label: string };
  color?: string;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
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

// Mock data for charts
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

const hcfPerformanceData = [
  { name: 'City Hospital', waste: 450, target: 500 },
  { name: 'Apollo Clinic', waste: 380, target: 400 },
  { name: 'Metro Health', waste: 320, target: 350 },
  { name: 'Care Plus', waste: 280, target: 300 },
  { name: 'Wellness Center', waste: 220, target: 250 },
];

const CbwtfDashboard: React.FC = () => {

  return (
    <Box>
      {/* Welcome Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Welcome back
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Here's what's happening with your facility today.
        </Typography>
      </Box>

      {/* Metric Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total Waste Collected"
            value="2,845 kg"
            subtitle="Today"
            icon={<WasteIcon />}
            trend={{ value: 12, label: 'vs yesterday' }}
            color="#10B981"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Bags Verified"
            value="342"
            subtitle="Today"
            icon={<LocalShipping />}
            trend={{ value: 8, label: 'vs yesterday' }}
            color="#6366F1"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Active Alerts"
            value="7"
            subtitle="3 critical"
            icon={<WarningIcon />}
            color="#EF4444"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Revenue Outstanding"
            value="₹4.2L"
            subtitle="This month"
            icon={<AttachMoney />}
            trend={{ value: -5, label: 'improved' }}
            color="#F59E0B"
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

      {/* HCF Performance */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Top HCFs by Waste Volume
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={hcfPerformanceData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis type="number" stroke="#94A3B8" />
                  <YAxis dataKey="name" type="category" stroke="#94A3B8" width={120} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      border: '1px solid #334155',
                      borderRadius: 8,
                    }}
                  />
                  <Bar dataKey="waste" fill="#6366F1" radius={[0, 4, 4, 0]} />
                  <Bar dataKey="target" fill="#334155" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default CbwtfDashboard;
