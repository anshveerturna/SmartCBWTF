import { useState } from 'react';
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

const RANGES = {
  paymentReminderStartDays: { min: 1, max: 30, label: 'Days before due to start reminders' },
  paymentReminderFrequencyDays: { min: 1, max: 14, label: 'Reminder frequency' },
  maxOverdueReminders: { min: 1, max: 10, label: 'Max overdue reminders' },
  agreementExpiryWarningDays: { min: 7, max: 90, label: 'Days before expiry to warn' },
} as const;

const editablePayload = (settings: NotificationSettings) => ({
  paymentReminderStartDays: settings.paymentReminderStartDays,
  paymentReminderFrequencyDays: settings.paymentReminderFrequencyDays,
  maxOverdueReminders: settings.maxOverdueReminders,
  agreementExpiryWarningDays: settings.agreementExpiryWarningDays,
});

const getValidationError = (settings: NotificationSettings | null | undefined): string | null => {
  if (!settings) return null;

  for (const [field, config] of Object.entries(RANGES)) {
    const value = settings[field as keyof typeof RANGES];
    if (!Number.isInteger(value) || value < config.min || value > config.max) {
      return `${config.label} must be between ${config.min} and ${config.max}.`;
    }
  }
  return null;
};

export default function NotificationSettings() {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<NotificationSettings | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const { data: settings, isLoading, error } = useQuery<NotificationSettings>({
    queryKey: ['notificationSettings'],
    queryFn: getNotificationSettings,
  });

  const updateMutation = useMutation({
    mutationFn: updateNotificationSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notificationSettings'] });
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    },
  });

  const handleChange = (field: keyof NotificationSettings) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const current = formData ?? settings;
    if (!current) return;
    const parsed = Number.parseInt(e.target.value, 10);
    setFormData({ ...current, [field]: Number.isNaN(parsed) ? 0 : parsed });
  };

  const handleSave = () => {
    const payload = formData ?? settings;
    const validationError = getValidationError(payload);
    if (payload && !validationError) {
      updateMutation.mutate(editablePayload(payload));
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

  const currentSettings = formData ?? settings;
  const validationError = getValidationError(currentSettings);

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

      {validationError && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          {validationError}
        </Alert>
      )}

      {updateMutation.isError && !validationError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to save notification settings
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
              value={currentSettings?.paymentReminderStartDays ?? 7}
              onChange={handleChange('paymentReminderStartDays')}
              helperText="Reminders begin this many days before the due date"
              inputProps={{ min: 1, max: 30 }}
            />
            <TextField
              fullWidth
              label="Reminder Frequency (Days)"
              type="number"
              value={currentSettings?.paymentReminderFrequencyDays ?? 3}
              onChange={handleChange('paymentReminderFrequencyDays')}
              helperText="Days between each reminder"
              inputProps={{ min: 1, max: 14 }}
            />
            <TextField
              fullWidth
              label="Max Overdue Reminders"
              type="number"
              value={currentSettings?.maxOverdueReminders ?? 5}
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
              value={currentSettings?.agreementExpiryWarningDays ?? 30}
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
              disabled={updateMutation.isPending || !!validationError}
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
