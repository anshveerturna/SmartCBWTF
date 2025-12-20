import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  People as PeopleIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { adminApi } from '../../../api/admin';
import type { UserDTO } from '../../../api/admin';

const roleColors: Record<string, string> = {
  SUPER_ADMIN: '#ef4444',
  CBWTF_ADMIN: '#3b82f6',
  HCF_ADMIN: '#10b981',
  DRIVER: '#f59e0b',
  PLANT_OPERATOR: '#8b5cf6',
  ACCOUNTANT: '#06b6d4',
};

export default function AllUsers() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedUser, setSelectedUser] = useState<UserDTO | null>(null);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['users', search, roleFilter, statusFilter, page, pageSize],
    queryFn: () => adminApi.listUsers({
      search: search || undefined,
      role: roleFilter || undefined,
      active: statusFilter === '' ? undefined : statusFilter === 'active',
      page,
      size: pageSize,
    }),
  });

  const disableMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => 
      adminApi.disableUser(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const enableMutation = useMutation({
    mutationFn: (id: string) => adminApi.enableUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, user: UserDTO) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
    setSelectedUser(user);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedUser(null);
  };

  const handleDisable = () => {
    if (selectedUser) {
      disableMutation.mutate({ id: selectedUser.id, reason: 'Disabled by SuperAdmin' });
    }
    handleMenuClose();
  };

  const handleEnable = () => {
    if (selectedUser) {
      enableMutation.mutate(selectedUser.id);
    }
    handleMenuClose();
  };

  const columns: GridColDef<UserDTO>[] = useMemo(() => [
    {
      field: 'username',
      headerName: 'Username',
      width: 140,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <Typography fontWeight={600} fontSize="0.875rem">
          {params.value}
        </Typography>
      ),
    },
    {
      field: 'fullName',
      headerName: 'Name',
      flex: 1,
      minWidth: 150,
    },
    {
      field: 'role',
      headerName: 'Role',
      width: 130,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <Chip
          label={params.value?.replace('_', ' ')}
          size="small"
          sx={{
            bgcolor: roleColors[params.value as string] || '#64748b',
            color: '#fff',
            fontWeight: 600,
            fontSize: '0.7rem',
          }}
        />
      ),
    },
    {
      field: 'cbwtfName',
      headerName: 'CBWTF',
      width: 150,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <Typography variant="body2" color="text.secondary" fontSize="0.85rem">
          {params.value || '-'}
        </Typography>
      ),
    },
    {
      field: 'active',
      headerName: 'Status',
      width: 90,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <Chip
          label={params.value ? 'Active' : 'Disabled'}
          color={params.value ? 'success' : 'error'}
          size="small"
          variant="outlined"
          sx={{ fontSize: '0.7rem' }}
        />
      ),
    },
    {
      field: 'email',
      headerName: 'Email',
      flex: 1,
      minWidth: 180,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <Typography variant="body2" color="text.secondary" fontSize="0.85rem" noWrap>
          {params.value || '-'}
        </Typography>
      ),
    },
    {
      field: 'actions',
      headerName: '',
      width: 50,
      sortable: false,
      disableColumnMenu: true,
      renderCell: (params: GridRenderCellParams<UserDTO>) => (
        <IconButton
          size="small"
          onClick={(e) => handleMenuClick(e, params.row)}
        >
          <MoreVertIcon fontSize="small" />
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
            User Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage all users across all CBWTFs
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/superadmin/users/new')}
          sx={{ borderRadius: 2 }}
        >
          Create User
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3, bgcolor: 'background.paper', borderRadius: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <TextField
              placeholder="Search by name, username, or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              sx={{ minWidth: 280 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>Role</InputLabel>
              <Select
                value={roleFilter}
                label="Role"
                onChange={(e) => setRoleFilter(e.target.value)}
              >
                <MenuItem value="">All Roles</MenuItem>
                <MenuItem value="SUPER_ADMIN">Super Admin</MenuItem>
                <MenuItem value="CBWTF_ADMIN">CBWTF Admin</MenuItem>
                <MenuItem value="HCF_ADMIN">HCF Admin</MenuItem>
                <MenuItem value="DRIVER">Driver</MenuItem>
                <MenuItem value="PLANT_OPERATOR">Plant Operator</MenuItem>
                <MenuItem value="ACCOUNTANT">Accountant</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 110 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="disabled">Disabled</MenuItem>
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
          Failed to load users. Please try again.
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
              rowHeight={52}
              columnHeaderHeight={48}
              sx={{
                border: 'none',
                '& .MuiDataGrid-columnHeaders': {
                  bgcolor: 'action.hover',
                  borderBottom: 1,
                  borderColor: 'divider',
                },
                '& .MuiDataGrid-cell': {
                  borderColor: 'divider',
                  display: 'flex',
                  alignItems: 'center',
                },
                '& .MuiDataGrid-row': {
                  cursor: 'pointer',
                  '&:hover': {
                    bgcolor: 'action.hover',
                  },
                },
              }}
              onRowClick={(params) => navigate(`/superadmin/users/${params.id}`)}
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
                    <PeopleIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography color="text.secondary">No users found</Typography>
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
          if (selectedUser) navigate(`/superadmin/users/${selectedUser.id}`);
          handleMenuClose();
        }}>
          View Details
        </MenuItem>
        {selectedUser?.active ? (
          <MenuItem onClick={handleDisable} sx={{ color: 'error.main' }}>
            Disable User
          </MenuItem>
        ) : (
          <MenuItem onClick={handleEnable} sx={{ color: 'success.main' }}>
            Enable User
          </MenuItem>
        )}
        <MenuItem onClick={handleMenuClose}>
          Force Password Reset
        </MenuItem>
      </Menu>
    </Box>
  );
}
