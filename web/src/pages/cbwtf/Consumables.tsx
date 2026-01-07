import React, { useState, useEffect } from 'react';
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
  FormControlLabel,
  Switch,
  TextField,
  MenuItem,
  Skeleton,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Inventory2 as InventoryIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import {
  listConsumables,
  listConsumableCategories,
} from '../../api/cbwtf';
import type {
  ConsumableItemDTO,
  ConsumableCategoryDTO,
} from '../../api/cbwtf';

const Consumables: React.FC = () => {
  const navigate = useNavigate();
  const [consumables, setConsumables] = useState<ConsumableItemDTO[]>([]);
  const [categories, setCategories] = useState<ConsumableCategoryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [includeInactive, setIncludeInactive] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState<string>('all');

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [consumablesData, categoriesData] = await Promise.all([
        listConsumables(includeInactive),
        listConsumableCategories(),
      ]);
      setConsumables(consumablesData);
      setCategories(categoriesData);
    } catch (err) {
      setError('Failed to load consumables');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [includeInactive]);

  const formatCurrency = (amount: number | null) => {
    if (amount === null) return '—';
    return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
  };

  const filteredConsumables = consumables.filter(
    item => categoryFilter === 'all' || item.categoryId === categoryFilter
  );

  if (loading) {
    return (
      <Box sx={{ p: 3 }}>
        <Skeleton variant="rectangular" height={60} sx={{ mb: 2 }} />
        <Skeleton variant="rectangular" height={400} />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>
            Consumables
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage operational supplies for HCFs
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/cbwtf/consumables/new')}
        >
          Add Consumable
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            select
            size="small"
            label="Category"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="all">All Categories</MenuItem>
            {categories.map((cat) => (
              <MenuItem key={cat.id} value={cat.id}>
                {cat.name} ({cat.itemCount})
              </MenuItem>
            ))}
          </TextField>

          <FormControlLabel
            control={
              <Switch
                checked={includeInactive}
                onChange={(e) => setIncludeInactive(e.target.checked)}
                size="small"
              />
            }
            label="Show Inactive"
          />

          <Typography variant="body2" color="text.secondary" sx={{ ml: 'auto' }}>
            {filteredConsumables.length} consumable{filteredConsumables.length !== 1 ? 's' : ''}
          </Typography>
        </Box>
      </Paper>

      {/* Consumables Table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell width={60}>Image</TableCell>
                <TableCell>Consumable Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>HSN Code</TableCell>
                <TableCell>Unit</TableCell>
                <TableCell align="right">Price (₹)</TableCell>
                <TableCell align="center">Status</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredConsumables.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} align="center" sx={{ py: 6 }}>
                    <InventoryIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                      No consumables found
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      Add your first consumable to get started
                    </Typography>
                    <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/cbwtf/consumables/new')}>
                      Add Consumable
                    </Button>
                  </TableCell>
                </TableRow>
              ) : (
                filteredConsumables.map((item) => (
                  <TableRow
                    key={item.id}
                    hover
                    sx={{ cursor: 'pointer', opacity: item.isActive ? 1 : 0.6 }}
                    onClick={() => navigate(`/cbwtf/consumables/${item.id}`)}
                  >
                    <TableCell>
                      <Avatar
                        src={item.imageUrl ? `http://localhost:8080${item.imageUrl}?t=${new Date(item.updatedAt).getTime()}` : undefined}
                        variant="rounded"
                        sx={{ width: 40, height: 40, bgcolor: 'grey.200' }}
                      >
                        <InventoryIcon fontSize="small" />
                      </Avatar>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontFamily="monospace" fontWeight={500}>
                        {item.consumableCode}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {item.name}
                      </Typography>
                      {item.description && (
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ maxWidth: 200 }} noWrap>
                          {item.description}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip label={item.categoryName} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontFamily="monospace">
                        {item.hsnCode || '—'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={item.unitOfMeasure} size="small" />
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" fontWeight={500}>
                        {formatCurrency(item.activePrice)}
                      </Typography>
                      {item.activeGstRate && (
                        <Typography variant="caption" color="text.secondary">
                          +{item.activeGstRate}% GST
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        label={item.isActive ? 'Active' : 'Inactive'}
                        size="small"
                        color={item.isActive ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/cbwtf/consumables/${item.id}`);
                        }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );
};

export default Consumables;
