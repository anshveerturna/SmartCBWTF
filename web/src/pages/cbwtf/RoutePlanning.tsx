import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  TextField,
  Chip,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Divider,
  InputAdornment,
  Collapse,
  Stepper,
  Step,
  StepLabel,
  Autocomplete,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Close as CloseIcon,
  Search as SearchIcon,
  KeyboardArrowUp as ArrowUpIcon,
  KeyboardArrowDown as ArrowDownIcon,
  Person as PersonIcon,
  Check as CheckIcon,
  Save as SaveIcon,
} from '@mui/icons-material';
import { MapContainer, TileLayer, Marker, Polyline, useMap, Tooltip as MapTooltip } from 'react-leaflet';
import { LatLngBounds } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import {
  listRoutes,
  getRoute,
  createRoute,
  updateRoute,
  setRouteStatus,
  setRouteWaypoints,
  assignRoute,
  unassignRoute,
  getRouteMapData,
  getStaffForSelection,
} from '../../api/cbwtf';
import type {
  RouteDTO,
  RouteDetailDTO,
  RouteMapDataDTO,
  RouteStatus,
  HcfGeoPointDTO,
  RouteExecutionDTO,
} from '../../api/cbwtf';
import { getRouteExecution } from '../../api/cbwtf';
import { hasFiniteCoordinate } from '../../utils/browser';

// ========== MARKER ICONS ==========
const createHcfMarker = (isSelected: boolean, color: string = '#DC2626') => {
  const size = isSelected ? 32 : 24;
  const fillColor = isSelected ? color : '#DC2626';
  return L.divIcon({
    className: '',
    html: `<svg width="${size}" height="${size}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <rect x="2" y="2" width="20" height="20" rx="3" fill="${fillColor}" stroke="white" stroke-width="2"/>
      <path d="M12 7V17M7 12H17" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
    </svg>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
};

const createNumberMarker = (num: number, color: string) => {
  return L.divIcon({
    className: '',
    html: `<div style="background:${color};color:white;width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:12px;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);">${num}</div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13],
  });
};

// ========== CONSTANTS ==========
const ROUTE_COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16'];
const WIZARD_STEPS = ['Route Basics', 'Select HCFs', 'Order Waypoints', 'Assign Staff'];

const statusColors: Record<RouteStatus, 'default' | 'success' | 'warning'> = {
  DRAFT: 'default',
  ACTIVE: 'success',
  TEMPORARILY_SUSPENDED: 'warning',
};

const statusLabels: Record<RouteStatus, string> = {
  DRAFT: 'Draft',
  ACTIVE: 'Active',
  TEMPORARILY_SUSPENDED: 'Suspended',
};

// ========== MAP CONTROLLER ==========
function FitBounds({ bounds }: { bounds: LatLngBounds | null }) {
  const map = useMap();
  useEffect(() => {
    if (bounds?.isValid()) {
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 12 });
    }
  }, [bounds, map]);
  return null;
}

// ========== MAIN COMPONENT ==========
export default function RoutePlanning() {
  // ===== DATA STATE =====
  const [routes, setRoutes] = useState<RouteDTO[]>([]);
  const [selectedRouteId, setSelectedRouteId] = useState<string | null>(null);
  const [routeDetail, setRouteDetail] = useState<RouteDetailDTO | null>(null);
  const [routeExecution, setRouteExecution] = useState<RouteExecutionDTO | null>(null);
  const [mapData, setMapData] = useState<RouteMapDataDTO | null>(null);
  const [staffOptions, setStaffOptions] = useState<{ id: string; name: string }[]>([]);
  const [staffLoadingError, setStaffLoadingError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // ===== FILTER STATE =====
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<RouteStatus | 'ALL'>('ALL');

  // ===== BUILDER STATE =====
  const [builderOpen, setBuilderOpen] = useState(false);
  const [builderMode, setBuilderMode] = useState<'create' | 'edit'>('create');
  const [wizardStep, setWizardStep] = useState(0);
  const [editingRouteId, setEditingRouteId] = useState<string | null>(null);

  // ===== FORM STATE =====
  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formColor, setFormColor] = useState(ROUTE_COLORS[0]);
  const [formCompletionDays, setFormCompletionDays] = useState(1);
  const [formHcfs, setFormHcfs] = useState<string[]>([]);
  const [formStaffId, setFormStaffId] = useState('');

  // ===== LOAD DATA =====
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [routeList, mapResult] = await Promise.all([listRoutes(), getRouteMapData()]);
      setRoutes(routeList);
      setMapData(mapResult);
    } catch (err) {
      setError('Failed to load data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    getStaffForSelection()
      .then(opts => {
        setStaffOptions(opts);
        setStaffLoadingError(null);
      })
      .catch(err => {
        console.error(err);
        setStaffOptions([]);
        setStaffLoadingError('Failed to load staff list');
      });
  }, [loadData]);

  useEffect(() => {
    if (selectedRouteId && !builderOpen) {
      getRoute(selectedRouteId).then(setRouteDetail).catch(console.error);
      getRouteExecution(selectedRouteId).then(setRouteExecution).catch(console.error);
    } else {
      setRouteExecution(null);
    }
  }, [selectedRouteId, builderOpen]);

  // ===== COMPUTED =====
  const filteredRoutes = useMemo(() => {
    return routes.filter(r => {
      if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
      if (searchQuery && !r.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
      return true;
    });
  }, [routes, statusFilter, searchQuery]);

  const allHcfsBounds = useMemo(() => {
    if (!mapData?.hcfs.length) return null;
    const valid = mapData.hcfs.filter(h => hasFiniteCoordinate(h.gpsLat) && hasFiniteCoordinate(h.gpsLon));
    if (!valid.length) return null;
    return new LatLngBounds(valid.map(h => [h.gpsLat!, h.gpsLon!] as [number, number]));
  }, [mapData]);

  const selectedRouteBounds = useMemo(() => {
    if (!routeDetail?.waypoints.length || !mapData) return null;
    const coords = routeDetail.waypoints
      .map(w => mapData.hcfs.find(h => h.id === w.hcfId))
      .filter((h): h is HcfGeoPointDTO => !!h?.gpsLat)
      .map(h => [h.gpsLat!, h.gpsLon!] as [number, number]);
    if (!coords.length) return null;
    return new LatLngBounds(coords);
  }, [routeDetail, mapData]);

  const getHcfById = (id: string) => mapData?.hcfs.find(h => h.id === id);

  // ===== BUILDER ACTIONS =====
  const openBuilderCreate = () => {
    setBuilderMode('create');
    setEditingRouteId(null);
    setFormName('');
    setFormDescription('');
    setFormColor(ROUTE_COLORS[Math.floor(Math.random() * ROUTE_COLORS.length)]);
    setFormCompletionDays(1);
    setFormHcfs([]);
    setFormStaffId('');
    setWizardStep(0);
    setBuilderOpen(true);
    setSelectedRouteId(null);
    setRouteDetail(null);
  };

  const openBuilderEdit = () => {
    if (!routeDetail) return;
    setBuilderMode('edit');
    setEditingRouteId(routeDetail.id);
    setFormName(routeDetail.name);
    setFormDescription(routeDetail.description || '');
    setFormColor(routes.find(r => r.id === routeDetail.id)?.color || ROUTE_COLORS[0]);
    setFormCompletionDays(routes.find(r => r.id === routeDetail.id)?.completionDays || 1);
    setFormHcfs(routeDetail.waypoints.map(w => w.hcfId));
    setFormStaffId(routeDetail.currentAssignment?.staffId || '');
    setWizardStep(0);
    setBuilderOpen(true);
  };

  const closeBuilder = () => {
    setBuilderOpen(false);
    setWizardStep(0);
  };

  const toggleHcfInBuilder = (hcfId: string) => {
    setFormHcfs(prev => prev.includes(hcfId) ? prev.filter(x => x !== hcfId) : [...prev, hcfId]);
  };

  const moveWaypoint = (idx: number, dir: 'up' | 'down') => {
    setFormHcfs(prev => {
      const arr = [...prev];
      const target = dir === 'up' ? idx - 1 : idx + 1;
      if (target < 0 || target >= arr.length) return prev;
      [arr[idx], arr[target]] = [arr[target], arr[idx]];
      return arr;
    });
  };

  const saveBuilder = async () => {
    if (!formName.trim() || formHcfs.length === 0) return;
    try {
      // Sanitize HCF IDs (remove duplicates and empty strings)
      const sanitizedHcfs = Array.from(new Set(formHcfs.filter(id => !!id)));
      
      if (builderMode === 'create') {
        const created = await createRoute({ 
          name: formName, 
          description: formDescription || undefined, 
          color: formColor,
          completionDays: formCompletionDays || 1
        });
        await setRouteWaypoints(created.id, { hcfIds: sanitizedHcfs });
        if (formStaffId) await assignRoute(created.id, { staffId: formStaffId });
        setSelectedRouteId(created.id);
      } else if (editingRouteId) {
        await updateRoute(editingRouteId, { 
          name: formName, 
          description: formDescription || undefined, 
          color: formColor,
          completionDays: formCompletionDays || 1
        });
        await setRouteWaypoints(editingRouteId, { hcfIds: sanitizedHcfs });
        if (formStaffId) await assignRoute(editingRouteId, { staffId: formStaffId });
      }
      closeBuilder();
      loadData();
    } catch (err: unknown) {
      // Extract error message from Spring Boot response if available
      const apiError = err as { response?: { data?: { message?: string; detail?: string } } };
      const msg = apiError.response?.data?.message || apiError.response?.data?.detail || 'Failed to save route';
      setError(msg);
      console.error(err);
    }
  };

  // ===== DETAILS ACTIONS =====
  const handleStatusChange = async (status: RouteStatus) => {
    if (!routeDetail) return;
    try {
      await setRouteStatus(routeDetail.id, status);
      setRouteDetail({ ...routeDetail, status, isActive: status === 'ACTIVE' });
      loadData();
    } catch {
      setError('Failed to update status');
    }
  };

  const handleUnassign = async () => {
    if (!routeDetail) return;
    try {
      await unassignRoute(routeDetail.id);
      const updated = await getRoute(routeDetail.id);
      setRouteDetail(updated);
      loadData();
    } catch {
      setError('Failed to unassign');
    }
  };

  // ===== LOADING =====
  if (loading && !routes.length) {
    return <Box display="flex" justifyContent="center" alignItems="center" height="400px"><CircularProgress /></Box>;
  }

  // ===== RENDER =====
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2, minHeight: 'calc(100vh - 120px)' }}>
      
      {/* ════════════════════════════════════════════════════════════════════
          SECTION 1: ROUTE LIST (TOP - ALWAYS VISIBLE)
          ════════════════════════════════════════════════════════════════════ */}
      <Paper sx={{ p: 2 }}>
        {/* Header Row */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
          <Typography variant="h5" fontWeight={600} sx={{ flexGrow: 1 }}>
            Route Planning
          </Typography>
          <TextField
            size="small"
            placeholder="Search routes..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            sx={{ width: 200 }}
          />
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <Select value={statusFilter} onChange={e => setStatusFilter(e.target.value as RouteStatus | 'ALL')}>
              <MenuItem value="ALL">All Status</MenuItem>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="TEMPORARILY_SUSPENDED">Suspended</MenuItem>
            </Select>
          </FormControl>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openBuilderCreate} disabled={builderOpen}>
            New Route
          </Button>
        </Box>

        {/* Route Cards */}
          <TableContainer sx={{ maxHeight: 400 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Stops</TableCell>
                  <TableCell>Assigned</TableCell>
                  <TableCell>Timeframe</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredRoutes.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">No routes found</Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredRoutes.map(route => {
                    const isSelected = selectedRouteId === route.id;
                    return (
                      <TableRow 
                        key={route.id} 
                        hover 
                        selected={isSelected}
                        onClick={() => {
                          if (!builderOpen) {
                            setSelectedRouteId(isSelected ? null : route.id);
                            if (!isSelected) setRouteDetail(null);
                          }
                        }}
                        sx={{ cursor: builderOpen ? 'default' : 'pointer' }}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: route.color }} />
                            <Typography variant="body2" fontWeight={500}>{route.name}</Typography>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip size="small" label={statusLabels[route.status]} color={statusColors[route.status]} sx={{ height: 24 }} />
                        </TableCell>
                        <TableCell>{route.waypointCount}</TableCell>
                        <TableCell>{route.assignedStaffName || 'Unassigned'}</TableCell>
                        <TableCell>{route.completionDays ? `${route.completionDays} days` : '-'}</TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </TableContainer>
      </Paper>

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 2: ROUTE BUILDER (SLIDE-DOWN - CONTAINS MAP)
          ════════════════════════════════════════════════════════════════════ */}
      <Collapse in={builderOpen} unmountOnExit>
        <Paper sx={{ overflow: 'hidden' }}>
          {/* Builder Header */}
          <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6" fontWeight={600}>
              {builderMode === 'create' ? 'Create New Route' : 'Edit Route'}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button onClick={closeBuilder}>Cancel</Button>
              <Button variant="contained" startIcon={<SaveIcon />} onClick={saveBuilder} disabled={!formName.trim() || formHcfs.length === 0}>
                {builderMode === 'create' ? 'Create Route' : 'Save Changes'}
              </Button>
            </Box>
          </Box>

          {/* Stepper */}
          <Box sx={{ px: 2, py: 1.5, borderBottom: 1, borderColor: 'divider', bgcolor: 'action.hover' }}>
            <Stepper activeStep={wizardStep} alternativeLabel>
              {WIZARD_STEPS.map((label, idx) => (
                <Step key={label} completed={wizardStep > idx} onClick={() => setWizardStep(idx)} sx={{ cursor: 'pointer' }}>
                  <StepLabel>{label}</StepLabel>
                </Step>
              ))}
            </Stepper>
          </Box>

          {/* Builder Content */}
          <Box sx={{ display: 'flex', height: '45vh', minHeight: 350 }}>
            {/* Left: Step Content */}
            <Box sx={{ width: 360, p: 2, borderRight: 1, borderColor: 'divider', overflow: 'auto' }}>
              {/* Step 0: Route Basics */}
              {wizardStep === 0 && (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>Route Name *</Typography>
                  <TextField size="small" fullWidth value={formName} onChange={e => setFormName(e.target.value)} placeholder="Enter route name" sx={{ mb: 2 }} autoFocus />
                  <Typography variant="subtitle2" gutterBottom>Description</Typography>
                  <TextField size="small" fullWidth multiline rows={2} value={formDescription} onChange={e => setFormDescription(e.target.value)} placeholder="Optional description" sx={{ mb: 2 }} />
                  <Typography variant="subtitle2" gutterBottom>Route Color</Typography>
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 2 }}>
                    {ROUTE_COLORS.map(c => (
                      <Box key={c} onClick={() => setFormColor(c)} sx={{ width: 28, height: 28, borderRadius: '50%', bgcolor: c, cursor: 'pointer', border: formColor === c ? 3 : 0, borderColor: 'common.white', boxShadow: formColor === c ? 3 : 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {formColor === c && <CheckIcon sx={{ color: 'white', fontSize: 14 }} />}
                      </Box>
                    ))}
                  </Box>
                  <Typography variant="subtitle2" gutterBottom>Timeframe (Days)</Typography>
                  <TextField 
                    size="small" 
                    type="number" 
                    fullWidth 
                    value={formCompletionDays} 
                    onChange={e => setFormCompletionDays(parseInt(e.target.value) || 1)} 
                    inputProps={{ min: 1 }}
                    helperText="Duration for route completion cycle"
                    sx={{ mb: 2 }} 
                  />
                  <Box sx={{ mt: 3 }}>
                    <Button variant="contained" onClick={() => setWizardStep(1)} disabled={!formName.trim()}>
                      Next: Select HCFs
                    </Button>
                  </Box>
                </Box>
              )}

              {/* Step 1: Select HCFs */}
              {wizardStep === 1 && (
                <Box>
                  <Autocomplete
                    multiple
                    id="hcf-select"
                    options={mapData?.hcfs || []}
                    getOptionLabel={(option) => `${option.name} (${option.code})`}
                    value={formHcfs.map(id => getHcfById(id)).filter((h): h is HcfGeoPointDTO => !!h)}
                    onChange={(_, newValue) => {
                      setFormHcfs(newValue.map(v => v.id));
                    }}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        variant="outlined"
                        label="Search & Select HCFs"
                        placeholder="Type to search..."
                        size="small"
                        sx={{ mb: 2 }}
                      />
                    )}
                    renderOption={(props, option) => {
                      const { key, ...optionProps } = props;
                      return (
                        <li key={key} {...optionProps}>
                          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                            <Typography variant="body2">{option.name}</Typography>
                            <Typography variant="caption" color="text.secondary">{option.code}</Typography>
                          </Box>
                        </li>
                      );
                    }}
                    disableCloseOnSelect
                    limitTags={2}
                    ListboxProps={{ style: { maxHeight: 220 } }}
                    componentsProps={{
                      popper: {
                        modifiers: [
                          {
                            name: 'flip',
                            enabled: false,
                          },
                        ],
                      },
                    }}
                  />

                  <Alert severity="info" sx={{ mb: 2, fontSize: '0.8rem' }}>
                    Click HCF markers on the map to add/remove stops. Selected: <strong>{formHcfs.length}</strong>
                  </Alert>

                  <Typography variant="subtitle2" gutterBottom>Selected HCFs</Typography>
                  {formHcfs.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">No HCFs selected yet. Click markers on the map.</Typography>
                  ) : (
                    <Box sx={{ maxHeight: 200, overflow: 'auto' }}>
                      {formHcfs.map((id, idx) => {
                        const hcf = getHcfById(id);
                        return hcf ? (
                          <Box key={id} sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5, px: 1, bgcolor: 'action.hover', borderRadius: 1, mb: 0.5 }}>
                            <Box sx={{ width: 20, height: 20, borderRadius: '50%', bgcolor: formColor, color: 'white', fontSize: 10, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{idx + 1}</Box>
                            <Typography variant="body2" noWrap sx={{ flex: 1 }}>{hcf.name}</Typography>
                            <IconButton size="small" onClick={() => setFormHcfs(p => p.filter(x => x !== id))}><CloseIcon sx={{ fontSize: 14 }} /></IconButton>
                          </Box>
                        ) : null;
                      })}
                    </Box>
                  )}
                  <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
                    <Button onClick={() => setWizardStep(0)}>Back</Button>
                    <Button variant="contained" onClick={() => setWizardStep(2)} disabled={formHcfs.length === 0}>
                      Next: Order Waypoints
                    </Button>
                  </Box>
                </Box>
              )}

              {/* Step 2: Order Waypoints */}
              {wizardStep === 2 && (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>Waypoint Order</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Use arrows to reorder. The polyline on the map updates live.</Typography>
                  <Box sx={{ maxHeight: 250, overflow: 'auto' }}>
                    {formHcfs.map((id, idx) => {
                      const hcf = getHcfById(id);
                      return hcf ? (
                        <Box key={id} sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.75, px: 1, bgcolor: 'action.hover', borderRadius: 1, mb: 0.5 }}>
                          <Box sx={{ width: 24, height: 24, borderRadius: '50%', bgcolor: formColor, color: 'white', fontSize: 11, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{idx + 1}</Box>
                          <Box sx={{ flex: 1 }}>
                            <Typography variant="body2" fontWeight={500}>{hcf.name}</Typography>
                            <Typography variant="caption" color="text.secondary">{hcf.code}</Typography>
                          </Box>
                          <IconButton size="small" onClick={() => moveWaypoint(idx, 'up')} disabled={idx === 0}><ArrowUpIcon sx={{ fontSize: 16 }} /></IconButton>
                          <IconButton size="small" onClick={() => moveWaypoint(idx, 'down')} disabled={idx === formHcfs.length - 1}><ArrowDownIcon sx={{ fontSize: 16 }} /></IconButton>
                        </Box>
                      ) : null;
                    })}
                  </Box>
                  <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
                    <Button onClick={() => setWizardStep(1)}>Back</Button>
                    <Button variant="contained" onClick={() => setWizardStep(3)}>
                      Next: Assign Staff
                    </Button>
                  </Box>
                </Box>
              )}

              {/* Step 3: Assign Staff */}
              {wizardStep === 3 && (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>Assign Staff</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Select a staff member for this route.</Typography>
                  <FormControl size="small" fullWidth>
                    <InputLabel id="staff-select-label">Staff</InputLabel>
                    <Select 
                      labelId="staff-select-label"
                      value={formStaffId} 
                      label="Staff" 
                      onChange={e => setFormStaffId(e.target.value)}
                    >
                      {staffLoadingError ? (
                        <MenuItem value="" disabled>{staffLoadingError}</MenuItem>
                      ) : staffOptions.length > 0 ? (
                        staffOptions.map(s => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)
                      ) : (
                        <MenuItem value="" disabled>No staff found</MenuItem>
                      )}
                    </Select>
                  </FormControl>
                  <Divider sx={{ my: 3 }} />
                  <Typography variant="subtitle2" gutterBottom>Route Summary</Typography>
                  <Box sx={{ bgcolor: 'action.hover', p: 1.5, borderRadius: 1 }}>
                    <Typography variant="body2"><strong>Name:</strong> {formName}</Typography>
                    <Typography variant="body2"><strong>Stops:</strong> {formHcfs.length}</Typography>
                    <Typography variant="body2"><strong>Staff:</strong> {formStaffId ? staffOptions.find(s => s.id === formStaffId)?.name : 'Not selected'}</Typography>
                  </Box>
                  <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
                    <Button onClick={() => setWizardStep(2)}>Back</Button>
                    <Button variant="contained" startIcon={<SaveIcon />} onClick={saveBuilder} disabled={!formStaffId}>
                      {builderMode === 'create' ? 'Create Route' : 'Save Changes'}
                    </Button>
                  </Box>
                </Box>
              )}
            </Box>

            {/* Right: Live Map */}
            <Box sx={{ flex: 1, position: 'relative' }}>
              <MapContainer center={[22.5, 82.5]} zoom={5} style={{ height: '100%', width: '100%' }}>
                <TileLayer attribution='&copy; OpenStreetMap' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                <FitBounds bounds={allHcfsBounds} />

                {/* Preview polyline */}
                {formHcfs.length > 1 && (() => {
                  const coords = formHcfs.map(id => getHcfById(id)).filter((h): h is HcfGeoPointDTO => !!h && hasFiniteCoordinate(h.gpsLat) && hasFiniteCoordinate(h.gpsLon)).map(h => [h.gpsLat!, h.gpsLon!] as [number, number]);
                  return coords.length >= 2 ? <Polyline positions={coords} pathOptions={{ color: formColor, weight: 4 }} /> : null;
                })()}

                {/* HCF markers */}
                {mapData?.hcfs.filter(h => hasFiniteCoordinate(h.gpsLat) && hasFiniteCoordinate(h.gpsLon)).map(hcf => {
                  const isSelected = formHcfs.includes(hcf.id);
                  return (
                    <Marker key={hcf.id} position={[hcf.gpsLat!, hcf.gpsLon!]} icon={createHcfMarker(isSelected, formColor)} eventHandlers={{ click: () => toggleHcfInBuilder(hcf.id) }}>
                      <MapTooltip direction="top" offset={[0, -12]}>
                        <Typography variant="body2" fontWeight={600}>{hcf.name}</Typography>
                        <Typography variant="caption">{hcf.code}</Typography>
                        <Typography variant="caption" display="block">{isSelected ? '✓ Click to remove' : 'Click to add'}</Typography>
                      </MapTooltip>
                    </Marker>
                  );
                })}

                {/* Numbered markers */}
                {formHcfs.map((id, idx) => {
                  const hcf = getHcfById(id);
                  return hcf && hasFiniteCoordinate(hcf.gpsLat) && hasFiniteCoordinate(hcf.gpsLon)
                    ? <Marker key={`n-${id}`} position={[hcf.gpsLat, hcf.gpsLon]} icon={createNumberMarker(idx + 1, formColor)} zIndexOffset={1000} />
                    : null;
                })}
              </MapContainer>
            </Box>
          </Box>
        </Paper>
      </Collapse>

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 3: ROUTE DETAILS (BOTTOM - VISIBLE WHEN ROUTE SELECTED)
          ════════════════════════════════════════════════════════════════════ */}
      <Collapse in={!!selectedRouteId && !!routeDetail && !builderOpen} unmountOnExit>
        {routeDetail && (
          <Paper>
            {/* Details Header */}
            <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ width: 16, height: 16, borderRadius: '50%', bgcolor: routes.find(r => r.id === routeDetail.id)?.color || '#3B82F6' }} />
                <Typography variant="h6" fontWeight={600}>{routeDetail.name}</Typography>
                <Chip size="small" label={statusLabels[routeDetail.status]} color={statusColors[routeDetail.status]} />
              </Box>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button startIcon={<EditIcon />} onClick={openBuilderEdit}>Edit Route</Button>
                <IconButton onClick={() => { setSelectedRouteId(null); setRouteDetail(null); }}><CloseIcon /></IconButton>
              </Box>
            </Box>

            {/* Details Content */}
            <Box sx={{ display: 'flex' }}>
              {/* Left: Info */}
              <Box sx={{ width: 320, p: 2, borderRight: 1, borderColor: 'divider' }}>
                {routeDetail.description && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="overline" color="text.secondary">Description</Typography>
                    <Typography variant="body2">{routeDetail.description}</Typography>
                  </Box>
                )}

                <Box sx={{ mb: 2 }}>
                  <Typography variant="overline" color="text.secondary">Execution Plan</Typography>
                  <Typography variant="body2">
                    Cycle Duration: <strong>{routeExecution?.completionDays || 1} day(s)</strong>
                  </Typography>
                </Box>

                {routeExecution?.activeCycle && (
                  <Box sx={{ mb: 2, p: 1.5, bgcolor: 'action.hover', borderRadius: 1 }}>
                    <Typography variant="caption" fontWeight={600} display="block" gutterBottom>
                      Current Cycle (#{routeExecution.activeCycle.cycleNumber})
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="caption">Start: {routeExecution.activeCycle.cycleStart}</Typography>
                      <Typography variant="caption">End: {routeExecution.activeCycle.cycleEnd}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                      <Chip label={`${routeExecution.activeCycle.completedWaypoints} Done`} size="small" color="success" sx={{ height: 20, fontSize: '0.7rem' }} />
                      <Chip label={`${routeExecution.activeCycle.missedWaypoints} Missed`} size="small" color={routeExecution.activeCycle.missedWaypoints > 0 ? "error" : "default"} sx={{ height: 20, fontSize: '0.7rem' }} />
                      <Chip label={`${routeExecution.activeCycle.totalWaypoints - routeExecution.activeCycle.completedWaypoints - routeExecution.activeCycle.missedWaypoints} Pending`} size="small" sx={{ height: 20, fontSize: '0.7rem' }} />
                    </Box>
                  </Box>
                )}

                <Typography variant="overline" color="text.secondary">Status</Typography>
                <FormControl size="small" fullWidth sx={{ mb: 2 }}>
                  <Select value={routeDetail.status} onChange={e => handleStatusChange(e.target.value as RouteStatus)}>
                    <MenuItem value="DRAFT">Draft</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="TEMPORARILY_SUSPENDED">Suspended</MenuItem>
                  </Select>
                </FormControl>

                <Typography variant="overline" color="text.secondary">Assignment</Typography>
                {routeDetail.currentAssignment ? (
                  <Box sx={{ bgcolor: 'action.hover', p: 1.5, borderRadius: 1, mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <PersonIcon fontSize="small" color="primary" />
                      <Typography variant="body2" fontWeight={500}>{routeDetail.currentAssignment.staffName}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Button size="small" onClick={openBuilderEdit}>Reassign</Button>
                      <Button size="small" color="error" onClick={handleUnassign}>Unassign</Button>
                    </Box>
                  </Box>
                ) : (
                  <Button size="small" variant="outlined" startIcon={<PersonIcon />} onClick={openBuilderEdit} fullWidth sx={{ mb: 2 }}>
                    Assign Staff
                  </Button>
                )}

                <Typography variant="overline" color="text.secondary">Waypoints ({routeDetail.waypoints.length})</Typography>
                <Box sx={{ maxHeight: 200, overflow: 'auto' }}>
                  {routeDetail.waypoints.map((wp, idx) => {
                    const execLog = routeExecution?.executionLogs.find(l => l.waypointId === wp.id);
                    const isCompleted = execLog?.status === 'COMPLETED';
                    const isMissed = execLog?.status === 'MISSED';
                    
                    return (
                      <Box key={wp.id} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 0.5, px: 1, bgcolor: 'action.hover', borderRadius: 1, mb: 0.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box sx={{ width: 20, height: 20, borderRadius: '50%', bgcolor: routes.find(r => r.id === routeDetail.id)?.color, color: 'white', fontSize: 10, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{idx + 1}</Box>
                          <Box>
                            <Typography variant="body2" fontWeight={500}>{wp.hcfName}</Typography>
                            <Typography variant="caption" color="text.secondary">{wp.hcfCode}</Typography>
                          </Box>
                        </Box>
                        {execLog && (
                          <Chip 
                            label={isCompleted ? 'Done' : isMissed ? 'Missed' : 'Pending'} 
                            size="small" 
                            color={isCompleted ? 'success' : isMissed ? 'error' : 'default'}
                            variant={isCompleted || isMissed ? 'filled' : 'outlined'}
                            sx={{ height: 16, fontSize: '0.65rem' }}
                          />
                        )}
                      </Box>
                    );
                  })}
                </Box>
              </Box>

              {/* Right: Map */}
              <Box sx={{ flex: 1, height: 350 }}>
                <MapContainer center={[22.5, 82.5]} zoom={5} style={{ height: '100%', width: '100%' }}>
                  <TileLayer attribution='&copy; OpenStreetMap' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                  <FitBounds bounds={selectedRouteBounds} />

                  {/* Route polyline */}
                  {(() => {
                    const coords = routeDetail.waypoints
                      .map(w => getHcfById(w.hcfId))
                      .filter((h): h is HcfGeoPointDTO => !!h && hasFiniteCoordinate(h.gpsLat) && hasFiniteCoordinate(h.gpsLon))
                      .map(h => [h.gpsLat!, h.gpsLon!] as [number, number]);
                    const color = routes.find(r => r.id === routeDetail.id)?.color || '#3B82F6';
                    return coords.length >= 2 ? <Polyline positions={coords} pathOptions={{ color, weight: 5 }} /> : null;
                  })()}

                  {/* Numbered waypoint markers */}
                  {routeDetail.waypoints.map((wp, idx) => {
                    const hcf = getHcfById(wp.hcfId);
                    const color = routes.find(r => r.id === routeDetail.id)?.color || '#3B82F6';
                    return hcf && hasFiniteCoordinate(hcf.gpsLat) && hasFiniteCoordinate(hcf.gpsLon) ? (
                      <Marker key={wp.id} position={[hcf.gpsLat, hcf.gpsLon]} icon={createNumberMarker(idx + 1, color)}>
                        <MapTooltip direction="top" offset={[0, -15]}>
                          <Typography variant="body2" fontWeight={600}>Stop {idx + 1}: {hcf.name}</Typography>
                        </MapTooltip>
                      </Marker>
                    ) : null;
                  })}
                </MapContainer>
              </Box>
            </Box>
          </Paper>
        )}
      </Collapse>

      {/* Error Alert */}
      {error && <Alert severity="error" onClose={() => setError(null)} sx={{ position: 'fixed', bottom: 16, right: 16, zIndex: 9999 }}>{error}</Alert>}
    </Box>
  );
}
