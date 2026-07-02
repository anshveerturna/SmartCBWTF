import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Card,
  Container,
  Typography,
  CircularProgress,
  Chip,
  Divider,
  Grid,
  Button,
  ThemeProvider,
  createTheme,
  CssBaseline,
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  LocalHospital as HospitalIcon,
  VerifiedUser as VerifiedIcon,
  CalendarMonth as CalendarIcon,
  Description as DescriptionIcon
} from '@mui/icons-material';
import { API_BASE_URL } from '../../api/client';

// Force Light Theme for this page
const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0F766E', // Teal 700
      light: '#14B8A6', // Teal 500
      dark: '#0D9488', // Teal 600
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#475569', // Slate 600
      light: '#94A3B8', // Slate 400
      dark: '#334155', // Slate 700
      contrastText: '#ffffff',
    },
    background: {
      default: '#F8FAFC', // Slate 50
      paper: '#FFFFFF',
    },
    text: {
      primary: '#1E293B', // Slate 800
      secondary: '#64748B', // Slate 500
    },
    success: {
      main: '#22C55E', // Green 500
      light: '#DCFCE7', // Green 100
      contrastText: '#14532D', // Green 900
    },
    error: {
      main: '#EF4444', // Red 500
      light: '#FEE2E2', // Red 100
      contrastText: '#7F1D1D', // Red 900
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 700,
      color: '#0F172A', // Slate 900
    },
    h6: {
      fontWeight: 600,
      color: '#334155', // Slate 700
    },
    subtitle1: {
      color: '#475569', // Slate 600
    },
    body2: {
      color: '#64748B', // Slate 500
    },
  },
  shape: {
    borderRadius: 12, // Modern rounded corners
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
        },
      },
    },
  },
});

interface AgreementVerificationDTO {
  verified: boolean;
  agreementId: string | null;
  agreementNumber: string | null;
  status: string | null;
  facilityName: string | null;
  facilityCode: string | null;
  hcfName: string | null;
  hcfCode: string | null;
  startDate: string | null;
  endDate: string | null;
  version: number | null;
  createdAt: string | null;
  message: string;
}

const displayValue = (value?: string | number | null) => value == null || value === '' ? '-' : String(value);

const formatStatus = (status?: string | null) => displayValue(status).replace(/_/g, ' ');

const formatDate = (value?: string | null) => {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('en-IN');
};

interface LegacyAgreementVerificationDTO {
  valid?: boolean;
  agreementNumber: string;
  validFrom: string;
  validUntil: string;
}

const AgreementVerify: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [data, setData] = useState<AgreementVerificationDTO | null>(null);
  const [loading, setLoading] = useState(() => Boolean(id));
  const [error, setError] = useState<string | null>(() => id ? null : 'Agreement reference is missing');

  useEffect(() => {
    if (!id) {
      return;
    }
    // Use pure fetch to bypass axios interceptors that might attach auth headers
    fetch(`${API_BASE_URL}/api/public/agreement/verify/${encodeURIComponent(id)}`)
      .then(async (res) => {
        if (!res.ok) {
          if (res.status === 404) {
             throw new Error('Agreement not found');
          }
          throw new Error('Verification service unavailable');
        }
        return res.json();
      })
      .then((data) => {
        setData(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Verification failed', err);
        setError(err.message);
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <ThemeProvider theme={lightTheme}>
        <CssBaseline />
        <Box sx={{ 
          minHeight: '100vh', 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center',
          bgcolor: 'background.default'
        }}>
          <CircularProgress color="primary" />
        </Box>
      </ThemeProvider>
    );
  }

  if (error || !data) {
    return (
      <ThemeProvider theme={lightTheme}>
        <CssBaseline />
        <Box sx={{ 
          minHeight: '100vh', 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center',
          bgcolor: 'background.default',
          p: 2
        }}>
          <Card sx={{ maxWidth: 480, width: '100%', p: 4, textAlign: 'center' }}>
            <Box sx={{ color: 'error.main', mb: 2 }}>
              <CancelIcon sx={{ fontSize: 64 }} />
            </Box>
            <Typography variant="h5" gutterBottom sx={{ fontWeight: 'bold' }}>
              Verification Failed
            </Typography>
            <Typography color="text.secondary">
              {error || 'Unable to verify agreement details. Please try again later.'}
            </Typography>
            <Button 
              variant="outlined" 
              sx={{ mt: 3 }}
              href="https://smartcbwtf.com"
            >
              Go to Home
            </Button>
          </Card>
        </Box>
      </ThemeProvider>
    );
  }

  const legacyData = data as AgreementVerificationDTO & LegacyAgreementVerificationDTO;
  const isActive = data.verified && data.status === 'ACTIVE';
  const statusColor = isActive ? 'success' : 'error';
  const statusBg = isActive ? 'success.light' : 'error.light';
  const statusText = isActive ? 'success.contrastText' : 'error.contrastText';
  const statusLabel = formatStatus(data.status);
  const validityStart = data.startDate ?? legacyData.validFrom;
  const validityEnd = data.endDate ?? legacyData.validUntil;

  return (
    <ThemeProvider theme={lightTheme}>
      <CssBaseline />
      <Box sx={{ 
        minHeight: '100vh', 
        bgcolor: 'background.default',
        py: 6,
        px: 2
      }}>
        <Container maxWidth="md">
          {/* Header Branding */}
          <Box sx={{ textAlign: 'center', mb: 6 }}>
            <img src="/logo.svg" alt="SmartCBWTF" style={{ height: 48, marginBottom: 16 }} />
            <Typography variant="h5" sx={{ fontWeight: 800, color: 'primary.main', letterSpacing: '-0.5px' }}>
              SmartCBWTF
            </Typography>
            <Typography variant="subtitle2" sx={{ letterSpacing: '2px', textTransform: 'uppercase', mt: 1, opacity: 0.7 }}>
              Official Verification Portal
            </Typography>
          </Box>

          <Card elevation={0} sx={{ 
            overflow: 'hidden', 
            border: '1px solid',
            borderColor: 'divider'
          }}>
            {/* Status Banner */}
            <Box sx={{ 
              bgcolor: statusBg, 
              color: statusText,
              p: 4, 
              textAlign: 'center',
              borderBottom: '1px solid',
              borderColor: isActive ? 'success.main' : 'error.main'
            }}>
              {isActive ? (
                <CheckCircleIcon sx={{ fontSize: 64, mb: 1, color: 'success.main' }} />
              ) : (
                <CancelIcon sx={{ fontSize: 64, mb: 1, color: 'error.main' }} />
              )}
              <Typography variant="h4" sx={{ mb: 1, color: isActive ? 'success.dark' : 'error.dark' }}>
                {isActive ? 'AGREEMENT VERIFIED' : 'AGREEMENT NOT ACTIVE'}
              </Typography>
              <Typography variant="subtitle1" sx={{ color: isActive ? 'success.dark' : 'error.dark', opacity: 0.9 }}>
                {isActive 
                  ? 'This record matches an active agreement in our system.' 
                  : data.message || `This agreement is not active. Status: ${statusLabel}`
                }
              </Typography>
            </Box>

            <Box sx={{ p: { xs: 3, md: 5 } }}>
              {/* Reference Number Section */}
              <Box sx={{ textAlign: 'center', mb: 5 }}>
                <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: '1px' }}>
                  Agreement Reference Number
                </Typography>
                <Typography variant="h5" sx={{ fontFamily: 'monospace', fontWeight: 600, mt: 0.5, letterSpacing: '1px' }}>
                  {displayValue(data.agreementNumber)}
                </Typography>
                <Chip 
                  label={statusLabel}
                  color={statusColor} 
                  sx={{ mt: 2, fontWeight: 'bold' }} 
                />
              </Box>

              <Grid container spacing={4}>
                {/* HCF Details Column */}
                <Grid item xs={12} md={6}>
                  <Box sx={{ p: 3, bgcolor: '#F1F5F9', borderRadius: 2, height: '100%' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <HospitalIcon color="primary" sx={{ mr: 1 }} />
                      <Typography variant="h6">Healthcare Facility Details</Typography>
                    </Box>
                    <Divider sx={{ mb: 2 }} />
                    
                    <Typography variant="subtitle2" color="text.secondary">Name</Typography>
                    <Typography variant="body1" fontWeight={600} gutterBottom>{displayValue(data.hcfName)}</Typography>

                    <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 2 }}>HCF Code</Typography>
                    <Typography variant="body1" fontWeight={500}>{displayValue(data.hcfCode)}</Typography>

                    <Typography variant="body2" color="text.secondary" sx={{ mt: 3 }}>
                      Detailed contact and address records are available only to authorized portal users.
                    </Typography>
                  </Box>
                </Grid>

                {/* Agreement Info Column */}
                <Grid item xs={12} md={6}>
                  <Box sx={{ p: 3, bgcolor: '#F1F5F9', borderRadius: 2, height: '100%' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <DescriptionIcon color="primary" sx={{ mr: 1 }} />
                      <Typography variant="h6">Agreement Information</Typography>
                    </Box>
                    <Divider sx={{ mb: 2 }} />

                    <Typography variant="subtitle2" color="text.secondary">Validity Period</Typography>
                    <Box sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        mt: 0.5, 
                        mb: 2, 
                        p: 1.5, 
                        bgcolor: isActive ? 'success.light' : 'error.light', 
                        borderRadius: 1,
                        color: isActive ? 'success.dark' : 'error.dark'
                    }}>
                      <CalendarIcon fontSize="small" sx={{ mr: 1 }} />
                      <Typography variant="body1" fontWeight={700}>
                        {formatDate(validityStart)} <span style={{ margin: '0 8px', opacity: 0.7 }}>to</span> {formatDate(validityEnd)}
                      </Typography>
                    </Box>

                    <Typography variant="subtitle2" color="text.secondary">Authorized CBWTF</Typography>
                    <Typography variant="body1" fontWeight={600} gutterBottom>{displayValue(data.facilityName)}</Typography>

                    {data.createdAt && (
                      <>
                        <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 2 }}>Record Created</Typography>
                        <Typography variant="body2">{formatDate(data.createdAt)}</Typography>
                      </>
                    )}
                  </Box>
                </Grid>
              </Grid>

              {/* Secure Footer */}
              <Box sx={{ mt: 6, pt: 3, borderTop: '1px dashed #E2E8F0', textAlign: 'center' }}>
                <Box sx={{ display: 'inline-flex', alignItems: 'center', color: 'text.secondary', opacity: 0.8 }}>
                  <VerifiedIcon sx={{ fontSize: 16, mr: 1, color: 'primary.main' }} />
                  <Typography variant="caption">
                    System Generated Verification &bull; No Signature Required &bull; Generated on {new Date().toLocaleDateString()}
                  </Typography>
                </Box>
              </Box>
            </Box>
          </Card>
        </Container>
      </Box>
    </ThemeProvider>
  );
};

export default AgreementVerify;
