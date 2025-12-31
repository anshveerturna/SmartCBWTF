import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Alert,
  Snackbar,
  CircularProgress,
  Paper,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  Email as EmailIcon,
  Refresh as RefreshIcon,
  Preview as PreviewIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { 
  getEmailTemplates, 
  getEmailTemplate, 
  updateEmailTemplate, 
  previewEmailTemplate,
  resetEmailTemplate,
} from '../../../api/cbwtf';
import type { EmailTemplateDTO } from '../../../api/cbwtf';
import { useTheme } from '@mui/material/styles';

// Template display names
const TEMPLATE_NAMES: Record<string, string> = {
  HCF_WELCOME: 'Welcome Email',
  HCF_CREDENTIALS: 'Credentials Email',
  AGREEMENT_EXPIRING: 'Agreement Expiry Warning',
  INVOICE_GENERATED: 'Invoice Generated',
  PAYMENT_REMINDER: 'Payment Reminder',
  PAYMENT_OVERDUE: 'Payment Overdue',
};

// Sample data for preview
const SAMPLE_DATA: Record<string, Record<string, string>> = {
  HCF_WELCOME: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    agreementNumber: 'AGR-2024-001',
    registrationDate: '2024-01-15',
  },
  HCF_CREDENTIALS: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    username: 'cityhospital',
    loginUrl: 'https://app.smartcbwtf.com/login',
  },
  AGREEMENT_EXPIRING: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    agreementNumber: 'AGR-2024-001',
    expiryDate: '2025-01-15',
  },
  INVOICE_GENERATED: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    invoiceNumber: 'INV-2024-0123',
    amount: '15,000',
    dueDate: '2024-02-15',
  },
  PAYMENT_REMINDER: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    invoiceNumber: 'INV-2024-0123',
    amountDue: '15,000',
    dueDate: '2024-02-15',
  },
  PAYMENT_OVERDUE: {
    hcfName: 'City Hospital',
    facilityName: 'Delhi CBWTF',
    invoiceNumber: 'INV-2024-0123',
    amountDue: '15,000',
    daysOverdue: '7',
  },
};

interface EmailTemplateSectionProps {
  onSettingsChange?: () => void;
}

export default function EmailTemplateSection({ onSettingsChange }: EmailTemplateSectionProps) {
  const queryClient = useQueryClient();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [selectedTemplate, setSelectedTemplate] = useState<string>('HCF_WELCOME');
  const [editedBody, setEditedBody] = useState<string>('');
  const [editedSubject, setEditedSubject] = useState<string>('');
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewHtml, setPreviewHtml] = useState('');
  const [resetDialogOpen, setResetDialogOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  // Fetch all templates
  const { data: templates, isLoading: templatesLoading } = useQuery<EmailTemplateDTO[]>({
    queryKey: ['emailTemplates'],
    queryFn: getEmailTemplates,
  });

  // Fetch selected template
  const { data: template, isLoading: templateLoading, refetch } = useQuery<EmailTemplateDTO>({
    queryKey: ['emailTemplate', selectedTemplate],
    queryFn: () => getEmailTemplate(selectedTemplate),
    enabled: !!selectedTemplate,
  });

  // Sync editor when template loads
  useEffect(() => {
    if (template) {
      setEditedBody(template.bodyTemplate || '');
      setEditedSubject(template.subjectTemplate || '');
    }
  }, [template]);

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: (data: EmailTemplateDTO) => updateEmailTemplate(selectedTemplate, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emailTemplates'] });
      queryClient.invalidateQueries({ queryKey: ['emailTemplate', selectedTemplate] });
      setSnackbar({ open: true, message: 'Template saved successfully', severity: 'success' });
      onSettingsChange?.();
    },
    onError: (error: Error) => {
      setSnackbar({ open: true, message: error.message || 'Failed to save template', severity: 'error' });
    },
  });

  // Preview mutation
  const previewMutation = useMutation({
    mutationFn: () => previewEmailTemplate(selectedTemplate, editedBody, SAMPLE_DATA[selectedTemplate] || {}),
    onSuccess: (data) => {
      setPreviewHtml(data.html);
      setPreviewOpen(true);
    },
    onError: (error: Error) => {
      setSnackbar({ open: true, message: error.message || 'Preview failed', severity: 'error' });
    },
  });

  // Reset mutation
  const resetMutation = useMutation({
    mutationFn: () => resetEmailTemplate(selectedTemplate),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emailTemplates'] });
      queryClient.invalidateQueries({ queryKey: ['emailTemplate', selectedTemplate] });
      setResetDialogOpen(false);
      setSnackbar({ open: true, message: 'Template reset to default', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Failed to reset template', severity: 'error' });
    },
  });

  const handleSave = () => {
    updateMutation.mutate({
      templateCode: selectedTemplate,
      subjectTemplate: editedSubject,
      bodyTemplate: editedBody,
    });
  };

  const insertPlaceholder = (placeholder: string) => {
    const textarea = document.getElementById('template-editor') as HTMLTextAreaElement;
    if (textarea) {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const newValue = editedBody.slice(0, start) + `{{${placeholder}}}` + editedBody.slice(end);
      setEditedBody(newValue);
    } else {
      setEditedBody(editedBody + `{{${placeholder}}}`);
    }
  };

  if (templatesLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* Template Selector */}
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Box sx={{ px: 3, py: 2, bgcolor: isDark ? 'background.default' : 'grey.100', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            <EmailIcon sx={{ fontSize: 20, mr: 1, verticalAlign: 'middle' }} />
            Email Templates
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Customize email templates sent to HCFs
          </Typography>
        </Box>

        <Box sx={{ p: 3 }}>
          <FormControl fullWidth sx={{ mb: 3 }}>
            <InputLabel>Select Template</InputLabel>
            <Select
              value={selectedTemplate}
              label="Select Template"
              onChange={(e) => setSelectedTemplate(e.target.value)}
            >
              {Object.entries(TEMPLATE_NAMES).map(([code, name]) => (
                <MenuItem key={code} value={code}>{name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          {templateLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : template ? (
            <>
              {/* Subject Line */}
              <TextField
                fullWidth
                label="Subject Line"
                value={editedSubject}
                onChange={(e) => setEditedSubject(e.target.value)}
                sx={{ mb: 2 }}
                helperText="Use {{placeholders}} for dynamic content"
              />

              {/* Placeholder chips */}
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  <InfoIcon sx={{ fontSize: 14, mr: 0.5, verticalAlign: 'middle' }} />
                  Available placeholders (click to insert):
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {template.availablePlaceholders?.map((p) => (
                    <Tooltip key={p} title={`Insert {{${p}}}`}>
                      <Chip
                        label={`{{${p}}}`}
                        size="small"
                        variant={template.requiredPlaceholders?.includes(p) ? 'filled' : 'outlined'}
                        color={template.requiredPlaceholders?.includes(p) ? 'primary' : 'default'}
                        onClick={() => insertPlaceholder(p)}
                        sx={{ cursor: 'pointer' }}
                      />
                    </Tooltip>
                  ))}
                </Box>
                {template.requiredPlaceholders && template.requiredPlaceholders.length > 0 && (
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                    Required placeholders are highlighted
                  </Typography>
                )}
              </Box>

              {/* HTML Editor */}
              <TextField
                id="template-editor"
                fullWidth
                multiline
                rows={15}
                label="Template Body (HTML)"
                value={editedBody}
                onChange={(e) => setEditedBody(e.target.value)}
                sx={{ 
                  mb: 2,
                  '& .MuiInputBase-input': {
                    fontFamily: 'monospace',
                    fontSize: '0.875rem',
                  }
                }}
              />

              {/* Action Buttons */}
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    variant="outlined"
                    startIcon={<PreviewIcon />}
                    onClick={() => previewMutation.mutate()}
                    disabled={previewMutation.isPending}
                  >
                    {previewMutation.isPending ? 'Loading...' : 'Preview'}
                  </Button>
                  <Button
                    variant="outlined"
                    color="warning"
                    startIcon={<RefreshIcon />}
                    onClick={() => setResetDialogOpen(true)}
                  >
                    Reset to Default
                  </Button>
                </Box>
                <Button
                  variant="contained"
                  onClick={handleSave}
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save Template'}
                </Button>
              </Box>
            </>
          ) : (
            <Alert severity="info">Select a template to edit</Alert>
          )}
        </Box>
      </Paper>

      {/* Preview Dialog */}
      <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Email Preview</DialogTitle>
        <DialogContent>
          <Box
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              p: 2,
              bgcolor: 'white',
              minHeight: 400,
            }}
            dangerouslySetInnerHTML={{ __html: previewHtml }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreviewOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Reset Confirmation Dialog */}
      <Dialog open={resetDialogOpen} onClose={() => setResetDialogOpen(false)}>
        <DialogTitle>Reset Template to Default?</DialogTitle>
        <DialogContent>
          <Typography>
            This will replace your custom template with the system default.
            A new version will be created and your current version will be preserved in history.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResetDialogOpen(false)}>Cancel</Button>
          <Button
            color="warning"
            onClick={() => resetMutation.mutate()}
            disabled={resetMutation.isPending}
          >
            {resetMutation.isPending ? 'Resetting...' : 'Reset'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
