import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  CircularProgress,
  Alert,
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Paper,
} from '@mui/material';
import {
  LocalShipping,
  GpsFixed,
  GpsOff,
  ArrowBack,
  Refresh,
} from '@mui/icons-material';
import { getLiveMap, type LiveMapDTO, type LivePositionDTO } from '../../api/cbwtf';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LocationAddress } from '../../components/LocationAddress';

// Custom truck SVG icon creator
const createTruckIcon = (color: string) => {
  const svg = `
    <svg viewBox="0 0 24 24" width="32" height="32" xmlns="http://www.w3.org/2000/svg">
      <path fill="${color}" stroke="#fff" stroke-width="1" d="M20,8H17V4H3C1.9,4,1,4.9,1,6V17H3C3,18.7,4.3,20,6,20S9,18.7,9,17H15C15,18.7,16.3,20,18,20S21,18.7,21,17H23V12L20,8M6,18.5C5.2,18.5,4.5,17.8,4.5,17S5.2,15.5,6,15.5S7.5,16.2,7.5,17S6.8,18.5,6,18.5M18,18.5C17.2,18.5,16.5,17.8,16.5,17S17.2,15.5,18,15.5S19.5,16.2,19.5,17S18.8,18.5,18,18.5M19.5,12H17V9.5H18.5L19.5,12Z"/>
    </svg>
  `;
  
  return L.divIcon({
    html: svg,
    className: 'truck-marker',
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });
};

// Create online (green) and offline (red) truck icons
const onlineTruckIcon = createTruckIcon('#22C55E'); // Green
const offlineTruckIcon = createTruckIcon('#EF4444'); // Red

const AUTO_REFRESH_INTERVAL = 10000; // 10 seconds

// Default center (India)
const DEFAULT_CENTER: [number, number] = [20.5937, 78.9629];
const DEFAULT_ZOOM = 5;

// Component to fit map bounds to markers
function FitBounds({ vehicles }: { vehicles: LivePositionDTO[] }) {
  const map = useMap();
  
  useEffect(() => {
    const validVehicles = vehicles.filter(v => v.latitude && v.longitude);
    if (validVehicles.length > 0) {
      const bounds = L.latLngBounds(
        validVehicles.map(v => [v.latitude!, v.longitude!] as [number, number])
      );
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
    }
  }, [vehicles, map]);
  
  return null;
}

const VehicleLiveMap = () => {
  const navigate = useNavigate();
  const [selectedVehicle, setSelectedVehicle] = useState<LivePositionDTO | null>(null);

  const {
    data: liveMap,
    isLoading,
    error,
    refetch,
  } = useQuery<LiveMapDTO>({
    queryKey: ['cbwtf-live-map'],
    queryFn: getLiveMap,
    refetchInterval: AUTO_REFRESH_INTERVAL,
  });

  const formatTimeAgo = (dateString: string | null): string => {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    return `${Math.floor(diffMins / 60)}h ago`;
  };

  // Auto-update selection when data refreshes
  useEffect(() => {
    if (selectedVehicle && liveMap?.vehicles) {
      const updated = liveMap.vehicles.find(v => v.id === selectedVehicle.id);
      if (updated) {
        setSelectedVehicle(updated);
      }
    }
  }, [liveMap, selectedVehicle?.id]);

  // Get vehicles with valid coordinates
  const vehiclesWithLocation = liveMap?.vehicles.filter(v => v.latitude && v.longitude) || [];

  return (
    <Box sx={{ p: 3, height: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button
            startIcon={<ArrowBack />}
            onClick={() => navigate('/cbwtf/vehicles')}
          >
            Back
          </Button>
          <Typography variant="h5" sx={{ fontWeight: 600 }}>
            Live Vehicle Map
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip 
            icon={<GpsFixed />}
            label={`${liveMap?.onlineCount ?? 0} Online`}
            color="success"
          />
          <Chip 
            icon={<GpsOff />}
            label={`${(liveMap?.totalCount ?? 0) - (liveMap?.onlineCount ?? 0)} Offline`}
            color="default"
          />
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => refetch()}
            size="small"
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load live map data. Auto-retrying...
        </Alert>
      )}

      {isLoading && !liveMap && (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Main Content */}
      {liveMap && (
        <Box sx={{ display: 'flex', gap: 2, flex: 1, minHeight: 0 }}>
          {/* Vehicle List Sidebar */}
          <Card sx={{ width: 320, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <CardContent sx={{ pb: 1 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Vehicles ({liveMap.totalCount})
              </Typography>
            </CardContent>
            <Divider />
            <List sx={{ overflow: 'auto', flex: 1 }}>
              {liveMap.vehicles.map((vehicle) => (
                <ListItem
                  key={vehicle.id}
                  component="div"
                  onClick={() => setSelectedVehicle(vehicle)}
                  sx={{
                    cursor: 'pointer',
                    bgcolor: selectedVehicle?.id === vehicle.id ? 'action.selected' : 'transparent',
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                >
                  <ListItemIcon>
                    {vehicle.gpsStatus === 'ONLINE' ? (
                      <GpsFixed color="success" />
                    ) : (
                      <GpsOff color="disabled" />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={vehicle.registrationNumber}
                    secondary={
                      <>
                        {vehicle.vehicleType} • {formatTimeAgo(vehicle.lastGpsAt)}
                      </>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </Card>

          {/* Map Area */}
          <Paper 
            sx={{ 
              flex: 1, 
              display: 'flex', 
              flexDirection: 'column',
              overflow: 'hidden',
              borderRadius: 2,
            }}
          >
            {/* OpenStreetMap with Leaflet */}
            <Box sx={{ flex: 1, position: 'relative' }}>
              <MapContainer
                center={DEFAULT_CENTER}
                zoom={DEFAULT_ZOOM}
                style={{ height: '100%', width: '100%' }}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                
                {/* Fit map to vehicle bounds */}
                {vehiclesWithLocation.length > 0 && (
                  <FitBounds vehicles={vehiclesWithLocation} />
                )}
                
                {/* Vehicle markers */}
                {vehiclesWithLocation.map((vehicle) => (
                  <Marker
                    key={vehicle.id}
                    position={[vehicle.latitude!, vehicle.longitude!]}
                    icon={vehicle.gpsStatus === 'ONLINE' ? onlineTruckIcon : offlineTruckIcon}
                    eventHandlers={{
                      click: () => setSelectedVehicle(vehicle),
                    }}
                  >
                    <Popup>
                      <Box sx={{ minWidth: 150 }}>
                        <Typography variant="subtitle2" fontWeight={600}>
                          {vehicle.registrationNumber}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {vehicle.vehicleType}
                        </Typography>
                        {vehicle.driverName && (
                          <Typography variant="body2">
                            Driver: {vehicle.driverName}
                          </Typography>
                        )}
                        <Typography variant="caption" color="text.secondary">
                          Last update: {formatTimeAgo(vehicle.lastGpsAt)}
                        </Typography>
                        <Box sx={{ mt: 0.5 }}>
                          <LocationAddress 
                            latitude={vehicle.latitude} 
                            longitude={vehicle.longitude}
                            showCoords
                          />
                        </Box>
                      </Box>
                    </Popup>
                  </Marker>
                ))}
              </MapContainer>

              {/* No location data overlay */}
              {vehiclesWithLocation.length === 0 && (
                <Box
                  sx={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    textAlign: 'center',
                    bgcolor: 'background.paper',
                    p: 3,
                    borderRadius: 2,
                    boxShadow: 3,
                    zIndex: 1000,
                  }}
                >
                  <LocalShipping sx={{ fontSize: 48, color: 'grey.400', mb: 1 }} />
                  <Typography variant="h6" color="text.secondary">
                    No Location Data
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Vehicles will appear on the map once they report GPS data.
                  </Typography>
                </Box>
              )}
            </Box>

            {/* Footer with timestamp */}
            <Box sx={{ p: 1, bgcolor: 'background.paper', borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="caption" color="text.secondary">
                Last updated: {new Date(liveMap.timestamp).toLocaleTimeString()} • 
                Auto-refresh every {AUTO_REFRESH_INTERVAL / 1000}s • 
                {vehiclesWithLocation.length} of {liveMap.totalCount} vehicles have GPS data
              </Typography>
            </Box>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default VehicleLiveMap;

