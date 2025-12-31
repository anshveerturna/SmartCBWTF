import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Box,
  Typography,
  TextField,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Switch,
  FormControlLabel,
  Paper,
  Chip,
  Tooltip,
} from '@mui/material';
import {
  Save as SaveIcon,
  Lock as LockIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { type EmailSettingsDTO, updateEmailSettings } from '../../../api/cbwtf';
import { useTheme } from '@mui/material/styles';

interface Props {
  data: EmailSettingsDTO;
  onSave: () => void;
}

const EmailSettingsSection = ({ data, onSave }: Props) => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  
  // Only editable fields are in state
  const [formData, setFormData] = useState({
    useGenericSender: data.useGenericSender,
    notificationEmail: data.notificationEmail || '',
    ccAdminOnHcfEmails: data.ccAdminOnHcfEmails,
    emailNotificationsEnabled: data.emailNotificationsEnabled,
    inAppAlertsEnabled: data.inAppAlertsEnabled,
  });
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => updateEmailSettings({
      ...data, // Include read-only fields for type safety
      ...formData,
    }),
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update email settings');
    }
  });

  const handleToggle = (field: keyof typeof formData) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData({ ...formData, [field]: e.target.checked });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate();
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Email & Notifications</Typography>
        <Button 
          variant="contained" 
          startIcon={mutation.isPending ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
          type="submit"
          disabled={mutation.isPending}
        >
          Save Changes
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      {mutation.isSuccess && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings updated successfully
        </Alert>
      )}

      {/* Sender Identity Section - READ ONLY */}
      <Paper 
        variant="outlined" 
        sx={{ 
          p: 3, 
          mb: 3, 
          bgcolor: isDark ? 'background.default' : 'grey.50',
          borderColor: isDark ? 'divider' : 'grey.300'
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <LockIcon color="action" fontSize="small" />
          <Typography variant="subtitle1" fontWeight={600}>
            Sender Identity (System-Controlled)
          </Typography>
          {data.senderSlugLocked && (
            <Chip label="Locked" size="small" color="warning" variant="outlined" />
          )}
        </Box>
        
        <Alert severity="info" sx={{ mb: 2 }} icon={<InfoIcon />}>
          Sender identity is managed by SmartCBWTF to ensure secure email delivery and prevent spoofing.
        </Alert>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
              From Name
            </Typography>
            <Typography variant="body1" fontWeight={500}>
              {data.resolvedSenderName || 'SmartCBWTF'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Automatically generated from facility name
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
              From Address
            </Typography>
            <Typography variant="body1" fontWeight={500} sx={{ fontFamily: 'monospace' }}>
              {data.resolvedSenderEmail || 'no-reply@smartcbwtf.com'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Automatically generated sender address
            </Typography>
          </Grid>
        </Grid>

        <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
          <Tooltip title="When enabled, emails will be sent from no-reply@smartcbwtf.com instead of your facility-specific address">
            <FormControlLabel
              control={
                <Switch
                  checked={formData.useGenericSender}
                  onChange={handleToggle('useGenericSender')}
                />
              }
              label="Use generic sender (no-reply@smartcbwtf.com)"
            />
          </Tooltip>
        </Box>
      </Paper>

      {/* Notification Email - EDITABLE */}
      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
          Notification Receiving Email
        </Typography>
        <TextField
          fullWidth
          type="email"
          label="CBWTF Notification Email"
          value={formData.notificationEmail}
          onChange={(e) => setFormData({ ...formData, notificationEmail: e.target.value })}
          helperText="System emails (alerts, compliance reports, billing events) will be sent to this address"
          sx={{ mb: 2 }}
        />
      </Paper>

      {/* Other Settings */}
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.ccAdminOnHcfEmails}
                onChange={handleToggle('ccAdminOnHcfEmails')}
              />
            }
            label="CC Admin on HCF Communications"
          />
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.emailNotificationsEnabled}
                onChange={handleToggle('emailNotificationsEnabled')}
              />
            }
            label="Enable Email Notifications"
          />
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.inAppAlertsEnabled}
                onChange={handleToggle('inAppAlertsEnabled')}
              />
            }
            label="Enable In-App Alerts"
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default EmailSettingsSection;
