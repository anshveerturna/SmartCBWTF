import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem,
  Stack, Skeleton, Alert, IconButton, Chip,
} from '@mui/material';
import { Search as SearchIcon, Refresh as RefreshIcon, QrCode as QrIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterQrLabels() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-qr-labels', search, status, page, pageSize],
    queryFn: () => adminApi.listMasterQrLabels({ search: search || undefined, status: status || undefined, page, size: pageSize }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'qrCode', 
      headerName: 'QR Code', 
      width: 180,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'serialNo', 
      headerName: 'Serial No', 
      width: 130,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'category', 
      headerName: 'Category', 
      width: 100,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => <Chip label={p.value || '-'} size="small" color="info" variant="outlined" />,
    },
    { 
      field: 'status', 
      headerName: 'Status', 
      width: 100,
      headerAlign: 'left',
      align: 'left',
      renderCell: (p) => (
        <Chip 
          label={p.value || '-'} 
          size="small" 
          color={p.value === 'ISSUED' ? 'warning' : p.value === 'USED' ? 'success' : 'error'} 
        />
      ),
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
      field: 'issuedAt', 
      headerName: 'Issued', 
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
        <Typography variant="h4" fontWeight={700}>QR Labels</Typography>
        <Typography variant="body2" color="text.secondary">All QR labels across all CBWTFs (Read-Only)</Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField 
              placeholder="Search QR code or serial..." 
              value={search} 
              onChange={(e) => setSearch(e.target.value)} 
              size="small" 
              sx={{ minWidth: 260 }} 
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment> }} 
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select value={status} label="Status" onChange={(e) => setStatus(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ISSUED">Issued</MenuItem>
                <MenuItem value="USED">Used</MenuItem>
                <MenuItem value="VOID">Void</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load QR labels.</Alert>}

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
                    <QrIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No QR labels found</Typography>
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
