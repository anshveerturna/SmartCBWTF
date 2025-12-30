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
  FormControlLabel
} from '@mui/material';
import { Save as SaveIcon } from '@mui/icons-material';
import { type EmailSettingsDTO, updateEmailSettings } from '../../../api/cbwtf';

interface Props {
  data: EmailSettingsDTO;
  onSave: () => void;
}

const EmailSettingsSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<EmailSettingsDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateEmailSettings,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update email settings');
    }
  });

  const handleChange = (field: keyof EmailSettingsDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
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

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Sender Name"
            value={formData.senderName}
            onChange={handleChange('senderName')}
            helperText="Name displayed in FROM field of emails"
            required
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="email"
            label="Sender Email"
            value={formData.senderEmail}
            onChange={handleChange('senderEmail')}
            helperText="Email address used for sending notifications"
            required
          />
        </Grid>

         <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.ccAdminOnHcfEmails}
                onChange={handleChange('ccAdminOnHcfEmails')}
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
                onChange={handleChange('emailNotificationsEnabled')}
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
                onChange={handleChange('inAppAlertsEnabled')}
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
