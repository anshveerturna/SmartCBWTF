import { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Tabs,
  Tab,
  TextField,
  Switch,
  Button,
  Alert,
  Skeleton,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  alpha,
  Snackbar,
} from '@mui/material';
import {
  Save as SaveIcon,
  History as HistoryIcon,
  Warning as WarningIcon,
  Refresh as RefreshIcon,
  Security as SecurityIcon,
  Settings as SettingsIcon,
  CreditCard as BillingIcon,
  ToggleOn as FeatureIcon,
  Speed as OperationalIcon,
  Gavel as ComplianceIcon,
  Shield as SafetyIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import type { SystemConfigDTO, ConfigAuditDTO } from '../../api/admin';

// Category configuration with icons and labels
const CATEGORIES = [
  { key: 'PLATFORM_GLOBAL', label: 'Platform', icon: <SettingsIcon /> },
  { key: 'SECURITY', label: 'Security', icon: <SecurityIcon /> },
  { key: 'SUBSCRIPTION', label: 'Billing', icon: <BillingIcon /> },
  { key: 'FEATURE_DEFAULTS', label: 'Features', icon: <FeatureIcon /> },
  { key: 'OPERATIONAL', label: 'Operational', icon: <OperationalIcon /> },
  { key: 'COMPLIANCE', label: 'Compliance', icon: <ComplianceIcon /> },
  { key: 'SAFETY_CONTROLS', label: 'Safety', icon: <SafetyIcon /> },
];

export default function SystemConfig() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);
  const [editedValues, setEditedValues] = useState<Record<string, string>>({});
  const [confirmDialog, setConfirmDialog] = useState<{ open: boolean; config: SystemConfigDTO | null; confirmText: string }>({
    open: false,
    config: null,
    confirmText: '',
  });
  const [auditDialog, setAuditDialog] = useState<{ open: boolean; key: string; audits: ConfigAuditDTO[] }>({
    open: false,
    key: '',
    audits: [],
  });
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Fetch all configs
  const { data: configsByCategory, isLoading, error } = useQuery({
    queryKey: ['system-configs'],
    queryFn: adminApi.getAllSystemConfigs,
  });

  // Update config mutation
  const updateMutation = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => 
      adminApi.updateSystemConfig(key, value),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['system-configs'] });
      setEditedValues(prev => {
        const next = { ...prev };
        delete next[variables.key];
        return next;
      });
      setSnackbar({ open: true, message: 'Configuration saved successfully', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to save configuration', severity: 'error' });
    },
  });

  // Refresh cache mutation
  const refreshMutation = useMutation({
    mutationFn: adminApi.refreshConfigCache,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['system-configs'] });
      setSnackbar({ open: true, message: 'Cache refreshed successfully', severity: 'success' });
    },
  });

  const currentCategory = CATEGORIES[activeTab];
  const configs = configsByCategory?.[currentCategory.key] ?? [];

  const handleValueChange = (key: string, value: string) => {
    setEditedValues(prev => ({ ...prev, [key]: value }));
  };

  const hasChanges = (key: string, originalValue: string) => {
    return editedValues[key] !== undefined && editedValues[key] !== originalValue;
  };

  const handleSave = (config: SystemConfigDTO) => {
    const newValue = editedValues[config.key] ?? config.value;
    
    if (config.requiresConfirmation) {
      setConfirmDialog({ open: true, config, confirmText: '' });
    } else {
      updateMutation.mutate({ key: config.key, value: newValue });
    }
  };

  const handleConfirmSave = () => {
    if (confirmDialog.confirmText !== 'CONFIRM' || !confirmDialog.config) {
      return;
    }
    const newValue = editedValues[confirmDialog.config.key] ?? confirmDialog.config.value;
    updateMutation.mutate({ key: confirmDialog.config.key, value: newValue });
    setConfirmDialog({ open: false, config: null, confirmText: '' });
  };

  const handleViewAudit = async (key: string) => {
    try {
      const audits = await adminApi.getConfigAuditHistory(key);
      setAuditDialog({ open: true, key, audits });
    } catch {
      setSnackbar({ open: true, message: 'Failed to load audit history', severity: 'error' });
    }
  };

  const renderConfigInput = (config: SystemConfigDTO) => {
    const currentValue = editedValues[config.key] ?? config.value;
    const isChanged = hasChanges(config.key, config.value);

    if (config.valueType === 'BOOLEAN') {
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Switch
            checked={currentValue === 'true'}
            onChange={(e) => handleValueChange(config.key, e.target.checked ? 'true' : 'false')}
            disabled={config.isReadonly}
            color={config.requiresConfirmation ? 'error' : 'primary'}
          />
          <Typography variant="body2" color={currentValue === 'true' ? 'success.main' : 'text.secondary'}>
            {currentValue === 'true' ? 'Enabled' : 'Disabled'}
          </Typography>
          {isChanged && (
            <Button
              size="small"
              variant="contained"
              startIcon={<SaveIcon />}
              onClick={() => handleSave(config)}
              disabled={updateMutation.isPending}
            >
              Save
            </Button>
          )}
        </Box>
      );
    }

    if (config.valueType === 'NUMBER') {
      const rules = config.validationRules;
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <TextField
            type="number"
            value={currentValue}
            onChange={(e) => handleValueChange(config.key, e.target.value)}
            disabled={config.isReadonly}
            size="small"
            sx={{ width: 150 }}
            inputProps={{
              min: rules?.min,
              max: rules?.max,
            }}
            helperText={rules ? `Range: ${rules.min ?? 0} - ${rules.max ?? '∞'}` : undefined}
          />
          {isChanged && (
            <Button
              size="small"
              variant="contained"
              startIcon={<SaveIcon />}
              onClick={() => handleSave(config)}
              disabled={updateMutation.isPending}
            >
              Save
            </Button>
          )}
        </Box>
      );
    }

    // STRING or JSON
    return (
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
        <TextField
          value={currentValue}
          onChange={(e) => handleValueChange(config.key, e.target.value)}
          disabled={config.isReadonly || config.isSensitive}
          size="small"
          fullWidth
          multiline={config.valueType === 'JSON'}
          rows={config.valueType === 'JSON' ? 2 : 1}
          sx={{ maxWidth: 400 }}
        />
        {isChanged && (
          <Button
            size="small"
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={() => handleSave(config)}
            disabled={updateMutation.isPending}
          >
            Save
          </Button>
        )}
      </Box>
    );
  };

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Failed to load system configuration. Please try again.
      </Alert>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            System Configuration
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Platform-wide settings and policies. Changes are audited and versioned.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => refreshMutation.mutate()}
          disabled={refreshMutation.isPending}
        >
          Refresh Cache
        </Button>
      </Box>

      {/* Category Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs 
          value={activeTab} 
          onChange={(_, v) => setActiveTab(v)}
          variant="scrollable"
          scrollButtons="auto"
        >
          {CATEGORIES.map((cat) => (
            <Tab
              key={cat.key}
              icon={cat.icon}
              label={cat.label}
              iconPosition="start"
              sx={{ 
                minHeight: 48,
                ...(cat.key === 'SAFETY_CONTROLS' && { color: 'error.main' })
              }}
            />
          ))}
        </Tabs>
      </Box>

      {/* Safety Warning for SAFETY_CONTROLS */}
      {currentCategory.key === 'SAFETY_CONTROLS' && (
        <Alert severity="error" sx={{ mb: 3 }} icon={<WarningIcon />}>
          <strong>Critical Safety Controls</strong> - These settings can disable core functionality.
          All changes require typing "CONFIRM" and will be double-audited.
        </Alert>
      )}

      {/* Configuration List */}
      <Card>
        <CardContent>
          {isLoading ? (
            <Box>
              {[1, 2, 3, 4, 5].map((i) => (
                <Box key={i} sx={{ mb: 3 }}>
                  <Skeleton width={200} height={24} />
                  <Skeleton width={400} height={40} />
                </Box>
              ))}
            </Box>
          ) : configs.length === 0 ? (
            <Typography color="text.secondary">No configurations in this category.</Typography>
          ) : (
            <Box>
              {configs.map((config) => (
                <Box
                  key={config.key}
                  sx={{
                    py: 2,
                    px: 2,
                    borderRadius: 1,
                    mb: 1,
                    bgcolor: config.requiresConfirmation ? alpha('#f44336', 0.05) : 'transparent',
                    '&:hover': { bgcolor: config.requiresConfirmation ? alpha('#f44336', 0.08) : 'action.hover' },
                  }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Box sx={{ flex: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          {config.displayName}
                        </Typography>
                        {config.requiresConfirmation && (
                          <Chip label="Requires Confirmation" size="small" color="error" variant="outlined" />
                        )}
                        {config.isReadonly && (
                          <Chip label="Read-only" size="small" variant="outlined" />
                        )}
                        {config.isSensitive && (
                          <Chip label="Sensitive" size="small" color="warning" variant="outlined" />
                        )}
                      </Box>
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                        {config.description}
                      </Typography>
                      <Typography variant="caption" color="text.disabled">
                        Key: {config.key} | Version: {config.version}
                        {config.updatedBy && ` | Last updated by ${config.updatedBy}`}
                      </Typography>
                    </Box>
                    <Tooltip title="View Audit History">
                      <IconButton size="small" onClick={() => handleViewAudit(config.key)}>
                        <HistoryIcon />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  {renderConfigInput(config)}
                </Box>
              ))}
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Confirmation Dialog */}
      <Dialog open={confirmDialog.open} onClose={() => setConfirmDialog({ open: false, config: null, confirmText: '' })}>
        <DialogTitle sx={{ color: 'error.main' }}>
          ⚠️ Confirm Critical Change
        </DialogTitle>
        <DialogContent>
          <Typography sx={{ mb: 2 }}>
            You are about to change: <strong>{confirmDialog.config?.displayName}</strong>
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            This is a critical safety setting. Please type <strong>CONFIRM</strong> to proceed.
          </Typography>
          <TextField
            fullWidth
            value={confirmDialog.confirmText}
            onChange={(e) => setConfirmDialog(prev => ({ ...prev, confirmText: e.target.value }))}
            placeholder="Type CONFIRM"
            autoFocus
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog({ open: false, config: null, confirmText: '' })}>
            Cancel
          </Button>
          <Button 
            variant="contained" 
            color="error"
            onClick={handleConfirmSave}
            disabled={confirmDialog.confirmText !== 'CONFIRM'}
          >
            Confirm Change
          </Button>
        </DialogActions>
      </Dialog>

      {/* Audit History Dialog */}
      <Dialog 
        open={auditDialog.open} 
        onClose={() => setAuditDialog({ open: false, key: '', audits: [] })}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Audit History: {auditDialog.key}
        </DialogTitle>
        <DialogContent>
          {auditDialog.audits.length === 0 ? (
            <Typography color="text.secondary">No audit history found.</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Time</TableCell>
                  <TableCell>Changed By</TableCell>
                  <TableCell>Old Value</TableCell>
                  <TableCell>New Value</TableCell>
                  <TableCell>Reason</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {auditDialog.audits.map((audit) => (
                  <TableRow key={audit.id}>
                    <TableCell>{new Date(audit.changedAt).toLocaleString()}</TableCell>
                    <TableCell>{audit.changedBy ?? 'System'}</TableCell>
                    <TableCell sx={{ maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {audit.oldValue ?? '-'}
                    </TableCell>
                    <TableCell sx={{ maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {audit.newValue}
                    </TableCell>
                    <TableCell>{audit.reason ?? '-'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAuditDialog({ open: false, key: '', audits: [] })}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
