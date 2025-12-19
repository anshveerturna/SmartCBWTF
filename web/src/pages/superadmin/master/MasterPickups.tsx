import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, FormControl, InputLabel, Select, MenuItem,
  Stack, Skeleton, Alert, IconButton, Chip,
} from '@mui/material';
import { Refresh as RefreshIcon, LocalShipping as PickupIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterPickups() {
  const [eventType, setEventType] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-pickups', eventType, page, pageSize],
    queryFn: () => adminApi.listMasterPickups({ eventType: eventType || undefined, page, size: pageSize }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'qrCode', 
      headerName: 'QR Code', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'hcfName', 
      headerName: 'HCF', 
      flex: 1, 
      minWidth: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'cbwtfName', 
      headerName: 'CBWTF', 
      width: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'eventType', 
      headerName: 'Type', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => (
        <Chip 
          label={p.value?.replace('_', ' ') || '-'} 
          size="small" 
          color={p.value === 'HCF_COLLECTION' ? 'primary' : 'success'} 
          variant="outlined" 
        />
      ),
    },
    { 
      field: 'weightKg', 
      headerName: 'Weight (kg)', 
      width: 100, 
      headerAlign: 'right',
      align: 'right',
      renderCell: (p) => p.value != null ? Number(p.value).toFixed(2) : '-',
    },
    { 
      field: 'anomalyState', 
      headerName: 'Status', 
      width: 120,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => (
        <Chip 
          label={p.value || 'OK'} 
          size="small" 
          color={!p.value || p.value === 'OK' ? 'success' : 'error'} 
        />
      ),
    },
    { 
      field: 'eventTs', 
      headerName: 'Event Time', 
      width: 160,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value ? new Date(p.value).toLocaleString() : '-',
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
        <Typography variant="h4" fontWeight={700}>Waste Pickups</Typography>
        <Typography variant="body2" color="text.secondary">All bag events across all CBWTFs (Read-Only)</Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Event Type</InputLabel>
              <Select value={eventType} label="Event Type" onChange={(e) => setEventType(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                <MenuItem value="HCF_COLLECTION">HCF Collection</MenuItem>
                <MenuItem value="CBWTF_VERIFICATION">CBWTF Verification</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load pickups.</Alert>}

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
                    <PickupIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No pickups found</Typography>
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
