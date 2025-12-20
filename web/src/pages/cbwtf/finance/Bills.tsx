import { Box, Card, CardContent, Typography, Alert } from '@mui/material';
import { Description as BillsIcon } from '@mui/icons-material';

export default function Bills() {
  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Bills & Payments</Typography>
        <Typography variant="body2" color="text.secondary">Track payment history and pending bills</Typography>
      </Box>

      <Card sx={{ borderRadius: 2 }}>
        <CardContent sx={{ textAlign: 'center', py: 8 }}>
          <BillsIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">Payment Tracking</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            View pending bills and payment history from HCFs
          </Typography>
          <Alert severity="info" sx={{ mt: 3, maxWidth: 400, mx: 'auto' }}>
            This feature is coming soon. Payment tracking will be available after invoice generation.
          </Alert>
        </CardContent>
      </Card>
    </Box>
  );
}
