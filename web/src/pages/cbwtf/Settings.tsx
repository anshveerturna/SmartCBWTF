import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Tabs,
  Tab,
  TextField,
  Button,
  Alert,
  Skeleton,
  Chip,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TablePagination,
  CircularProgress,
} from '@mui/material';
import {
  Save as SaveIcon,
  Warning as WarningIcon,
  Lock as LockIcon,
  Business as LegalIcon,
  Description as AgreementIcon,
  Timeline as AuditIcon,
  Palette as BrandingIcon,
  AccountBalance as FinancialIcon,
} from '@mui/icons-material';
import BrandingSection from './settings/BrandingSection';
import AgreementRulesSection from './settings/AgreementRulesSection';
import FinancialSettingsSection from './settings/FinancialSettingsSection';
import dayjs from 'dayjs';
import {
  getFacilitySettings,
  updateLegalProfile,
  getSettingsAuditHistory,
} from '../../api/cbwtf';
import type {
  LegalProfileDTO,
  LockedFieldsDTO,
  SettingsAuditDTO,
} from '../../api/cbwtf';

// Tab configuration
const TABS = [
  { key: 'legal', label: 'Legal & Profile', icon: <LegalIcon /> },
  { key: 'agreement', label: 'Agreements', icon: <AgreementIcon /> },
  { key: 'branding', label: 'Branding', icon: <BrandingIcon /> },
  { key: 'financial', label: 'Financial & Billing', icon: <FinancialIcon /> },
  { key: 'audit', label: 'Audit History', icon: <AuditIcon /> },
];

// Enterprise-style setting row component
interface SettingRowProps {
  label: string;
  description?: string;
  locked?: boolean;
  lockedReason?: string;
  children: React.ReactNode;
  noBorder?: boolean;
}

const SettingRow = ({ label, description, locked, lockedReason, children, noBorder }: SettingRowProps) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      py: 2.5,
      px: 0,
      borderBottom: noBorder ? 'none' : '1px solid',
      borderColor: 'divider',
      gap: 4,
      '&:hover': {
        bgcolor: 'action.hover',
        mx: -3,
        px: 3,
        borderRadius: 1,
      },
      transition: 'all 0.15s ease',
    }}
  >
    <Box sx={{ flex: '1 1 45%', minWidth: 0 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
        <Typography variant="body1" sx={{ fontWeight: 500, color: 'text.primary' }}>
          {label}
        </Typography>
        {locked && (
          <Tooltip title={lockedReason || 'This field is locked'} arrow>
            <Chip
              icon={<LockIcon sx={{ fontSize: 12 }} />}
              label="Locked"
              size="small"
              color="warning"
              variant="outlined"
              sx={{ height: 20, fontSize: 10, '& .MuiChip-icon': { ml: 0.5 } }}
            />
          </Tooltip>
        )}
      </Box>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.5 }}>
          {description}
        </Typography>
      )}
    </Box>
    <Box sx={{ flex: '0 0 320px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
      {children}
    </Box>
  </Box>
);

// Section header
const SectionHeader = ({ title, description }: { title: string; description?: string }) => (
  <Box sx={{ mb: 1, pb: 2, borderBottom: '2px solid', borderColor: 'primary.main' }}>
    <Typography variant="h6" sx={{ fontWeight: 600, mb: 0.5 }}>
      {title}
    </Typography>
    {description && (
      <Typography variant="body2" color="text.secondary">
        {description}
      </Typography>
    )}
  </Box>
);

export default function Settings() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });
  const [confirmDialog, setConfirmDialog] = useState<{ open: boolean; section: string; onConfirm: () => void }>({
    open: false,
    section: '',
    onConfirm: () => {},
  });

  // Form states
  const [legalForm, setLegalForm] = useState<LegalProfileDTO | null>(null);

  // Audit state
  const [auditPage, setAuditPage] = useState(0);
  const [auditRowsPerPage, setAuditRowsPerPage] = useState(20);

  // Queries
  const { data: settings, isLoading, error } = useQuery({
    queryKey: ['facility-settings'],
    queryFn: getFacilitySettings,
  });

  const { data: auditData, isLoading: auditLoading } = useQuery({
    queryKey: ['settings-audit', auditPage, auditRowsPerPage],
    queryFn: () => getSettingsAuditHistory(undefined, auditPage, auditRowsPerPage),
    enabled: activeTab === 4,
  });

  const legalMutation = useMutation({
    mutationFn: updateLegalProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['facility-settings'] });
      setSnackbar({ open: true, message: 'Legal profile saved successfully', severity: 'success' });
    },
    onError: (err: Error) => {
      setSnackbar({ open: true, message: err.message || 'Failed to save Legal profile', severity: 'error' });
    },
  });

  const lockedFields = settings?.lockedFields || {} as LockedFieldsDTO;
  const effectiveLegalForm = legalForm ?? settings?.legal ?? {};
  const updateLegalField = (field: keyof LegalProfileDTO, value: string) => {
    setLegalForm({ ...effectiveLegalForm, [field]: value });
  };

  // Render content
  const renderTabContent = () => {
    if (isLoading) {
      return (
        <Box>
          {[1, 2, 3, 4, 5].map((i) => (
            <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', py: 2.5, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Box sx={{ flex: 1 }}>
                <Skeleton width={200} height={24} />
                <Skeleton width={300} height={20} sx={{ mt: 0.5 }} />
              </Box>
              <Skeleton width={280} height={40} />
            </Box>
          ))}
        </Box>
      );
    }

    switch (activeTab) {
      case 0: // Legal & Profile
        return (
          <Box>
            <SectionHeader title="Legal & Entity Profile" description="Official registration and identity information for invoices and compliance reports." />
            
            <SettingRow label="Legal Name" description="Official registered name that appears on invoices and legal documents.">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.legalName || ''}
                onChange={(e) => updateLegalField('legalName', e.target.value)}
                placeholder="Enter registered legal name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Trade Name" description="Display name shown on dashboards and internal views.">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.tradeName || ''}
                onChange={(e) => updateLegalField('tradeName', e.target.value)}
                placeholder="Enter trade name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="GSTIN" locked={lockedFields.gstLocked} lockedReason="Locked after first invoice generation">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.gstin || ''}
                onChange={(e) => updateLegalField('gstin', e.target.value)}
                disabled={lockedFields.gstLocked}
                placeholder="22AAAAA0000A1Z5"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="PAN" description="Permanent Account Number for tax purposes.">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.pan || ''}
                onChange={(e) => updateLegalField('pan', e.target.value)}
                placeholder="AAAAA0000A"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Authorization Number" description="SPCB Authorization Number." locked={lockedFields.complianceLocked} lockedReason="Locked after first compliance report">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.authorizationNumber || ''}
                onChange={(e) => updateLegalField('authorizationNumber', e.target.value)}
                disabled={lockedFields.complianceLocked}
                placeholder="SPCB Authorization No."
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="SPCB Name" description="State Pollution Control Board name.">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.spcbName || ''}
                onChange={(e) => updateLegalField('spcbName', e.target.value)}
                placeholder="Enter SPCB name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="SPCB State" description="State of the Pollution Control Board.">
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.spcbState || ''}
                onChange={(e) => updateLegalField('spcbState', e.target.value)}
                placeholder="Enter state"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Registered Address" description="Full registered office address.">
              <TextField
                fullWidth
                size="small"
                multiline
                rows={2}
                value={effectiveLegalForm.registeredAddress || ''}
                onChange={(e) => updateLegalField('registeredAddress', e.target.value)}
                placeholder="Enter full address"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Official Email" description="Primary contact email for the CBWTF, displayed on agreements and official documents.">
              <TextField
                fullWidth
                size="small"
                type="email"
                value={effectiveLegalForm.officialEmail || ''}
                onChange={(e) => updateLegalField('officialEmail', e.target.value)}
                placeholder="info@example.com"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Official Phone" description="Primary contact phone number." noBorder>
              <TextField
                fullWidth
                size="small"
                value={effectiveLegalForm.officialPhone || ''}
                onChange={(e) => updateLegalField('officialPhone', e.target.value)}
                placeholder="+91 XXXXX XXXXX"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={legalMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => legalMutation.mutate(effectiveLegalForm)}
                disabled={legalMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 1: // Agreements
        return settings ? (
          <AgreementRulesSection
            data={settings.agreementRules}
            onSave={() => queryClient.invalidateQueries({ queryKey: ['facility-settings'] })}
          />
        ) : null;

      case 2: // Branding
        return <BrandingSection onSettingsChange={() => queryClient.invalidateQueries({ queryKey: ['settings-audit'] })} />;

      case 3: // Financial & Billing
        return settings ? (
          <FinancialSettingsSection
            data={settings.financial}
            lockedFields={lockedFields}
            onSave={() => queryClient.invalidateQueries({ queryKey: ['facility-settings'] })}
          />
        ) : null;

      case 4: // Audit History
        return (
          <Box>
            <SectionHeader title="Configuration History" description="Complete audit log of all settings changes with timestamps and user information." />

            <TableContainer component={Paper} variant="outlined" sx={{ mt: 2 }}>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ bgcolor: 'action.hover' }}>
                    <TableCell sx={{ fontWeight: 600 }}>Timestamp</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Section</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Setting</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Old Value</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>New Value</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Changed By</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>IP Address</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {auditLoading ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                        <CircularProgress size={24} />
                      </TableCell>
                    </TableRow>
                  ) : (auditData?.content || []).length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                        <Typography color="text.secondary">No changes recorded yet</Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    (auditData?.content || []).map((log: SettingsAuditDTO) => (
                      <TableRow key={log.id} hover>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>{dayjs(log.changedAt).format('DD MMM YYYY, HH:mm')}</TableCell>
                        <TableCell><Chip label={log.section} size="small" variant="outlined" /></TableCell>
                        <TableCell>{log.settingKey}</TableCell>
                        <TableCell sx={{ color: 'error.main', maxWidth: 120 }}>
                          <Tooltip title={log.oldValue || '-'}><Typography variant="body2" noWrap>{log.oldValue || '-'}</Typography></Tooltip>
                        </TableCell>
                        <TableCell sx={{ color: 'success.main', maxWidth: 120 }}>
                          <Tooltip title={log.newValue || '-'}><Typography variant="body2" noWrap>{log.newValue || '-'}</Typography></Tooltip>
                        </TableCell>
                        <TableCell>{log.changedByUsername}</TableCell>
                        <TableCell>{log.ipAddress || '-'}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              component="div"
              count={auditData?.totalElements || 0}
              page={auditPage}
              onPageChange={(_, newPage) => setAuditPage(newPage)}
              rowsPerPage={auditRowsPerPage}
              onRowsPerPageChange={(e) => { setAuditRowsPerPage(parseInt(e.target.value, 10)); setAuditPage(0); }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </Box>
        );

      default:
        return null;
    }
  };

  if (error) {
    return <Alert severity="error" sx={{ m: 2 }}>Failed to load settings. Please try again.</Alert>;
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Facility Settings</Typography>
          <Typography variant="body1" color="text.secondary">Configure system behavior, rules, and preferences</Typography>
        </Box>
        <Chip label={`Version ${settings?.settingsVersion || 1}`} color="primary" variant="outlined" sx={{ fontWeight: 600 }} />
      </Box>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto">
          {TABS.map((tab) => (
            <Tab key={tab.key} icon={tab.icon} label={tab.label} iconPosition="start" sx={{ minHeight: 48, textTransform: 'none', fontWeight: 500 }} />
          ))}
        </Tabs>
      </Box>

      {/* Content */}
      <Card variant="outlined">
        <CardContent sx={{ p: 4 }}>
          {renderTabContent()}
        </CardContent>
      </Card>

      {/* Confirmation Dialog */}
      <Dialog open={confirmDialog.open} onClose={() => setConfirmDialog({ open: false, section: '', onConfirm: () => {} })}>
        <DialogTitle sx={{ color: 'warning.main', display: 'flex', alignItems: 'center', gap: 1 }}>
          <WarningIcon /> Confirm Change
        </DialogTitle>
        <DialogContent>
          <Typography>You are modifying <strong>{confirmDialog.section}</strong>. This setting may be locked after first use. Are you sure?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog({ open: false, section: '', onConfirm: () => {} })}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={() => { confirmDialog.onConfirm(); setConfirmDialog({ open: false, section: '', onConfirm: () => {} }); }}>Confirm</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert severity={snackbar.severity} onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
