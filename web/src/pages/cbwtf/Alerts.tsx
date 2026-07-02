import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  IconButton,
  CircularProgress,
  Alert as MuiAlert,
  Tooltip,
  TablePagination,
  Button,
  Badge,
  Stack,
} from '@mui/material';
import {
  Notifications as AlertsIcon,
  CheckCircle as ReadIcon,
  Circle as UnreadIcon,
  DoneAll as MarkAllReadIcon,
  Route as RouteIcon,
  Inventory as BagIcon,
  Build as OperationalIcon,
  Dashboard as DashboardIcon,
} from '@mui/icons-material';
import {
  getUnifiedAlerts,
  markAlertAsRead,
  markAllAlertsAsRead,
  resolveRouteAlert,
  type UnifiedAlertDTO,
} from '../../api/cbwtf';

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('en-IN', { 
    day: '2-digit', 
    month: 'short', 
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getSeverityColor = (severity: string): 'error' | 'warning' | 'info' => {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL': return 'error';
    case 'HIGH': return 'error';
    case 'WARN': return 'warning';
    case 'WARNING': return 'warning';
    case 'MEDIUM': return 'warning';
    default: return 'info';
  }
};

const getSourceIcon = (alert: UnifiedAlertDTO) => {
  if (alert.source === 'route') return <RouteIcon fontSize="small" />;
  if (alert.source === 'bag') return <BagIcon fontSize="small" />;
  if (alert.source === 'dashboard') return <DashboardIcon fontSize="small" />;
  return <OperationalIcon fontSize="small" />;
};

const formatLabel = (value: string) => {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
};

export default function Alerts() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  const { data: alertsData, isLoading, isError } = useQuery({
    queryKey: ['unified-alerts'],
    queryFn: getUnifiedAlerts,
    refetchInterval: 60000,
  });

  const markReadMutation = useMutation({
    mutationFn: markAlertAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      queryClient.invalidateQueries({ queryKey: ['unified-alerts'] });
      queryClient.invalidateQueries({ queryKey: ['unified-alert-count'] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: markAllAlertsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      queryClient.invalidateQueries({ queryKey: ['unified-alerts'] });
      queryClient.invalidateQueries({ queryKey: ['unified-alert-count'] });
    },
  });

  const resolveRouteMutation = useMutation({
    mutationFn: (alertId: string) => resolveRouteAlert(alertId, 'Resolved from alerts page'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unified-alerts'] });
      queryClient.invalidateQueries({ queryKey: ['unified-alert-count'] });
      queryClient.invalidateQueries({ queryKey: ['route-alerts'] });
    },
  });

  const alerts = useMemo(() => alertsData?.alerts ?? [], [alertsData?.alerts]);
  const activeCount = alertsData?.count || 0;
  const portalUnreadCount = alerts.filter((alert) => alert.canMarkRead).length;

  const visibleAlerts = useMemo(
    () => alerts.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage),
    [alerts, page, rowsPerPage]
  );

  const handleMarkAsRead = (id: string | undefined) => {
    if (!id) return;
    markReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    markAllReadMutation.mutate();
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" alignItems="center" justifyContent="space-between" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Badge badgeContent={activeCount} color="error">
            <AlertsIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          </Badge>
          <Box>
            <Typography variant="h4" fontWeight="bold">
              Alerts
            </Typography>
            <Typography variant="body2" color="text.secondary">
              All active system, route, dashboard, and bag alerts in one place
            </Typography>
          </Box>
        </Box>
        {portalUnreadCount > 0 && (
          <Button
            variant="outlined"
            startIcon={<MarkAllReadIcon />}
            onClick={handleMarkAllAsRead}
            disabled={markAllReadMutation.isPending}
          >
            Mark Stored Alerts Read
          </Button>
        )}
      </Box>

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mb: 2 }}>
            <Chip label={`${alerts.length} total alerts`} color="primary" variant="outlined" />
            <Chip label={`${activeCount} active`} color={activeCount > 0 ? 'error' : 'success'} variant="outlined" />
            <Chip label={`${portalUnreadCount} unread stored alerts`} variant="outlined" />
          </Stack>

          {isLoading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : isError ? (
            <MuiAlert severity="error">Unable to load alerts right now. Please try again.</MuiAlert>
          ) : alerts.length === 0 ? (
            <MuiAlert severity="success">No alerts. All systems operating normally.</MuiAlert>
          ) : (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table size="medium">
                  <TableHead>
                    <TableRow>
                      <TableCell width={50}></TableCell>
                      <TableCell>Alert</TableCell>
                      <TableCell>Severity</TableCell>
                      <TableCell>Source</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Time</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {visibleAlerts.map((alert) => (
                      <TableRow 
                        key={alert.id} 
                        hover
                        sx={{ 
                          backgroundColor: alert.canMarkRead || alert.canResolve ? 'action.hover' : 'transparent',
                          opacity: alert.isRead || alert.isResolved ? 0.78 : 1
                        }}
                      >
                        <TableCell>
                          {alert.isRead || alert.isResolved ? (
                            <ReadIcon color="disabled" fontSize="small" />
                          ) : (
                            <UnreadIcon color="primary" fontSize="small" />
                          )}
                        </TableCell>
                        <TableCell>
                          <Box display="flex" alignItems="center" gap={1}>
                            {getSourceIcon(alert)}
                            <Box>
                              <Typography fontWeight={alert.isRead || alert.isResolved ? 500 : 700}>
                                {alert.title}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {alert.message}
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={formatLabel(alert.severity)}
                            color={getSeverityColor(alert.severity)}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip label={alert.sourceLabel} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell>
                          <Chip label={formatLabel(alert.type)} size="small" />
                        </TableCell>
                        <TableCell>{formatDateTime(alert.createdAt)}</TableCell>
                        <TableCell align="center">
                          {alert.canMarkRead && (
                            <Tooltip title="Mark as Read">
                              <IconButton 
                                size="small" 
                                onClick={() => handleMarkAsRead(alert.rawId)}
                                disabled={markReadMutation.isPending}
                              >
                                <ReadIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                          {alert.canResolve && (
                            <Tooltip title="Resolve route alert">
                              <IconButton
                                size="small"
                                onClick={() => alert.rawId && resolveRouteMutation.mutate(alert.rawId)}
                                disabled={resolveRouteMutation.isPending}
                              >
                                <ReadIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <TablePagination
                component="div"
                count={alerts.length}
                page={page}
                onPageChange={(_, newPage) => setPage(newPage)}
                rowsPerPage={rowsPerPage}
                onRowsPerPageChange={(e) => {
                  setRowsPerPage(parseInt(e.target.value, 10));
                  setPage(0);
                }}
              />
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
