import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, Button, Chip, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, FormControl, InputLabel, Select, MenuItem, Stack,
  IconButton, Tooltip, Alert, Snackbar, CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon, QrCode as QrIcon, Block as RevokeIcon, Visibility as ViewIcon,
  Download as DownloadIcon, PictureAsPdf as PdfIcon,
} from '@mui/icons-material';
import { listQrs, revokeQr, getHcfList, generateLabelsForHcf, type QrDetail } from '../../api/cbwtf';
import QRCode from 'qrcode';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const WASTE_CATEGORIES = ['YELLOW', 'RED', 'BLUE', 'WHITE'] as const;
const STATUS_COLORS: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
  ACTIVE: 'success', USED: 'warning', VERIFIED: 'info', EXPIRED: 'default', REVOKED: 'error', BLOCKED: 'error',
};
const CATEGORY_COLORS: Record<string, string> = {
  YELLOW: '#FFC107', RED: '#F44336', BLUE: '#2196F3', WHITE: '#9E9E9E',
};

export default function QrLabels() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [generateDialogOpen, setGenerateDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedQr, setSelectedQr] = useState<QrDetail | null>(null);
  const [qrImageUrl, setQrImageUrl] = useState<string>('');
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false, message: '', severity: 'success',
  });
  const [generateForm, setGenerateForm] = useState({
    hcfId: '',
    categoryQuantities: { YELLOW: 0, RED: 0, BLUE: 0, WHITE: 0 } as Record<string, number>,
  });

  const totalLabels = Object.values(generateForm.categoryQuantities).reduce((sum, q) => sum + (q || 0), 0);

  const { data: qrs = [], isLoading } = useQuery({
    queryKey: ['cbwtf-qrs', statusFilter],
    queryFn: () => listQrs(undefined, statusFilter || undefined),
  });

  const { data: hcfs = [] } = useQuery({
    queryKey: ['cbwtf-hcfs-for-qr'],
    queryFn: () => getHcfList(),
  });

  const revokeMutation = useMutation({
    mutationFn: (id: string) => revokeQr(id, 'Revoked by admin'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-qrs'] });
      setSnackbar({ open: true, message: 'QR Code revoked', severity: 'success' });
    },
    onError: (error: Error) => {
      setSnackbar({ open: true, message: error.message || 'Failed to revoke QR', severity: 'error' });
    },
  });

  const generateMutation = useMutation({
    mutationFn: () => {
      // Filter out zero-quantity categories
      const filtered: Record<string, number> = {};
      for (const [cat, qty] of Object.entries(generateForm.categoryQuantities)) {
        if (qty > 0) filtered[cat] = qty;
      }
      return generateLabelsForHcf({
        hcfId: generateForm.hcfId,
        categoryQuantities: filtered,
      });
    },
    onSuccess: (data) => {
      setGenerateDialogOpen(false);
      setSnackbar({ open: true, message: data.message || 'QR labels generated!', severity: 'success' });
      setGenerateForm({ hcfId: '', categoryQuantities: { YELLOW: 0, RED: 0, BLUE: 0, WHITE: 0 } });
      if (data.pdfUrl) {
        window.open(getDownloadUrl(data.pdfUrl), '_blank');
      }
    },
    onError: (error: any) => {
      const message = error?.response?.data?.error || error?.message || 'Failed to generate labels';
      setSnackbar({ open: true, message, severity: 'error' });
    },
  });

  const getDownloadUrl = (url: string) => {
    if (!url) return '';
    let filePath = url;
    if (!url.startsWith('/files/')) {
      const parts = url.split(/[/\\]/);
      const filename = parts[parts.length - 1];
      filePath = '/files/' + filename;
    }
    return API_BASE_URL ? API_BASE_URL + filePath : filePath;
  };

  const handleGenerate = () => {
    if (!generateForm.hcfId) {
      setSnackbar({ open: true, message: 'Please select an HCF', severity: 'error' });
      return;
    }
    if (totalLabels < 1) {
      setSnackbar({ open: true, message: 'Enter quantity for at least one waste category', severity: 'error' });
      return;
    }
    if (totalLabels > 500) {
      setSnackbar({ open: true, message: 'Total labels cannot exceed 500', severity: 'error' });
      return;
    }
    generateMutation.mutate();
  };

  const setCategoryQty = (cat: string, val: number) => {
    setGenerateForm((prev) => ({
      ...prev,
      categoryQuantities: { ...prev.categoryQuantities, [cat]: Math.max(0, Math.min(500, val)) },
    }));
  };

  const handleViewQr = async (qr: QrDetail) => {
    setSelectedQr(qr);
    try {
      const url = await QRCode.toDataURL(qr.qrPayloadJson, { width: 300, margin: 2 });
      setQrImageUrl(url);
      setViewDialogOpen(true);
    } catch {
      setSnackbar({ open: true, message: 'Failed to generate QR image', severity: 'error' });
    }
  };

  const handleDownloadQr = () => {
    if (qrImageUrl && selectedQr) {
      const link = document.createElement('a');
      link.download = 'QR-' + selectedQr.hcfName + '-' + selectedQr.wasteCategory + '.png';
      link.href = qrImageUrl;
      link.click();
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <QrIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" fontWeight="bold">QR Labels</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setGenerateDialogOpen(true)}>
          Generate QR
        </Button>
      </Box>

      {/* Filter bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ py: 2 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Status</InputLabel>
              <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="USED">Used</MenuItem>
                <MenuItem value="VERIFIED">Verified</MenuItem>
                <MenuItem value="EXPIRED">Expired</MenuItem>
                <MenuItem value="REVOKED">Revoked</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body2" color="text.secondary">
              {qrs.length} QR codes
            </Typography>
          </Stack>
        </CardContent>
      </Card>

      {/* QR codes table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.100' }}>
              <TableCell>HCF</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Valid Period</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : qrs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    No QR codes found. Click &quot;Generate QR&quot; to create labels.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              qrs.map((qr) => (
                <TableRow key={qr.id} hover>
                  <TableCell>
                    <Typography fontWeight="medium">{qr.hcfName}</Typography>
                    <Typography variant="caption" color="text.secondary">{qr.agreementNumber}</Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={qr.wasteCategory}
                      size="small"
                      sx={{
                        bgcolor: CATEGORY_COLORS[qr.wasteCategory],
                        color: qr.wasteCategory === 'WHITE' ? 'black' : 'white',
                        fontWeight: 'bold',
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{formatDate(qr.validFrom)}</Typography>
                    <Typography variant="caption" color="text.secondary">to {formatDate(qr.validTo)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={qr.status} size="small" color={STATUS_COLORS[qr.status]} />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{formatDate(qr.createdAt)}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="View QR Code">
                      <IconButton onClick={() => handleViewQr(qr)} color="primary">
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                    {qr.status === 'ACTIVE' && (
                      <Tooltip title="Revoke QR">
                        <IconButton
                          onClick={() => revokeMutation.mutate(qr.id)}
                          color="error"
                          disabled={revokeMutation.isPending}
                        >
                          <RevokeIcon />
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

      {/* Generate QR Dialog */}
      <Dialog open={generateDialogOpen} onClose={() => setGenerateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box display="flex" alignItems="center" gap={1}>
            <QrIcon color="primary" />
            Generate QR Labels
          </Box>
        </DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2, mt: 1 }}>
            Generate printable QR bag labels for an HCF. Set quantity for each waste category
            you need. All labels are combined into a single PDF (3&times;3 grid per page).
          </Alert>
          <Stack spacing={3}>
            <FormControl fullWidth required>
              <InputLabel>Select HCF</InputLabel>
              <Select
                value={generateForm.hcfId}
                label="Select HCF"
                onChange={(e) => setGenerateForm({ ...generateForm, hcfId: e.target.value })}
              >
                {hcfs.map((hcf) => (
                  <MenuItem key={hcf.id} value={hcf.id}>
                    {hcf.name} ({hcf.code})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <Typography variant="subtitle2" color="text.secondary">
              Quantity per Waste Category
            </Typography>

            {WASTE_CATEGORIES.map((cat) => (
              <Box key={cat} display="flex" alignItems="center" gap={2}>
                <Box
                  sx={{
                    width: 20, height: 20, borderRadius: '50%',
                    bgcolor: CATEGORY_COLORS[cat], flexShrink: 0,
                    border: cat === 'WHITE' ? '1px solid #ccc' : 'none',
                  }}
                />
                <Typography sx={{ width: 70, fontWeight: 'bold' }}>{cat}</Typography>
                <TextField
                  type="number"
                  size="small"
                  value={generateForm.categoryQuantities[cat] || 0}
                  onChange={(e) => setCategoryQty(cat, parseInt(e.target.value) || 0)}
                  inputProps={{ min: 0, max: 500, style: { textAlign: 'center' } }}
                  sx={{ width: 120 }}
                />
                <Typography variant="caption" color="text.secondary">
                  {Math.ceil((generateForm.categoryQuantities[cat] || 0) / 9)} page(s)
                </Typography>
              </Box>
            ))}

            <Alert severity={totalLabels > 500 ? 'error' : 'info'} sx={{ py: 0.5 }}>
              <strong>Total: {totalLabels} labels</strong> &mdash;{' '}
              {Math.ceil(totalLabels / 9)} page(s) &bull; 9 labels per page
              {totalLabels > 500 && ' — Max 500 labels per batch!'}
            </Alert>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenerateDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleGenerate}
            disabled={generateMutation.isPending || totalLabels < 1 || totalLabels > 500}
            startIcon={generateMutation.isPending ? <CircularProgress size={16} /> : <PdfIcon />}
          >
            {generateMutation.isPending ? 'Generating...' : `Generate ${totalLabels} Labels`}
          </Button>
        </DialogActions>
      </Dialog>

      {/* View QR Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="xs">
        <DialogTitle>QR Code</DialogTitle>
        <DialogContent>
          {selectedQr && (
            <Box textAlign="center">
              <img src={qrImageUrl} alt="QR Code" style={{ maxWidth: '100%' }} />
              <Typography variant="h6" mt={2}>{selectedQr.hcfName}</Typography>
              <Chip
                label={selectedQr.wasteCategory}
                sx={{
                  bgcolor: CATEGORY_COLORS[selectedQr.wasteCategory],
                  color: selectedQr.wasteCategory === 'WHITE' ? 'black' : 'white',
                  mt: 1,
                }}
              />
              <Typography variant="body2" color="text.secondary" mt={1}>
                Valid: {formatDate(selectedQr.validFrom)} - {formatDate(selectedQr.validTo)}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button variant="outlined" startIcon={<DownloadIcon />} onClick={handleDownloadQr}>
            Download PNG
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
