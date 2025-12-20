import { Box, Card, CardContent, Typography, Alert } from '@mui/material';
import { Receipt as ReceiptIcon } from '@mui/icons-material';

export default function Invoices() {
  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Invoices</Typography>
        <Typography variant="body2" color="text.secondary">View and download HCF invoices</Typography>
      </Box>

      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ textAlign: 'center', py: 8 }}>
          <ReceiptIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">Invoice Management</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            View and download invoices for HCF collections
          </Typography>
          <Alert severity="info" sx={{ mt: 3, maxWidth: 400, mx: 'auto' }}>
            This feature is coming soon. Invoice generation will be available after HCF billing is enabled.
          </Alert>
        </CardContent>
      </Card>
    </Box>
  );
}
