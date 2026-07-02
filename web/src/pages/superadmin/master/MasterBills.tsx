import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Card,
  CardContent,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Skeleton,
  Typography,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { Receipt as BillsIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { adminApi } from '../../../api/admin';

const statusOptions = ['', 'PENDING', 'PAID', 'PARTIAL', 'OVERDUE', 'CANCELLED'];

const formatCurrency = (value: unknown): string => {
  const amount = typeof value === 'number' ? value : Number(value ?? 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
};

const formatDate = (value: unknown): string => {
  if (!value) return '-';
  const date = new Date(String(value));
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString('en-IN');
};

export default function MasterBills() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [status, setStatus] = useState('');

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-bills', page, pageSize, status],
    queryFn: () => adminApi.listMasterInvoices({
      status: status || undefined,
      page,
      size: pageSize,
    }),
  });

  const columns: GridColDef[] = [
    { field: 'invoiceNumber', headerName: 'Invoice', width: 150 },
    { field: 'hcfName', headerName: 'HCF', flex: 1, minWidth: 180 },
    { field: 'cbwtfName', headerName: 'CBWTF', flex: 1, minWidth: 180 },
    {
      field: 'totalAmount',
      headerName: 'Amount',
      width: 130,
      headerAlign: 'right',
      align: 'right',
      renderCell: (params) => formatCurrency(params.value),
    },
    { field: 'status', headerName: 'Status', width: 120 },
    {
      field: 'periodStart',
      headerName: 'Period Start',
      width: 130,
      renderCell: (params) => formatDate(params.value),
    },
    {
      field: 'periodEnd',
      headerName: 'Period End',
      width: 130,
      renderCell: (params) => formatDate(params.value),
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 130,
      renderCell: (params) => formatDate(params.value),
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
          <Typography variant="h4" fontWeight={700}>Bills</Typography>
          <Typography variant="body2" color="text.secondary">All invoices across all CBWTFs</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="master-bill-status-label">Status</InputLabel>
            <Select
              labelId="master-bill-status-label"
              value={status}
              label="Status"
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
            >
              {statusOptions.map((option) => (
                <MenuItem key={option || 'ALL'} value={option}>
                  {option || 'All'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load bills.</Alert>}

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
              onPaginationModelChange={(model) => {
                setPage(model.page);
                setPageSize(model.pageSize);
              }}
              pageSizeOptions={[20, 50, 100]}
              disableRowSelectionOnClick
              autoHeight
              sx={dataGridSx}
              slots={{
                noRowsOverlay: () => (
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', py: 6 }}>
                    <BillsIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No bills found</Typography>
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
