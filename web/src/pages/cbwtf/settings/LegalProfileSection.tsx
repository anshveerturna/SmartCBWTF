import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Box,
  Typography,
  TextField,
  Button,
  Grid,
  Alert,
  CircularProgress
} from '@mui/material';
import { Save as SaveIcon } from '@mui/icons-material';
import { type LegalProfileDTO, type LockedFieldsDTO, updateLegalProfile } from '../../../api/cbwtf';

interface Props {
  data: LegalProfileDTO;
  lockedFields: LockedFieldsDTO;
  onSave: () => void;
}

const LegalProfileSection = ({ data, lockedFields, onSave }: Props) => {
  const [formData, setFormData] = useState<LegalProfileDTO>(data);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: updateLegalProfile,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update legal profile');
    }
  });

  const handleChange = (field: keyof LegalProfileDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData({ ...formData, [field]: e.target.value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Legal & Entity Profile</Typography>
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
            label="Legal Name (for Invoices)"
            value={formData.legalName || ''}
            onChange={handleChange('legalName')}
            required
            helperText="Official registered name of the entity"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Trade Name (Display Name)"
            value={formData.tradeName || ''}
            onChange={handleChange('tradeName')}
            helperText="Name displayed on dashboards and internal views"
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Authorization Number"
            value={formData.authorizationNumber || ''}
            onChange={handleChange('authorizationNumber')}
            helperText="SPCB Authorization Number"
            disabled={lockedFields.complianceLocked}
            InputProps={{
              endAdornment: lockedFields.complianceLocked ? (
                <Typography variant="caption" color="warning.main">Locked</Typography>
              ) : null
            }}
          />
          {lockedFields.complianceLocked && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              Locked after first compliance report. Contact SuperAdmin to change.
            </Typography>
          )}
        </Grid>

        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="GSTIN"
            value={formData.gstin || ''}
            onChange={handleChange('gstin')}
            disabled={lockedFields.gstLocked}
             InputProps={{
              endAdornment: lockedFields.gstLocked ? (
                <Typography variant="caption" color="warning.main">Locked</Typography>
              ) : null
            }}
          />
          {lockedFields.gstLocked && (
             <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              Locked after first invoice generation. Contact SuperAdmin to change.
            </Typography>
          )}
        </Grid>
        
        <Grid item xs={12} md={6}>
           <TextField
            fullWidth
            label="PAN"
            value={formData.pan || ''}
            onChange={handleChange('pan')}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="SPCB Name"
            value={formData.spcbName || ''}
            onChange={handleChange('spcbName')}
            disabled={lockedFields.complianceLocked}
          />
        </Grid>
        
         <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="SPCB State"
            value={formData.spcbState || ''}
            onChange={handleChange('spcbState')}
            disabled={lockedFields.complianceLocked}
          />
        </Grid>

        <Grid item xs={12}>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Registered Address"
            value={formData.registeredAddress || ''}
            onChange={handleChange('registeredAddress')}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Official Email"
            type="email"
            value={formData.officialEmail || ''}
            onChange={handleChange('officialEmail')}
          />
        </Grid>
         <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Official Phone"
            value={formData.officialPhone || ''}
            onChange={handleChange('officialPhone')}
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default LegalProfileSection;
