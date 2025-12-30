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
import { type FinancialSettingsDTO, type LockedFieldsDTO, updateFinancialSettings } from '../../../api/cbwtf';

interface Props {
  data: FinancialSettingsDTO;
  lockedFields: LockedFieldsDTO;
  onSave: () => void;
}

const FinancialSettingsSection = ({ data, lockedFields, onSave }: Props) => {
  const [formData, setFormData] = useState<FinancialSettingsDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateFinancialSettings,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update financial settings');
    }
  });

  const handleChange = (field: keyof FinancialSettingsDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = field === 'gstEnabled' ? e.target.checked : parseFloat(e.target.value);
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Financial & Billing</Typography>
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

      {lockedFields.gstLocked && (
        <Alert severity="info" sx={{ mb: 3 }}>
          GST rates are locked because invoices have already been generated. 
          Contact SuperAdmin if changes are required for valid business reasons.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.gstEnabled}
                onChange={handleChange('gstEnabled')}
                disabled={lockedFields.gstLocked}
              />
            }
            label="Enable GST Calculation"
          />
        </Grid>

        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="CGST %"
            value={formData.cgstPercent}
            onChange={handleChange('cgstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="SGST %"
            value={formData.sgstPercent}
            onChange={handleChange('sgstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="IGST %"
            value={formData.igstPercent}
            onChange={handleChange('igstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        
        <Grid item xs={12}>
           <Typography variant="caption" color="text.secondary">
             Note: IGST is applied for inter-state transactions based on the HCF's state. 
             CGST+SGST is applied for intra-state transactions.
           </Typography>
        </Grid>
      </Grid>
    </Box>
  );
};

export default FinancialSettingsSection;
