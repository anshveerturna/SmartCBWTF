import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Tab,
  Tabs,
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
} from '@mui/material';
import {
  Notifications as AlertsIcon,
  CheckCircle as ReadIcon,
  Circle as UnreadIcon,
  DoneAll as MarkAllReadIcon,
  AttachMoney as FinancialIcon,
  Build as OperationalIcon,
  Gavel as ComplianceIcon,
} from '@mui/icons-material';
import { getAlerts, getUnreadAlertCount, markAlertAsRead, markAllAlertsAsRead } from '../../api/cbwtf';

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
  switch (severity) {
    case 'CRITICAL': return 'error';
    case 'WARN': return 'warning';
    default: return 'info';
  }
};

const getTypeIcon = (type: string) => {
  if (type.includes('PAYMENT') || type.includes('BILL')) return <FinancialIcon fontSize="small" />;
  if (type.includes('BAG') || type.includes('GPS') || type.includes('PICKUP')) return <OperationalIcon fontSize="small" />;
  return <ComplianceIcon fontSize="small" />;
};

export default function Alerts() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  const categories = ['ALL', 'FINANCIAL', 'OPERATIONAL', 'COMPLIANCE'];
  const category = categories[activeTab];

  // Fetch alerts
  const { data: alertsData, isLoading } = useQuery({
    queryKey: ['alerts', category, page, rowsPerPage],
    queryFn: () => getAlerts(page, rowsPerPage, category === 'ALL' ? undefined : category),
  });

  // Fetch unread count
  const { data: unreadData } = useQuery({
    queryKey: ['alertsUnread'],
    queryFn: getUnreadAlertCount,
  });

  // Mark as read mutation
  const markReadMutation = useMutation({
    mutationFn: markAlertAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      queryClient.invalidateQueries({ queryKey: ['alertsUnread'] });
    },
  });

  // Mark all as read mutation
  const markAllReadMutation = useMutation({
    mutationFn: markAllAlertsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      queryClient.invalidateQueries({ queryKey: ['alertsUnread'] });
    },
  });

  const handleMarkAsRead = (id: string) => {
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
          <Badge badgeContent={unreadData?.count || 0} color="error">
            <AlertsIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          </Badge>
          <Box>
            <Typography variant="h4" fontWeight="bold">
              Alerts
            </Typography>
            <Typography variant="body2" color="text.secondary">
              System notifications and compliance alerts
            </Typography>
          </Box>
        </Box>
        {(unreadData?.count || 0) > 0 && (
          <Button
            variant="outlined"
            startIcon={<MarkAllReadIcon />}
            onClick={handleMarkAllAsRead}
            disabled={markAllReadMutation.isPending}
          >
            Mark All as Read
          </Button>
        )}
      </Box>

      {/* Tabs */}
      <Card>
        <Tabs
          value={activeTab}
          onChange={(_, newValue) => {
            setActiveTab(newValue);
            setPage(0);
          }}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          <Tab label="All Alerts" />
          <Tab label="Financial" icon={<FinancialIcon />} iconPosition="start" />
          <Tab label="Operational" icon={<OperationalIcon />} iconPosition="start" />
          <Tab label="Compliance" icon={<ComplianceIcon />} iconPosition="start" />
        </Tabs>

        <CardContent>
          {isLoading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : alertsData?.content?.length === 0 ? (
            <MuiAlert severity="success">No alerts. All systems operating normally.</MuiAlert>
          ) : (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell width={50}></TableCell>
                      <TableCell>Alert</TableCell>
                      <TableCell>Severity</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Time</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {alertsData?.content?.map((alert: any) => (
                      <TableRow 
                        key={alert.id} 
                        hover
                        sx={{ 
                          backgroundColor: alert.isRead ? 'transparent' : 'action.hover',
                          opacity: alert.isRead ? 0.7 : 1
                        }}
                      >
                        <TableCell>
                          {alert.isRead ? (
                            <ReadIcon color="disabled" fontSize="small" />
                          ) : (
                            <UnreadIcon color="primary" fontSize="small" />
                          )}
                        </TableCell>
                        <TableCell>
                          <Box display="flex" alignItems="center" gap={1}>
                            {getTypeIcon(alert.type)}
                            <Box>
                              <Typography fontWeight={alert.isRead ? 400 : 600}>
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
                            label={alert.severity}
                            color={getSeverityColor(alert.severity)}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip label={alert.type.replace(/_/g, ' ')} size="small" />
                        </TableCell>
                        <TableCell>{formatDateTime(alert.createdAt)}</TableCell>
                        <TableCell align="center">
                          {!alert.isRead && (
                            <Tooltip title="Mark as Read">
                              <IconButton 
                                size="small" 
                                onClick={() => handleMarkAsRead(alert.id)}
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
                count={alertsData?.totalElements || 0}
                page={page}
                onPageChange={(_, newPage) => setPage(newPage)}
                rowsPerPage={rowsPerPage}
                onRowsPerPageChange={(e) => setRowsPerPage(parseInt(e.target.value, 10))}
              />
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
