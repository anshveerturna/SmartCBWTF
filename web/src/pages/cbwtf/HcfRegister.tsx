import { useState, useEffect, useRef, useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Grid,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Switch,
  IconButton,
  Stack,
  Alert,
  Snackbar,
  Paper,
  InputAdornment,
  CircularProgress,
  LinearProgress,
  Select,
  MenuItem,
  InputLabel,
  Autocomplete,
  Chip,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Business as HcfIcon,
  LocationOn as LocationIcon,
  CloudUpload as UploadIcon,
  CheckCircle as CheckIcon,
} from '@mui/icons-material';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { State, City } from 'country-state-city';
import { registerHcf, uploadRentAgreement, getFacilitySettings, type CbwtfAdminHcfRegistrationRequest } from '../../api/cbwtf';

// Custom location marker icon
const createLocationIcon = () => {
  const svg = `
    <svg viewBox="0 0 24 24" width="32" height="32" xmlns="http://www.w3.org/2000/svg">
      <path fill="#EA4335" stroke="#fff" stroke-width="1" d="M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zm0,9.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z"/>
    </svg>
  `;
  return L.divIcon({
    html: svg,
    className: 'location-marker',
    iconSize: [32, 32],
    iconAnchor: [16, 32],
  });
};

const locationIcon = createLocationIcon();

// Map click handler component
function LocationPicker({
  position,
  onLocationSelect,
}: {
  position: { lat: number; lng: number } | null;
  onLocationSelect: (pos: { lat: number; lng: number }) => void;
}) {
  const map = useMap();

  useEffect(() => {
    if (position) {
      map.setView(position, map.getZoom());
    }
  }, [position, map]);

  useMapEvents({
    click(e) {
      onLocationSelect(e.latlng);
    },
  });

  return position ? (
    <Marker
      position={position}
      icon={locationIcon}
      draggable
      eventHandlers={{
        dragend: (e) => {
          const marker = e.target;
          const pos = marker.getLatLng();
          onLocationSelect(pos);
        },
      }}
    />
  ) : null;
}

export default function HcfRegister() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Fetch facility settings for agreement defaults
  const { data: facilitySettings } = useQuery({
    queryKey: ['facility-settings'],
    queryFn: getFacilitySettings,
    staleTime: 5 * 60 * 1000,
  });

  // Compute default end date from settings
  const defaultValidityMonths = facilitySettings?.agreementRules?.defaultAgreementValidityMonths || 12;
  const computeEndDate = (startDateStr: string, months: number) => {
    const d = new Date(startDateStr);
    d.setMonth(d.getMonth() + months);
    return d.toISOString().split('T')[0];
  };

  // Form state
  const [form, setForm] = useState({
    name: '',
    address: '',
    pincode: '',
    state: '',
    stateCode: '',
    city: '',
    doctorName: '',
    contactPhone: '',
    contactEmail: '',
    panNo: '',
    gstNo: '',
    aadharNo: '',
    ownershipType: 'OWNED',
    rentAgreementUrl: '',
    bedded: false,
    numberOfBeds: '',
    monthlyCharges: '',
    occupancy: '',
    otherNotes: '',
    agreementStartDate: new Date().toISOString().split('T')[0],
    agreementEndDate: '',
    perBedPerDayRate: '',
    excessRatePerKg: '',
    taxRate: '5',
    // New HCF category fields
    hcfType: 'HOSPITAL',
    seatCount: '',
    // Custom agreement number
    customAgreementNumber: '',
  });

  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);

  // States and Cities data
  const states = useMemo(() => State.getStatesOfCountry('IN'), []);
  const cities = useMemo(() => {
    if (!form.stateCode) return [];
    return City.getCitiesOfState('IN', form.stateCode);
  }, [form.stateCode]);

  // Auto-compute agreement end date from settings
  useEffect(() => {
    if (form.agreementStartDate && (!form.agreementEndDate || form.agreementEndDate === '')) {
      setForm(prev => ({
        ...prev,
        agreementEndDate: computeEndDate(prev.agreementStartDate, defaultValidityMonths),
      }));
    }
  }, [defaultValidityMonths, form.agreementStartDate]);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // File upload state
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [rentAgreementFile, setRentAgreementFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [isUploading, setIsUploading] = useState(false);

  // Initialize map to India center
  useEffect(() => {
    if (!location) {
      setLocation({ lat: 20.5937, lng: 78.9629 });
    }
  }, [location]);

  // Mutation
  const mutation = useMutation({
    mutationFn: (data: CbwtfAdminHcfRegistrationRequest) => registerHcf(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcfs'] });
      setSnackbar({ open: true, message: 'HCF registered successfully!', severity: 'success' });
      setTimeout(() => navigate(`/cbwtf/hcfs/${data.id}`), 1500);
    },
    onError: (error: Error) => {
      setSnackbar({ open: true, message: error.message || 'Failed to register HCF', severity: 'error' });
    },
  });

  const handleInputChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [field]: e.target.value });
  };

  const handleSubmit = () => {
    if (!location) {
      setSnackbar({ open: true, message: 'Please select a location on the map', severity: 'error' });
      return;
    }

    const request: CbwtfAdminHcfRegistrationRequest = {
      name: form.name,
      address: form.address,
      pincode: form.pincode,
      state: form.state,
      doctorName: form.doctorName,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail,
      panNo: form.panNo || undefined,
      gstNo: form.gstNo || undefined,
      aadharNo: form.aadharNo || undefined,
      ownershipType: form.ownershipType,
      rentAgreementUrl: form.ownershipType === 'RENTED' ? form.rentAgreementUrl : undefined,
      bedded: form.bedded,
      numberOfBeds: form.bedded ? parseInt(form.numberOfBeds) || undefined : undefined,
      monthlyCharges: form.monthlyCharges ? parseFloat(form.monthlyCharges) : undefined,
      occupancy: form.occupancy ? parseFloat(form.occupancy) : undefined,
      otherNotes: form.otherNotes || undefined,
      gpsLat: location.lat,
      gpsLon: location.lng,
      agreementStartDate: form.agreementStartDate,
      agreementEndDate: form.agreementEndDate,
      perBedPerDayRate: form.perBedPerDayRate ? parseFloat(form.perBedPerDayRate) : undefined,
      excessRatePerKg: form.excessRatePerKg ? parseFloat(form.excessRatePerKg) : undefined,
      taxRate: form.taxRate ? parseFloat(form.taxRate) : 5,
      // New HCF category fields
      hcfType: form.hcfType as 'HOSPITAL' | 'DENTAL' | 'CLINIC' | 'PATHOLOGY_COLLECTION' | 'PATHOLOGY_STORAGE',
      city: form.city || undefined,
      seatCount: (form.hcfType === 'DENTAL' || form.hcfType === 'CLINIC') && form.seatCount 
        ? parseInt(form.seatCount) : undefined,
      // Custom agreement number
      customAgreementNumber: form.customAgreementNumber.trim() || undefined,
    };

    mutation.mutate(request);
  };

  const labelFixSx = {
    '& .MuiInputLabel-root': {
      bgcolor: 'background.paper',
      px: 0.5,
    },
  };

  const isFormValid = () => {
    return (
      form.name.trim() &&
      form.address.trim() &&
      form.pincode.match(/^\d{6}$/) &&
      form.state.trim() &&
      form.doctorName.trim() &&
      form.contactPhone.trim() &&
      form.contactEmail.trim() &&
      // PAN, GST, Aadhar are now optional - only validate format if provided
      (!form.panNo || form.panNo.match(/^[A-Z]{5}[0-9]{4}[A-Z]$/)) &&
      (!form.aadharNo || form.aadharNo.match(/^\d{12}$/)) &&
      (form.ownershipType !== 'RENTED' || form.rentAgreementUrl.trim()) &&
      (!form.bedded || (form.numberOfBeds && parseInt(form.numberOfBeds) > 0)) &&
      location &&
      form.agreementStartDate &&
      form.agreementEndDate
    );
  };

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <IconButton onClick={() => navigate('/cbwtf/hcfs')}>
          <BackIcon />
        </IconButton>
        <HcfIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Typography variant="h4" fontWeight="bold">
          Register New HCF
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Left Column: Form */}
        <Grid item xs={12} md={7}>
          <Stack spacing={3}>
            {/* HCF Information */}
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" color="primary" mb={2}>
                  HCF Information
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      sx={labelFixSx}
                      label="HCF Name *"
                      fullWidth
                      value={form.name}
                      onChange={handleInputChange('name')}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      sx={labelFixSx}
                      label="Address *"
                      fullWidth
                      multiline
                      rows={2}
                      value={form.address}
                      onChange={handleInputChange('address')}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      sx={labelFixSx}
                      label="Pincode *"
                      fullWidth
                      value={form.pincode}
                      onChange={handleInputChange('pincode')}
                      inputProps={{ maxLength: 6 }}
                      error={form.pincode.length > 0 && !form.pincode.match(/^\d{6}$/)}
                      helperText={form.pincode.length > 0 && !form.pincode.match(/^\d{6}$/) ? '6 digits required' : ''}
                    />
                  </Grid>
                  <Grid item xs={6}>
                     <Autocomplete
                      options={states}
                      getOptionLabel={(option) => option.name}
                      value={states.find((s) => s.name === form.state) || null}
                      onChange={(_, newValue) => {
                        setForm({
                          ...form,
                          state: newValue ? newValue.name : '',
                          stateCode: newValue ? newValue.isoCode : '',
                          city: '', // Reset city when state changes
                        });
                      }}
                      componentsProps={{
                        popper: {
                          modifiers: [
                            {
                              name: 'flip',
                              enabled: false,
                            },
                            {
                              name: 'preventOverflow',
                              options: {
                                boundary: 'window',
                              },
                            },
                          ],
                        },
                      }}
                      ListboxProps={{ style: { maxHeight: 250 } }}
                      renderInput={(params) => (
                        <TextField 
                          {...params} 
                          label="State"
                          required 
                          fullWidth 
                          sx={labelFixSx}
                        />
                      )}
                      isOptionEqualToValue={(option, value) => option.name === value.name}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <Autocomplete
                      options={cities}
                      getOptionLabel={(option) => option.name}
                      disabled={!form.stateCode}
                      value={cities.find(c => c.name === form.city) || null}
                      onChange={(_, newValue) => {
                        setForm({
                          ...form,
                          city: newValue ? newValue.name : '',
                        });
                      }}
                       componentsProps={{
                        popper: {
                          modifiers: [
                            {
                              name: 'flip',
                              enabled: false,
                            },
                            {
                              name: 'preventOverflow',
                              options: {
                                boundary: 'window',
                              },
                            },
                          ],
                        },
                      }}
                      ListboxProps={{ style: { maxHeight: 250 } }}
                      renderInput={(params) => (
                        <TextField 
                          {...params} 
                          label="City" 
                          fullWidth 
                          sx={labelFixSx}
                        />
                      )}
                      isOptionEqualToValue={(option, value) => option.name === value.name}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <FormControl fullWidth>
                      <InputLabel>HCF Type *</InputLabel>
                      <Select
                        value={form.hcfType}
                        label="HCF Type *"
                        onChange={(e) => setForm({ ...form, hcfType: e.target.value })}
                      >
                        <MenuItem value="HOSPITAL">Hospital</MenuItem>
                        <MenuItem value="DENTAL">Dental</MenuItem>
                        <MenuItem value="CLINIC">Clinic</MenuItem>
                        <MenuItem value="PATHOLOGY_COLLECTION">Pathology Lab (Collection)</MenuItem>
                        <MenuItem value="PATHOLOGY_STORAGE">Pathology Lab (Storage)</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      sx={labelFixSx}
                      label="Doctor/Owner Name *"
                      fullWidth
                      value={form.doctorName}
                      onChange={handleInputChange('doctorName')}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      sx={labelFixSx}
                      label="Contact Phone *"
                      fullWidth
                      value={form.contactPhone}
                      onChange={handleInputChange('contactPhone')}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      sx={labelFixSx}
                      label="Email *"
                      fullWidth
                      type="email"
                      value={form.contactEmail}
                      onChange={handleInputChange('contactEmail')}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Government IDs (Optional) */}
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" color="primary" mb={2}>
                  Government IDs (Optional)
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="PAN Number"
                      fullWidth
                      value={form.panNo}
                      onChange={(e) => setForm({ ...form, panNo: e.target.value.toUpperCase() })}
                      inputProps={{ maxLength: 10 }}
                      error={form.panNo.length > 0 && !form.panNo.match(/^[A-Z]{5}[0-9]{4}[A-Z]$/)}
                      helperText={form.panNo.length > 0 && !form.panNo.match(/^[A-Z]{5}[0-9]{4}[A-Z]$/) ? 'Invalid PAN format' : ''}
                    />
                  </Grid>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="GST Number"
                      fullWidth
                      value={form.gstNo}
                      onChange={(e) => setForm({ ...form, gstNo: e.target.value.toUpperCase() })}
                      inputProps={{ maxLength: 15 }}
                    />
                  </Grid>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="Aadhar Number"
                      fullWidth
                      value={form.aadharNo}
                      onChange={handleInputChange('aadharNo')}
                      inputProps={{ maxLength: 12 }}
                      error={form.aadharNo.length > 0 && !form.aadharNo.match(/^\d{12}$/)}
                      helperText={form.aadharNo.length > 0 && !form.aadharNo.match(/^\d{12}$/) ? 'Must be 12 digits' : ''}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Property & Facility */}
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" color="primary" mb={2}>
                  Property & Facility
                </Typography>
                <Stack spacing={2}>
                  <FormControl>
                    <FormLabel>Ownership Type *</FormLabel>
                    <RadioGroup
                      row
                      value={form.ownershipType}
                      onChange={(e) => setForm({ ...form, ownershipType: e.target.value })}
                    >
                      <FormControlLabel value="OWNED" control={<Radio />} label="Own Property" />
                      <FormControlLabel value="RENTED" control={<Radio />} label="Rented" />
                    </RadioGroup>
                  </FormControl>

                  {form.ownershipType === 'RENTED' && (
                    <Box>
                      <Typography variant="subtitle2" mb={1}>
                        Rent Agreement * (PDF or Image, max 20MB)
                      </Typography>
                      <input
                        type="file"
                        ref={fileInputRef}
                        accept=".pdf,image/*"
                        style={{ display: 'none' }}
                        onChange={async (e) => {
                          const file = e.target.files?.[0];
                          if (!file) return;
                          
                          // Validate size (20MB)
                          if (file.size > 20 * 1024 * 1024) {
                            setSnackbar({ open: true, message: 'File size cannot exceed 20MB', severity: 'error' });
                            return;
                          }
                          
                          setRentAgreementFile(file);
                          setIsUploading(true);
                          setUploadProgress(0);
                          
                          try {
                            // Simulate progress
                            const progressInterval = setInterval(() => {
                              setUploadProgress((p) => Math.min(p + 10, 90));
                            }, 100);
                            
                            const result = await uploadRentAgreement(file);
                            clearInterval(progressInterval);
                            setUploadProgress(100);
                            setForm({ ...form, rentAgreementUrl: result.url });
                            setSnackbar({ open: true, message: 'Rent agreement uploaded!', severity: 'success' });
                          } catch (err) {
                            setSnackbar({ open: true, message: 'Failed to upload file', severity: 'error' });
                            setRentAgreementFile(null);
                          } finally {
                            setIsUploading(false);
                          }
                        }}
                      />
                      
                      {!rentAgreementFile && !form.rentAgreementUrl ? (
                        <Paper
                          variant="outlined"
                          sx={{
                            p: 3,
                            textAlign: 'center',
                            cursor: 'pointer',
                            bgcolor: 'action.hover',
                            '&:hover': { bgcolor: 'action.selected' },
                          }}
                          onClick={() => fileInputRef.current?.click()}
                        >
                          <UploadIcon sx={{ fontSize: 40, color: 'text.secondary', mb: 1 }} />
                          <Typography variant="body2" color="text.secondary">
                            Click to upload rent agreement
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            PDF, JPEG, PNG (max 20MB)
                          </Typography>
                        </Paper>
                      ) : (
                        <Paper
                          variant="outlined"
                          sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}
                        >
                          {isUploading ? (
                            <Box sx={{ flex: 1 }}>
                              <Typography variant="body2" mb={1}>
                                Uploading {rentAgreementFile?.name}...
                              </Typography>
                              <LinearProgress variant="determinate" value={uploadProgress} />
                            </Box>
                          ) : (
                            <>
                              <CheckIcon color="success" />
                              <Box sx={{ flex: 1 }}>
                              <Typography variant="body2" fontWeight="medium">
                                  {rentAgreementFile?.name || 'Rent Agreement'}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {rentAgreementFile ? `${(rentAgreementFile.size / 1024 / 1024).toFixed(2)} MB` : 'Uploaded'}
                                </Typography>
                              </Box>
                              <Button
                                size="small"
                                color="error"
                                onClick={() => {
                                  setRentAgreementFile(null);
                                  setForm({ ...form, rentAgreementUrl: '' });
                                }}
                              >
                                Remove
                              </Button>
                            </>
                          )}
                        </Paper>
                      )}
                    </Box>
                  )}

                  <FormControlLabel
                    control={
                      <Switch
                        checked={form.bedded}
                        onChange={(e) => setForm({ ...form, bedded: e.target.checked })}
                      />
                    }
                    label={form.hcfType === 'HOSPITAL' ? 'Bedded Facility' : 'Seated Facility'}
                  />

                  {form.bedded && (
                    <TextField
                      label={form.hcfType === 'HOSPITAL' ? 'Number of Beds *' : 'Number of Seats *'}
                      type="number"
                      value={form.numberOfBeds}
                      onChange={handleInputChange('numberOfBeds')}
                      sx={{ width: 200, ...labelFixSx }}
                    />
                  )}

                  <Stack direction="row" spacing={2} alignItems="flex-start">
                    <TextField
                      label="Monthly Charges"
                      type="number"
                      value={form.monthlyCharges}
                      onChange={handleInputChange('monthlyCharges')}
                      InputProps={{
                        startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                      }}
                      sx={{ width: 200, ...labelFixSx }}
                    />
                    <TextField
                      label="Tax Rate (GST %)"
                      type="number"
                      value={form.taxRate}
                      onChange={handleInputChange('taxRate')}
                      InputProps={{
                        endAdornment: <InputAdornment position="end">%</InputAdornment>,
                      }}
                      helperText="Default 5%"
                      sx={{ width: 150, ...labelFixSx }}
                    />
                    <TextField
                      label="Occupancy (%)"
                      type="number"
                      value={form.occupancy}
                      onChange={handleInputChange('occupancy')}
                      InputProps={{
                        endAdornment: <InputAdornment position="end">%</InputAdornment>,
                      }}
                      helperText="Discount percentage"
                      sx={{ width: 150, ...labelFixSx }}
                    />
                  </Stack>
                </Stack>
              </CardContent>
            </Card>

            {/* Agreement Period */}
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" color="primary" mb={2}>
                  Agreement Period
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="body2" color="text.secondary">
                        Quick Select Validity:
                      </Typography>
                      <Chip
                        label="3 Months"
                        onClick={() => {
                          const endDate = computeEndDate(form.agreementStartDate, 3);
                          setForm({ ...form, agreementEndDate: endDate });
                        }}
                        color="primary"
                        variant="outlined"
                        clickable
                      />
                      <Chip
                        label="6 Months"
                        onClick={() => {
                          const endDate = computeEndDate(form.agreementStartDate, 6);
                          setForm({ ...form, agreementEndDate: endDate });
                        }}
                        color="primary"
                        variant="outlined"
                        clickable
                      />
                      <Chip
                        label="1 Year"
                        onClick={() => {
                          const endDate = computeEndDate(form.agreementStartDate, 12);
                          setForm({ ...form, agreementEndDate: endDate });
                        }}
                        color="primary"
                        variant="outlined"
                        clickable
                      />
                    </Stack>
                  </Grid>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="Start Date *"
                      type="date"
                      fullWidth
                      value={form.agreementStartDate}
                      onChange={handleInputChange('agreementStartDate')}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="End Date *"
                      type="date"
                      fullWidth
                      value={form.agreementEndDate}
                      onChange={handleInputChange('agreementEndDate')}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={4}>
                    <TextField
                      sx={labelFixSx}
                      label="Excess Rate per Kg"
                      type="number"
                      fullWidth
                      value={form.excessRatePerKg}
                      onChange={handleInputChange('excessRatePerKg')}
                      InputProps={{
                        startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                      }}
                      helperText="Charged per kg for waste exceeding 277g/bed/day"
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      sx={labelFixSx}
                      label="Custom Agreement Number (Optional)"
                      fullWidth
                      value={form.customAgreementNumber}
                      onChange={handleInputChange('customAgreementNumber')}
                      helperText="Leave blank to auto-generate based on your format settings. Enter a custom number to override."
                      placeholder="e.g., CUSTOM-2026-001"
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Notes */}
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" color="primary" mb={2}>
                  Additional Notes
                </Typography>
                <TextField
                  sx={labelFixSx}
                  label="Notes (optional)"
                  fullWidth
                  multiline
                  rows={3}
                  value={form.otherNotes}
                  onChange={handleInputChange('otherNotes')}
                />
              </CardContent>
            </Card>
          </Stack>
        </Grid>

        {/* Right Column: Map */}
        <Grid item xs={12} md={5}>
          <Card sx={{ position: 'sticky', top: 24 }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <LocationIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">
                  Set HCF Location *
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Click on the map to set the HCF location, or drag the marker to adjust.
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  height: 400,
                  overflow: 'hidden',
                  borderRadius: 2,
                }}
              >
                <MapContainer
                  center={[20.5937, 78.9629]}
                  zoom={5}
                  style={{ height: '100%', width: '100%' }}
                >
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  <LocationPicker position={location} onLocationSelect={setLocation} />
                </MapContainer>
              </Paper>
              {location && (
                <Paper variant="outlined" sx={{ p: 2, mt: 2, bgcolor: 'background.default' }}>
                  <Typography variant="caption" color="text.secondary" mb={1.5} display="block">
                    Coordinates (drag marker or edit below)
                  </Typography>
                  <Stack direction="row" spacing={2}>
                    <TextField
                      sx={labelFixSx}
                      label="Latitude"
                      fullWidth
                      type="number"
                      value={parseFloat(location.lat.toFixed(6))}
                      onChange={(e) => {
                        const lat = parseFloat(e.target.value);
                        if (!isNaN(lat) && lat >= -90 && lat <= 90) {
                          setLocation({ ...location, lat });
                        }
                      }}
                      inputProps={{ step: 0.0001, min: -90, max: 90 }}
                      InputProps={{
                        sx: { fontFamily: 'monospace' }
                      }}
                    />
                    <TextField
                      sx={labelFixSx}
                      label="Longitude"
                      fullWidth
                      type="number"
                      value={parseFloat(location.lng.toFixed(6))}
                      onChange={(e) => {
                        const lng = parseFloat(e.target.value);
                        if (!isNaN(lng) && lng >= -180 && lng <= 180) {
                          setLocation({ ...location, lng });
                        }
                      }}
                      inputProps={{ step: 0.0001, min: -180, max: 180 }}
                      InputProps={{
                        sx: { fontFamily: 'monospace' }
                      }}
                    />
                  </Stack>
                </Paper>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Action Buttons */}
      <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
        <Button variant="outlined" onClick={() => navigate('/cbwtf/hcfs')}>
          Cancel
        </Button>
        <Button
          variant="contained"
          size="large"
          onClick={handleSubmit}
          disabled={!isFormValid() || mutation.isPending}
          startIcon={mutation.isPending && <CircularProgress size={20} color="inherit" />}
        >
          {mutation.isPending ? 'Registering...' : 'Register HCF'}
        </Button>
      </Box>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
