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
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';
import QRCode from 'qrcode';
import { jsPDF } from 'jspdf';

// Waste categories with colors
const WASTE_CATEGORIES = [
  { code: 'YELLOW', name: 'Infectious Waste', color: '#FFEB3B' },
  { code: 'RED', name: 'Contaminated Recyclables', color: '#F44336' },
  { code: 'BLUE', name: 'Glassware Waste', color: '#2196F3' },
  { code: 'WHITE', name: 'Sharps Waste', color: '#9E9E9E' },
];

interface QrLabel {
  id: string;
  wasteCategory: string;
  status: string;
  validFrom: string;
  validTo: string;
  createdAt: string;
  qrPayload: string;
  isActive: boolean;
  isUsable: boolean;
}

// Helper to format date as YYYY-MM-DD
const formatDateForInput = (date: Date) => {
  return date.toISOString().split('T')[0];
};

const QrLabels: React.FC = () => {
  const queryClient = useQueryClient();
  const [category, setCategory] = useState('YELLOW');
  const [validFrom, setValidFrom] = useState(formatDateForInput(new Date()));
  const [validTo, setValidTo] = useState(formatDateForInput(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)));
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Selection state
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  
  // View dialog state
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedQr, setSelectedQr] = useState<QrLabel | null>(null);
  const [qrImageUrl, setQrImageUrl] = useState<string>('');

  // Fetch labels
  const { data: labelsData, isLoading } = useQuery({
    queryKey: ['hcf-qr-labels'],
    queryFn: async () => {
      const res = await apiClient.get('/api/hcf/qr-labels');
      return res.data as { labels: QrLabel[]; total: number };
    },
  });

  // Generate mutation
  const generateMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post('/api/hcf/qr-labels/generate', {
        wasteCategory: category,
        validFrom: new Date(validFrom).toISOString(),
        validTo: new Date(validTo + 'T23:59:59').toISOString(),
      });
      return res.data;
    },
    onSuccess: () => {
      setSuccess('QR label generated successfully');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['hcf-qr-labels'] });
    },
    onError: (err: Error & { response?: { data?: { message?: string } } }) => {
      setError(err.response?.data?.message || 'Failed to generate label');
      setSuccess(null);
    },
  });

  // Deactivate mutation
  const deactivateMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.put(`/api/hcf/qr-labels/${id}/deactivate`);
      return res.data;
    },
    onSuccess: () => {
      setSuccess('QR label deactivated successfully');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['hcf-qr-labels'] });
    },
    onError: (err: Error & { response?: { data?: { message?: string } } }) => {
      setError(err.response?.data?.message || 'Failed to deactivate label');
      setSuccess(null);
    },
  });

  const handleGenerate = () => {
    setError(null);
    setSuccess(null);
    generateMutation.mutate();
  };

  const handleViewQr = async (label: QrLabel) => {
    setSelectedQr(label);
    try {
      const url = await QRCode.toDataURL(label.qrPayload, { width: 300, margin: 2 });
      setQrImageUrl(url);
      setViewDialogOpen(true);
    } catch (err) {
      setError('Failed to generate QR image');
    }
  };

  const handleDownloadQr = () => {
    if (qrImageUrl && selectedQr) {
      const link = document.createElement('a');
      const dateStr = new Date(selectedQr.validFrom).toLocaleDateString('en-IN').replace(/\//g, '-');
      link.download = `QR-${selectedQr.wasteCategory}-${dateStr}.png`;
      link.href = qrImageUrl;
      link.click();
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked && labelsData?.labels) {
      setSelectedIds(new Set(labelsData.labels.map(l => l.id)));
    } else {
      setSelectedIds(new Set());
    }
  };

  const handleSelectOne = (id: string, checked: boolean) => {
    const newSelected = new Set(selectedIds);
    if (checked) {
      newSelected.add(id);
    } else {
      newSelected.delete(id);
    }
    setSelectedIds(newSelected);
  };

  const handleExportPdf = async () => {
    if (selectedIds.size === 0 || !labelsData?.labels) return;

    const selectedLabels = labelsData.labels.filter(l => selectedIds.has(l.id));
    const pdf = new jsPDF();
    
    for (let i = 0; i < selectedLabels.length; i++) {
      const label = selectedLabels[i];
      
      if (i > 0) {
        pdf.addPage();
      }

      try {
        // Generate QR code image
        const qrDataUrl = await QRCode.toDataURL(label.qrPayload, { width: 200, margin: 2 });
        
        // Add title
        pdf.setFontSize(20);
        pdf.setFont('helvetica', 'bold');
        pdf.text('SmartCBWTF - QR Label', 105, 30, { align: 'center' });
        
        // Add QR code image
        pdf.addImage(qrDataUrl, 'PNG', 55, 45, 100, 100);
        
        // Add details
        pdf.setFontSize(14);
        pdf.setFont('helvetica', 'normal');
        
        const category = WASTE_CATEGORIES.find(c => c.code === label.wasteCategory);
        pdf.text(`Category: ${category?.name || label.wasteCategory}`, 105, 160, { align: 'center' });
        
        pdf.setFontSize(12);
        pdf.text(`Valid From: ${new Date(label.validFrom).toLocaleDateString('en-IN')}`, 105, 175, { align: 'center' });
        pdf.text(`Valid To: ${new Date(label.validTo).toLocaleDateString('en-IN')}`, 105, 190, { align: 'center' });
        pdf.text(`Status: ${label.status}`, 105, 205, { align: 'center' });
        
        // Add footer
        pdf.setFontSize(10);
        pdf.setTextColor(128);
        pdf.text(`Generated: ${new Date(label.createdAt).toLocaleString('en-IN')}`, 105, 280, { align: 'center' });
        pdf.text(`ID: ${label.id}`, 105, 287, { align: 'center' });
        pdf.setTextColor(0);
      } catch (err) {
        console.error('Error generating QR for PDF:', err);
      }
    }

    pdf.save(`QR-Labels-${new Date().toISOString().slice(0, 10)}.pdf`);
    setSuccess(`Exported ${selectedLabels.length} QR label(s) to PDF`);
  };

  const getStatusChip = (label: QrLabel) => {
    if (label.status === 'ACTIVE' && label.isUsable) {
      return <Chip icon={<CheckCircle />} label="Active" size="small" color="success" />;
    } else if (label.status === 'ACTIVE' && !label.isUsable) {
      return <Chip icon={<Schedule />} label="Scheduled" size="small" color="info" />;
    } else if (label.status === 'EXPIRED') {
      return <Chip icon={<Warning />} label="Expired" size="small" color="default" />;
    } else if (label.status === 'USED') {
      return <Chip label="Used" size="small" color="warning" />;
    } else if (label.status === 'VERIFIED') {
      return <Chip label="Verified" size="small" color="success" />;
    } else if (label.status === 'REVOKED') {
      return <Chip label="Revoked" size="small" color="error" />;
    }
    return <Chip label={label.status} size="small" />;
  };

  const getCategoryColor = (code: string) => {
    return WASTE_CATEGORIES.find(c => c.code === code)?.color || '#9E9E9E';
  };

  const isAllSelected = labelsData?.labels && labelsData.labels.length > 0 && 
    labelsData.labels.every(l => selectedIds.has(l.id));
  const isSomeSelected = selectedIds.size > 0;

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          QR Labels
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Generate and manage QR labels for waste collection
        </Typography>
      </Box>

      {/* Alerts */}
      {error && <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      {/* Generation Form */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
            <QrCode /> Generate New Labels
          </Typography>
          <Grid container spacing={3} alignItems="flex-end">
            <Grid size={{ xs: 12, sm: 3 }}>
              <FormControl fullWidth>
                <InputLabel>Waste Category</InputLabel>
                <Select
                  value={category}
                  label="Waste Category"
                  onChange={(e) => setCategory(e.target.value)}
                >
                  {WASTE_CATEGORIES.map((cat) => (
                    <MenuItem key={cat.code} value={cat.code}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box
                          sx={{
                            width: 16,
                            height: 16,
                            borderRadius: 1,
                            bgcolor: cat.color,
                            border: cat.code === 'WHITE' ? '1px solid #999' : 'none',
                          }}
                        />
                        {cat.name}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Valid From"
                type="date"
                fullWidth
                value={validFrom}
                onChange={(e) => setValidFrom(e.target.value)}
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: formatDateForInput(new Date()) }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Valid To"
                type="date"
                fullWidth
                value={validTo}
                onChange={(e) => setValidTo(e.target.value)}
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: validFrom }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <Button
                variant="contained"
                fullWidth
                startIcon={generateMutation.isPending ? <CircularProgress size={20} /> : <Add />}
                onClick={handleGenerate}
                disabled={generateMutation.isPending}
                sx={{ height: 56 }}
              >
                Generate
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Labels Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">
              Generated Labels ({labelsData?.total || 0})
              {selectedIds.size > 0 && (
                <Typography component="span" color="primary.main" sx={{ ml: 2 }}>
                  {selectedIds.size} selected
                </Typography>
              )}
            </Typography>
            <Button 
              startIcon={<PictureAsPdf />} 
              variant="outlined" 
              size="small"
              disabled={selectedIds.size === 0}
              onClick={handleExportPdf}
            >
              Export PDF ({selectedIds.size})
            </Button>
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : labelsData?.labels?.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center', bgcolor: alpha('#6366F1', 0.05) }}>
              <QrCode sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
              <Typography color="text.secondary">
                No QR labels generated yet. Use the form above to create labels.
              </Typography>
            </Paper>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={isAllSelected}
                        indeterminate={isSomeSelected && !isAllSelected}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                      />
                    </TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Valid From</TableCell>
                    <TableCell>Valid Until</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {labelsData?.labels?.slice(0, 50).map((label) => (
                    <TableRow key={label.id} hover selected={selectedIds.has(label.id)}>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selectedIds.has(label.id)}
                          onChange={(e) => handleSelectOne(label.id, e.target.checked)}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box
                            sx={{
                              width: 12,
                              height: 12,
                              borderRadius: 0.5,
                              bgcolor: getCategoryColor(label.wasteCategory),
                              border: label.wasteCategory === 'WHITE' ? '1px solid #999' : 'none',
                            }}
                          />
                          {label.wasteCategory}
                        </Box>
                      </TableCell>
                      <TableCell>{getStatusChip(label)}</TableCell>
                      <TableCell>{new Date(label.validFrom).toLocaleDateString()}</TableCell>
                      <TableCell>{new Date(label.validTo).toLocaleDateString()}</TableCell>
                      <TableCell>{new Date(label.createdAt).toLocaleDateString()}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="View QR Code">
                          <IconButton size="small" onClick={() => handleViewQr(label)} color="primary">
                            <Visibility />
                          </IconButton>
                        </Tooltip>
                        {(label.status === 'ACTIVE' || label.isUsable) && (
                          <Tooltip title="Deactivate">
                            <IconButton 
                              size="small" 
                              onClick={() => deactivateMutation.mutate(label.id)}
                              color="error"
                              disabled={deactivateMutation.isPending}
                            >
                              <Block />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* View QR Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>QR Label</DialogTitle>
        <DialogContent>
          {selectedQr && (
            <Box textAlign="center">
              <img src={qrImageUrl} alt="QR Code" style={{ maxWidth: '100%' }} />
              <Typography variant="h6" mt={2}>
                {WASTE_CATEGORIES.find(c => c.code === selectedQr.wasteCategory)?.name || selectedQr.wasteCategory}
              </Typography>
              <Box sx={{ mt: 1 }}>
                {getStatusChip(selectedQr)}
              </Box>
              <Typography variant="body2" color="text.secondary" mt={2}>
                Valid: {new Date(selectedQr.validFrom).toLocaleDateString()} - {new Date(selectedQr.validTo).toLocaleDateString()}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button
            variant="contained"
            startIcon={<Download />}
            onClick={handleDownloadQr}
          >
            Download PNG
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default QrLabels;
