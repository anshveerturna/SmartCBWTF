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
import { type AgreementRulesDTO, updateAgreementRules } from '../../../api/cbwtf';

interface Props {
  data: AgreementRulesDTO;
  onSave: () => void;
}

const AgreementRulesSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<AgreementRulesDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateAgreementRules,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update agreement rules');
    }
  });

  const handleChange = (field: keyof AgreementRulesDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = field === 'blockOverlappingAgreements' ? e.target.checked : parseInt(e.target.value);
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
       <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Agreement & Contract Rules</Typography>
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
            label="Default Agreement Validity (Months)"
            value={formData.defaultAgreementValidityMonths}
            onChange={handleChange('defaultAgreementValidityMonths')}
            helperText="Standard duration for new agreements"
            inputProps={{ min: 1, max: 60 }}
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            type="number"
            label="Renewal Window (Days)"
            value={formData.agreementRenewalWindowDays}
            onChange={handleChange('agreementRenewalWindowDays')}
            helperText="Days before expiry to allow renewal"
            inputProps={{ min: 7, max: 90 }}
          />
        </Grid>

         <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.blockOverlappingAgreements}
                onChange={handleChange('blockOverlappingAgreements')}
              />
            }
            label="Block Overlapping Agreements"
          />
           <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
             Prevent creating a new agreement if an active agreement already covers the same period.
           </Typography>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AgreementRulesSection;
