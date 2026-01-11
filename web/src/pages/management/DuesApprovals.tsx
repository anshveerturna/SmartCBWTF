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
  Chip,
  CircularProgress,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Checkbox,
  Alert,
  alpha,
} from '@mui/material';
import {
  CheckCircle,
  Cancel,
  Pending,
  Business,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface ClearanceRequest {
  id: string;
  hcfId: string;
  hcfName: string;
  hcfCode: string;
  facilityName: string;
  agreementNumber?: string;
  status: string;
  requestedAt: string;
  submittedAt?: string;
  amountCleared?: number;
  cbwtfNotes?: string;
}

const DuesApprovals: React.FC = () => {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // Fetch pending approvals
  const { data, isLoading } = useQuery({
    queryKey: ['management-dues-approvals'],
    queryFn: async () => {
      const res = await apiClient.get('/api/management/dues-approvals');
      return res.data;
    },
  });

  // Approve mutation
  const approveMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post(`/api/management/dues-approvals/${id}/approve`);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['management-dues-approvals'] });
    },
  });

  // Reject mutation
  const rejectMutation = useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason: string }) => {
      const res = await apiClient.post(`/api/management/dues-approvals/${id}/reject`, { reason });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['management-dues-approvals'] });
      setRejectDialogOpen(false);
      setRejectReason('');
    },
  });

  // Bulk approve mutation
  const bulkApproveMutation = useMutation({
    mutationFn: async (ids: string[]) => {
      const res = await apiClient.post('/api/management/dues-approvals/bulk-approve', { ids });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['management-dues-approvals'] });
      setSelected(new Set());
    },
  });

  const handleSelectAll = (checked: boolean) => {
    if (checked && data?.requests) {
      setSelected(new Set(data.requests.map((r: ClearanceRequest) => r.id)));
    } else {
      setSelected(new Set());
    }
  };

  const handleSelect = (id: string, checked: boolean) => {
    const newSelected = new Set(selected);
    if (checked) {
      newSelected.add(id);
    } else {
      newSelected.delete(id);
    }
    setSelected(newSelected);
  };

  const openRejectDialog = (id: string) => {
    setRejectId(id);
    setRejectDialogOpen(true);
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Dues Clearance Approvals
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Review and approve HCF dues clearance requests
        </Typography>
      </Box>

      {/* Bulk Actions */}
      {selected.size > 0 && (
        <Alert
          severity="info"
          sx={{ mb: 3 }}
          action={
            <Button
              color="success"
              variant="contained"
              size="small"
              startIcon={bulkApproveMutation.isPending ? <CircularProgress size={16} /> : <CheckCircle />}
              onClick={() => bulkApproveMutation.mutate(Array.from(selected))}
              disabled={bulkApproveMutation.isPending}
            >
              Approve Selected ({selected.size})
            </Button>
          }
        >
          {selected.size} request(s) selected
        </Alert>
      )}

      {/* Pending Requests */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Pending color="warning" /> Pending Approvals ({data?.total || 0})
            </Typography>
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : data?.requests?.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#10B981', 0.05) }}>
              <CheckCircle sx={{ fontSize: 48, color: 'success.main', mb: 2 }} />
              <Typography color="text.secondary">
                All caught up! No pending approvals.
              </Typography>
            </Paper>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={!!data?.requests?.length && selected.size === data?.requests?.length}
                        indeterminate={!!data?.requests?.length && selected.size > 0 && selected.size < data?.requests?.length}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                      />
                    </TableCell>
                    <TableCell>HCF</TableCell>
                    <TableCell>Facility</TableCell>
                    <TableCell>Agreement No.</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Submitted</TableCell>
                    <TableCell>Notes</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data?.requests?.map((req: ClearanceRequest) => (
                    <TableRow key={req.id} hover selected={selected.has(req.id)}>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selected.has(req.id)}
                          onChange={(e) => handleSelect(req.id, e.target.checked)}
                        />
                      </TableCell>

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
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Business fontSize="small" sx={{ color: 'primary.main', opacity: 0.7 }} />
                          <Typography variant="body2">{req.facilityName}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                         <Typography variant="body2" sx={{ fontFamily: 'monospace', bgcolor: alpha('#fff', 0.05), py: 0.5, px: 1, borderRadius: 1, display: 'inline-block' }}>
                           {req.agreementNumber || '-'}
                         </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'success.light' }}>
                           {req.amountCleared ? `₹${req.amountCleared.toLocaleString()}` : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {req.submittedAt ? new Date(req.submittedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 200, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {req.cbwtfNotes || '-'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                          <Button
                            size="small"
                            variant="outlined"
                            color="success"
                            startIcon={approveMutation.isPending ? <CircularProgress size={14} /> : <CheckCircle />}
                            onClick={() => approveMutation.mutate(req.id)}
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

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Clearance Request</DialogTitle>
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

export default DuesApprovals;
