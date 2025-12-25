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

const AUTO_REFRESH_INTERVAL = 10000; // 10 seconds

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

  // Auto-clear selection when data updates
  useEffect(() => {
    if (selectedVehicle && liveMap?.vehicles) {
      const updated = liveMap.vehicles.find(v => v.id === selectedVehicle.id);
      if (updated) {
        setSelectedVehicle(updated);
      }
    }
  }, [liveMap, selectedVehicle?.id]);

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
              bgcolor: 'grey.100',
            }}
          >
            {/* Map Placeholder - In production, integrate with Leaflet/Google Maps */}
            <Box 
              sx={{ 
                flex: 1, 
                display: 'flex', 
                flexDirection: 'column',
                alignItems: 'center', 
                justifyContent: 'center',
                p: 4,
                textAlign: 'center',
              }}
            >
              <LocalShipping sx={{ fontSize: 64, color: 'grey.400', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                Map Integration Ready
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 400, mb: 3 }}>
                This area will display an interactive map with vehicle positions.
                Integrate with Leaflet, Google Maps, or Mapbox based on your preference.
              </Typography>

              {/* Show selected vehicle details */}
              {selectedVehicle && (
                <Card sx={{ mt: 2, minWidth: 300 }}>
                  <CardContent>
                    <Typography variant="subtitle1" fontWeight={600}>
                      {selectedVehicle.registrationNumber}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {selectedVehicle.vehicleType} • {selectedVehicle.driverName || 'No driver'}
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="caption" color="text.secondary">
                        Last Position:
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {selectedVehicle.latitude?.toFixed(6)}, {selectedVehicle.longitude?.toFixed(6)}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Updated: {formatTimeAgo(selectedVehicle.lastGpsAt)}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              )}

              {/* Summary Stats */}
              <Box sx={{ mt: 4, display: 'flex', gap: 4 }}>
                {liveMap.vehicles.slice(0, 3).map((v) => (
                  <Box key={v.id} sx={{ textAlign: 'center' }}>
                    <Chip
                      icon={<LocalShipping />}
                      label={v.registrationNumber}
                      color={v.gpsStatus === 'ONLINE' ? 'success' : 'default'}
                      variant="outlined"
                    />
                    <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
                      {v.latitude?.toFixed(4)}, {v.longitude?.toFixed(4)}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Box>

            {/* Footer with timestamp */}
            <Box sx={{ p: 1, bgcolor: 'background.paper', borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="caption" color="text.secondary">
                Last updated: {new Date(liveMap.timestamp).toLocaleTimeString()} • 
                Auto-refresh every {AUTO_REFRESH_INTERVAL / 1000}s
              </Typography>
            </Box>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default VehicleLiveMap;
