import React, { useState } from 'react';
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
  Button,
  CircularProgress,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  alpha,
} from '@mui/material';
import {
  CheckCircle,
  Cancel,
  Pending,
  ArrowRightAlt,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getPendingCorrections,
  approveCorrection,
  rejectCorrection
} from '../../api/topManagement';
import type { PendingCorrection } from '../../api/topManagement';

const CorrectionRequests: React.FC = () => {
  const queryClient = useQueryClient();
  const [approveId, setApproveId] = useState<string | null>(null);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // Fetch pending approvals
  const { data: requests, isLoading } = useQuery({
    queryKey: ['management-correction-requests'],
    queryFn: getPendingCorrections,
  });

  // Approve mutation
  const approveMutation = useMutation({
    mutationFn: approveCorrection,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['management-correction-requests'] });
      setApproveId(null);
    },
  });

  // Reject mutation
  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectCorrection(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['management-correction-requests'] });
      setRejectDialogOpen(false);
      setRejectReason('');
    },
  });

  const openRejectDialog = (id: string) => {
    setRejectId(id);
    setRejectDialogOpen(true);
  };

  const handleApproveConfirm = () => {
    if (approveId) {
      approveMutation.mutate(approveId);
    }
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Agreement Correction Requests
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Review and approve corrections to active HCF agreements
        </Typography>
      </Box>

      {/* Pending Requests */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Pending color="warning" /> Pending Approvals ({requests?.length || 0})
            </Typography>
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : !requests || requests.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#10B981', 0.05) }}>
              <CheckCircle sx={{ fontSize: 48, color: 'success.main', mb: 2 }} />
              <Typography color="text.secondary">
                All caught up! No pending correction requests.
              </Typography>
            </Paper>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>HCF</TableCell>
                    <TableCell>Contact</TableCell>
                    <TableCell>Agreement No.</TableCell>
                    <TableCell>Field to Change</TableCell>
                    <TableCell>Current → Requested</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell>Submitted</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {requests.map((req: PendingCorrection) => (
                    <TableRow key={req.id} hover>
                      <TableCell>
                        <Box>
                          <Typography variant="subtitle2" fontWeight={600} sx={{ color: 'text.primary' }}>
                            {req.hcfName}
                          </Typography>
                          <Typography variant="caption" sx={{ color: 'text.secondary', fontFamily: 'monospace', letterSpacing: '0.5px' }}>
                            {req.hcfCode}
                          </Typography>
                        </Box>
                      </TableCell>

                      <TableCell>
                        <Typography variant="body2">{req.contactPhone}</Typography>
                        <Typography variant="caption" color="text.secondary">{req.doctorName}</Typography>
                      </TableCell>

                      <TableCell>
                         <Typography variant="body2" sx={{ fontFamily: 'monospace', bgcolor: alpha('#fff', 0.05), py: 0.5, px: 1, borderRadius: 1, display: 'inline-block' }}>
                           {req.agreementNumber || '-'}
                         </Typography>
                      </TableCell>

                      <TableCell>
                        <Typography variant="subtitle2" fontWeight={700} color="primary.main">
                           {req.fieldName}
                        </Typography>
                      </TableCell>

                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" color="error.main" sx={{ textDecoration: 'line-through' }}>
                                {req.currentValue || 'N/A'}
                            </Typography>
                            <ArrowRightAlt fontSize="small" color="action" />
                            <Typography variant="body2" color="success.main" fontWeight={600}>
                                {req.requestedValue}
                            </Typography>
                        </Box>
                      </TableCell>

                      <TableCell>
                        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 200, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={req.reason}>
                          {req.reason}
                        </Typography>
                      </TableCell>

                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(req.requestedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                        </Typography>
                      </TableCell>

                      <TableCell align="right">
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                          <Button
                            size="small"
                            variant="outlined"
                            color="success"
                            startIcon={approveMutation.isPending ? <CircularProgress size={14} /> : <CheckCircle />}
                            onClick={() => setApproveId(req.id)}
                            disabled={approveMutation.isPending}
                            sx={{ textTransform: 'none', fontWeight: 600 }}
                          >
                            Approve
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            startIcon={<Cancel />}
                            onClick={() => openRejectDialog(req.id)}
                            sx={{ textTransform: 'none', fontWeight: 600 }}
                          >
                            Reject
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={approveId !== null} onClose={() => setApproveId(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Approve Correction Request</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Approving this request will permanently replace the old agreement value.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveId(null)} disabled={approveMutation.isPending}>Cancel</Button>
          <Button
            color="success"
            variant="contained"
            onClick={handleApproveConfirm}
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? <CircularProgress size={20} /> : 'Approve'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Correction Request</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Rejection Reason"
            fullWidth
            multiline
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="Provide a reason for rejection..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => rejectId && rejectMutation.mutate({ id: rejectId, reason: rejectReason })}
            disabled={!rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? <CircularProgress size={20} /> : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CorrectionRequests;
