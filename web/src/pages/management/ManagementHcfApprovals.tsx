import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Alert,
  Snackbar,
} from '@mui/material';
import {
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import dayjs from 'dayjs';
import {
  getPendingHcfApprovals,
  approveHcfRegistration,
  rejectHcfRegistration,
} from '../../api/topManagement';

const errorMessage = (err: unknown, fallback: string): string =>
  err instanceof Error && err.message ? err.message : fallback;

export default function ManagementHcfApprovals() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [rejectDialog, setRejectDialog] = useState<{ open: boolean; id: string | null }>({
    open: false,
    id: null,
  });
  const [approveDialog, setApproveDialog] = useState<{ open: boolean; id: string | null }>({
    open: false,
    id: null,
  });
  const [rejectReason, setRejectReason] = useState('');
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  const { data: hcfs, isLoading, error } = useQuery({
    queryKey: ['top-mgmt-hcf-approvals'],
    queryFn: getPendingHcfApprovals,
  });

  const approveMutation = useMutation({
    mutationFn: approveHcfRegistration,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['top-mgmt-hcf-approvals'] });
      setSnackbar({ open: true, message: 'HCF approved successfully', severity: 'success' });
      setApproveDialog({ open: false, id: null });
    },
    onError: (err: unknown) => {
      setSnackbar({
        open: true,
        message: errorMessage(err, 'Failed to approve HCF'),
        severity: 'error',
      });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      rejectHcfRegistration(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['top-mgmt-hcf-approvals'] });
      setSnackbar({ open: true, message: 'HCF rejected successfully', severity: 'success' });
      setRejectDialog({ open: false, id: null });
      setRejectReason('');
    },
    onError: (err: unknown) => {
      setSnackbar({
        open: true,
        message: errorMessage(err, 'Failed to reject HCF'),
        severity: 'error',
      });
    },
  });

  const handleApprove = (id: string) => {
    setApproveDialog({ open: true, id });
  };

  const handleApproveConfirm = () => {
    if (approveDialog.id) {
      approveMutation.mutate(approveDialog.id);
    }
  };

  const handleRejectClick = (id: string) => {
    setRejectDialog({ open: true, id });
  };

  const handleRejectConfirm = () => {
    if (rejectDialog.id && rejectReason.trim()) {
      rejectMutation.mutate({ id: rejectDialog.id, reason: rejectReason.trim() });
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load pending HCF approvals.
      </Alert>
    );
  }

  const approvalTarget = hcfs?.find((hcf) => hcf.id === approveDialog.id);

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" mb={3}>
        Pending HCF Registrations
      </Typography>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Requested Date</TableCell>
                <TableCell>HCF Name</TableCell>
                <TableCell>Agreement No.</TableCell>
                <TableCell>Contact</TableCell>
                <TableCell>Beds</TableCell>
                <TableCell>Monthly Charges (₹)</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {hcfs?.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <Typography color="text.secondary" py={3}>
                      No pending HCF registrations.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                hcfs?.map((hcf) => (
                  <TableRow key={hcf.id}>
                    <TableCell>
                      {dayjs(hcf.requestedAt).format('DD MMM YYYY, HH:mm')}
                    </TableCell>
                    <TableCell>
                      <Typography variant="subtitle2">{hcf.name}</Typography>
                      {hcf.code && (
                        <Typography variant="caption" color="text.secondary">
                          {hcf.code}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      {hcf.agreementNumber ? (
                        <Chip label={hcf.agreementNumber} size="small" color="primary" variant="outlined" />
                      ) : (
                        <Typography variant="caption" color="text.secondary">—</Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{hcf.contactPhone}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {hcf.contactEmail}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {hcf.numberOfBeds ? (
                        <Chip label={`${hcf.numberOfBeds} Beds`} size="small" />
                      ) : (
                        <Typography variant="caption" color="text.secondary">
                          Non-Bedded
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      {hcf.monthlyCharges
                        ? hcf.monthlyCharges.toLocaleString('en-IN')
                        : 'N/A'}
                    </TableCell>
                    <TableCell align="right">
                      <Box display="flex" gap={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          variant="outlined"
                          color="info"
                          startIcon={<ViewIcon />}
                          onClick={() => navigate(`/management/hcfs/${hcf.id}`)}
                        >
                          View
                        </Button>
                        <Button
                          size="small"
                          variant="contained"
                          color="success"
                          startIcon={<ApproveIcon />}
                          onClick={() => handleApprove(hcf.id)}
                          disabled={approveMutation.isPending || rejectMutation.isPending}
                        >
                          Approve
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          startIcon={<RejectIcon />}
                          onClick={() => handleRejectClick(hcf.id)}
                          disabled={approveMutation.isPending || rejectMutation.isPending}
                        >
                          Reject
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Approve Dialog */}
      <Dialog
        open={approveDialog.open}
        onClose={() => !approveMutation.isPending && setApproveDialog({ open: false, id: null })}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Approve HCF Registration</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            Approve {approvalTarget?.name || 'this HCF'} and generate portal credentials?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setApproveDialog({ open: false, id: null })}
            disabled={approveMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleApproveConfirm}
            color="success"
            variant="contained"
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? 'Approving...' : 'Approve'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog
        open={rejectDialog.open}
        onClose={() => !rejectMutation.isPending && setRejectDialog({ open: false, id: null })}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Reject HCF Registration</DialogTitle>
        <DialogContent>
          <Typography mb={2}>
            Please provide a specific reason for rejection. This will be emailed to the HCF.
            All associated agreements will be deleted.
          </Typography>
          <TextField
            autoFocus
            margin="dense"
            label="Rejection Reason"
            fullWidth
            multiline
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            disabled={rejectMutation.isPending}
            error={rejectReason.length > 0 && rejectReason.trim().length === 0}
            helperText="Reason cannot be empty"
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setRejectDialog({ open: false, id: null })}
            disabled={rejectMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleRejectConfirm}
            color="error"
            variant="contained"
            disabled={!rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? 'Rejecting...' : 'Confirm Rejection'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
