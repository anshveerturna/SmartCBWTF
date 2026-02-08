import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Alert,
  Container,
} from '@mui/material';
import { useParams } from 'react-router-dom';
import VerifiedIcon from '@mui/icons-material/Verified';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import GppBadIcon from '@mui/icons-material/GppBad';
import SecurityIcon from '@mui/icons-material/Security';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.smartcbwtf.com';

interface VerificationResult {
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

const AgreementVerify: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [result, setResult] = useState<VerificationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setError('No agreement ID provided');
      setLoading(false);
      return;
    }

    fetch(`${API_BASE_URL}/api/public/agreement/verify/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error('Network error');
        return res.json();
      })
      .then((data: VerificationResult) => {
        setResult(data);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to verify agreement. Please try again.');
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          background: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #f0fdf4 100%)',
        }}
      >
        <Box sx={{ textAlign: 'center' }}>
          <CircularProgress size={48} sx={{ color: '#16a34a' }} />
          <Typography variant="body2" sx={{ mt: 2, color: '#15803d' }}>
            Verifying agreement...
          </Typography>
        </Box>
      </Box>
    );
  }

  const isActive = result?.status === 'ACTIVE';

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #f0fdf4 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        py: 4,
        px: 2,
      }}
    >
      {/* Header with SmartCBWTF branding */}
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
          <SecurityIcon sx={{ fontSize: 32, color: '#16a34a' }} />
          <Typography variant="h4" sx={{ fontWeight: 800, color: '#15803d', letterSpacing: '-0.5px' }}>
            SmartCBWTF
          </Typography>
        </Box>
        <Typography variant="body2" sx={{ color: '#4b5563', fontWeight: 500 }}>
          Bio-Medical Waste Agreement Verification
        </Typography>
        <Box sx={{ width: 60, height: 3, bgcolor: '#16a34a', borderRadius: 2, mx: 'auto', mt: 1 }} />
      </Box>

      <Container maxWidth="sm">
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : result && !result.verified ? (
          /* Not Found / Invalid */
          <Card elevation={3} sx={{ borderRadius: 3 }}>
            <CardContent sx={{ textAlign: 'center', py: 4 }}>
              <GppBadIcon sx={{ fontSize: 64, color: 'error.main', mb: 2 }} />
              <Typography variant="h6" fontWeight={600} color="error.main" gutterBottom>
                Verification Failed
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {result.message}
              </Typography>
            </CardContent>
          </Card>
        ) : result ? (
          /* Verified */
          <Card elevation={3} sx={{ borderRadius: 3, overflow: 'visible', border: '1px solid #dcfce7' }}>
            {/* Status Banner */}
            <Box
              sx={{
                bgcolor: isActive ? '#16a34a' : '#ca8a04',
                color: 'white',
                py: 2,
                px: 3,
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                borderTopLeftRadius: 12,
                borderTopRightRadius: 12,
              }}
            >
              {isActive ? (
                <VerifiedIcon sx={{ fontSize: 28 }} />
              ) : (
                <ErrorOutlineIcon sx={{ fontSize: 28 }} />
              )}
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>
                  {isActive ? 'Agreement Verified & Active' : 'Agreement Verified'}
                </Typography>
                <Typography variant="caption">{result.message}</Typography>
              </Box>
            </Box>

            <CardContent sx={{ px: 3, py: 3 }}>
              {/* Agreement Details */}
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                Agreement Details
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={1.5}>
                <DetailRow label="Agreement No." value={result.agreementNumber} />
                <DetailRow
                  label="Status"
                  value={
                    <Chip
                      label={result.status}
                      size="small"
                      color={isActive ? 'success' : 'warning'}
                      variant="outlined"
                    />
                  }
                />
                <DetailRow label="Version" value={result.version ? `V${result.version}` : null} />
                <DetailRow
                  label="Effective From"
                  value={result.startDate ? formatDate(result.startDate) : null}
                />
                <DetailRow
                  label="Valid Until"
                  value={
                    result.endDate ? formatDate(result.endDate) : 'Until Terminated'
                  }
                />
              </Stack>

              <Box sx={{ mt: 3 }} />

              {/* CBWTF (First Party) */}
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                CBWTF (First Party)
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={1.5}>
                <DetailRow label="Facility Name" value={result.facilityName} />
                <DetailRow label="Facility Code" value={result.facilityCode} />
              </Stack>

              <Box sx={{ mt: 3 }} />

              {/* HCF (Second Party) */}
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                HCF (Second Party)
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={1.5}>
                <DetailRow label="HCF Name" value={result.hcfName} />
                <DetailRow label="HCF Code" value={result.hcfCode} />
              </Stack>

              <Box sx={{ mt: 3 }} />

              {/* Footer */}
              <Typography variant="caption" color="text.disabled" sx={{ display: 'block', textAlign: 'center' }}>
                Agreement created on{' '}
                {result.createdAt ? formatDateTime(result.createdAt) : 'N/A'}
              </Typography>
            </CardContent>
          </Card>
        ) : null}

        {/* Branding Footer */}
        <Box sx={{ textAlign: 'center', mt: 4, mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5, mb: 0.5 }}>
            <SecurityIcon sx={{ fontSize: 14, color: '#16a34a' }} />
            <Typography variant="caption" sx={{ color: '#15803d', fontWeight: 600 }}>
              SmartCBWTF
            </Typography>
          </Box>
          <Typography variant="caption" color="text.disabled">
            Bio-Medical Waste Compliance Platform
          </Typography>
        </Box>
      </Container>
    </Box>
  );
};

/* --- Helper Components --- */

const DetailRow: React.FC<{ label: string; value: React.ReactNode }> = ({
  label,
  value,
}) => (
  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <Typography variant="body2" color="text.secondary" fontWeight={500}>
      {label}
    </Typography>
    <Typography variant="body2" fontWeight={600}>
      {value ?? '—'}
    </Typography>
  </Box>
);

function formatDate(dateStr: string): string {
  try {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

function formatDateTime(isoStr: string): string {
  try {
    const d = new Date(isoStr);
    return d.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return isoStr;
  }
}

export default AgreementVerify;
