import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Chip,
  Button,
  TextField,
  CircularProgress,
  Divider,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Snackbar,
  Alert,
  Collapse,
  Avatar,
  Badge,
} from '@mui/material';
import {
  Save as SaveIcon,
  History as HistoryIcon,
  Visibility as PreviewIcon,
  CheckCircle as ActiveIcon,
  ExpandMore as ExpandIcon,
  ExpandLess as CollapseIcon,
  Email as EmailIcon,
  Code as CodeIcon,
  Schedule as ScheduleIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material';
import Editor from '@monaco-editor/react';
import api from '../../api/client';

// Types
interface GlobalEmailTemplate {
  id: string;
  templateCode: string;
  category: string;
  subject: string;
  bodyHtml: string;
  requiredPlaceholders: string[];
  optionalPlaceholders: string[];
  version: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface RenderedEmail {
  templateCode: string;
  templateVersion: number;
  templateChecksum: string;
  subject: string;
  bodyHtml: string;
}

// Category display config with icons
const CATEGORY_CONFIG: Record<string, { label: string; color: string; bgColor: string; icon: string }> = {
  REGISTRATION: { label: 'Registration', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.1)', icon: '📋' },
  BILLING: { label: 'Billing', color: '#8b5cf6', bgColor: 'rgba(139, 92, 246, 0.1)', icon: '💳' },
  PAYMENT: { label: 'Payments', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.1)', icon: '💰' },
  COMPLIANCE: { label: 'Compliance', color: '#3b82f6', bgColor: 'rgba(59, 130, 246, 0.1)', icon: '📜' },
  SYSTEM: { label: 'System', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.1)', icon: '⚙️' },
};

// API functions
const getActiveTemplates = async (): Promise<GlobalEmailTemplate[]> => {
  const response = await api.get('/api/superadmin/email-templates');
  return response.data;
};

const getTemplateVersions = async (code: string): Promise<GlobalEmailTemplate[]> => {
  const response = await api.get(`/api/superadmin/email-templates/${code}/versions`);
  return response.data;
};

const updateTemplate = async (code: string, data: { subject: string; bodyHtml: string; requiredPlaceholders: string[]; optionalPlaceholders: string[] }): Promise<GlobalEmailTemplate> => {
  const response = await api.put(`/api/superadmin/email-templates/${code}`, data);
  return response.data;
};

const activateVersion = async (code: string, version: number): Promise<GlobalEmailTemplate> => {
  const response = await api.post(`/api/superadmin/email-templates/${code}/activate/${version}`);
  return response.data;
};

const previewTemplate = async (code: string, sampleData: Record<string, string>): Promise<RenderedEmail> => {
  const response = await api.post(`/api/superadmin/email-templates/${code}/preview`, sampleData);
  return response.data;
};

// Sample data for previews
const SAMPLE_DATA: Record<string, Record<string, string>> = {
  HCF_WELCOME: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF' },
  HCF_CREDENTIALS: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', username: 'cityhospital', password: 'TempPass123', loginUrl: 'https://app.smartcbwtf.com' },
  AGREEMENT_SUBMITTED: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', agreementNumber: 'AGR-2024-001', submittedDate: '31/12/2024' },
  AGREEMENT_APPROVED: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', agreementNumber: 'AGR-2024-001', effectiveDate: '01/01/2025', expiryDate: '31/12/2025' },
  AGREEMENT_REJECTED: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', agreementNumber: 'AGR-2024-001', rejectionReason: 'Incomplete documentation' },
  AGREEMENT_EXPIRY: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', agreementNumber: 'AGR-2024-001', expiryDate: '31/12/2024', daysRemaining: '7' },
  INVOICE_GENERATED: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', invoiceNumber: 'INV-2024-001', invoiceDate: '01/12/2024', invoiceAmount: '15,000', dueDate: '15/12/2024' },
  PAYMENT_REMINDER: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', invoiceNumber: 'INV-2024-001', amountDue: '15,000', dueDate: '15/12/2024' },
  PAYMENT_OVERDUE: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', invoiceNumber: 'INV-2024-001', amountDue: '15,000', daysPastDue: '10' },
  PAYMENT_RECEIVED: { hcfName: 'City Hospital', facilityName: 'Metro CBWTF', invoiceNumber: 'INV-2024-001', amountReceived: '15,000', paymentDate: '20/12/2024', receiptNumber: 'RCT-2024-001' },
};

const formatTemplateName = (code: string) => code.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());

export default function EmailTemplates() {
  const queryClient = useQueryClient();
  const [selectedCode, setSelectedCode] = useState<string | null>(null);
  const [editSubject, setEditSubject] = useState('');
  const [editBody, setEditBody] = useState('');
  const [previewTab, setPreviewTab] = useState(0);
  const [historyDialog, setHistoryDialog] = useState(false);
  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({
    REGISTRATION: true, BILLING: true, PAYMENT: true, COMPLIANCE: true, SYSTEM: true
  });
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'success' });

  // Queries
  const { data: templates, isLoading } = useQuery({
    queryKey: ['superadmin-email-templates'],
    queryFn: getActiveTemplates,
  });

  const { data: versions } = useQuery({
    queryKey: ['template-versions', selectedCode],
    queryFn: () => selectedCode ? getTemplateVersions(selectedCode) : Promise.resolve([]),
    enabled: !!selectedCode && historyDialog,
  });

  const { data: preview, refetch: refetchPreview, isFetching: isPreviewFetching } = useQuery({
    queryKey: ['template-preview', selectedCode],
    queryFn: () => selectedCode ? previewTemplate(selectedCode, SAMPLE_DATA[selectedCode] || {}) : Promise.resolve(null),
    enabled: !!selectedCode && previewTab === 1,
  });

  // Group templates by category
  const groupedTemplates = useMemo(() => {
    if (!templates) return {};
    return templates.reduce((acc, template) => {
      const cat = template.category || 'SYSTEM';
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(template);
      return acc;
    }, {} as Record<string, GlobalEmailTemplate[]>);
  }, [templates]);

  // Mutations
  const saveMutation = useMutation({
    mutationFn: () => {
      const template = templates?.find(t => t.templateCode === selectedCode);
      if (!selectedCode || !template) return Promise.reject();
      return updateTemplate(selectedCode, {
        subject: editSubject,
        bodyHtml: editBody,
        requiredPlaceholders: template.requiredPlaceholders,
        optionalPlaceholders: template.optionalPlaceholders,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['superadmin-email-templates'] });
      setSnackbar({ open: true, message: 'Template saved successfully', severity: 'success' });
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : 'Failed to save template';
      setSnackbar({ open: true, message, severity: 'error' });
    },
  });

  const activateMutation = useMutation({
    mutationFn: ({ code, version }: { code: string; version: number }) => activateVersion(code, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['superadmin-email-templates'] });
      queryClient.invalidateQueries({ queryKey: ['template-versions', selectedCode] });
      setSnackbar({ open: true, message: 'Version activated', severity: 'success' });
    },
  });

  const handleSelectTemplate = (template: GlobalEmailTemplate) => {
    setSelectedCode(template.templateCode);
    setEditSubject(template.subject);
    setEditBody(template.bodyHtml);
    setPreviewTab(0);
  };

  const toggleCategory = (cat: string) => {
    setExpandedCategories(prev => ({ ...prev, [cat]: !prev[cat] }));
  };

  const copyPlaceholder = (placeholder: string) => {
    navigator.clipboard.writeText(`{{${placeholder}}}`);
    setSnackbar({ open: true, message: `Copied {{${placeholder}}}`, severity: 'success' });
  };

  const selectedTemplate = templates?.find(t => t.templateCode === selectedCode);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress size={40} sx={{ color: '#10b981' }} />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3, height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <Avatar sx={{ bgcolor: 'rgba(16, 185, 129, 0.1)', width: 48, height: 48 }}>
            <EmailIcon sx={{ color: '#10b981', fontSize: 24 }} />
          </Avatar>
          <Box>
            <Typography variant="h5" fontWeight={700} sx={{ color: '#f8fafc', letterSpacing: '-0.02em' }}>
              Email Templates
            </Typography>
            <Typography variant="body2" sx={{ color: '#94a3b8' }}>
              Manage versioned email templates with audit trail
            </Typography>
          </Box>
        </Box>
      </Box>

      {/* Main Content */}
      <Box sx={{ display: 'flex', gap: 2, flex: 1, minHeight: 0 }}>
        {/* Left Panel - Template List */}
        <Paper 
          elevation={0}
          sx={{ 
            width: 320, 
            flexShrink: 0, 
            overflow: 'auto',
            bgcolor: 'rgba(30, 41, 59, 0.5)',
            border: '1px solid rgba(148, 163, 184, 0.1)',
            borderRadius: 2,
          }}
        >
          <Box sx={{ p: 2, borderBottom: '1px solid rgba(148, 163, 184, 0.1)' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography variant="subtitle2" fontWeight={600} sx={{ color: '#e2e8f0' }}>
                All Templates
              </Typography>
              <Chip 
                label={templates?.length || 0} 
                size="small" 
                sx={{ 
                  bgcolor: 'rgba(16, 185, 129, 0.15)', 
                  color: '#10b981',
                  fontWeight: 600,
                  fontSize: 11,
                  height: 22,
                }}
              />
            </Box>
          </Box>
          
          <List disablePadding sx={{ px: 1, py: 1 }}>
            {Object.entries(CATEGORY_CONFIG).map(([catKey, catConfig]) => {
              const catTemplates = groupedTemplates[catKey] || [];
              if (catTemplates.length === 0) return null;
              return (
                <Box key={catKey} sx={{ mb: 1 }}>
                  <ListItemButton 
                    onClick={() => toggleCategory(catKey)} 
                    sx={{ 
                      borderRadius: 1.5,
                      py: 1,
                      '&:hover': { bgcolor: 'rgba(148, 163, 184, 0.08)' }
                    }}
                  >
                    <Typography sx={{ fontSize: 16, mr: 1 }}>{catConfig.icon}</Typography>
                    <ListItemText 
                      primary={catConfig.label} 
                      primaryTypographyProps={{ 
                        fontWeight: 600, 
                        fontSize: 13,
                        color: '#e2e8f0',
                      }} 
                    />
                    <Badge 
                      badgeContent={catTemplates.length} 
                      sx={{ 
                        mr: 1,
                        '& .MuiBadge-badge': { 
                          bgcolor: catConfig.bgColor,
                          color: catConfig.color,
                          fontWeight: 600,
                          fontSize: 10,
                        }
                      }}
                    />
                    {expandedCategories[catKey] ? 
                      <CollapseIcon sx={{ fontSize: 18, color: '#64748b' }} /> : 
                      <ExpandIcon sx={{ fontSize: 18, color: '#64748b' }} />
                    }
                  </ListItemButton>
                  
                  <Collapse in={expandedCategories[catKey]}>
                    <Box sx={{ pl: 1 }}>
                      {catTemplates.map((template) => (
                        <ListItem key={template.templateCode} disablePadding sx={{ mb: 0.5 }}>
                          <ListItemButton
                            selected={selectedCode === template.templateCode}
                            onClick={() => handleSelectTemplate(template)}
                            sx={{ 
                              borderRadius: 1.5,
                              py: 1,
                              pl: 2,
                              transition: 'all 0.15s ease',
                              bgcolor: selectedCode === template.templateCode 
                                ? 'rgba(16, 185, 129, 0.15)' 
                                : 'transparent',
                              borderLeft: selectedCode === template.templateCode 
                                ? '3px solid #10b981' 
                                : '3px solid transparent',
                              '&:hover': { 
                                bgcolor: selectedCode === template.templateCode 
                                  ? 'rgba(16, 185, 129, 0.15)'
                                  : 'rgba(148, 163, 184, 0.08)'
                              },
                            }}
                          >
                            <ListItemText
                              primary={formatTemplateName(template.templateCode)}
                              secondary={`Version ${template.version}`}
                              primaryTypographyProps={{ 
                                fontSize: 13, 
                                fontWeight: 500,
                                color: selectedCode === template.templateCode ? '#10b981' : '#cbd5e1',
                              }}
                              secondaryTypographyProps={{ 
                                fontSize: 11,
                                color: '#64748b',
                              }}
                            />
                            {template.isActive && (
                              <Box 
                                sx={{ 
                                  width: 8, 
                                  height: 8, 
                                  borderRadius: '50%', 
                                  bgcolor: '#10b981',
                                  boxShadow: '0 0 6px rgba(16, 185, 129, 0.5)',
                                }}
                              />
                            )}
                          </ListItemButton>
                        </ListItem>
                      ))}
                    </Box>
                  </Collapse>
                </Box>
              );
            })}
          </List>
        </Paper>

        {/* Center Panel - Editor */}
        <Paper 
          elevation={0}
          sx={{ 
            flex: 1, 
            overflow: 'hidden', 
            display: 'flex', 
            flexDirection: 'column',
            bgcolor: 'rgba(30, 41, 59, 0.5)',
            border: '1px solid rgba(148, 163, 184, 0.1)',
            borderRadius: 2,
          }}
        >
          {selectedTemplate ? (
            <>
              {/* Template Header */}
              <Box sx={{ 
                p: 2, 
                borderBottom: '1px solid rgba(148, 163, 184, 0.1)',
                display: 'flex', 
                alignItems: 'center', 
                gap: 2,
              }}>
                <Box sx={{ flex: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
                    <Typography variant="h6" fontWeight={600} sx={{ color: '#f8fafc' }}>
                      {formatTemplateName(selectedTemplate.templateCode)}
                    </Typography>
                    <Chip 
                      label={`v${selectedTemplate.version}`} 
                      size="small" 
                      sx={{ 
                        bgcolor: 'rgba(148, 163, 184, 0.15)',
                        color: '#94a3b8',
                        fontSize: 11,
                        height: 22,
                        fontWeight: 600,
                      }}
                    />
                    <Chip 
                      label={CATEGORY_CONFIG[selectedTemplate.category]?.label || selectedTemplate.category}
                      size="small" 
                      sx={{ 
                        bgcolor: CATEGORY_CONFIG[selectedTemplate.category]?.bgColor,
                        color: CATEGORY_CONFIG[selectedTemplate.category]?.color,
                        fontSize: 11,
                        height: 22,
                        fontWeight: 600,
                      }}
                    />
                  </Box>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Last modified: {new Date(selectedTemplate.updatedAt).toLocaleDateString()}
                  </Typography>
                </Box>
                
                <Tooltip title="Version History" arrow>
                  <IconButton 
                    size="small" 
                    onClick={() => setHistoryDialog(true)}
                    sx={{ 
                      color: '#94a3b8',
                      '&:hover': { bgcolor: 'rgba(148, 163, 184, 0.1)' }
                    }}
                  >
                    <HistoryIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                
                <Button
                  variant="contained"
                  size="small"
                  startIcon={saveMutation.isPending ? <CircularProgress size={14} color="inherit" /> : <SaveIcon />}
                  onClick={() => saveMutation.mutate()}
                  disabled={saveMutation.isPending}
                  sx={{
                    bgcolor: '#10b981',
                    color: '#fff',
                    fontWeight: 600,
                    textTransform: 'none',
                    px: 2,
                    '&:hover': { bgcolor: '#059669' },
                    '&:disabled': { bgcolor: 'rgba(16, 185, 129, 0.5)' },
                  }}
                >
                  Save Changes
                </Button>
              </Box>

              {/* Placeholders Bar */}
              <Box sx={{ 
                px: 2, 
                py: 1.5, 
                bgcolor: 'rgba(15, 23, 42, 0.5)',
                borderBottom: '1px solid rgba(148, 163, 184, 0.1)',
                display: 'flex', 
                alignItems: 'center',
                gap: 1,
                flexWrap: 'wrap',
              }}>
                <CodeIcon sx={{ fontSize: 16, color: '#64748b' }} />
                <Typography variant="caption" sx={{ color: '#64748b', mr: 1 }}>Placeholders:</Typography>
                {selectedTemplate.requiredPlaceholders.map(p => (
                  <Chip 
                    key={p} 
                    label={`{{${p}}}`} 
                    size="small" 
                    onClick={() => copyPlaceholder(p)}
                    icon={<CopyIcon sx={{ fontSize: '12px !important' }} />}
                    sx={{ 
                      height: 24,
                      bgcolor: 'rgba(16, 185, 129, 0.1)',
                      color: '#10b981',
                      fontSize: 11,
                      fontFamily: 'monospace',
                      cursor: 'pointer',
                      '& .MuiChip-icon': { color: '#10b981' },
                      '&:hover': { bgcolor: 'rgba(16, 185, 129, 0.2)' },
                    }}
                  />
                ))}
                {selectedTemplate.optionalPlaceholders.map(p => (
                  <Chip 
                    key={p} 
                    label={`{{${p}}}`} 
                    size="small" 
                    onClick={() => copyPlaceholder(p)}
                    icon={<CopyIcon sx={{ fontSize: '12px !important' }} />}
                    sx={{ 
                      height: 24,
                      bgcolor: 'rgba(148, 163, 184, 0.1)',
                      color: '#94a3b8',
                      fontSize: 11,
                      fontFamily: 'monospace',
                      cursor: 'pointer',
                      '& .MuiChip-icon': { color: '#94a3b8' },
                      '&:hover': { bgcolor: 'rgba(148, 163, 184, 0.2)' },
                    }}
                  />
                ))}
              </Box>

              {/* Tabs */}
              <Tabs 
                value={previewTab} 
                onChange={(_, v) => { setPreviewTab(v); if (v === 1) refetchPreview(); }} 
                sx={{ 
                  px: 2, 
                  borderBottom: '1px solid rgba(148, 163, 184, 0.1)',
                  minHeight: 44,
                  '& .MuiTab-root': {
                    minHeight: 44,
                    textTransform: 'none',
                    fontWeight: 500,
                    color: '#64748b',
                    '&.Mui-selected': { color: '#10b981' },
                  },
                  '& .MuiTabs-indicator': { bgcolor: '#10b981' },
                }}
              >
                <Tab label="Edit" icon={<CodeIcon sx={{ fontSize: 16 }} />} iconPosition="start" />
                <Tab label="Preview" icon={<PreviewIcon sx={{ fontSize: 16 }} />} iconPosition="start" />
              </Tabs>

              {/* Content Area */}
              <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
                {previewTab === 0 ? (
                  <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                    <Box sx={{ p: 2 }}>
                      <TextField
                        label="Email Subject"
                        fullWidth
                        size="small"
                        value={editSubject}
                        onChange={(e) => setEditSubject(e.target.value)}
                        placeholder="Enter email subject line..."
                        sx={{
                          '& .MuiOutlinedInput-root': {
                            bgcolor: 'rgba(15, 23, 42, 0.5)',
                            '& fieldset': { borderColor: 'rgba(148, 163, 184, 0.2)' },
                            '&:hover fieldset': { borderColor: 'rgba(148, 163, 184, 0.3)' },
                            '&.Mui-focused fieldset': { borderColor: '#10b981' },
                          },
                          '& .MuiInputLabel-root': { color: '#64748b' },
                          '& .MuiInputBase-input': { color: '#e2e8f0' },
                        }}
                      />
                    </Box>
                    <Box sx={{ flex: 1, minHeight: 300 }}>
                      <Editor
                        height="100%"
                        defaultLanguage="html"
                        value={editBody}
                        onChange={(value) => setEditBody(value || '')}
                        theme="vs-dark"
                        options={{
                          minimap: { enabled: false },
                          wordWrap: 'on',
                          fontSize: 13,
                          lineNumbers: 'on',
                          scrollBeyondLastLine: false,
                          padding: { top: 16 },
                          fontFamily: 'JetBrains Mono, Monaco, Consolas, monospace',
                        }}
                      />
                    </Box>
                  </Box>
                ) : (
                  <Box sx={{ p: 3, bgcolor: '#1e293b', height: '100%', overflow: 'auto' }}>
                    {isPreviewFetching ? (
                      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                        <CircularProgress size={32} sx={{ color: '#10b981' }} />
                      </Box>
                    ) : preview ? (
                      <Paper 
                        elevation={8} 
                        sx={{ 
                          maxWidth: 640, 
                          mx: 'auto', 
                          bgcolor: '#ffffff',
                          borderRadius: 2,
                          overflow: 'hidden',
                        }}
                      >
                        {/* Email Header */}
                        <Box sx={{ p: 2.5, bgcolor: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                          <Box sx={{ display: 'flex', gap: 1.5, mb: 1 }}>
                            <Typography sx={{ minWidth: 60, color: '#64748b', fontSize: 13, fontWeight: 500 }}>From:</Typography>
                            <Typography sx={{ color: '#1e293b', fontSize: 13, fontWeight: 500 }}>noreply@smartcbwtf.com</Typography>
                          </Box>
                          <Box sx={{ display: 'flex', gap: 1.5, mb: 1 }}>
                            <Typography sx={{ minWidth: 60, color: '#64748b', fontSize: 13, fontWeight: 500 }}>To:</Typography>
                            <Typography sx={{ color: '#1e293b', fontSize: 13 }}>recipient@example.com</Typography>
                          </Box>
                          <Box sx={{ display: 'flex', gap: 1.5 }}>
                            <Typography sx={{ minWidth: 60, color: '#64748b', fontSize: 13, fontWeight: 500 }}>Subject:</Typography>
                            <Typography sx={{ color: '#1e293b', fontSize: 13, fontWeight: 600 }}>{preview.subject}</Typography>
                          </Box>
                        </Box>
                        
                        {/* Email Body */}
                        <Box sx={{ p: 3 }}>
                          <div 
                            dangerouslySetInnerHTML={{ __html: preview.bodyHtml }} 
                            style={{ 
                              fontFamily: 'Arial, Helvetica, sans-serif', 
                              fontSize: '14px', 
                              lineHeight: '1.7',
                              color: '#334155',
                            }}
                          />
                        </Box>

                        {/* Footer */}
                        <Box sx={{ 
                          px: 2.5, 
                          py: 1.5, 
                          bgcolor: '#f1f5f9', 
                          borderTop: '1px solid #e2e8f0',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <ScheduleIcon sx={{ fontSize: 14, color: '#94a3b8' }} />
                            <Typography sx={{ color: '#94a3b8', fontSize: 11 }}>
                              {preview.templateCode} v{preview.templateVersion}
                            </Typography>
                          </Box>
                          <Typography sx={{ color: '#cbd5e1', fontSize: 10, fontFamily: 'monospace' }}>
                            {preview.templateChecksum.substring(0, 16)}
                          </Typography>
                        </Box>
                      </Paper>
                    ) : null}
                  </Box>
                )}
              </Box>
            </>
          ) : (
            <Box sx={{ 
              display: 'flex', 
              flexDirection: 'column',
              justifyContent: 'center', 
              alignItems: 'center', 
              height: '100%',
              gap: 2,
            }}>
              <Avatar sx={{ width: 80, height: 80, bgcolor: 'rgba(148, 163, 184, 0.1)' }}>
                <EmailIcon sx={{ fontSize: 40, color: '#475569' }} />
              </Avatar>
              <Typography sx={{ color: '#64748b', fontSize: 15 }}>
                Select a template to start editing
              </Typography>
            </Box>
          )}
        </Paper>
      </Box>

      {/* Version History Dialog */}
      <Dialog 
        open={historyDialog} 
        onClose={() => setHistoryDialog(false)} 
        maxWidth="sm" 
        fullWidth
        PaperProps={{
          sx: {
            bgcolor: '#1e293b',
            backgroundImage: 'none',
            border: '1px solid rgba(148, 163, 184, 0.1)',
          }
        }}
      >
        <DialogTitle sx={{ color: '#f8fafc', borderBottom: '1px solid rgba(148, 163, 184, 0.1)' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <HistoryIcon sx={{ color: '#10b981' }} />
            Version History
          </Box>
        </DialogTitle>
        <DialogContent sx={{ p: 0 }}>
          <List>
            {versions?.map((v, index) => (
              <ListItem
                key={v.id}
                sx={{ 
                  borderBottom: index < (versions?.length || 0) - 1 ? '1px solid rgba(148, 163, 184, 0.1)' : 'none',
                  py: 2,
                }}
                secondaryAction={
                  v.isActive ? (
                    <Chip 
                      label="Active" 
                      size="small" 
                      icon={<ActiveIcon />}
                      sx={{ 
                        bgcolor: 'rgba(16, 185, 129, 0.15)',
                        color: '#10b981',
                        '& .MuiChip-icon': { color: '#10b981' },
                      }}
                    />
                  ) : (
                    <Button 
                      size="small" 
                      onClick={() => activateMutation.mutate({ code: v.templateCode, version: v.version })}
                      sx={{ 
                        color: '#94a3b8',
                        textTransform: 'none',
                        '&:hover': { bgcolor: 'rgba(148, 163, 184, 0.1)' }
                      }}
                    >
                      Activate
                    </Button>
                  )
                }
              >
                <ListItemText
                  primary={`Version ${v.version}`}
                  secondary={new Date(v.updatedAt).toLocaleString()}
                  primaryTypographyProps={{ color: '#e2e8f0', fontWeight: 500 }}
                  secondaryTypographyProps={{ color: '#64748b', fontSize: 12 }}
                />
              </ListItem>
            ))}
          </List>
        </DialogContent>
        <DialogActions sx={{ borderTop: '1px solid rgba(148, 163, 184, 0.1)', p: 2 }}>
          <Button 
            onClick={() => setHistoryDialog(false)}
            sx={{ color: '#94a3b8', textTransform: 'none' }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          severity={snackbar.severity} 
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          sx={{ 
            bgcolor: snackbar.severity === 'success' ? 'rgba(16, 185, 129, 0.95)' : 'rgba(239, 68, 68, 0.95)',
            color: '#fff',
            '& .MuiAlert-icon': { color: '#fff' },
          }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
