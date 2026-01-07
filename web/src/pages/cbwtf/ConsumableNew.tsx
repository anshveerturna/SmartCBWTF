import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  TextField,
  MenuItem,
  Alert,
  IconButton,
  Avatar,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Save as SaveIcon,
  CloudUpload as UploadIcon,
  Inventory2 as InventoryIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import {
  createConsumable,
  listConsumableCategories,
  uploadConsumableImage,
} from '../../api/cbwtf';
import type {
  ConsumableCategoryDTO,
  CreateConsumableRequest,
} from '../../api/cbwtf';

const ConsumableNew: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [categories, setCategories] = useState<ConsumableCategoryDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  
  const [form, setForm] = useState<CreateConsumableRequest>({
    categoryId: '',
    consumableCode: '',
    name: '',
    description: '',
    hsnCode: '',
    unitOfMeasure: 'Pcs',
    initialPrice: undefined,
    gstRate: 18,
  });

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await listConsumableCategories();
        setCategories(data);
        if (data.length > 0) {
          setForm(prev => ({ ...prev, categoryId: data[0].id }));
        }
      } catch (err) {
        setError('Failed to load categories');
      }
    };
    loadCategories();
  }, []);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.categoryId || !form.consumableCode || !form.name || !form.unitOfMeasure) {
      setError('Please fill all required fields');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Create the consumable first
      const created = await createConsumable(form);
      
      // Then upload image if provided
      if (imageFile) {
        try {
          await uploadConsumableImage(created.id, imageFile);
        } catch (imgErr) {
          console.error('Image upload failed:', imgErr);
          // Continue to detail page even if image upload fails
        }
      }
      
      navigate(`/cbwtf/consumables/${created.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create consumable');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={() => navigate('/cbwtf/consumables')} sx={{ mr: 2 }}>
          <BackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700}>
          Add New Consumable
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
        {/* Form */}
        <Paper sx={{ p: 3, flex: '1 1 500px' }}>
          <form onSubmit={handleSubmit}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
              {/* Category */}
              <TextField
                select
                fullWidth
                required
                label="Category"
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              >
                {categories.map((cat) => (
                  <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>
                ))}
              </TextField>

              {/* Consumable Code */}
              <TextField
                fullWidth
                required
                label="Consumable Code"
                placeholder="e.g., BIN_YELLOW_10L"
                helperText="Unique code for this consumable. Cannot be changed later."
                value={form.consumableCode}
                onChange={(e) => setForm({ ...form, consumableCode: e.target.value.toUpperCase().replace(/\s/g, '_') })}
              />

              {/* Name */}
              <TextField
                fullWidth
                required
                label="Name"
                placeholder="e.g., Yellow Waste Bin (10 Liters)"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />

              {/* Description */}
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                placeholder="Detailed description for HCF reference"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />

              <Box sx={{ display: 'flex', gap: 2 }}>
                {/* HSN Code */}
                <TextField
                  fullWidth
                  label="HSN Code"
                  placeholder="e.g., 3923"
                  value={form.hsnCode}
                  onChange={(e) => setForm({ ...form, hsnCode: e.target.value })}
                />

                {/* Unit of Measure */}
                <TextField
                  fullWidth
                  required
                  label="Unit of Measure"
                  placeholder="e.g., Pcs, Kg, Pack"
                  value={form.unitOfMeasure}
                  onChange={(e) => setForm({ ...form, unitOfMeasure: e.target.value })}
                />
              </Box>

              <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 1 }}>
                Initial Pricing (Optional — can be added later)
              </Typography>

              <Box sx={{ display: 'flex', gap: 2 }}>
                {/* Initial Price */}
                <TextField
                  fullWidth
                  type="number"
                  label="Price per Unit (₹)"
                  placeholder="e.g., 150"
                  value={form.initialPrice || ''}
                  onChange={(e) => setForm({ ...form, initialPrice: e.target.value ? Number(e.target.value) : undefined })}
                />

                {/* GST Rate */}
                <TextField
                  fullWidth
                  type="number"
                  label="GST Rate (%)"
                  value={form.gstRate || 18}
                  onChange={(e) => setForm({ ...form, gstRate: Number(e.target.value) })}
                />
              </Box>

              {/* Submit */}
              <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  startIcon={<SaveIcon />}
                  disabled={loading}
                >
                  {loading ? 'Creating...' : 'Create Consumable'}
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => navigate('/cbwtf/consumables')}
                >
                  Cancel
                </Button>
              </Box>
            </Box>
          </form>
        </Paper>

        {/* Image Upload Section */}
        <Paper sx={{ p: 3, flex: '0 0 300px', textAlign: 'center' }}>
          <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
            Product Image
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Optional — can be added later
          </Typography>

          <Avatar
            src={imagePreview || undefined}
            variant="rounded"
            sx={{ width: 200, height: 200, mx: 'auto', mb: 2, bgcolor: 'grey.100' }}
          >
            <InventoryIcon sx={{ fontSize: 80, color: 'grey.400' }} />
          </Avatar>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            hidden
            onChange={handleImageChange}
          />

          {imagePreview ? (
            <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
              <Button
                variant="outlined"
                startIcon={<UploadIcon />}
                onClick={() => fileInputRef.current?.click()}
              >
                Change
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={handleRemoveImage}
              >
                Remove
              </Button>
            </Box>
          ) : (
            <Button
              variant="outlined"
              startIcon={<UploadIcon />}
              onClick={() => fileInputRef.current?.click()}
            >
              Upload Image
            </Button>
          )}

          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 2 }}>
            Recommended: 400×400px, PNG or JPG
          </Typography>
        </Paper>
      </Box>
    </Box>
  );
};

export default ConsumableNew;
