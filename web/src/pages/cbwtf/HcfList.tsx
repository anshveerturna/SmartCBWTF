import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as ViewIcon,
  Business as HcfIcon,
  Pending as PendingIcon,
} from '@mui/icons-material';
import { getHcfList } from '../../api/cbwtf';

const getStatusColor = (status: string | null) => {
  switch (status) {
    case 'ACTIVE':
      return 'success';
    case 'EXPIRED':
      return 'warning';
    case 'TERMINATED':
      return 'error';
    case 'DISPUTED':
      return 'error';
    default:
      return 'default';
  }
};

const getDuesColor = (duesStatus: string | null) => {
  switch (duesStatus) {
    case 'CLEAR':
      return 'success';
    case 'PENDING':
      return 'warning';
    case 'DISPUTED':
      return 'error';
    default:
      return 'default';
  }
};

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

export default function HcfList() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  const { data: hcfs, isLoading, error } = useQuery({
    queryKey: ['cbwtf-hcfs'],
    queryFn: getHcfList,
  });

  const filteredHcfs = useMemo(() => {
    if (!hcfs) return [];
    
    return hcfs.filter((hcf) => {
      // Search filter
      const searchLower = searchQuery.toLowerCase();
      const matchesSearch =
        !searchQuery ||
        hcf.name.toLowerCase().includes(searchLower) ||
        hcf.code.toLowerCase().includes(searchLower) ||
        (hcf.agreementNumber && hcf.agreementNumber.toLowerCase().includes(searchLower));

      // Status filter
      const matchesStatus =
        statusFilter === 'all' || hcf.agreementStatus === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [hcfs, searchQuery, statusFilter]);

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load HCFs. Please try again later.
      </Alert>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <HcfIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" fontWeight="bold">
            HCF Management
          </Typography>
        </Box>
        <Stack direction="row" spacing={2}>
          <Button
            variant="contained"
            startIcon={<HcfIcon />}
            onClick={() => navigate('/cbwtf/hcfs/register')}
          >
            Register HCF
          </Button>
          <Button
            variant="outlined"
            startIcon={<PendingIcon />}
            onClick={() => navigate('/cbwtf/hcfs/pending')}
          >
            Pending Approvals
          </Button>
        </Stack>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
            <TextField
              placeholder="Search by name, code, or agreement..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="small"
              sx={{ flex: 2, minWidth: 300 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="all">All Statuses</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="EXPIRED">Expired</MenuItem>
                <MenuItem value="TERMINATED">Terminated</MenuItem>
                <MenuItem value="DISPUTED">Disputed</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </CardContent>
      </Card>

      {/* HCF Table */}
      <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.100' }}>
              <TableCell sx={{ fontWeight: 'bold' }}>HCF Name</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Code</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Agreement #</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Dues</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Beds</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Last Pickup</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }} align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredHcfs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    {searchQuery || statusFilter !== 'all'
                      ? 'No HCFs match your filters'
                      : 'No HCFs found'}
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              filteredHcfs.map((hcf) => (
                <TableRow
                  key={hcf.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/cbwtf/hcfs/${hcf.id}`)}
                >
                  <TableCell>
                    <Typography fontWeight="medium">{hcf.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {hcf.address}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {hcf.code}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {hcf.agreementNumber || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={hcf.agreementStatus || 'N/A'}
                      color={getStatusColor(hcf.agreementStatus) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={hcf.duesStatus || 'N/A'}
                      color={getDuesColor(hcf.duesStatus) as any}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>{hcf.numberOfBeds || '-'}</TableCell>
                  <TableCell>
                    {hcf.lastPickupAt ? formatDate(hcf.lastPickupAt) : '-'}
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="View Details">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/cbwtf/hcfs/${hcf.id}`);
                        }}
                      >
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Summary */}
      {hcfs && hcfs.length > 0 && (
        <Box mt={2} display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="body2" color="text.secondary">
            Showing {filteredHcfs.length} of {hcfs.length} HCFs
          </Typography>
          <Stack direction="row" spacing={2}>
            <Typography component="div" variant="body2" color="text.secondary">
              <Chip label="Active" color="success" size="small" sx={{ mr: 0.5 }} />
              {hcfs.filter((h) => h.agreementStatus === 'ACTIVE').length}
            </Typography>
            <Typography component="div" variant="body2" color="text.secondary">
              <Chip label="Expired" color="error" size="small" sx={{ mr: 0.5 }} />
              {hcfs.filter((h) => h.agreementStatus !== 'ACTIVE').length}
            </Typography>
          </Stack>
        </Box>
      )}
    </Box>
  );
}
