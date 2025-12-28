import { useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Grid,
  Divider,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Tooltip,
  Stack,
  Paper,
  TextField,
  MenuItem,
  Avatar,
  Snackbar,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Person as PersonIcon,
  LocalShipping as DriverIcon,
  Engineering as PlantIcon,
  GpsFixed as OnlineIcon,
  GpsOff as OfflineIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
  LockReset as ResetIcon,
  Block as DisableIcon,
  CheckCircle as EnableIcon,
  ContentCopy as CopyIcon,
  AccessTime as TimeIcon,
  LocationOn as LocationIcon,
  EventNote as AttendanceIcon,
  Edit as EditIcon,
  VpnKey as KeyIcon,
  Refresh as RefreshIcon,
  PhotoCamera as PhotoCameraIcon,
  Save as SaveIcon,
} from '@mui/icons-material';
import {
  getStaffDetail,
  disableStaff,
  enableStaff,
  unlockStaff,
  updateStaff,
  updateStaffCredentials,
  requestGpsRefresh,
  uploadStaffPhoto,
  removeStaffPhoto,
  type StaffDetailDTO,
  type UpdateStaffRequest,
  type UpdateCredentialsRequest,
} from '../../api/cbwtf';

const roleLabels: Record<string, string> = {
  DRIVER: 'Driver',
  PLANT_OPERATOR: 'Plant Operator',
};

export default function StaffDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [passwordDialog, setPasswordDialog] = useState<{ open: boolean; password: string | null }>({
    open: false,
    password: null,
  });
  const [confirmDialog, setConfirmDialog] = useState<{ open: boolean; action: 'disable' | 'enable' | null }>({
    open: false,
    action: null,
  });
  // Inline edit form state
  const [editForm, setEditForm] = useState<UpdateStaffRequest>({
    fullName: '',
    email: '',
    phone: '',
    gender: '',
    dob: '',
  });
  const [isEditing, setIsEditing] = useState(false);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });
  const [credentialsDialog, setCredentialsDialog] = useState(false);
  const [credentialsForm, setCredentialsForm] = useState<UpdateCredentialsRequest>({
    username: '',
    password: '',
    forcePasswordChange: false,
  });

  // Fetch staff detail
  const { data: staff, isLoading, error } = useQuery<StaffDetailDTO>({
    queryKey: ['staff-detail', id],
    queryFn: () => getStaffDetail(id!),
    enabled: !!id,
  });

  // Mutations
  const disableMutation = useMutation({
    mutationFn: () => disableStaff(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
      setConfirmDialog({ open: false, action: null });
    },
  });

  const enableMutation = useMutation({
    mutationFn: () => enableStaff(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
      setConfirmDialog({ open: false, action: null });
    },
  });

  const unlockMutation = useMutation({
    mutationFn: () => unlockStaff(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
    },
  });

  const gpsRefreshMutation = useMutation({
    mutationFn: () => requestGpsRefresh(id!),
    onSuccess: () => {
      // Invalidate after short delay to give Android app time to respond
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      }, 3000);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateStaffRequest) => updateStaff(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
      setIsEditing(false);
      setSnackbar({ open: true, message: 'Profile updated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to update profile', severity: 'error' });
    },
  });

  const photoUploadMutation = useMutation({
    mutationFn: (file: File) => uploadStaffPhoto(id!, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      setSnackbar({ open: true, message: 'Photo uploaded successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to upload photo', severity: 'error' });
    },
  });

  const photoRemoveMutation = useMutation({
    mutationFn: () => removeStaffPhoto(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      setSnackbar({ open: true, message: 'Photo removed successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to remove photo', severity: 'error' });
    },
  });

  const startEditing = () => {
    if (staff) {
      setEditForm({
        fullName: staff.fullName,
        email: staff.email || '',
        phone: staff.phone || '',
        gender: staff.gender || '',
        dob: staff.dob || '',
      });
      setIsEditing(true);
    }
  };

  const cancelEditing = () => {
    setIsEditing(false);
  };

  const handleSave = () => {
    updateMutation.mutate(editForm);
  };

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      photoUploadMutation.mutate(file);
    }
  };

  const getInitials = (name: string) => {
    return name?.split(' ').map(n => n[0]).join('').toUpperCase() || 'ST';
  };

  const credentialsMutation = useMutation({
    mutationFn: (data: UpdateCredentialsRequest) => updateStaffCredentials(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
      setCredentialsDialog(false);
      setCredentialsForm({ username: '', password: '', forcePasswordChange: false });
    },
  });

  const openCredentialsDialog = () => {
    if (staff) {
      setCredentialsForm({
        username: staff.username,
        password: '',
        forcePasswordChange: false,
      });
      setCredentialsDialog(true);
    }
  };

  const handleCredentialsSubmit = () => {
    credentialsMutation.mutate(credentialsForm);
  };

  const formatDateTime = (dateValue: string | number | null | undefined): string => {
    if (!dateValue) return 'Never';
    try {
      // Handle epoch timestamps (numbers) and ISO strings
      const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue);
      if (isNaN(date.getTime())) return 'Unknown';
      return date.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'Unknown';
    }
  };

  const formatTimeAgo = (dateValue: string | number | null | undefined): string => {
    if (!dateValue) return 'Never';
    try {
      const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue);
      if (isNaN(date.getTime())) return '';
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      if (diffMs < 0) return 'Just now';
      const diffMins = Math.floor(diffMs / 60000);
      
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins} min ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours} hr ago`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays} days ago`;
      return `${Math.floor(diffDays / 7)} weeks ago`;
    } catch {
      return '';
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !staff) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          Failed to load staff details. The staff member may not exist or you don't have access.
        </Alert>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/cbwtf/staff')} sx={{ mt: 2 }}>
          Back to Staff List
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <IconButton onClick={() => navigate('/cbwtf/staff')}>
          <BackIcon />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4" sx={{ fontWeight: 600 }}>
            {staff.fullName}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
            {staff.username}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          {staff.active ? (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DisableIcon />}
              onClick={() => setConfirmDialog({ open: true, action: 'disable' })}
            >
              Disable
            </Button>
          ) : (
            <Button
              variant="outlined"
              color="success"
              startIcon={<EnableIcon />}
              onClick={() => setConfirmDialog({ open: true, action: 'enable' })}
            >
              Enable
            </Button>
          )}
        </Stack>
      </Box>

      <Grid container spacing={3}>
        {/* Profile Card - New Design with Avatar and Inline Editing */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ borderRadius: 2, height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PersonIcon /> Profile
                </Typography>
                {!isEditing ? (
                  <Button size="small" startIcon={<EditIcon />} onClick={startEditing}>
                    Edit
                  </Button>
                ) : (
                  <Stack direction="row" spacing={1}>
                    <Button size="small" onClick={cancelEditing}>Cancel</Button>
                    <Button 
                      size="small" 
                      variant="contained" 
                      startIcon={updateMutation.isPending ? <CircularProgress size={16} /> : <SaveIcon />}
                      onClick={handleSave}
                      disabled={updateMutation.isPending}
                    >
                      Save
                    </Button>
                  </Stack>
                )}
              </Box>
              <Divider sx={{ mb: 3 }} />
              
              {/* Avatar Section */}
              <Box sx={{ textAlign: 'center', mb: 3 }}>
                <Box sx={{ position: 'relative', display: 'inline-block' }}>
                  <Avatar
                    src={staff.profilePhotoUrl ? `http://localhost:8080${staff.profilePhotoUrl}` : undefined}
                    sx={{ width: 100, height: 100, fontSize: '2.5rem', bgcolor: 'primary.main', mx: 'auto' }}
                    slotProps={{ img: { sx: { objectFit: 'cover' } } }}
                  >
                    {getInitials(staff.fullName)}
                  </Avatar>
                  <IconButton
                    sx={{
                      position: 'absolute',
                      bottom: 0,
                      right: 0,
                      bgcolor: 'grey.600',
                      color: 'white',
                      width: 32,
                      height: 32,
                      '&:hover': { bgcolor: 'grey.700' },
                    }}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={photoUploadMutation.isPending}
                  >
                    {photoUploadMutation.isPending ? <CircularProgress size={16} color="inherit" /> : <PhotoCameraIcon fontSize="small" />}
                  </IconButton>
                  <input
                    type="file"
                    ref={fileInputRef}
                    hidden
                    accept="image/*"
                    onChange={handlePhotoUpload}
                  />
                </Box>
                {staff.profilePhotoUrl && (
                  <Button
                    size="small"
                    color="error"
                    variant="text"
                    onClick={() => photoRemoveMutation.mutate()}
                    disabled={photoRemoveMutation.isPending}
                    sx={{ mt: 1 }}
                  >
                    Remove Photo
                  </Button>
                )}
                <Typography variant="h6" sx={{ mt: 1, fontWeight: 600 }}>
                  {staff.fullName}
                </Typography>
                <Stack direction="row" spacing={1} justifyContent="center" sx={{ mt: 1 }}>
                  <Chip
                    icon={staff.role === 'DRIVER' ? <DriverIcon /> : <PlantIcon />}
                    label={roleLabels[staff.role]}
                    size="small"
                    color={staff.role === 'DRIVER' ? 'primary' : 'secondary'}
                  />
                  <Chip
                    label={staff.active ? 'Active' : 'Disabled'}
                    size="small"
                    color={staff.active ? 'success' : 'default'}
                  />
                </Stack>
              </Box>
              
              <Divider sx={{ my: 2 }} />
              
              {/* Editable Fields */}
              <Stack spacing={2}>
                {isEditing ? (
                  <>
                    <TextField
                      label="Full Name"
                      value={editForm.fullName}
                      onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                      size="small"
                      fullWidth
                      required
                    />
                    <TextField
                      label="Email"
                      value={editForm.email}
                      onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                      size="small"
                      fullWidth
                    />
                    <TextField
                      label="Phone"
                      value={editForm.phone}
                      onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                      size="small"
                      fullWidth
                    />
                    <TextField
                      label="Gender"
                      select
                      value={editForm.gender || ''}
                      onChange={(e) => setEditForm({ ...editForm, gender: e.target.value })}
                      size="small"
                      fullWidth
                    >
                      <MenuItem value="">Not specified</MenuItem>
                      <MenuItem value="MALE">Male</MenuItem>
                      <MenuItem value="FEMALE">Female</MenuItem>
                      <MenuItem value="OTHER">Other</MenuItem>
                    </TextField>
                    <TextField
                      label="Date of Birth"
                      type="date"
                      value={editForm.dob || ''}
                      onChange={(e) => setEditForm({ ...editForm, dob: e.target.value })}
                      size="small"
                      fullWidth
                      slotProps={{ inputLabel: { shrink: true } }}
                    />
                  </>
                ) : (
                  <>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <EmailIcon fontSize="small" color="action" />
                      <Typography variant="body2">{staff.email || 'Not provided'}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <PhoneIcon fontSize="small" color="action" />
                      <Typography variant="body2">{staff.phone || 'Not provided'}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Gender</Typography>
                      <Typography variant="body2">{staff.gender || 'Not provided'}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Date of Birth</Typography>
                      <Typography variant="body2">{staff.dob || 'Not provided'}</Typography>
                    </Box>
                  </>
                )}
                
                <Divider />
                
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Created</Typography>
                  <Typography variant="body2">{formatDateTime(staff.createdAt)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Last Login</Typography>
                  <Typography variant="body2">{formatTimeAgo(staff.lastLoginAt)}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* GPS Status Card */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ borderRadius: 2, height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <LocationIcon /> GPS Status
                </Typography>
                <Tooltip title="Request location update from Android app">
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={gpsRefreshMutation.isPending ? <CircularProgress size={16} /> : <RefreshIcon />}
                    onClick={() => gpsRefreshMutation.mutate()}
                    disabled={gpsRefreshMutation.isPending}
                  >
                    {gpsRefreshMutation.isPending ? 'Requesting...' : 'Refresh'}
                  </Button>
                </Tooltip>
              </Box>
              <Divider sx={{ my: 2 }} />

              <Box sx={{ textAlign: 'center', py: 1 }}>
                {staff.gpsStatus === 'ONLINE' ? (
                  <>
                    <OnlineIcon sx={{ fontSize: 40, color: 'success.main', mb: 0.5 }} />
                    <Typography variant="subtitle1" color="success.main">Online</Typography>
                  </>
                ) : staff.gpsStatus === 'OFFLINE' ? (
                  <>
                    <OfflineIcon sx={{ fontSize: 40, color: 'text.disabled', mb: 0.5 }} />
                    <Typography variant="subtitle1" color="text.secondary">Offline</Typography>
                  </>
                ) : (
                  <>
                    <OfflineIcon sx={{ fontSize: 40, color: 'text.disabled', mb: 0.5 }} />
                    <Typography variant="subtitle1" color="text.secondary">Never Connected</Typography>
                  </>
                )}
              </Box>

              <Stack spacing={2} sx={{ mt: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Last GPS Update</Typography>
                  <Typography variant="body2">{formatTimeAgo(staff.lastGpsAt)}</Typography>
                </Box>

                {/* Mini Map */}
                {staff.lastGpsLat && staff.lastGpsLon && (
                  <Paper 
                    variant="outlined" 
                    sx={{ 
                      overflow: 'hidden', 
                      borderRadius: 2,
                      bgcolor: 'grey.100'
                    }}
                  >
                    <Box
                      component="iframe"
                      src={`https://www.openstreetmap.org/export/embed.html?bbox=${staff.lastGpsLon - 0.01}%2C${staff.lastGpsLat - 0.01}%2C${staff.lastGpsLon + 0.01}%2C${staff.lastGpsLat + 0.01}&layer=mapnik&marker=${staff.lastGpsLat}%2C${staff.lastGpsLon}`}
                      sx={{
                        border: 0,
                        width: '100%',
                        height: 180,
                        display: 'block',
                      }}
                      loading="lazy"
                      title="Staff Location Map"
                    />
                    <Box sx={{ p: 1, bgcolor: 'background.paper' }}>
                      <Typography variant="caption" color="text.secondary">
                        Last Known Position
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {staff.lastGpsLat.toFixed(6)}, {staff.lastGpsLon.toFixed(6)}
                      </Typography>
                    </Box>
                  </Paper>
                )}

                {!staff.lastGpsLat && !staff.lastGpsLon && (
                  <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', bgcolor: 'grey.50' }}>
                    <LocationIcon sx={{ fontSize: 32, color: 'grey.400', mb: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      No location data available
                    </Typography>
                  </Paper>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Last Attendance Card */}
        <Grid size={{ xs: 12 }}>
          <Card sx={{ borderRadius: 2, background: 'linear-gradient(135deg, rgba(76, 175, 80, 0.05) 0%, transparent 100%)' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'success.main' }}>
                <AttendanceIcon /> Last Attendance
              </Typography>
              <Divider sx={{ my: 2 }} />

              {staff.lastAttendanceAt ? (
                <Box sx={{ 
                  display: 'flex', 
                  flexWrap: 'wrap',
                  alignItems: 'center', 
                  gap: 4,
                  p: 2,
                  borderRadius: 2,
                  bgcolor: 'background.paper',
                  border: '1px solid',
                  borderColor: 'divider'
                }}>
                  <Box sx={{ minWidth: 200 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      Healthcare Facility
                    </Typography>
                    <Typography variant="body1" fontWeight={600} sx={{ mt: 0.5 }}>
                      {staff.lastAttendanceHcf || 'Unknown'}
                    </Typography>
                  </Box>
                  <Box sx={{ minWidth: 180 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      Date & Time
                    </Typography>
                    <Typography variant="body1" fontWeight={500} sx={{ mt: 0.5 }}>
                      {formatDateTime(staff.lastAttendanceAt)}
                    </Typography>
                  </Box>
                  <Chip
                    icon={<TimeIcon />}
                    label={formatTimeAgo(staff.lastAttendanceAt)}
                    size="medium"
                    color="success"
                    variant="outlined"
                    sx={{ fontWeight: 500, px: 1 }}
                  />
                </Box>
              ) : (
                <Box sx={{ 
                  p: 3, 
                  textAlign: 'center', 
                  borderRadius: 2, 
                  bgcolor: 'action.hover' 
                }}>
                  <Typography color="text.secondary">
                    No attendance records yet for this staff member.
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* App Credentials Card */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ borderRadius: 2, height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <KeyIcon /> App Credentials
                </Typography>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={openCredentialsDialog}
                >
                  Update Credentials
                </Button>
              </Box>
              <Divider sx={{ my: 2 }} />

              <Stack spacing={2}>
                <Box>
                  <Typography variant="body2" color="text.secondary">Username</Typography>
                  <Typography variant="body1" fontWeight={600} sx={{ fontFamily: 'monospace' }}>
                    {staff.username}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary">Password</Typography>
                  <Typography variant="body1" color="text.secondary" fontStyle="italic">
                    •••••••• (hashed, cannot be displayed)
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary">Status</Typography>
                  <Chip
                    label={staff.active ? 'Active' : 'Disabled'}
                    size="small"
                    color={staff.active ? 'success' : 'default'}
                    sx={{ mt: 0.5 }}
                  />
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary">Last Login</Typography>
                  <Typography variant="body1">
                    {staff.lastLoginAt ? formatDateTime(staff.lastLoginAt) : 'Never'}
                  </Typography>
                </Box>
              </Stack>

              <Button
                variant="outlined"
                color="warning"
                fullWidth
                startIcon={<ResetIcon />}
                onClick={() => unlockMutation.mutate()}
                disabled={unlockMutation.isPending}
                sx={{ mt: 3 }}
              >
                {unlockMutation.isPending ? 'Unlocking...' : 'Unlock Account'}
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Confirm Dialog */}
      <Dialog open={confirmDialog.open} onClose={() => setConfirmDialog({ open: false, action: null })}>
        <DialogTitle>
          {confirmDialog.action === 'disable' ? 'Disable Staff Account' : 'Enable Staff Account'}
        </DialogTitle>
        <DialogContent>
          <Typography>
            {confirmDialog.action === 'disable' 
              ? `Are you sure you want to disable ${staff.fullName}? They will no longer be able to log in.`
              : `Are you sure you want to re-enable ${staff.fullName}?`
            }
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog({ open: false, action: null })}>Cancel</Button>
          <Button
            variant="contained"
            color={confirmDialog.action === 'disable' ? 'error' : 'success'}
            onClick={() => confirmDialog.action === 'disable' ? disableMutation.mutate() : enableMutation.mutate()}
            disabled={disableMutation.isPending || enableMutation.isPending}
          >
            {confirmDialog.action === 'disable' ? 'Disable' : 'Enable'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Password Dialog */}
      <PasswordRevealDialog
        open={passwordDialog.open}
        password={passwordDialog.password}
        onClose={() => setPasswordDialog({ open: false, password: null })}
      />

      {/* Success/Error Snackbar */}
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={3000} 
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>

      {/* Edit Credentials Dialog */}
      <Dialog open={credentialsDialog} onClose={() => setCredentialsDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Login Credentials</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Changing credentials will immediately affect the staff member's ability to login to the Android app.
          </Alert>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <TextField
              label="Username"
              value={credentialsForm.username}
              onChange={(e) => setCredentialsForm({ ...credentialsForm, username: e.target.value })}
              fullWidth
              helperText="Must be unique across the system"
            />
            <TextField
              label="New Password"
              type="password"
              value={credentialsForm.password}
              onChange={(e) => setCredentialsForm({ ...credentialsForm, password: e.target.value })}
              fullWidth
              helperText="Leave blank to keep current password"
            />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <input
                type="checkbox"
                id="forcePasswordChange"
                checked={credentialsForm.forcePasswordChange || false}
                onChange={(e) => setCredentialsForm({ ...credentialsForm, forcePasswordChange: e.target.checked })}
              />
              <label htmlFor="forcePasswordChange">
                Force password change on next login
              </label>
            </Box>
          </Stack>
          {credentialsMutation.isError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              Failed to update credentials. The username may already exist.
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCredentialsDialog(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCredentialsSubmit}
            disabled={credentialsMutation.isPending || !credentialsForm.username?.trim()}
          >
            {credentialsMutation.isPending ? 'Saving...' : 'Update Credentials'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

// Password Reveal Dialog
function PasswordRevealDialog({
  open,
  password,
  onClose,
}: {
  open: boolean;
  password: string | null;
  onClose: () => void;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (password) {
      navigator.clipboard.writeText(password);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Password Reset</DialogTitle>
      <DialogContent>
        <Alert severity="success" sx={{ mb: 2 }}>
          Password has been reset successfully.
        </Alert>
        <Alert severity="warning" icon={false}>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>New Temporary Password</strong> (shown only once):
          </Typography>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            bgcolor: 'grey.100',
            p: 1.5,
            borderRadius: 1,
          }}>
            <Typography variant="h6" sx={{ fontFamily: 'monospace', flex: 1 }}>
              {password}
            </Typography>
            <Tooltip title={copied ? 'Copied!' : 'Copy'}>
              <IconButton onClick={handleCopy} size="small">
                <CopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            The staff member will be required to change this password on next login.
          </Typography>
        </Alert>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">Done</Button>
      </DialogActions>
    </Dialog>
  );
}
