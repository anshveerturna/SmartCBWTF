import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Typography, TextField, Stack, Skeleton, Alert, IconButton } from '@mui/material';
import { Refresh as RefreshIcon, AccessTime as AttendanceIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterAttendance() {
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-attendance', dateFrom, dateTo, page, pageSize],
    queryFn: () => adminApi.listMasterAttendance({ from: dateFrom || undefined, to: dateTo || undefined, page, size: pageSize }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'driverName', 
      headerName: 'Driver', 
      flex: 1, 
      minWidth: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'hcfName', 
      headerName: 'HCF', 
      flex: 1, 
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'eventTs', 
      headerName: 'Event Time', 
      width: 170,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value ? new Date(p.value).toLocaleString() : '-',
    },
    { 
      field: 'gpsLat', 
      headerName: 'Latitude', 
      width: 100,
      headerAlign: 'right',
      align: 'right',
      renderCell: (p) => p.value != null ? Number(p.value).toFixed(4) : '-',
    },
    { 
      field: 'gpsLon', 
      headerName: 'Longitude', 
      width: 100,
      headerAlign: 'right',
      align: 'right',
      renderCell: (p) => p.value != null ? Number(p.value).toFixed(4) : '-',
    },
    { 
      field: 'distanceFromHcfM', 
      headerName: 'Distance (m)', 
      width: 110,
      headerAlign: 'right',
      align: 'right',
      renderCell: (p) => p.value != null ? Math.round(Number(p.value)) : '-',
    },
    { 
      field: 'createdAt', 
      headerName: 'Created', 
      width: 110,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => p.value ? new Date(p.value).toLocaleDateString() : '-',
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
        <Typography variant="h4" fontWeight={700}>Attendance Records</Typography>
        <Typography variant="body2" color="text.secondary">All driver attendance across all CBWTFs (Read-Only)</Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField 
              label="From" 
              type="date" 
              value={dateFrom} 
              onChange={(e) => setDateFrom(e.target.value)} 
              size="small" 
              slotProps={{ inputLabel: { shrink: true } }} 
            />
            <TextField 
              label="To" 
              type="date" 
              value={dateTo} 
              onChange={(e) => setDateTo(e.target.value)} 
              size="small" 
              slotProps={{ inputLabel: { shrink: true } }} 
            />
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load attendance.</Alert>}

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
                    <AttendanceIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No attendance records found</Typography>
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
