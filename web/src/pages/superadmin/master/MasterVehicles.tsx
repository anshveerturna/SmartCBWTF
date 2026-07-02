import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Typography, Skeleton, Alert, IconButton } from '@mui/material';
import { Refresh as RefreshIcon, DirectionsCar as VehicleIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterVehicles() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-vehicles', page, pageSize],
    queryFn: () => adminApi.listMasterVehicles({ page, size: pageSize }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'registrationNo', 
      headerName: 'Registration', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'vehicleType', 
      headerName: 'Type', 
      width: 120,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'cbwtfName', 
      headerName: 'CBWTF', 
      flex: 1, 
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'driverName', 
      headerName: 'Driver', 
      width: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'status', 
      headerName: 'Status', 
      width: 100,
      headerAlign: 'left',
      align: 'left',
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
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>Vehicles</Typography>
          <Typography variant="body2" color="text.secondary">All vehicles across all CBWTFs</Typography>
        </Box>
        <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load vehicles.</Alert>}

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
                    <VehicleIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No vehicles found</Typography>
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
