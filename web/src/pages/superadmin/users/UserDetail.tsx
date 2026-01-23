import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Card, CardContent, Typography, Button, TextField, Stack, Alert, IconButton,
  Chip, Avatar, Divider, Grid, CircularProgress, Dialog, DialogTitle, DialogContent,
  DialogActions,
} from '@mui/material';
import {
  ArrowBack as BackIcon, Save as SaveIcon, LockReset as ResetIcon,
  Block as DisableIcon, CheckCircle as EnableIcon, LocationOn as LocationIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import { adminApi } from '../../../api/admin';
import type { UpdateUserRequest } from '../../../api/admin';
import apiClient from '../../../api/client';

const roleColors: Record<string, string> = {
  SUPER_ADMIN: '#ef4444', CBWTF_ADMIN: '#3b82f6', HCF_ADMIN: '#10b981',
  DRIVER: '#f59e0b', PLANT_OPERATOR: '#8b5cf6', ACCOUNTANT: '#06b6d4',
};

interface LocationData {
  latitude?: number;
  longitude?: number;
  accuracy?: number;
  recordedAt?: string;
  error?: string;
}

export default function UserDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState<UpdateUserRequest>({});
  const [passwordDialog, setPasswordDialog] = useState(false);
  const [newPassword, setNewPassword] = useState('');

  const { data: user, isLoading, error, refetch } = useQuery({
    queryKey: ['user', id],
    queryFn: () => adminApi.getUser(id!),
    enabled: !!id,
  });

  // Initialize form data when user loads
  useEffect(() => {
    if (user) {
      setFormData({
        fullName: user.fullName || undefined,
        email: user.email || undefined,
        phone: user.phone || undefined,
      });
    }
  }, [user]);

  const { data: location } = useQuery<LocationData>({
    queryKey: ['user-location', id],
    queryFn: () => apiClient.get(`/api/admin/users/${id}/location`).then(r => r.data),
    enabled: !!id && !!user && ['DRIVER', 'PLANT_OPERATOR'].includes(user.role),
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateUserRequest) => adminApi.updateUser(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', id] });
      setEditing(false);
    },
  });

  const disableMutation = useMutation({
    mutationFn: (reason: string) => adminApi.disableUser(id!, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', id] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const enableMutation = useMutation({
    mutationFn: () => adminApi.enableUser(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', id] });
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const passwordMutation = useMutation({
    mutationFn: (password: string) => adminApi.changeUserPassword(id!, password),
    onSuccess: () => {
      setPasswordDialog(false);
      setNewPassword('');
      refetch();
    },
  });

  const handleSave = () => {
    updateMutation.mutate(formData);
  };

  const handlePasswordReset = () => {
    if (newPassword.length >= 8) {
      passwordMutation.mutate(newPassword);
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !user) {
    return (
      <Box>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/superadmin/users')}>Back</Button>
        <Alert severity="error" sx={{ mt: 2 }}>User not found.</Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3, gap: 2 }}>
        <IconButton onClick={() => navigate('/superadmin/users')}><BackIcon /></IconButton>
        <Avatar sx={{ width: 56, height: 56, bgcolor: roleColors[user.role] || '#64748b' }}>
          {user.fullName?.charAt(0) || <PersonIcon />}
        </Avatar>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h5" fontWeight={700}>{user.fullName || user.username}</Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip label={user.role?.replace('_', ' ')} size="small" sx={{ bgcolor: roleColors[user.role], color: '#fff', fontWeight: 600 }} />
            <Chip label={user.active ? 'Active' : 'Disabled'} size="small" color={user.active ? 'success' : 'error'} variant="outlined" />
          </Stack>
        </Box>
        {!editing ? (
          <Button variant="contained" onClick={() => setEditing(true)}>Edit</Button>
        ) : (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" onClick={() => setEditing(false)}>Cancel</Button>
            <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} disabled={updateMutation.isPending}>Save</Button>
          </Stack>
        )}
      </Box>

      <Grid container spacing={3}>
        {/* Basic Info */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>User Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Stack spacing={2}>
                <TextField
                  label="Full Name" fullWidth
                  value={formData.fullName || ''} disabled={!editing}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                />
                <TextField
                  label="Email" fullWidth type="email"
                  value={formData.email || ''} disabled={!editing}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
                <TextField
                  label="Phone" fullWidth
                  value={formData.phone || ''} disabled={!editing}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Actions */}
        <Grid item xs={12} md={4}>
          <Stack spacing={2}>
            {/* CBWTF Info */}
            <Card>
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">CBWTF</Typography>
                <Typography variant="body1">{user.cbwtfName || '-'}</Typography>
                <Typography variant="caption" color="text.secondary">ID: {user.cbwtfId || '-'}</Typography>
              </CardContent>
            </Card>

            {/* Location (for operational roles) */}
            {['DRIVER', 'PLANT_OPERATOR'].includes(user.role) && (
              <Card>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <LocationIcon fontSize="small" /> Last Location
                  </Typography>
                  {location?.latitude ? (
                    <>
                      <Typography variant="body2">Lat: {location.latitude?.toFixed(6)}</Typography>
                      <Typography variant="body2">Lon: {location.longitude?.toFixed(6)}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {location.recordedAt ? new Date(location.recordedAt).toLocaleString() : 'Never'}
                      </Typography>
                    </>
                  ) : (
                    <Typography variant="body2" color="text.secondary">No location recorded</Typography>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Quick Actions */}
            <Card>
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>Actions</Typography>
                <Stack spacing={1}>
                  <Button fullWidth variant="outlined" startIcon={<ResetIcon />} onClick={() => setPasswordDialog(true)}>
                    Reset Password
                  </Button>
                  {user.active ? (
                    <Button fullWidth variant="outlined" color="error" startIcon={<DisableIcon />}
                      onClick={() => disableMutation.mutate('Disabled by SuperAdmin')}
                      disabled={disableMutation.isPending}>
                      Disable User
                    </Button>
                  ) : (
                    <Button fullWidth variant="outlined" color="success" startIcon={<EnableIcon />}
                      onClick={() => enableMutation.mutate()}
                      disabled={enableMutation.isPending}>
                      Enable User
                    </Button>
                  )}
                </Stack>
              </CardContent>
            </Card>

            {/* Created/Updated */}
            <Card>
              <CardContent>
                <Typography variant="caption" color="text.secondary">Created: {user.createdAt ? new Date(user.createdAt).toLocaleString() : '-'}</Typography><br/>
                <Typography variant="caption" color="text.secondary">Updated: {user.updatedAt ? new Date(user.updatedAt).toLocaleString() : '-'}</Typography>
              </CardContent>
            </Card>
          </Stack>
        </Grid>
      </Grid>

      {/* Password Reset Dialog */}
      <Dialog open={passwordDialog} onClose={() => setPasswordDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Reset Password</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth margin="normal" label="New Password" type="password"
            value={newPassword} onChange={(e) => setNewPassword(e.target.value)}
            helperText="Minimum 8 characters"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handlePasswordReset} disabled={newPassword.length < 8 || passwordMutation.isPending}>
            Reset
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
