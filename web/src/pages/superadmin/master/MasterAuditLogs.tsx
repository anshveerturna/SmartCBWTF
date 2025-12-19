import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Typography, FormControl, InputLabel, Select, MenuItem, Stack, Skeleton, Alert, IconButton, Chip } from '@mui/material';
import { Refresh as RefreshIcon, History as AuditIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterAuditLogs() {
  const [action, setAction] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-audit-logs', action, page, pageSize],
    queryFn: () => adminApi.listMasterAuditLogs({ action: action || undefined, page, size: pageSize }),
  });

  const actionColors: Record<string, 'error' | 'success' | 'warning' | 'info' | 'primary' | 'default'> = {
    CREATED: 'success',
    PLAN_CHANGED: 'info',
    SUSPENDED: 'error',
    REACTIVATED: 'success',
    USER_CREATED: 'primary',
    USER_DISABLED: 'error',
    USER_ENABLED: 'success',
    ACCESS_REVOKED: 'error',
  };

  const columns: GridColDef[] = [
    { 
      field: 'createdAt', 
      headerName: 'Time', 
      width: 170,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value ? new Date(p.value).toLocaleString() : '-',
    },
    { 
      field: 'action', 
      headerName: 'Action', 
      width: 150,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => (
        <Chip 
          label={String(p.value || '-').replace(/_/g, ' ')} 
          size="small" 
          color={actionColors[p.value as string] || 'default'} 
        />
      ),
    },
    { 
      field: 'entityType', 
      headerName: 'Entity', 
      width: 100,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'performedByUsername', 
      headerName: 'Performed By', 
      width: 150,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'performedByRole', 
      headerName: 'Role', 
      width: 120,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'oldValue', 
      headerName: 'Old Value', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value || '-',
    },
    { 
      field: 'newValue', 
      headerName: 'New Value', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value || '-',
    },
    { 
      field: 'notes', 
      headerName: 'Notes', 
      flex: 1, 
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value || '-',
    },
  ];

  const dataGridSx = {
    border: 'none',
    '& .MuiDataGrid-columnHeaders': { bgcolor: 'grey.100', borderBottom: '2px solid', borderColor: 'divider' },
    '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 600 },
    '& .MuiDataGrid-cell': { borderBottom: '1px solid', borderColor: 'divider' },
  };

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Audit Logs</Typography>
        <Typography variant="body2" color="text.secondary">All subscription and user audit events (Read-Only)</Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Action</InputLabel>
              <Select value={action} label="Action" onChange={(e) => setAction(e.target.value)}>
                <MenuItem value="">All Actions</MenuItem>
                <MenuItem value="CREATED">Created</MenuItem>
                <MenuItem value="PLAN_CHANGED">Plan Changed</MenuItem>
                <MenuItem value="SUSPENDED">Suspended</MenuItem>
                <MenuItem value="REACTIVATED">Reactivated</MenuItem>
                <MenuItem value="USER_CREATED">User Created</MenuItem>
                <MenuItem value="USER_DISABLED">User Disabled</MenuItem>
                <MenuItem value="USER_ENABLED">User Enabled</MenuItem>
                <MenuItem value="ACCESS_REVOKED">Access Revoked</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load audit logs.</Alert>}

      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>{[...Array(5)].map((_, i) => <Skeleton key={i} height={52} />)}</Box>
          ) : (
            <DataGrid
              rows={data?.content || []}
              columns={columns}
              rowCount={data?.totalElements || 0}
              paginationMode="server"
              paginationModel={{ page, pageSize }}
              onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize); }}
              pageSizeOptions={[20, 50, 100]}
              disableRowSelectionOnClick
              autoHeight
              sx={dataGridSx}
              slots={{
                noRowsOverlay: () => (
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', py: 6 }}>
                    <AuditIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No audit logs found</Typography>
                  </Box>
                ),
              }}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
