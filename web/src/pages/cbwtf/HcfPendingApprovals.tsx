import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Stack,
  IconButton,
  CircularProgress,
  Alert,
  Snackbar,
  FormControl,
  FormControlLabel,
  FormLabel,
  Radio,
  RadioGroup,
  InputAdornment,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Pending as PendingIcon,
  Edit as EditIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import {
  getPendingHcfs,
  approveHcf,
  rejectHcf,
  updateHcfBillingModel,
} from '../../api/cbwtf';
import type { HcfListItem, HcfApprovalRequest, BillingModel } from '../../api/cbwtf';

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function HcfPendingApprovals() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // State
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [confirmApproveOpen, setConfirmApproveOpen] = useState(false);
  const [selectedHcf, setSelectedHcf] = useState<HcfListItem | null>(null);
  const [approvalForm, setApprovalForm] = useState<HcfApprovalRequest>({
    perBedPerDayRate: 50,
    excessRatePerKg: 100,
  });
  const [rejectReason, setRejectReason] = useState('');
  const [billingModel, setBillingModel] = useState<BillingModel>('BEDDED');
  const [numberOfBeds, setNumberOfBeds] = useState<number | null>(null);
  const [monthlyCharges, setMonthlyCharges] = useState<number | null>(null);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Queries
  const { data: pendingHcfs, isLoading, error } = useQuery({
    queryKey: ['cbwtf-hcfs-pending'],
    queryFn: getPendingHcfs,
  });

  // Mutations
  const approveMutation = useMutation({
    mutationFn: () => approveHcf(selectedHcf!.id, approvalForm),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcfs-pending'] });
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcfs'] });
      setApproveDialogOpen(false);
      setConfirmApproveOpen(false);
      setSelectedHcf(null);
      setSnackbar({ open: true, message: 'HCF approved successfully. Billing model is now locked.', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to approve HCF', severity: 'error' });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectHcf(selectedHcf!.id, { reason: rejectReason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcfs-pending'] });
      setRejectDialogOpen(false);
      setSelectedHcf(null);
      setRejectReason('');
      setSnackbar({ open: true, message: 'HCF registration rejected', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to reject HCF', severity: 'error' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => updateHcfBillingModel(selectedHcf!.id, {
      billingModel,
      numberOfBeds: billingModel === 'BEDDED' ? numberOfBeds : null,
      monthlyCharges: billingModel === 'FIXED_MONTHLY' ? monthlyCharges : null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcfs-pending'] });
      setEditDialogOpen(false);
      setSnackbar({ open: true, message: 'Billing model updated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to update billing model', severity: 'error' });
    },
  });

  // Handlers
  const openApproveDialog = (hcf: HcfListItem) => {
    setSelectedHcf(hcf);
    // Initialize billing form from HCF
    setBillingModel(hcf.billingModel || 'BEDDED');
    setNumberOfBeds(hcf.numberOfBeds);
    setMonthlyCharges(hcf.monthlyCharges);
    setApproveDialogOpen(true);
  };

  const openRejectDialog = (hcf: HcfListItem) => {
    setSelectedHcf(hcf);
    setRejectDialogOpen(true);
  };

  const openEditDialog = (hcf: HcfListItem) => {
    setSelectedHcf(hcf);
    setBillingModel(hcf.billingModel || 'BEDDED');
    setNumberOfBeds(hcf.numberOfBeds);
    setMonthlyCharges(hcf.monthlyCharges);
    setEditDialogOpen(true);
  };

  const proceedToConfirmApproval = () => {
    setApproveDialogOpen(false);
    setConfirmApproveOpen(true);
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load pending HCFs. Please try again later.
      </Alert>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <IconButton onClick={() => navigate('/cbwtf/hcfs')}>
          <BackIcon />
        </IconButton>
        <PendingIcon sx={{ fontSize: 32, color: 'warning.main' }} />
        <Typography variant="h4" fontWeight="bold">
          Pending HCF Approvals
        </Typography>
        {pendingHcfs && pendingHcfs.length > 0 && (
          <Chip
            label={`${pendingHcfs.length} pending`}
            color="warning"
            size="small"
          />
        )}
      </Box>

      {/* Table */}
      {pendingHcfs && pendingHcfs.length > 0 ? (
        <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: 'warning.light' }}>
                <TableCell sx={{ fontWeight: 'bold' }}>HCF Name</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Code</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Address</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Billing</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Beds / Charge</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Contact</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Registered At</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }} align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {pendingHcfs.map((hcf) => (
                <TableRow key={hcf.id} hover>
                  <TableCell>
                    <Typography fontWeight="medium">{hcf.name}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {hcf.code}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ maxWidth: 200 }} noWrap>
                      {hcf.address}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip 
                      size="small" 
                      label={hcf.billingModel || 'Not Set'}
                      color={hcf.billingModel === 'FIXED_MONTHLY' ? 'info' : 'default'}
                      sx={{ fontWeight: 500 }}
                    />
                  </TableCell>
                  <TableCell>
                    {hcf.billingModel === 'FIXED_MONTHLY' 
                      ? `₹${hcf.monthlyCharges?.toLocaleString() || '0'}/mo`
                      : `${hcf.numberOfBeds || 0} beds`}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{hcf.contactPhone || '-'}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {hcf.contactEmail || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>{formatDate(hcf.createdAt)}</TableCell>
                  <TableCell align="center">
                    <Stack direction="row" spacing={1} justifyContent="center">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={() => openEditDialog(hcf)}
                        title="Edit Billing Model"
                      >
                        <EditIcon />
                      </IconButton>
                      <Button
                        variant="contained"
                        color="success"
                        size="small"
                        startIcon={<ApproveIcon />}
                        onClick={() => openApproveDialog(hcf)}
                      >
                        Approve
                      </Button>
                      <Button
                        variant="outlined"
                        color="error"
                        size="small"
                        startIcon={<RejectIcon />}
                        onClick={() => openRejectDialog(hcf)}
                      >
                        Reject
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      ) : (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <PendingIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              No pending HCF registrations
            </Typography>
            <Typography variant="body2" color="text.secondary">
              HCFs registered via the Android app will appear here for approval.
            </Typography>
          </CardContent>
        </Card>
      )}

      {/* Approve Dialog */}
      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ color: 'success.main' }}>
          <Box display="flex" alignItems="center" gap={1}>
            <ApproveIcon />
            Review & Approve HCF Registration
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedHcf && (
            <>
              <Typography gutterBottom>
                Approving <strong>{selectedHcf.name}</strong> ({selectedHcf.code})
              </Typography>
              
              {/* Billing Model Summary */}
              <Alert severity="info" sx={{ my: 2 }}>
                <strong>Current Billing Model:</strong> {billingModel === 'BEDDED' ? 'Per Bed' : 'Fixed Monthly'}
                <br />
                {billingModel === 'BEDDED' 
                  ? `Beds: ${numberOfBeds || 'Not set'}`
                  : `Monthly Charge: ₹${monthlyCharges?.toLocaleString() || 'Not set'}`}
                <br />
                <Typography variant="caption">
                  Click "Edit Billing Model" to change before approval.
                </Typography>
              </Alert>

              <Stack spacing={2} sx={{ mt: 2 }}>
                <TextField
                  label="Base Rate (₹ per bed per day)"
                  type="number"
                  value={approvalForm.perBedPerDayRate}
                  onChange={(e) => setApprovalForm({ ...approvalForm, perBedPerDayRate: parseFloat(e.target.value) })}
                  fullWidth
                  required
                />
                <TextField
                  label="Excess Waste Rate (₹ per kg)"
                  type="number"
                  value={approvalForm.excessRatePerKg}
                  onChange={(e) => setApprovalForm({ ...approvalForm, excessRatePerKg: parseFloat(e.target.value) })}
                  fullWidth
                  required
                />
              </Stack>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)}>Cancel</Button>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<EditIcon />}
            onClick={() => {
              setApproveDialogOpen(false);
              setEditDialogOpen(true);
            }}
          >
            Edit Billing Model
          </Button>
          <Button
            variant="contained"
            color="success"
            onClick={proceedToConfirmApproval}
            disabled={!approvalForm.perBedPerDayRate || !approvalForm.excessRatePerKg ||
              (billingModel === 'BEDDED' && !numberOfBeds) ||
              (billingModel === 'FIXED_MONTHLY' && !monthlyCharges)}
          >
            Review & Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ color: 'error.main' }}>
          <Box display="flex" alignItems="center" gap={1}>
            <RejectIcon />
            Reject HCF Registration
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedHcf && (
            <>
              <Typography gutterBottom>
                Rejecting registration for <strong>{selectedHcf.name}</strong>
              </Typography>
              <TextField
                label="Reason for rejection"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                fullWidth
                multiline
                rows={3}
                required
                sx={{ mt: 2 }}
              />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => rejectMutation.mutate()}
            disabled={!rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? 'Rejecting...' : 'Reject Registration'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Billing Model Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box display="flex" alignItems="center" gap={1}>
            <EditIcon color="primary" />
            Edit Billing Model
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedHcf && (
            <>
              <Typography gutterBottom>
                Editing billing model for <strong>{selectedHcf.name}</strong>
              </Typography>
              
              <FormControl component="fieldset" sx={{ mt: 2, mb: 2 }}>
                <FormLabel component="legend">Billing Model</FormLabel>
                <RadioGroup
                  row
                  value={billingModel}
                  onChange={(e) => setBillingModel(e.target.value as BillingModel)}
                >
                  <FormControlLabel value="BEDDED" control={<Radio />} label="Per Bed (₹ per bed per day)" />
                  <FormControlLabel value="FIXED_MONTHLY" control={<Radio />} label="Fixed Monthly (₹/month)" />
                </RadioGroup>
              </FormControl>

              {billingModel === 'BEDDED' ? (
                <TextField
                  label="Number of Beds"
                  type="number"
                  value={numberOfBeds || ''}
                  onChange={(e) => setNumberOfBeds(parseInt(e.target.value) || null)}
                  fullWidth
                  required
                  helperText="Enter the number of hospital beds"
                />
              ) : (
                <TextField
                  label="Monthly Charge"
                  type="number"
                  value={monthlyCharges || ''}
                  onChange={(e) => setMonthlyCharges(parseFloat(e.target.value) || null)}
                  fullWidth
                  required
                  InputProps={{
                    startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                  }}
                  helperText="Fixed monthly charge (ignores pickups)"
                />
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => updateMutation.mutate()}
            disabled={updateMutation.isPending || 
              (billingModel === 'BEDDED' && !numberOfBeds) ||
              (billingModel === 'FIXED_MONTHLY' && !monthlyCharges)}
          >
            {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Confirm Approval Warning Dialog */}
      <Dialog open={confirmApproveOpen} onClose={() => setConfirmApproveOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ color: 'warning.main' }}>
          <Box display="flex" alignItems="center" gap={1}>
            <WarningIcon />
            Confirm HCF Approval
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedHcf && (
            <>
              <Alert severity="warning" sx={{ mb: 2 }}>
                <strong>Warning:</strong> This action is irreversible. Once approved, the billing model 
                (<strong>{billingModel}</strong>) will be <strong>locked</strong> and cannot be changed.
              </Alert>
              
              <Typography gutterBottom>
                Approving <strong>{selectedHcf.name}</strong> with:
              </Typography>
              
              <Box component="ul" sx={{ pl: 2 }}>
                <li>
                  <Typography>
                    Billing Model: <strong>{billingModel === 'BEDDED' ? 'Per Bed' : 'Fixed Monthly'}</strong>
                  </Typography>
                </li>
                <li>
                  <Typography>
                    {billingModel === 'BEDDED' 
                      ? `Beds: ${numberOfBeds}`
                      : `Monthly Charge: ₹${monthlyCharges?.toLocaleString()}`}
                  </Typography>
                </li>
                <li>
                  <Typography>
                    Rate: ₹{approvalForm.perBedPerDayRate}/bed/day
                  </Typography>
                </li>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmApproveOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="warning"
            onClick={() => approveMutation.mutate()}
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? 'Approving...' : 'Yes, Approve & Lock Billing Model'}
          </Button>
        </DialogActions>
      </Dialog>

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
