import { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Switch,
  TextField,
  Button,
  Alert,
  Divider,
  Chip,
  Stack,
  IconButton,
  InputAdornment,
  alpha,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Visibility,
  VisibilityOff,
  Check as CheckIcon,
  Warning as WarningIcon,
  CreditCard as CardIcon,
  AccountBalance as BankIcon,
  Sync as SyncIcon,
} from '@mui/icons-material';

interface GatewayConfig {
  name: string;
  enabled: boolean;
  testMode: boolean;
  keyId: string;
  keySecret: string;
  webhookSecret: string;
  status: 'connected' | 'disconnected' | 'error';
}

const defaultGateways: GatewayConfig[] = [
  {
    name: 'Razorpay',
    enabled: true,
    testMode: true,
    keyId: 'rzp_test_xxxxxxxxxxxx',
    keySecret: '',
    webhookSecret: '',
    status: 'connected',
  },
];

export default function PaymentGateway() {
  const [gateways, setGateways] = useState<GatewayConfig[]>(defaultGateways);
  const [showSecrets, setShowSecrets] = useState<Record<string, boolean>>({});
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);

  const toggleGateway = (index: number) => {
    setGateways(prev => prev.map((g, i) => 
      i === index ? { ...g, enabled: !g.enabled } : g
    ));
  };

  const toggleTestMode = (index: number) => {
    setGateways(prev => prev.map((g, i) => 
      i === index ? { ...g, testMode: !g.testMode } : g
    ));
  };

  const updateField = (index: number, field: keyof GatewayConfig, value: string) => {
    setGateways(prev => prev.map((g, i) => 
      i === index ? { ...g, [field]: value } : g
    ));
  };

  const handleSave = async () => {
    setSaving(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    setSaving(false);
    setSuccess(true);
    setTimeout(() => setSuccess(false), 3000);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'success';
      case 'error': return 'error';
      default: return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Payment Gateway Manager
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Configure payment gateways for subscription billing and HCF payments
        </Typography>
      </Box>

      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Payment gateway configuration saved successfully.
        </Alert>
      )}

      <Alert severity="info" sx={{ mb: 3 }}>
        Payment gateway integration is used for:
        <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
          <li>CBWTF subscription payments</li>
          <li>HCF platform fee collection (coming soon)</li>
          <li>Automated invoice payments</li>
        </ul>
      </Alert>

      <Grid container spacing={3}>
        {gateways.map((gateway, index) => (
          <Grid item key={gateway.name} xs={12} lg={6}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                {/* Header */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Box
                      sx={{
                        width: 48,
                        height: 48,
                        borderRadius: 2,
                        bgcolor: alpha(gateway.enabled ? '#10B981' : '#9CA3AF', 0.12),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {gateway.name === 'Razorpay' ? (
                        <CardIcon sx={{ color: gateway.enabled ? '#10B981' : '#9CA3AF' }} />
                      ) : (
                        <BankIcon sx={{ color: gateway.enabled ? '#10B981' : '#9CA3AF' }} />
                      )}
                    </Box>
                    <Box>
                      <Typography variant="h6" sx={{ fontWeight: 600 }}>
                        {gateway.name}
                      </Typography>
                      <Chip
                        size="small"
                        icon={gateway.status === 'connected' ? <CheckIcon /> : <WarningIcon />}
                        label={gateway.status}
                        color={getStatusColor(gateway.status) as 'success' | 'error' | 'default'}
                        variant="outlined"
                      />
                    </Box>
                  </Box>
                  <Switch
                    checked={gateway.enabled}
                    onChange={() => toggleGateway(index)}
                    color="success"
                  />
                </Box>

                <Divider sx={{ my: 2 }} />

                {/* Test Mode Toggle */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Test Mode (Sandbox)
                  </Typography>
                  <Switch
                    checked={gateway.testMode}
                    onChange={() => toggleTestMode(index)}
                    disabled={!gateway.enabled}
                    size="small"
                  />
                </Box>

                {gateway.testMode && gateway.enabled && (
                  <Alert severity="warning" sx={{ mb: 2 }}>
                    Test mode enabled - no real transactions will be processed
                  </Alert>
                )}

                {/* API Keys */}
                <Stack spacing={2}>
                  <TextField
                    label="API Key ID"
                    value={gateway.keyId}
                    onChange={(e) => updateField(index, 'keyId', e.target.value)}
                    disabled={!gateway.enabled}
                    fullWidth
                    size="small"
                    placeholder={`${gateway.name.toLowerCase()}_test_xxxxxxxxxxxx`}
                  />
                  <TextField
                    label="API Key Secret"
                    type={showSecrets[`${index}-secret`] ? 'text' : 'password'}
                    value={gateway.keySecret}
                    onChange={(e) => updateField(index, 'keySecret', e.target.value)}
                    disabled={!gateway.enabled}
                    fullWidth
                    size="small"
                    placeholder="Enter secret key"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={() => setShowSecrets(prev => ({
                              ...prev,
                              [`${index}-secret`]: !prev[`${index}-secret`]
                            }))}
                            edge="end"
                            size="small"
                          >
                            {showSecrets[`${index}-secret`] ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                  <TextField
                    label="Webhook Secret"
                    type={showSecrets[`${index}-webhook`] ? 'text' : 'password'}
                    value={gateway.webhookSecret}
                    onChange={(e) => updateField(index, 'webhookSecret', e.target.value)}
                    disabled={!gateway.enabled}
                    fullWidth
                    size="small"
                    placeholder="Webhook signing secret"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={() => setShowSecrets(prev => ({
                              ...prev,
                              [`${index}-webhook`]: !prev[`${index}-webhook`]
                            }))}
                            edge="end"
                            size="small"
                          >
                            {showSecrets[`${index}-webhook`] ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Stack>

                {/* Test Connection */}
                <Box sx={{ mt: 2 }}>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<SyncIcon />}
                    disabled={!gateway.enabled || !gateway.keyId}
                  >
                    Test Connection
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Save Button */}
      <Box sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          variant="contained"
          size="large"
          onClick={handleSave}
          disabled={saving}
        >
          {saving ? 'Saving...' : 'Save Configuration'}
        </Button>
      </Box>

      {/* Webhook URLs */}
      <Card sx={{ mt: 4 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Webhook URLs
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Configure these URLs in your payment gateway dashboard to receive payment notifications.
          </Typography>
          <Stack spacing={1}>
            <Box sx={{ p: 1.5, bgcolor: 'grey.100', borderRadius: 1, fontFamily: 'monospace', fontSize: '0.875rem' }}>
              <strong>Razorpay:</strong> https://api.smartcbwtf.com/webhooks/razorpay
            </Box>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
