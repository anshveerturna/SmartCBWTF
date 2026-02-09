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
  LocalHospital as HcfIcon,
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

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * HCF List for 0-30 Beds (Small HCFs)
 * These are CBWTF-managed only - NO portal access per regulatory norms.
 */
export default function HcfListSmall() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [cityFilter, setCityFilter] = useState<string>('all');
  const [stateFilter, setStateFilter] = useState<string>('all');
  const [hcfTypeFilter, setHcfTypeFilter] = useState<string>('all');

  const { data: allHcfs, isLoading, error } = useQuery({
    queryKey: ['cbwtf-hcfs'],
    queryFn: getHcfList,
  });

  // Get unique cities, states, and HCF types for filter dropdowns
  const { cities, states, hcfTypes } = useMemo(() => {
    if (!allHcfs) return { cities: [], states: [], hcfTypes: [] };
    const citySet = new Set<string>();
    const stateSet = new Set<string>();
    const typeSet = new Set<string>();
    
    allHcfs.forEach((hcf) => {
      // Small HCFs only for filters
      let isSmallHcf = false;
      if (hcf.bedAccessCategory === 'BEDS_0_TO_30') {
        isSmallHcf = true;
      } else if (hcf.bedAccessCategory === 'ABOVE_30_BEDS') {
        isSmallHcf = false;
      } else {
        isSmallHcf = hcf.numberOfBeds === null || hcf.numberOfBeds <= 30;
      }

      if (isSmallHcf) {
        if (hcf.city) citySet.add(hcf.city);
        if (hcf.state) stateSet.add(hcf.state);
        if (hcf.hcfType) typeSet.add(hcf.hcfType);
      }
    });

    return {
      cities: Array.from(citySet).sort(),
      states: Array.from(stateSet).sort(),
      hcfTypes: Array.from(typeSet).sort(),
    };
  }, [allHcfs]);

  // Filter to only show 0-30 beds HCFs
  const filteredHcfs = useMemo(() => {
    if (!allHcfs) return [];
    
    return allHcfs.filter((hcf) => {
      // Determine if this is a small HCF (0-30 beds)
      let isSmallHcf: boolean;
      
      if (hcf.bedAccessCategory === 'BEDS_0_TO_30') {
        // Explicit category set
        isSmallHcf = true;
      } else if (hcf.bedAccessCategory === 'ABOVE_30_BEDS') {
        // Explicit category set as large
        isSmallHcf = false;
      } else {
        // Category is null/undefined - fall back to bed count
        isSmallHcf = hcf.numberOfBeds === null || hcf.numberOfBeds <= 30;
      }
      
      if (!isSmallHcf) return false;

      // Search filter - now includes address
      const searchLower = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !searchQuery ||
        hcf.name.toLowerCase().includes(searchLower) ||
        hcf.code.toLowerCase().includes(searchLower) ||
        (hcf.agreementNumber && hcf.agreementNumber.toLowerCase().includes(searchLower)) ||
        (hcf.address && hcf.address.toLowerCase().includes(searchLower));

      // Status filter
      const matchesStatus =
        statusFilter === 'all' || hcf.agreementStatus === statusFilter;

      // City filter
      const matchesCity =
        cityFilter === 'all' || (hcf.city && hcf.city === cityFilter);

      // State filter
      const matchesState =
        stateFilter === 'all' || (hcf.state && hcf.state === stateFilter);

      // HCF Type filter
      const matchesType =
        hcfTypeFilter === 'all' || hcf.hcfType === hcfTypeFilter;

      return matchesSearch && matchesStatus && matchesCity && matchesState && matchesType;
    });
  }, [allHcfs, searchQuery, statusFilter, cityFilter, stateFilter, hcfTypeFilter]);

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
            HCFs (0–30 Beds)
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
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap">
            <TextField
              placeholder="Search by name, code, agreement, or address..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="small"
              sx={{ flex: 2, minWidth: 250 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
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
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>City</InputLabel>
              <Select
                value={cityFilter}
                label="City"
                onChange={(e) => setCityFilter(e.target.value)}
              >
                <MenuItem value="all">All Cities</MenuItem>
                {cities.map((city) => (
                  <MenuItem key={city} value={city}>{city}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>State</InputLabel>
              <Select
                value={stateFilter}
                label="State"
                onChange={(e) => setStateFilter(e.target.value)}
              >
                <MenuItem value="all">All States</MenuItem>
                {states.map((state) => (
                  <MenuItem key={state} value={state}>{state}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>HCF Type</InputLabel>
              <Select
                value={hcfTypeFilter}
                label="HCF Type"
                onChange={(e) => setHcfTypeFilter(e.target.value)}
              >
                <MenuItem value="all">All Types</MenuItem>
                <MenuItem value="HOSPITAL">Hospital</MenuItem>
                <MenuItem value="DENTAL">Dental</MenuItem>
                <MenuItem value="CLINIC">Clinic</MenuItem>
                <MenuItem value="PATHOLOGY_COLLECTION">Pathology (Collection)</MenuItem>
                <MenuItem value="PATHOLOGY_STORAGE">Pathology (Storage)</MenuItem>
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
              <TableCell sx={{ fontWeight: 'bold' }}>City</TableCell>
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
                      : 'No HCFs with 0–30 beds found'}
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
                    <Typography variant="body2">
                      {hcf.city || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>{hcf.numberOfBeds || '0'}</TableCell>
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
      {allHcfs && allHcfs.length > 0 && (
        <Box mt={2} display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="body2" color="text.secondary">
            Showing {filteredHcfs.length} HCFs with 0–30 beds
          </Typography>
          <Stack direction="row" spacing={2}>
            <Typography component="div" variant="body2" color="text.secondary">
              <Chip label="Active" color="success" size="small" sx={{ mr: 0.5 }} />
              {filteredHcfs.filter((h) => h.agreementStatus === 'ACTIVE').length}
            </Typography>
            <Typography component="div" variant="body2" color="text.secondary">
              <Chip label="Inactive" color="default" size="small" sx={{ mr: 0.5 }} />
              {filteredHcfs.filter((h) => h.agreementStatus !== 'ACTIVE').length}
            </Typography>
          </Stack>
        </Box>
      )}
    </Box>
  );
}
