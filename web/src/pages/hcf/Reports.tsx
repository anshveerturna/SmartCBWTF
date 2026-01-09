import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Chip,
  Paper,
  alpha,
  Stepper,
  Step,
  StepLabel,
} from '@mui/material';
import {
  Lock,
  CheckCircle,
  HourglassEmpty,
  Send,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface ClearanceStatus {
  status: string;
  hasReportAccess: boolean;
  requestedAt?: string;
  submittedAt?: string;
  approvedAt?: string;
  rejectionReason?: string;
  message?: string;
}

const steps = ['Request Submitted', 'CBWTF Verification', 'Management Approval', 'Access Granted'];

const getActiveStep = (status: string): number => {
  switch (status) {
    case 'PENDING': return 1;
    case 'SUBMITTED': return 2;
    case 'APPROVED': return 4;
    case 'REJECTED': return -1;
    default: return 0;
  }
};

const Reports: React.FC = () => {
  const queryClient = useQueryClient();

  // Fetch clearance status
  const { data: statusData, isLoading } = useQuery({
    queryKey: ['hcf-dues-status'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/dues/status');
      return res.data as ClearanceStatus;
    },
  });

  // Request access mutation
  const requestMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post('/api/hcf/dues/request', {});
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hcf-dues-status'] });
    },
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <CircularProgress />
      </Box>
    );
  }

  const status = statusData?.status || 'NONE';
  const hasAccess = statusData?.hasReportAccess || false;
  const activeStep = getActiveStep(status);

  // If access granted, show reports
  if (hasAccess) {
    return (
      <Box>
        <Box sx={{ mb: 4 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Reports
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Download your monthly and yearly waste reports
          </Typography>
        </Box>

        <Alert severity="success" sx={{ mb: 3 }}>
          <strong>Access Granted</strong> — You can now download reports.
        </Alert>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>Monthly Reports</Typography>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              {['2026-01', '2025-12', '2025-11'].map((period) => (
                <Button key={period} variant="outlined" size="small">
                  {period}
                </Button>
              ))}
            </Box>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>Yearly Reports</Typography>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              {['2025', '2024'].map((year) => (
                <Button key={year} variant="outlined" size="small">
                  {year}
                </Button>
              ))}
            </Box>
          </CardContent>
        </Card>
      </Box>
    );
  }

  // No access - show locked state
  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Reports
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Monthly and yearly waste reports
        </Typography>
      </Box>

      {/* Locked State */}
      <Paper
        sx={{
          p: 4,
          textAlign: 'center',
          bgcolor: alpha('#F59E0B', 0.05),
          border: '2px dashed',
          borderColor: alpha('#F59E0B', 0.3),
          mb: 4,
        }}
      >
        <Lock sx={{ fontSize: 64, color: 'warning.main', mb: 2 }} />
        <Typography variant="h5" sx={{ fontWeight: 600, mb: 1 }}>
          Access Restricted — Dues Pending
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3, maxWidth: 500, mx: 'auto' }}>
          Monthly and yearly reports require dues clearance approval from CBWTF and Top Management.
        </Typography>

        {status === 'NONE' && (
          <Button
            variant="contained"
            size="large"
            startIcon={requestMutation.isPending ? <CircularProgress size={20} /> : <Send />}
            onClick={() => requestMutation.mutate()}
            disabled={requestMutation.isPending}
          >
            Request Report Access
          </Button>
        )}

        {status === 'REJECTED' && (
          <>
            <Alert severity="error" sx={{ mb: 2, maxWidth: 500, mx: 'auto' }}>
              <strong>Request Rejected:</strong> {statusData?.rejectionReason || 'Contact CBWTF'}
            </Alert>
            <Button
              variant="contained"
              size="large"
              startIcon={<Send />}
              onClick={() => requestMutation.mutate()}
              disabled={requestMutation.isPending}
            >
              Submit New Request
            </Button>
          </>
        )}
      </Paper>

      {/* Progress Tracker */}
      {status !== 'NONE' && status !== 'REJECTED' && (
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 3 }}>
              Request Status
            </Typography>

            <Stepper activeStep={activeStep} alternativeLabel>
              {steps.map((label, index) => (
                <Step key={label} completed={index < activeStep}>
                  <StepLabel>{label}</StepLabel>
                </Step>
              ))}
            </Stepper>

            <Box sx={{ mt: 4, textAlign: 'center' }}>
              {status === 'PENDING' && (
                <Chip
                  icon={<HourglassEmpty />}
                  label="Awaiting CBWTF Verification"
                  color="warning"
                />
              )}
              {status === 'SUBMITTED' && (
                <Chip
                  icon={<HourglassEmpty />}
                  label="Approval Pending — Awaiting Management Review"
                  color="info"
                />
              )}
            </Box>

            <Box sx={{ mt: 3, p: 2, bgcolor: alpha('#6366F1', 0.05), borderRadius: 1 }}>
              <Typography variant="caption" color="text.secondary">
                Requested: {statusData?.requestedAt ? new Date(statusData.requestedAt).toLocaleString() : '-'}
              </Typography>
              {statusData?.submittedAt && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Submitted to Management: {new Date(statusData.submittedAt).toLocaleString()}
                </Typography>
              )}
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default Reports;
