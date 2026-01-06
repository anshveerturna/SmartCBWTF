import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Chip,
  Divider,
  Button,
  CircularProgress,
  Alert,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Tooltip,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { getBillDetail, downloadBillPdf } from '../../api/cbwtf';
import { useState } from 'react';

// Format currency
const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(amount);
};

// Format date
const formatMonth = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'long' });
};

const formatDate = (dateStr: string | null) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export default function BillDetail() {
  const { billId } = useParams<{ billId: string }>();
  const navigate = useNavigate();
  const [downloading, setDownloading] = useState(false);

  const { data: bill, isLoading, error } = useQuery({
    queryKey: ['bill', billId],
    queryFn: () => getBillDetail(billId!),
    enabled: !!billId,
  });

  const handleDownload = async () => {
    if (!billId) return;
    setDownloading(true);
    try {
      const blob = await downloadBillPdf(billId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `bill_${bill?.hcfName?.replace(/\s+/g, '_') || billId}_${bill?.billingMonth || 'bill'}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch {
      console.error('Failed to download');
    } finally {
      setDownloading(false);
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !bill) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load bill details
      </Alert>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/cbwtf/billing')}>
          Back
        </Button>
        <Typography variant="h4" fontWeight="bold" sx={{ flex: 1 }}>
          Bill Details
        </Typography>
        <Button
          variant="contained"
          startIcon={downloading ? <CircularProgress size={18} color="inherit" /> : <DownloadIcon />}
          onClick={handleDownload}
          disabled={downloading}
        >
          Download Bill PDF
        </Button>
      </Box>

      {/* Bill Identity Header */}
      <Paper sx={{ p: 3, mb: 3, bgcolor: 'primary.main', color: 'white' }}>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="overline" sx={{ opacity: 0.8 }}>HCF Name</Typography>
            <Typography variant="h5" fontWeight="bold">{bill.hcfName}</Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="overline" sx={{ opacity: 0.8 }}>Billing Month</Typography>
            <Typography variant="h5" fontWeight="bold">{formatMonth(bill.billingMonth)}</Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Box display="flex" flexDirection="column" alignItems={{ xs: 'flex-start', md: 'flex-end' }}>
              <Chip label={bill.status} color="success" sx={{ mb: 1 }} />
            </Box>
          </Grid>
        </Grid>
      </Paper>

      <Grid container spacing={3}>
        {/* Agreement Info */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Agreement Details
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Table size="small">
                <TableBody>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold', border: 0 }}>Agreement Code</TableCell>
                    <TableCell sx={{ border: 0 }}>{bill.agreementCode}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold', border: 0 }}>Agreement Version</TableCell>
                    <TableCell sx={{ border: 0 }}>v{bill.agreementVersion}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>

        {/* Pickup Snapshot */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Pickup Snapshot
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Table size="small">
                <TableBody>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold', border: 0 }}>Total Pickup Events</TableCell>
                    <TableCell sx={{ border: 0 }}>{bill.pickupEventCount}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold', border: 0 }}>Total Pickup Weight</TableCell>
                    <TableCell sx={{ border: 0 }}>{bill.pickupWeightKg.toFixed(3)} kg</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold', border: 0 }}>Pickup Hash</TableCell>
                    <TableCell sx={{ border: 0 }}>
                      <Tooltip title={bill.pickupEventHash || 'N/A'}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                          {bill.pickupEventHash ? `${bill.pickupEventHash.substring(0, 16)}...` : 'N/A'}
                        </Typography>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>

        {/* Rate Snapshot (Frozen) */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Rate Snapshot (Frozen at Billing Time)
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid size={{ xs: 6, sm: 4, md: 2 }}>
                  <Typography variant="caption" color="text.secondary">Beds</Typography>
                  <Typography variant="h6">{bill.bedCount}</Typography>
                </Grid>
                <Grid size={{ xs: 6, sm: 4, md: 2 }}>
                  <Typography variant="caption" color="text.secondary">Base grams/bed/day</Typography>
                  <Typography variant="h6">{bill.baseGramsPerBedPerDay.toFixed(2)}</Typography>
                </Grid>
                <Grid size={{ xs: 6, sm: 4, md: 2 }}>
                  <Typography variant="caption" color="text.secondary">Base rate/bed/day</Typography>
                  <Typography variant="h6">{formatCurrency(bill.baseRatePerBedPerDay)}</Typography>
                </Grid>
                <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                  <Typography variant="caption" color="text.secondary">Excess rate/kg</Typography>
                  <Typography variant="h6">{formatCurrency(bill.excessRatePerKg)}</Typography>
                </Grid>
                <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                  <Typography variant="caption" color="text.secondary">Excess Rate Effective From</Typography>
                  <Typography variant="h6">{formatDate(bill.excessRateEffectiveFrom)}</Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Calculation Breakdown */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Calculation Breakdown
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={4}>
                {/* Weight Breakdown */}
                <Grid size={{ xs: 12, md: 4 }}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Weight Analysis
                  </Typography>
                  <Table size="small">
                    <TableBody>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>Base Allowance</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{bill.baseAllowanceKg.toFixed(3)} kg</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>Pickup Weight</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{bill.pickupWeightKg.toFixed(3)} kg</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0, fontWeight: 'bold', color: bill.excessWeightKg > 0 ? 'error.main' : 'inherit' }}>
                          Excess Weight
                        </TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right', fontWeight: 'bold', color: bill.excessWeightKg > 0 ? 'error.main' : 'inherit' }}>
                          {bill.excessWeightKg.toFixed(3)} kg
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </Grid>

                {/* Amount Breakdown */}
                <Grid size={{ xs: 12, md: 8 }}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Amount Breakdown
                  </Typography>
                  <Table size="small">
                    <TableBody>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>Base Amount</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{formatCurrency(bill.baseAmount)}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>Excess Amount</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{formatCurrency(bill.excessAmount)}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0, fontWeight: 'bold' }}>Subtotal</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right', fontWeight: 'bold' }}>{formatCurrency(bill.subtotal)}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>CGST @ 9%</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{formatCurrency(bill.cgst)}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ border: 0 }}>SGST @ 9%</TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right' }}>{formatCurrency(bill.sgst)}</TableCell>
                      </TableRow>
                      <TableRow sx={{ bgcolor: 'primary.main' }}>
                        <TableCell sx={{ border: 0, fontWeight: 'bold', color: 'white', fontSize: '1.1rem' }}>
                          TOTAL
                        </TableCell>
                        <TableCell sx={{ border: 0, textAlign: 'right', fontWeight: 'bold', color: 'white', fontSize: '1.1rem' }}>
                          {formatCurrency(bill.totalAmount)}
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Read-only footer */}
      <Alert severity="info" sx={{ mt: 3 }}>
        This is an operational bill. GST invoice will be issued separately via Tally.
        Adjustments can be applied by CBWTF Admin if required.
      </Alert>
    </Box>
  );
}
