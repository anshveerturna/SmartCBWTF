import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  CircularProgress,
  Alert,
  IconButton,
  Tooltip,
  Chip,
  Button,
} from '@mui/material';
import {
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Receipt as InvoiceIcon,
} from '@mui/icons-material';
import { listInvoices, downloadInvoiceById } from '../../../api/cbwtf';
import type { InvoiceSummary } from '../../../api/cbwtf';

// Format currency
const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(amount);
};

// Format date
const formatMonth = (dateStr: string | null) => {
  if (!dateStr) return 'N/A';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'long' });
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

export default function InvoiceList() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [downloading, setDownloading] = useState<string | null>(null);

  // Fetch invoices
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['invoices', page, rowsPerPage],
    queryFn: () => listInvoices(page, rowsPerPage),
  });

  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleView = (invoice: InvoiceSummary) => {
    // Navigate to bill detail page (invoices are linked to bills)
    navigate(`/cbwtf/finance/invoices/${invoice.id}`);
  };

  const handleDownload = async (invoice: InvoiceSummary) => {
    setDownloading(invoice.id);
    try {
      const blob = await downloadInvoiceById(invoice.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice_${invoice.invoiceNumber.replace(/\//g, '_')}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to download invoice:', err);
    } finally {
      setDownloading(null);
    }
  };

  return (
    <Box>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Invoices
          </Typography>
          <Typography variant="body2" color="text.secondary">
            GST-compliant invoices generated from bills
          </Typography>
        </Box>
        <Button
          startIcon={<RefreshIcon />}
          variant="outlined"
          onClick={() => refetch()}
        >
          Refresh
        </Button>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load invoices: {(error as Error).message}
        </Alert>
      )}

      {/* Read-only Info */}
      <Alert severity="info" sx={{ mb: 2 }}>
        Invoices are legal GST documents. They cannot be edited or deleted.
      </Alert>

      {/* Invoice Table */}
      <Paper sx={{ mb: 2 }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Invoice #</TableCell>
                <TableCell>HCF Name</TableCell>
                <TableCell>Billing Month</TableCell>
                <TableCell>Invoice Date</TableCell>
                <TableCell align="right">Total Amount</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    <CircularProgress size={30} />
                  </TableCell>
                </TableRow>
              ) : data?.content?.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    <Box py={4} display="flex" flexDirection="column" alignItems="center">
                      <InvoiceIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
                      <Typography color="text.secondary">No invoices found</Typography>
                      <Typography variant="caption" color="text.disabled">
                        Invoices are generated from finalized bills
                      </Typography>
                    </Box>
                  </TableCell>
                </TableRow>
              ) : (
                data?.content?.map((invoice) => (
                  <TableRow key={invoice.id} hover>
                    <TableCell>
                      <Chip
                        label={invoice.invoiceNumber}
                        size="small"
                        color="primary"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{invoice.hcfName}</TableCell>
                    <TableCell>{formatMonth(invoice.billingMonth)}</TableCell>
                    <TableCell>{formatDate(invoice.invoiceDate)}</TableCell>
                    <TableCell align="right">
                      <Typography fontWeight="bold">
                        {formatCurrency(invoice.totalAmount)}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="View Invoice Details">
                        <IconButton
                          size="small"
                          onClick={() => handleView(invoice)}
                        >
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Download PDF">
                        <IconButton
                          size="small"
                          onClick={() => handleDownload(invoice)}
                          disabled={downloading === invoice.id}
                        >
                          {downloading === invoice.id ? (
                            <CircularProgress size={18} />
                          ) : (
                            <DownloadIcon />
                          )}
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={data?.totalElements || 0}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Paper>
    </Box>
  );
}
