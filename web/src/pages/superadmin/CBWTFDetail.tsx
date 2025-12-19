import { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Divider,
  Switch,
  FormControlLabel,
  Alert,
  Skeleton,
  List,
  ListItem,
  ListItemText,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Block as BlockIcon,
  PlayArrow as PlayArrowIcon,
  AccessTime as AccessTimeIcon,
} from '@mui/icons-material';
import { adminApi, FEATURE_FLAGS } from '../../api/admin';

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  ACTIVE: 'success',
  TRIAL: 'info',
  EXPIRED: 'error',
  SUSPENDED: 'warning',
  CANCELLED: 'default',
};

export default function CBWTFDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const isNewlyCreated = location.state?.newlyCreated;

  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [suspendReason, setSuspendReason] = useState('');
  const [tempAccessDialogOpen, setTempAccessDialogOpen] = useState(false);
  const [tempAccessDays, setTempAccessDays] = useState(7);

  const { data: cbwtf, isLoading, error } = useQuery({
    queryKey: ['cbwtf', id],
    queryFn: () => adminApi.getCBWTF(id!),
    enabled: !!id,
  });

  const { data: auditHistory } = useQuery({
    queryKey: ['cbwtf-audit', id],
    queryFn: () => adminApi.getAuditHistory(id!, { page: 0, size: 10 }),
    enabled: !!id,
  });

  const suspendMutation = useMutation({
    mutationFn: ({ reason }: { reason: string }) => adminApi.suspendCBWTF(id!, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
      setSuspendDialogOpen(false);
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: () => adminApi.reactivateCBWTF(id!, 365, 'Reactivated by admin'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
    },
  });

  const tempAccessMutation = useMutation({
    mutationFn: ({ days }: { days: number }) => 
      adminApi.grantTemporaryAccess(id!, days, 'Temporary access granted'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
      setTempAccessDialogOpen(false);
    },
  });

  const featureMutation = useMutation({
    mutationFn: ({ feature, enabled }: { feature: string; enabled: boolean }) =>
      adminApi.updateFeatures(id!, { [feature]: enabled }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cbwtf', id] });
    },
  });

  if (isLoading) {
    return (
      <Box>
        <Skeleton height={40} width={200} />
        <Skeleton height={300} />
      </Box>
    );
  }

  if (error || !cbwtf) {
    return (
      <Alert severity="error">
        CBWTF not found or failed to load.
        <Button onClick={() => navigate('/superadmin/cbwtfs')}>Back to CBWTFs</Button>
      </Alert>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/superadmin/cbwtfs')}
          sx={{ mb: 2 }}
        >
          Back to CBWTFs
        </Button>
        
        {isNewlyCreated && (
          <Alert severity="success" sx={{ mb: 2 }}>
            CBWTF created successfully! A temporary password has been sent to the admin.
          </Alert>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography variant="h4" fontWeight={700}>
              {cbwtf.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" fontFamily="monospace">
              {cbwtf.code}
            </Typography>
          </Box>
          <Chip
            label={cbwtf.subscriptionStatus}
            color={statusColors[cbwtf.subscriptionStatus]}
            sx={{ fontWeight: 600 }}
          />
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Subscription Card */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Subscription
              </Typography>
              <Stack direction="row" spacing={4} flexWrap="wrap">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Plan
                  </Typography>
                  <Typography fontWeight={600}>{cbwtf.subscriptionPlan}</Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Chip
                    label={cbwtf.subscriptionStatus}
                    color={statusColors[cbwtf.subscriptionStatus]}
                    size="small"
                  />
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Expires
                  </Typography>
                  <Typography>
                    {cbwtf.subscriptionExpiresAt
                      ? new Date(cbwtf.subscriptionExpiresAt).toLocaleDateString()
                      : '-'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Onboarded
                  </Typography>
                  <Typography>
                    {cbwtf.onboardedAt
                      ? new Date(cbwtf.onboardedAt).toLocaleDateString()
                      : '-'}
                  </Typography>
                </Box>
              </Stack>

              <Divider sx={{ my: 3 }} />

              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => navigate(`/superadmin/cbwtfs/${id}/edit`)}
                >
                  Edit Subscription
                </Button>
                {cbwtf.subscriptionStatus === 'ACTIVE' ? (
                  <Button
                    variant="outlined"
                    color="warning"
                    startIcon={<BlockIcon />}
                    onClick={() => setSuspendDialogOpen(true)}
                  >
                    Suspend
                  </Button>
                ) : (
                  <Button
                    variant="outlined"
                    color="success"
                    startIcon={<PlayArrowIcon />}
                    onClick={() => reactivateMutation.mutate()}
                    disabled={reactivateMutation.isPending}
                  >
                    Reactivate
                  </Button>
                )}
                {(cbwtf.subscriptionStatus === 'EXPIRED' || cbwtf.subscriptionStatus === 'SUSPENDED') && (
                  <Button
                    variant="outlined"
                    startIcon={<AccessTimeIcon />}
                    onClick={() => setTempAccessDialogOpen(true)}
                  >
                    Grant Temp Access
                  </Button>
                )}
              </Box>
            </CardContent>
          </Card>

          {/* Feature Flags */}
          <Card sx={{ borderRadius: 2, mt: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Feature Flags
              </Typography>
              <Grid container spacing={2}>
                {Object.entries(FEATURE_FLAGS).map(([key, label]) => (
                  <Grid size={{ xs: 12, sm: 6 }} key={key}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={cbwtf.features[key] ?? false}
                          onChange={(e) =>
                            featureMutation.mutate({ feature: key, enabled: e.target.checked })
                          }
                          disabled={featureMutation.isPending}
                        />
                      }
                      label={label.replace(/_/g, ' ')}
                    />
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Right Column */}
        <Grid size={{ xs: 12, md: 4 }}>
          {/* Stats */}
          <Card sx={{ borderRadius: 2, mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Statistics
              </Typography>
              <Stack direction="row" spacing={4}>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {cbwtf.hcfCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    HCFs
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {cbwtf.activeUserCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Users
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          {/* Contact Info */}
          <Card sx={{ borderRadius: 2, mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Contact
              </Typography>
              <Stack spacing={1}>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Email
                  </Typography>
                  <Typography>{cbwtf.contactEmail || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Phone
                  </Typography>
                  <Typography>{cbwtf.contactPhone || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Address
                  </Typography>
                  <Typography>{cbwtf.address}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          {/* Audit History */}
          <Card sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Activity
              </Typography>
              <List dense>
                {auditHistory?.content.slice(0, 5).map((audit) => (
                  <ListItem key={audit.id} disablePadding>
                    <ListItemText
                      primary={audit.action.replace(/_/g, ' ')}
                      secondary={new Date(audit.performedAt).toLocaleString()}
                      primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                      secondaryTypographyProps={{ variant: 'caption' }}
                    />
                  </ListItem>
                ))}
                {(!auditHistory || auditHistory.content.length === 0) && (
                  <Typography variant="body2" color="text.secondary">
                    No activity yet
                  </Typography>
                )}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Suspend Dialog */}
      <Dialog open={suspendDialogOpen} onClose={() => setSuspendDialogOpen(false)}>
        <DialogTitle>Suspend CBWTF</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            This will prevent all users from accessing this CBWTF's data.
          </Typography>
          <TextField
            label="Reason"
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            fullWidth
            multiline
            rows={2}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSuspendDialogOpen(false)}>Cancel</Button>
          <Button
            color="warning"
            variant="contained"
            onClick={() => suspendMutation.mutate({ reason: suspendReason })}
            disabled={suspendMutation.isPending}
          >
            Suspend
          </Button>
        </DialogActions>
      </Dialog>

      {/* Temp Access Dialog */}
      <Dialog open={tempAccessDialogOpen} onClose={() => setTempAccessDialogOpen(false)}>
        <DialogTitle>Grant Temporary Access</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Grant time-limited access to an expired or suspended CBWTF.
          </Typography>
          <TextField
            label="Days"
            type="number"
            value={tempAccessDays}
            onChange={(e) => setTempAccessDays(parseInt(e.target.value) || 7)}
            fullWidth
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTempAccessDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => tempAccessMutation.mutate({ days: tempAccessDays })}
            disabled={tempAccessMutation.isPending}
          >
            Grant Access
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
