import { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  Alert,
  Snackbar,
  CircularProgress,
  Divider,
  Paper,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  Delete as DeleteIcon,
  Image as ImageIcon,
} from '@mui/icons-material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getBranding, updateBranding, uploadLogo, deleteLogo } from '../../../api/cbwtf';
import { apiAssetUrl } from '../../../api/client';
import type { BrandingDTO } from '../../../api/cbwtf';
import { useTheme } from '@mui/material/styles';

// Reuse the SettingRow component style
const SettingRow = ({ 
  label, 
  description, 
  children 
}: { 
  label: string; 
  description?: string; 
  children: React.ReactNode 
}) => (
  <Box
    sx={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      py: 2,
      px: 3,
      borderBottom: '1px solid',
      borderColor: 'divider',
      '&:last-child': { borderBottom: 'none' },
      '&:hover': { bgcolor: 'action.hover' },
      transition: 'background-color 0.15s',
    }}
  >
    <Box sx={{ flex: 1, mr: 3 }}>
      <Typography variant="body1" fontWeight={500}>
        {label}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {description}
        </Typography>
      )}
    </Box>
    <Box sx={{ minWidth: 280, display: 'flex', justifyContent: 'flex-end' }}>
      {children}
    </Box>
  </Box>
);

interface BrandingSectionProps {
  onSettingsChange?: () => void;
}

export default function BrandingSection({ onSettingsChange }: BrandingSectionProps) {
  const queryClient = useQueryClient();
  const theme = useTheme();
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });
  const [imageError, setImageError] = useState(false);

  // Form state
  const [formData, setFormData] = useState<BrandingDTO | null>(null);

  // Fetch branding data
  const { data: branding, isLoading, error } = useQuery<BrandingDTO>({
    queryKey: ['branding'],
    queryFn: getBranding,
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: updateBranding,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['branding'] });
      setSnackbar({ open: true, message: 'Branding settings saved successfully', severity: 'success' });
      onSettingsChange?.();
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to save branding settings', severity: 'error' });
    },
  });

  // Logo upload mutation
  const uploadMutation = useMutation({
    mutationFn: uploadLogo,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['branding'] });
      setFormData(prev => ({ ...(prev ?? branding), logoUrl: data.logoUrl }));
      setImageError(false);
      setSnackbar({ open: true, message: 'Logo uploaded successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to upload logo', severity: 'error' });
    },
  });

  // Logo delete mutation
  const deleteMutation = useMutation({
    mutationFn: deleteLogo,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['branding'] });
      setFormData(prev => ({ ...(prev ?? branding), logoUrl: undefined }));
      setSnackbar({ open: true, message: 'Logo deleted successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to delete logo', severity: 'error' });
    },
  });

  const handleLogoUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        setSnackbar({ open: true, message: 'Logo file must be under 2MB', severity: 'error' });
        return;
      }
      if (!['image/png', 'image/jpeg'].includes(file.type)) {
        setSnackbar({ open: true, message: 'Only PNG and JPEG images are allowed', severity: 'error' });
        return;
      }
      uploadMutation.mutate(file);
    }
  };

  const handleSave = () => {
    updateMutation.mutate({
      ...branding,
      ...(formData ?? {}),
    });
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">Failed to load branding settings</Alert>;
  }

  const currentBranding = { ...branding, ...formData };
  const updateBrandingField = (updates: Partial<BrandingDTO>) => {
    setFormData({ ...currentBranding, ...updates });
  };
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box>
      {/* Logo Section */}
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Box sx={{ px: 3, py: 2, bgcolor: isDark ? 'background.default' : 'grey.100', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            <ImageIcon sx={{ fontSize: 20, mr: 1, verticalAlign: 'middle' }} />
            Company Logo
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Upload your company logo to appear on invoices, receipts, and emails
          </Typography>
        </Box>
        
        <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 3 }}>
          {currentBranding.logoUrl && !imageError ? (
            <Box
              component="img"
              src={apiAssetUrl(currentBranding.logoUrl)}
              alt="Company logo"
              onError={() => setImageError(true)}
              sx={{
                width: 120,
                height: 120,
                objectFit: 'contain',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                bgcolor: 'background.paper',
                p: 1,
              }}
            />
          ) : (
            <Box
              sx={{
                width: 120,
                height: 120,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '2px dashed',
                borderColor: 'divider',
                borderRadius: 1,
                bgcolor: isDark ? 'background.default' : 'grey.100',
              }}
            >
              <Typography variant="body2" color="text.secondary" textAlign="center">
                {imageError ? 'Failed to load' : 'No logo'}<br />uploaded
              </Typography>
            </Box>
          )}
          
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Button
              variant="outlined"
              component="label"
              startIcon={<UploadIcon />}
              disabled={uploadMutation.isPending}
            >
              {uploadMutation.isPending ? 'Uploading...' : 'Upload Logo'}
              <input
                type="file"
                hidden
                accept="image/png,image/jpeg"
                onChange={handleLogoUpload}
              />
            </Button>
            {currentBranding.logoUrl && (
              <Tooltip title="Delete logo">
                <IconButton
                  color="error"
                  onClick={() => deleteMutation.mutate()}
                  disabled={deleteMutation.isPending}
                  size="small"
                >
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            )}
            <Typography variant="caption" color="text.secondary">
              PNG or JPEG, max 2MB
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Logo Display Options */}
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Box sx={{ px: 3, py: 2, bgcolor: isDark ? 'background.default' : 'grey.100', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            Logo Display Options
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Choose where to show your logo
          </Typography>
        </Box>

        <SettingRow label="Show on Invoices" description="Display logo on generated invoices">
          <FormControlLabel
            control={
              <Switch
                checked={currentBranding.showLogoOnInvoice ?? true}
                onChange={(e) => updateBrandingField({ showLogoOnInvoice: e.target.checked })}
              />
            }
            label=""
          />
        </SettingRow>

        <SettingRow label="Show on Receipts" description="Display logo on payment receipts">
          <FormControlLabel
            control={
              <Switch
                checked={currentBranding.showLogoOnReceipt ?? true}
                onChange={(e) => updateBrandingField({ showLogoOnReceipt: e.target.checked })}
              />
            }
            label=""
          />
        </SettingRow>

        <SettingRow label="Show in Emails" description="Display logo in email headers">
          <FormControlLabel
            control={
              <Switch
                checked={currentBranding.showLogoOnEmail ?? true}
                onChange={(e) => updateBrandingField({ showLogoOnEmail: e.target.checked })}
              />
            }
            label=""
          />
        </SettingRow>
      </Paper>

      {/* Footer Text */}
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Box sx={{ px: 3, py: 2, bgcolor: isDark ? 'background.default' : 'grey.100', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            Document Footer Text
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Custom text to appear at the bottom of documents
          </Typography>
        </Box>

        <Box sx={{ p: 3 }}>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Invoice Footer"
            placeholder="Enter custom footer text for invoices..."
            value={currentBranding.invoiceFooterText || ''}
            onChange={(e) => updateBrandingField({ invoiceFooterText: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Receipt Footer"
            placeholder="Enter custom footer text for receipts..."
            value={currentBranding.receiptFooterText || ''}
            onChange={(e) => updateBrandingField({ receiptFooterText: e.target.value })}
          />
        </Box>
      </Paper>

      <Divider sx={{ my: 2 }} />

      {/* Save Button */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={updateMutation.isPending}
        >
          {updateMutation.isPending ? 'Saving...' : 'Save Branding Settings'}
        </Button>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
