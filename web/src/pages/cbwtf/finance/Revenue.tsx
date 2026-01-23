import { Box, Card, CardContent, Typography, Alert, Grid } from '@mui/material';
import { TrendingUp as RevenueIcon, AccountBalanceWallet as WalletIcon } from '@mui/icons-material';

export default function Revenue() {
  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Revenue Dashboard</Typography>
        <Typography variant="body2" color="text.secondary">Earnings overview and financial reports</Typography>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', color: '#fff' }}>
            <CardContent>
              <WalletIcon sx={{ fontSize: 40, opacity: 0.8 }} />
              <Typography variant="h4" fontWeight={700} sx={{ mt: 1 }}>₹0</Typography>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Total Revenue (This Month)</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', color: '#fff' }}>
            <CardContent>
              <RevenueIcon sx={{ fontSize: 40, opacity: 0.8 }} />
              <Typography variant="h4" fontWeight={700} sx={{ mt: 1 }}>₹0</Typography>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Pending Payments</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)', color: '#fff' }}>
            <CardContent>
              <WalletIcon sx={{ fontSize: 40, opacity: 0.8 }} />
              <Typography variant="h4" fontWeight={700} sx={{ mt: 1 }}>0</Typography>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Active HCF Contracts</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ borderRadius: 2, mt: 3 }}>
        <CardContent sx={{ textAlign: 'center', py: 6 }}>
          <Alert severity="info" sx={{ maxWidth: 500, mx: 'auto' }}>
            Revenue tracking will be available once HCF billing and invoicing is enabled. 
            Set up your bank accounts to receive payments.
          </Alert>
        </CardContent>
      </Card>
    </Box>
  );
}
