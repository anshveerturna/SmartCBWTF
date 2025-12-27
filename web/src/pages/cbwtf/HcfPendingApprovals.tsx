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
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Pending as PendingIcon,
} from '@mui/icons-material';
import {
  getPendingHcfs,
  approveHcf,
  rejectHcf,
} from '../../api/cbwtf';
import type { HcfListItem, HcfApprovalRequest } from '../../api/cbwtf';

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
  const [selectedHcf, setSelectedHcf] = useState<HcfListItem | null>(null);
  const [approvalForm, setApprovalForm] = useState<HcfApprovalRequest>({
    perBedPerDayRate: 50,
    excessRatePerKg: 100,
  });
  const [rejectReason, setRejectReason] = useState('');
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
      setSelectedHcf(null);
      setSnackbar({ open: true, message: 'HCF approved successfully', severity: 'success' });
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

  // Handlers
  const openApproveDialog = (hcf: HcfListItem) => {
    setSelectedHcf(hcf);
    setApproveDialogOpen(true);
  };

  const openRejectDialog = (hcf: HcfListItem) => {
    setSelectedHcf(hcf);
    setRejectDialogOpen(true);
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
                <TableCell sx={{ fontWeight: 'bold' }}>Beds</TableCell>
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
                  <TableCell>{hcf.numberOfBeds || '-'}</TableCell>
                  <TableCell>
                    <Typography variant="body2">{hcf.contactPhone || '-'}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {hcf.contactEmail || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>{formatDate(hcf.createdAt)}</TableCell>
                  <TableCell align="center">
                    <Stack direction="row" spacing={1} justifyContent="center">
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
            Approve HCF Registration
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedHcf && (
            <>
              <Typography gutterBottom>
                Approving <strong>{selectedHcf.name}</strong> ({selectedHcf.code})
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                This will create an agreement and activate the HCF.
              </Typography>
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
            variant="contained"
            color="success"
            onClick={() => approveMutation.mutate()}
            disabled={approveMutation.isPending || !approvalForm.perBedPerDayRate || !approvalForm.excessRatePerKg}
          >
            {approveMutation.isPending ? 'Approving...' : 'Approve & Create Agreement'}
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
