import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Divider,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  ArrowBack as ArrowBackIcon,
  Check as CheckIcon,
} from '@mui/icons-material';
import { adminApi } from '../../../api/admin';
import type { CreateUserRequest } from '../../../api/admin';

const defaultFormData: CreateUserRequest = {
  username: '',
  fullName: '',
  email: '',
  phone: '',
  role: 'CBWTF_ADMIN',
  cbwtfId: undefined,
  hcfId: undefined,
};

export default function CreateUser() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<CreateUserRequest>(defaultFormData);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Fetch CBWTFs for dropdown
  const { data: cbwtfsData } = useQuery({
    queryKey: ['cbwtfs-for-dropdown'],
    queryFn: () => adminApi.listCBWTFs({ size: 100 }),
  });

  const mutation = useMutation({
    mutationFn: adminApi.createUser,
    onSuccess: (user) => {
      navigate(`/superadmin/users/${user.id}`, {
        state: { newlyCreated: true },
      });
    },
  });

  const updateField = (field: keyof CreateUserRequest, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.username) newErrors.username = 'Username is required';
    if (!formData.fullName) newErrors.fullName = 'Full name is required';
    if (!formData.email) newErrors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }
    if (!formData.role) newErrors.role = 'Role is required';
    
    // CBWTF required for non-SUPER_ADMIN roles
    if (formData.role !== 'SUPER_ADMIN' && !formData.cbwtfId) {
      newErrors.cbwtfId = 'CBWTF is required for this role';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (validate()) {
      mutation.mutate(formData);
    }
  };

  const showCbwtfSelector = formData.role !== 'SUPER_ADMIN';
  const showHcfSelector = formData.role === 'HCF_ADMIN';

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/superadmin/users')}
          sx={{ mb: 2 }}
        >
          Back to Users
        </Button>
        <Typography variant="h4" fontWeight={700}>
          Create New User
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Create a new user and assign them to a CBWTF or HCF
        </Typography>
      </Box>

      {/* Error */}
      {mutation.isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to create user. Username may already exist.
        </Alert>
      )}

      {/* Form */}
      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h6" gutterBottom>
            User Details
          </Typography>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Username"
                value={formData.username}
                onChange={(e) => updateField('username', e.target.value)}
                error={!!errors.username}
                helperText={errors.username || 'This will be used for login'}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Full Name"
                value={formData.fullName}
                onChange={(e) => updateField('fullName', e.target.value)}
                error={!!errors.fullName}
                helperText={errors.fullName}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Email"
                type="email"
                value={formData.email}
                onChange={(e) => updateField('email', e.target.value)}
                error={!!errors.email}
                helperText={errors.email}
                fullWidth
                required
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Phone"
                value={formData.phone}
                onChange={(e) => updateField('phone', e.target.value)}
                fullWidth
                placeholder="+91 98765 43210"
              />
            </Grid>
          </Grid>

          <Divider sx={{ my: 4 }}>Role & Assignment</Divider>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth error={!!errors.role}>
                <InputLabel>Role</InputLabel>
                <Select
                  value={formData.role}
                  label="Role"
                  onChange={(e) => updateField('role', e.target.value)}
                >
                  <MenuItem value="SUPER_ADMIN">Super Admin</MenuItem>
                  <MenuItem value="CBWTF_ADMIN">CBWTF Admin</MenuItem>
                  <MenuItem value="HCF_ADMIN">HCF Admin</MenuItem>
                  <MenuItem value="DRIVER">Driver</MenuItem>
                  <MenuItem value="PLANT_OPERATOR">Plant Operator</MenuItem>
                  <MenuItem value="ACCOUNTANT">Accountant</MenuItem>
                  <MenuItem value="TOP_MANAGEMENT">Top Management</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            {showCbwtfSelector && (
              <Grid size={{ xs: 12, md: 4 }}>
                <FormControl fullWidth error={!!errors.cbwtfId}>
                  <InputLabel>CBWTF</InputLabel>
                  <Select
                    value={formData.cbwtfId || ''}
                    label="CBWTF"
                    onChange={(e) => updateField('cbwtfId', e.target.value || undefined)}
                  >
                    <MenuItem value="">Select CBWTF</MenuItem>
                    {cbwtfsData?.content.map((cbwtf) => (
                      <MenuItem key={cbwtf.id} value={cbwtf.id}>
                        {cbwtf.name} ({cbwtf.code})
                      </MenuItem>
                    ))}
                  </Select>
                  {errors.cbwtfId && (
                    <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 2 }}>
                      {errors.cbwtfId}
                    </Typography>
                  )}
                </FormControl>
              </Grid>
            )}
            {showHcfSelector && (
              <Grid size={{ xs: 12, md: 4 }}>
                <FormControl fullWidth>
                  <InputLabel>HCF</InputLabel>
                  <Select
                    value={formData.hcfId || ''}
                    label="HCF"
                    onChange={(e) => updateField('hcfId', e.target.value || undefined)}
                    disabled={!formData.cbwtfId}
                  >
                    <MenuItem value="">Select HCF</MenuItem>
                    {/* TODO: Fetch HCFs based on selected CBWTF */}
                  </Select>
                </FormControl>
              </Grid>
            )}
          </Grid>

          <Alert severity="info" sx={{ mt: 3 }}>
            A temporary password will be generated and the user will be required to change it on first login.
          </Alert>

          {/* Actions */}
          <Stack direction="row" justifyContent="flex-end" spacing={2} sx={{ mt: 4 }}>
            <Button onClick={() => navigate('/superadmin/users')}>
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={mutation.isPending}
              endIcon={<CheckIcon />}
            >
              {mutation.isPending ? 'Creating...' : 'Create User'}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
