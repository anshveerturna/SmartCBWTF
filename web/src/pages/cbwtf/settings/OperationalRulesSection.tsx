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
import { type OperationalRulesDTO, type LockedFieldsDTO, updateOperationalRules } from '../../../api/cbwtf';

interface Props {
  data: OperationalRulesDTO;
  lockedFields: LockedFieldsDTO;
  onSave: () => void;
}

const OperationalRulesSection = ({ data, lockedFields, onSave }: Props) => {
  const [formData, setFormData] = useState<OperationalRulesDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateOperationalRules,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update operational rules');
    }
  });

  const handleChange = (field: keyof OperationalRulesDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    let value: any;
    if (e.target.type === 'checkbox') {
        value = e.target.checked;
    } else if (e.target.type === 'number') {
        value = e.target.value === '' ? '' : parseFloat(e.target.value);
    } else {
        value = e.target.value;
    }
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">QR & Operational Rules</Typography>
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

      {lockedFields.qrRulesLocked && (
        <Alert severity="info" sx={{ mb: 3 }}>
          Some QR settings may be restricted because QR codes have already been actively generated in the system.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="number"
            label="QR Validity (Days)"
            value={formData.qrValidityDays}
            onChange={handleChange('qrValidityDays')}
            helperText="Days before an unused QR code expires"
            inputProps={{ min: 1, max: 365 }}
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="number"
            label="Geofence Radius (Meters)"
            value={formData.gpsGeofenceRadiusM}
            onChange={handleChange('gpsGeofenceRadiusM')}
            helperText="Allowed GPS deviation for pickups"
            inputProps={{ min: 50, max: 5000 }}
          />
        </Grid>
        
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="number"
            label="Max Unverified Bags"
            value={formData.maxUnverifiedBags}
            onChange={handleChange('maxUnverifiedBags')}
            helperText="Max bags allowed without barcode scan per pickup"
            inputProps={{ min: 1, max: 500 }}
          />
        </Grid>
        <Grid item xs={12} md={6}>
           <TextField
            fullWidth
            type="number"
            label="Min Blue Waste (%)"
            value={formData.blueWasteMinPercent}
            onChange={handleChange('blueWasteMinPercent')}
            helperText="Minimum % of waste expected to be Blue category"
            inputProps={{ step: 0.1, min: 0, max: 100 }}
          />
        </Grid>

        <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.allowMultipleActiveQrs}
                onChange={handleChange('allowMultipleActiveQrs')}
              />
            }
            label="Allow Multiple Active QRs per HCF"
          />
        </Grid>
        <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.requireCbwtfVerification}
                onChange={handleChange('requireCbwtfVerification')}
              />
            }
            label="Require CBWTF Verification Scan"
          />
           <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
             Bags must be scanned at CBWTF facility to close the chain of custody.
           </Typography>
        </Grid>
      </Grid>
    </Box>
  );
};

export default OperationalRulesSection;
