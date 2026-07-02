import { useState, useRef } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Box,
  Typography,
  TextField,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Switch,
  FormControlLabel,
  Divider,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  Save as SaveIcon,
  CloudUpload as UploadIcon,
  QrCode2 as QrIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import {
  type FinancialSettingsDTO,
  type LockedFieldsDTO,
  updateFinancialSettings,
  uploadPaymentQr,
  deletePaymentQr,
} from '../../../api/cbwtf';
import { apiAssetUrl } from '../../../api/client';

interface Props {
  data: FinancialSettingsDTO;
  lockedFields: LockedFieldsDTO;
  onSave: () => void;
}

const FinancialSettingsSection = ({ data, lockedFields, onSave }: Props) => {
  const [formData, setFormData] = useState<FinancialSettingsDTO>(data);
  const [error, setError] = useState<string | null>(null);
  const [qrUploading, setQrUploading] = useState(false);
  const [deleteQrDialogOpen, setDeleteQrDialogOpen] = useState(false);
  const qrInputRef = useRef<HTMLInputElement>(null);

  const mutation = useMutation({
    mutationFn: updateFinancialSettings,
    onSuccess: () => {
      onSave();
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || 'Failed to update financial settings');
    }
  });

  const handleChange = (field: keyof FinancialSettingsDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = field === 'gstEnabled' ? e.target.checked : parseFloat(e.target.value);
    setFormData({ ...formData, [field]: value });
  };

  const handleTextChange = (field: keyof FinancialSettingsDTO) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData({ ...formData, [field]: e.target.value });
  };

  const handleQrUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    if (file.size > 2 * 1024 * 1024) {
      setError('File size must be under 2MB');
      return;
    }

    setQrUploading(true);
    setError(null);
    try {
      const result = await uploadPaymentQr(file);
      setFormData({ ...formData, paymentQrUrl: result.paymentQrUrl });
      onSave(); // Refresh settings
    } catch (err: unknown) {
      console.error(err);
      setError('Failed to upload QR. Please try a valid PNG/JPEG under 2MB.');
    } finally {
      setQrUploading(false);
      if (qrInputRef.current) qrInputRef.current.value = '';
    }
  };

  const handleDeleteQr = async () => {
    try {
      await deletePaymentQr();
      setFormData({ ...formData, paymentQrUrl: undefined });
      setDeleteQrDialogOpen(false);
      onSave();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to delete QR');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Financial & Billing</Typography>
        <Button 
          variant="contained" 
          startIcon={mutation.isPending ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
          type="submit"
          disabled={mutation.isPending}
        >
          Save Changes
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
       {mutation.isSuccess && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings updated successfully
        </Alert>
      )}

      {lockedFields.gstLocked && (
        <Alert severity="info" sx={{ mb: 3 }}>
          GST rates are locked because invoices have already been generated. 
          Contact SuperAdmin if changes are required for valid business reasons.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12}>
           <FormControlLabel
            control={
              <Switch
                checked={formData.gstEnabled}
                onChange={handleChange('gstEnabled')}
                disabled={lockedFields.gstLocked}
              />
            }
            label="Enable GST Calculation"
          />
        </Grid>

        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="CGST %"
            value={formData.cgstPercent}
            onChange={handleChange('cgstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="SGST %"
            value={formData.sgstPercent}
            onChange={handleChange('sgstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            type="number"
            label="IGST %"
            value={formData.igstPercent}
            onChange={handleChange('igstPercent')}
            disabled={!formData.gstEnabled || lockedFields.gstLocked}
            inputProps={{ step: 0.1, min: 0, max: 28 }}
          />
        </Grid>
        
        <Grid item xs={12}>
           <Typography variant="caption" color="text.secondary">
             Note: IGST is applied for inter-state transactions based on the HCF's state. 
             CGST+SGST is applied for intra-state transactions.
           </Typography>
        </Grid>
      </Grid>

      {/* === Bank & Payment Details === */}
      <Divider sx={{ my: 4 }} />
      <Typography variant="h6" sx={{ mb: 2 }}>Bank & Payment Details</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        These details appear in Agreement PDFs alongside the UPI QR code.
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Account Name"
            value={formData.bankAccountName || ''}
            onChange={handleTextChange('bankAccountName')}
            placeholder="e.g. Global Environmental Solutions"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Account Number"
            value={formData.bankAccountNumber || ''}
            onChange={handleTextChange('bankAccountNumber')}
            placeholder="e.g. 505105010010646"
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="IFSC Code"
            value={formData.bankIfsc || ''}
            onChange={handleTextChange('bankIfsc')}
            placeholder="e.g. UBIN0816914"
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Bank Name"
            value={formData.bankName || ''}
            onChange={handleTextChange('bankName')}
            placeholder="e.g. Union Bank of India"
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Branch"
            value={formData.bankBranch || ''}
            onChange={handleTextChange('bankBranch')}
            placeholder="e.g. Rudrapur"
          />
        </Grid>

        {/* Payment QR Upload */}
        <Grid item xs={12}>
          <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
            <Box>
              {formData.paymentQrUrl ? (
                <Box
                  component="img"
                  src={apiAssetUrl(formData.paymentQrUrl)}
                  alt="Payment QR"
                  sx={{ width: 100, height: 100, objectFit: 'contain', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}
                />
              ) : (
                <Box sx={{ width: 100, height: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'grey.100', borderRadius: 1 }}>
                  <QrIcon sx={{ fontSize: 40, color: 'grey.400' }} />
                </Box>
              )}
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle2">UPI Payment QR Code</Typography>
              <Typography variant="caption" color="text.secondary">
                Upload a QR code image (PNG/JPEG, max 2MB) that will appear in agreement PDFs.
              </Typography>
              <Box sx={{ mt: 1, display: 'flex', gap: 1 }}>
                <input
                  type="file"
                  accept="image/png,image/jpeg"
                  hidden
                  ref={qrInputRef}
                  onChange={handleQrUpload}
                />
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={qrUploading ? <CircularProgress size={16} /> : <UploadIcon />}
                  onClick={() => qrInputRef.current?.click()}
                  disabled={qrUploading}
                >
                  {formData.paymentQrUrl ? 'Replace QR' : 'Upload QR'}
                </Button>
                {formData.paymentQrUrl && (
                  <Button
                    variant="outlined"
                    color="error"
                    size="small"
                    startIcon={<DeleteIcon />}
                    onClick={() => setDeleteQrDialogOpen(true)}
                    disabled={qrUploading}
                  >
                    Remove
                  </Button>
                )}
              </Box>
            </Box>
          </Paper>
        </Grid>
      </Grid>
      <Dialog open={deleteQrDialogOpen} onClose={() => setDeleteQrDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Remove Payment QR</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Remove the UPI QR code from future agreement PDFs?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteQrDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleDeleteQr}>
            Remove
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FinancialSettingsSection;
