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
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  PhotoCamera as PhotoCameraIcon,
  Edit as EditIcon,
  Lock as LockIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import {
  getMyProfile,
  updateMyProfile,
  uploadMyPhoto,
  removeMyPhoto,
  changePassword,
} from '../../../api/superadminProfile';
import type { SuperAdminProfile } from '../../../api/superadminProfile';
import { useAuth } from '../../../auth';

const MyProfile: React.FC = () => {
  const { updateUserProfile } = useAuth();
  const [profile, setProfile] = useState<SuperAdminProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const data = await getMyProfile();
      setProfile(data);
      setFormData({
        fullName: data.fullName || '',
        email: data.email || '',
        phone: data.phone || '',
      });
    } catch {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const updated = await updateMyProfile(formData);
      setProfile(updated);
      setSuccess('Profile updated successfully');
    } catch {
      setError('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setSaving(true);
      const result = await uploadMyPhoto(file);
      setProfile(prev => prev ? { ...prev, profilePhotoUrl: result.photoUrl } : prev);
      // Update header avatar immediately
      updateUserProfile({ profile_photo_url: result.photoUrl });
      setSuccess('Photo uploaded successfully!');
    } catch {
      setError('Failed to upload photo');
    } finally {
      setSaving(false);
    }
  };

  const handleRemovePhoto = async () => {
    try {
      setSaving(true);
      await removeMyPhoto();
      setProfile(prev => prev ? { ...prev, profilePhotoUrl: null } : prev);
      // Update header avatar immediately
      updateUserProfile({ profile_photo_url: null });
      setSuccess('Photo removed successfully!');
    } catch {
      setError('Failed to remove photo');
    } finally {
      setSaving(false);
    }
  };

  // Password validation rules
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

    // Validate passwords match
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }

    // Validate password requirements
    const validations = validatePassword(passwordForm.newPassword);
    const failed = validations.filter(v => !v.valid);
    if (failed.length > 0) {
      setPasswordError('Password requirements not met: ' + failed.map(f => f.message).join(', '));
      return;
    }

    try {
      setSaving(true);
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setSuccess('Password changed successfully');
      setPasswordDialogOpen(false);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      setPasswordError(errObj.message || 'Failed to change password');
    } finally {
      setSaving(false);
    }
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
    return name?.split(' ').map(n => n[0]).join('').toUpperCase() || 'SA';
  };

  return (
    <Box>
      <Grid container spacing={4}>
        {/* Photo Section */}
        <Grid item xs={12} md={4}>
          <Box sx={{ textAlign: 'center' }}>
            <Box sx={{ position: 'relative', display: 'inline-block' }}>
              <Avatar
                src={profile.profilePhotoUrl ? `http://localhost:8080${profile.profilePhotoUrl}` : undefined}
                sx={{ width: 150, height: 150, fontSize: '3rem', bgcolor: 'primary.main' }}
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
          </Box>
        </Grid>

        {/* Edit Form */}
        <Grid item xs={12} md={8}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
            <EditIcon sx={{ mr: 1 }} />
            <Typography variant="h6">Edit Profile</Typography>
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
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            />
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Phone"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </Grid>
            </Grid>
          </Stack>

          <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={saving}
              startIcon={saving ? <CircularProgress size={20} /> : <CheckCircleIcon />}
            >
              Save Changes
            </Button>
            <Button
              variant="outlined"
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
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Last Login"
                value={profile.lastLoginAt ? new Date(profile.lastLoginAt).toLocaleString() : 'Never'}
                disabled
              />
            </Grid>
            <Grid item xs={12} sm={6}>
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

export default MyProfile;
