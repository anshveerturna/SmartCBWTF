import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Divider,
  Stack,
} from '@mui/material';
import {
  Settings as SettingsIcon,
  Save as SaveIcon,
} from '@mui/icons-material';
import { getNotificationSettings, updateNotificationSettings } from '../../api/cbwtf';

interface NotificationSettings {
  paymentReminderStartDays: number;
  paymentReminderFrequencyDays: number;
  maxOverdueReminders: number;
  agreementExpiryWarningDays: number;
  updatedAt: string;
}

export default function NotificationSettings() {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<NotificationSettings | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const { data: settings, isLoading, error } = useQuery<NotificationSettings>({
    queryKey: ['notificationSettings'],
    queryFn: getNotificationSettings,
  });

  useEffect(() => {
    if (settings && !formData) {
      setFormData(settings);
    }
  }, [settings, formData]);

  const updateMutation = useMutation({
    mutationFn: updateNotificationSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationSettings'] });
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    },
  });

  const handleChange = (field: keyof NotificationSettings) => (e: React.ChangeEvent<HTMLInputElement>) => {
    if (formData) {
      setFormData({ ...formData, [field]: parseInt(e.target.value) || 0 });
    }
  };

  const handleSave = () => {
    if (formData) {
      updateMutation.mutate(formData);
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">Failed to load notification settings</Alert>;
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <SettingsIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Notification Settings
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Configure payment reminders and agreement expiry warnings
          </Typography>
        </Box>
      </Box>

      {saveSuccess && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings saved successfully!
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Payment Reminders
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            Configure when and how often payment reminders are sent to HCFs
          </Typography>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <TextField
              fullWidth
              label="Days Before Due to Start Reminders"
              type="number"
              value={formData?.paymentReminderStartDays ?? settings?.paymentReminderStartDays ?? 7}
              onChange={handleChange('paymentReminderStartDays')}
              helperText="Reminders begin this many days before the due date"
              inputProps={{ min: 1, max: 30 }}
            />
            <TextField
              fullWidth
              label="Reminder Frequency (Days)"
              type="number"
              value={formData?.paymentReminderFrequencyDays ?? settings?.paymentReminderFrequencyDays ?? 3}
              onChange={handleChange('paymentReminderFrequencyDays')}
              helperText="Days between each reminder"
              inputProps={{ min: 1, max: 14 }}
            />
            <TextField
              fullWidth
              label="Max Overdue Reminders"
              type="number"
              value={formData?.maxOverdueReminders ?? settings?.maxOverdueReminders ?? 5}
              onChange={handleChange('maxOverdueReminders')}
              helperText="Stop reminders after this many overdue notices"
              inputProps={{ min: 1, max: 10 }}
            />
          </Stack>

          <Divider sx={{ my: 4 }} />

          <Typography variant="h6" gutterBottom>
            Agreement Expiry Warnings
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            Alert HCFs when their agreement is about to expire
          </Typography>

          <Box maxWidth={400}>
            <TextField
              fullWidth
              label="Days Before Expiry to Warn"
              type="number"
              value={formData?.agreementExpiryWarningDays ?? settings?.agreementExpiryWarningDays ?? 30}
              onChange={handleChange('agreementExpiryWarningDays')}
              helperText="Send warning this many days before agreement expires"
              inputProps={{ min: 7, max: 90 }}
            />
          </Box>

          <Box mt={4} display="flex" justifyContent="flex-end">
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={updateMutation.isPending}
            >
              {updateMutation.isPending ? 'Saving...' : 'Save Settings'}
            </Button>
          </Box>

          {settings?.updatedAt && (
            <Typography variant="caption" color="text.secondary" mt={2} display="block">
              Last updated: {new Date(settings.updatedAt).toLocaleString('en-IN')}
            </Typography>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
