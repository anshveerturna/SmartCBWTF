import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Chip,
  Divider,
  Stack,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Business as BusinessIcon,
  Person as PersonIcon,
  LocationOn as LocationIcon,
  Receipt as ReceiptIcon,
} from '@mui/icons-material';

import {
  getHcfApprovalDetail,
  downloadHcfApprovalRentAgreement,
  approveHcfRegistration,
  rejectHcfRegistration,
} from '../../api/topManagement';
import { openExternalUrl } from '../../utils/browser';

const displayValue = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') {
    return 'Not provided';
  }
  return String(value);
};

export default function ManagementHcfDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [documentError, setDocumentError] = useState('');

  const { data: detail, isLoading, error } = useQuery({
    queryKey: ['top-mgmt-hcf-detail', id],
    queryFn: () => getHcfApprovalDetail(id!),
    enabled: !!id,
  });

  const approveMutation = useMutation({
    mutationFn: () => approveHcfRegistration(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['top-mgmt-hcf-approvals'] });
      navigate('/management/hcfs');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectHcfRegistration(id!, rejectReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['top-mgmt-hcf-approvals'] });
      setRejectOpen(false);
      navigate('/management/hcfs');
    },
  });

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="50vh">
        <CircularProgress />
      </Box>
    );
  }

  if (error || !detail) {
    return (
      <Box p={3}>
        <Alert severity="error">Failed to load HCF details.</Alert>
      </Box>
    );
  }

  const hcf = detail;
  const agreement = detail.agreement;

  const openRentAgreement = async () => {
    if (!id) return;
    setDocumentError('');
    try {
      const blob = await downloadHcfApprovalRentAgreement(id);
      const url = window.URL.createObjectURL(blob);
      openExternalUrl(url);
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
    } catch {
      setDocumentError('Failed to open rent agreement document.');
    }
  };

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto', p: 3 }}>
      {/* Header */}
      <Stack direction="row" alignItems="center" spacing={2} mb={4}>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/management/hcfs')} color="inherit">
          Back
        </Button>
        <Typography variant="h4" fontWeight="bold" sx={{ flexGrow: 1 }}>
          Review Registration
        </Typography>
        <Chip label={agreement?.status || 'Pending Approval'} color="warning" />
      </Stack>

      <Grid container spacing={3}>
        {/* Basic Info */}
        <Grid item xs={12} md={6}>
          <Card elevation={2} sx={{ height: '100%', borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <BusinessIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">HCF Details</Typography>
              </Stack>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={2}>
                <Box>
                  <Typography variant="caption" color="text.secondary">Facility Name</Typography>
                  <Typography variant="body1" fontWeight="medium">{hcf.name}</Typography>
                </Box>
                <Grid container spacing={2}>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">HCF Type</Typography>
                      <Typography variant="body2">{displayValue(hcf.hcfType)}</Typography>
                   </Grid>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">Beds</Typography>
                      <Typography variant="body2">{hcf.bedded ? `${hcf.numberOfBeds} Beds` : 'Non-Bedded'}</Typography>
                   </Grid>
                </Grid>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Code</Typography>
                    <Typography variant="body2">{displayValue(hcf.code)}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Seat Count</Typography>
                    <Typography variant="body2">{displayValue(hcf.seatCount)}</Typography>
                  </Grid>
                </Grid>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Pincode</Typography>
                    <Typography variant="body2">{displayValue(hcf.pincode)}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">City / State</Typography>
                    <Typography variant="body2">{displayValue([hcf.city, hcf.state].filter(Boolean).join(', '))}</Typography>
                  </Grid>
                </Grid>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Contact Info */}
        <Grid item xs={12} md={6}>
          <Card elevation={2} sx={{ height: '100%', borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <PersonIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">Contact & Legal</Typography>
              </Stack>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={2}>
                <Box>
                  <Typography variant="caption" color="text.secondary">Doctor / Admin Name</Typography>
                  <Typography variant="body1" fontWeight="medium">{hcf.doctorName || 'Not Provided'}</Typography>
                </Box>
                <Grid container spacing={2}>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">Phone</Typography>
                      <Typography variant="body2">{hcf.contactPhone}</Typography>
                   </Grid>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">Email</Typography>
                      <Typography variant="body2" noWrap>{hcf.contactEmail || 'N/A'}</Typography>
                   </Grid>
                </Grid>
                <Grid container spacing={2}>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">PAN No.</Typography>
                      <Typography variant="body2">{displayValue(hcf.panNo)}</Typography>
                   </Grid>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">GST No.</Typography>
                      <Typography variant="body2">{displayValue(hcf.gstNo)}</Typography>
                   </Grid>
                </Grid>
                <Grid container spacing={2}>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">Aadhar No.</Typography>
                      <Typography variant="body2">{displayValue(hcf.aadharNo)}</Typography>
                   </Grid>
                   <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">Ownership</Typography>
                      <Typography variant="body2">{displayValue(hcf.ownershipType)}</Typography>
                   </Grid>
                </Grid>
                {hcf.rentAgreementUrl && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">Rent Agreement</Typography>
                    <Box mt={0.5}>
                      <Button size="small" variant="outlined" onClick={openRentAgreement}>
                        Open uploaded document
                      </Button>
                    </Box>
                    {documentError && <Alert severity="error" sx={{ mt: 1 }}>{documentError}</Alert>}
                  </Box>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Location Info */}
        <Grid item xs={12}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <LocationIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">Location</Typography>
              </Stack>
              <Divider sx={{ mb: 2 }} />

              <Typography variant="body1">{hcf.address}</Typography>
              <Typography variant="body2" color="text.secondary" mt={1}>
                 Coordinates: {hcf.gpsLat?.toFixed(4)}, {hcf.gpsLon?.toFixed(4)}
              </Typography>
              <Typography variant="body2" color="text.secondary" mt={1}>
                Requested on: {dayjs(hcf.createdAt).format('DD MMM YYYY, HH:mm')}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Billing Info */}
        <Grid item xs={12}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <ReceiptIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">Agreement & Billing setup</Typography>
              </Stack>
              <Divider sx={{ mb: 2 }} />

              <Grid container spacing={3}>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Proposed Monthly Charges</Typography>
                  <Typography variant="h6">
                    {hcf.monthlyCharges != null ? `₹${hcf.monthlyCharges.toLocaleString('en-IN')}` : 'Not provided'}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Agreement Number (Assigned)</Typography>
                  <Typography variant="body1">{displayValue(agreement?.agreementNumber)}</Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Validity Period</Typography>
                  <Typography variant="body1">
                    {agreement?.startDate ? dayjs(agreement.startDate).format('MMM YYYY') : 'TBD'} -
                    {agreement?.endDate ? dayjs(agreement.endDate).format('MMM YYYY') : 'TBD'}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Per Bed Per Day</Typography>
                  <Typography variant="body1">
                    {agreement?.perBedPerDayRate != null ? `₹${agreement.perBedPerDayRate.toLocaleString('en-IN')}` : 'Not provided'}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Tax Rate</Typography>
                  <Typography variant="body1">{hcf.taxRate != null ? `${hcf.taxRate}%` : 'Default'}</Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Excess Rate / Kg</Typography>
                  <Typography variant="body1">
                    {hcf.excessRatePerKg != null ? `₹${hcf.excessRatePerKg.toLocaleString('en-IN')}` : 'Not provided'}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Billing Model</Typography>
                  <Typography variant="body1">{displayValue(hcf.billingModel)}</Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Occupancy</Typography>
                  <Typography variant="body1">{hcf.occupancy != null ? `${hcf.occupancy}%` : 'Not provided'}</Typography>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Typography variant="caption" color="text.secondary">Base Billing Config</Typography>
                  <Typography variant="body1">
                    {detail.billingConfig
                      ? `${detail.billingConfig.baseGramsPerBedPerDay} g/day @ ₹${detail.billingConfig.baseRatePerBedPerDay.toLocaleString('en-IN')}`
                      : 'Not configured'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Notes */}
        <Grid item xs={12}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" mb={2}>Submitted Notes</Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="body2" color={hcf.otherNotes ? 'text.primary' : 'text.secondary'}>
                {hcf.otherNotes || 'No additional notes were submitted.'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Actions */}
        <Grid item xs={12}>
          <Stack direction="row" spacing={2} justifyContent="flex-end" mt={2}>
             <Button
                variant="outlined"
                color="error"
                startIcon={<RejectIcon />}
                onClick={() => setRejectOpen(true)}
                disabled={approveMutation.isPending || rejectMutation.isPending}
             >
                Reject
             </Button>
             <Button
                variant="contained"
                color="success"
                startIcon={<ApproveIcon />}
                onClick={() => approveMutation.mutate()}
                disabled={approveMutation.isPending || rejectMutation.isPending}
             >
                {approveMutation.isPending ? 'Approving...' : 'Approve Registration'}
             </Button>
          </Stack>
        </Grid>

      </Grid>

      {/* Reject Dialog */}
      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Registration</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" paragraph>
            Please provide a reason for rejecting this HCF registration. The CBWTF administrator will see this and can correct the issues to resubmit.
          </Typography>
          <TextField
            autoFocus
            margin="dense"
            label="Reason for Rejection"
            fullWidth
            multiline
            rows={4}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)} color="inherit">Cancel</Button>
          <Button
            onClick={() => rejectMutation.mutate()}
            color="error"
            variant="contained"
            disabled={!rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? 'Rejecting...' : 'Yes, Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
