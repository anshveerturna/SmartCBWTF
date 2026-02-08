import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Chip,
  CircularProgress,
  alpha,
  Paper,
} from '@mui/material';
import {
  Description,
  Business,
  LocalHospital,
  CalendarMonth,
  Payments,
  Download,
  CheckCircle,
  Warning,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface AgreementData {
  id: string;
  agreementNumber: string;
  status: string;
  duesStatus: string;
  startDate: string;
  endDate: string | null;
  perBedPerDayRate: number;
  version: number;
  termsAccepted: boolean;
  termsVersion: string | null;
  termsAcceptedAt: string | null;
  createdAt: string;
  // Facility (CBWTF) info
  facilityCode: string;
  facilityName: string;
  facilityAddress: string;
  facilityEmail: string;
  facilityPhone: string;
  // HCF info
  hcfCode: string;
  hcfName: string;
  hcfAddress: string;
  hcfState: string;
  hcfPincode: string;
  hcfBeds: number;
  billingModel: string;
  // PDF
  pdfAvailable: boolean;
}

const InfoRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
    <Typography variant="body2" color="text.secondary">{label}</Typography>
    <Typography variant="body2" fontWeight={500}>{value || '—'}</Typography>
  </Box>
);

const StatusChip: React.FC<{ status: string }> = ({ status }) => {
  const config: Record<string, { color: 'success' | 'warning' | 'error' | 'default'; icon: React.ReactElement }> = {
    ACTIVE: { color: 'success', icon: <CheckCircle /> },
    EXPIRED: { color: 'warning', icon: <Warning /> },
    TERMINATED: { color: 'error', icon: <ErrorIcon /> },
    DISPUTED: { color: 'error', icon: <ErrorIcon /> },
  };
  const { color, icon } = config[status] || { color: 'default', icon: null };
  return <Chip icon={icon} label={status} color={color} size="small" />;
};

const DuesChip: React.FC<{ status: string }> = ({ status }) => {
  const isClean = status === 'CLEAR';
  return (
    <Chip
      label={status}
      color={isClean ? 'success' : 'warning'}
      variant="outlined"
      size="small"
    />
  );
};

const formatDate = (dateStr: string | null) => {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

const calculateDuration = (start: string, end: string | null) => {
  if (!end) return 'Ongoing';
  const startDate = new Date(start);
  const endDate = new Date(end);
  const months = Math.round((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24 * 30));
  return `${months} month${months !== 1 ? 's' : ''}`;
};

const Agreement: React.FC = () => {
  const { data: agreement, isLoading, error } = useQuery<AgreementData>({
    queryKey: ['hcf-agreement'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/agreement');
      return res.data;
    },
  });

  const handleDownloadPdf = async () => {
    try {
      const response = await apiClient.get('/api/hcf/agreement/pdf', {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Agreement_${agreement?.agreementNumber || 'document'}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download PDF:', err);
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !agreement) {
    return (
      <Box>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 2 }}>Agreement</Typography>
        <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#F44336', 0.05) }}>
          <ErrorIcon sx={{ fontSize: 48, color: 'error.main', mb: 2 }} />
          <Typography variant="h6" color="error.main" gutterBottom>
            No Active Agreement Found
          </Typography>
          <Typography color="text.secondary">
            Please contact your CBWTF administrator for assistance.
          </Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Agreement Details
          </Typography>
          <Typography variant="body1" color="text.secondary">
            View your agreement information with {agreement.facilityName}
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<Download />}
          onClick={handleDownloadPdf}
        >
          Download PDF
        </Button>
      </Box>

      <Grid container spacing={3}>
        {/* Agreement Overview Card */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Description color="primary" />
                <Typography variant="h6">Agreement Overview</Typography>
              </Box>
              
              <InfoRow label="Agreement Number" value={
                <Typography variant="body2" fontWeight={700} color="primary.main">
                  {agreement.agreementNumber}
                </Typography>
              } />
              <InfoRow label="Status" value={<StatusChip status={agreement.status} />} />
              <InfoRow label="Dues Status" value={<DuesChip status={agreement.duesStatus} />} />
              <InfoRow label="Version" value={`V${agreement.version}`} />
              <InfoRow label="Created On" value={formatDate(agreement.createdAt)} />
            </CardContent>
          </Card>
        </Grid>

        {/* Contract Period Card */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <CalendarMonth color="primary" />
                <Typography variant="h6">Contract Period</Typography>
              </Box>
              
              <InfoRow label="Start Date" value={formatDate(agreement.startDate)} />
              <InfoRow label="End Date" value={formatDate(agreement.endDate)} />
              <InfoRow label="Duration" value={calculateDuration(agreement.startDate, agreement.endDate)} />
              <InfoRow label="Terms Accepted" value={
                agreement.termsAccepted ? (
                  <Chip icon={<CheckCircle />} label="Yes" color="success" size="small" variant="outlined" />
                ) : (
                  <Chip label="No" color="default" size="small" variant="outlined" />
                )
              } />
              {agreement.termsVersion && (
                <InfoRow label="Terms Version" value={agreement.termsVersion} />
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* CBWTF Details Card */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Business color="primary" />
                <Typography variant="h6">CBWTF Details</Typography>
              </Box>
              
              <InfoRow label="Facility Code" value={agreement.facilityCode} />
              <InfoRow label="Facility Name" value={agreement.facilityName} />
              <InfoRow label="Address" value={agreement.facilityAddress} />
              <InfoRow label="Email" value={agreement.facilityEmail} />
              <InfoRow label="Phone" value={agreement.facilityPhone} />
            </CardContent>
          </Card>
        </Grid>

        {/* HCF Details Card */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <LocalHospital color="primary" />
                <Typography variant="h6">Your Facility Details</Typography>
              </Box>
              
              <InfoRow label="Facility Name" value={agreement.hcfName} />
              <InfoRow label="Address" value={agreement.hcfAddress} />
              <InfoRow label="State" value={agreement.hcfState} />
              <InfoRow label="Pincode" value={agreement.hcfPincode} />
              <InfoRow label="Number of Beds" value={agreement.hcfBeds} />
            </CardContent>
          </Card>
        </Grid>

        {/* Billing Information Card */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Payments color="primary" />
                <Typography variant="h6">Billing Information</Typography>
              </Box>
              
              <Grid container spacing={3}>
                <Grid item xs={12} sm={4}>
                  <Paper sx={{ p: 3, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
                    <Typography variant="h4" fontWeight={700} color="primary.main">
                      ₹{agreement.perBedPerDayRate?.toFixed(2) || '0.00'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Per Bed Per Day Rate
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Paper sx={{ p: 3, textAlign: 'center', bgcolor: alpha('#10B981', 0.05) }}>
                    <Typography variant="h4" fontWeight={700} color="success.main">
                      {agreement.hcfBeds || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Registered Beds
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Paper sx={{ p: 3, textAlign: 'center', bgcolor: alpha('#F59E0B', 0.05) }}>
                    <Typography variant="h4" fontWeight={700} sx={{ color: '#F59E0B' }}>
                      {agreement.billingModel || 'N/A'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Billing Model
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Agreement;
