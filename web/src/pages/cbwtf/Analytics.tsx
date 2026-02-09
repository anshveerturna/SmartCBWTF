import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  TablePagination,
  Tabs,
  Tab,
} from '@mui/material';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import {
  getAnalyticsTotalWaste,
  getAnalyticsWasteByCategory,
  getAnalyticsActiveHcfs,
  getAnalyticsProcessedBags,
} from '../../api/cbwtf';
import type {
  TotalWasteResponse,
  WasteByCategoryResponse,
  HcfOption,
  ProcessedBagsResponse,
} from '../../api/cbwtf';

// Time range options
const TIME_RANGES = [
  { label: 'Today', days: 0 },
  { label: 'Past 7 Days', days: 7 },
  { label: 'Past 1 Month', days: 30 },
  { label: 'Past 3 Months', days: 90 },
  { label: 'Past 6 Months', days: 180 },
  { label: 'Past 1 Year', days: 365 },
];

// Category colors
const CATEGORY_COLORS: Record<string, string> = {
  YELLOW: '#FFC107',
  RED: '#F44336',
  BLUE: '#2196F3',
  WHITE: '#9E9E9E',
};

const formatDate = (date: Date): string => {
  return date.toISOString().split('T')[0];
};

const Analytics = () => {
  const [timeRange, setTimeRange] = useState(30);
  const [selectedHcf, setSelectedHcf] = useState<string>('');
  const [activeTab, setActiveTab] = useState(0);
  const [bagsPage, setBagsPage] = useState(0);
  const [bagsPageSize, setBagsPageSize] = useState(20);

  // Calculate date range
  const dateRange = useMemo(() => {
    const to = new Date();
    const from = new Date();
    if (timeRange === 0) {
      from.setHours(0, 0, 0, 0);
    } else {
      from.setDate(from.getDate() - timeRange);
    }
    return {
      from: formatDate(from),
      to: formatDate(to),
    };
  }, [timeRange]);

  // Fetch HCF options
  const {
    data: hcfOptions,
    isLoading: hcfLoading,
    error: hcfError,
  } = useQuery<HcfOption[]>({
    queryKey: ['analytics-hcfs'],
    queryFn: getAnalyticsActiveHcfs,
  });

  // Fetch total waste
  const {
    data: totalWaste,
    isLoading: totalWasteLoading,
    error: totalWasteError,
  } = useQuery<TotalWasteResponse>({
    queryKey: ['analytics-total-waste', dateRange.from, dateRange.to, selectedHcf],
    queryFn: () => getAnalyticsTotalWaste(dateRange.from, dateRange.to, selectedHcf || undefined),
  });

  // Fetch waste by category
  const {
    data: categoryData,
    isLoading: categoryLoading,
    error: categoryError,
  } = useQuery<WasteByCategoryResponse>({
    queryKey: ['analytics-category', dateRange.from, dateRange.to, selectedHcf],
    queryFn: () => getAnalyticsWasteByCategory(dateRange.from, dateRange.to, selectedHcf || undefined),
  });

  // Fetch processed bags list
  const {
    data: processedBagsData,
    isLoading: processedBagsLoading,
    error: processedBagsError,
  } = useQuery<ProcessedBagsResponse>({
    queryKey: ['analytics-processed-bags', dateRange.from, dateRange.to, selectedHcf, bagsPage, bagsPageSize],
    queryFn: () => getAnalyticsProcessedBags(dateRange.from, dateRange.to, selectedHcf || undefined, bagsPage, bagsPageSize),
    enabled: activeTab === 1, // Only fetch when bags tab is active
  });

  const isLoading = hcfLoading || totalWasteLoading || categoryLoading;
  const hasError = hcfError || totalWasteError || categoryError;

  // Prepare chart data
  const pieChartData = useMemo(() => {
    if (!categoryData?.categories) return [];
    return categoryData.categories.map((cat) => ({
      name: cat.category,
      value: Number(cat.weightKg),
      percent: Number(cat.percentContribution),
      color: CATEGORY_COLORS[cat.category] || '#9E9E9E',
    }));
  }, [categoryData]);

  const barChartData = useMemo(() => {
    if (!categoryData?.categories) return [];
    return categoryData.categories.map((cat) => ({
      category: cat.category,
      weight: Number(cat.weightKg),
      percent: Number(cat.percentContribution),
      fill: CATEGORY_COLORS[cat.category] || '#9E9E9E',
    }));
  }, [categoryData]);

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 600 }}>
        Waste Analytics
      </Typography>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel>Time Range</InputLabel>
                <Select
                  value={timeRange}
                  label="Time Range"
                  onChange={(e) => setTimeRange(Number(e.target.value))}
                >
                  {TIME_RANGES.map((range) => (
                    <MenuItem key={range.days} value={range.days}>
                      {range.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel>HCF Filter</InputLabel>
                <Select
                  value={selectedHcf}
                  label="HCF Filter"
                  onChange={(e) => setSelectedHcf(e.target.value)}
                >
                  <MenuItem value="">All HCFs</MenuItem>
                  {hcfOptions?.map((hcf) => (
                    <MenuItem key={hcf.id} value={hcf.id}>
                      {hcf.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4} md={6}>
              <Typography variant="body2" color="text.secondary">
                Period: {totalWaste?.periodLabel || `${dateRange.from} to ${dateRange.to}`}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Error State */}
      {hasError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load analytics data. Please try again.
        </Alert>
      )}

      {/* Loading State */}
      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Metrics Cards */}
      {!isLoading && !hasError && (
        <>
          <Grid container spacing={3} sx={{ mb: 3 }}>
            {/* Total Waste Card */}
            <Grid item xs={12} sm={6} md={4}>
              <Card sx={{ height: '100%' }}>
                <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1, fontWeight: 600 }}>
                    Total Waste Collected
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
                    <Typography variant="h3" sx={{ fontWeight: 700, color: 'primary.main' }}>
                      {Number(totalWaste?.totalWeightKg || 0).toFixed(2)}
                    </Typography>
                    <Typography variant="h6" color="text.secondary">
                      KG
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Active HCFs Card */}
            <Grid item xs={12} sm={6} md={4}>
              <Card sx={{ height: '100%' }}>
                <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1, fontWeight: 600 }}>
                    Active HCFs
                  </Typography>
                  <Box>
                    <Typography variant="h3" sx={{ fontWeight: 700, color: 'success.main' }}>
                      {hcfOptions?.length || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      With ACTIVE agreements
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Categories Tracked Card */}
            <Grid item xs={12} sm={6} md={4}>
              <Card sx={{ height: '100%' }}>
                <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1, fontWeight: 600 }}>
                    Categories Tracked
                  </Typography>
                  <Box>
                    <Typography variant="h3" sx={{ fontWeight: 700, color: 'info.main' }}>
                      {categoryData?.categories?.length || 0}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Waste categories
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Charts Row */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            {/* Pie Chart */}
            <Grid item xs={12} md={6}>
              <Card sx={{ height: 400 }}>
                <CardContent sx={{ height: '100%' }}>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Waste by Category (KG)
                  </Typography>
                  {pieChartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={300}>
                      <PieChart>
                        <Pie
                          data={pieChartData}
                          cx="50%"
                          cy="50%"
                          labelLine={false}
                          label={({ name, percent }) => `${name}: ${(percent ?? 0).toFixed(1)}%`}
                          outerRadius={100}
                          dataKey="value"
                        >
                          {pieChartData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(value) => `${Number(value ?? 0).toFixed(2)} KG`} />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 300 }}>
                      <Typography color="text.secondary">No data available</Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>

            {/* Bar Chart */}
            <Grid item xs={12} md={6}>
              <Card sx={{ height: 400 }}>
                <CardContent sx={{ height: '100%' }}>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Category Contribution (%)
                  </Typography>
                  {barChartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={300}>
                      <BarChart data={barChartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="category" />
                        <YAxis domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                        <Tooltip formatter={(value) => `${Number(value ?? 0).toFixed(2)}%`} />
                        <Bar dataKey="percent" name="Contribution">
                          {barChartData.map((entry, index) => (
                            <Cell key={`bar-${index}`} fill={entry.fill} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 300 }}>
                      <Typography color="text.secondary">No data available</Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Tabs for Tables */}
          <Card>
            <CardContent>
              <Tabs
                value={activeTab}
                onChange={(_, newValue) => {
                  setActiveTab(newValue);
                  if (newValue === 1) {
                    setBagsPage(0); // Reset page when switching to bags tab
                  }
                }}
                sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
              >
                <Tab label="Category Breakdown" />
                <Tab label="Bags Processed List" />
              </Tabs>

              {/* Category Breakdown Table */}
              {activeTab === 0 && (
                <>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Category Breakdown Details
                  </Typography>
                  <TableContainer component={Paper} variant="outlined">
                    <Table>
                      <TableHead>
                        <TableRow sx={{ bgcolor: 'action.selected' }}>
                          <TableCell>Category</TableCell>
                          <TableCell align="right">Weight (KG)</TableCell>
                          <TableCell align="right">Contribution (%)</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {categoryData?.categories?.map((cat) => (
                          <TableRow key={cat.category} hover>
                            <TableCell>
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Box
                                  sx={{
                                    width: 12,
                                    height: 12,
                                    borderRadius: '50%',
                                    bgcolor: CATEGORY_COLORS[cat.category] || '#9E9E9E',
                                  }}
                                />
                                <Chip
                                  label={cat.category}
                                  size="small"
                                  sx={{
                                    bgcolor: CATEGORY_COLORS[cat.category] || '#9E9E9E',
                                    color: cat.category === 'WHITE' ? '#000' : '#fff',
                                  }}
                                />
                              </Box>
                            </TableCell>
                            <TableCell align="right">
                              <Typography fontWeight={500}>
                                {Number(cat.weightKg).toFixed(2)}
                              </Typography>
                            </TableCell>
                            <TableCell align="right">
                              <Typography fontWeight={500}>
                                {Number(cat.percentContribution).toFixed(2)}%
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ))}
                        {(!categoryData?.categories || categoryData.categories.length === 0) && (
                          <TableRow>
                            <TableCell colSpan={3} align="center">
                              <Typography color="text.secondary" sx={{ py: 2 }}>
                                No data available for the selected period
                              </Typography>
                            </TableCell>
                          </TableRow>
                        )}
                        {categoryData?.categories && categoryData.categories.length > 0 && (
                          <TableRow sx={{ bgcolor: 'action.hover' }}>
                            <TableCell>
                              <Typography fontWeight={700}>TOTAL</Typography>
                            </TableCell>
                            <TableCell align="right">
                              <Typography fontWeight={700}>
                                {Number(categoryData.grandTotalKg).toFixed(2)}
                              </Typography>
                            </TableCell>
                            <TableCell align="right">
                              <Typography fontWeight={700}>100.00%</Typography>
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </>
              )}

              {/* Bags Processed List Table */}
              {activeTab === 1 && (
                <>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Bags Processed List
                  </Typography>
                  {processedBagsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                      <CircularProgress />
                    </Box>
                  ) : processedBagsError ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                      Failed to load processed bags data. Please try again.
                    </Alert>
                  ) : (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow sx={{ bgcolor: 'grey.100' }}>
                              <TableCell>Waste Category</TableCell>
                              <TableCell>QR Code / Bag ID</TableCell>
                              <TableCell align="right">Weight (KG)</TableCell>
                              <TableCell>Timestamp</TableCell>
                              <TableCell>Staff Name</TableCell>
                              <TableCell>HCF Name</TableCell>
                              <TableCell>Event Type</TableCell>
                              <TableCell>Status</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {processedBagsData?.bags?.map((bag) => (
                              <TableRow key={bag.id} hover>
                                <TableCell>
                                  <Chip
                                    label={bag.category}
                                    size="small"
                                    sx={{
                                      bgcolor: CATEGORY_COLORS[bag.category] || '#9E9E9E',
                                      color: bag.category === 'WHITE' ? '#000' : '#fff',
                                    }}
                                  />
                                </TableCell>
                                <TableCell>
                                  <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                                    {bag.qrCode}
                                  </Typography>
                                </TableCell>
                                <TableCell align="right">
                                  <Typography fontWeight={500}>
                                    {Number(bag.weightKg).toFixed(3)}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <Typography variant="body2">
                                    {bag.timestamp}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <Typography variant="body2">
                                    {bag.staffName}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <Typography variant="body2" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {bag.hcfName}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <Chip
                                    label={bag.eventType === 'HCF_COLLECTION' ? 'Collection' : 'Verification'}
                                    size="small"
                                    color={bag.eventType === 'HCF_COLLECTION' ? 'primary' : 'success'}
                                    variant="outlined"
                                  />
                                </TableCell>
                                <TableCell>
                                  <Chip
                                    label={bag.anomalyState || 'OK'}
                                    size="small"
                                    color={
                                      bag.anomalyState === 'OK' || !bag.anomalyState
                                        ? 'success'
                                        : bag.anomalyState === 'OUT_OF_GEOFENCE'
                                        ? 'warning'
                                        : 'error'
                                    }
                                    variant="filled"
                                  />
                                </TableCell>
                              </TableRow>
                            ))}
                            {(!processedBagsData?.bags || processedBagsData.bags.length === 0) && (
                              <TableRow>
                                <TableCell colSpan={8} align="center">
                                  <Typography color="text.secondary" sx={{ py: 2 }}>
                                    No bags processed for the selected period
                                  </Typography>
                                </TableCell>
                              </TableRow>
                            )}
                          </TableBody>
                        </Table>
                      </TableContainer>
                      {processedBagsData && processedBagsData.totalCount > 0 && (
                        <TablePagination
                          component="div"
                          count={processedBagsData.totalCount}
                          page={bagsPage}
                          onPageChange={(_, newPage) => setBagsPage(newPage)}
                          rowsPerPage={bagsPageSize}
                          onRowsPerPageChange={(e) => {
                            setBagsPageSize(parseInt(e.target.value, 10));
                            setBagsPage(0);
                          }}
                          rowsPerPageOptions={[10, 20, 50, 100]}
                        />
                      )}
                    </>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </Box>
  );
};

export default Analytics;
