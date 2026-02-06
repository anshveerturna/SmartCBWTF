import { useState, useEffect, useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
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
  Divider,
  Paper,
  Chip,
} from '@mui/material';
import { Save as SaveIcon, Preview as PreviewIcon } from '@mui/icons-material';
import { type AgreementRulesDTO, updateAgreementRules, previewAgreementNumber } from '../../../api/cbwtf';

interface Props {
  data: AgreementRulesDTO;
  onSave: () => void;
}

const AgreementRulesSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<AgreementRulesDTO>(data);
  const [error, setError] = useState<string | null>(null);
  const [previewDebounce, setPreviewDebounce] = useState<ReturnType<typeof setTimeout> | null>(null);

  // Live preview of agreement number
  const { data: previewData, refetch: refetchPreview } = useQuery({
    queryKey: ['agreement-number-preview', formData.agreementNumberPrefix, formData.agreementNumberSeparator,
      formData.agreementNumberSequenceDigits, formData.agreementNumberIncludeFacilityCode,
      formData.agreementNumberIncludeYear],
    queryFn: () => previewAgreementNumber({
      prefix: formData.agreementNumberPrefix,
      separator: formData.agreementNumberSeparator,
      digits: formData.agreementNumberSequenceDigits,
      includeFacilityCode: formData.agreementNumberIncludeFacilityCode,
      includeYear: formData.agreementNumberIncludeYear,
    }),
    enabled: false, // Manual trigger via debounce
  });

  // Debounced preview fetch
  const triggerPreview = useCallback(() => {
    if (previewDebounce) clearTimeout(previewDebounce);
    const timeout = setTimeout(() => {
      refetchPreview();
    }, 500);
    setPreviewDebounce(timeout);
  }, [refetchPreview, previewDebounce]);

  // Fetch preview on mount and when format fields change
  useEffect(() => {
    triggerPreview();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formData.agreementNumberPrefix, formData.agreementNumberSeparator,
    formData.agreementNumberSequenceDigits, formData.agreementNumberIncludeFacilityCode,
    formData.agreementNumberIncludeYear]);

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
    const boolFields: (keyof AgreementRulesDTO)[] = [
      'blockOverlappingAgreements', 'agreementNumberIncludeFacilityCode', 'agreementNumberIncludeYear'
    ];
    const intFields: (keyof AgreementRulesDTO)[] = [
      'defaultAgreementValidityMonths', 'agreementRenewalWindowDays', 'agreementNumberSequenceDigits'
    ];

    let value: string | number | boolean;
    if (boolFields.includes(field)) {
      value = e.target.checked;
    } else if (intFields.includes(field)) {
      value = parseInt(e.target.value) || 0;
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
            helperText="Sets the default end date when registering a new HCF"
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

        {/* Agreement Number Format Section */}
        <Grid item xs={12}>
          <Divider sx={{ my: 1 }} />
          <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
            Agreement Number Format
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Configure how agreement numbers are auto-generated. You can also override this with a custom number when registering an HCF.
          </Typography>
        </Grid>

        {/* Live Preview */}
        <Grid item xs={12}>
          <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover', display: 'flex', alignItems: 'center', gap: 2 }}>
            <PreviewIcon color="primary" />
            <Box>
              <Typography variant="caption" color="text.secondary">
                Next Agreement Number Preview
              </Typography>
              <Typography variant="h6" fontFamily="monospace" fontWeight="bold" color="primary">
                {previewData?.preview || '...'}
              </Typography>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Prefix"
            value={formData.agreementNumberPrefix}
            onChange={handleChange('agreementNumberPrefix')}
            helperText='Middle segment (e.g., "HCF", "AGMT")'
            inputProps={{ maxLength: 20 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Separator"
            value={formData.agreementNumberSeparator}
            onChange={handleChange('agreementNumberSeparator')}
            helperText='Character between segments (e.g., "-", "/")'
            inputProps={{ maxLength: 5 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="Sequence Digits"
            value={formData.agreementNumberSequenceDigits}
            onChange={handleChange('agreementNumberSequenceDigits')}
            helperText="Zero-padded digit count (e.g., 5 → 00001)"
            inputProps={{ min: 1, max: 10 }}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.agreementNumberIncludeFacilityCode}
                onChange={handleChange('agreementNumberIncludeFacilityCode')}
              />
            }
            label="Include Facility Code"
          />
          <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
            Prepend your CBWTF facility code as the first segment.
          </Typography>
        </Grid>

        <Grid item xs={12} md={6}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.agreementNumberIncludeYear}
                onChange={handleChange('agreementNumberIncludeYear')}
              />
            }
            label="Include Year"
          />
          <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
            Include the current year in the agreement number.
          </Typography>
        </Grid>

        {/* Format breakdown */}
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, alignItems: 'center' }}>
            <Typography variant="caption" color="text.secondary" mr={1}>Format:</Typography>
            {formData.agreementNumberIncludeFacilityCode && (
              <>
                <Chip label="Facility Code" size="small" variant="outlined" color="info" />
                <Typography variant="caption" color="text.secondary">{formData.agreementNumberSeparator}</Typography>
              </>
            )}
            <Chip label={formData.agreementNumberPrefix || 'PREFIX'} size="small" variant="outlined" color="primary" />
            {formData.agreementNumberIncludeYear && (
              <>
                <Typography variant="caption" color="text.secondary">{formData.agreementNumberSeparator}</Typography>
                <Chip label="YYYY" size="small" variant="outlined" color="secondary" />
              </>
            )}
            <Typography variant="caption" color="text.secondary">{formData.agreementNumberSeparator}</Typography>
            <Chip label={'0'.repeat(formData.agreementNumberSequenceDigits || 5).slice(0, -1) + '1'} size="small" variant="outlined" />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AgreementRulesSection;
