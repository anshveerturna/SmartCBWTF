import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  CircularProgress,
  Alert,
  Tooltip,
  TablePagination,
} from '@mui/material';
import {
  Assessment as ReportIcon,
  CheckCircle as ReadyIcon,
  Warning as FlaggedIcon,
  PictureAsPdf as PdfIcon,
  TableChart as ExcelIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { getComplianceReports, downloadComplianceReportPdf, downloadAnnualReportExcel } from '../../api/cbwtf';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-IN', { 
    day: '2-digit', 
    month: 'short', 
    year: 'numeric' 
  });
};

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('en-IN', { 
    day: '2-digit', 
    month: 'short', 
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

export default function ComplianceReports() {
  const [activeTab, setActiveTab] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  // Fetch daily reports
  const { data: dailyReports, isLoading: loadingDaily } = useQuery({
    queryKey: ['compliance', 'daily', page, rowsPerPage],
    queryFn: () => getComplianceReports('daily', page, rowsPerPage),
    enabled: activeTab === 0,
  });

  // Fetch monthly reports
  const { data: monthlyReports, isLoading: loadingMonthly } = useQuery({
    queryKey: ['compliance', 'monthly', page, rowsPerPage],
    queryFn: () => getComplianceReports('monthly', page, rowsPerPage),
    enabled: activeTab === 1,
  });

  // Fetch annual reports
  const { data: annualReports, isLoading: loadingAnnual } = useQuery({
    queryKey: ['compliance', 'annual', page, rowsPerPage],
    queryFn: () => getComplianceReports('annual', page, rowsPerPage),
    enabled: activeTab === 2,
  });

  // Fetch barcode reports
  const { data: barcodeReports, isLoading: loadingBarcode } = useQuery({
    queryKey: ['compliance', 'barcode', page, rowsPerPage],
    queryFn: () => getComplianceReports('barcode', page, rowsPerPage),
    enabled: activeTab === 3,
  });

  // Fetch violation reports
  const { data: violationReports, isLoading: loadingViolations } = useQuery({
    queryKey: ['compliance', 'violations', page, rowsPerPage],
    queryFn: () => getComplianceReports('violations', page, rowsPerPage),
    enabled: activeTab === 4,
  });

  const handleDownloadPdf = async (type: string, id: string) => {
    try {
      await downloadComplianceReportPdf(type, id);
    } catch (error) {
      console.error('Failed to download PDF:', error);
    }
  };

  const handleDownloadExcel = async (id: string) => {
    try {
      await downloadAnnualReportExcel(id);
    } catch (error) {
      console.error('Failed to download Excel:', error);
    }
  };

  const StatusChip = ({ status }: { status: string }) => (
    <Chip
      icon={status === 'READY' ? <ReadyIcon /> : <FlaggedIcon />}
      label={status}
      color={status === 'READY' ? 'success' : 'warning'}
      size="small"
      variant="outlined"
    />
  );

  const CompletenessChip = ({ completeness }: { completeness: string }) => (
    <Chip
      label={completeness}
      color={completeness === 'COMPLETE' ? 'default' : 'warning'}
      size="small"
      sx={{ fontSize: '0.7rem' }}
    />
  );

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <ReportIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Compliance & Reports
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Auto-generated regulatory compliance reports • No manual generation required
          </Typography>
        </Box>
      </Box>

      {/* Info Alert */}
      <Alert severity="info" sx={{ mb: 3 }}>
        Reports are generated automatically by the system. Daily reports at 01:00 AM, 
        Monthly on the 1st, Annual on April 1st. All reports are immutable and include integrity checksums.
      </Alert>

      {/* Tabs */}
      <Card>
        <Tabs
          value={activeTab}
          onChange={(_, newValue) => {
            setActiveTab(newValue);
            setPage(0);
          }}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          <Tab label="Daily Reports" />
          <Tab label="Monthly Reports" />
          <Tab label="Annual (Form IV)" />
          <Tab label="Barcode Compliance" />
          <Tab label="Violations" />
        </Tabs>

        <CardContent>
          {/* Daily Reports Tab */}
          <TabPanel value={activeTab} index={0}>
            {loadingDaily ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : dailyReports?.content?.length === 0 ? (
              <Alert severity="info">No daily reports generated yet. Reports will appear after the first scheduled run at 01:00 AM IST.</Alert>
            ) : (
              <>
                <TableContainer component={Paper} variant="outlined">
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Report Date</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Completeness</TableCell>
                        <TableCell>Generated At</TableCell>
                        <TableCell align="center">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {dailyReports?.content?.map((report: any) => (
                        <TableRow key={report.id} hover>
                          <TableCell>
                            <Typography fontWeight={500}>{formatDate(report.reportDate)}</Typography>
                          </TableCell>
                          <TableCell>
                            <StatusChip status={report.status} />
                          </TableCell>
                          <TableCell>
                            <CompletenessChip completeness={report.dataCompleteness} />
                          </TableCell>
                          <TableCell>{formatDateTime(report.generatedAt)}</TableCell>
                          <TableCell align="center">
                            <Tooltip title="View Details">
                              <IconButton size="small">
                                <ViewIcon />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Download PDF">
                              <IconButton 
                                size="small" 
                                onClick={() => handleDownloadPdf('daily', report.id)}
                              >
                                <PdfIcon color="error" />
                              </IconButton>
                            </Tooltip>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <TablePagination
                  component="div"
                  count={dailyReports?.totalElements || 0}
                  page={page}
                  onPageChange={(_, newPage) => setPage(newPage)}
                  rowsPerPage={rowsPerPage}
                  onRowsPerPageChange={(e) => setRowsPerPage(parseInt(e.target.value, 10))}
                />
              </>
            )}
          </TabPanel>

          {/* Monthly Reports Tab */}
          <TabPanel value={activeTab} index={1}>
            {loadingMonthly ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : monthlyReports?.content?.length === 0 ? (
              <Alert severity="info">No monthly reports generated yet.</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Report Month</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Completeness</TableCell>
                      <TableCell>Generated At</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {monthlyReports?.content?.map((report: any) => (
                      <TableRow key={report.id} hover>
                        <TableCell>
                          <Typography fontWeight={500}>
                            {new Date(report.reportMonth).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' })}
                          </Typography>
                        </TableCell>
                        <TableCell><StatusChip status={report.status} /></TableCell>
                        <TableCell><CompletenessChip completeness={report.dataCompleteness} /></TableCell>
                        <TableCell>{formatDateTime(report.generatedAt)}</TableCell>
                        <TableCell align="center">
                          <Tooltip title="Download PDF">
                            <IconButton size="small" onClick={() => handleDownloadPdf('monthly', report.id)}>
                              <PdfIcon color="error" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>

          {/* Annual Reports (Form IV) Tab */}
          <TabPanel value={activeTab} index={2}>
            {loadingAnnual ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : annualReports?.content?.length === 0 ? (
              <Alert severity="info">No annual reports (Form IV) generated yet. Report will be generated on April 1st.</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Financial Year</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Completeness</TableCell>
                      <TableCell>Generated At</TableCell>
                      <TableCell align="center">Downloads</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {annualReports?.content?.map((report: any) => (
                      <TableRow key={report.id} hover>
                        <TableCell>
                          <Typography fontWeight={500}>FY {report.financialYear}</Typography>
                        </TableCell>
                        <TableCell><StatusChip status={report.status} /></TableCell>
                        <TableCell><CompletenessChip completeness={report.dataCompleteness} /></TableCell>
                        <TableCell>{formatDateTime(report.generatedAt)}</TableCell>
                        <TableCell align="center">
                          <Tooltip title="Download PDF">
                            <IconButton 
                              size="small" 
                              onClick={() => handleDownloadPdf('annual', report.id)}
                              disabled={!report.hasPdf}
                            >
                              <PdfIcon color={report.hasPdf ? 'error' : 'disabled'} />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Download Excel">
                            <IconButton 
                              size="small" 
                              onClick={() => handleDownloadExcel(report.id)}
                              disabled={!report.hasExcel}
                            >
                              <ExcelIcon color={report.hasExcel ? 'success' : 'disabled'} />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>

          {/* Barcode Compliance Tab */}
          <TabPanel value={activeTab} index={3}>
            {loadingBarcode ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : barcodeReports?.content?.length === 0 ? (
              <Alert severity="info">No barcode compliance reports generated yet.</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Report Date</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Generated At</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {barcodeReports?.content?.map((report: any) => (
                      <TableRow key={report.id} hover>
                        <TableCell>{formatDate(report.reportDate)}</TableCell>
                        <TableCell><Chip label={report.reportType} size="small" /></TableCell>
                        <TableCell><StatusChip status={report.status} /></TableCell>
                        <TableCell>{formatDateTime(report.generatedAt)}</TableCell>
                        <TableCell align="center">
                          <Tooltip title="Download PDF">
                            <IconButton size="small" onClick={() => handleDownloadPdf('barcode', report.id)}>
                              <PdfIcon color="error" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>

          {/* Violations Tab */}
          <TabPanel value={activeTab} index={4}>
            {loadingViolations ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : violationReports?.content?.length === 0 ? (
              <Alert severity="success">No violations detected. All operations are compliant.</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Report Date</TableCell>
                      <TableCell>Violations</TableCell>
                      <TableCell>Completeness</TableCell>
                      <TableCell>Generated At</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {violationReports?.content?.map((report: any) => (
                      <TableRow key={report.id} hover>
                        <TableCell>{formatDate(report.reportDate)}</TableCell>
                        <TableCell>
                          <Chip 
                            label={report.violationCount} 
                            color={report.violationCount > 0 ? 'error' : 'success'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell><CompletenessChip completeness={report.dataCompleteness} /></TableCell>
                        <TableCell>{formatDateTime(report.generatedAt)}</TableCell>
                        <TableCell align="center">
                          <Tooltip title="View Details">
                            <IconButton size="small">
                              <ViewIcon />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </TabPanel>
        </CardContent>
      </Card>
    </Box>
  );
}
