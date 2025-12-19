import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
  Skeleton,
  Alert,
  IconButton,
  Chip,
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  LocalHospital as HcfIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';

export default function MasterHcfs() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['master-hcfs', search, statusFilter, page, pageSize],
    queryFn: () => adminApi.listMasterHcfs({
      search: search || undefined,
      status: statusFilter || undefined,
      page,
      size: pageSize,
    }),
  });

  const columns: GridColDef[] = [
    { 
      field: 'code', 
      headerName: 'Code', 
      width: 120,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'name', 
      headerName: 'HCF Name', 
      flex: 1, 
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'address', 
      headerName: 'Address', 
      flex: 1, 
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'contactPhone', 
      headerName: 'Phone', 
      width: 130,
      headerAlign: 'left',
      align: 'left',
    },
    { 
      field: 'numberOfBeds', 
      headerName: 'Beds', 
      width: 80, 
      headerAlign: 'center',
      align: 'center',
    },
    { 
      field: 'status', 
      headerName: 'Status', 
      width: 140,
      headerAlign: 'left',
      align: 'left',
      renderCell: (params) => (
        <Chip
          label={params.value?.replace('_', ' ') || '-'}
          size="small"
          color={params.value === 'ACTIVE' ? 'success' : params.value === 'PENDING_APPROVAL' ? 'warning' : 'default'}
          variant="outlined"
        />
      ),
    },
    { 
      field: 'createdAt', 
      headerName: 'Created', 
      width: 110,
      headerAlign: 'left',
      align: 'left',
      renderCell: (params) => params.value ? new Date(params.value).toLocaleDateString() : '-',
    },
  ];

  const dataGridSx = {
    border: 'none',
    '& .MuiDataGrid-columnHeaders': { 
      bgcolor: 'grey.100',
      borderBottom: '2px solid',
      borderColor: 'divider',
    },
    '& .MuiDataGrid-columnHeaderTitle': {
      fontWeight: 600,
    },
    '& .MuiDataGrid-cell': {
      borderBottom: '1px solid',
      borderColor: 'divider',
    },
    '& .MuiDataGrid-row:hover': {
      bgcolor: 'action.hover',
    },
  };

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Healthcare Facilities</Typography>
        <Typography variant="body2" color="text.secondary">
          All HCFs across all CBWTFs (Read-Only)
        </Typography>
      </Box>

      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <TextField
              placeholder="Search by name or code..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              sx={{ minWidth: 280 }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>,
              }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="PENDING_APPROVAL">Pending</MenuItem>
                <MenuItem value="REJECTED">Rejected</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={() => refetch()} title="Refresh"><RefreshIcon /></IconButton>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>Failed to load HCFs.</Alert>}

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
                    <HcfIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No HCFs found</Typography>
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
