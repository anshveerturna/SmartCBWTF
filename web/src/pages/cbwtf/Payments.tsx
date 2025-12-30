import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  Stack,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  InputAdornment,
  Tabs,
  Tab,
  IconButton,
} from '@mui/material';
import {
  Payment as PaymentIcon,
  Add as AddIcon,
  CurrencyRupee as RupeeIcon,
  TrendingUp as TrendingUpIcon,
  AccountBalance as BalanceIcon,
  Star as StarIcon,
  StarBorder as StarBorderIcon,
  Block as BlockIcon,
} from '@mui/icons-material';
import axios from 'axios';

interface Payment {
  id: string;
  hcfId: string;
  hcfName: string;
  paymentDate: string;
  amount: number;
  mode: string;
  referenceNumber: string | null;
  payerName: string | null;
  bankName: string | null;
  createdAt: string;
}

interface Summary {
  totalOutstanding: number;
  collectedMTD: number;
  totalAdvance: number;
}

interface Hcf {
  id: string;
  name: string;
}

interface BankAccount {
  id: string;
  bankName: string;
  accountName: string;
  accountNumber: string;
  ifscCode: string;
  upiId: string | null;
  isPrimary: boolean;
  status: string;
}

const api = axios.create({ baseURL: '/api/cbwtf' });

const fetchPayments = async (): Promise<{ content: Payment[] }> => {
  const { data } = await api.get('/payments');
  return data;
};

const fetchSummary = async (): Promise<Summary> => {
  const { data } = await api.get('/payments/summary');
  return data;
};

const fetchHcfs = async (): Promise<Hcf[]> => {
  const { data } = await api.get('/hcfs');
  return Array.isArray(data) ? data : (data.content ?? []);
};

const fetchBankAccounts = async (): Promise<BankAccount[]> => {
  const { data } = await api.get('/bank-accounts');
  return Array.isArray(data) ? data : (data.content ?? []);
};

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(amount);
};

export default function Payments() {
  const queryClient = useQueryClient();
  const [tabIndex, setTabIndex] = useState(0);
  const [recordDialogOpen, setRecordDialogOpen] = useState(false);
  const [addBankDialogOpen, setAddBankDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    hcfId: '',
    bankAccountId: '',
    paymentDate: new Date().toISOString().split('T')[0],
    amount: '',
    mode: 'UPI',
    referenceNumber: '',
    payerName: '',
    notes: '',
  });
  const [bankFormData, setBankFormData] = useState({
    accountName: '',
    accountNumber: '',
    ifscCode: '',
    bankName: '',
    upiId: '',
  });

  const { data: paymentsData, isLoading } = useQuery({
    queryKey: ['payments'],
    queryFn: fetchPayments,
  });

  const { data: summary } = useQuery({
    queryKey: ['paymentsSummary'],
    queryFn: fetchSummary,
  });

  const { data: hcfs } = useQuery({
    queryKey: ['hcfsForPayment'],
    queryFn: fetchHcfs,
  });

  const { data: bankAccounts } = useQuery({
    queryKey: ['bankAccounts'],
    queryFn: fetchBankAccounts,
  });

  const recordMutation = useMutation({
    mutationFn: (data: typeof formData) => api.post('/payments', {
      ...data,
      amount: parseFloat(data.amount),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['paymentsSummary'] });
      setRecordDialogOpen(false);
      resetForm();
    },
  });

  const createBankMutation = useMutation({
    mutationFn: (data: typeof bankFormData) => api.post('/bank-accounts', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
      setAddBankDialogOpen(false);
      setBankFormData({ accountName: '', accountNumber: '', ifscCode: '', bankName: '', upiId: '' });
    },
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (id: string) => api.post(`/bank-accounts/${id}/set-primary`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bankAccounts'] }),
  });

  const disableMutation = useMutation({
    mutationFn: (id: string) => api.put(`/bank-accounts/${id}/disable`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bankAccounts'] }),
  });

  const resetForm = () => {
    setFormData({
      hcfId: '',
      bankAccountId: '',
      paymentDate: new Date().toISOString().split('T')[0],
      amount: '',
      mode: 'UPI',
      referenceNumber: '',
      payerName: '',
      notes: '',
    });
  };

  const handleSubmit = () => {
    if (!formData.hcfId || !formData.amount) return;
    recordMutation.mutate(formData);
  };

  const getModeColor = (mode: string) => {
    switch (mode) {
      case 'UPI': return 'primary';
      case 'NET_BANKING': return 'secondary';
      case 'DEBIT_CARD': return 'info';
      case 'CREDIT_CARD': return 'warning';
      default: return 'default';
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  const payments = paymentsData?.content || [];
  const activeBankAccounts = bankAccounts?.filter(a => a.status === 'ACTIVE') || [];

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <PaymentIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Box>
            <Typography variant="h4" fontWeight="bold">
              Payments
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Record payments and manage bank accounts
            </Typography>
          </Box>
        </Box>
        {tabIndex === 0 ? (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setRecordDialogOpen(true)}>
            Record Payment
          </Button>
        ) : (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddBankDialogOpen(true)}>
            Add Bank Account
          </Button>
        )}
      </Box>

      {/* Tabs */}
      <Tabs value={tabIndex} onChange={(_, v) => setTabIndex(v)} sx={{ mb: 3 }}>
        <Tab label="Payments" />
        <Tab label="Bank Accounts" />
      </Tabs>

      {/* Tab 0: Payments */}
      {tabIndex === 0 && (
        <>
          {/* Summary Cards */}
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3} mb={3}>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Box display="flex" alignItems="center" gap={2}>
                  <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'error.light' }}>
                    <RupeeIcon sx={{ color: 'error.main' }} />
                  </Box>
                  <Box>
                    <Typography variant="body2" color="text.secondary">Outstanding</Typography>
                    <Typography variant="h5" fontWeight="bold">
                      {formatCurrency(summary?.totalOutstanding || 0)}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Box display="flex" alignItems="center" gap={2}>
                  <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'success.light' }}>
                    <TrendingUpIcon sx={{ color: 'success.main' }} />
                  </Box>
                  <Box>
                    <Typography variant="body2" color="text.secondary">Collected (MTD)</Typography>
                    <Typography variant="h5" fontWeight="bold">
                      {formatCurrency(summary?.collectedMTD || 0)}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Box display="flex" alignItems="center" gap={2}>
                  <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'info.light' }}>
                    <BalanceIcon sx={{ color: 'info.main' }} />
                  </Box>
                  <Box>
                    <Typography variant="body2" color="text.secondary">Advance Balance</Typography>
                    <Typography variant="h5" fontWeight="bold">
                      {formatCurrency(summary?.totalAdvance || 0)}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Stack>

          {/* Payments Table */}
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>HCF</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell>Mode</TableCell>
                  <TableCell>Reference</TableCell>
                  <TableCell>Bank</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {payments.map((payment) => (
                  <TableRow key={payment.id}>
                    <TableCell>{new Date(payment.paymentDate).toLocaleDateString('en-IN')}</TableCell>
                    <TableCell>{payment.hcfName}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 'bold', color: 'success.main' }}>
                      {formatCurrency(payment.amount)}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={payment.mode.replace('_', ' ')}
                        color={getModeColor(payment.mode) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell sx={{ fontFamily: 'monospace' }}>
                      {payment.referenceNumber || '-'}
                    </TableCell>
                    <TableCell>{payment.bankName || '-'}</TableCell>
                  </TableRow>
                ))}
                {payments.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">No payments recorded yet.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}

      {/* Tab 1: Bank Accounts */}
      {tabIndex === 1 && (
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
              {bankAccounts?.map((account) => (
                <TableRow key={account.id}>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {account.isPrimary && <StarIcon sx={{ color: 'warning.main', fontSize: 20 }} />}
                      {account.bankName}
                    </Box>
                  </TableCell>
                  <TableCell>{account.accountName}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>
                    {'•'.repeat(Math.max(0, account.accountNumber.length - 4))}{account.accountNumber.slice(-4)}
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
                        <IconButton size="small" title="Set as Primary" onClick={() => setPrimaryMutation.mutate(account.id)}>
                          <StarBorderIcon />
                        </IconButton>
                      )}
                      {account.status === 'ACTIVE' && (
                        <IconButton size="small" color="error" title="Disable Account" onClick={() => disableMutation.mutate(account.id)}>
                          <BlockIcon />
                        </IconButton>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {(!bankAccounts || bankAccounts.length === 0) && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No bank accounts configured.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Record Payment Dialog */}
      <Dialog open={recordDialogOpen} onClose={() => setRecordDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Payment</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>HCF</InputLabel>
              <Select
                value={formData.hcfId}
                label="HCF"
                onChange={(e) => setFormData({ ...formData, hcfId: e.target.value })}
              >
                {hcfs?.map((hcf) => (
                  <MenuItem key={hcf.id} value={hcf.id}>{hcf.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Payment Date"
              type="date"
              fullWidth
              value={formData.paymentDate}
              onChange={(e) => setFormData({ ...formData, paymentDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Amount"
              type="number"
              fullWidth
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
              InputProps={{ startAdornment: <InputAdornment position="start">₹</InputAdornment> }}
            />
            <FormControl fullWidth>
              <InputLabel>Payment Mode</InputLabel>
              <Select
                value={formData.mode}
                label="Payment Mode"
                onChange={(e) => setFormData({ ...formData, mode: e.target.value })}
              >
                <MenuItem value="UPI">UPI</MenuItem>
                <MenuItem value="NET_BANKING">Net Banking</MenuItem>
                <MenuItem value="DEBIT_CARD">Debit Card</MenuItem>
                <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Reference Number"
              fullWidth
              value={formData.referenceNumber}
              onChange={(e) => setFormData({ ...formData, referenceNumber: e.target.value })}
              placeholder="Transaction ID, UTR, etc."
            />
            <FormControl fullWidth>
              <InputLabel>Bank Account</InputLabel>
              <Select
                value={formData.bankAccountId}
                label="Bank Account"
                onChange={(e) => setFormData({ ...formData, bankAccountId: e.target.value })}
              >
                <MenuItem value="">-- Select --</MenuItem>
                {activeBankAccounts.map((acc) => (
                  <MenuItem key={acc.id} value={acc.id}>
                    {acc.bankName} - ****{acc.accountNumber.slice(-4)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Notes (Optional)"
              fullWidth
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              multiline
              rows={2}
            />
          </Stack>
          {recordMutation.isError && (
            <Alert severity="error" sx={{ mt: 2 }}>Failed to record payment.</Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRecordDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={recordMutation.isPending || !formData.hcfId || !formData.amount}>
            {recordMutation.isPending ? 'Recording...' : 'Record Payment'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add Bank Account Dialog */}
      <Dialog open={addBankDialogOpen} onClose={() => setAddBankDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Bank Account</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Bank Name" fullWidth value={bankFormData.bankName} onChange={(e) => setBankFormData({ ...bankFormData, bankName: e.target.value })} />
            <TextField label="Account Name" fullWidth value={bankFormData.accountName} onChange={(e) => setBankFormData({ ...bankFormData, accountName: e.target.value })} />
            <TextField label="Account Number" fullWidth value={bankFormData.accountNumber} onChange={(e) => setBankFormData({ ...bankFormData, accountNumber: e.target.value })} />
            <TextField label="IFSC Code" fullWidth value={bankFormData.ifscCode} onChange={(e) => setBankFormData({ ...bankFormData, ifscCode: e.target.value.toUpperCase() })} />
            <TextField label="UPI ID (Optional)" fullWidth value={bankFormData.upiId} onChange={(e) => setBankFormData({ ...bankFormData, upiId: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddBankDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => createBankMutation.mutate(bankFormData)} disabled={createBankMutation.isPending}>
            {createBankMutation.isPending ? 'Adding...' : 'Add Account'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
