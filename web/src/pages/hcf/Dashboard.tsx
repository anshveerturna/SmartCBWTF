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
} from 'recharts';

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

// Mock pickup history
const recentPickups = [
  { id: 1, date: '2025-12-19', bags: 12, weight: '45.2 kg', status: 'Verified' },
  { id: 2, date: '2025-12-18', bags: 8, weight: '32.1 kg', status: 'Verified' },
  { id: 3, date: '2025-12-17', bags: 15, weight: '58.7 kg', status: 'Verified' },
  { id: 4, date: '2025-12-16', bags: 10, weight: '41.3 kg', status: 'Verified' },
];

// Mock trend data
const wasteData = [
  { week: 'W1', waste: 120 },
  { week: 'W2', waste: 145 },
  { week: 'W3', waste: 132 },
  { week: 'W4', waste: 158 },
];

const HcfDashboard: React.FC = () => {

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Your Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Waste management overview for your facility
        </Typography>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="This Month Waste"
            value="285 kg"
            subtitle="December 2025"
            icon={<WasteIcon />}
            color="#10B981"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Total Pickups"
            value="45"
            subtitle="This month"
            icon={<LocalShipping />}
            color="#6366F1"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Pending Invoice"
            value="₹12,450"
            subtitle="Due Dec 31"
            icon={<Receipt />}
            color="#F59E0B"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard
            title="Compliance Score"
            value="94%"
            subtitle="Excellent"
            icon={<CheckCircle />}
            color="#10B981"
          />
        </Grid>
      </Grid>

      {/* Charts & Lists */}
      <Grid container spacing={3}>
        {/* Waste Trend */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ height: 350 }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Monthly Waste Trend
              </Typography>
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={wasteData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="week" stroke="#94A3B8" />
                  <YAxis stroke="#94A3B8" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      border: '1px solid #334155',
                      borderRadius: 8,
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="waste"
                    stroke="#6366F1"
                    fill={alpha('#6366F1', 0.2)}
                    strokeWidth={2}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Pickups */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ height: 350 }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Recent Pickups
              </Typography>
              <List disablePadding>
                {recentPickups.map((pickup, index) => (
                  <React.Fragment key={pickup.id}>
                    <ListItem disablePadding sx={{ py: 1 }}>
                      <ListItemText
                        primary={pickup.date}
                        secondary={`${pickup.bags} bags • ${pickup.weight}`}
                        primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                        secondaryTypographyProps={{ variant: 'caption' }}
                      />
                      <Chip
                        label={pickup.status}
                        size="small"
                        color="success"
                        sx={{ height: 20, fontSize: '0.65rem' }}
                      />
                    </ListItem>
                    {index < recentPickups.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default HcfDashboard;
