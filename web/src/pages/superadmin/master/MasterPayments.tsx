import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Typography, Stack, Skeleton, Alert, IconButton } from '@mui/material';
import { Refresh as RefreshIcon, Payment as PaymentIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterPayments() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-payments', page, pageSize],
    queryFn: () => adminApi.listMasterPayments({ page, size: pageSize }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'paymentId', 
      headerName: 'Payment ID', 
      width: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'invoiceNumber', 
      headerName: 'Invoice', 
      width: 140,
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
      field: 'cbwtfName', 
      headerName: 'CBWTF', 
      width: 160,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'amount', 
      headerName: 'Amount', 
      width: 120,
      headerAlign: 'right',
      align: 'right',
    },
    { 
      field: 'paymentMethod', 
      headerName: 'Method', 
      width: 110,
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
      field: 'paidAt', 
      headerName: 'Paid At', 
      width: 150,
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
        <Typography variant="h4" fontWeight={700}>Payments</Typography>
        <Typography variant="body2" color="text.secondary">All payments across all CBWTFs (Read-Only)</Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
            <Typography variant="body2" color="text.secondary">
              Payment entity not yet implemented — this page will populate when Payment domain is added.
            </Typography>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load payments.</Alert>}

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
                    <PaymentIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No payments found</Typography>
                    <Typography variant="caption" color="text.disabled" sx={{ mt: 1 }}>Payment entity pending implementation</Typography>
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
