import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  Tooltip,
  Stack,
  TablePagination,
} from '@mui/material';
import {
  Add as AddIcon,
  Person as PersonIcon,
  LocalShipping as DriverIcon,
  Engineering as PlantIcon,
  GpsFixed as OnlineIcon,
  GpsOff as OfflineIcon,
  Visibility as ViewIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material';
import {
  getStaffList,
  createStaff,
  type StaffDTO,
  type CreateStaffRequest,
  type PageResponse,
} from '../../api/cbwtf';

const roleLabels: Record<string, string> = {
  DRIVER: 'Driver',
  PLANT_OPERATOR: 'Plant Operator',
};

const roleIcons: Record<string, React.ReactNode> = {
  DRIVER: <DriverIcon fontSize="small" />,
  PLANT_OPERATOR: <PlantIcon fontSize="small" />,
};

export default function Staff() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newStaffPassword, setNewStaffPassword] = useState<string | null>(null);

  // Fetch staff list
  const { data: staffData, isLoading, error } = useQuery<PageResponse<StaffDTO>>({
    queryKey: ['staff-list', page, rowsPerPage, roleFilter],
    queryFn: () => getStaffList(page, rowsPerPage, roleFilter || undefined),
  });

  // Create staff mutation
  const createMutation = useMutation({
    mutationFn: createStaff,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['staff-list'] });
      if (data.tempPassword) {
        setNewStaffPassword(data.tempPassword);
      }
    },
  });

  const handleCreateSubmit = async (formData: CreateStaffRequest) => {
    await createMutation.mutateAsync(formData);
  };

  const formatTimeAgo = (dateString: string | null): string => {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return date.toLocaleDateString();
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 600, mb: 0.5 }}>
            Staff Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage drivers and plant operators for your facility
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Add Staff
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3, borderRadius: 2 }}>
        <CardContent sx={{ py: 2 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Role</InputLabel>
              <Select
                value={roleFilter}
                label="Role"
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setPage(0);
                }}
              >
                <MenuItem value="">All Roles</MenuItem>
                <MenuItem value="DRIVER">Drivers</MenuItem>
                <MenuItem value="PLANT_OPERATOR">Plant Operators</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body2" color="text.secondary">
              {staffData?.totalElements ?? 0} staff members
            </Typography>
          </Stack>
        </CardContent>
      </Card>

      {/* Error State */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load staff list. Please try again.
        </Alert>
      )}

      {/* Loading State */}
      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Staff Table */}
      {staffData && (
        <Paper sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow sx={{ bgcolor: 'grey.50' }}>
                  <TableCell sx={{ fontWeight: 600 }}>Username</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Name</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Role</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>GPS</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Last Active</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {staffData.content.map((staff) => (
                  <TableRow 
                    key={staff.id} 
                    hover
                    sx={{ 
                      cursor: 'pointer',
                      opacity: staff.active ? 1 : 0.6,
                    }}
                    onClick={() => navigate(`/cbwtf/staff/${staff.id}`)}
                  >
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 500 }}>
                        {staff.username}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <PersonIcon fontSize="small" color="action" />
                        {staff.fullName}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        icon={roleIcons[staff.role] as React.ReactElement}
                        label={roleLabels[staff.role]}
                        size="small"
                        variant="outlined"
                        color={staff.role === 'DRIVER' ? 'primary' : 'secondary'}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={staff.active ? 'Active' : 'Disabled'}
                        size="small"
                        color={staff.active ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      {staff.gpsStatus === 'ONLINE' ? (
                        <Chip
                          icon={<OnlineIcon />}
                          label="Online"
                          size="small"
                          color="success"
                          variant="outlined"
                        />
                      ) : staff.gpsStatus === 'OFFLINE' ? (
                        <Chip
                          icon={<OfflineIcon />}
                          label="Offline"
                          size="small"
                          color="default"
                          variant="outlined"
                        />
                      ) : (
                        <Typography variant="body2" color="text.secondary">
                          —
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {formatTimeAgo(staff.lastGpsAt)}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View Details">
                        <IconButton 
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/cbwtf/staff/${staff.id}`);
                          }}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
                {staffData.content.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                      <Typography color="text.secondary">
                        No staff members found. Add your first staff member to get started.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={staffData.totalElements}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(parseInt(e.target.value, 10));
              setPage(0);
            }}
            rowsPerPageOptions={[10, 20, 50]}
          />
        </Paper>
      )}

      {/* Create Staff Dialog */}
      <CreateStaffDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSubmit={handleCreateSubmit}
        isLoading={createMutation.isPending}
        error={createMutation.error}
      />

      {/* Password Dialog */}
      <PasswordRevealDialog
        open={!!newStaffPassword}
        password={newStaffPassword}
        onClose={() => {
          setNewStaffPassword(null);
          setCreateDialogOpen(false);
        }}
      />
    </Box>
  );
}

// Create Staff Dialog Component
function CreateStaffDialog({
  open,
  onClose,
  onSubmit,
  isLoading,
  error,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateStaffRequest) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}) {
  const [formData, setFormData] = useState<CreateStaffRequest>({
    fullName: '',
    email: '',
    phone: '',
    role: 'DRIVER',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit(formData);
  };

  const handleClose = () => {
    setFormData({ fullName: '', email: '', phone: '', role: 'DRIVER' });
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Add New Staff Member</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error.message || 'Failed to create staff member'}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Full Name"
              required
              fullWidth
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            />
            <FormControl fullWidth required>
              <InputLabel>Role</InputLabel>
              <Select
                value={formData.role}
                label="Role"
                onChange={(e) => setFormData({ ...formData, role: e.target.value as 'DRIVER' | 'PLANT_OPERATOR' })}
              >
                <MenuItem value="DRIVER">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <DriverIcon fontSize="small" />
                    Driver
                  </Box>
                </MenuItem>
                <MenuItem value="PLANT_OPERATOR">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <PlantIcon fontSize="small" />
                    Plant Operator
                  </Box>
                </MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Email (Optional)"
              type="email"
              fullWidth
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
            <TextField
              label="Phone (Optional)"
              fullWidth
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            />
            <Alert severity="info" icon={false}>
              <Typography variant="body2">
                A unique username will be auto-generated based on your CBWTF code and role.
                <br />
                A temporary password will be displayed after creation.
              </Typography>
            </Alert>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} disabled={isLoading}>Cancel</Button>
          <Button 
            type="submit" 
            variant="contained" 
            disabled={isLoading || !formData.fullName}
          >
            {isLoading ? <CircularProgress size={20} /> : 'Create Staff'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

// Password Reveal Dialog
function PasswordRevealDialog({
  open,
  password,
  onClose,
}: {
  open: boolean;
  password: string | null;
  onClose: () => void;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (password) {
      navigator.clipboard.writeText(password);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Staff Member Created</DialogTitle>
      <DialogContent>
        <Alert severity="success" sx={{ mb: 2 }}>
          Staff member created successfully.
        </Alert>
        <Alert severity="warning" icon={false} sx={{ mb: 2 }}>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>Temporary Password</strong> (shown only once):
          </Typography>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            bgcolor: 'grey.100',
            p: 1.5,
            borderRadius: 1,
            fontFamily: 'monospace',
          }}>
            <Typography variant="h6" sx={{ fontFamily: 'monospace', flex: 1 }}>
              {password}
            </Typography>
            <Tooltip title={copied ? 'Copied!' : 'Copy'}>
              <IconButton onClick={handleCopy} size="small">
                <CopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            The staff member will be required to change this password on first login.
          </Typography>
        </Alert>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">Done</Button>
      </DialogActions>
    </Dialog>
  );
}
