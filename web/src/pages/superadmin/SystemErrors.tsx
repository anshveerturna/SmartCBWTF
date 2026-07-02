import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Skeleton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  CheckCircle as ResolveIcon,
  Refresh as RefreshIcon,
  ReportProblem as ErrorIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../api/admin';
import type { SystemErrorDetailDTO } from '../../api/admin';

type StatusFilter = '' | SystemErrorDetailDTO['status'];
type SeverityFilter = '' | SystemErrorDetailDTO['severity'];

interface ResolveDialogState {
  open: boolean;
  errorId: string;
  title: string;
  notes: string;
}

const emptyResolveDialog = (): ResolveDialogState => ({
  open: false,
  errorId: '',
  title: '',
  notes: '',
});

const statusColor: Record<SystemErrorDetailDTO['status'], 'default' | 'info' | 'success' | 'warning'> = {
  OPEN: 'warning',
  IN_PROGRESS: 'info',
  RESOLVED: 'success',
  IGNORED: 'default',
};

const severityColor: Record<SystemErrorDetailDTO['severity'], 'default' | 'info' | 'warning' | 'error'> = {
  CRITICAL: 'error',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info',
};

const formatDateTime = (value: string | null | undefined) =>
  value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-';

export default function SystemErrors() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<StatusFilter>('OPEN');
  const [severity, setSeverity] = useState<SeverityFilter>('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [resolveDialog, setResolveDialog] = useState<ResolveDialogState>(() => emptyResolveDialog());

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['system-errors', status, severity, search, page, pageSize],
    queryFn: () => adminApi.listSystemErrors({
      status: status || undefined,
      severity: severity || undefined,
      search: search || undefined,
      page,
      size: pageSize,
    }),
  });

  const { data: stats } = useQuery({
    queryKey: ['system-error-stats'],
    queryFn: adminApi.getErrorStats,
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, notes }: { id: string; notes?: string }) => adminApi.resolveError(id, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['system-errors'] });
      queryClient.invalidateQueries({ queryKey: ['system-error-stats'] });
      queryClient.invalidateQueries({ queryKey: ['platform-stats'] });
      setResolveDialog(emptyResolveDialog());
    },
  });

  const columns: GridColDef<SystemErrorDetailDTO>[] = [
    {
      field: 'createdAt',
      headerName: 'Time',
      width: 175,
      renderCell: (params) => formatDateTime(params.row.createdAt),
    },
    {
      field: 'severity',
      headerName: 'Severity',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.row.severity}
          size="small"
          color={severityColor[params.row.severity]}
          variant={params.row.severity === 'CRITICAL' ? 'filled' : 'outlined'}
        />
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 125,
      renderCell: (params) => (
        <Chip
          label={params.row.status.replace('_', ' ')}
          size="small"
          color={statusColor[params.row.status]}
        />
      ),
    },
    {
      field: 'title',
      headerName: 'Issue',
      flex: 1,
      minWidth: 260,
      renderCell: (params) => (
        <Box sx={{ py: 1 }}>
          <Typography variant="body2" fontWeight={600}>{params.row.title}</Typography>
          {params.row.description && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
              {params.row.description}
            </Typography>
          )}
        </Box>
      ),
    },
    {
      field: 'component',
      headerName: 'Component',
      width: 160,
      renderCell: (params) => params.row.component || '-',
    },
    {
      field: 'tenant',
      headerName: 'Tenant',
      width: 145,
      renderCell: (params) => params.row.hcfCode || params.row.cbwtfCode || '-',
    },
    {
      field: 'source',
      headerName: 'Source',
      width: 140,
      renderCell: (params) => params.row.source.replace(/_/g, ' '),
    },
    {
      field: 'reportedBy',
      headerName: 'Reported By',
      width: 145,
      renderCell: (params) => params.row.reportedBy || '-',
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 110,
      sortable: false,
      filterable: false,
      align: 'center',
      headerAlign: 'center',
      renderCell: (params) => (
        params.row.status === 'RESOLVED' ? (
          <Typography variant="caption" color="text.secondary">Done</Typography>
        ) : (
          <Tooltip title="Resolve error">
            <span>
              <IconButton
                size="small"
                color="success"
                onClick={() => setResolveDialog({
                  open: true,
                  errorId: params.row.id,
                  title: params.row.title,
                  notes: '',
                })}
                disabled={resolveMutation.isPending}
              >
                <ResolveIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        )
      ),
    },
  ];

  const applySearch = () => {
    setPage(0);
    setSearch(searchInput.trim());
  };

  const clearFilters = () => {
    setStatus('OPEN');
    setSeverity('');
    setSearchInput('');
    setSearch('');
    setPage(0);
  };

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>System Errors</Typography>
        <Typography variant="body2" color="text.secondary">
          Platform issue queue for Super Admin review and resolution
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
        {[
          { label: 'Open', value: stats?.open ?? 0, color: 'warning.main' },
          { label: 'In Progress', value: stats?.inProgress ?? 0, color: 'info.main' },
          { label: 'Critical', value: stats?.critical ?? 0, color: 'error.main' },
          { label: 'Errors', value: stats?.errors ?? 0, color: 'error.dark' },
          { label: 'Warnings', value: stats?.warnings ?? 0, color: 'warning.dark' },
        ].map((item) => (
          <Box
            key={item.label}
            sx={{
              minWidth: 150,
              flex: '1 1 150px',
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              p: 2,
              bgcolor: 'background.paper',
            }}
          >
            <Typography variant="caption" color="text.secondary">{item.label}</Typography>
            <Typography variant="h5" fontWeight={700} sx={{ color: item.color }}>{item.value}</Typography>
          </Box>
        ))}
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack
            component="form"
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ xs: 'stretch', md: 'center' }}
            onSubmit={(event) => {
              event.preventDefault();
              applySearch();
            }}
          >
            <TextField
              size="small"
              label="Search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              sx={{ minWidth: 260, flex: 1 }}
            />
            <FormControl size="small" sx={{ minWidth: 170 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={status}
                label="Status"
                onChange={(event) => {
                  setStatus(event.target.value as StatusFilter);
                  setPage(0);
                }}
              >
                <MenuItem value="">All Statuses</MenuItem>
                <MenuItem value="OPEN">Open</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="RESOLVED">Resolved</MenuItem>
                <MenuItem value="IGNORED">Ignored</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Severity</InputLabel>
              <Select
                value={severity}
                label="Severity"
                onChange={(event) => {
                  setSeverity(event.target.value as SeverityFilter);
                  setPage(0);
                }}
              >
                <MenuItem value="">All Severities</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
                <MenuItem value="ERROR">Error</MenuItem>
                <MenuItem value="WARNING">Warning</MenuItem>
                <MenuItem value="INFO">Info</MenuItem>
              </Select>
            </FormControl>
            <Button type="submit" variant="contained">Search</Button>
            <Button variant="outlined" onClick={clearFilters}>Reset</Button>
            <Tooltip title="Refresh">
              <IconButton onClick={() => refetch()}>
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load system errors.</Alert>}
      {resolveMutation.isError && <Alert severity="error" sx={{ mb: 3 }}>Failed to resolve the selected error.</Alert>}

      <Card>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>{[...Array(6)].map((_, index) => <Skeleton key={index} height={56} />)}</Box>
          ) : (
            <DataGrid
              rows={data?.content || []}
              columns={columns}
              rowCount={data?.totalElements || 0}
              paginationMode="server"
              paginationModel={{ page, pageSize }}
              onPaginationModelChange={(model) => {
                setPage(model.page);
                setPageSize(model.pageSize);
              }}
              pageSizeOptions={[20, 50, 100]}
              disableRowSelectionOnClick
              getRowHeight={() => 'auto'}
              autoHeight
              sx={{
                border: 'none',
                '& .MuiDataGrid-columnHeaders': { bgcolor: 'grey.100', borderBottom: '2px solid', borderColor: 'divider' },
                '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 600 },
                '& .MuiDataGrid-cell': { alignItems: 'center', py: 1 },
              }}
              slots={{
                noRowsOverlay: () => (
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 6 }}>
                    <ErrorIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No system errors found</Typography>
                  </Box>
                ),
              }}
            />
          )}
        </CardContent>
      </Card>

      <Dialog
        open={resolveDialog.open}
        onClose={() => !resolveMutation.isPending && setResolveDialog(emptyResolveDialog())}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Resolve Error</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {resolveDialog.title}
          </Typography>
          <TextField
            label="Resolution Notes"
            value={resolveDialog.notes}
            onChange={(event) => setResolveDialog((current) => ({ ...current, notes: event.target.value }))}
            fullWidth
            multiline
            minRows={3}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveDialog(emptyResolveDialog())} disabled={resolveMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="success"
            disabled={!resolveDialog.errorId || resolveMutation.isPending}
            onClick={() => resolveMutation.mutate({
              id: resolveDialog.errorId,
              notes: resolveDialog.notes.trim() || undefined,
            })}
          >
            Resolve
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
