import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
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
  TextField,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  LocationOn as LocationIcon,
  Business as HcfIcon,
  Description as AgreementIcon,
  AttachMoney as BillingIcon,
  Assessment as StatsIcon,
  Edit as EditIcon,
} from '@mui/icons-material';

import {
  getHcfDetail,
  updateHcfLocation,
  renewAgreement,
  type UpdateLocationRequest,
  type RenewAgreementRequest,
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
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Location editing state
  const [locationDialogOpen, setLocationDialogOpen] = useState(false);
  const [tempLocation, setTempLocation] = useState<{ lat: number; lng: number } | null>(null);

  // Renewal state
  const [renewDialogOpen, setRenewDialogOpen] = useState(false);
  const [renewForm, setRenewForm] = useState<RenewAgreementRequest>({
    startDate: '',
    endDate: '',
    perBedPerDayRate: 0,
  });

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

  // Renewal mutation
  const renewalMutation = useMutation({
    mutationFn: (data: RenewAgreementRequest) => renewAgreement(id!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', id] });
      setRenewDialogOpen(false);
      setSnackbar({ open: true, message: 'Agreement renewed successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to renew agreement', severity: 'error' });
    },
  });


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

      </Box>

      <Grid container spacing={3}>
        {/* Left Column: Profile & Location */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Stack spacing={3}>
            {/* Profile Card - Read Only */}
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <HcfIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      HCF Profile
                    </Typography>
                  </Box>
                  <Chip 
                    label={hcf.hcfStatus} 
                    color={hcf.hcfStatus === 'ACTIVE' ? 'success' : hcf.hcfStatus === 'PENDING_APPROVAL' ? 'warning' : 'default'} 
                    size="small"
                  />
                </Box>
                
                <Stack spacing={2.5}>
                  {/* Basic Info */}
                  <Box>
                    <Typography variant="overline" color="primary.main" fontWeight="bold">
                      Basic Information
                    </Typography>
                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                      <Grid size={{ xs: 12 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Name</Typography>
                          <Typography fontWeight={500}>{hcf.name}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 12 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Address</Typography>
                          <Typography>{hcf.address}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">State</Typography>
                          <Typography>{hcf.state || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Pincode</Typography>
                          <Typography fontFamily="monospace">{hcf.pincode || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Doctor/Owner Name</Typography>
                          <Typography>{hcf.doctorName || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Contact Phone</Typography>
                          <Typography fontFamily="monospace">{hcf.contactPhone || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Email</Typography>
                          <Typography>{hcf.contactEmail || '-'}</Typography>
                        </Box>
                      </Grid>
                    </Grid>
                  </Box>

                  {/* Government IDs */}
                  <Box>
                    <Typography variant="overline" color="primary.main" fontWeight="bold">
                      Government IDs
                    </Typography>
                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">PAN Number</Typography>
                          <Typography fontFamily="monospace">{hcf.panNo || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">GST Number</Typography>
                          <Typography fontFamily="monospace">{hcf.gstNo || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Aadhar Number</Typography>
                          <Typography fontFamily="monospace">{hcf.aadharNo || '-'}</Typography>
                        </Box>
                      </Grid>

                    </Grid>
                  </Box>

                  {/* Facility Details */}
                  <Box>
                    <Typography variant="overline" color="primary.main" fontWeight="bold">
                      Facility Details
                    </Typography>
                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Ownership Type</Typography>
                          <Box>
                            <Chip
                              label={hcf.ownershipType === 'RENTED' ? 'Rented' : 'Owned'}
                              size="small"
                              color={hcf.ownershipType === 'RENTED' ? 'warning' : 'success'}
                              variant="filled"
                            />
                          </Box>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        {(hcf.ownershipType === 'RENTED' || hcf.rentAgreementUrl) && (
                          <Box>
                            <Typography variant="caption" color="text.secondary">
                              Rent Agreement {hcf.ownershipType === 'RENTED' ? '(Mandatory)' : ''}
                            </Typography>
                            <Box>
                              {hcf.rentAgreementUrl ? (
                                <Button
                                  variant="outlined"
                                  size="small"
                                  href={hcf.rentAgreementUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  startIcon={<AgreementIcon />}
                                  sx={{ mt: 0.5 }}
                                >
                                  View Document
                                </Button>
                              ) : (
                                <Typography variant="body2" color="error" sx={{ fontStyle: 'italic', mt: 0.5 }}>
                                  Not Uploaded
                                </Typography>
                              )}
                            </Box>
                          </Box>
                        )}
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Facility Type</Typography>
                          <Box>
                            <Chip 
                              label={hcf.bedded ? 'Bedded Facility' : 'Non-Bedded Facility'} 
                              size="small" 
                              color="default" 
                              variant="outlined" 
                            />
                          </Box>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Number of Beds</Typography>
                          <Typography>{hcf.numberOfBeds || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Monthly Charges</Typography>
                          <Typography fontWeight={500}>{formatCurrency(hcf.monthlyCharges)}</Typography>
                        </Box>
                      </Grid>
                    </Grid>
                  </Box>

                  {/* GPS Coordinates */}
                  <Box>
                    <Typography variant="overline" color="primary.main" fontWeight="bold">
                      GPS Location
                    </Typography>
                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Latitude</Typography>
                          <Typography fontFamily="monospace">{hcf.registrationGpsLat || hcf.gpsLat || '-'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Longitude</Typography>
                          <Typography fontFamily="monospace">{hcf.registrationGpsLon || hcf.gpsLon || '-'}</Typography>
                        </Box>
                      </Grid>
                      {hcf.registrationGpsAccuracy && (
                        <Grid size={{ xs: 6 }}>
                          <Box>
                            <Typography variant="caption" color="text.secondary">Accuracy</Typography>
                            <Typography fontFamily="monospace">{hcf.registrationGpsAccuracy.toFixed(2)}m</Typography>
                          </Box>
                        </Grid>
                      )}
                    </Grid>
                  </Box>

                  {/* Notes */}
                  {hcf.otherNotes && (
                    <Box>
                      <Typography variant="overline" color="primary.main" fontWeight="bold">
                        Notes
                      </Typography>
                      <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5, bgcolor: 'background.default' }}>
                        <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic', whiteSpace: 'pre-wrap' }}>
                          {hcf.otherNotes}
                        </Typography>
                      </Paper>
                    </Box>
                  )}

                  {/* Registration Info */}
                  <Box sx={{ pt: 1, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Grid container spacing={2}>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Registered By</Typography>
                          <Typography variant="body2">{hcf.registeredByUsername || 'System'}</Typography>
                        </Box>
                      </Grid>
                      <Grid size={{ xs: 6 }}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">Created At</Typography>
                          <Typography variant="body2">
                            {hcf.createdAt ? new Date(hcf.createdAt).toLocaleDateString('en-IN', {
                              day: '2-digit',
                              month: 'short',
                              year: 'numeric'
                            }) : '-'}
                          </Typography>
                        </Box>
                      </Grid>
                    </Grid>
                  </Box>
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
            {/* Agreement Card */}
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <AgreementIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Agreement Details
                    </Typography>
                  </Box>
                  {hcf.agreement?.status === 'EXPIRED' && hcf.agreement?.duesStatus === 'CLEAR' && (
                    <Button
                      variant="contained"
                      size="small"
                      onClick={() => {
                        // Default to starting tomorrow for 1 year
                        const tomorrow = new Date();
                        tomorrow.setDate(tomorrow.getDate() + 1);
                        const nextYear = new Date(tomorrow);
                        nextYear.setFullYear(nextYear.getFullYear() + 1);
                        setRenewForm({
                          startDate: tomorrow.toISOString().split('T')[0],
                          endDate: nextYear.toISOString().split('T')[0],
                          perBedPerDayRate: hcf.agreement?.perBedPerDayRate || 15.50,
                        });
                        setRenewDialogOpen(true);
                      }}
                    >
                      Renew Agreement
                    </Button>
                  )}

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

            {/* Billing Information Card (READ-ONLY) */}
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <BillingIcon color="primary" />
                  <Typography variant="h6" fontWeight="bold">
                    Billing Information
                  </Typography>
                </Box>
                {hcf.billingConfig ? (
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Base Allowance</Typography>
                      <Typography>{hcf.billingConfig.baseGramsPerBedPerDay}g per bed/day</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Bed Rate (Contractual)</Typography>
                      <Typography>{formatCurrency(hcf.billingConfig.baseRatePerBedPerDay)} per bed/day</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Excess Rate (Global)</Typography>
                      <Typography>
                        {hcf.billingConfig.globalExcessRatePerKg 
                          ? formatCurrency(hcf.billingConfig.globalExcessRatePerKg) + ' per kg'
                          : '₹50.00 per kg'}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Effective From</Typography>
                      <Typography>{formatDate(hcf.billingConfig.effectiveFrom)}</Typography>
                    </Box>
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

      {/* Renewal Dialog */}
      <Dialog open={renewDialogOpen} onClose={() => setRenewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Renew Agreement</DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 2 }}>
            <Alert severity="info">
              This will create a NEW agreement with a NEW agreement number. The old agreement will remain EXPIRED.
            </Alert>
            <TextField
              label="Start Date"
              type="date"
              value={renewForm.startDate}
              onChange={(e) => setRenewForm({ ...renewForm, startDate: e.target.value })}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="End Date"
              type="date"
              value={renewForm.endDate}
              onChange={(e) => setRenewForm({ ...renewForm, endDate: e.target.value })}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Bed Rate (₹ per bed/day)"
              type="number"
              value={renewForm.perBedPerDayRate}
              onChange={(e) => setRenewForm({ ...renewForm, perBedPerDayRate: parseFloat(e.target.value) })}
              fullWidth
              inputProps={{ step: '0.50', min: '0' }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenewDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => renewalMutation.mutate(renewForm)}
            disabled={renewalMutation.isPending || !renewForm.startDate || !renewForm.endDate || !renewForm.perBedPerDayRate}
          >
            {renewalMutation.isPending ? 'Renewing...' : 'Create New Agreement'}
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
