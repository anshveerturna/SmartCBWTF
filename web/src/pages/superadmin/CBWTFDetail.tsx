import { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Divider,
  Switch,
  FormControlLabel,
  Alert,
  Skeleton,
  List,
  ListItem,
  ListItemText,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Block as BlockIcon,
  PlayArrow as PlayArrowIcon,
  AccessTime as AccessTimeIcon,
  Key as KeyIcon,
  LockReset as LockResetIcon,
} from '@mui/icons-material';
import { adminApi, FEATURE_FLAGS } from '../../api/admin';

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  ACTIVE: 'success',
  TRIAL: 'info',
  EXPIRED: 'error',
  SUSPENDED: 'warning',
  CANCELLED: 'default',
};

export default function CBWTFDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const isNewlyCreated = location.state?.newlyCreated;

  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [suspendReason, setSuspendReason] = useState('');
  const [tempAccessDialogOpen, setTempAccessDialogOpen] = useState(false);
  const [tempAccessDays, setTempAccessDays] = useState(7);
  
  // Admin credentials state
  const [credentialsDialogOpen, setCredentialsDialogOpen] = useState(false);
  const [credentialsForm, setCredentialsForm] = useState({ username: '', password: '' });
  const [credentialsSaving, setCredentialsSaving] = useState(false);
  const [credentialsSuccess, setCredentialsSuccess] = useState<string | null>(null);
  const [credentialsError, setCredentialsError] = useState<string | null>(null);
  const [forceResetLoading, setForceResetLoading] = useState(false);
  
  // Edit mode state
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    name: '', address: '', ownerName: '', contactEmail: '', contactPhone: '',
    panNumber: '', gstNumber: '', aadharNumber: '', gpsLat: '', gpsLon: '',
  });

  const { data: cbwtf, isLoading, error } = useQuery({
    queryKey: ['cbwtf', id],
    queryFn: () => adminApi.getCBWTF(id!),
    enabled: !!id,
  });

  const { data: auditHistory } = useQuery({
    queryKey: ['cbwtf-audit', id],
    queryFn: () => adminApi.getAuditHistory(id!, { page: 0, size: 10 }),
    enabled: !!id,
  });

  // Fetch CBWTF admin info
  const { data: adminInfo, refetch: refetchAdmin } = useQuery({
    queryKey: ['cbwtf-admin', id],
    queryFn: () => adminApi.getCBWTFAdmin(id!),
    enabled: !!id,
  });

  // Password validation rules
  const validatePassword = (password: string) => {
    const rules = [
      { test: (p: string) => p.length >= 8, message: 'At least 8 characters' },
      { test: (p: string) => p.length <= 12, message: 'At most 12 characters' },
      { test: (p: string) => /[A-Z]/.test(p), message: 'One uppercase letter' },
      { test: (p: string) => /[a-z]/.test(p), message: 'One lowercase letter' },
      { test: (p: string) => /[0-9]/.test(p), message: 'One number' },
      { test: (p: string) => /[!@#$%^&*(),.?":{}|<>]/.test(p), message: 'One special character' },
    ];
    return rules.map(rule => ({ ...rule, valid: rule.test(password) }));
  };

  const isPasswordValid = (password: string) => validatePassword(password).every(r => r.valid);

  const handleSaveCredentials = async () => {
    if (!credentialsForm.username || !isPasswordValid(credentialsForm.password)) return;
    try {
      setCredentialsSaving(true);
      setCredentialsError(null);
      await adminApi.changeCBWTFCredentials(id!, credentialsForm.username, credentialsForm.password);
      // Wait for the refetch to complete BEFORE closing dialog
      await refetchAdmin();
      setCredentialsSuccess('Credentials updated successfully!');
      setCredentialsDialogOpen(false);
      setCredentialsForm({ username: '', password: '' });
    } catch {
      setCredentialsError('Failed to update credentials');
    } finally {
      setCredentialsSaving(false);
    }
  };

  const suspendMutation = useMutation({
    mutationFn: ({ reason }: { reason: string }) => adminApi.suspendCBWTF(id!, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
      setSuspendDialogOpen(false);
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: () => adminApi.reactivateCBWTF(id!, 365, 'Reactivated by admin'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
    },
  });

  const tempAccessMutation = useMutation({
    mutationFn: ({ days }: { days: number }) => 
      adminApi.grantTemporaryAccess(id!, days, 'Temporary access granted'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
      setTempAccessDialogOpen(false);
    },
  });

  const featureMutation = useMutation({
    mutationFn: ({ feature, enabled }: { feature: string; enabled: boolean }) =>
      adminApi.updateFeatures(id!, { [feature]: enabled }),
    // Optimistic update to prevent UI flashing
    onMutate: async ({ feature, enabled }) => {
      await queryClient.cancelQueries({ queryKey: ['cbwtf', id] });
      const previousData = queryClient.getQueryData(['cbwtf', id]);
      queryClient.setQueryData(['cbwtf', id], (old: typeof cbwtf) => old ? {
        ...old,
        features: { ...old.features, [feature]: enabled }
      } : old);
      return { previousData };
    },
    onError: (_err, _vars, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(['cbwtf', id], context.previousData);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: typeof editForm) => adminApi.updateCBWTF(id!, {
      name: data.name,
      address: data.address,
      ownerName: data.ownerName || undefined,
      contactEmail: data.contactEmail || undefined,
      contactPhone: data.contactPhone || undefined,
      panNumber: data.panNumber || undefined,
      gstNumber: data.gstNumber || undefined,
      aadharNumber: data.aadharNumber || undefined,
      gpsLat: data.gpsLat ? parseFloat(data.gpsLat) : undefined,
      gpsLon: data.gpsLon ? parseFloat(data.gpsLon) : undefined,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
      setIsEditing(false);
    },
  });

  const startEditing = () => {
    if (cbwtf) {
      setEditForm({
        name: cbwtf.name || '',
        address: cbwtf.address || '',
        ownerName: cbwtf.ownerName || '',
        contactEmail: cbwtf.contactEmail || '',
        contactPhone: cbwtf.contactPhone || '',
        panNumber: cbwtf.panNumber || '',
        gstNumber: cbwtf.gstNumber || '',
        aadharNumber: cbwtf.aadharNumber || '',
        gpsLat: cbwtf.gpsLat?.toString() || '',
        gpsLon: cbwtf.gpsLon?.toString() || '',
      });
      setIsEditing(true);
    }
  };

  const cancelEditing = () => setIsEditing(false);
  const saveChanges = () => updateMutation.mutate(editForm);

  if (isLoading) {
    return (
      <Box>
        <Skeleton height={40} width={200} />
        <Skeleton height={300} />
      </Box>
    );
  }

  if (error || !cbwtf) {
    return (
      <Alert severity="error">
        CBWTF not found or failed to load.
        <Button onClick={() => navigate('/superadmin/cbwtfs')}>Back to CBWTFs</Button>
      </Alert>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/superadmin/cbwtfs')}
          sx={{ mb: 2 }}
        >
          Back to CBWTFs
        </Button>
        
        {isNewlyCreated && (
          <Alert severity="success" sx={{ mb: 2 }}>
            CBWTF created successfully! A temporary password has been sent to the admin.
          </Alert>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography variant="h4" fontWeight={700}>
              {cbwtf.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" fontFamily="monospace">
              {cbwtf.code}
            </Typography>
          </Box>
          <Chip
            label={cbwtf.subscriptionStatus}
            color={statusColors[cbwtf.subscriptionStatus]}
            sx={{ fontWeight: 600 }}
          />
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Subscription Card */}
        <Grid item xs={12} md={8}>
          <Card sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Subscription
              </Typography>
              <Stack direction="row" spacing={4} flexWrap="wrap">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Plan
                  </Typography>
                  <Typography fontWeight={600}>{cbwtf.subscriptionPlan}</Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Chip
                    label={cbwtf.subscriptionStatus}
                    color={statusColors[cbwtf.subscriptionStatus]}
                    size="small"
                  />
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Expires
                  </Typography>
                  <Typography>
                    {cbwtf.subscriptionExpiresAt
                      ? new Date(cbwtf.subscriptionExpiresAt).toLocaleDateString()
                      : '-'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Onboarded
                  </Typography>
                  <Typography>
                    {cbwtf.onboardedAt
                      ? new Date(cbwtf.onboardedAt).toLocaleDateString()
                      : '-'}
                  </Typography>
                </Box>
              </Stack>

              <Divider sx={{ my: 3 }} />

              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => navigate(`/superadmin/cbwtfs/${id}/edit`)}
                >
                  Edit Subscription
                </Button>
                {cbwtf.subscriptionStatus === 'ACTIVE' ? (
                  <Button
                    variant="outlined"
                    color="warning"
                    startIcon={<BlockIcon />}
                    onClick={() => setSuspendDialogOpen(true)}
                  >
                    Suspend
                  </Button>
                ) : (
                  <Button
                    variant="outlined"
                    color="success"
                    startIcon={<PlayArrowIcon />}
                    onClick={() => reactivateMutation.mutate()}
                    disabled={reactivateMutation.isPending}
                  >
                    Reactivate
                  </Button>
                )}
                {(cbwtf.subscriptionStatus === 'EXPIRED' || cbwtf.subscriptionStatus === 'SUSPENDED') && (
                  <Button
                    variant="outlined"
                    startIcon={<AccessTimeIcon />}
                    onClick={() => setTempAccessDialogOpen(true)}
                  >
                    Grant Temp Access
                  </Button>
                )}
              </Box>
            </CardContent>
          </Card>

          {/* Feature Flags */}
          <Card sx={{ borderRadius: 2, mt: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Feature Flags
              </Typography>
              <Grid container spacing={2}>
                {Object.entries(FEATURE_FLAGS).map(([key, label]) => (
                  <Grid item xs={12} sm={6} key={key}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={cbwtf.features[key] ?? false}
                          onChange={(e) =>
                            featureMutation.mutate({ feature: key, enabled: e.target.checked })
                          }
                          disabled={featureMutation.isPending}
                        />
                      }
                      label={label.replace(/_/g, ' ')}
                    />
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>

          {/* Business Details - Editable */}
          <Card sx={{ borderRadius: 2, mt: 3 }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Business Details</Typography>
                {!isEditing ? (
                  <Button variant="outlined" size="small" onClick={startEditing}>Edit</Button>
                ) : (
                  <Stack direction="row" spacing={1}>
                    <Button variant="outlined" size="small" onClick={cancelEditing}>Cancel</Button>
                    <Button variant="contained" size="small" onClick={saveChanges} disabled={updateMutation.isPending}>Save</Button>
                  </Stack>
                )}
              </Box>
              {isEditing ? (
                <Stack spacing={2}>
                  <TextField label="Name" fullWidth size="small" value={editForm.name} 
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} />
                  <TextField label="Address" fullWidth size="small" multiline rows={2} value={editForm.address}
                    onChange={(e) => setEditForm({ ...editForm, address: e.target.value })} />
                  <TextField label="Owner Name" fullWidth size="small" value={editForm.ownerName}
                    onChange={(e) => setEditForm({ ...editForm, ownerName: e.target.value })} />
                  <Stack direction="row" spacing={2}>
                    <TextField label="Email" fullWidth size="small" value={editForm.contactEmail}
                      onChange={(e) => setEditForm({ ...editForm, contactEmail: e.target.value })} />
                    <TextField label="Phone" fullWidth size="small" value={editForm.contactPhone}
                      onChange={(e) => setEditForm({ ...editForm, contactPhone: e.target.value })} />
                  </Stack>
                  <Divider />
                  <Stack direction="row" spacing={2}>
                    <TextField label="PAN Number" fullWidth size="small" value={editForm.panNumber}
                      onChange={(e) => setEditForm({ ...editForm, panNumber: e.target.value.toUpperCase() })} />
                    <TextField label="GST Number" fullWidth size="small" value={editForm.gstNumber}
                      onChange={(e) => setEditForm({ ...editForm, gstNumber: e.target.value.toUpperCase() })} />
                  </Stack>
                  <TextField label="Aadhar Number" fullWidth size="small" value={editForm.aadharNumber}
                    onChange={(e) => setEditForm({ ...editForm, aadharNumber: e.target.value })} />
                  <Stack direction="row" spacing={2}>
                    <TextField label="GPS Latitude" fullWidth size="small" type="number" value={editForm.gpsLat}
                      onChange={(e) => setEditForm({ ...editForm, gpsLat: e.target.value })} />
                    <TextField label="GPS Longitude" fullWidth size="small" type="number" value={editForm.gpsLon}
                      onChange={(e) => setEditForm({ ...editForm, gpsLon: e.target.value })} />
                  </Stack>
                </Stack>
              ) : (
                <Stack spacing={1}>
                  <Box><Typography variant="body2" color="text.secondary">Name</Typography><Typography>{cbwtf.name}</Typography></Box>
                  <Box><Typography variant="body2" color="text.secondary">Address</Typography><Typography>{cbwtf.address}</Typography></Box>
                  <Box><Typography variant="body2" color="text.secondary">Owner</Typography><Typography>{cbwtf.ownerName || '-'}</Typography></Box>
                  <Divider sx={{ my: 1 }} />
                  <Stack direction="row" spacing={4}>
                    <Box><Typography variant="body2" color="text.secondary">PAN</Typography><Typography fontFamily="monospace">{cbwtf.panNumber || '-'}</Typography></Box>
                    <Box><Typography variant="body2" color="text.secondary">GST</Typography><Typography fontFamily="monospace">{cbwtf.gstNumber || '-'}</Typography></Box>
                    <Box><Typography variant="body2" color="text.secondary">Aadhar</Typography><Typography fontFamily="monospace">{cbwtf.aadharNumber || '-'}</Typography></Box>
                  </Stack>
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Right Column */}
        <Grid item xs={12} md={4}>
          {/* Stats */}
          <Card sx={{ borderRadius: 2, mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Statistics
              </Typography>
              <Stack direction="row" spacing={4}>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {cbwtf.hcfCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    HCFs
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {cbwtf.activeUserCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Users
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          {/* Contact Info */}
          <Card sx={{ borderRadius: 2, mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Contact
              </Typography>
              <Stack spacing={1}>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Email
                  </Typography>
                  <Typography>{cbwtf.contactEmail || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Phone
                  </Typography>
                  <Typography>{cbwtf.contactPhone || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Address
                  </Typography>
                  <Typography>{cbwtf.address}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          {/* Admin Credentials */}
          <Card sx={{ borderRadius: 2, mb: 3 }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  <KeyIcon sx={{ mr: 1, verticalAlign: 'middle', fontSize: 20 }} />
                  Admin Credentials
                </Typography>
                <Button 
                  variant="outlined" 
                  size="small"
                  onClick={() => {
                    setCredentialsForm({ 
                      username: adminInfo?.username || '', 
                      password: '' 
                    });
                    setCredentialsDialogOpen(true);
                  }}
                >
                  Update Credentials
                </Button>
              </Box>
              {adminInfo?.hasAdmin ? (
                <Stack spacing={1}>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Username
                    </Typography>
                    <Typography fontFamily="monospace" fontWeight={600}>
                      {adminInfo.username}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Password
                    </Typography>
                    <Typography variant="body2" color="text.secondary" fontStyle="italic">
                      ••••••• (hashed, cannot be displayed)
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Status
                    </Typography>
                    <Chip 
                      label={adminInfo.active ? 'Active' : 'Disabled'} 
                      color={adminInfo.active ? 'success' : 'error'} 
                      size="small" 
                    />
                  </Box>
                  {adminInfo.lastLoginAt && (
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Last Login
                      </Typography>
                      <Typography variant="body2">
                        {new Date(adminInfo.lastLoginAt).toLocaleString()}
                      </Typography>
                    </Box>
                  )}
                  <Divider sx={{ my: 1 }} />
                  <Button
                    variant="outlined"
                    color="warning"
                    size="small"
                    startIcon={<LockResetIcon />}
                    disabled={forceResetLoading}
                    onClick={async () => {
                      if (!confirm('This will require the CBWTF admin to change their password on next login. Continue?')) return;
                      try {
                        setForceResetLoading(true);
                        await adminApi.forceCBWTFPasswordReset(id!);
                        await refetchAdmin();
                        setCredentialsSuccess('Password reset will be required on next login');
                      } catch {
                        setCredentialsError('Failed to force password reset');
                      } finally {
                        setForceResetLoading(false);
                      }
                    }}
                  >
                    {forceResetLoading ? 'Processing...' : 'Force Password Reset'}
                  </Button>
                </Stack>
              ) : (
                <Alert severity="warning">No admin user found for this CBWTF</Alert>
              )}
              {credentialsSuccess && (
                <Alert severity="success" sx={{ mt: 2 }} onClose={() => setCredentialsSuccess(null)}>
                  {credentialsSuccess}
                </Alert>
              )}
            </CardContent>
          </Card>

          {/* Audit History */}
          <Card sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Activity
              </Typography>
              <List dense>
                {auditHistory?.content.slice(0, 5).map((audit) => (
                  <ListItem key={audit.id} disablePadding>
                    <ListItemText
                      primary={audit.action.replace(/_/g, ' ')}
                      secondary={new Date(audit.performedAt).toLocaleString()}
                      primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                      secondaryTypographyProps={{ variant: 'caption' }}
                    />
                  </ListItem>
                ))}
                {(!auditHistory || auditHistory.content.length === 0) && (
                  <Typography variant="body2" color="text.secondary">
                    No activity yet
                  </Typography>
                )}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Suspend Dialog */}
      <Dialog open={suspendDialogOpen} onClose={() => setSuspendDialogOpen(false)}>
        <DialogTitle>Suspend CBWTF</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            This will prevent all users from accessing this CBWTF's data.
          </Typography>
          <TextField
            label="Reason"
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            fullWidth
            multiline
            rows={2}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSuspendDialogOpen(false)}>Cancel</Button>
          <Button
            color="warning"
            variant="contained"
            onClick={() => suspendMutation.mutate({ reason: suspendReason })}
            disabled={suspendMutation.isPending}
          >
            Suspend
          </Button>
        </DialogActions>
      </Dialog>

      {/* Temp Access Dialog */}
      <Dialog open={tempAccessDialogOpen} onClose={() => setTempAccessDialogOpen(false)}>
        <DialogTitle>Grant Temporary Access</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Grant time-limited access to an expired or suspended CBWTF.
          </Typography>
          <TextField
            label="Days"
            type="number"
            value={tempAccessDays}
            onChange={(e) => setTempAccessDays(parseInt(e.target.value) || 7)}
            fullWidth
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTempAccessDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => tempAccessMutation.mutate({ days: tempAccessDays })}
            disabled={tempAccessMutation.isPending}
          >
            Grant Access
          </Button>
        </DialogActions>
      </Dialog>

      {/* Credentials Dialog */}
      <Dialog open={credentialsDialogOpen} onClose={() => setCredentialsDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Update Admin Credentials</DialogTitle>
        <DialogContent>

          <TextField
            fullWidth
            label="Username"
            value={credentialsForm.username}
            onChange={(e) => setCredentialsForm({ ...credentialsForm, username: e.target.value })}
            sx={{ mt: 1, mb: 2 }}
          />
          <TextField
            fullWidth
            type="password"
            label="New Password"
            value={credentialsForm.password}
            onChange={(e) => setCredentialsForm({ ...credentialsForm, password: e.target.value })}
            sx={{ mb: 2 }}
          />
          
          {/* Password Requirements Checklist */}
          <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Password Requirements:</Typography>
            {validatePassword(credentialsForm.password).map((rule, idx) => (
              <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    bgcolor: rule.valid ? 'success.main' : 'error.main',
                  }}
                />
                <Typography
                  variant="caption"
                  sx={{ color: rule.valid ? 'success.main' : 'text.secondary' }}
                >
                  {rule.message}
                </Typography>
              </Box>
            ))}
          </Box>
          
          {credentialsError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {credentialsError}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCredentialsDialogOpen(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={handleSaveCredentials}
            disabled={credentialsSaving || !credentialsForm.username || !isPasswordValid(credentialsForm.password)}
          >
            Save Credentials
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
