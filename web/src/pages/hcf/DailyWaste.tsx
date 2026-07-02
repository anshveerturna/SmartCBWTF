import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Paper,
  alpha,
} from '@mui/material';
import {
  CalendarMonth,
  Scale,
  Category,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface WasteEvent {
  id: string;
  eventType: string;
  timestamp: string;
  category: string;
  weightKg: number;
  anomalyState: string;
}

interface DailyWasteResponse {
  date: string;
  totalEvents: number;
  totalWeightKg: number;
  byCategory: Record<string, { count: number; weightKg: number }>;
  eventLimit: number;
  events: WasteEvent[];
}

interface WeekSummaryResponse {
  startDate: string;
  endDate: string;
  days: Array<{
    date: string;
    dayOfWeek: string;
    eventCount: number;
    totalWeightKg: number;
  }>;
}

// Category colors matching CPCB norms
const CATEGORY_COLORS: Record<string, string> = {
  YELLOW: '#FFEB3B',
  RED: '#F44336',
  BLUE: '#2196F3',
  WHITE: '#9E9E9E',
};

const DailyWaste: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });

  // Fetch daily waste data
  const { data: dailyData, isLoading: dailyLoading } = useQuery({
    queryKey: ['hcf-waste-daily', selectedDate],
    queryFn: async () => {
      const res = await apiClient.get(`/api/hcf/waste/daily?date=${selectedDate}`);
      return res.data as DailyWasteResponse;
    },
  });

  // Fetch week summary
  const { data: weekData, isLoading: weekLoading } = useQuery({
    queryKey: ['hcf-waste-week'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/waste/week-summary');
      return res.data as WeekSummaryResponse;
    },
  });

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Daily Waste Data
        </Typography>
        <Typography variant="body1" color="text.secondary">
          View waste collection records by date
        </Typography>
      </Box>

      {/* Week Summary */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Last 7 Days Overview
          </Typography>
          {weekLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Grid container spacing={1}>
              {weekData?.days.map((day) => (
                <Grid item xs key={day.date}>
                  <Paper
                    onClick={() => setSelectedDate(day.date)}
                    sx={{
                      p: 1.5,
                      textAlign: 'center',
                      cursor: 'pointer',
                      bgcolor: day.date === selectedDate ? 'primary.main' : 'background.paper',
                      color: day.date === selectedDate ? 'white' : 'text.primary',
                      '&:hover': {
                        bgcolor: day.date === selectedDate ? 'primary.dark' : alpha('#6366F1', 0.1),
                      },
                    }}
                  >
                    <Typography variant="caption" display="block">
                      {day.dayOfWeek.slice(0, 3)}
                    </Typography>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {day.totalWeightKg.toFixed(1)}
                    </Typography>
                    <Typography variant="caption" color={day.date === selectedDate ? 'inherit' : 'text.secondary'}>
                      kg
                    </Typography>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          )}
        </CardContent>
      </Card>

      {/* Date Selector & Summary */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={4}>
          <TextField
            type="date"
            label="Select Date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            fullWidth
            InputLabelProps={{ shrink: true }}
            InputProps={{
              startAdornment: <CalendarMonth sx={{ mr: 1, color: 'text.secondary' }} />,
            }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Scale sx={{ fontSize: 40, color: 'primary.main' }} />
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Weight
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                  {dailyData?.totalWeightKg?.toFixed(2) || '0'} kg
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Category sx={{ fontSize: 40, color: 'secondary.main' }} />
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Events
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                  {dailyData?.totalEvents || 0}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Category Breakdown */}
      {dailyData?.byCategory && Object.keys(dailyData.byCategory).length > 0 && (
        <Card sx={{ mb: 4 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Category Breakdown
            </Typography>
            <Grid container spacing={2}>
              {Object.entries(dailyData.byCategory).map(([category, data]) => (
                <Grid item xs={6} sm={3} key={category}>
                  <Paper
                    sx={{
                      p: 2,
                      borderLeft: `4px solid ${CATEGORY_COLORS[category] || '#9E9E9E'}`,
                    }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      {category}
                    </Typography>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {data.weightKg.toFixed(2)} kg
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {data.count} bags
                    </Typography>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Events Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Collection Events for {selectedDate}
          </Typography>
          {dailyData && dailyData.totalEvents > dailyData.events.length && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Showing latest {dailyData.events.length} of {dailyData.totalEvents}
            </Typography>
          )}

          {dailyLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : dailyData?.events?.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
              <CalendarMonth sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
              <Typography color="text.secondary">
                No waste collection events for this date.
              </Typography>
            </Paper>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Time</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Weight (kg)</TableCell>
                    <TableCell>Event Type</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dailyData?.events?.map((event) => (
                    <TableRow key={event.id} hover>
                      <TableCell>
                        {new Date(event.timestamp).toLocaleTimeString()}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box
                            sx={{
                              width: 12,
                              height: 12,
                              borderRadius: 0.5,
                              bgcolor: CATEGORY_COLORS[event.category] || '#9E9E9E',
                              border: event.category === 'WHITE' ? '1px solid #999' : 'none',
                            }}
                          />
                          {event.category}
                        </Box>
                      </TableCell>
                      <TableCell>{event.weightKg.toFixed(3)}</TableCell>
                      <TableCell>
                        <Chip
                          label={event.eventType.replace('_', ' ')}
                          size="small"
                          color={event.eventType === 'CBWTF_VERIFICATION' ? 'success' : 'primary'}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={event.anomalyState}
                          size="small"
                          color={event.anomalyState === 'OK' ? 'success' : 'warning'}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default DailyWaste;
