import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TablePagination,
  CircularProgress,
  InputAdornment,
} from '@mui/material';
import {
  Save as SaveIcon,
  Warning as WarningIcon,
  Lock as LockIcon,
  Business as LegalIcon,
  AccountBalance as FinancialIcon,
  Payment as PaymentIcon,
  Description as AgreementIcon,
  QrCode as OperationalIcon,
  VerifiedUser as ComplianceIcon,
  Email as EmailIcon,
  Timeline as AuditIcon,
  Percent as PercentIcon,
  Palette as BrandingIcon,
  MailOutline as TemplateIcon,
} from '@mui/icons-material';
import BrandingSection from './settings/BrandingSection';
import EmailTemplateSection from './settings/EmailTemplateSection';
import dayjs from 'dayjs';
import {
  getFacilitySettings,
  updateLegalProfile,
  updateFinancialSettings,
  updatePaymentReminders,
  updateAgreementRules,
  updateOperationalRules,
  updateComplianceSettings,
  updateEmailSettings,
  getSettingsAuditHistory,
} from '../../api/cbwtf';
import type {
  FacilitySettingsDTO,
  LegalProfileDTO,
  FinancialSettingsDTO,
  PaymentReminderDTO,
  AgreementRulesDTO,
  OperationalRulesDTO,
  ComplianceSettingsDTO,
  EmailSettingsDTO,
  LockedFieldsDTO,
  SettingsAuditDTO,
} from '../../api/cbwtf';

// Tab configuration
const TABS = [
  { key: 'legal', label: 'Legal & Profile', icon: <LegalIcon /> },
  { key: 'financial', label: 'Financial', icon: <FinancialIcon /> },
  { key: 'payment', label: 'Payment Rules', icon: <PaymentIcon /> },
  { key: 'agreement', label: 'Agreements', icon: <AgreementIcon /> },
  { key: 'operational', label: 'Operational', icon: <OperationalIcon /> },
  { key: 'compliance', label: 'Compliance', icon: <ComplianceIcon /> },
  { key: 'email', label: 'Notifications', icon: <EmailIcon /> },
  { key: 'branding', label: 'Branding', icon: <BrandingIcon /> },
  { key: 'emailTemplates', label: 'Email Templates', icon: <TemplateIcon /> },
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
  const [legalForm, setLegalForm] = useState<LegalProfileDTO>({});
  const [financialForm, setFinancialForm] = useState<FinancialSettingsDTO>({
    cgstPercent: 9, sgstPercent: 9, igstPercent: 18, gstEnabled: true,
  });
  const [paymentForm, setPaymentForm] = useState<PaymentReminderDTO>({
    gracePeriodDays: 7, autoAlertEscalation: true,
  });
  const [agreementForm, setAgreementForm] = useState<AgreementRulesDTO>({
    defaultAgreementValidityMonths: 12, agreementRenewalWindowDays: 30, blockOverlappingAgreements: true,
  });
  const [operationalForm, setOperationalForm] = useState<OperationalRulesDTO>({
    qrValidityDays: 30, allowMultipleActiveQrs: false, requireCbwtfVerification: true,
    gpsGeofenceRadiusM: 100, maxUnverifiedBags: 10, blueWasteMinPercent: 5,
  });
  const [complianceForm, setComplianceForm] = useState<ComplianceSettingsDTO>({
    dailyReportTime: '08:00', monthlyReportDay: 1, enforceChecksum: true,
  });
  const [emailForm, setEmailForm] = useState<EmailSettingsDTO>({
    resolvedSenderName: '', resolvedSenderEmail: '', senderSlugLocked: false,
    useGenericSender: false, notificationEmail: null, ccAdminOnHcfEmails: true,
    emailNotificationsEnabled: true, inAppAlertsEnabled: true,
  });

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
    enabled: activeTab === 9,
  });

  useEffect(() => {
    if (settings) {
      setLegalForm(settings.legal);
      setFinancialForm(settings.financial);
      setPaymentForm(settings.paymentReminders);
      setAgreementForm(settings.agreementRules);
      setOperationalForm(settings.operational);
      setComplianceForm(settings.compliance);
      setEmailForm(settings.email);
    }
  }, [settings]);

  // Mutations
  const createMutation = (mutationFn: (data: unknown) => Promise<void>, sectionName: string) =>
    useMutation({
      mutationFn,
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['facility-settings'] });
        setSnackbar({ open: true, message: `${sectionName} saved successfully`, severity: 'success' });
      },
      onError: (err: Error) => {
        setSnackbar({ open: true, message: err.message || `Failed to save ${sectionName}`, severity: 'error' });
      },
    });

  const legalMutation = createMutation(updateLegalProfile, 'Legal profile');
  const financialMutation = createMutation(updateFinancialSettings, 'Financial settings');
  const paymentMutation = createMutation(updatePaymentReminders, 'Payment reminders');
  const agreementMutation = createMutation(updateAgreementRules, 'Agreement rules');
  const operationalMutation = createMutation(updateOperationalRules, 'Operational settings');
  const complianceMutation = createMutation(updateComplianceSettings, 'Compliance settings');
  const emailMutation = createMutation(updateEmailSettings, 'Email settings');

  const lockedFields = settings?.lockedFields || {} as LockedFieldsDTO;

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
                value={legalForm.legalName || ''}
                onChange={(e) => setLegalForm({ ...legalForm, legalName: e.target.value })}
                placeholder="Enter registered legal name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Trade Name" description="Display name shown on dashboards and internal views.">
              <TextField
                fullWidth
                size="small"
                value={legalForm.tradeName || ''}
                onChange={(e) => setLegalForm({ ...legalForm, tradeName: e.target.value })}
                placeholder="Enter trade name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="GSTIN" locked={lockedFields.gstLocked} lockedReason="Locked after first invoice generation">
              <TextField
                fullWidth
                size="small"
                value={legalForm.gstin || ''}
                onChange={(e) => setLegalForm({ ...legalForm, gstin: e.target.value })}
                disabled={lockedFields.gstLocked}
                placeholder="22AAAAA0000A1Z5"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="PAN" description="Permanent Account Number for tax purposes.">
              <TextField
                fullWidth
                size="small"
                value={legalForm.pan || ''}
                onChange={(e) => setLegalForm({ ...legalForm, pan: e.target.value })}
                placeholder="AAAAA0000A"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Authorization Number" description="SPCB Authorization Number." locked={lockedFields.complianceLocked} lockedReason="Locked after first compliance report">
              <TextField
                fullWidth
                size="small"
                value={legalForm.authorizationNumber || ''}
                onChange={(e) => setLegalForm({ ...legalForm, authorizationNumber: e.target.value })}
                disabled={lockedFields.complianceLocked}
                placeholder="SPCB Authorization No."
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="SPCB Name" description="State Pollution Control Board name.">
              <TextField
                fullWidth
                size="small"
                value={legalForm.spcbName || ''}
                onChange={(e) => setLegalForm({ ...legalForm, spcbName: e.target.value })}
                placeholder="Enter SPCB name"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="SPCB State" description="State of the Pollution Control Board.">
              <TextField
                fullWidth
                size="small"
                value={legalForm.spcbState || ''}
                onChange={(e) => setLegalForm({ ...legalForm, spcbState: e.target.value })}
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
                value={legalForm.registeredAddress || ''}
                onChange={(e) => setLegalForm({ ...legalForm, registeredAddress: e.target.value })}
                placeholder="Enter full address"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <SettingRow label="Official Phone" description="Primary contact phone number." noBorder>
              <TextField
                fullWidth
                size="small"
                value={legalForm.officialPhone || ''}
                onChange={(e) => setLegalForm({ ...legalForm, officialPhone: e.target.value })}
                placeholder="+91 XXXXX XXXXX"
                sx={{ maxWidth: 320 }}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={legalMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => legalMutation.mutate(legalForm)}
                disabled={legalMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 1: // Financial
        return (
          <Box>
            <SectionHeader title="Financial & Billing" description="GST rates and tax configuration for invoice generation." />

            <SettingRow label="GST Enabled" description="Enable or disable GST calculation on invoices." locked={lockedFields.gstLocked} lockedReason="Locked after first invoice generation">
              <Switch
                checked={financialForm.gstEnabled}
                onChange={(e) => setFinancialForm({ ...financialForm, gstEnabled: e.target.checked })}
                disabled={lockedFields.gstLocked}
              />
            </SettingRow>

            {financialForm.gstEnabled && (
              <>
                <SettingRow label="CGST Rate" description="Central GST rate percentage." locked={lockedFields.gstLocked}>
                  <TextField
                    size="small"
                    type="number"
                    value={financialForm.cgstPercent}
                    onChange={(e) => setFinancialForm({ ...financialForm, cgstPercent: Number(e.target.value) })}
                    disabled={lockedFields.gstLocked}
                    InputProps={{ endAdornment: <InputAdornment position="end"><PercentIcon fontSize="small" /></InputAdornment> }}
                    sx={{ width: 120 }}
                  />
                </SettingRow>

                <SettingRow label="SGST Rate" description="State GST rate percentage." locked={lockedFields.gstLocked}>
                  <TextField
                    size="small"
                    type="number"
                    value={financialForm.sgstPercent}
                    onChange={(e) => setFinancialForm({ ...financialForm, sgstPercent: Number(e.target.value) })}
                    disabled={lockedFields.gstLocked}
                    InputProps={{ endAdornment: <InputAdornment position="end"><PercentIcon fontSize="small" /></InputAdornment> }}
                    sx={{ width: 120 }}
                  />
                </SettingRow>

                <SettingRow label="IGST Rate" description="Integrated GST rate for inter-state transactions." locked={lockedFields.gstLocked} noBorder>
                  <TextField
                    size="small"
                    type="number"
                    value={financialForm.igstPercent}
                    onChange={(e) => setFinancialForm({ ...financialForm, igstPercent: Number(e.target.value) })}
                    disabled={lockedFields.gstLocked}
                    InputProps={{ endAdornment: <InputAdornment position="end"><PercentIcon fontSize="small" /></InputAdornment> }}
                    sx={{ width: 120 }}
                  />
                </SettingRow>
              </>
            )}

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={financialMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => financialMutation.mutate(financialForm)}
                disabled={financialMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 2: // Payment Rules
        return (
          <Box>
            <SectionHeader title="Payment & Reminder Rules" description="Configure payment grace periods and automated alert escalation." />

            <SettingRow label="Grace Period" description="Number of days after due date before payment escalation triggers.">
              <TextField
                size="small"
                type="number"
                value={paymentForm.gracePeriodDays}
                onChange={(e) => setPaymentForm({ ...paymentForm, gracePeriodDays: Number(e.target.value) })}
                InputProps={{ endAdornment: <InputAdornment position="end">days</InputAdornment> }}
                inputProps={{ min: 1, max: 90 }}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="Auto Alert Escalation" description="Automatically escalate alerts when payment is overdue beyond grace period." noBorder>
              <Switch
                checked={paymentForm.autoAlertEscalation}
                onChange={(e) => setPaymentForm({ ...paymentForm, autoAlertEscalation: e.target.checked })}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={paymentMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => paymentMutation.mutate(paymentForm)}
                disabled={paymentMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 3: // Agreement Rules
        return (
          <Box>
            <SectionHeader title="Agreement & Contract Rules" description="Default validity periods and renewal policies for HCF agreements." />

            <SettingRow label="Default Validity" description="Default duration for new HCF agreements.">
              <TextField
                size="small"
                type="number"
                value={agreementForm.defaultAgreementValidityMonths}
                onChange={(e) => setAgreementForm({ ...agreementForm, defaultAgreementValidityMonths: Number(e.target.value) })}
                InputProps={{ endAdornment: <InputAdornment position="end">months</InputAdornment> }}
                inputProps={{ min: 1, max: 60 }}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="Renewal Window" description="Days before expiry to prompt renewal reminders.">
              <TextField
                size="small"
                type="number"
                value={agreementForm.agreementRenewalWindowDays}
                onChange={(e) => setAgreementForm({ ...agreementForm, agreementRenewalWindowDays: Number(e.target.value) })}
                InputProps={{ endAdornment: <InputAdornment position="end">days</InputAdornment> }}
                inputProps={{ min: 7, max: 180 }}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="Block Overlapping Agreements" description="Prevent creating agreements with overlapping dates for same HCF." noBorder>
              <Switch
                checked={agreementForm.blockOverlappingAgreements}
                onChange={(e) => setAgreementForm({ ...agreementForm, blockOverlappingAgreements: e.target.checked })}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={agreementMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => agreementMutation.mutate(agreementForm)}
                disabled={agreementMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 4: // Operational
        return (
          <Box>
            <SectionHeader title="QR & Operational Rules" description="QR code validity, geofencing, and bag verification settings." />

            <SettingRow label="QR Validity" description="How long generated QR codes remain valid." locked={lockedFields.qrRulesLocked} lockedReason="Locked after first QR generation">
              <TextField
                size="small"
                type="number"
                value={operationalForm.qrValidityDays}
                onChange={(e) => setOperationalForm({ ...operationalForm, qrValidityDays: Number(e.target.value) })}
                disabled={lockedFields.qrRulesLocked}
                InputProps={{ endAdornment: <InputAdornment position="end">days</InputAdornment> }}
                inputProps={{ min: 1, max: 365 }}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="GPS Geofence Radius" description="Radius for location-based verification at HCF sites.">
              <TextField
                size="small"
                type="number"
                value={operationalForm.gpsGeofenceRadiusM}
                onChange={(e) => setOperationalForm({ ...operationalForm, gpsGeofenceRadiusM: Number(e.target.value) })}
                InputProps={{ endAdornment: <InputAdornment position="end">meters</InputAdornment> }}
                inputProps={{ min: 50, max: 1000 }}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="Max Unverified Bags" description="Maximum bags allowed before verification is required.">
              <TextField
                size="small"
                type="number"
                value={operationalForm.maxUnverifiedBags}
                onChange={(e) => setOperationalForm({ ...operationalForm, maxUnverifiedBags: Number(e.target.value) })}
                inputProps={{ min: 1, max: 100 }}
                sx={{ width: 100 }}
              />
            </SettingRow>

            <SettingRow label="Blue Waste Minimum" description="Minimum percentage of blue waste required in collections.">
              <TextField
                size="small"
                type="number"
                value={operationalForm.blueWasteMinPercent}
                onChange={(e) => setOperationalForm({ ...operationalForm, blueWasteMinPercent: Number(e.target.value) })}
                InputProps={{ endAdornment: <InputAdornment position="end"><PercentIcon fontSize="small" /></InputAdornment> }}
                inputProps={{ min: 0, max: 100 }}
                sx={{ width: 120 }}
              />
            </SettingRow>

            <SettingRow label="Allow Multiple Active QRs" description="Allow multiple active QR codes per HCF simultaneously.">
              <Switch
                checked={operationalForm.allowMultipleActiveQrs}
                onChange={(e) => setOperationalForm({ ...operationalForm, allowMultipleActiveQrs: e.target.checked })}
              />
            </SettingRow>

            <SettingRow label="Require CBWTF Verification" description="Require CBWTF staff to verify bags upon arrival." noBorder>
              <Switch
                checked={operationalForm.requireCbwtfVerification}
                onChange={(e) => setOperationalForm({ ...operationalForm, requireCbwtfVerification: e.target.checked })}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={operationalMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => operationalMutation.mutate(operationalForm)}
                disabled={operationalMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 5: // Compliance
        return (
          <Box>
            <SectionHeader title="Compliance & Reporting" description="Scheduled report generation times and data integrity settings." />

            <SettingRow label="Daily Report Time" description="Time of day to generate daily compliance reports.">
              <TextField
                size="small"
                type="time"
                value={complianceForm.dailyReportTime}
                onChange={(e) => setComplianceForm({ ...complianceForm, dailyReportTime: e.target.value })}
                sx={{ width: 140 }}
              />
            </SettingRow>

            <SettingRow label="Monthly Report Day" description="Day of month to generate monthly compliance reports.">
              <TextField
                size="small"
                type="number"
                value={complianceForm.monthlyReportDay}
                onChange={(e) => setComplianceForm({ ...complianceForm, monthlyReportDay: Number(e.target.value) })}
                inputProps={{ min: 1, max: 28 }}
                sx={{ width: 100 }}
              />
            </SettingRow>

            <SettingRow label="Enforce Checksum" description="Require data integrity checksums on compliance reports." noBorder>
              <Switch
                checked={complianceForm.enforceChecksum}
                onChange={(e) => setComplianceForm({ ...complianceForm, enforceChecksum: e.target.checked })}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={complianceMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => complianceMutation.mutate(complianceForm)}
                disabled={complianceMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 6: // Email
        return (
          <Box>
            <SectionHeader title="Email & Notifications" description="Configure notification preferences. Sender identity is system-controlled." />

            {/* Sender Identity - READ ONLY */}
            <Box sx={{ mb: 3, p: 2, bgcolor: 'background.default', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <LockIcon fontSize="small" />
                Sender Identity (System-Controlled)
              </Typography>
              <SettingRow label="From Name" description="Automatically generated from your facility name.">
                <TextField
                  fullWidth
                  size="small"
                  value={emailForm.resolvedSenderName || 'SmartCBWTF'}
                  disabled
                  sx={{ maxWidth: 280, '& .MuiInputBase-input.Mui-disabled': { color: 'text.primary', WebkitTextFillColor: 'unset' } }}
                />
              </SettingRow>
              <SettingRow label="From Address" description="Automatically generated sender address." noBorder>
                <TextField
                  fullWidth
                  size="small"
                  value={emailForm.resolvedSenderEmail || 'no-reply@smartcbwtf.com'}
                  disabled
                  sx={{ maxWidth: 280, fontFamily: 'monospace', '& .MuiInputBase-input.Mui-disabled': { color: 'text.primary', WebkitTextFillColor: 'unset' } }}
                />
              </SettingRow>
            </Box>

            <SettingRow label="Use Generic Sender" description="Send from no-reply@smartcbwtf.com instead of facility-specific address.">
              <Switch
                checked={emailForm.useGenericSender}
                onChange={(e) => setEmailForm({ ...emailForm, useGenericSender: e.target.checked })}
              />
            </SettingRow>

            <Alert severity="info" sx={{ mb: 2 }}>
              System notifications (alerts, billing, compliance reports) are sent to your <strong>profile email</strong>. 
              Update it in <a href="/cbwtf/profile" style={{ color: 'inherit' }}>My Profile</a>.
            </Alert>

            <SettingRow label="CC Admin on HCF Emails" description="Copy admin on all emails sent to HCF contacts.">
              <Switch
                checked={emailForm.ccAdminOnHcfEmails}
                onChange={(e) => setEmailForm({ ...emailForm, ccAdminOnHcfEmails: e.target.checked })}
              />
            </SettingRow>

            <SettingRow label="Email Notifications" description="Enable email notifications for system events.">
              <Switch
                checked={emailForm.emailNotificationsEnabled}
                onChange={(e) => setEmailForm({ ...emailForm, emailNotificationsEnabled: e.target.checked })}
              />
            </SettingRow>

            <SettingRow label="In-App Alerts" description="Enable in-app alert notifications." noBorder>
              <Switch
                checked={emailForm.inAppAlertsEnabled}
                onChange={(e) => setEmailForm({ ...emailForm, inAppAlertsEnabled: e.target.checked })}
              />
            </SettingRow>

            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={emailMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                onClick={() => emailMutation.mutate(emailForm)}
                disabled={emailMutation.isPending}
                sx={{ px: 4, fontWeight: 600 }}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        );

      case 7: // Branding
        return <BrandingSection onSettingsChange={() => queryClient.invalidateQueries({ queryKey: ['settings-audit'] })} />;

      case 8: // Email Templates
        return <EmailTemplateSection onSettingsChange={() => queryClient.invalidateQueries({ queryKey: ['settings-audit'] })} />;

      case 9: // Audit History
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
