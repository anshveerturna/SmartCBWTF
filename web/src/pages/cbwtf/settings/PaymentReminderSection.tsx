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
import { type PaymentReminderDTO, updatePaymentReminders } from '../../../api/cbwtf';

interface Props {
  data: PaymentReminderDTO;
  onSave: () => void;
}

const PaymentReminderSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<PaymentReminderDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updatePaymentReminders,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update payment settings');
    }
  });

  const handleChange = (field: keyof PaymentReminderDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
     const value = field === 'autoAlertEscalation' ? e.target.checked : parseInt(e.target.value);
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Payments & Reminders</Typography>
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
            type="number"
            label="Grace Period (Days)"
            value={formData.gracePeriodDays}
            onChange={handleChange('gracePeriodDays')}
            helperText="Days after invoice generation before late fees apply"
             inputProps={{ min: 0, max: 30 }}
          />
        </Grid>

         <Grid item xs={12} md={6}>
            <FormControlLabel
            control={
              <Switch
                checked={formData.autoAlertEscalation}
                onChange={handleChange('autoAlertEscalation')}
              />
            }
            label="Auto-Escalate Overdue Alerts"
            sx={{ mt: 2 }}
          />
           <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
             Automatically increase alert frequency and urgency for overdue invoices.
           </Typography>
        </Grid>
      </Grid>
    </Box>
  );
};

export default PaymentReminderSection;
