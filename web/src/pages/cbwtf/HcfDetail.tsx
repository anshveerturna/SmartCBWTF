import { useState, useRef, useEffect } from 'react';
import dayjs from 'dayjs';
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
  Divider,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  LocationOn as LocationIcon,
  Business as HcfIcon,
  Description as AgreementIcon,
  AttachMoney as BillingIcon,
  Assessment as StatsIcon,
  Edit as EditIcon,
  PictureAsPdf as PdfIcon,
} from '@mui/icons-material';

import apiClient from '../../api/client';
import {
  getHcfDetail,
  updateHcfLocation,
  renewAgreement,
  downloadHcfAgreementPdf,
  downloadHcfAgreementPrintPdf,
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

// Portal Access Card for all HCFs
interface PortalAdminInfo {
  eligible: boolean;
  hasAdmin?: boolean;
  username?: string;
  fullName?: string;
  active?: boolean;
  reason?: string;
}

interface CreateAdminResponse {
  success: boolean;
  username: string;
  tempPassword: string;
  message: string;
}

function PortalAccessCard({ hcfId, isSmallHcf = false }: { hcfId: string; isSmallHcf?: boolean }) {
  const queryClient = useQueryClient();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [createdCredentials, setCreatedCredentials] = useState<{ username: string; password: string } | null>(null);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false, message: '', severity: 'success'
  });

  const { data: adminInfo, isLoading, isError, error } = useQuery({
    queryKey: ['hcf-portal-admin', hcfId],
    queryFn: async () => {
      const res = await apiClient.get<PortalAdminInfo>(`/api/cbwtf/hcfs/${hcfId}/portal-admin`);
      return res.data;
    },
    retry: 1
  });

  // For 30+ beds HCFs - create admin if eligible
  const createAdminMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post<CreateAdminResponse>(`/api/cbwtf/hcfs/${hcfId}/portal-admin/create`);
      return res.data;
    },
    onSuccess: (data) => {
      setCreatedCredentials({ username: data.username, password: data.tempPassword });
      setSnackbar({ open: true, message: 'HCF Admin created successfully!', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['hcf-portal-admin', hcfId] });
    },
    onError: (err: any) => {
      const message = err.response?.data?.message || err.message || 'Failed to create admin';
      setSnackbar({ open: true, message, severity: 'error' });
    }
  });

  // For 0-30 beds HCFs - enable portal access manually
  const enablePortalAccessMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post<CreateAdminResponse>(`/api/cbwtf/hcfs/${hcfId}/enable-portal-access`);
      return res.data;
    },
    onSuccess: (data) => {
      setCreatedCredentials({ username: data.username, password: data.tempPassword });
      setSnackbar({ open: true, message: 'Portal access enabled successfully!', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['hcf-portal-admin', hcfId] });
      queryClient.invalidateQueries({ queryKey: ['cbwtf-hcf', hcfId] });
    },
    onError: (err: any) => {
      const message = err.response?.data?.message || err.message || 'Failed to enable portal access';
      setSnackbar({ open: true, message, severity: 'error' });
    }
  });

  const resetPasswordMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post(`/api/cbwtf/hcfs/${hcfId}/portal-admin/reset-password`, { newPassword });
      return res.data;
    },
    onSuccess: () => {
      setNewPassword('');
      setConfirmPassword('');
      setSnackbar({ open: true, message: 'Password updated successfully', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['hcf-portal-admin', hcfId] });
    },
    onError: (err: any) => {
      const message = err.response?.data?.message || err.message || 'Failed to update password';
      setSnackbar({ open: true, message, severity: 'error' });
    }
  });

  const passwordsMatch = newPassword === confirmPassword && newPassword.length >= 8;

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box display="flex" justifyContent="center" py={2}>
            <CircularProgress size={24} />
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
         <CardContent>
            <Alert severity="error">
              Failed to load Portal Access: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
         </CardContent>
      </Card>
    );
  }

  // For small HCFs (0-30 beds) that are not yet enabled, show the enable button
  if (!adminInfo?.eligible && isSmallHcf) {
    return (
      <Card>
        <CardContent>
          <Box display="flex" alignItems="center" gap={1} mb={2}>
            <Typography variant="h6" fontWeight="bold">
              🔐 Portal Access
            </Typography>
            <Chip label="0-30 Beds" size="small" color="warning" variant="outlined" />
          </Box>

          {createdCredentials && (
            <Alert severity="success" sx={{ mb: 2 }}>
              <Typography variant="subtitle2" fontWeight="bold">Portal Access Enabled!</Typography>
              <Typography variant="body2">Username: <strong>{createdCredentials.username}</strong></Typography>
              <Typography variant="body2">Temp Password: <strong style={{ fontFamily: 'monospace' }}>{createdCredentials.password}</strong></Typography>
              <Typography variant="caption" color="text.secondary">Save this password now - it won't be shown again!</Typography>
            </Alert>
          )}

          <Stack spacing={2}>
            <Alert severity="info">
              This HCF is eligible for manual portal access. Click below to enable.
            </Alert>
            <Button
              variant="contained"
              color="primary"
              onClick={() => enablePortalAccessMutation.mutate()}
              disabled={enablePortalAccessMutation.isPending}
              fullWidth
              startIcon={enablePortalAccessMutation.isPending ? <CircularProgress size={16} color="inherit" /> : null}
            >
              {enablePortalAccessMutation.isPending ? 'Enabling...' : 'Enable Portal Access'}
            </Button>
            <Typography variant="caption" color="text.secondary">
              This will create an HCF admin account with the agreement number as username.
            </Typography>
          </Stack>

          <Snackbar
            open={snackbar.open}
            autoHideDuration={4000}
            onClose={() => setSnackbar({ ...snackbar, open: false })}
          >
            <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
          </Snackbar>
        </CardContent>
      </Card>
    );
  }

  // For 30+ beds that are not eligible (should not happen normally)
  if (!adminInfo?.eligible) {
    return (
       <Card>
         <CardContent>
            <Alert severity="warning">
              HCF is not eligible for portal access.
            </Alert>
         </CardContent>
      </Card>
    ); 
  }

  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <Typography variant="h6" fontWeight="bold">
            🔐 Portal Access
          </Typography>
          <Chip label={isSmallHcf ? "0-30 Beds (Manual)" : "30+ Beds"} size="small" color="primary" variant="outlined" />
        </Box>

        {/* Show newly created credentials */}
        {createdCredentials && (
          <Alert severity="success" sx={{ mb: 2 }}>
            <Typography variant="subtitle2" fontWeight="bold">Admin Created!</Typography>
            <Typography variant="body2">Username: <strong>{createdCredentials.username}</strong></Typography>
            <Typography variant="body2">Temp Password: <strong style={{ fontFamily: 'monospace' }}>{createdCredentials.password}</strong></Typography>
            <Typography variant="caption" color="text.secondary">Save this password now - it won't be shown again!</Typography>
          </Alert>
        )}

        {adminInfo.hasAdmin ? (
          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">Username (Agreement Number)</Typography>
              <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'background.default' }}>
                <Typography fontFamily="monospace" fontWeight={500}>
                  {adminInfo.username}
                </Typography>
              </Paper>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">Reset Password</Typography>
              <Stack spacing={1} sx={{ mt: 0.5 }}>
                <TextField
                  size="small"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="New password (min 8 chars)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  fullWidth
                />
                <TextField
                  size="small"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Confirm password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  fullWidth
                  error={confirmPassword.length > 0 && newPassword !== confirmPassword}
                  helperText={confirmPassword.length > 0 && newPassword !== confirmPassword ? 'Passwords do not match' : ''}
                />
                <Box display="flex" gap={1}>
                  <Button
                    variant="text"
                    size="small"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </Button>
                  <Button
                    variant="contained"
                    size="small"
                    onClick={() => resetPasswordMutation.mutate()}
                    disabled={!passwordsMatch || resetPasswordMutation.isPending}
                  >
                    {resetPasswordMutation.isPending ? 'Updating...' : 'Update Password'}
                  </Button>
                </Box>
              </Stack>
            </Box>
          </Stack>
        ) : (
          <Stack spacing={2}>
            <Alert severity="info">
              No HCF admin user exists for this facility yet.
            </Alert>
            <Button
              variant="contained"
              color="primary"
              onClick={() => createAdminMutation.mutate()}
              disabled={createAdminMutation.isPending}
              fullWidth
            >
              {createAdminMutation.isPending ? 'Creating...' : 'Create HCF Admin'}
            </Button>
            <Typography variant="caption" color="text.secondary">
              Username will be set to the Agreement Number. A temporary password will be generated.
            </Typography>
          </Stack>
        )}

        <Snackbar
          open={snackbar.open}
          autoHideDuration={4000}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
        >
          <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
        </Snackbar>
      </CardContent>
    </Card>
  );
}

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
        <IconButton onClick={() => {
          // Navigate back to the correct HCF list based on bed count
          const isLargeHcf = hcf?.numberOfBeds !== null && hcf?.numberOfBeds !== undefined && hcf.numberOfBeds > 30;
          navigate(isLargeHcf ? '/cbwtf/hcfs/large' : '/cbwtf/hcfs/small');
        }}>
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
        <Grid item xs={12} md={6}>
          <Stack spacing={3}>
            {/* Facility Profile Card */}
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <HcfIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">Facility Profile</Typography>
                  </Box>
                  <Chip 
                    label={hcf.hcfStatus} 
                    color={hcf.hcfStatus === 'ACTIVE' ? 'success' : hcf.hcfStatus === 'PENDING_APPROVAL' ? 'warning' : 'default'} 
                    size="small"
                  />
                </Box>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Facility Name</Typography>
                    <Typography fontWeight={500}>{hcf.name}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Facility Type</Typography>
                    <Box mt={0.5}>
                      <Chip 
                        label={hcf.bedded ? 'Bedded Facility' : 'Non-Bedded Facility'} 
                        size="small" 
                        variant="outlined" 
                      />
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Beds / Seats</Typography>
                    <Typography>{hcf.numberOfBeds || hcf.seatCount || 'Not Provided'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Doctor / Owner Name</Typography>
                    <Typography>{hcf.doctorName || '-'}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Contact & Address Card */}
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <LocationIcon color="primary" />
                  <Typography variant="h6" fontWeight="bold">Contact & Location</Typography>
                </Box>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Contact Phone</Typography>
                    <Typography fontFamily="monospace">{hcf.contactPhone || '-'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Contact Email</Typography>
                    <Typography>{hcf.contactEmail || '-'}</Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Address</Typography>
                    <Typography>{hcf.address || 'Address not provided'}</Typography>
                    <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
                      {[hcf.city, hcf.state].filter(Boolean).join(', ')} {hcf.pincode ? `- ${hcf.pincode}` : ''}
                    </Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Legal & Compliance Card */}
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <AgreementIcon color="primary" />
                  <Typography variant="h6" fontWeight="bold">Legal & Compliance</Typography>
                </Box>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Ownership Type</Typography>
                    <Box mt={0.5}>
                      <Chip
                        label={hcf.ownershipType === 'RENTED' ? 'Rented' : 'Owned'}
                        size="small"
                        color={hcf.ownershipType === 'RENTED' ? 'warning' : 'success'}
                        variant="filled"
                      />
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Rent Agreement</Typography>
                    <Box mt={0.5}>
                      {hcf.rentAgreementUrl ? (
                        <Button variant="outlined" size="small" href={hcf.rentAgreementUrl} target="_blank">
                          View Document
                        </Button>
                      ) : (
                        <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                          Not Uploaded
                        </Typography>
                      )}
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">PAN Number</Typography>
                    <Typography fontFamily="monospace">{hcf.panNo || '-'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">GST Number</Typography>
                    <Typography fontFamily="monospace">{hcf.gstNo || '-'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Aadhar Number</Typography>
                    <Typography fontFamily="monospace">{hcf.aadharNo || '-'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">PCB Authorization</Typography>
                    <Typography fontFamily="monospace">{hcf.pcbAuthorizationNo || '-'}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Notes Section */}
            {hcf.otherNotes && (
              <Card>
                <CardContent>
                   <Typography variant="overline" color="primary.main" fontWeight="bold">Notes</Typography>
                   <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic', whiteSpace: 'pre-wrap', mt: 1 }}>
                     {hcf.otherNotes}
                   </Typography>
                </CardContent>
              </Card>
            )}

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
        <Grid item xs={12} md={6}>
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
                <Divider sx={{ mb: 2 }} />
                
                {hcf.agreement ? (
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Agreement Number</Typography>
                      <Typography fontFamily="monospace" fontWeight="bold">
                        {hcf.agreement.agreementNumber}
                      </Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Status</Typography>
                      <Box mt={0.5}>
                        <Chip
                          label={hcf.agreement.status}
                          color={hcf.agreement.status === 'ACTIVE' ? 'success' : 'warning'}
                          size="small"
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Dues Status</Typography>
                      <Box mt={0.5}>
                        <Chip
                          label={hcf.agreement.duesStatus}
                          color={hcf.agreement.duesStatus === 'CLEAR' ? 'success' : 'warning'}
                          size="small"
                          variant="outlined"
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Period</Typography>
                      <Typography>
                        {formatDate(hcf.agreement.startDate)} - {formatDate(hcf.agreement.endDate)}
                      </Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Rate per Bed/Day</Typography>
                      <Typography>{formatCurrency(hcf.agreement.perBedPerDayRate)}</Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                       <Typography variant="caption" color="text.secondary">Monthly Charges</Typography>
                       <Typography fontWeight={500}>{formatCurrency(hcf.monthlyCharges)}</Typography>
                    </Grid>
                    {hcf?.occupancy != null && hcf.occupancy > 0 && (
                      <Grid item xs={12} sm={6}>
                        <Typography variant="caption" color="text.secondary">Occupancy Rate</Typography>
                        <Typography fontWeight={500}>{hcf.occupancy}%</Typography>
                      </Grid>
                    )}
                    {hcf.monthlyCharges && hcf.agreement?.startDate && hcf.agreement?.endDate && (
                      <Grid item xs={12} sm={6}>
                        <Typography variant="caption" color="text.secondary">Total Agreement Charge</Typography>
                        <Typography fontWeight={700} color="primary.main">
                          {(() => {
                            const months = dayjs(hcf.agreement.endDate).diff(dayjs(hcf.agreement.startDate), 'month', true);
                            if (months <= 0) return '-';
                            const occFactor = (hcf.occupancy && hcf.occupancy > 0) ? (hcf.occupancy / 100) : 1;
                            const discountedMonthly = hcf.monthlyCharges * occFactor;
                            const subtotal = discountedMonthly * months;
                            const gst = subtotal * 0.05; // 5% GST
                            const total = subtotal + gst;
                            return `${formatCurrency(total)} (Incl. 5% GST)`;
                          })()}
                        </Typography>
                      </Grid>
                    )}
                    
                    <Grid item xs={12} display="flex" gap={2} mt={1}>
                      <Button
                        variant="outlined"
                        size="small"
                        startIcon={<PdfIcon />}
                        onClick={async () => {
                          try {
                            const blob = await downloadHcfAgreementPdf(id!);
                            const url = window.URL.createObjectURL(blob);
                            const link = document.createElement('a');
                            link.href = url;
                            const safeNum = (hcf.agreement?.agreementNumber || 'document').replace(/\//g, '_');
                            link.setAttribute('download', `Agreement_${safeNum}.pdf`);
                            document.body.appendChild(link);
                            link.click();
                            link.remove();
                            window.URL.revokeObjectURL(url);
                          } catch {
                            setSnackbar({ open: true, message: 'Failed to download agreement PDF. It may still be generating.', severity: 'error' });
                          }
                        }}
                      >
                        Download PDF
                      </Button>
                      <Button
                        variant="outlined"
                        size="small"
                        color="secondary"
                        startIcon={<PdfIcon />}
                        onClick={async () => {
                          try {
                            const blob = await downloadHcfAgreementPrintPdf(id!);
                            const url = window.URL.createObjectURL(blob);
                            const link = document.createElement('a');
                            link.href = url;
                            const safeNum = (hcf.agreement?.agreementNumber || 'document').replace(/\//g, '_');
                            link.setAttribute('download', `Agreement_Print_${safeNum}.pdf`);
                            document.body.appendChild(link);
                            link.click();
                            link.remove();
                            window.URL.revokeObjectURL(url);
                          } catch {
                            setSnackbar({ open: true, message: 'Failed to download print agreement PDF.', severity: 'error' });
                          }
                        }}
                      >
                        Print PDF
                      </Button>
                    </Grid>
                  </Grid>
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
                        {hcf.billingConfig.globalExcessRatePerKg != null
                          ? formatCurrency(hcf.billingConfig.globalExcessRatePerKg) + ' per kg'
                          : '-'}
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

            {/* Portal Access Card - Shown for all approved HCFs */}
            {hcf.agreement?.status === 'ACTIVE' && (
              <PortalAccessCard 
                hcfId={id!} 
                isSmallHcf={!hcf.numberOfBeds || hcf.numberOfBeds <= 30} 
              />
            )}
           </Stack>
        </Grid>

        {/* Operational Summary (Bottom) */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <StatsIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">
                  Operational Summary
                </Typography>
              </Box>
              <Grid container spacing={3}>
                <Grid item xs={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalPickups || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Pickups
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalAttendanceMarks || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Attendance Marks
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Paper sx={{ p: 2, textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {hcf.summary?.totalWasteKg?.toFixed(1) || '0'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Waste (kg)
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} md={3}>
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
             <Grid item xs={6}>
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
             <Grid item xs={6}>
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
            {hcf?.billingModel === 'BEDDED' ? (
              <TextField
                label="Bed Rate (₹ per bed/day)"
                type="number"
                value={renewForm.perBedPerDayRate}
                onChange={(e) => setRenewForm({ ...renewForm, perBedPerDayRate: parseFloat(e.target.value) })}
                fullWidth
                inputProps={{ step: '0.50', min: '0' }}
              />
            ) : (
              <TextField
                label="Monthly Charge (₹)"
                type="number"
                value={renewForm.monthlyCharges}
                onChange={(e) => setRenewForm({ ...renewForm, monthlyCharges: parseFloat(e.target.value) })}
                fullWidth
                inputProps={{ step: '100', min: '0' }}
              />
            )}

            {/* Bill Amount Calculator for Fixed Monthly */}
            {hcf?.billingModel === 'FIXED_MONTHLY' && renewForm.startDate && renewForm.endDate && (
              (() => {
                const months = dayjs(renewForm.endDate).diff(dayjs(renewForm.startDate), 'month', true);
                if (months <= 0) return null;
                const occFactor = (hcf.occupancy && hcf.occupancy > 0) ? (hcf.occupancy / 100) : 1;
                const discountedMonthly = (renewForm.monthlyCharges || 0) * occFactor;
                const subtotal = discountedMonthly * months;
                const gst = subtotal * 0.05;
                const total = subtotal + gst;

                return (
                  <Paper elevation={0} sx={{ p: 2, bgcolor: 'primary.50', border: '1px solid', borderColor: 'primary.200', borderRadius: 2 }}>
                      <Typography variant="subtitle2" color="primary.900" gutterBottom>
                        Estimated Bill Calculator
                      </Typography>
                      <Stack spacing={1}>
                        <Box display="flex" justifyContent="space-between">
                          <Typography variant="body2" color="text.secondary">Base Monthly Charge:</Typography>
                          <Typography variant="body2">₹{(renewForm.monthlyCharges || 0).toFixed(2)}</Typography>
                        </Box>
                        {hcf?.occupancy != null && hcf.occupancy > 0 && (
                          <Box display="flex" justifyContent="space-between">
                            <Typography variant="body2" color="text.secondary">After Occupancy ({hcf.occupancy}%):</Typography>
                            <Typography variant="body2">₹{discountedMonthly.toFixed(2)}/mo</Typography>
                          </Box>
                        )}
                        <Box display="flex" justifyContent="space-between">
                          <Typography variant="body2" color="text.secondary">Duration:</Typography>
                          <Typography variant="body2">{months.toFixed(1)} Months</Typography>
                        </Box>
                        <Box display="flex" justifyContent="space-between">
                          <Typography variant="body2" color="text.secondary">Subtotal:</Typography>
                          <Typography variant="body2">₹{subtotal.toFixed(2)}</Typography>
                        </Box>
                        <Box display="flex" justifyContent="space-between">
                          <Typography variant="body2" color="text.secondary">GST (5%):</Typography>
                          <Typography variant="body2">₹{gst.toFixed(2)}</Typography>
                        </Box>
                        <Box display="flex" justifyContent="space-between" mt={1} pt={1} borderTop="1px dashed" borderColor="primary.300">
                          <Typography variant="subtitle2" color="primary.900">Total Estimated Bill:</Typography>
                          <Typography variant="subtitle2" color="primary.900" fontWeight="bold">₹{total.toFixed(2)}</Typography>
                        </Box>
                      </Stack>
                  </Paper>
                );
              })()
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenewDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => renewalMutation.mutate(renewForm)}
            disabled={renewalMutation.isPending || !renewForm.startDate || !renewForm.endDate || (hcf?.billingModel === 'BEDDED' && !renewForm.perBedPerDayRate) || (hcf?.billingModel === 'FIXED_MONTHLY' && !renewForm.monthlyCharges)}
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
