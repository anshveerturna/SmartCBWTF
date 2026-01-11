import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Stack,
  TextField,
  Button,
  Avatar,
  Typography,
  CircularProgress,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  Chip,
  IconButton,
  Paper,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  PhotoCamera as PhotoCameraIcon,
  Lock as LockIcon,
  Person as PersonIcon,
  LocalHospital as LocalHospitalIcon,
  History as HistoryIcon,
} from '@mui/icons-material';
import {
  getHcfProfile,
  uploadHcfPhoto,
  removeHcfPhoto,
  changeHcfPassword,
  getHcfActivityLogs,
} from '../../api/hcfProfile';
import type { HcfProfile, ActivityLog } from '../../api/hcfProfile';
import { useAuth } from '../../auth';

const HcfProfilePage: React.FC = () => {
  const { updateUserProfile } = useAuth();
  const [profile, setProfile] = useState<HcfProfile | null>(null);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadProfile();
    loadActivityLogs();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const data = await getHcfProfile();
      setProfile(data);
    } catch {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const loadActivityLogs = async () => {
    try {
      const data = await getHcfActivityLogs(20);
      setActivityLogs(data.logs);
    } catch {
      // Silent fail for logs
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setSaving(true);
      const result = await uploadHcfPhoto(file);
      setProfile(prev => prev ? { ...prev, profilePhotoUrl: result.photoUrl } : prev);
      updateUserProfile({ profile_photo_url: result.photoUrl });
      setSuccess('Photo uploaded successfully!');
      loadActivityLogs();
    } catch {
      setError('Failed to upload photo');
    } finally {
      setSaving(false);
    }
  };

  const handleRemovePhoto = async () => {
    try {
      setSaving(true);
      await removeHcfPhoto();
      setProfile(prev => prev ? { ...prev, profilePhotoUrl: null } : prev);
      updateUserProfile({ profile_photo_url: null });
      setSuccess('Photo removed successfully!');
      loadActivityLogs();
    } catch {
      setError('Failed to remove photo');
    } finally {
      setSaving(false);
    }
  };

  const validatePassword = (password: string) => {
    const rules = [
      { test: (p: string) => p.length >= 8, message: 'At least 8 characters' },
      { test: (p: string) => p.length <= 12, message: 'At most 12 characters' },
      { test: (p: string) => /[A-Z]/.test(p), message: 'One uppercase letter' },
      { test: (p: string) => /[a-z]/.test(p), message: 'One lowercase letter' },
      { test: (p: string) => /[0-9]/.test(p), message: 'One number' },
      { test: (p: string) => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(p), message: 'One special character (!@#$%^&*...)' },
    ];
    return rules.map(rule => ({ ...rule, valid: rule.test(password) }));
  };

  const handlePasswordChange = async () => {
    setPasswordError(null);

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }

    const validations = validatePassword(passwordForm.newPassword);
    const failed = validations.filter(v => !v.valid);
    if (failed.length > 0) {
      setPasswordError('Password requirements not met: ' + failed.map(f => f.message).join(', '));
      return;
    }

    try {
      setSaving(true);
      await changeHcfPassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setSuccess('Password changed successfully');
      setPasswordDialogOpen(false);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      loadActivityLogs();
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      setPasswordError(errObj.message || 'Failed to change password');
    } finally {
      setSaving(false);
    }
  };

  const formatAction = (action: string) => {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!profile) {
    return <Alert severity="error">Failed to load profile</Alert>;
  }

  const getInitials = (name: string) => {
    return name?.split(' ').map(n => n[0]).join('').toUpperCase() || 'HA';
  };

  const getTimeAgo = (date: Date) => {
    const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);
    const intervals = [
      { label: 'year', seconds: 31536000 },
      { label: 'month', seconds: 2592000 },
      { label: 'day', seconds: 86400 },
      { label: 'hour', seconds: 3600 },
      { label: 'minute', seconds: 60 },
    ];
    for (const interval of intervals) {
      const count = Math.floor(seconds / interval.seconds);
      if (count >= 1) {
        return `${count} ${interval.label}${count > 1 ? 's' : ''} ago`;
      }
    }
    return 'Just now';
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
        <PersonIcon fontSize="large" /> My Profile
      </Typography>

      <Paper sx={{ p: 3, borderRadius: 2, mb: 3 }}>
        <Grid container spacing={4}>
          {/* Photo Section */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Box sx={{ textAlign: 'center' }}>
              <Box sx={{ position: 'relative', display: 'inline-block' }}>
                <Avatar
                  src={profile.profilePhotoUrl ? `http://localhost:8080${profile.profilePhotoUrl}` : undefined}
                  sx={{ width: 150, height: 150, fontSize: '3rem', bgcolor: 'primary.main' }}
                  slotProps={{ img: { sx: { objectFit: 'cover' } } }}
                >
                  {getInitials(profile.fullName)}
                </Avatar>
                <IconButton
                  sx={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    bgcolor: 'grey.600',
                    color: 'white',
                    '&:hover': { bgcolor: 'grey.700' },
                  }}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <PhotoCameraIcon />
                </IconButton>
                <input
                  type="file"
                  ref={fileInputRef}
                  hidden
                  accept="image/*"
                  onChange={handlePhotoUpload}
                />
              </Box>
              {profile.profilePhotoUrl && (
                <Button
                  size="small"
                  color="error"
                  variant="text"
                  onClick={handleRemovePhoto}
                  sx={{ mt: 1 }}
                >
                  Remove Photo
                </Button>
              )}

              <Typography variant="h5" sx={{ mt: 2, fontWeight: 600 }}>
                {profile.fullName || profile.username}
              </Typography>
              <Chip
                label={profile.role.replace('_', ' ')}
                color="primary"
                size="small"
                sx={{ mt: 1 }}
              />
              <Chip
                label={profile.active ? 'ACTIVE' : 'DISABLED'}
                color={profile.active ? 'success' : 'error'}
                size="small"
                sx={{ mt: 1, ml: 1 }}
              />
              
              {profile.hcfName && (
                <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                  <LocalHospitalIcon fontSize="small" color="action" />
                  <Typography variant="body2" color="text.secondary">
                    {profile.hcfName}
                  </Typography>
                </Box>
              )}
            </Box>
          </Grid>

          {/* Profile Info */}
          <Grid size={{ xs: 12, md: 8 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
              <PersonIcon sx={{ mr: 1 }} />
              <Typography variant="h6">Profile Information</Typography>
            </Box>

            <Stack spacing={2}>
              <TextField
                fullWidth
                label="Username"
                value={profile.username}
                disabled
                helperText="Username cannot be changed"
              />
              <TextField
                fullWidth
                label="Full Name"
                value={profile.fullName || ''}
                disabled
              />
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Email"
                    value={profile.email || ''}
                    disabled
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Phone"
                    value={profile.phone || ''}
                    disabled
                  />
                </Grid>
              </Grid>
            </Stack>

            <Box sx={{ mt: 3 }}>
              <Button
                variant="contained"
                onClick={() => setPasswordDialogOpen(true)}
                startIcon={<LockIcon />}
              >
                Change Password
              </Button>
            </Box>

            <Divider sx={{ my: 4 }} />

            {/* Read-only Info */}
            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 2 }}>
              Account Information (Read Only)
            </Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Last Login"
                  value={profile.lastLoginAt ? new Date(profile.lastLoginAt).toLocaleString() : 'Never'}
                  disabled
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Created At"
                  value={new Date(profile.createdAt).toLocaleString()}
                  disabled
                />
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      </Paper>

      {/* Activity Logs Section */}
      <Paper sx={{ p: 3, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <HistoryIcon sx={{ mr: 1 }} />
          <Typography variant="h6">Recent Activity</Typography>
          <Chip 
            label={`${activityLogs.length} events`} 
            size="small" 
            sx={{ ml: 'auto', bgcolor: 'action.selected' }} 
          />
        </Box>

        {activityLogs.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <HistoryIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
            <Typography color="text.secondary">
              No activity logs yet
            </Typography>
          </Box>
        ) : (
          <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
            {activityLogs.map((log, index) => {
              const actionColor = log.action.includes('LOGIN') ? 'info' 
                : log.action.includes('PASSWORD') ? 'warning'
                : log.action.includes('PHOTO') ? 'secondary'
                : log.action.includes('PROFILE') ? 'success'
                : 'default';
              
              const timeAgo = getTimeAgo(new Date(log.timestamp));
              
              return (
                <Box 
                  key={log.id}
                  sx={{ 
                    display: 'flex', 
                    gap: 2, 
                    py: 2,
                    px: 1,
                    borderBottom: index < activityLogs.length - 1 ? '1px solid' : 'none',
                    borderColor: 'divider',
                    '&:hover': { bgcolor: 'action.hover', borderRadius: 1 },
                    transition: 'background-color 0.2s'
                  }}
                >
                  {/* Timeline dot */}
                  <Box sx={{ 
                    display: 'flex', 
                    flexDirection: 'column', 
                    alignItems: 'center',
                    pt: 0.5
                  }}>
                    <Box sx={{ 
                      width: 10, 
                      height: 10, 
                      borderRadius: '50%', 
                      bgcolor: `${actionColor}.main`,
                      boxShadow: `0 0 0 4px rgba(var(--mui-palette-${actionColor}-mainChannel) / 0.2)`
                    }} />
                  </Box>
                  
                  {/* Content */}
                  <Box sx={{ flex: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                      <Chip 
                        label={formatAction(log.action)} 
                        size="small" 
                        color={actionColor as 'info' | 'warning' | 'secondary' | 'success' | 'default'}
                        sx={{ 
                          fontWeight: 600,
                          fontSize: '0.75rem'
                        }}
                      />
                      <Typography variant="caption" color="text.secondary">
                        on {log.entityType.replace(/_/g, ' ')}
                      </Typography>
                    </Box>
                    <Typography variant="caption" color="text.disabled">
                      {timeAgo} • {new Date(log.timestamp).toLocaleString()}
                    </Typography>
                  </Box>
                </Box>
              );
            })}
          </Box>
        )}
      </Paper>

      {/* Password Dialog */}
      <Dialog open={passwordDialogOpen} onClose={() => setPasswordDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Change Password</DialogTitle>
        <DialogContent>
          {passwordError && <Alert severity="error" sx={{ mb: 2 }}>{passwordError}</Alert>}
          <TextField
            fullWidth
            type="password"
            label="Current Password"
            value={passwordForm.currentPassword}
            onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
            sx={{ mt: 2 }}
          />
          <TextField
            fullWidth
            type="password"
            label="New Password"
            value={passwordForm.newPassword}
            onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
            sx={{ mt: 2 }}
          />
          
          {/* Password Requirements Checklist */}
          <Box sx={{ mt: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Password Requirements:</Typography>
            {validatePassword(passwordForm.newPassword).map((rule, idx) => (
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
          
          <TextField
            fullWidth
            type="password"
            label="Confirm New Password"
            value={passwordForm.confirmPassword}
            onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
            sx={{ mt: 2 }}
            error={passwordForm.confirmPassword.length > 0 && passwordForm.newPassword !== passwordForm.confirmPassword}
            helperText={passwordForm.confirmPassword.length > 0 && passwordForm.newPassword !== passwordForm.confirmPassword ? 'Passwords do not match' : ''}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialogOpen(false)}>Cancel</Button>
          <Button 
            onClick={handlePasswordChange} 
            variant="contained" 
            disabled={saving || validatePassword(passwordForm.newPassword).some(r => !r.valid) || passwordForm.newPassword !== passwordForm.confirmPassword}
          >
            Change Password
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbars */}
      <Snackbar open={!!success} autoHideDuration={3000} onClose={() => setSuccess(null)}>
        <Alert severity="success">{success}</Alert>
      </Snackbar>
      <Snackbar open={!!error} autoHideDuration={5000} onClose={() => setError(null)}>
        <Alert severity="error">{error}</Alert>
      </Snackbar>
    </Box>
  );
};

export default HcfProfilePage;
