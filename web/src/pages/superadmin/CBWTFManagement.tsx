import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  InputAdornment,
  Button,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Stack,
  Skeleton,
  Alert,
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Business as BusinessIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { adminApi } from '../../api/admin';
import type { CBWTFDTO } from '../../api/admin';

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  ACTIVE: 'success',
  TRIAL: 'info',
  EXPIRED: 'error',
  SUSPENDED: 'warning',
  CANCELLED: 'default',
};

const planColors: Record<string, string> = {
  BASIC: '#64748b',
  PRO: '#3b82f6',
  ENTERPRISE: '#8b5cf6',
  TRIAL: '#f59e0b',
};

export default function CBWTFManagement() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedCBWTF, setSelectedCBWTF] = useState<CBWTFDTO | null>(null);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['cbwtfs', search, statusFilter, page, pageSize],
    queryFn: () => adminApi.listCBWTFs({
      search: search || undefined,
      status: statusFilter || undefined,
      page,
      size: pageSize,
    }),
  });

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, cbwtf: CBWTFDTO) => {
    setAnchorEl(event.currentTarget);
    setSelectedCBWTF(cbwtf);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedCBWTF(null);
  };

  const columns: GridColDef[] = useMemo(() => [
    {
      field: 'code',
      headerName: 'Code',
      width: 130,
      headerAlign: 'left',
      align: 'left',
      renderCell: (params: GridRenderCellParams<CBWTFDTO>) => (
        <Typography fontWeight={600} fontFamily="monospace">
          {params.value}
        </Typography>
      ),
    },
    {
      field: 'name',
      headerName: 'CBWTF Name',
      flex: 1,
      minWidth: 180,
      headerAlign: 'left',
      align: 'left',
    },
    {
      field: 'subscriptionPlan',
      headerName: 'Plan',
      width: 110,
      headerAlign: 'left',
      align: 'left',
      renderCell: (params: GridRenderCellParams<CBWTFDTO>) => (
        <Chip
          label={params.value}
          size="small"
          sx={{
            bgcolor: planColors[params.value as string] || '#64748b',
            color: '#fff',
            fontWeight: 600,
          }}
        />
      ),
    },
    {
      field: 'subscriptionStatus',
      headerName: 'Status',
      width: 110,
      headerAlign: 'center',
      align: 'center',
      renderCell: (params: GridRenderCellParams<CBWTFDTO>) => (
        <Chip
          label={params.value}
          color={statusColors[params.value as string] || 'default'}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'hcfCount',
      headerName: 'HCFs',
      width: 70,
      headerAlign: 'right',
      align: 'right',
    },
    {
      field: 'activeUserCount',
      headerName: 'Users',
      width: 70,
      headerAlign: 'right',
      align: 'right',
    },
    {
      field: 'subscriptionExpiresAt',
      headerName: 'Expires',
      width: 110,
      headerAlign: 'left',
      align: 'left',
      renderCell: (params: GridRenderCellParams<CBWTFDTO>) => {
        if (!params.value) return '-';
        const date = new Date(params.value as string);
        const isExpiringSoon = date.getTime() - Date.now() < 30 * 24 * 60 * 60 * 1000;
        return (
          <Typography
            variant="body2"
            color={isExpiringSoon ? 'error.main' : 'text.secondary'}
          >
            {date.toLocaleDateString()}
          </Typography>
        );
      },
    },
    {
      field: 'actions',
      headerName: '',
      width: 50,
      headerAlign: 'center',
      align: 'center',
      sortable: false,
      renderCell: (params: GridRenderCellParams<CBWTFDTO>) => (
        <IconButton
          size="small"
          onClick={(e) => handleMenuClick(e, params.row)}
        >
          <MoreVertIcon />
        </IconButton>
      ),
    },
  ], []);

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>
            CBWTF Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage all CBWTF facilities and subscriptions
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/superadmin/cbwtfs/new')}
          sx={{ borderRadius: 2 }}
        >
          Onboard CBWTF
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3, bgcolor: 'background.paper', borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField
              placeholder="Search by name or code..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              sx={{ minWidth: 300 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="TRIAL">Trial</MenuItem>
                <MenuItem value="EXPIRED">Expired</MenuItem>
                <MenuItem value="SUSPENDED">Suspended</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={() => refetch()}>
              <RefreshIcon />
            </IconButton>
          </Stack>
        </CardContent>
      </Card>

      {/* Error State */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load CBWTFs. Please try again.
        </Alert>
      )}

      {/* Data Grid */}
      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ p: 3 }}>
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} height={52} />
              ))}
            </Box>
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
              pageSizeOptions={[10, 25, 50]}
              disableRowSelectionOnClick
              autoHeight
              sx={{
                border: 'none',
                '& .MuiDataGrid-cell': {
                  borderColor: 'divider',
                },
                '& .MuiDataGrid-columnHeaders': {
                  bgcolor: 'action.hover',
                },
                '& .MuiDataGrid-row:hover': {
                  bgcolor: 'action.hover',
                },
              }}
              onRowClick={(params) => navigate(`/superadmin/cbwtfs/${params.id}`)}
              slots={{
                noRowsOverlay: () => (
                  <Box
                    sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      height: '100%',
                      py: 4,
                    }}
                  >
                    <BusinessIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No CBWTFs found</Typography>
                  </Box>
                ),
              }}
            />
          )}
        </CardContent>
      </Card>

      {/* Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => {
          if (selectedCBWTF) navigate(`/superadmin/cbwtfs/${selectedCBWTF.id}`);
          handleMenuClose();
        }}>
          View Details
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedCBWTF) navigate(`/superadmin/cbwtfs/${selectedCBWTF.id}/edit`);
          handleMenuClose();
        }}>
          Edit Subscription
        </MenuItem>
        {selectedCBWTF?.subscriptionStatus === 'ACTIVE' && (
          <MenuItem onClick={handleMenuClose} sx={{ color: 'warning.main' }}>
            Suspend CBWTF
          </MenuItem>
        )}
        {(selectedCBWTF?.subscriptionStatus === 'SUSPENDED' || 
          selectedCBWTF?.subscriptionStatus === 'EXPIRED') && (
          <MenuItem onClick={handleMenuClose} sx={{ color: 'success.main' }}>
            Reactivate CBWTF
          </MenuItem>
        )}
      </Menu>
    </Box>
  );
}
