import React, { useState } from 'react';
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
  Chip,
  IconButton,
  Button,
  CircularProgress,
  Paper,
  alpha,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Collapse,
  Tabs,
  Tab,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import {
  CheckCircle,
  LocalShipping,
  Cancel,
  ExpandMore,
  ExpandLess,
  Inventory,
  Receipt,
  Download,
  History,
  ShoppingCart,
} from '@mui/icons-material';
import CurrencyRupeeIcon from '@mui/icons-material/CurrencyRupee';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface OrderItem {
  name: string;
  quantity: number;
  unit: string;
  pricePerUnit: number;
  gstRate: number;
  lineTotal: number;
  imageUrl?: string;
}

interface Order {
  id: string;
  orderNumber: string;
  status: string;
  itemCount: number;
  subtotal: number;
  gstAmount: number;
  totalAmount: number;
  orderedAt: string;
  confirmedAt?: string;
  dispatchedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  hcfId: string;
  hcfName: string;
  hcfCode: string;
  hcfAddress: string;
  agreementNumber?: string;
  items?: OrderItem[];
  hcfNotes?: string;
  cbwtfNotes?: string;
}

const ConsumableOrders: React.FC = () => {
  const queryClient = useQueryClient();
  const [mainTab, setMainTab] = useState(0); // 0 = Orders, 1 = Analytics
  const [tab, setTab] = useState(0);
  const [expandedOrder, setExpandedOrder] = useState<string | null>(null);
  const [actionDialog, setActionDialog] = useState<{ open: boolean; orderId: string; action: string }>({
    open: false, orderId: '', action: ''
  });
  const [notes, setNotes] = useState('');
  const [analyticsPeriod, setAnalyticsPeriod] = useState<'day' | 'week' | 'month'>('month');

  const statusFilters = ['', 'PENDING', 'CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'];
  const statusFilter = statusFilters[tab];

  // Fetch orders
  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['cbwtf-consumable-orders', statusFilter],
    queryFn: async () => {
      const url = statusFilter 
        ? `/api/cbwtf/consumable-orders?status=${statusFilter}`
        : '/api/cbwtf/consumable-orders';
      const res = await apiClient.get(url);
      return res.data as { orders: Order[]; total: number };
    },
  });

  // Fetch analytics
  const { data: analyticsData, isLoading: analyticsLoading } = useQuery({
    queryKey: ['cbwtf-consumable-analytics', analyticsPeriod],
    queryFn: async () => {
      const res = await apiClient.get(`/api/cbwtf/consumable-orders/analytics?period=${analyticsPeriod}`);
      return res.data as {
        period: string;
        totalOrders: number;
        totalAmount: number;
        statusBreakdown: Record<string, number>;
        dailyBreakdown: Record<string, number>;
        orders: Order[];
      };
    },
    enabled: mainTab === 1,
  });

  // Fetch order details when expanded
  const { data: orderDetails } = useQuery({
    queryKey: ['cbwtf-consumable-order-detail', expandedOrder],
    queryFn: async () => {
      if (!expandedOrder) return null;
      const res = await apiClient.get(`/api/cbwtf/consumable-orders/${expandedOrder}`);
      return res.data as Order;
    },
    enabled: !!expandedOrder,
  });

  // Action mutations
  const actionMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: string }) => {
      const res = await apiClient.post(`/api/cbwtf/consumable-orders/${id}/${action}`, { notes });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-consumable-orders'] });
      setActionDialog({ open: false, orderId: '', action: '' });
      setNotes('');
    },
  });

  const getStatusChip = (status: string) => {
    switch (status) {
      case 'PENDING': return <Chip label="Pending" color="warning" size="small" />;
      case 'CONFIRMED': return <Chip label="Confirmed" color="info" size="small" />;
      case 'DISPATCHED': return <Chip icon={<LocalShipping />} label="Dispatched" color="primary" size="small" />;
      case 'DELIVERED': return <Chip icon={<CheckCircle />} label="Delivered" color="success" size="small" />;
      case 'CANCELLED': return <Chip label="Cancelled" color="default" size="small" />;
      default: return <Chip label={status} size="small" />;
    }
  };

  const getActionButtons = (order: Order) => {
    const buttons = [];
    if (order.status === 'PENDING') {
      buttons.push(
        <Button key="confirm" size="small" variant="contained" color="success" startIcon={<CheckCircle />}
          onClick={() => setActionDialog({ open: true, orderId: order.id, action: 'confirm' })}>
          Confirm
        </Button>,
        <Button key="cancel" size="small" variant="outlined" color="error" startIcon={<Cancel />}
          onClick={() => setActionDialog({ open: true, orderId: order.id, action: 'cancel' })}>
          Cancel
        </Button>
      );
    } else if (order.status === 'CONFIRMED') {
      buttons.push(
        <Button key="dispatch" size="small" variant="contained" color="primary" startIcon={<LocalShipping />}
          onClick={() => setActionDialog({ open: true, orderId: order.id, action: 'dispatch' })}>
          Mark Dispatched
        </Button>
      );
    } else if (order.status === 'DISPATCHED') {
      buttons.push(
        <Button key="deliver" size="small" variant="contained" color="success" startIcon={<CheckCircle />}
          onClick={() => setActionDialog({ open: true, orderId: order.id, action: 'deliver' })}>
          Mark Delivered
        </Button>
      );
    }
    return buttons;
  };

  const handleAction = () => {
    actionMutation.mutate({ id: actionDialog.orderId, action: actionDialog.action });
  };

  const getActionTitle = (action: string) => {
    switch (action) {
      case 'confirm': return 'Confirm Order';
      case 'dispatch': return 'Mark as Dispatched';
      case 'deliver': return 'Mark as Delivered';
      case 'cancel': return 'Cancel Order';
      default: return 'Action';
    }
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 1 }}>
          <Receipt fontSize="large" /> Consumable Orders
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage consumable orders from healthcare facilities
        </Typography>
      </Box>

      {/* Main Tabs: Orders / Order History */}
      <Tabs value={mainTab} onChange={(_, v) => setMainTab(v)} sx={{ mb: 3 }}>
        <Tab label="Orders" icon={<ShoppingCart />} iconPosition="start" />
        <Tab label="Order History" icon={<History />} iconPosition="start" />
      </Tabs>

      {mainTab === 0 && (
        <>
          {/* Status Tabs */}
          <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
            <Tab label="All" />
            <Tab label={<Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              Pending
              {ordersData?.orders?.filter(o => o.status === 'PENDING').length ? (
                <Chip label={ordersData.orders.filter(o => o.status === 'PENDING').length} size="small" color="warning" sx={{ height: 20 }} />
              ) : null}
            </Box>} />
            <Tab label="Confirmed" />
            <Tab label="Dispatched" />
            <Tab label="Delivered" />
            <Tab label="Cancelled" />
          </Tabs>

      {/* Orders Table */}
      <Card>
        <CardContent>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : ordersData?.orders?.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
              <Inventory sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
              <Typography color="text.secondary">No orders found</Typography>
            </Paper>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell width={40}></TableCell>
                    <TableCell>Order #</TableCell>
                    <TableCell>HCF</TableCell>
                    <TableCell align="center">Items</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ordersData?.orders?.map((order) => (
                    <React.Fragment key={order.id}>
                      <TableRow hover>
                        <TableCell>
                          <IconButton size="small" onClick={() => setExpandedOrder(expandedOrder === order.id ? null : order.id)}>
                            {expandedOrder === order.id ? <ExpandLess /> : <ExpandMore />}
                          </IconButton>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }}>
                            {order.orderNumber}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight={500}>{order.hcfName}</Typography>
                          <Typography variant="caption" color="text.secondary">{order.agreementNumber || order.hcfCode}</Typography>
                        </TableCell>
                        <TableCell align="center">{order.itemCount}</TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" fontWeight={500}>₹{order.totalAmount?.toLocaleString()}</Typography>
                        </TableCell>
                        <TableCell>{getStatusChip(order.status)}</TableCell>
                        <TableCell>
                          <Typography variant="caption">
                            {new Date(order.orderedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            {new Date(order.orderedAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          <Box sx={{ display: 'flex', gap: 1 }}>
                            {getActionButtons(order)}
                          </Box>
                        </TableCell>
                      </TableRow>
                      
                      {/* Expanded Row */}
                      <TableRow>
                        <TableCell colSpan={8} sx={{ py: 0, borderBottom: expandedOrder === order.id ? undefined : 'none' }}>
                          <Collapse in={expandedOrder === order.id}>
                            <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1, my: 1 }}>
                              <Typography variant="subtitle2" sx={{ mb: 2 }}>Order Items</Typography>
                              {orderDetails?.items ? (
                                <Table size="small">
                                  <TableHead>
                                    <TableRow>
                                      <TableCell width={60}>Image</TableCell>
                                      <TableCell>Item</TableCell>
                                      <TableCell align="center">Qty</TableCell>
                                      <TableCell>Unit</TableCell>
                                      <TableCell align="right">Price</TableCell>
                                      <TableCell align="right">GST</TableCell>
                                      <TableCell align="right">Total</TableCell>
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {orderDetails.items.map((item, idx) => (
                                      <TableRow key={idx}>
                                        <TableCell>
                                          {item.imageUrl ? (
                                            <Box
                                              component="img"
                                              src={`http://localhost:8080${item.imageUrl}`}
                                              alt={item.name}
                                              sx={{ width: 45, height: 45, objectFit: 'cover', borderRadius: 1 }}
                                            />
                                          ) : (
                                            <Box sx={{ width: 45, height: 45, bgcolor: 'action.hover', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                              <Inventory sx={{ color: 'text.disabled', fontSize: 18 }} />
                                            </Box>
                                          )}
                                        </TableCell>
                                        <TableCell>{item.name}</TableCell>
                                        <TableCell align="center">{item.quantity}</TableCell>
                                        <TableCell>{item.unit}</TableCell>
                                        <TableCell align="right">₹{item.pricePerUnit}</TableCell>
                                        <TableCell align="right">{item.gstRate}%</TableCell>
                                        <TableCell align="right">₹{item.lineTotal?.toLocaleString()}</TableCell>
                                      </TableRow>
                                    ))}
                                  </TableBody>
                                </Table>
                              ) : (
                                <CircularProgress size={20} />
                              )}
                              
                              {orderDetails?.hcfNotes && (
                                <Box sx={{ mt: 2 }}>
                                  <Typography variant="caption" color="text.secondary">Customer Notes:</Typography>
                                  <Typography variant="body2">{orderDetails.hcfNotes}</Typography>
                                </Box>
                              )}
                              
                              <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                                <Typography variant="body2">
                                  <strong>HCF Address:</strong> {order.hcfAddress}
                                </Typography>
                              </Box>
                            </Box>
                          </Collapse>
                        </TableCell>
                      </TableRow>
                    </React.Fragment>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Action Dialog */}
      <Dialog open={actionDialog.open} onClose={() => setActionDialog({ open: false, orderId: '', action: '' })} maxWidth="sm" fullWidth>
        <DialogTitle>{getActionTitle(actionDialog.action)}</DialogTitle>
        <DialogContent>
          <TextField
            label="Notes (Optional)"
            fullWidth
            multiline
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            sx={{ mt: 2 }}
            placeholder={actionDialog.action === 'cancel' ? 'Reason for cancellation...' : 'Add any notes...'}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setActionDialog({ open: false, orderId: '', action: '' })}>Cancel</Button>
          <Button 
            variant="contained" 
            color={actionDialog.action === 'cancel' ? 'error' : 'primary'}
            onClick={handleAction}
            disabled={actionMutation.isPending}
          >
            {actionMutation.isPending ? <CircularProgress size={20} /> : getActionTitle(actionDialog.action)}
          </Button>
        </DialogActions>
      </Dialog>
        </>
      )}

      {mainTab === 1 && (
        <Box>
          {/* Period Selector and Export Button */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <ToggleButtonGroup
              value={analyticsPeriod}
              exclusive
              onChange={(_, v) => v && setAnalyticsPeriod(v)}
              size="small"
            >
              <ToggleButton value="day">Today</ToggleButton>
              <ToggleButton value="week">This Week</ToggleButton>
              <ToggleButton value="month">This Month</ToggleButton>
            </ToggleButtonGroup>

            <Button
              variant="contained"
              startIcon={<Download />}
              onClick={async () => {
                try {
                  const res = await apiClient.get(`/api/cbwtf/consumable-orders/export?period=${analyticsPeriod}`, {
                    responseType: 'blob'
                  });
                  const blob = new Blob([res.data], { type: 'text/csv' });
                  const url = window.URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `consumable_orders_${analyticsPeriod}_${new Date().toISOString().split('T')[0]}.csv`;
                  document.body.appendChild(a);
                  a.click();
                  window.URL.revokeObjectURL(url);
                  document.body.removeChild(a);
                } catch (err) {
                  console.error('Export failed:', err);
                  alert('Export failed. Please try again.');
                }
              }}
            >
              Export to Excel (CSV)
            </Button>
          </Box>

          {analyticsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <>
              {/* Stats Cards */}
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
                <Box sx={{ flex: '1 1 200px', minWidth: 200 }}>
                  <Card sx={{ bgcolor: alpha('#6366F1', 0.1), border: '1px solid', borderColor: alpha('#6366F1', 0.3) }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <ShoppingCart color="primary" />
                        <Typography variant="body2" color="text.secondary">Total Orders</Typography>
                      </Box>
                      <Typography variant="h4" fontWeight={700}>{analyticsData?.totalOrders || 0}</Typography>
                    </CardContent>
                  </Card>
                </Box>
                <Box sx={{ flex: '1 1 200px', minWidth: 200 }}>
                  <Card sx={{ bgcolor: alpha('#10B981', 0.1), border: '1px solid', borderColor: alpha('#10B981', 0.3) }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <CurrencyRupeeIcon sx={{ color: '#10B981' }} />
                        <Typography variant="body2" color="text.secondary">Total Amount</Typography>
                      </Box>
                      <Typography variant="h4" fontWeight={700}>₹{analyticsData?.totalAmount?.toLocaleString() || 0}</Typography>
                    </CardContent>
                  </Card>
                </Box>
                <Box sx={{ flex: '1 1 200px', minWidth: 200 }}>
                  <Card sx={{ bgcolor: alpha('#F59E0B', 0.1), border: '1px solid', borderColor: alpha('#F59E0B', 0.3) }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <Inventory sx={{ color: '#F59E0B' }} />
                        <Typography variant="body2" color="text.secondary">Pending</Typography>
                      </Box>
                      <Typography variant="h4" fontWeight={700}>{analyticsData?.statusBreakdown?.PENDING || 0}</Typography>
                    </CardContent>
                  </Card>
                </Box>
                <Box sx={{ flex: '1 1 200px', minWidth: 200 }}>
                  <Card sx={{ bgcolor: alpha('#10B981', 0.1), border: '1px solid', borderColor: alpha('#10B981', 0.3) }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <CheckCircle sx={{ color: '#10B981' }} />
                        <Typography variant="body2" color="text.secondary">Delivered</Typography>
                      </Box>
                      <Typography variant="h4" fontWeight={700}>{analyticsData?.statusBreakdown?.DELIVERED || 0}</Typography>
                    </CardContent>
                  </Card>
                </Box>
              </Box>

              {/* Status Breakdown */}
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>Status Breakdown</Typography>
                  <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                    {Object.entries(analyticsData?.statusBreakdown || {}).map(([status, count]) => (
                      <Chip
                        key={status}
                        label={`${status}: ${count}`}
                        color={
                          status === 'PENDING' ? 'warning' :
                          status === 'CONFIRMED' ? 'info' :
                          status === 'DISPATCHED' ? 'primary' :
                          status === 'DELIVERED' ? 'success' : 'default'
                        }
                        sx={{ fontWeight: 600 }}
                      />
                    ))}
                  </Box>
                </CardContent>
              </Card>

              {/* Orders Table */}
              <Card>
                <CardContent>
                  <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
                    Orders ({analyticsPeriod === 'day' ? 'Today' : analyticsPeriod === 'week' ? 'This Week' : 'This Month'})
                  </Typography>
                  {analyticsData?.orders?.length === 0 ? (
                    <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
                      <Inventory sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                      <Typography color="text.secondary">No orders in this period</Typography>
                    </Paper>
                  ) : (
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell width={50}></TableCell>
                            <TableCell>Order #</TableCell>
                            <TableCell>HCF</TableCell>
                            <TableCell>Agreement</TableCell>
                            <TableCell align="center">Items</TableCell>
                            <TableCell align="right">Total</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Date</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {analyticsData?.orders?.map((order) => (
                            <React.Fragment key={order.id}>
                              <TableRow hover>
                                <TableCell>
                                  <IconButton size="small" onClick={() => setExpandedOrder(expandedOrder === order.id ? null : order.id)}>
                                    {expandedOrder === order.id ? <ExpandLess /> : <ExpandMore />}
                                  </IconButton>
                                </TableCell>
                                <TableCell>
                                  <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'monospace' }}>
                                    {order.orderNumber}
                                  </Typography>
                                </TableCell>
                                <TableCell>{order.hcfName}</TableCell>
                                <TableCell>{order.agreementNumber || '-'}</TableCell>
                                <TableCell align="center">{order.itemCount}</TableCell>
                                <TableCell align="right">₹{order.totalAmount?.toLocaleString()}</TableCell>
                                <TableCell>
                                  <Chip 
                                    label={order.status} 
                                    size="small"
                                    color={
                                      order.status === 'PENDING' ? 'warning' :
                                      order.status === 'CONFIRMED' ? 'info' :
                                      order.status === 'DISPATCHED' ? 'primary' :
                                      order.status === 'DELIVERED' ? 'success' : 'default'
                                    }
                                  />
                                </TableCell>
                                <TableCell>
                                  {new Date(order.orderedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                                </TableCell>
                              </TableRow>
                              {/* Expanded Row */}
                              <TableRow>
                                <TableCell colSpan={8} sx={{ py: 0, borderBottom: expandedOrder === order.id ? undefined : 'none' }}>
                                  <Collapse in={expandedOrder === order.id}>
                                    <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1, my: 1 }}>
                                      <Typography variant="subtitle2" sx={{ mb: 2 }}>Order Items</Typography>
                                      {orderDetails?.items ? (
                                        <Table size="small">
                                          <TableHead>
                                            <TableRow>
                                              <TableCell width={60}>Image</TableCell>
                                              <TableCell>Item</TableCell>
                                              <TableCell align="center">Qty</TableCell>
                                              <TableCell>Unit</TableCell>
                                              <TableCell align="right">Price</TableCell>
                                              <TableCell align="right">GST</TableCell>
                                              <TableCell align="right">Total</TableCell>
                                            </TableRow>
                                          </TableHead>
                                          <TableBody>
                                            {orderDetails.items.map((item, idx) => (
                                              <TableRow key={idx}>
                                                <TableCell>
                                                  {item.imageUrl ? (
                                                    <Box
                                                      component="img"
                                                      src={`http://localhost:8080${item.imageUrl}`}
                                                      alt={item.name}
                                                      sx={{ width: 45, height: 45, objectFit: 'cover', borderRadius: 1 }}
                                                    />
                                                  ) : (
                                                    <Box sx={{ width: 45, height: 45, bgcolor: 'action.hover', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                                      <Inventory sx={{ color: 'text.disabled', fontSize: 18 }} />
                                                    </Box>
                                                  )}
                                                </TableCell>
                                                <TableCell>{item.name}</TableCell>
                                                <TableCell align="center">{item.quantity}</TableCell>
                                                <TableCell>{item.unit}</TableCell>
                                                <TableCell align="right">₹{item.pricePerUnit}</TableCell>
                                                <TableCell align="right">{item.gstRate}%</TableCell>
                                                <TableCell align="right">₹{item.lineTotal?.toLocaleString()}</TableCell>
                                              </TableRow>
                                            ))}
                                          </TableBody>
                                        </Table>
                                      ) : (
                                        <CircularProgress size={20} />
                                      )}
                                      <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                                        <Typography variant="body2">
                                          <strong>HCF Address:</strong> {order.hcfAddress}
                                        </Typography>
                                      </Box>
                                    </Box>
                                  </Collapse>
                                </TableCell>
                              </TableRow>
                            </React.Fragment>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </CardContent>
              </Card>
            </>
          )}
        </Box>
      )}
    </Box>
  );
};

export default ConsumableOrders;
