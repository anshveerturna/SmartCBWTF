import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  Stack,
} from '@mui/material';
import {
  AccountBalance as BankIcon,
  Add as AddIcon,
  Star as StarIcon,
  StarBorder as StarBorderIcon,
  Block as BlockIcon,
} from '@mui/icons-material';
import apiClient from '../../api/client';

interface BankAccount {
  id: string;
  accountName: string;
  accountNumber: string;
  ifscCode: string;
  bankName: string;
  upiId: string | null;
  isPrimary: boolean;
  status: string;
  createdAt: string;
  disabledAt: string | null;
}

const fetchBankAccounts = async (): Promise<BankAccount[]> => {
  const { data } = await apiClient.get('/api/cbwtf/bank-accounts');
  // Handle both array and paginated response
  return Array.isArray(data) ? data : (data.content ?? []);
};

export default function BankAccounts() {
  const queryClient = useQueryClient();
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [disableDialogOpen, setDisableDialogOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<BankAccount | null>(null);
  const [formData, setFormData] = useState({
    accountName: '',
    accountNumber: '',
    ifscCode: '',
    bankName: '',
    upiId: '',
  });

  const { data: accounts, isLoading, error } = useQuery({
    queryKey: ['bankAccounts'],
    queryFn: fetchBankAccounts,
  });

  const createMutation = useMutation({
    mutationFn: (data: typeof formData) => apiClient.post('/api/cbwtf/bank-accounts', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
      setAddDialogOpen(false);
      resetForm();
    },
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/cbwtf/bank-accounts/${id}/set-primary`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bankAccounts'] }),
  });

  const disableMutation = useMutation({
    mutationFn: (id: string) => apiClient.put(`/api/cbwtf/bank-accounts/${id}/disable`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
      setDisableDialogOpen(false);
      setSelectedAccount(null);
    },
  });

  const resetForm = () => {
    setFormData({ accountName: '', accountNumber: '', ifscCode: '', bankName: '', upiId: '' });
  };

  const handleSubmit = () => {
    createMutation.mutate(formData);
  };

  const handleDisable = (account: BankAccount) => {
    setSelectedAccount(account);
    setDisableDialogOpen(true);
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">Failed to load bank accounts</Alert>;
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <BankIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Box>
            <Typography variant="h4" fontWeight="bold">
              Bank Accounts
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Manage accounts for receiving payments
            </Typography>
          </Box>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddDialogOpen(true)}>
          Add Account
        </Button>
      </Box>

      {/* Accounts Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Bank</TableCell>
              <TableCell>Account Name</TableCell>
              <TableCell>Account Number</TableCell>
              <TableCell>IFSC</TableCell>
              <TableCell>UPI ID</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {accounts?.map((account) => (
              <TableRow key={account.id}>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    {account.isPrimary && (
                      <StarIcon sx={{ color: 'warning.main', fontSize: 20 }} />
                    )}
                    {account.bankName}
                  </Box>
                </TableCell>
                <TableCell>{account.accountName}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>
                  {account.accountNumber.slice(-4).padStart(account.accountNumber.length, '•')}
                </TableCell>
                <TableCell>{account.ifscCode}</TableCell>
                <TableCell>{account.upiId || '-'}</TableCell>
                <TableCell>
                  <Chip
                    label={account.status}
                    color={account.status === 'ACTIVE' ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right">
                  <Stack direction="row" spacing={1} justifyContent="flex-end">
                    {account.status === 'ACTIVE' && !account.isPrimary && (
                      <IconButton
                        size="small"
                        title="Set as Primary"
                        onClick={() => setPrimaryMutation.mutate(account.id)}
                      >
                        <StarBorderIcon />
                      </IconButton>
                    )}
                    {account.status === 'ACTIVE' && (
                      <IconButton
                        size="small"
                        color="error"
                        title="Disable Account"
                        onClick={() => handleDisable(account)}
                      >
                        <BlockIcon />
                      </IconButton>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
            {accounts?.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    No bank accounts configured. Add one to start receiving payments.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Add Account Dialog */}
      <Dialog open={addDialogOpen} onClose={() => setAddDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Bank Account</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Bank Name"
              fullWidth
              value={formData.bankName}
              onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
            />
            <TextField
              label="Account Name"
              fullWidth
              value={formData.accountName}
              onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
            />
            <TextField
              label="Account Number"
              fullWidth
              value={formData.accountNumber}
              onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
            />
            <TextField
              label="IFSC Code"
              fullWidth
              value={formData.ifscCode}
              onChange={(e) => setFormData({ ...formData, ifscCode: e.target.value.toUpperCase() })}
            />
            <TextField
              label="UPI ID (Optional)"
              fullWidth
              value={formData.upiId}
              onChange={(e) => setFormData({ ...formData, upiId: e.target.value })}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={createMutation.isPending}
          >
            {createMutation.isPending ? 'Adding...' : 'Add Account'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Disable Confirmation Dialog */}
      <Dialog open={disableDialogOpen} onClose={() => setDisableDialogOpen(false)}>
        <DialogTitle>Disable Bank Account?</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to disable <strong>{selectedAccount?.accountName}</strong>?
            This account will no longer be available for new payments.
          </Typography>
          <Alert severity="warning" sx={{ mt: 2 }}>
            Past payments linked to this account will not be affected.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDisableDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => selectedAccount && disableMutation.mutate(selectedAccount.id)}
            disabled={disableMutation.isPending}
          >
            {disableMutation.isPending ? 'Disabling...' : 'Disable Account'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
