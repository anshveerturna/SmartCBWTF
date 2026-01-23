import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  TextField,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Paper,
  Alert,
  alpha,
  Tabs,
  Tab,
  Badge,
  Collapse,
} from '@mui/material';
import {
  ShoppingCart,
  Add,
  Remove,
  Delete,
  CheckCircle,
  LocalShipping,
  Receipt,
  ExpandMore,
  ExpandLess,
  Inventory,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

interface CatalogItem {
  id: string;
  code: string;
  name: string;
  description: string;
  category: string;
  unit: string;
  price: number;
  gstRate: number;
  imageUrl?: string;
}

interface CartItem extends CatalogItem {
  quantity: number;
}

interface Order {
  id: string;
  orderNumber: string;
  status: string;
  itemCount: number;
  totalAmount: number;
  orderedAt: string;
  confirmedAt?: string;
  deliveredAt?: string;
}

const ConsumablesOrder: React.FC = () => {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState(0);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [notes, setNotes] = useState('');
  const [success, setSuccess] = useState<string | null>(null);
  const [expandedOrder, setExpandedOrder] = useState<string | null>(null);

  // Fetch catalog
  const { data: catalogData, isLoading: catalogLoading } = useQuery({
    queryKey: ['hcf-consumables-catalog'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/consumables/catalog');
      return res.data as { items: CatalogItem[] };
    },
  });

  // Fetch orders
  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['hcf-consumables-orders'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/consumables/orders');
      return res.data as { orders: Order[]; total: number };
    },
  });

  // Fetch order details when expanded
  const { data: orderDetails } = useQuery({
    queryKey: ['hcf-order-detail', expandedOrder],
    queryFn: async () => {
      if (!expandedOrder) return null;
      const res = await apiClient.get(`/api/hcf/consumables/orders/${expandedOrder}`);
      return res.data as {
        id: string;
        orderNumber: string;
        status: string;
        items: Array<{
          name: string;
          quantity: number;
          unit: string;
          pricePerUnit: number;
          gstRate: number;
          lineTotal: number;
          imageUrl?: string;
        }>;
      };
    },
    enabled: !!expandedOrder,
  });

  // Place order mutation
  const orderMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post('/api/hcf/consumables/order', {
        items: cart.map(item => ({ itemId: item.id, quantity: item.quantity })),
        notes,
      });
      return res.data;
    },
    onSuccess: (data) => {
      setSuccess(`Order ${data.orderNumber} placed successfully!`);
      setCart([]);
      setNotes('');
      queryClient.invalidateQueries({ queryKey: ['hcf-consumables-orders'] });
      setTab(1);
    },
  });

  const addToCart = (item: CatalogItem) => {
    const existing = cart.find(c => c.id === item.id);
    if (existing) {
      setCart(cart.map(c => c.id === item.id ? { ...c, quantity: c.quantity + 1 } : c));
    } else {
      setCart([...cart, { ...item, quantity: 1 }]);
    }
  };

  const updateQuantity = (id: string, delta: number) => {
    setCart(cart.map(c => {
      if (c.id === id) {
        const newQty = c.quantity + delta;
        return newQty > 0 ? { ...c, quantity: newQty } : c;
      }
      return c;
    }));
  };

  const removeFromCart = (id: string) => {
    setCart(cart.filter(c => c.id !== id));
  };

  const cartTotal = cart.reduce((sum, item) => {
    const subtotal = item.price * item.quantity;
    const gst = subtotal * (item.gstRate / 100);
    return sum + subtotal + gst;
  }, 0);

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

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Order Consumables
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Order bins, bags, PPE and other supplies from your CBWTF
        </Typography>
      </Box>

      {success && <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      {/* Tabs */}
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label="Catalog" />
        <Tab 
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              Order History
              {ordersData?.total ? (
                <Chip label={ordersData.total} size="small" color="primary" sx={{ height: 20, fontSize: '0.7rem' }} />
              ) : null}
            </Box>
          } 
        />
      </Tabs>

      {tab === 0 && (
        <Grid container spacing={3}>
          {/* Catalog */}
          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2 }}>Available Items</Typography>
                
                {catalogLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                    <CircularProgress />
                  </Box>
                ) : catalogData?.items?.length === 0 ? (
                  <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
                    <Typography color="text.secondary">No consumables available</Typography>
                  </Paper>
                ) : (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell width={60}>Image</TableCell>
                          <TableCell>Code</TableCell>
                          <TableCell>Name</TableCell>
                          <TableCell>Category</TableCell>
                          <TableCell>Unit</TableCell>
                          <TableCell align="right">Price</TableCell>
                          <TableCell align="center">Add</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {catalogData?.items?.map((item) => (
                          <TableRow key={item.id} hover>
                            <TableCell>
                              {item.imageUrl ? (
                                <Box
                                  component="img"
                                  src={`http://localhost:8080${item.imageUrl}`}
                                  alt={item.name}
                                  sx={{ width: 50, height: 50, objectFit: 'cover', borderRadius: 1 }}
                                />
                              ) : (
                                <Box sx={{ width: 50, height: 50, bgcolor: 'action.hover', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                  <ShoppingCart sx={{ color: 'text.disabled', fontSize: 20 }} />
                                </Box>
                              )}
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
                                {item.code}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2" fontWeight={500}>{item.name}</Typography>
                              {item.description && (
                                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                                  {item.description.length > 50 ? item.description.substring(0, 50) + '...' : item.description}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell>
                              <Chip label={item.category} size="small" variant="outlined" />
                            </TableCell>
                            <TableCell>{item.unit}</TableCell>
                            <TableCell align="right">
                              <Typography variant="body2" fontWeight={500}>₹{item.price?.toFixed(2)}</Typography>
                              <Typography variant="caption" color="text.secondary">+{item.gstRate}% GST</Typography>
                            </TableCell>
                            <TableCell align="center">
                              <IconButton color="primary" size="small" onClick={() => addToCart(item)}>
                                <Add />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Cart */}
          <Grid item xs={12} md={4}>
            <Card sx={{ position: 'sticky', top: 80 }}>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <ShoppingCart /> Cart ({cart.length})
                </Typography>

                {cart.length === 0 ? (
                  <Paper sx={{ p: 3, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
                    <Typography color="text.secondary">Cart is empty</Typography>
                  </Paper>
                ) : (
                  <>
                    {cart.map((item) => (
                      <Box key={item.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
                        <Box>
                          <Typography variant="body2" fontWeight={500}>{item.name}</Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                            {item.code}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            ₹{item.price} × {item.quantity}
                          </Typography>
                        </Box>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <IconButton size="small" onClick={() => updateQuantity(item.id, -1)}>
                            <Remove fontSize="small" />
                          </IconButton>
                          <Typography variant="body2">{item.quantity}</Typography>
                          <IconButton size="small" onClick={() => updateQuantity(item.id, 1)}>
                            <Add fontSize="small" />
                          </IconButton>
                          <IconButton size="small" color="error" onClick={() => removeFromCart(item.id)}>
                            <Delete fontSize="small" />
                          </IconButton>
                        </Box>
                      </Box>
                    ))}

                    <Box sx={{ mt: 2, pt: 2, borderTop: '2px solid', borderColor: 'primary.main' }}>
                      <Typography variant="h6" sx={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span>Total</span>
                        <span>₹{cartTotal.toFixed(2)}</span>
                      </Typography>
                    </Box>

                    <TextField
                      label="Order Notes (Optional)"
                      fullWidth
                      multiline
                      rows={2}
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      sx={{ mt: 2 }}
                    />

                    <Button
                      variant="contained"
                      fullWidth
                      size="large"
                      startIcon={orderMutation.isPending ? <CircularProgress size={20} /> : <Receipt />}
                      onClick={() => orderMutation.mutate()}
                      disabled={orderMutation.isPending}
                      sx={{ mt: 2 }}
                    >
                      Place Order
                    </Button>
                  </>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tab === 1 && (
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>Order History</Typography>
            
            {ordersLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : ordersData?.orders?.length === 0 ? (
              <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
                <ShoppingCart sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                <Typography color="text.secondary">No orders yet</Typography>
              </Paper>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell width={50}></TableCell>
                      <TableCell>Order #</TableCell>
                      <TableCell>Items</TableCell>
                      <TableCell>Total</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Date</TableCell>
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
                            <Typography variant="body2" fontWeight={500} sx={{ fontFamily: 'monospace' }}>
                              {order.orderNumber}
                            </Typography>
                          </TableCell>
                          <TableCell>{order.itemCount} items</TableCell>
                          <TableCell>₹{order.totalAmount.toLocaleString()}</TableCell>
                          <TableCell>{getStatusChip(order.status)}</TableCell>
                          <TableCell>{new Date(order.orderedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</TableCell>
                        </TableRow>
                        {/* Expanded Row */}
                        <TableRow>
                          <TableCell colSpan={6} sx={{ py: 0, borderBottom: expandedOrder === order.id ? undefined : 'none' }}>
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
                                              <Box sx={{ width: 45, height: 45, bgcolor: 'background.paper', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
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
      )}
    </Box>
  );
};

export default ConsumablesOrder;
