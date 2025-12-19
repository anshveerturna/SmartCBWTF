import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Stepper,
  Step,
  StepLabel,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  InputAdornment,
  Divider,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  ArrowBack as ArrowBackIcon,
  ArrowForward as ArrowForwardIcon,
  Check as CheckIcon,
} from '@mui/icons-material';
import { adminApi, type OnboardTenantRequest } from '../../api/admin';

const steps = ['Basic Info', 'Contact', 'Location', 'Subscription', 'Review'];

const defaultFormData: OnboardTenantRequest = {
  code: '',
  name: '',
  address: '',
  contactEmail: '',
  contactPhone: '',
  gpsLat: undefined,
  gpsLon: undefined,
  geofenceRadiusM: 100,
  subscriptionPlan: 'TRIAL',
  trialDays: 30,
  adminEmail: '',
  adminName: '',
};

export default function TenantOnboarding() {
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [formData, setFormData] = useState<OnboardTenantRequest>(defaultFormData);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const mutation = useMutation({
    mutationFn: adminApi.onboardTenant,
    onSuccess: (tenant) => {
      navigate(`/superadmin/tenants/${tenant.id}`, {
        state: { newlyCreated: true },
      });
    },
  });

  const updateField = (field: keyof OnboardTenantRequest, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const validateStep = (step: number): boolean => {
    const newErrors: Record<string, string> = {};

    if (step === 0) {
      if (!formData.code) newErrors.code = 'Code is required';
      else if (!/^[A-Z0-9_-]+$/.test(formData.code)) {
        newErrors.code = 'Code must be uppercase alphanumeric';
      }
      if (!formData.name) newErrors.name = 'Name is required';
      if (!formData.address) newErrors.address = 'Address is required';
    }

    if (step === 1) {
      if (!formData.contactEmail) newErrors.contactEmail = 'Email is required';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)) {
        newErrors.contactEmail = 'Invalid email format';
      }
    }

    if (step === 3) {
      if (!formData.adminEmail) newErrors.adminEmail = 'Admin email is required';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.adminEmail)) {
        newErrors.adminEmail = 'Invalid email format';
      }
      if (!formData.adminName) newErrors.adminName = 'Admin name is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep(activeStep)) {
      if (activeStep === steps.length - 1) {
        mutation.mutate(formData);
      } else {
        setActiveStep((prev) => prev + 1);
      }
    }
  };

  const handleBack = () => {
    setActiveStep((prev) => prev - 1);
  };

  const renderStepContent = (step: number) => {
    switch (step) {
      case 0: // Basic Info
        return (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="Facility Code"
                value={formData.code}
                onChange={(e) => updateField('code', e.target.value.toUpperCase())}
                error={!!errors.code}
                helperText={errors.code || 'e.g., PUNE-CBWTF-01'}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 8 }}>
              <TextField
                label="Facility Name"
                value={formData.name}
                onChange={(e) => updateField('name', e.target.value)}
                error={!!errors.name}
                helperText={errors.name}
                fullWidth
                required
              />
            </Grid>
            <Grid size={12}>
              <TextField
                label="Address"
                value={formData.address}
                onChange={(e) => updateField('address', e.target.value)}
                error={!!errors.address}
                helperText={errors.address}
                fullWidth
                multiline
                rows={2}
                required
              />
            </Grid>
          </Grid>
        );

      case 1: // Contact
        return (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Contact Email"
                type="email"
                value={formData.contactEmail}
                onChange={(e) => updateField('contactEmail', e.target.value)}
                error={!!errors.contactEmail}
                helperText={errors.contactEmail}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Contact Phone"
                value={formData.contactPhone}
                onChange={(e) => updateField('contactPhone', e.target.value)}
                fullWidth
                placeholder="+91 98765 43210"
              />
            </Grid>
          </Grid>
        );

      case 2: // Location
        return (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="GPS Latitude"
                type="number"
                value={formData.gpsLat ?? ''}
                onChange={(e) => updateField('gpsLat', parseFloat(e.target.value) || undefined)}
                fullWidth
                inputProps={{ step: 0.000001 }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="GPS Longitude"
                type="number"
                value={formData.gpsLon ?? ''}
                onChange={(e) => updateField('gpsLon', parseFloat(e.target.value) || undefined)}
                fullWidth
                inputProps={{ step: 0.000001 }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="Geofence Radius"
                type="number"
                value={formData.geofenceRadiusM ?? ''}
                onChange={(e) => updateField('geofenceRadiusM', parseInt(e.target.value) || 100)}
                fullWidth
                InputProps={{
                  endAdornment: <InputAdornment position="end">meters</InputAdornment>,
                }}
              />
            </Grid>
          </Grid>
        );

      case 3: // Subscription
        return (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth>
                <InputLabel>Subscription Plan</InputLabel>
                <Select
                  value={formData.subscriptionPlan}
                  label="Subscription Plan"
                  onChange={(e) => updateField('subscriptionPlan', e.target.value)}
                >
                  <MenuItem value="TRIAL">Trial</MenuItem>
                  <MenuItem value="BASIC">Basic</MenuItem>
                  <MenuItem value="PRO">Pro</MenuItem>
                  <MenuItem value="ENTERPRISE">Enterprise</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Trial Days"
                type="number"
                value={formData.trialDays ?? 30}
                onChange={(e) => updateField('trialDays', parseInt(e.target.value) || 0)}
                fullWidth
                helperText="Set to 0 for immediate activation"
              />
            </Grid>
            <Grid size={12}>
              <Divider sx={{ my: 2 }}>Initial Admin User</Divider>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Admin Email"
                type="email"
                value={formData.adminEmail}
                onChange={(e) => updateField('adminEmail', e.target.value)}
                error={!!errors.adminEmail}
                helperText={errors.adminEmail || 'This will be the login username'}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Admin Name"
                value={formData.adminName}
                onChange={(e) => updateField('adminName', e.target.value)}
                error={!!errors.adminName}
                helperText={errors.adminName}
                fullWidth
                required
              />
            </Grid>
          </Grid>
        );

      case 4: // Review
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Review Tenant Details
            </Typography>
            <Stack spacing={2}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Facility Code
                </Typography>
                <Typography fontFamily="monospace">{formData.code}</Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Facility Name
                </Typography>
                <Typography>{formData.name}</Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Address
                </Typography>
                <Typography>{formData.address}</Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Contact
                </Typography>
                <Typography>{formData.contactEmail}</Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Subscription
                </Typography>
                <Typography>
                  {formData.subscriptionPlan}
                  {formData.trialDays && formData.trialDays > 0 && ` (${formData.trialDays} day trial)`}
                </Typography>
              </Box>
              <Divider />
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Admin Email (Login)
                </Typography>
                <Typography>{formData.adminEmail}</Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Admin Name
                </Typography>
                <Typography>{formData.adminName}</Typography>
              </Box>
            </Stack>
            <Alert severity="info" sx={{ mt: 3 }}>
              A temporary password will be generated for the admin user. They will be required to change it on first login.
            </Alert>
          </Box>
        );

      default:
        return null;
    }
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/superadmin/tenants')}
          sx={{ mb: 2 }}
        >
          Back to Tenants
        </Button>
        <Typography variant="h4" fontWeight={700}>
          Onboard New Tenant
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Register a new CBWTF facility on the platform
        </Typography>
      </Box>

      {/* Stepper */}
      <Card sx={{ mb: 4, bgcolor: 'background.paper', borderRadius: 2 }}>
        <CardContent>
          <Stepper activeStep={activeStep} alternativeLabel>
            {steps.map((label) => (
              <Step key={label}>
                <StepLabel>{label}</StepLabel>
              </Step>
            ))}
          </Stepper>
        </CardContent>
      </Card>

      {/* Error */}
      {mutation.isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to onboard tenant. Please check the details and try again.
        </Alert>
      )}

      {/* Form Content */}
      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 4 }}>
          {renderStepContent(activeStep)}

          {/* Actions */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
            <Button
              onClick={handleBack}
              disabled={activeStep === 0}
              startIcon={<ArrowBackIcon />}
            >
              Back
            </Button>
            <Button
              variant="contained"
              onClick={handleNext}
              disabled={mutation.isPending}
              endIcon={activeStep === steps.length - 1 ? <CheckIcon /> : <ArrowForwardIcon />}
            >
              {mutation.isPending
                ? 'Creating...'
                : activeStep === steps.length - 1
                ? 'Create Tenant'
                : 'Next'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
