import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  InputAdornment,
  IconButton,
  CircularProgress,
  alpha,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Lock as LockIcon,
  CheckCircle,
  Cancel,
} from '@mui/icons-material';
import { useAuth } from '../auth';
import apiClient from '../api/client';

// Password requirements
const PASSWORD_REQUIREMENTS = [
  { id: 'length', label: 'At least 8 characters', regex: /.{8,}/ },
  { id: 'uppercase', label: 'One uppercase letter', regex: /[A-Z]/ },
  { id: 'lowercase', label: 'One lowercase letter', regex: /[a-z]/ },
  { id: 'number', label: 'One number', regex: /\d/ },
  { id: 'special', label: 'One special character (@$!%*?&)', regex: /[@$!%*?&]/ },
];

// Form validation schema
const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/\d/, 'Password must contain at least one number')
    .regex(/[@$!%*?&]/, 'Password must contain at least one special character'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

const ChangePassword: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const newPassword = watch('newPassword');

  const onSubmit = async (data: ChangePasswordFormData) => {
    setError(null);
    setIsSubmitting(true);
    try {
      await apiClient.post('/api/users/me/change-password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });

      // Password changed successfully - now we need to re-login to get a fresh token
      // without the must_change_password flag
      alert('Password changed successfully! Please log in with your new password.');
      logout();
      navigate('/login', { replace: true });
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to change password. Please try again.');
      } else {
        setError('Failed to change password. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background decoration */}
      <Box
        sx={{
          position: 'absolute',
          top: -100,
          right: -100,
          width: 400,
          height: 400,
          borderRadius: '50%',
          background: (theme) =>
            `radial-gradient(circle, ${alpha(theme.palette.warning.main, 0.2)} 0%, transparent 70%)`,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          bottom: -150,
          left: -150,
          width: 500,
          height: 500,
          borderRadius: '50%',
          background: (theme) =>
            `radial-gradient(circle, ${alpha(theme.palette.secondary.main, 0.15)} 0%, transparent 70%)`,
        }}
      />

      <Card
        sx={{
          maxWidth: 480,
          width: '100%',
          zIndex: 1,
        }}
      >
        <CardContent sx={{ p: 4 }}>
          {/* Header */}
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Box
              component="img"
              src="/logo.svg"
              alt="SmartCBWTF"
              sx={{
                width: 64,
                height: 64,
                mb: 2,
              }}
            />
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              Change Your Password
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              You must change your password before continuing.
            </Typography>
          </Box>

          {/* Error Alert */}
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {/* Change Password Form */}
          <form onSubmit={handleSubmit(onSubmit)}>
            <TextField
              {...register('currentPassword')}
              label="Current Password"
              type={showCurrentPassword ? 'text' : 'password'}
              fullWidth
              margin="normal"
              error={!!errors.currentPassword}
              helperText={errors.currentPassword?.message}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      edge="end"
                      size="small"
                    >
                      {showCurrentPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              autoComplete="current-password"
              autoFocus
            />

            <TextField
              {...register('newPassword')}
              label="New Password"
              type={showNewPassword ? 'text' : 'password'}
              fullWidth
              margin="normal"
              error={!!errors.newPassword}
              helperText={errors.newPassword?.message}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      edge="end"
                      size="small"
                    >
                      {showNewPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              autoComplete="new-password"
            />

            {/* Password Requirements */}
            {newPassword && (
              <Box sx={{ mt: 1, mb: 2 }}>
                <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                  Password Requirements:
                </Typography>
                <List dense sx={{ py: 0 }}>
                  {PASSWORD_REQUIREMENTS.map((req) => {
                    const met = req.regex.test(newPassword);
                    return (
                      <ListItem key={req.id} sx={{ py: 0, px: 0 }}>
                        <ListItemIcon sx={{ minWidth: 28 }}>
                          {met ? (
                            <CheckCircle sx={{ fontSize: 16, color: 'success.main' }} />
                          ) : (
                            <Cancel sx={{ fontSize: 16, color: 'text.disabled' }} />
                          )}
                        </ListItemIcon>
                        <ListItemText
                          primary={req.label}
                          primaryTypographyProps={{
                            variant: 'caption',
                            color: met ? 'success.main' : 'text.secondary',
                          }}
                        />
                      </ListItem>
                    );
                  })}
                </List>
              </Box>
            )}

            <TextField
              {...register('confirmPassword')}
              label="Confirm New Password"
              type={showConfirmPassword ? 'text' : 'password'}
              fullWidth
              margin="normal"
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      edge="end"
                      size="small"
                    >
                      {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              autoComplete="new-password"
            />

            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={isSubmitting}
              sx={{ mt: 3, mb: 2 }}
            >
              {isSubmitting ? <CircularProgress size={24} color="inherit" /> : 'Change Password'}
            </Button>
          </form>

          <Button
            variant="text"
            fullWidth
            onClick={handleLogout}
            sx={{ color: 'text.secondary' }}
          >
            Sign out instead
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ChangePassword;
