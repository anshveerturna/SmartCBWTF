import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, Button, TextField, Stack, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert, Skeleton,
} from '@mui/material';
import {
  Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon,
  Star as StarIcon, StarBorder as StarBorderIcon,
  AccountBalance as BankIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import apiClient from '../../../api/client';

interface BankAccountDTO {
  id: string;
  accountName: string;
  accountNumber: string;
  ifscCode: string;
  bankName: string;
  branchName: string | null;
  isPrimary: boolean;
  createdAt: string;
}

const bankAccountApi = {
  list: async (): Promise<BankAccountDTO[]> => {
    const res = await apiClient.get('/api/cbwtf/bank-accounts');
    return res.data;
  },
  create: async (data: Omit<BankAccountDTO, 'id' | 'isPrimary' | 'createdAt'>): Promise<BankAccountDTO> => {
    const res = await apiClient.post('/api/cbwtf/bank-accounts', data);
    return res.data;
  },
  update: async (id: string, data: Omit<BankAccountDTO, 'id' | 'isPrimary' | 'createdAt'>): Promise<BankAccountDTO> => {
    const res = await apiClient.put(`/api/cbwtf/bank-accounts/${id}`, data);
    return res.data;
  },
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/cbwtf/bank-accounts/${id}`);
  },
  setPrimary: async (id: string): Promise<BankAccountDTO> => {
    const res = await apiClient.post(`/api/cbwtf/bank-accounts/${id}/set-primary`);
    return res.data;
  },
};

const emptyForm = { accountName: '', accountNumber: '', ifscCode: '', bankName: '', branchName: '' };

export default function BankAccounts() {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { data: accounts = [], isLoading, error } = useQuery({
    queryKey: ['bank-accounts'],
    queryFn: bankAccountApi.list,
  });

  const createMutation = useMutation({
    mutationFn: bankAccountApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }); closeDialog(); },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: typeof emptyForm }) => bankAccountApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }); closeDialog(); },
  });

  const deleteMutation = useMutation({
    mutationFn: bankAccountApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }); setDeleteId(null); },
  });

  const primaryMutation = useMutation({
    mutationFn: bankAccountApi.setPrimary,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }),
  });

  const openCreate = () => { setForm(emptyForm); setEditingId(null); setDialogOpen(true); };
  const openEdit = (account: BankAccountDTO) => {
    setForm({ accountName: account.accountName, accountNumber: account.accountNumber, ifscCode: account.ifscCode, bankName: account.bankName, branchName: account.branchName || '' });
    setEditingId(account.id);
    setDialogOpen(true);
  };
  const closeDialog = () => { setDialogOpen(false); setEditingId(null); setForm(emptyForm); };

  const handleSubmit = () => {
    if (editingId) {
      updateMutation.mutate({ id: editingId, data: form });
    } else {
      createMutation.mutate(form);
    }
  };

  const columns: GridColDef<BankAccountDTO>[] = [
    {
      field: 'isPrimary', headerName: '', width: 50,
      renderCell: (params: GridRenderCellParams<BankAccountDTO>) => (
        <IconButton size="small" onClick={() => !params.value && primaryMutation.mutate(params.row.id)} disabled={params.value}>
          {params.value ? <StarIcon color="warning" /> : <StarBorderIcon />}
        </IconButton>
      ),
    },
    { field: 'accountName', headerName: 'Account Name', flex: 1, minWidth: 150 },
    { field: 'bankName', headerName: 'Bank', flex: 1, minWidth: 120 },
    { field: 'accountNumber', headerName: 'Account Number', width: 160, renderCell: (p) => <Typography fontFamily="monospace">{p.value}</Typography> },
    { field: 'ifscCode', headerName: 'IFSC', width: 120, renderCell: (p) => <Typography fontFamily="monospace">{p.value}</Typography> },
    { field: 'branchName', headerName: 'Branch', width: 140, renderCell: (p) => p.value || '-' },
    {
      field: 'actions', headerName: '', width: 100, sortable: false,
      renderCell: (params: GridRenderCellParams<BankAccountDTO>) => (
        <Stack direction="row">
          <IconButton size="small" onClick={() => openEdit(params.row)}><EditIcon fontSize="small" /></IconButton>
          <IconButton size="small" color="error" onClick={() => setDeleteId(params.row.id)} disabled={params.row.isPrimary}><DeleteIcon fontSize="small" /></IconButton>
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>Bank Accounts</Typography>
          <Typography variant="body2" color="text.secondary">Manage bank accounts for receiving payments</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>Add Account</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>Failed to load bank accounts</Alert>}

      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>{[...Array(3)].map((_, i) => <Skeleton key={i} height={56} />)}</Box>
          ) : accounts.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <BankIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
              <Typography color="text.secondary">No bank accounts added yet</Typography>
              <Button variant="outlined" sx={{ mt: 2 }} onClick={openCreate}>Add Your First Account</Button>
            </Box>
          ) : (
            <DataGrid rows={accounts} columns={columns} autoHeight disableRowSelectionOnClick
              sx={{ border: 'none', '& .MuiDataGrid-cell': { borderColor: 'divider' } }} />
          )}
        </CardContent>
      </Card>

      {/* Add/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit Bank Account' : 'Add Bank Account'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Account Name" fullWidth value={form.accountName} onChange={(e) => setForm({ ...form, accountName: e.target.value })} placeholder="e.g., Primary Business Account" />
            <TextField label="Bank Name" fullWidth value={form.bankName} onChange={(e) => setForm({ ...form, bankName: e.target.value })} />
            <Stack direction="row" spacing={2}>
              <TextField label="Account Number" fullWidth value={form.accountNumber} onChange={(e) => setForm({ ...form, accountNumber: e.target.value })} />
              <TextField label="IFSC Code" fullWidth value={form.ifscCode} onChange={(e) => setForm({ ...form, ifscCode: e.target.value.toUpperCase() })} />
            </Stack>
            <TextField label="Branch Name (Optional)" fullWidth value={form.branchName} onChange={(e) => setForm({ ...form, branchName: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={!form.accountName || !form.accountNumber || !form.ifscCode || !form.bankName || createMutation.isPending || updateMutation.isPending}>
            {editingId ? 'Update' : 'Add Account'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={!!deleteId} onClose={() => setDeleteId(null)}>
        <DialogTitle>Delete Bank Account?</DialogTitle>
        <DialogContent>
          <Typography>This action cannot be undone. The primary account cannot be deleted.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteId(null)}>Cancel</Button>
          <Button color="error" onClick={() => deleteId && deleteMutation.mutate(deleteId)} disabled={deleteMutation.isPending}>Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
