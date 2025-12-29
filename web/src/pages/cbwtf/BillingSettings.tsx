import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Snackbar,
  Card,
  CardContent,
} from '@mui/material';
import { Save as SaveIcon } from '@mui/icons-material';
import {
  getFacilityBillingConfig,
  updateExcessRate,
  getExcessRateHistory,
} from '../../api/cbwtf';

// Format currency
const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(amount);
};

// Format date
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export default function BillingSettings() {
  const queryClient = useQueryClient();
  const [newRate, setNewRate] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  // Fetch current config
  const { data: config, isLoading: configLoading, error: configError } = useQuery({
    queryKey: ['billingConfig'],
    queryFn: getFacilityBillingConfig,
  });

  // Fetch rate history
  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ['excessRateHistory'],
    queryFn: getExcessRateHistory,
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: () => updateExcessRate(parseFloat(newRate), effectiveFrom),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['billingConfig'] });
      queryClient.invalidateQueries({ queryKey: ['excessRateHistory'] });
      setNewRate('');
      setEffectiveFrom('');
      setSnackbar({ open: true, message: 'Excess rate updated successfully', severity: 'success' });
    },
    onError: (error: Error) => {
      setSnackbar({ open: true, message: error.message || 'Failed to update rate', severity: 'error' });
    },
  });

  // Handle submit
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRate || !effectiveFrom) {
      setSnackbar({ open: true, message: 'Please fill all fields', severity: 'error' });
      return;
    }
    if (parseFloat(newRate) <= 0) {
      setSnackbar({ open: true, message: 'Rate must be positive', severity: 'error' });
      return;
    }
    if (new Date(effectiveFrom) < new Date(new Date().toISOString().split('T')[0])) {
      setSnackbar({ open: true, message: 'Effective date cannot be in the past', severity: 'error' });
      return;
    }
    updateMutation.mutate();
  };

  // Get today's date in YYYY-MM-DD format
  const today = new Date().toISOString().split('T')[0];

  return (
    <Box>
      {/* Header */}
      <Typography variant="h4" fontWeight="bold" mb={3}>
        Billing Settings
      </Typography>

      {/* Error */}
      {configError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load billing config
        </Alert>
      )}

      {/* Current Rate Card */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card sx={{ bgcolor: 'primary.main', color: 'white' }}>
            <CardContent>
              <Typography gutterBottom sx={{ opacity: 0.9 }}>
                Current Excess Waste Rate
              </Typography>
              {configLoading ? (
                <CircularProgress color="inherit" size={24} />
              ) : (
                <>
                  <Typography variant="h3" fontWeight="bold">
                    {config ? formatCurrency(config.excessRatePerKg) : '-'}
                    <Typography component="span" variant="h6" sx={{ ml: 1, opacity: 0.9 }}>
                      per kg
                    </Typography>
                  </Typography>
                  <Typography sx={{ mt: 1, opacity: 0.9 }}>
                    Effective from: {config ? formatDate(config.excessRateEffectiveFrom) : '-'}
                  </Typography>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Update Form */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight="bold" gutterBottom>
              Set New Excess Rate
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              New rate will apply to all future billing cycles starting from the effective date.
              Past bills are not affected.
            </Typography>
            <form onSubmit={handleSubmit}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Rate per kg (₹)"
                    type="number"
                    fullWidth
                    value={newRate}
                    onChange={(e) => setNewRate(e.target.value)}
                    inputProps={{ step: '0.01', min: '0' }}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Effective From"
                    type="date"
                    fullWidth
                    value={effectiveFrom}
                    onChange={(e) => setEffectiveFrom(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    inputProps={{ min: today }}
                    required
                  />
                </Grid>
                <Grid item xs={12}>
                  <Button
                    type="submit"
                    variant="contained"
                    startIcon={updateMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                    disabled={updateMutation.isPending}
                  >
                    Update Rate
                  </Button>
                </Grid>
              </Grid>
            </form>
          </Paper>
        </Grid>
      </Grid>

      {/* Rate History */}
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" fontWeight="bold" gutterBottom>
          Rate Change History
        </Typography>
        <Divider sx={{ mb: 2 }} />
        
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Rate per kg</TableCell>
                <TableCell>Effective From</TableCell>
                <TableCell>Changed At</TableCell>
                <TableCell>Changed By</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {historyLoading ? (
                <TableRow>
                  <TableCell colSpan={4} align="center">
                    <CircularProgress size={24} />
                  </TableCell>
                </TableRow>
              ) : history?.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} align="center">
                    <Typography color="text.secondary">No rate changes recorded</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                history?.map((item, index) => (
                  <TableRow key={index}>
                    <TableCell>{formatCurrency(item.ratePerKg)}</TableCell>
                    <TableCell>{formatDate(item.effectiveFrom)}</TableCell>
                    <TableCell>{formatDate(item.changedAt)}</TableCell>
                    <TableCell>{item.changedBy || 'System'}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
