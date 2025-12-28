import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
  IconButton,
  Tooltip,
  Alert,
  Snackbar,
  CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  QrCode as QrIcon,
  Block as RevokeIcon,
  Visibility as ViewIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { listQrs, generateQr, revokeQr, getHcfList, type QrDetail, type QrGenerateRequest } from '../../api/cbwtf';
import QRCode from 'qrcode';

const WASTE_CATEGORIES = ['YELLOW', 'RED', 'BLUE', 'WHITE'] as const;

const STATUS_COLORS: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
  ACTIVE: 'success',
  USED: 'warning',
  VERIFIED: 'info',
  EXPIRED: 'default',
  REVOKED: 'error',
  BLOCKED: 'error',
};

const CATEGORY_COLORS: Record<string, string> = {
  YELLOW: '#FFC107',
  RED: '#F44336',
  BLUE: '#2196F3',
  WHITE: '#9E9E9E',
};

export default function QrLabels() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [generateDialogOpen, setGenerateDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedQr, setSelectedQr] = useState<QrDetail | null>(null);
  const [qrImageUrl, setQrImageUrl] = useState<string>('');
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Generate form state
  const [generateForm, setGenerateForm] = useState({
    hcfId: '',
    wasteCategory: '' as 'YELLOW' | 'RED' | 'BLUE' | 'WHITE' | '',
    validFrom: new Date().toISOString().slice(0, 16),
    validTo: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
  });

  // Fetch QRs
  const { data: qrs = [], isLoading } = useQuery({
    queryKey: ['cbwtf-qrs', statusFilter],
    queryFn: () => listQrs(undefined, statusFilter || undefined),
  });

  // Fetch HCFs for dropdown
  const { data: hcfs = [] } = useQuery({
    queryKey: ['cbwtf-hcfs-for-qr'],
    queryFn: () => getHcfList(),
  });

  // Generate mutation
  const generateMutation = useMutation({
    mutationFn: (data: QrGenerateRequest) => generateQr(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf-qrs'] });
      setGenerateDialogOpen(false);
      setSnackbar({ open: true, message: 'QR Code generated successfully!', severity: 'success' });
      setGenerateForm({
        hcfId: '',
        wasteCategory: '',
        validFrom: new Date().toISOString().slice(0, 16),
        validTo: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.error || error?.response?.data?.message || error?.message || 'Failed to generate QR';
      setSnackbar({ open: true, message, severity: 'error' });
    },
  });

  // Revoke mutation
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

  const handleGenerate = () => {
    if (!generateForm.hcfId || !generateForm.wasteCategory) {
      setSnackbar({ open: true, message: 'Please fill all required fields', severity: 'error' });
      return;
    }
    generateMutation.mutate({
      hcfId: generateForm.hcfId,
      wasteCategory: generateForm.wasteCategory as 'YELLOW' | 'RED' | 'BLUE' | 'WHITE',
      validFrom: new Date(generateForm.validFrom).toISOString(),
      validTo: new Date(generateForm.validTo).toISOString(),
    });
  };

  const handleViewQr = async (qr: QrDetail) => {
    setSelectedQr(qr);
    try {
      const url = await QRCode.toDataURL(qr.qrPayloadJson, { width: 300, margin: 2 });
      setQrImageUrl(url);
      setViewDialogOpen(true);
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to generate QR image', severity: 'error' });
    }
  };

  const handleDownloadQr = () => {
    if (qrImageUrl && selectedQr) {
      const link = document.createElement('a');
      link.download = `QR-${selectedQr.hcfName}-${selectedQr.wasteCategory}.png`;
      link.href = qrImageUrl;
      link.click();
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <QrIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" fontWeight="bold">
            QR Authorization
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setGenerateDialogOpen(true)}
        >
          Generate QR
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ py: 2 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
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

      {/* QR Table */}
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
                    No QR codes found. Click "Generate QR" to create one.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              qrs.map((qr) => (
                <TableRow key={qr.id} hover>
                  <TableCell>
                    <Typography fontWeight="medium">{qr.hcfName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {qr.agreementNumber}
                    </Typography>
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
                    <Typography variant="body2">
                      {formatDate(qr.validFrom)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      to {formatDate(qr.validTo)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={qr.status}
                      size="small"
                      color={STATUS_COLORS[qr.status]}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {formatDate(qr.createdAt)}
                    </Typography>
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

      {/* Generate Dialog */}
      <Dialog open={generateDialogOpen} onClose={() => setGenerateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate QR Code</DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel>Select HCF</InputLabel>
              <Select
                value={generateForm.hcfId}
                label="Select HCF"
                onChange={(e) => setGenerateForm({ ...generateForm, hcfId: e.target.value })}
              >
                {hcfs.map((hcf) => (
                  <MenuItem key={hcf.id} value={hcf.id}>
                    {hcf.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth required>
              <InputLabel>Waste Category</InputLabel>
              <Select
                value={generateForm.wasteCategory}
                label="Waste Category"
                onChange={(e) => setGenerateForm({ ...generateForm, wasteCategory: e.target.value as any })}
              >
                {WASTE_CATEGORIES.map((cat) => (
                  <MenuItem key={cat} value={cat}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Box
                        sx={{
                          width: 16,
                          height: 16,
                          borderRadius: '50%',
                          bgcolor: CATEGORY_COLORS[cat],
                        }}
                      />
                      {cat}
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Valid From"
              type="datetime-local"
              fullWidth
              value={generateForm.validFrom}
              onChange={(e) => setGenerateForm({ ...generateForm, validFrom: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="Valid To"
              type="datetime-local"
              fullWidth
              value={generateForm.validTo}
              onChange={(e) => setGenerateForm({ ...generateForm, validTo: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenerateDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleGenerate}
            disabled={generateMutation.isPending}
            startIcon={generateMutation.isPending && <CircularProgress size={16} />}
          >
            Generate
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
              <Typography variant="h6" mt={2}>
                {selectedQr.hcfName}
              </Typography>
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
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            onClick={handleDownloadQr}
          >
            Download
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
