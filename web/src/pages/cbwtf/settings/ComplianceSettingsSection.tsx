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
import { type ComplianceSettingsDTO, type LockedFieldsDTO, updateComplianceSettings } from '../../../api/cbwtf';

interface Props {
  data: ComplianceSettingsDTO;
  lockedFields: LockedFieldsDTO;
  onSave: () => void;
}

const ComplianceSettingsSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<ComplianceSettingsDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateComplianceSettings,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update compliance settings');
    }
  });

  const handleChange = (field: keyof ComplianceSettingsDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = field === 'enforceChecksum' ? e.target.checked : e.target.value;
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Compliance & Reporting</Typography>
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
            type="time"
            label="Daily Report Generation Time"
            value={formData.dailyReportTime}
            onChange={handleChange('dailyReportTime')}
            InputLabelProps={{ shrink: true }}
            inputProps={{ step: 300 }} // 5 min
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="number"
            label="Monthly Report Generation Day"
            value={formData.monthlyReportDay}
            onChange={handleChange('monthlyReportDay')}
            helperText="Day of month (1-28) to generate monthly summary"
            inputProps={{ min: 1, max: 28 }}
          />
        </Grid>
        
        <Grid item xs={12} md={6}>
           <TextField
            fullWidth
            type="date"
            label="Annual Form IV Target Date"
            value={formData.annualFormIvDate || ''}
            onChange={handleChange('annualFormIvDate')}
            InputLabelProps={{ shrink: true }}
             helperText="Target deadline for Form IV submission"
          />
        </Grid>

        <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.enforceChecksum}
                onChange={handleChange('enforceChecksum')}
              />
            }
            label="Enforce Report Checksums"
          />
           <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
             Ensure all generated PDF reports have a verifiable cryptographic checksum for tamper-evidence.
           </Typography>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ComplianceSettingsSection;
