import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  CircularProgress,
  Alert,
  Paper,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  AccessTime as TimeIcon,
  Person as PersonIcon,
  LocalHospital as HcfIcon,
  LocationOn as LocationIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { getAttendanceLogs, type AttendanceRecord } from '../../api/cbwtf';
import { LocationAddress } from '../../components/LocationAddress';

export default function AttendanceList() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(25);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['attendance', page, rowsPerPage],
    queryFn: () => getAttendanceLogs(page, rowsPerPage),
  });

  const formatDateTime = (dateValue: string | number | null | undefined): string => {
    if (!dateValue) return 'Unknown';
    try {
      const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue);
      if (isNaN(date.getTime())) return 'Unknown';
      return date.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return 'Unknown';
    }
  };

  const formatTimeAgo = (dateValue: string | number | null | undefined): string => {
    if (!dateValue) return '';
    try {
      const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue);
      if (isNaN(date.getTime())) return '';
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      if (diffMs < 0) return 'Just now';
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays}d ago`;
      return '';
    } catch {
      return '';
    }
  };

  const getRoleColor = (role: string | null): 'primary' | 'secondary' | 'default' => {
    if (!role) return 'default';
    if (role === 'DRIVER') return 'primary';
    if (role === 'PLANT_OPERATOR') return 'secondary';
    return 'default';
  };

  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">Failed to load attendance records. Please try again.</Alert>
      </Box>
    );
  }

  const records = data?.records || [];
  const totalRecords = data?.totalRecords || 0;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <TimeIcon /> Attendance Log
          </Typography>
          <Typography color="text.secondary">
            Track staff attendance records across all healthcare facilities
          </Typography>
        </Box>
        <Tooltip title="Refresh">
          <IconButton onClick={() => refetch()} color="primary">
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>

      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 0 }}>
          {records.length === 0 ? (
            <Box sx={{ p: 4, textAlign: 'center' }}>
              <TimeIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                No attendance records found
              </Typography>
              <Typography color="text.secondary" variant="body2">
                Attendance records will appear here when staff mark their attendance via the mobile app.
              </Typography>
            </Box>
          ) : (
            <>
              <TableContainer component={Paper} elevation={0}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, bgcolor: 'action.hover' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <PersonIcon fontSize="small" /> Staff
                        </Box>
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, bgcolor: 'action.hover' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <HcfIcon fontSize="small" /> Healthcare Facility
                        </Box>
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, bgcolor: 'action.hover' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <TimeIcon fontSize="small" /> Timestamp
                        </Box>
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600, bgcolor: 'action.hover' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <LocationIcon fontSize="small" /> Address
                        </Box>
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {records.map((record: AttendanceRecord) => (
                      <TableRow 
                        key={record.id} 
                        hover
                        sx={{ 
                          cursor: record.hcfId ? 'pointer' : 'default',
                          '&:hover': { bgcolor: 'action.hover' }
                        }}
                        onClick={() => record.hcfId && navigate(`/cbwtf/hcfs/${record.hcfId}`)}
                      >
                        <TableCell>
                          <Box>
                            <Typography variant="body1" fontWeight={500}>
                              {record.staffName}
                            </Typography>
                            {record.staffRole && (
                              <Chip 
                                label={record.staffRole.replace('_', ' ')} 
                                size="small" 
                                color={getRoleColor(record.staffRole)}
                                variant="outlined"
                                sx={{ mt: 0.5, fontSize: '0.7rem' }}
                              />
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body1">
                            {record.hcfName}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Box>
                            <Typography variant="body2">
                              {formatDateTime(record.eventTs)}
                            </Typography>
                            {formatTimeAgo(record.eventTs) && (
                              <Typography variant="caption" color="text.secondary">
                                {formatTimeAgo(record.eventTs)}
                              </Typography>
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <LocationAddress 
                            latitude={record.gpsLat} 
                            longitude={record.gpsLon} 
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <TablePagination
                component="div"
                count={totalRecords}
                page={page}
                onPageChange={handleChangePage}
                rowsPerPage={rowsPerPage}
                onRowsPerPageChange={handleChangeRowsPerPage}
                rowsPerPageOptions={[10, 25, 50, 100]}
              />
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
