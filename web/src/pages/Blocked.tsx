import React from 'react';
import { Box, Typography, Button, Card, CardContent, alpha } from '@mui/material';
import { Block as BlockIcon, SupportAgent as SupportIcon } from '@mui/icons-material';

/**
 * Blocked page shown when subscription is inactive/expired.
 * Reads reason from sessionStorage (set by API client).
 */
const Blocked: React.FC = () => {
  const reason = sessionStorage.getItem('blocked_reason') || 'Your subscription is inactive.';

  const handleContactSupport = () => {
    // Could open mailto or support page
    window.location.href = 'mailto:support@smartcbwtf.com?subject=Subscription%20Issue';
  };

  const handleReturnToLogin = () => {
    sessionStorage.removeItem('blocked_reason');
    window.location.href = '/login';
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 3,
      }}
    >
      <Card sx={{ maxWidth: 480, textAlign: 'center' }}>
        <CardContent sx={{ p: 4 }}>
          <Box
            sx={{
              width: 80,
              height: 80,
              borderRadius: '50%',
              bgcolor: (theme) => alpha(theme.palette.error.main, 0.1),
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mx: 'auto',
              mb: 3,
            }}
          >
            <BlockIcon sx={{ fontSize: 40, color: 'error.main' }} />
          </Box>

          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700 }}>
            Account Blocked
          </Typography>

          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            {reason}
          </Typography>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
            Please contact your administrator or our support team to resolve this issue.
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Button
              variant="contained"
              color="primary"
              startIcon={<SupportIcon />}
              onClick={handleContactSupport}
              fullWidth
            >
              Contact Support
            </Button>
            <Button
              variant="outlined"
              onClick={handleReturnToLogin}
              fullWidth
            >
              Return to Login
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Blocked;
