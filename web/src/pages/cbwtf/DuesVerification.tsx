import { useState } from 'react';
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
  Alert,
  alpha,
} from '@mui/material';
import {
  CheckCircle,
  Pending,
  Send as SendIcon,
  CurrencyRupee
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

// ... imports

interface DuesRequest {
  id: string;
  hcfName: string;
  hcfCode: string;
  agreementNumber: string;
  requestedAt: string;
  status: string;
  amount?: number;
  cbwtfNotes?: string;
}

export default function DuesVerification() {
  const queryClient = useQueryClient();
  const [submitDialogOpen, setSubmitDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  
  // Submit state
  const [amountCleared, setAmountCleared] = useState('');
  const [notes, setNotes] = useState('');

  // Reject state
  const [outstandingAmount, setOutstandingAmount] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  
  // Fetch my facility's requests
  const { data: requests, isLoading } = useQuery<DuesRequest[]>({
    queryKey: ['cbwtf-dues-requests'],
    queryFn: () => apiClient.get<DuesRequest[]>('/api/cbwtf/dues-clearance?status=PENDING').then((res) => res.data),
  });

  // Submit Mutation
  const submitMutation = useMutation({
    mutationFn: async ({ id, amount, notes }: { id: string; amount: number; notes: string }) => {
      await apiClient.post(`/api/cbwtf/dues-clearance/${id}/submit`, { amount, notes });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-dues-requests'] });
      setSubmitDialogOpen(false);
      resetForm();
    },
  });

  // Reject Mutation
  const rejectMutation = useMutation({
    mutationFn: async ({ id, amount, reason }: { id: string; amount: number; reason: string }) => {
      await apiClient.post(`/api/cbwtf/dues-clearance/${id}/reject`, { amount, reason });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-dues-requests'] });
      setRejectDialogOpen(false);
      resetForm();
    },
  });

  const resetForm = () => {
    setAmountCleared('');
    setNotes('');
    setOutstandingAmount('');
    setRejectReason('');
    setSelectedId(null);
  };

  const handleOpenSubmit = (id: string) => {
    setSelectedId(id);
    setSubmitDialogOpen(true);
  };

  const handleOpenReject = (id: string) => {
    setSelectedId(id);
    setRejectDialogOpen(true);
  };

  return (
    <Box>
       <Box sx={{ mb: 4 }}>
        <Typography variant="h4" fontWeight={700}>
          Dues Verification
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Verify HCF dues are cleared and submit requests to management for approval.
        </Typography>
      </Box>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Pending color="primary" /> Pending Requests
          </Typography>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
               <CircularProgress />
            </Box>
          ) : !requests || requests.length === 0 ? (
             <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#3b82f6', 0.05) }}>
               <CheckCircle sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
               <Typography color="text.secondary">No pending requests.</Typography>
             </Paper>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                     <TableCell>HCF</TableCell>
                     <TableCell>Agreement No.</TableCell>
                     <TableCell>Requested At</TableCell>
                     <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {requests.map(req => (
                    <TableRow key={req.id}>
                      <TableCell>
                        <Box>
                          <Typography variant="body2" fontWeight={600}>{req.hcfName}</Typography>
                          <Typography variant="caption" color="text.secondary">{req.hcfCode}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{req.agreementNumber || '-'}</TableCell>
                      <TableCell>{new Date(req.requestedAt).toLocaleDateString()}</TableCell>
                      <TableCell align="right">
                        <Button
                           size="small"
                           variant="contained"
                           color="success"
                           onClick={() => handleOpenSubmit(req.id)}
                           startIcon={<SendIcon />}
                           sx={{ mr: 1 }}
                        >
                          Confirm Cleared
                        </Button>
                        <Button
                           size="small"
                           color="error"
                           onClick={() => handleOpenReject(req.id)}
                        >
                          Reject
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Submit Dialog */}
      <Dialog open={submitDialogOpen} onClose={() => setSubmitDialogOpen(false)} maxWidth="sm" fullWidth>
         <DialogTitle>Confirm Dues Cleared</DialogTitle>
         <DialogContent>
           <Alert severity="info" sx={{ mb: 3 }}>
             By submitting this, you confirm that you have verified the payment offline.
           </Alert>
           <TextField
             label="Amount Cleared (₹)"
             type="number"
             fullWidth
             required
             value={amountCleared}
             onChange={(e) => setAmountCleared(e.target.value)}
             sx={{ mb: 2 }}
             InputProps={{
               startAdornment: <CurrencyRupee color="action" />
             }}
           />
           <TextField
             label="Notes for Management (optional)"
             fullWidth
             multiline
             rows={2}
             value={notes}
             onChange={(e) => setNotes(e.target.value)}
             placeholder="e.g. Verified via bank statement"
           />
         </DialogContent>
         <DialogActions>
           <Button onClick={() => setSubmitDialogOpen(false)}>Cancel</Button>
           <Button 
             variant="contained"
             color="success"
             onClick={() => selectedId && submitMutation.mutate({ 
               id: selectedId, 
               amount: parseFloat(amountCleared),
               notes 
             })}
             disabled={!amountCleared || submitMutation.isPending}
           >
             {submitMutation.isPending ? 'Submitting...' : 'Submit for Approval'}
           </Button>
         </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
         <DialogTitle>Reject Request</DialogTitle>
         <DialogContent>
           <Alert severity="warning" sx={{ mb: 3 }}>
             Please specify the outstanding dues amount. This will be shown to the HCF.
           </Alert>
           <TextField
             label="Outstanding Dues Amount (₹)"
             type="number"
             fullWidth
             required
             value={outstandingAmount}
             onChange={(e) => setOutstandingAmount(e.target.value)}
             sx={{ mb: 2 }}
             InputProps={{
               startAdornment: <CurrencyRupee color="action" />
             }}
           />
           <TextField
             label="Rejection Reason"
             fullWidth
             required
             multiline
             rows={2}
             value={rejectReason}
             onChange={(e) => setRejectReason(e.target.value)}
             placeholder="e.g. Dues pending for Dec 2025"
           />
         </DialogContent>
         <DialogActions>
           <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
           <Button 
             variant="contained"
             color="error"
             onClick={() => selectedId && rejectMutation.mutate({ 
               id: selectedId, 
               amount: parseFloat(outstandingAmount),
               reason: rejectReason 
             })}
             disabled={!outstandingAmount || !rejectReason || rejectMutation.isPending}
           >
             {rejectMutation.isPending ? 'Rejecting...' : 'Reject Request'}
           </Button>
         </DialogActions>
      </Dialog>
    </Box>
  );
}
