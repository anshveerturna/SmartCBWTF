import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Avatar,
  IconButton,
  TextField,
  MenuItem,
  Skeleton,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tooltip,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Delete as DeleteIcon,
  CloudUpload as UploadIcon,
  Add as AddIcon,
  CheckCircle as ActiveIcon,
  Block as InactiveIcon,
  Inventory2 as InventoryIcon,
} from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getConsumable,
  updateConsumable,
  addConsumablePricing,
  uploadConsumableImage,
  deleteConsumableImage,
  deactivateConsumable,
  activateConsumable,
  listConsumableCategories,
} from '../../api/cbwtf';
import type {
  ConsumableItemDTO,
  ConsumableCategoryDTO,
  UpdateConsumableRequest,
  AddPricingRequest,
} from '../../api/cbwtf';
import { apiAssetUrl } from '../../api/client';

const ConsumableDetail: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [consumable, setConsumable] = useState<ConsumableItemDTO | null>(null);
  const [categories, setCategories] = useState<ConsumableCategoryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<UpdateConsumableRequest>({});
  const [pricingDialogOpen, setPricingDialogOpen] = useState(false);
  const [deleteImageDialogOpen, setDeleteImageDialogOpen] = useState(false);
  const [pricingForm, setPricingForm] = useState<AddPricingRequest>({
    pricePerUnit: 0,
    gstRate: 18,
    effectiveFrom: new Date().toISOString().split('T')[0],
  });

  const fetchData = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const [consumableData, categoriesData] = await Promise.all([
        getConsumable(id),
        listConsumableCategories(),
      ]);
      setConsumable(consumableData);
      setCategories(categoriesData);
      setEditForm({
        categoryId: consumableData.categoryId,
        name: consumableData.name,
        description: consumableData.description || '',
        hsnCode: consumableData.hsnCode || '',
        unitOfMeasure: consumableData.unitOfMeasure,
      });
    } catch (err) {
      setError('Failed to load consumable');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSave = async () => {
    if (!id) return;
    try {
      setSaving(true);
      const updated = await updateConsumable(id, editForm);
      setConsumable(updated);
      setEditing(false);
    } catch {
      setError('Failed to save changes');
    } finally {
      setSaving(false);
    }
  };

  const handleAddPricing = async () => {
    if (!id) return;
    try {
      setSaving(true);
      const updated = await addConsumablePricing(id, pricingForm);
      setConsumable(updated);
      setPricingDialogOpen(false);
    } catch {
      setError('Failed to add pricing');
    } finally {
      setSaving(false);
    }
  };

  const handleImageUpload = async (file: File) => {
    if (!id) return;
    try {
      setSaving(true);
      const updated = await uploadConsumableImage(id, file);
      setConsumable(updated);
    } catch {
      setError('Failed to upload image');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteImage = async () => {
    if (!id) return;
    try {
      setSaving(true);
      const updated = await deleteConsumableImage(id);
      setConsumable(updated);
      setDeleteImageDialogOpen(false);
    } catch {
      setError('Failed to delete image');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async () => {
    if (!id || !consumable) return;
    try {
      setSaving(true);
      if (consumable.isActive) {
        await deactivateConsumable(id);
      } else {
        await activateConsumable(id);
      }
      fetchData();
    } catch {
      setError('Failed to update status');
    } finally {
      setSaving(false);
    }
  };

  const formatCurrency = (amount: number | null) => {
    if (amount === null) return '—';
    return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <Box sx={{ p: 3 }}>
        <Skeleton variant="rectangular" height={60} sx={{ mb: 2 }} />
        <Skeleton variant="rectangular" height={400} />
      </Box>
    );
  }

  if (!consumable) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">Consumable not found</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={() => navigate('/cbwtf/consumables')} sx={{ mr: 2 }}>
          <BackIcon />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h5" fontWeight={700}>
            {consumable.name}
          </Typography>
          <Typography variant="body2" color="text.secondary" fontFamily="monospace">
            {consumable.consumableCode}
          </Typography>
        </Box>
        <Chip
          label={consumable.isActive ? 'Active' : 'Inactive'}
          color={consumable.isActive ? 'success' : 'default'}
          sx={{ mr: 2 }}
        />
        {!editing ? (
          <Button variant="outlined" startIcon={<EditIcon />} onClick={() => setEditing(true)}>
            Edit
          </Button>
        ) : (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} disabled={saving}>
              Save
            </Button>
            <Button variant="outlined" startIcon={<CancelIcon />} onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </Box>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
        {/* Left Column */}
        <Box sx={{ flex: '1 1 600px', minWidth: 0 }}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
              Basic Information
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  fullWidth
                  label="Name"
                  value={editing ? editForm.name : consumable.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  disabled={!editing}
                  size="small"
                />
                <TextField
                  fullWidth
                  select
                  label="Category"
                  value={editing ? editForm.categoryId : consumable.categoryId}
                  onChange={(e) => setEditForm({ ...editForm, categoryId: e.target.value })}
                  disabled={!editing}
                  size="small"
                >
                  {categories.map((cat) => (
                    <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>
                  ))}
                </TextField>
              </Box>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  fullWidth
                  label="HSN Code"
                  value={editing ? editForm.hsnCode : consumable.hsnCode || ''}
                  onChange={(e) => setEditForm({ ...editForm, hsnCode: e.target.value })}
                  disabled={!editing}
                  size="small"
                />
                <TextField
                  fullWidth
                  label="Unit"
                  value={editing ? editForm.unitOfMeasure : consumable.unitOfMeasure}
                  onChange={(e) => setEditForm({ ...editForm, unitOfMeasure: e.target.value })}
                  disabled={!editing}
                  size="small"
                />
              </Box>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                value={editing ? editForm.description : consumable.description || ''}
                onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                disabled={!editing}
                size="small"
              />
            </Box>
          </Paper>

          {/* Pricing History */}
          <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight={600}>Pricing History</Typography>
              <Button variant="outlined" size="small" startIcon={<AddIcon />} onClick={() => setPricingDialogOpen(true)}>
                Add Price
              </Button>
            </Box>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Price</TableCell>
                    <TableCell>GST</TableCell>
                    <TableCell>From</TableCell>
                    <TableCell>To</TableCell>
                    <TableCell align="center">Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {consumable.pricingHistory?.length ? consumable.pricingHistory.map((p) => (
                    <TableRow key={p.id} sx={{ bgcolor: p.isActive ? 'success.50' : undefined }}>
                      <TableCell><Typography fontWeight={p.isActive ? 600 : 400}>{formatCurrency(p.pricePerUnit)}</Typography></TableCell>
                      <TableCell>{p.gstRate}%</TableCell>
                      <TableCell>{formatDate(p.effectiveFrom)}</TableCell>
                      <TableCell>{formatDate(p.effectiveTo)}</TableCell>
                      <TableCell align="center">
                        <Chip label={p.isActive ? 'Active' : 'Expired'} size="small" color={p.isActive ? 'success' : 'default'} variant={p.isActive ? 'filled' : 'outlined'} />
                      </TableCell>
                    </TableRow>
                  )) : (
                    <TableRow><TableCell colSpan={5} align="center">No pricing yet</TableCell></TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Box>

        {/* Right Column */}
        <Box sx={{ flex: '0 0 320px' }}>
          <Paper sx={{ p: 3, mb: 3, textAlign: 'center' }}>
            <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>Image</Typography>
            <Avatar 
              src={apiAssetUrl(consumable.imageUrl, new Date(consumable.updatedAt).getTime())}
              variant="rounded" 
              sx={{ width: 200, height: 200, mx: 'auto', mb: 2, bgcolor: 'grey.100' }}
            >
              <InventoryIcon sx={{ fontSize: 80, color: 'grey.400' }} />
            </Avatar>
            <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
              <Button 
                variant="outlined" 
                startIcon={<UploadIcon />} 
                disabled={saving}
                onClick={() => {
                  const input = document.getElementById('consumable-image-upload') as HTMLInputElement;
                  if (input) input.click();
                }}
              >
                {consumable.imageUrl ? 'Replace' : 'Upload'}
              </Button>
              {consumable.imageUrl && (
                <Button 
                  variant="outlined" 
                  color="error"
                  startIcon={<DeleteIcon />} 
                  disabled={saving}
                  onClick={() => setDeleteImageDialogOpen(true)}
                >
                  Delete
                </Button>
              )}
            </Box>
            <input 
              id="consumable-image-upload"
              type="file" 
              hidden 
              accept="image/*" 
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  handleImageUpload(e.target.files[0]);
                }
                e.target.value = ''; // Reset input to allow re-uploading same file
              }} 
            />
          </Paper>

          {consumable.referenceDisplayText && (
            <Paper sx={{ p: 3, mb: 3 }}>
              <Typography variant="h6" fontWeight={600} sx={{ mb: 1 }}>Quantity Reference</Typography>
              <Tooltip title="Planning only — not billing">
                <Box sx={{ p: 2, bgcolor: 'info.50', borderRadius: 1 }}>📊 {consumable.referenceDisplayText}</Box>
              </Tooltip>
            </Paper>
          )}

          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>Status</Typography>
            <Button fullWidth variant={consumable.isActive ? 'outlined' : 'contained'} color={consumable.isActive ? 'error' : 'success'} startIcon={consumable.isActive ? <InactiveIcon /> : <ActiveIcon />} onClick={handleToggleStatus} disabled={saving}>
              {consumable.isActive ? 'Deactivate' : 'Activate'}
            </Button>
          </Paper>
        </Box>
      </Box>

      {/* Pricing Dialog */}
      <Dialog open={pricingDialogOpen} onClose={() => setPricingDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add New Price</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField fullWidth label="Price (₹)" type="number" value={pricingForm.pricePerUnit} onChange={(e) => setPricingForm({ ...pricingForm, pricePerUnit: Number(e.target.value) })} />
            <TextField fullWidth label="GST (%)" type="number" value={pricingForm.gstRate} onChange={(e) => setPricingForm({ ...pricingForm, gstRate: Number(e.target.value) })} />
            <TextField fullWidth label="Effective From" type="date" value={pricingForm.effectiveFrom} onChange={(e) => setPricingForm({ ...pricingForm, effectiveFrom: e.target.value })} InputLabelProps={{ shrink: true }} />
            <Alert severity="info" icon={false}>Adding a new price deactivates the previous one.</Alert>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPricingDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddPricing} disabled={saving}>Add</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={deleteImageDialogOpen} onClose={() => setDeleteImageDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete Image</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Remove this consumable image from the catalog?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteImageDialogOpen(false)} disabled={saving}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleDeleteImage} disabled={saving}>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ConsumableDetail;
