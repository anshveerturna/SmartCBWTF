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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  Paper,
  Chip,
} from '@mui/material';
import { Save as SaveIcon, Preview as PreviewIcon, Article as TermsIcon } from '@mui/icons-material';
import { type AgreementRulesDTO, updateAgreementRules, updateAgreementTermsTemplate, previewAgreementNumber } from '../../../api/cbwtf';

interface Props {
  data: AgreementRulesDTO;
  onSave: () => void;
}

const MAX_TERMS_TEMPLATE_LENGTH = 20_000;

const AgreementRulesSection = ({ data, onSave }: Props) => {
  const [formData, setFormData] = useState<AgreementRulesDTO>(data);
  const [error, setError] = useState<string | null>(null);
  const [termsTemplate, setTermsTemplate] = useState<string>(data.agreementTermsTemplate || '');
  const [termsError, setTermsError] = useState<string | null>(null);
  const [termsSaved, setTermsSaved] = useState(false);
  const [previewDebounce, setPreviewDebounce] = useState<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setFormData(data);
    setTermsTemplate(data.agreementTermsTemplate || '');
  }, [data]);

  // Live preview of agreement number
  const { data: previewData, refetch: refetchPreview } = useQuery({
    queryKey: ['agreement-number-preview', formData.agreementNumberPrefix, formData.agreementNumberSeparator,
      formData.agreementNumberSequenceDigits, formData.agreementNumberIncludeFacilityCode,
      formData.agreementNumberIncludeYear, formData.agreementNumberTemplate, formData.agreementNumberResetFrequency],
    queryFn: () => previewAgreementNumber({
      prefix: formData.agreementNumberPrefix,
      separator: formData.agreementNumberSeparator,
      digits: formData.agreementNumberSequenceDigits,
      includeFacilityCode: formData.agreementNumberIncludeFacilityCode,
      includeYear: formData.agreementNumberIncludeYear,
      template: formData.agreementNumberTemplate,
      resetFrequency: formData.agreementNumberResetFrequency,
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
    formData.agreementNumberIncludeYear, formData.agreementNumberTemplate,
    formData.agreementNumberResetFrequency]);

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

  const termsMutation = useMutation({
    mutationFn: (template: string) => updateAgreementTermsTemplate(template),
    onSuccess: () => {
      setTermsSaved(true);
      setTermsError(null);
      setTimeout(() => setTermsSaved(false), 3000);
    },
    onError: (err: Error) => {
      setTermsError(err.message || 'Failed to update terms template');
    }
  });

  const termsValidationError =
    termsTemplate.length > MAX_TERMS_TEMPLATE_LENGTH
      ? `Agreement terms template must be ${MAX_TERMS_TEMPLATE_LENGTH.toLocaleString('en-IN')} characters or less.`
      : null;

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

        <Grid item xs={12} md={4}>
          <FormControl fullWidth>
            <InputLabel id="agreement-reset-frequency-label">Reset Frequency</InputLabel>
            <Select
              labelId="agreement-reset-frequency-label"
              label="Reset Frequency"
              value={formData.agreementNumberResetFrequency}
              onChange={(e) => setFormData({
                ...formData,
                agreementNumberResetFrequency: e.target.value as AgreementRulesDTO['agreementNumberResetFrequency'],
              })}
            >
              <MenuItem value="MONTHLY">Monthly</MenuItem>
              <MenuItem value="YEARLY">Yearly</MenuItem>
              <MenuItem value="NEVER">Never</MenuItem>
            </Select>
          </FormControl>
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

        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Custom Format Template"
            value={formData.agreementNumberTemplate || ''}
            onChange={handleChange('agreementNumberTemplate')}
            placeholder="{{sequence}} {{month}} {{year}}"
            helperText="Optional. Supported placeholders: {{sequence}}, {{month}}, {{year}}, {{prefix}}, {{facilityCode}}. Leave blank to use the legacy format fields below."
          />
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

      {/* Agreement Terms & Conditions Template */}
      <Divider sx={{ my: 4 }} />
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TermsIcon color="primary" />
          <Typography variant="h6">Agreement Terms &amp; Conditions Template</Typography>
        </Box>
        <Button
          variant="contained"
          size="small"
          startIcon={termsMutation.isPending ? <CircularProgress size={16} color="inherit" /> : <SaveIcon />}
          onClick={() => {
            if (!termsValidationError) termsMutation.mutate(termsTemplate);
          }}
          disabled={termsMutation.isPending || !!termsValidationError}
        >
          Save Terms
        </Button>
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        These terms &amp; conditions will be embedded in every agreement PDF generated for new HCFs. 
        Each clause should be on a new line. Numbering is applied automatically.
      </Typography>

      {termsError && (
        <Alert severity="error" sx={{ mb: 2 }}>{termsError}</Alert>
      )}
      {termsValidationError && (
        <Alert severity="warning" sx={{ mb: 2 }}>{termsValidationError}</Alert>
      )}
      {termsSaved && (
        <Alert severity="success" sx={{ mb: 2 }}>Terms template saved successfully</Alert>
      )}

      <TextField
        fullWidth
        multiline
        minRows={10}
        maxRows={25}
        value={termsTemplate}
        onChange={(e) => setTermsTemplate(e.target.value)}
        placeholder={"The CBWTF shall collect, transport, and dispose of biomedical waste...\nThe HCF shall segregate biomedical waste as per BMWM Rules...\nThe agreement shall be valid for the period specified above..."}
        sx={{
          '& .MuiInputBase-root': {
            fontFamily: 'monospace',
            fontSize: '0.875rem',
            lineHeight: 1.8,
          },
        }}
        helperText={`${termsTemplate.split('\n').filter(l => l.trim()).length} clause(s) — ${termsTemplate.length.toLocaleString('en-IN')}/${MAX_TERMS_TEMPLATE_LENGTH.toLocaleString('en-IN')} characters`}
      />
    </Box>
  );
};

export default AgreementRulesSection;
