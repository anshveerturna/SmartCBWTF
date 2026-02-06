import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
  CircularProgress,
  Paper,
  alpha,
  Checkbox,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Stack,
  Divider,
} from '@mui/material';
import {
  QrCode,
  Add,
  Download,
  CheckCircle,
  Warning,
  Schedule,
  Visibility,
  Block,
  PictureAsPdf,
  Receipt,
  ShoppingCart,
  History,
} from '@mui/icons-material';
import CurrencyRupeeIcon from '@mui/icons-material/CurrencyRupee';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';
import QRCode from 'qrcode';

// Waste categories with colors
const WASTE_CATEGORIES = [
  { code: 'YELLOW', name: 'Infectious Waste', color: '#FFEB3B' },
  { code: 'RED', name: 'Contaminated Recyclables', color: '#F44336' },
  { code: 'BLUE', name: 'Glassware Waste', color: '#2196F3' },
  { code: 'WHITE', name: 'Sharps Waste', color: '#9E9E9E' },
];

interface QrPricing {
  selfGeneratePrice: number;
  cbwtfRequestPrice: number;
  maxQuantity: number;
}

interface QrOrder {
  id: string;
  wasteCategory: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  orderType: 'HCF_SELF' | 'CBWTF_REQUEST';
  status: string;
  pdfUrl?: string;
  requestedAt: string;
  notes?: string;
}

const QrLabels: React.FC = () => {
  const queryClient = useQueryClient();
  const [category, setCategory] = useState('YELLOW');
  const [quantity, setQuantity] = useState<number>(50);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Selection state
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  
  // View dialog state
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<QrOrder | null>(null);

  // Fetch pricing
  const { data: pricing } = useQuery<QrPricing>({
    queryKey: ['qr-pricing'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/qr-orders/pricing');
      return res.data;
    },
  });

  // Fetch orders
  const { data: orders, isLoading } = useQuery<QrOrder[]>({
    queryKey: ['hcf-qr-orders'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/qr-orders');
      return res.data;
    },
  });

  // Generate mutation
  const generateMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post('/api/hcf/qr-orders/generate', {
        wasteCategory: category,
        quantity: quantity
      });
      return res.data;
    },
    onSuccess: (data) => {
      setSuccess(data.message || 'QR labels generated successfully');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['hcf-qr-orders'] });
      // Open PDF in new tab if available
      if (data.pdfUrl) {
        window.open(getDownloadUrl(data.pdfUrl), '_blank');
      }
    },
    onError: (err: any) => {
      setError(err.response?.data?.error || 'Failed to generate labels');
      setSuccess(null);
    },
  });

  // Request from CBWTF mutation
  const requestMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post('/api/hcf/qr-orders/request', {
        wasteCategory: category,
        quantity: quantity,
        notes: 'Requested from HCF Portal'
      });
      return res.data;
    },
    onSuccess: (data) => {
      setSuccess(data.message || 'Request submitted successfully');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['hcf-qr-orders'] });
    },
    onError: (err: any) => {
      setError(err.response?.data?.error || 'Failed to submit request');
      setSuccess(null);
    },
  });

  const handleGenerate = () => {
    if (!pricing) return;
    if (quantity > pricing.maxQuantity) {
      setError(`Maximum quantity allowed is ${pricing.maxQuantity}`);
      return;
    }
    setError(null);
    setSuccess(null);
    generateMutation.mutate();
  };

  const handleRequest = () => {
    if (!pricing) return;
    setError(null);
    setSuccess(null);
    requestMutation.mutate();
  };

  const calculateTotal = (isSelf: boolean) => {
    if (!pricing || !quantity) return 0;
    const price = isSelf ? pricing.selfGeneratePrice : pricing.cbwtfRequestPrice;
    return price * quantity;
  };

  const getStatusChip = (status: string) => {
    switch (status) {
      case 'FULFILLED': return <Chip label="Fulfilled" color="success" size="small" />;
      case 'PENDING': return <Chip label="Pending" color="warning" size="small" />;
      case 'REJECTED': return <Chip label="Rejected" color="error" size="small" />;
      default: return <Chip label={status} size="small" />;
    }
  };

  const getCategoryColor = (code: string) => {
    return WASTE_CATEGORIES.find(c => c.code === code)?.color || '#9E9E9E';
  };

  const getDownloadUrl = (url: string) => {
    if (!url) return '';
    // If it's already a correct relative URL
    if (url.startsWith('/files/')) return url;
    
    // If it's a local absolute path (legacy data), extract filename
    const parts = url.split(/[/\\]/); // Split by forward or backward slash
    const filename = parts[parts.length - 1];
    return `/files/${filename}`;
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          QR Labels
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Generate or request chargeable QR labels for waste collection
        </Typography>
      </Box>

      {/* Alerts */}
      {error && <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      {/* Pricing & Actions */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Self Generate Option */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%', border: '1px solid', borderColor: 'primary.light', bgcolor: alpha('#6366F1', 0.02) }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <QrCode color="primary" sx={{ mr: 1, fontSize: 28 }} />
                <Typography variant="h6">Self Generate</Typography>
                <Chip label="Instant" size="small" color="success" sx={{ ml: 'auto' }} />
              </Box>
              
              <Typography variant="body2" color="text.secondary" paragraph>
                Generate and download QR labels instantly. Charges will be added to your monthly bill.
              </Typography>

              <Box sx={{ my: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary">Price per Label</Typography>
                <Typography variant="h4" color="primary.main">
                  ₹{pricing?.selfGeneratePrice?.toFixed(2) || '0.00'}
                </Typography>
              </Box>

              <Stack spacing={2}>
                <FormControl fullWidth size="small">
                  <InputLabel>Waste Category</InputLabel>
                  <Select
                    value={category}
                    label="Waste Category"
                    onChange={(e) => setCategory(e.target.value)}
                  >
                    {WASTE_CATEGORIES.map((cat) => (
                      <MenuItem key={cat.code} value={cat.code}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: cat.color }} />
                          {cat.name}
                        </Box>
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <TextField
                  label="Quantity"
                  type="number"
                  size="small"
                  fullWidth
                  value={quantity}
                  onChange={(e) => setQuantity(parseInt(e.target.value) || 0)}
                  helperText={`Total Charge: ₹${calculateTotal(true).toFixed(2)}`}
                />

                <Button
                  variant="contained"
                  fullWidth
                  startIcon={generateMutation.isPending ? <CircularProgress size={20} color="inherit" /> : <Download />}
                  onClick={handleGenerate}
                  disabled={generateMutation.isPending || !quantity}
                >
                  Generate & Pay Later
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* CBWTF Request Option */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <ShoppingCart color="secondary" sx={{ mr: 1, fontSize: 28 }} />
                <Typography variant="h6">Request from CBWTF</Typography>
                <Chip label="Wait time applies" size="small" sx={{ ml: 'auto' }} />
              </Box>
              
              <Typography variant="body2" color="text.secondary" paragraph>
                Request printed labels from the facility. Higher charges apply for printing and delivery.
              </Typography>

              <Box sx={{ my: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle2" color="text.secondary">Price per Label</Typography>
                <Typography variant="h4" color="secondary.main">
                  ₹{pricing?.cbwtfRequestPrice?.toFixed(2) || '0.00'}
                </Typography>
              </Box>

              <Button
                variant="outlined"
                color="secondary"
                fullWidth
                size="large"
                startIcon={requestMutation.isPending ? <CircularProgress size={20} /> : <Receipt />}
                onClick={handleRequest}
                disabled={requestMutation.isPending || !quantity}
                sx={{ mt: 2 }}
              >
                Request Order (₹{calculateTotal(false).toFixed(2)})
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Orders History */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <History sx={{ mr: 1 }} />
            <Typography variant="h6">Order History</Typography>
          </Box>

          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Qty</TableCell>
                  <TableCell>Amount</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {isLoading ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <CircularProgress size={24} sx={{ my: 2 }} />
                    </TableCell>
                  </TableRow>
                ) : orders?.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                      No orders found
                    </TableCell>
                  </TableRow>
                ) : (
                  orders?.map((order) => (
                    <TableRow key={order.id} hover>
                      <TableCell>{new Date(order.requestedAt).toLocaleDateString()}</TableCell>
                      <TableCell>
                        {order.orderType === 'HCF_SELF' ? 'Self Generated' : 'Requested'}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: getCategoryColor(order.wasteCategory) }} />
                          {order.wasteCategory}
                        </Box>
                      </TableCell>
                      <TableCell>{order.quantity}</TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="bold">
                          ₹{order.totalAmount.toFixed(2)}
                        </Typography>
                      </TableCell>
                      <TableCell>{getStatusChip(order.status)}</TableCell>
                      <TableCell align="right">
                        {order.pdfUrl && (
                          <Tooltip title="Download PDF">
                            <IconButton 
                              size="small" 
                              color="primary" 
                              onClick={() => order.pdfUrl && window.open(getDownloadUrl(order.pdfUrl), '_blank')}
                            >
                              <PictureAsPdf />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default QrLabels;
