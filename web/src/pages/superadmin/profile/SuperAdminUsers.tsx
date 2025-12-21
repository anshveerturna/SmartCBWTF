import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  TextField,
  Avatar,
  Typography,
  Chip,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  InputAdornment,
  Tooltip,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Search as SearchIcon,
  Edit as EditIcon,
  Block as BlockIcon,
  CheckCircle as CheckCircleIcon,
  LockReset as LockResetIcon,
} from '@mui/icons-material';
import {
  listSuperAdminUsers,
  createSuperAdmin,
  updateSuperAdmin,
  disableSuperAdmin,
  enableSuperAdmin,
  resetSuperAdminPassword,
} from '../../../api/superadminProfile';
import type { SuperAdminUser, CreateSuperAdminRequest } from '../../../api/superadminProfile';

const SuperAdminUsers: React.FC = () => {
  const [users, setUsers] = useState<SuperAdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalRows, setTotalRows] = useState(0);

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateSuperAdminRequest>({
    username: '', fullName: '', email: '', phone: '', password: ''
  });

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editUser, setEditUser] = useState<SuperAdminUser | null>(null);
  const [editForm, setEditForm] = useState({ fullName: '', email: '', phone: '' });

  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordUser, setPasswordUser] = useState<SuperAdminUser | null>(null);
  const [tempPassword, setTempPassword] = useState<string | null>(null);

  useEffect(() => {
    loadUsers();
  }, [page, search]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await listSuperAdminUsers({
        search: search || undefined,
        page,
        size: pageSize
      });
      setUsers(response.content);
      setTotalRows(response.totalElements);
    } catch {
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    try {
      await createSuperAdmin(createForm);
      setSuccess('SuperAdmin created successfully');
      setCreateDialogOpen(false);
      setCreateForm({ username: '', fullName: '', email: '', phone: '', password: '' });
      loadUsers();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Failed to create user');
    }
  };

  const handleEdit = async () => {
    if (!editUser) return;
    try {
      await updateSuperAdmin(editUser.id, editForm);
      setSuccess('User updated successfully');
      setEditDialogOpen(false);
      loadUsers();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Failed to update user');
    }
  };

  const handleDisable = async (user: SuperAdminUser) => {
    if (!window.confirm(`Disable ${user.username}? They will no longer be able to log in.`)) return;
    try {
      await disableSuperAdmin(user.id);
      setSuccess('User disabled successfully');
      loadUsers();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Failed to disable user');
    }
  };

  const handleEnable = async (user: SuperAdminUser) => {
    try {
      await enableSuperAdmin(user.id);
      setSuccess('User enabled successfully');
      loadUsers();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Failed to enable user');
    }
  };

  const handleResetPassword = async () => {
    if (!passwordUser) return;
    try {
      const result = await resetSuperAdminPassword(passwordUser.id);
      setTempPassword(result.temporaryPassword);
      setSuccess('Password reset successfully');
      loadUsers();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setError(errObj.response?.data?.message || 'Failed to reset password');
    }
  };

  const openEditDialog = (user: SuperAdminUser) => {
    setEditUser(user);
    setEditForm({ fullName: user.fullName || '', email: user.email || '', phone: user.phone || '' });
    setEditDialogOpen(true);
  };

  const openPasswordDialog = (user: SuperAdminUser) => {
    setPasswordUser(user);
    setTempPassword(null);
    setPasswordDialogOpen(true);
  };

  const columns: GridColDef[] = [
    {
      field: 'photo',
      headerName: '',
      width: 60,
      sortable: false,
      renderCell: (params) => (
        <Avatar
          src={params.row.profilePhotoUrl ? `http://localhost:8080${params.row.profilePhotoUrl}` : undefined}
          sx={{ width: 36, height: 36 }}
        >
          {params.row.fullName?.charAt(0) || params.row.username?.charAt(0)}
        </Avatar>
      )
    },
    { field: 'username', headerName: 'Username', flex: 1 },
    { field: 'fullName', headerName: 'Full Name', flex: 1 },
    { field: 'email', headerName: 'Email', flex: 1.2 },
    { field: 'phone', headerName: 'Phone', flex: 0.8 },
    {
      field: 'active',
      headerName: 'Status',
      width: 100,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'Active' : 'Disabled'}
          color={params.value ? 'success' : 'error'}
          size="small"
        />
      )
    },
    {
      field: 'lastLoginAt',
      headerName: 'Last Login',
      width: 140,
      valueFormatter: (value) => value ? new Date(value).toLocaleDateString() : 'Never'
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 180,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => openEditDialog(params.row)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Reset Password">
            <IconButton size="small" onClick={() => openPasswordDialog(params.row)}>
              <LockResetIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {params.row.active ? (
            <Tooltip title="Disable">
              <IconButton size="small" color="error" onClick={() => handleDisable(params.row)}>
                <BlockIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Enable">
              <IconButton size="small" color="success" onClick={() => handleEnable(params.row)}>
                <CheckCircleIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      )
    }
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Manage SuperAdmins</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)}>
          Create SuperAdmin
        </Button>
      </Box>

      <TextField
        placeholder="Search by username or email..."
        size="small"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        InputProps={{
          startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>
        }}
        sx={{ mb: 2, width: 300 }}
      />

      <Box sx={{ width: '100%' }}>
        <DataGrid
          rows={users}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={totalRows}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => setPage(m.page)}
          pageSizeOptions={[10]}
          disableRowSelectionOnClick
          autoHeight
          sx={{ 
            bgcolor: 'background.paper', 
            borderRadius: 2,
            width: '100%',
            '& .MuiDataGrid-columnHeaders': {
              bgcolor: 'background.default',
            },
            '& .MuiDataGrid-cell': {
              display: 'flex',
              alignItems: 'center',
            },
          }}
        />
      </Box>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create SuperAdmin</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField fullWidth label="Username" value={createForm.username}
              onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })} />
            <TextField fullWidth label="Full Name" value={createForm.fullName}
              onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })} />
            <Grid container spacing={2}>
              <Grid size={6}>
                <TextField fullWidth label="Email" value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })} />
              </Grid>
              <Grid size={6}>
                <TextField fullWidth label="Phone" value={createForm.phone}
                  onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })} />
              </Grid>
            </Grid>
            <TextField fullWidth type="password" label="Password" value={createForm.password}
              onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
              helperText="User will be required to change password on first login" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>Create</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit SuperAdmin</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField fullWidth label="Full Name" value={editForm.fullName}
              onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })} />
            <Grid container spacing={2}>
              <Grid size={6}>
                <TextField fullWidth label="Email" value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} />
              </Grid>
              <Grid size={6}>
                <TextField fullWidth label="Phone" value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
              </Grid>
            </Grid>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleEdit}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Password Reset Dialog */}
      <Dialog open={passwordDialogOpen} onClose={() => setPasswordDialogOpen(false)}>
        <DialogTitle>Reset Password for {passwordUser?.username}</DialogTitle>
        <DialogContent>
          {tempPassword ? (
            <Alert severity="success" sx={{ mt: 2 }}>
              Temporary password: <strong>{tempPassword}</strong>
              <br />User must change this on next login.
            </Alert>
          ) : (
            <Typography sx={{ mt: 2 }}>
              This will generate a temporary password. The user will be required to change it on their next login.
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialogOpen(false)}>Close</Button>
          {!tempPassword && (
            <Button variant="contained" color="warning" onClick={handleResetPassword}>
              Reset Password
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Snackbar open={!!success} autoHideDuration={3000} onClose={() => setSuccess(null)}>
        <Alert severity="success">{success}</Alert>
      </Snackbar>
      <Snackbar open={!!error} autoHideDuration={5000} onClose={() => setError(null)}>
        <Alert severity="error">{error}</Alert>
      </Snackbar>
    </Box>
  );
};

export default SuperAdminUsers;
