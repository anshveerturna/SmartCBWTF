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
  CircularProgress
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import {
  Download as DownloadIcon,
  Lock as LockIcon,
  Pending as PendingIcon
} from '@mui/icons-material';
import apiClient from '../../api/client';
import { format } from 'date-fns';

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

interface DuesStatus {
  status: 'PENDING' | 'REQUESTED' | 'CLEARED';
  lastRequestStatus?: string;
  lastRequestDate?: string;
  rejectionReason?: string;
  outstandingDues?: number;
}

export default function ComplianceReports() {
  const [tabIndex, setTabIndex] = useState(0);
  const [selectedDate, setSelectedDate] = useState<Date | null>(new Date());
  // Removed unused monthly/yearly state for now
  const queryClient = useQueryClient();

  // 1. Fetch Dues Status
  const { data: duesStatus, refetch: refetchStatus } = useQuery<DuesStatus>({
    queryKey: ['hcf-dues-status'],
    queryFn: () => apiClient.get('/api/hcf/compliance/status').then((res: any) => res.data),
  });

  // 2. Fetch Daily Data
  const { data: dailyData, isLoading: loadingDaily } = useQuery<DailyData>({
    queryKey: ['hcf-daily-compliance', selectedDate],
    queryFn: () => apiClient.get('/api/hcf/compliance/daily', {
      params: { date: selectedDate ? format(selectedDate, 'yyyy-MM-dd') : undefined }
    }).then((res: any) => res.data),
    enabled: !!selectedDate
  });

  // 3. Request Access Mutation
  const requestAccessMutation = useMutation({
    mutationFn: () => apiClient.post('/api/hcf/compliance/request-access'),
    onSuccess: () => {
      refetchStatus();
      queryClient.invalidateQueries({ queryKey: ['hcf-dues-status'] });
    }
  });

  // 4. Monthly/Yearly Data (Placeholder execution as it depends on status)
  const isDuesCleared = duesStatus?.status === 'CLEARED';

  const renderStatusBanner = () => {
    if (isDuesCleared) return null;

    // Success state - after successful request submission
    if (requestAccessMutation.isSuccess) {
      return (
        <Alert severity="success" sx={{ mb: 3 }} icon={<PendingIcon />}>
          <Typography variant="subtitle2" fontWeight={600}>Request Submitted Successfully!</Typography>
          <Typography variant="body2">
            Please ensure all your dues are cleared. If verified by CBWTF, your request will be approved shortly.
          </Typography>
        </Alert>
      );
    }

    // Already requested - pending approval
    if (duesStatus?.status === 'REQUESTED') {
      return (
        <Alert severity="warning" sx={{ mb: 3 }} icon={<PendingIcon />}>
          <Typography variant="subtitle2" fontWeight={600}>Access Request Pending</Typography>
          <Typography variant="body2">
            Your request is awaiting verification by CBWTF and approval from management.
          </Typography>
        </Alert>
      );
    }

    // Last request was rejected - show error with reason
    if (duesStatus?.lastRequestStatus === 'REJECTED') {
      return (
        <Alert 
          severity="error" 
          sx={{ mb: 3 }}
          action={
            <Button 
              variant="outlined" 
              size="small" 
              color="inherit"
              onClick={() => requestAccessMutation.mutate()}
              disabled={requestAccessMutation.isPending}
            >
              Request Again
            </Button>
          }
        >
          <Typography variant="subtitle2" fontWeight={600}>Access Request Rejected</Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            {duesStatus.rejectionReason || 'Please clear your pending dues and try again.'}
          </Typography>
          {duesStatus.outstandingDues && (
             <Typography variant="body2" fontWeight={600} sx={{ mt: 1 }}>
               Outstanding Dues: ₹{duesStatus.outstandingDues.toLocaleString()}
             </Typography>
          )}
        </Alert>
      );
    }

    // Default - can request access
    return (
      <Alert 
        severity="info" 
        sx={{ mb: 3 }}
        action={
          <Button 
            variant="contained" 
            size="small" 
            onClick={() => requestAccessMutation.mutate()}
            disabled={requestAccessMutation.isPending}
          >
            {requestAccessMutation.isPending ? 'Submitting...' : 'Request Access'}
          </Button>
        }
      >
        <Typography variant="subtitle2" fontWeight={600}>Monthly & Yearly Reports Locked</Typography>
        <Typography variant="body2">
          Request access to view monthly and yearly compliance reports. Ensure your dues are cleared.
        </Typography>
      </Alert>
    );
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" fontWeight={700}>
          Compliance Reports
        </Typography>
        <Typography variant="body2" color="text.secondary">
          View daily waste collection, QR generation, and pickup history.
        </Typography>
      </Box>

      {/* Tabs */}
      <Tabs value={tabIndex} onChange={(_, v) => setTabIndex(v)} sx={{ mb: 3 }}>
        <Tab label="Daily Report" />
        <Tab label="Monthly Report" icon={!isDuesCleared ? <LockIcon fontSize="small" /> : undefined} iconPosition="end" />
        <Tab label="Yearly Report" icon={!isDuesCleared ? <LockIcon fontSize="small" /> : undefined} iconPosition="end" />
      </Tabs>

      {/* DAILY TAB */}
      {tabIndex === 0 && (
        <Grid container spacing={3}>
          {/* Controls */}
          <Grid size={{ xs: 12 }}>
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
            <Grid size={{ xs: 12 }} sx={{ textAlign: 'center', py: 5 }}>
              <CircularProgress />
            </Grid>
          ) : dailyData ? (
            <>
              {/* Summary Cards */}
              <Grid size={{ xs: 12, md: 4 }}>
                <Card sx={{ height: '100%', bgcolor: 'primary.light', color: 'primary.contrastText' }}>
                  <CardContent>
                    <Typography variant="overline" sx={{ opacity: 0.8 }}>Total Waste Collected</Typography>
                    <Typography variant="h3" fontWeight={700}>
                      {dailyData.totalWeight.toFixed(2)} <span style={{ fontSize: '1rem' }}>kg</span>
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Card sx={{ height: '100%', bgcolor: 'secondary.light', color: 'secondary.contrastText' }}>
                  <CardContent>
                    <Typography variant="overline" sx={{ opacity: 0.8 }}>QR Labels Generated</Typography>
                    <Typography variant="h3" fontWeight={700}>{dailyData.qrGenerated}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
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
              <Grid size={{ xs: 12 }}>
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

      {/* MONTHLY & YEARLY TABS (LOCKED) */}
      {(tabIndex === 1 || tabIndex === 2) && (
        <Box>
          {renderStatusBanner()}
          
          <Box sx={{ 
            opacity: isDuesCleared ? 1 : 0.4, 
            pointerEvents: isDuesCleared ? 'auto' : 'none',
            filter: isDuesCleared ? 'none' : 'grayscale(100%)'
          }}>
             {/* Simple Placeholder for Monthly/Yearly View - can be expanded */}
             <Card sx={{ p: 5, textAlign: 'center' }}>
                <Typography variant="h5" gutterBottom>
                  {tabIndex === 1 ? 'Monthly Compliance Report' : 'Yearly Compliance Report'}
                </Typography>
                <Typography color="text.secondary" sx={{ mb: 3 }}>
                   Detailed analytics and download options are available here.
                </Typography>
                {isDuesCleared && (
                   <Button variant="outlined" startIcon={<DownloadIcon />}>
                      Download Report
                   </Button>
                )}
             </Card>
          </Box>
        </Box>
      )}
    </Box>
  );
}
