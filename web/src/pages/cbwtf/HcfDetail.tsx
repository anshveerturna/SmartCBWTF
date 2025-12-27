import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  TextField,
  Button,
  Chip,
  Stack,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Paper,
  Snackbar,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  LocationOn as LocationIcon,
  Business as HcfIcon,
  Warning as WarningIcon,
  Description as AgreementIcon,
  AttachMoney as BillingIcon,
  Assessment as StatsIcon,
} from '@mui/icons-material';
import {
  getHcfDetail,
  updateHcf,
  updateHcfBillingConfig,
  deactivateHcf,
  updateHcfLocation,
  type UpdateHcfRequest,
  type BillingConfigRequest,
  type UpdateLocationRequest,
} from '../../api/cbwtf';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Custom location marker icon
const createLocationIcon = () => {
  const svg = `
    <svg viewBox="0 0 24 24" width="32" height="32" xmlns="http://www.w3.org/2000/svg">
      <path fill="#EA4335" stroke="#fff" stroke-width="1" d="M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -12,-7zm0,9.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z"/>
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

// Map interactions component
function LocationPicker({ 
  position, 
  onLocationSelect, 
  isEditing 
}: { 
  position: { lat: number; lng: number } | null;
  onLocationSelect: (pos: { lat: number; lng: number }) => void;
  isEditing: boolean;
}) {
  const map = useMap();
  const mapMovedByUser = useRef(false);

  // Update map view when position changes externally (only if not dragging to avoid jitter)
  useEffect(() => {
    if (position && !mapMovedByUser.current) {
      map.setView(position, map.getZoom());
    }
    mapMovedByUser.current = false; // Reset after potential external update
  }, [position, map]);
  
  useMapEvents({
    click(e) {
      if (isEditing) {
        onLocationSelect(e.latlng);
      }
    },
    dragstart: () => {
      mapMovedByUser.current = true;
    },
    zoomstart: () => {
      mapMovedByUser.current = true;
    },
  });

  return position ? (
    <Marker
      position={position}
      icon={locationIcon}
      draggable={isEditing}
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

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

const formatCurrency = (amount: number | null | undefined) => {
  if (amount === null || amount === undefined) return '-';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
  }).format(amount);
};

export default function HcfDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // State
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<UpdateHcfRequest>({});
  const [billingForm, setBillingForm] = useState<BillingConfigRequest>({
    baseRatePerBedPerDay: 0,
    excessRatePerKg: 0,
  });
  const [isBillingEditing, setIsBillingEditing] = useState(false);
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivateReason, setDeactivateReason] = useState('');
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Location editing state
  const [locationDialogOpen, setLocationDialogOpen] = useState(false);
  const [tempLocation, setTempLocation] = useState<{ lat: number; lng: number } | null>(null);

  // Queries
  const { data: hcf, isLoading, error } = useQuery({
    queryKey: ['cbwtf-hcf', id],
    queryFn: () => getHcfDetail(id!),
    enabled: !!id,
  });

  // Mutations
  const locationUpdateMutation = useMutation({
    mutationFn: (data: UpdateLocationRequest) => updateHcfLocation(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', id] });
      setLocationDialogOpen(false);
      setSnackbar({ open: true, message: 'Location updated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to update location', severity: 'error' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateHcfRequest) => updateHcf(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', id] });
      setIsEditing(false);
      setSnackbar({ open: true, message: 'Profile updated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to update profile', severity: 'error' });
    },
  });

  const billingMutation = useMutation({
    mutationFn: (data: BillingConfigRequest) => updateHcfBillingConfig(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', id] });
      setIsBillingEditing(false);
      setSnackbar({ open: true, message: 'Billing config updated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to update billing config', severity: 'error' });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: () => deactivateHcf(id!, { reason: deactivateReason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', id] });
      setDeactivateOpen(false);
      setSnackbar({ open: true, message: 'HCF deactivated successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to deactivate HCF', severity: 'error' });
    },
  });

  // Handlers
  const startEditing = () => {
    if (hcf) {
      setEditForm({
        name: hcf.name,
        contactEmail: hcf.contactEmail || '',
        contactPhone: hcf.contactPhone || '',
        address: hcf.address,
        numberOfBeds: hcf.numberOfBeds || undefined,
        doctorName: hcf.doctorName || '',
      });
      setIsEditing(true);
    }
  };

  const startBillingEditing = () => {
    if (hcf?.billingConfig) {
      setBillingForm({
        baseGramsPerBedPerDay: hcf.billingConfig.baseGramsPerBedPerDay,
        baseRatePerBedPerDay: hcf.billingConfig.baseRatePerBedPerDay,
        excessRatePerKg: hcf.billingConfig.excessRatePerKg,
      });
      setIsBillingEditing(true);
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error || !hcf) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load HCF details. Please try again later.
      </Alert>
    );
  }

  const isActive = hcf.agreement?.status === 'ACTIVE';

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <IconButton onClick={() => navigate('/cbwtf/hcfs')}>
          <BackIcon />
        </IconButton>
        <HcfIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Box flex={1}>
          <Typography variant="h4" fontWeight="bold">
            {hcf.name}
          </Typography>
          <Typography variant="body2" color="text.secondary" fontFamily="monospace">
            {hcf.code}
          </Typography>
        </Box>
        {isActive && (
          <Button
            variant="outlined"
            color="error"
            startIcon={<WarningIcon />}
            onClick={() => setDeactivateOpen(true)}
          >
            Deactivate HCF
          </Button>
        )}
      </Box>

      <Grid container spacing={3}>
        {/* Left Column: Profile & Location */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Stack spacing={3}>
            {/* Profile Card */}
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6" fontWeight="bold">
                    HCF Profile
                  </Typography>
                  {!isEditing ? (
                    <IconButton onClick={startEditing} disabled={!isActive}>
                      <EditIcon />
                    </IconButton>
                  ) : (
                    <Stack direction="row" spacing={1}>
                      <IconButton
                        color="primary"
                        onClick={() => updateMutation.mutate(editForm)}
                        disabled={updateMutation.isPending}
                      >
                        <SaveIcon />
                      </IconButton>
                      <IconButton onClick={() => setIsEditing(false)}>
                        <CancelIcon />
                      </IconButton>
                    </Stack>
                  )}
                </Box>
                <Stack spacing={2}>
                  {isEditing ? (
                    <>
                      <TextField
                        label="Name"
                        value={editForm.name || ''}
                        onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                        fullWidth
                        size="small"
                      />
                      <TextField
                        label="Email"
                        value={editForm.contactEmail || ''}
                        onChange={(e) => setEditForm({ ...editForm, contactEmail: e.target.value })}
                        fullWidth
                        size="small"
                      />
                      <TextField
                        label="Phone"
                        value={editForm.contactPhone || ''}
                        onChange={(e) => setEditForm({ ...editForm, contactPhone: e.target.value })}
                        fullWidth
                        size="small"
                      />
                      <TextField
                        label="Address"
                        value={editForm.address || ''}
                        onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
                        fullWidth
                        multiline
                        rows={2}
                        size="small"
                      />
                      <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <TextField
                            label="Number of Beds"
                            type="number"
                            value={editForm.numberOfBeds || ''}
                            onChange={(e) => setEditForm({ ...editForm, numberOfBeds: parseInt(e.target.value) || undefined })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                           <TextField
                            label="Monthly Charges (₹)"
                            type="number"
                            value={editForm.monthlyCharges || ''}
                            onChange={(e) => setEditForm({ ...editForm, monthlyCharges: parseFloat(e.target.value) || undefined })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                      </Grid>
                      <TextField
                        label="Doctor Name"
                        value={editForm.doctorName || ''}
                        onChange={(e) => setEditForm({ ...editForm, doctorName: e.target.value })}
                        fullWidth
                        size="small"
                      />
                      <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <TextField
                            label="GST No"
                            value={editForm.gstNo || ''}
                            onChange={(e) => setEditForm({ ...editForm, gstNo: e.target.value })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                          <TextField
                            label="PAN No"
                            value={editForm.panNo || ''}
                            onChange={(e) => setEditForm({ ...editForm, panNo: e.target.value })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                      </Grid>
                       <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <TextField
                            label="Aadhar No"
                            value={editForm.aadharNo || ''}
                            onChange={(e) => setEditForm({ ...editForm, aadharNo: e.target.value })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                          <TextField
                            label="PCB Auth No"
                            value={editForm.pcbAuthorizationNo || ''}
                            onChange={(e) => setEditForm({ ...editForm, pcbAuthorizationNo: e.target.value })}
                            fullWidth
                            size="small"
                          />
                        </Grid>
                      </Grid>
                      <TextField
                        label="Other Notes"
                        value={editForm.otherNotes || ''}
                        onChange={(e) => setEditForm({ ...editForm, otherNotes: e.target.value })}
                        fullWidth
                        multiline
                        rows={2}
                        size="small"
                      />
                    </>
                  ) : (
                    <>
                      <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                           <Box>
                            <Typography variant="caption" color="text.secondary">Email</Typography>
                            <Typography>{hcf.contactEmail || '-'}</Typography>
                          </Box>
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                           <Box>
                            <Typography variant="caption" color="text.secondary">Phone</Typography>
                            <Typography>{hcf.contactPhone || '-'}</Typography>
                          </Box>
                        </Grid>
                      </Grid>
                      <Box>
                        <Typography variant="caption" color="text.secondary">Address</Typography>
                        <Typography>{hcf.address}</Typography>
                      </Box>
                      <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <Box>
                            <Typography variant="caption" color="text.secondary">Beds</Typography>
                            <Typography>
                              {hcf.numberOfBeds || '-'} 
                              {hcf.bedded !== null && (
                                <Chip 
                                  label={hcf.bedded ? "Bedded" : "Non-Bedded"} 
                                  size="small" 
                                  color="default" 
                                  variant="outlined" 
                                  sx={{ ml: 1, height: 20, fontSize: '0.7rem' }} 
                                />
                              )}
                            </Typography>
                          </Box>
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                          <Box>
                            <Typography variant="caption" color="text.secondary">Monthly Charges</Typography>
                            <Typography>{formatCurrency(hcf.monthlyCharges)}</Typography>
                          </Box>
                        </Grid>
                      </Grid>
                      <Box>
                        <Typography variant="caption" color="text.secondary">Doctor Name</Typography>
                        <Typography>{hcf.doctorName || '-'}</Typography>
                      </Box>
                       <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <Box>
                             <Typography variant="caption" color="text.secondary">GST No</Typography>
                             <Typography fontFamily="monospace">{hcf.gstNo || '-'}</Typography>
                          </Box>
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                           <Box>
                             <Typography variant="caption" color="text.secondary">PAN No</Typography>
                             <Typography fontFamily="monospace">{hcf.panNo || '-'}</Typography>
                          </Box>
                        </Grid>
                      </Grid>
                      <Grid container spacing={2}>
                        <Grid size={{ xs: 6 }}>
                          <Box>
                             <Typography variant="caption" color="text.secondary">Aadhar No</Typography>
                             <Typography fontFamily="monospace">{hcf.aadharNo || '-'}</Typography>
                          </Box>
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                           <Box>
                             <Typography variant="caption" color="text.secondary">PCB Auth No</Typography>
                             <Typography fontFamily="monospace">{hcf.pcbAuthorizationNo || '-'}</Typography>
                          </Box>
                        </Grid>
                      </Grid>
                       <Box>
                        <Typography variant="caption" color="text.secondary">Notes</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                          {hcf.otherNotes || 'No notes available'}
                        </Typography>
                      </Box>
                    </>
                  )}
                </Stack>
              </CardContent>
            </Card>

            {/* Location Card */}
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <LocationIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Registered Location
                    </Typography>
                  </Box>
                  <Button
                    size="small"
                    startIcon={<EditIcon />}
                    onClick={() => {
                       if (hcf.gpsLat && hcf.gpsLon) {
                         setTempLocation({ lat: hcf.gpsLat, lng: hcf.gpsLon });
                       } else {
                         // Default to center of India or some reasonable default
                         setTempLocation({ lat: 20.5937, lng: 78.9629 });
                       }
                       setLocationDialogOpen(true);
                    }}
                    disabled={!isActive} // Only allow setting location if active
                  >
                    Set Location
                  </Button>
                </Box>
                
                <Stack spacing={2} sx={{ flex: 1 }}>
                  {hcf.gpsLat && hcf.gpsLon ? (
                    <Paper 
                      variant="outlined" 
                      sx={{ 
                        overflow: 'hidden', 
                        borderRadius: 2,
                      }}
                    >
                      <Box
                        component="iframe"
                        src={`https://www.openstreetmap.org/export/embed.html?bbox=${hcf.gpsLon - 0.01}%2C${hcf.gpsLat - 0.01}%2C${hcf.gpsLon + 0.01}%2C${hcf.gpsLat + 0.01}&layer=mapnik&marker=${hcf.gpsLat}%2C${hcf.gpsLon}`}
                        sx={{
                          border: 0,
                          width: '100%',
                          height: 180,
                          display: 'block',
                        }}
                        loading="lazy"
                        title="HCF Location Map"
                      />
                      <Box sx={{ p: 1, bgcolor: 'background.paper' }}>
                        <Typography variant="caption" color="text.secondary">
                          Coordinates
                        </Typography>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                          {hcf.gpsLat.toFixed(6)}, {hcf.gpsLon.toFixed(6)}
                        </Typography>
                      </Box>
                    </Paper>
                  ) : (
                    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
                      <LocationIcon sx={{ fontSize: 32, color: 'text.disabled', mb: 1 }} />
                      <Typography variant="body2" color="text.secondary">
                        No location data available
                      </Typography>
                    </Paper>
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Stack>
        </Grid>

        {/* Right Column: Agreement, Registration, Billing */}
        <Grid size={{ xs: 12, md: 6 }}>
           <Stack spacing={3}>
            {/* Agreement Card (Read-Only) */}
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <AgreementIcon color="primary" />
                  <Typography variant="h6" fontWeight="bold">
                    Agreement Details
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                    (Read-Only)
                  </Typography>
                </Box>
                {hcf.agreement ? (
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Agreement Number</Typography>
                      <Typography fontFamily="monospace" fontWeight="bold">
                        {hcf.agreement.agreementNumber}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Status</Typography>
                      <Box>
                        <Chip
                          label={hcf.agreement.status}
                          color={hcf.agreement.status === 'ACTIVE' ? 'success' : 'warning'}
                          size="small"
                        />
                      </Box>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Dues Status</Typography>
                      <Box>
                        <Chip
                          label={hcf.agreement.duesStatus}
                          color={hcf.agreement.duesStatus === 'CLEAR' ? 'success' : 'warning'}
                          size="small"
                          variant="outlined"
                        />
                      </Box>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Period</Typography>
                      <Typography>
                        {formatDate(hcf.agreement.startDate)} - {formatDate(hcf.agreement.endDate)}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Rate per Bed/Day</Typography>
                      <Typography>{formatCurrency(hcf.agreement.perBedPerDayRate)}</Typography>
                    </Box>
                  </Stack>
                ) : (
                  <Typography color="text.secondary">No active agreement</Typography>
                )}
              </CardContent>
            </Card>

             {/* Registration Info Card */}
             <Card>
              <CardContent>
                 <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <Typography variant="h6" fontWeight="bold">
                    Registration Info
                  </Typography>
                </Box>
                <Stack spacing={2}>
                   <Box>
                    <Typography variant="caption" color="text.secondary">Registered By</Typography>
                    <Typography>{hcf.registeredByUsername || 'Unknown'}</Typography>
                  </Box>
                   <Box>
                    <Typography variant="caption" color="text.secondary">Registration Date</Typography>
                    <Typography>{formatDate(hcf.createdAt)}</Typography>
                  </Box>
                   <Box>
                    <Typography variant="caption" color="text.secondary">Registration GPS</Typography>
                     {hcf.registrationGpsLat && hcf.registrationGpsLon ? (
                        <>
                           <Typography fontFamily="monospace">
                            {hcf.registrationGpsLat.toFixed(6)}, {hcf.registrationGpsLon.toFixed(6)}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            Accuracy: {hcf.registrationGpsAccuracy ? `${hcf.registrationGpsAccuracy.toFixed(1)}m` : '-'}
                          </Typography>
                          <Button
                            variant="text"
                            size="small"
                            onClick={() => window.open(`https://www.google.com/maps?q=${hcf.registrationGpsLat},${hcf.registrationGpsLon}`, '_blank')}
                            sx={{ p: 0, justifyContent: 'flex-start', mt: 0.5 }}
                          >
                            View location
                          </Button>
                        </>
                      ) : (
                        <Typography color="text.secondary">-</Typography>
                      )}
                  </Box>
                </Stack>
              </CardContent>
             </Card>

            {/* Billing Config Card */}
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <BillingIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Billing Configuration
                    </Typography>
                  </Box>
                  {!isBillingEditing && hcf.billingConfig ? (
                    <IconButton onClick={startBillingEditing} disabled={!isActive}>
                      <EditIcon />
                    </IconButton>
                  ) : isBillingEditing ? (
                    <Stack direction="row" spacing={1}>
                      <IconButton
                        color="primary"
                        onClick={() => billingMutation.mutate(billingForm)}
                        disabled={billingMutation.isPending}
                      >
                        <SaveIcon />
                      </IconButton>
                      <IconButton onClick={() => setIsBillingEditing(false)}>
                        <CancelIcon />
                      </IconButton>
                    </Stack>
                  ) : null}
                </Box>
                {hcf.billingConfig ? (
                  <Stack spacing={2}>
                    {isBillingEditing ? (
                      <>
                        <TextField
                          label="Base Allowance (grams/bed/day)"
                          type="number"
                          value={billingForm.baseGramsPerBedPerDay || 270}
                          onChange={(e) => setBillingForm({ ...billingForm, baseGramsPerBedPerDay: parseInt(e.target.value) })}
                          fullWidth
                          size="small"
                        />
                        <TextField
                          label="Base Rate (₹/bed/day)"
                          type="number"
                          value={billingForm.baseRatePerBedPerDay}
                          onChange={(e) => setBillingForm({ ...billingForm, baseRatePerBedPerDay: parseFloat(e.target.value) })}
                          fullWidth
                          size="small"
                        />
                        <TextField
                          label="Excess Rate (₹/kg)"
                          type="number"
                          value={billingForm.excessRatePerKg}
                          onChange={(e) => setBillingForm({ ...billingForm, excessRatePerKg: parseFloat(e.target.value) })}
                          fullWidth
                          size="small"
                        />
                      </>
                    ) : (
                      <>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Base Allowance</Typography>
                          <Typography>{hcf.billingConfig.baseGramsPerBedPerDay}g per bed/day</Typography>
                        </Box>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Base Rate</Typography>
                          <Typography>{formatCurrency(hcf.billingConfig.baseRatePerBedPerDay)} per bed/day</Typography>
                        </Box>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Excess Waste Rate</Typography>
                          <Typography>{formatCurrency(hcf.billingConfig.excessRatePerKg)} per kg</Typography>
                        </Box>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Effective From</Typography>
                          <Typography>{formatDate(hcf.billingConfig.effectiveFrom)}</Typography>
                        </Box>
                      </>
                    )}
                  </Stack>
                ) : (
                  <Typography color="text.secondary">No billing configuration</Typography>
                )}
              </CardContent>
            </Card>
           </Stack>
        </Grid>

        {/* Operational Summary (Bottom) */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <StatsIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">
                  Operational Summary
                </Typography>
              </Box>
              <Grid container spacing={3}>
                <Grid size={{ xs: 6, md: 3 }}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalPickups || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Pickups
                    </Typography>
                  </Paper>
                </Grid>
                <Grid size={{ xs: 6, md: 3 }}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalAttendanceMarks || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Attendance Marks
                    </Typography>
                  </Paper>
                </Grid>
                <Grid size={{ xs: 6, md: 3 }}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalWasteKg?.toFixed(1) || '0'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Waste (kg)
                    </Typography>
                  </Paper>
                </Grid>
                <Grid size={{ xs: 6, md: 3 }}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="body1" color="primary">
                      {hcf.summary?.lastPickupAt ? formatDate(hcf.summary.lastPickupAt) : '-'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Last Pickup
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Location Update Dialog */}
      <Dialog open={locationDialogOpen} onClose={() => setLocationDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Set HCF Location</DialogTitle>
        <DialogContent>
           <Grid container spacing={2} sx={{ mb: 2, mt: 1 }}>
             <Grid size={{ xs: 6 }}>
               <TextField
                 label="Latitude"
                 type="number"
                 fullWidth
                 value={tempLocation?.lat || ''}
                 onChange={(e) => {
                   const lat = parseFloat(e.target.value);
                   if (!isNaN(lat) && tempLocation) {
                     setTempLocation({ ...tempLocation, lat });
                   }
                 }}
               />
             </Grid>
             <Grid size={{ xs: 6 }}>
               <TextField
                 label="Longitude"
                 type="number"
                 fullWidth
                 value={tempLocation?.lng || ''}
                 onChange={(e) => {
                   const lng = parseFloat(e.target.value);
                   if (!isNaN(lng) && tempLocation) {
                     setTempLocation({ ...tempLocation, lng });
                   }
                 }}
               />
             </Grid>
           </Grid>
           
           <Box 
              sx={{ 
                height: 400, 
                width: '100%', 
                borderRadius: 2,
                overflow: 'hidden',
                border: '1px solid',
                borderColor: 'divider',
                position: 'relative'
              }}
            >
              <Alert 
                 severity="info" 
                 sx={{ 
                   position: 'absolute', 
                   top: 10, 
                   left: '50%', 
                   transform: 'translateX(-50%)', 
                   zIndex: 1000,
                   opacity: 0.9,
                   width: 'auto'
                 }}
               >
                 Click map or drag marker to set location
               </Alert>
               {locationDialogOpen && tempLocation && (
                 <MapContainer 
                   center={[tempLocation.lat, tempLocation.lng]} 
                   zoom={13} 
                   style={{ height: '100%', width: '100%' }}
                 >
                   <TileLayer
                     attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                     url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                   />
                   <LocationPicker 
                     position={tempLocation}
                     isEditing={true}
                     onLocationSelect={setTempLocation}
                   />
                 </MapContainer>
               )}
            </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLocationDialogOpen(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={() => {
              if (tempLocation) {
                locationUpdateMutation.mutate({
                  latitude: tempLocation.lat,
                  longitude: tempLocation.lng
                });
              }
            }}
            disabled={locationUpdateMutation.isPending}
          >
             {locationUpdateMutation.isPending ? 'Saving...' : 'Set Location'}
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Deactivate Dialog */}
      <Dialog open={deactivateOpen} onClose={() => setDeactivateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ color: 'error.main' }}>
          <Box display="flex" alignItems="center" gap={1}>
            <WarningIcon />
            Deactivate HCF
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            This will expire the agreement for <strong>{hcf.name}</strong>. 
            The HCF will no longer be able to use the system for attendance or pickups.
          </Typography>
          <TextField
            label="Reason for deactivation"
            value={deactivateReason}
            onChange={(e) => setDeactivateReason(e.target.value)}
            fullWidth
            multiline
            rows={3}
            required
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeactivateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => deactivateMutation.mutate()}
            disabled={!deactivateReason.trim() || deactivateMutation.isPending}
          >
            {deactivateMutation.isPending ? 'Deactivating...' : 'Deactivate'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
