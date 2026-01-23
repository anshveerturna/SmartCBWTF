import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Tabs,
  Tab,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import {
  Download as DownloadIcon,
  Lock as LockIcon,
  Pending as PendingIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import apiClient from '../../api/client';
import { format, isFuture, startOfMonth } from 'date-fns';

interface PickupEvent {
  timestamp: string;
  category: string;
  weight: number;
  bagSerial: string;
}

interface DailyData {
  date: string;
  totalWeight: number;
  qrGenerated: number;
  categoryWeights: Record<string, number>;
  pickups: PickupEvent[];
}

interface MonthlyData {
  period: string;
  totalWeight: number;
  accessStatus: 'NONE' | 'PENDING' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
}

interface DuesStatus {
  status: 'PENDING' | 'REQUESTED' | 'CLEARED';
}

export default function ComplianceReports() {
  const [tabIndex, setTabIndex] = useState(0);
  const [selectedDate, setSelectedDate] = useState<Date | null>(new Date());
  
  // Monthly Report State
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [downloading, setDownloading] = useState(false);

  const queryClient = useQueryClient();

  // 1. Fetch Dues Status (Global)
  const { data: duesStatus } = useQuery<DuesStatus>({
    queryKey: ['hcf-dues-status'],
    queryFn: () => apiClient.get('/api/hcf/compliance/status').then((res: any) => res.data),
  });

  // 2. Fetch Daily Data
  const { data: dailyData, isLoading: loadingDaily } = useQuery<DailyData>({
    queryKey: ['hcf-daily-compliance', selectedDate],
    queryFn: () => apiClient.get('/api/hcf/compliance/daily', {
      params: { date: selectedDate ? format(selectedDate, 'yyyy-MM-dd') : undefined }
    }).then((res: any) => res.data),
    enabled: !!selectedDate && tabIndex === 0
  });

  // 3. Fetch Monthly Data & Access Status
  const { data: monthlyData, isLoading: loadingMonthly, refetch: refetchMonthly } = useQuery<MonthlyData>({
    queryKey: ['hcf-monthly-compliance', selectedYear, selectedMonth],
    queryFn: () => apiClient.get('/api/hcf/compliance/monthly', {
        params: { year: selectedYear, month: selectedMonth }
    }).then((res: any) => res.data),
    enabled: tabIndex === 1,
    retry: false
  });

  // 4. Request Access Mutation
  const requestAccessMutation = useMutation({
    mutationFn: (payload: { month: number, year: number }) => apiClient.post('/api/hcf/compliance/request-access', payload),
    onSuccess: () => {
      refetchMonthly();
      queryClient.invalidateQueries({ queryKey: ['hcf-dues-status'] }); // Optional update
    },
    onError: (err: any) => {
        alert(err.response?.data?.message || 'Failed to request access.');
    }
  });

  // 5. Download Report
  const handleDownloadReport = async () => {
    try {
      setDownloading(true);
      const res = await apiClient.get('/api/hcf/compliance/monthly-report/pdf', {
        params: { year: selectedYear, month: selectedMonth },
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Monthly_Compliance_Report_${selectedYear}_${selectedMonth}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      console.error('Failed to download report', err);
      alert('Failed to generate report. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  const isFutureMonth = isFuture(startOfMonth(new Date(selectedYear, selectedMonth - 1)));
  
  // Render Access Control UI for Monthly Tab
  const renderAccessControl = () => {
      if (loadingMonthly) return <CircularProgress size={24} />;
      if (isFutureMonth) return <Alert severity="info">Compliance reports for future months are not available.</Alert>;
      
      const status = monthlyData?.accessStatus || 'NONE';

      if (status === 'APPROVED') {
          return (
            <Button 
                variant="contained" 
                color="success"
                startIcon={downloading ? <CircularProgress size={20} color="inherit" /> : <DownloadIcon />}
                onClick={handleDownloadReport}
                disabled={downloading}
                fullWidth
            >
                {downloading ? 'Downloading...' : 'Download PDF Report'}
            </Button>
          );
      }

      if (status === 'PENDING' || status === 'SUBMITTED') {
          return (
              <Alert severity="warning" icon={<PendingIcon />}>
                  Access Request Pending. Awaiting approval from CBWTF.
              </Alert>
          );
      }

      if (status === 'REJECTED') {
          return (
              <Box>
                <Alert severity="error" sx={{ mb: 2 }}>
                    Access Request Rejected. Please clear dues and try again.
                </Alert>
                <Button 
                    variant="contained" 
                    onClick={() => requestAccessMutation.mutate({ month: selectedMonth, year: selectedYear })}
                    disabled={requestAccessMutation.isPending}
                >
                    Request Access Again
                </Button>
              </Box>
          );
      }

      return (
          <Box>
            <Alert severity="info" sx={{ mb: 2 }} icon={<LockIcon />}>
                Access to this monthly report is locked. Please request access.
            </Alert>
            <Button 
                variant="contained" 
                color="primary"
                onClick={() => requestAccessMutation.mutate({ month: selectedMonth, year: selectedYear })}
                disabled={requestAccessMutation.isPending}
            >
                {requestAccessMutation.isPending ? 'Requesting...' : 'Request Access'}
            </Button>
          </Box>
      );
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" fontWeight={700}>
          Compliance Reports
        </Typography>
        <Typography variant="body2" color="text.secondary">
          View daily waste collection, QR generation, and download detailed reports.
        </Typography>
      </Box>

      {/* Tabs */}
      <Tabs value={tabIndex} onChange={(_, v) => setTabIndex(v)} sx={{ mb: 3 }}>
        <Tab label="Daily Report" />
        <Tab label="Monthly Report" />
        <Tab label="Yearly Report" />
      </Tabs>

      {/* DAILY TAB */}
      {tabIndex === 0 && (
        <Grid container spacing={3}>
          {/* Controls */}
          <Grid item xs={12}>
            <Card sx={{ p: 2 }}>
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <DatePicker
                  label="Select Date"
                  value={selectedDate}
                  onChange={(newValue) => setSelectedDate(newValue)}
                  slotProps={{ textField: { size: 'small' } }}
                  maxDate={new Date()}
                />
              </LocalizationProvider>
            </Card>
          </Grid>

          {loadingDaily ? (
            <Grid item xs={12} sx={{ textAlign: 'center', py: 5 }}>
              <CircularProgress />
            </Grid>
          ) : dailyData ? (
            <>
              {/* Summary Cards */}
              <Grid item xs={12} md={4}>
                <Card sx={{ height: '100%', bgcolor: 'primary.light', color: 'primary.contrastText' }}>
                  <CardContent>
                    <Typography variant="overline" sx={{ opacity: 0.8 }}>Total Waste Collected</Typography>
                    <Typography variant="h3" fontWeight={700}>
                      {dailyData.totalWeight.toFixed(2)} <span style={{ fontSize: '1rem' }}>kg</span>
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={4}>
                <Card sx={{ height: '100%', bgcolor: 'secondary.light', color: 'secondary.contrastText' }}>
                  <CardContent>
                    <Typography variant="overline" sx={{ opacity: 0.8 }}>QR Labels Generated</Typography>
                    <Typography variant="h3" fontWeight={700}>{dailyData.qrGenerated}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={4}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>Category Breakdown</Typography>
                    {Object.entries(dailyData.categoryWeights).map(([cat, weight]) => (
                      <Box key={cat} sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2">{cat}</Typography>
                        <Typography variant="body2" fontWeight={600}>{weight.toFixed(2)} kg</Typography>
                      </Box>
                    ))}
                    {Object.keys(dailyData.categoryWeights).length === 0 && (
                      <Typography variant="body2" color="text.secondary">No data</Typography>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* Pickup History Table */}
              <Grid item xs={12}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>Pickup Timeline</Typography>
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Time</TableCell>
                            <TableCell>Category</TableCell>
                            <TableCell>Bag Serial</TableCell>
                            <TableCell align="right">Weight (kg)</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {dailyData.pickups.map((pickup, idx) => (
                            <TableRow key={idx}>
                              <TableCell>{format(new Date(pickup.timestamp), 'HH:mm:ss')}</TableCell>
                              <TableCell>
                                <Chip label={pickup.category} size="small" 
                                  color={
                                    pickup.category === 'RED' ? 'error' : 
                                    pickup.category === 'YELLOW' ? 'warning' : 
                                    pickup.category === 'BLUE' ? 'primary' : 'default'
                                  } 
                                />
                              </TableCell>
                              <TableCell>{pickup.bagSerial}</TableCell>
                              <TableCell align="right" sx={{ fontWeight: 600 }}>{pickup.weight.toFixed(2)}</TableCell>
                            </TableRow>
                          ))}
                          {dailyData.pickups.length === 0 && (
                            <TableRow>
                              <TableCell colSpan={4} align="center">No pickups recorded for this date.</TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              </Grid>
            </>
          ) : null}
        </Grid>
      )}

      {/* MONTHLY TAB */}
      {tabIndex === 1 && (
        <Box>
            <Card sx={{ p: 4, mb: 3 }}>
                <Typography variant="h6" gutterBottom>Monthly Compliance Report</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
                    Select a month and year to download the detailed compliance report in PDF format.
                </Typography>

                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} md={3}>
                    <FormControl fullWidth size="small">
                        <InputLabel>Year</InputLabel>
                        <Select
                        value={selectedYear}
                        label="Year"
                        onChange={(e) => setSelectedYear(Number(e.target.value))}
                        >
                        {[2024, 2025, 2026].map(y => (
                            <MenuItem key={y} value={y}>{y}</MenuItem>
                        ))}
                        </Select>
                    </FormControl>
                    </Grid>
                    <Grid item xs={12} md={3}>
                    <FormControl fullWidth size="small">
                        <InputLabel>Month</InputLabel>
                        <Select
                        value={selectedMonth}
                        label="Month"
                        onChange={(e) => setSelectedMonth(Number(e.target.value))}
                        >
                        {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                            <MenuItem key={m} value={m}>
                                {format(new Date(2024, m - 1, 1), 'MMMM')}
                            </MenuItem>
                        ))}
                        </Select>
                    </FormControl>
                    </Grid>
                    
                    <Grid item xs={12} md={6}>
                         {/* Dynamic Action Button/Status */}
                         {renderAccessControl()}
                    </Grid>
                </Grid>
                
                {monthlyData && (
                    <Box sx={{ mt: 3, pt: 3, borderTop: '1px solid #eee' }}>
                        <Typography variant="overline" color="text.secondary">Available Data Preview</Typography>
                        <Typography variant="h4" fontWeight={600}>
                            {monthlyData.totalWeight.toFixed(2)} <span style={{ fontSize: '1rem' }}>kg Total</span>
                        </Typography>
                    </Box>
                )}
            </Card>
        </Box>
      )}

      {/* YEARLY TAB (Placeholder) */}
      {tabIndex === 2 && (
          <Alert severity="info" icon={<LockIcon />}>Yearly reports coming soon.</Alert>
      )}
    </Box>
  );
}
