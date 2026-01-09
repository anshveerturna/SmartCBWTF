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
} from '@mui/material';
import {
  QrCode,
  Add,
  Download,
  CheckCircle,
  Warning,
  Schedule,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';

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
  isActive: boolean;
  isUsable: boolean;
}

const QrLabels: React.FC = () => {
  const queryClient = useQueryClient();
  const [category, setCategory] = useState('YELLOW');
  const [quantity, setQuantity] = useState(10);
  const [validityDays, setValidityDays] = useState(30);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

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
        quantity,
        validityDays,
      });
      return res.data;
    },
    onSuccess: (data) => {
      setSuccess(`Successfully generated ${data.generated} QR labels`);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['hcf-qr-labels'] });
    },
    onError: (err: Error & { response?: { data?: { message?: string } } }) => {
      setError(err.response?.data?.message || 'Failed to generate labels');
      setSuccess(null);
    },
  });

  const handleGenerate = () => {
    setError(null);
    setSuccess(null);
    generateMutation.mutate();
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
    }
    return <Chip label={label.status} size="small" />;
  };

  const getCategoryColor = (code: string) => {
    return WASTE_CATEGORIES.find(c => c.code === code)?.color || '#9E9E9E';
  };

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
            <Grid size={{ xs: 12, sm: 4 }}>
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
                label="Quantity"
                type="number"
                fullWidth
                value={quantity}
                onChange={(e) => setQuantity(Math.min(100, Math.max(1, Number(e.target.value))))}
                inputProps={{ min: 1, max: 100 }}
                helperText="1-100 labels"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField
                label="Validity (Days)"
                type="number"
                fullWidth
                value={validityDays}
                onChange={(e) => setValidityDays(Math.min(365, Math.max(1, Number(e.target.value))))}
                inputProps={{ min: 1, max: 365 }}
                helperText="1-365 days"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
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
            </Typography>
            <Button startIcon={<Download />} variant="outlined" size="small">
              Export PDF
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
                    <TableCell>Category</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Valid From</TableCell>
                    <TableCell>Valid Until</TableCell>
                    <TableCell>Created</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {labelsData?.labels?.slice(0, 50).map((label) => (
                    <TableRow key={label.id} hover>
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
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default QrLabels;
