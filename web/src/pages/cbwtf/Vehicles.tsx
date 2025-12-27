import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import {
  LocalShipping,
  GpsFixed,
  GpsOff,
  GpsNotFixed,
  Refresh,
} from '@mui/icons-material';
import { getVehicles, type VehicleDTO } from '../../api/cbwtf';
import { useNavigate } from 'react-router-dom';
import { LocationAddress } from '../../components/LocationAddress';

const getGpsStatusChip = (status: string) => {
  switch (status) {
    case 'ONLINE':
      return <Chip icon={<GpsFixed />} label="Online" color="success" size="small" />;
    case 'OFFLINE':
      return <Chip icon={<GpsOff />} label="Offline" color="error" size="small" />;
    case 'CONNECTED':
      return <Chip icon={<GpsNotFixed />} label="Connected" color="info" size="small" />;
    case 'PENDING':
    default:
      return <Chip icon={<GpsNotFixed />} label="Pending" color="default" size="small" />;
  }
};

const formatTimeAgo = (dateString: string | null): string => {
  if (!dateString) return 'Never';
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
  return `${Math.floor(diffMins / 1440)}d ago`;
};

const Vehicles = () => {
  const navigate = useNavigate();
  
  const {
    data: vehicles,
    isLoading,
    error,
    refetch,
  } = useQuery<VehicleDTO[]>({
    queryKey: ['cbwtf-vehicles'],
    queryFn: getVehicles,
  });

  const onlineCount = vehicles?.filter(v => v.gpsStatus === 'ONLINE').length ?? 0;
  const totalCount = vehicles?.length ?? 0;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 600 }}>
            Vehicles
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {onlineCount} of {totalCount} vehicles online
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => refetch()}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<LocalShipping />}
            onClick={() => navigate('/cbwtf/vehicles/live-map')}
          >
            Live Map
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load vehicles. Please try again.
        </Alert>
      )}

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {!isLoading && !error && (
        <Card>
          <CardContent sx={{ p: 0 }}>
            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow sx={{ bgcolor: 'grey.100' }}>
                    <TableCell>Registration</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>GPS Status</TableCell>
                    <TableCell>Last Update</TableCell>
                    <TableCell>Driver</TableCell>
                    <TableCell>Location</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {vehicles?.map((vehicle) => (
                    <TableRow key={vehicle.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <LocalShipping fontSize="small" color="action" />
                          <Typography fontWeight={600}>
                            {vehicle.registrationNumber}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={vehicle.vehicleType || 'N/A'} 
                          size="small" 
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        {getGpsStatusChip(vehicle.gpsStatus)}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {formatTimeAgo(vehicle.lastGpsAt)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {vehicle.driverName || '-'}
                      </TableCell>
                      <TableCell>
                        <LocationAddress 
                          latitude={vehicle.lastLatitude ? Number(vehicle.lastLatitude) : null} 
                          longitude={vehicle.lastLongitude ? Number(vehicle.lastLongitude) : null} 
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                  {(!vehicles || vehicles.length === 0) && (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography color="text.secondary" sx={{ py: 4 }}>
                          No vehicles found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default Vehicles;
