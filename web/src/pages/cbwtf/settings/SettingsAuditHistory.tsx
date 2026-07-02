import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  TablePagination,
  Chip,
  Tooltip
} from '@mui/material';
import { getSettingsAuditHistory, type SettingsAuditDTO } from '../../../api/cbwtf';
import dayjs from 'dayjs';

const SettingsAuditHistory = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  const { data, isLoading } = useQuery({
    queryKey: ['settingsAudit', page, rowsPerPage],
    queryFn: () => getSettingsAuditHistory(undefined, page, rowsPerPage)
  });

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  const logs = data?.content || [];
  const total = data?.totalElements || 0;

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Configuration History
      </Typography>
      
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Timestamp</TableCell>
              <TableCell>Section</TableCell>
              <TableCell>Setting</TableCell>
              <TableCell>Old Value</TableCell>
              <TableCell>New Value</TableCell>
              <TableCell>Changed By</TableCell>
              <TableCell>Source IP</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {logs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No changes recorded yet
                </TableCell>
              </TableRow>
            ) : (
              logs.map((log: SettingsAuditDTO) => (
                <TableRow key={log.id}>
                  <TableCell>
                    {dayjs(log.changedAt).format('DD MMM YYYY, HH:mm')}
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={log.section} variant="outlined" />
                  </TableCell>
                  <TableCell>{log.settingKey}</TableCell>
                  <TableCell sx={{ color: 'error.main', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    <Tooltip title={log.oldValue || 'null'}>
                        <span>{log.oldValue || '-'}</span>
                    </Tooltip>
                  </TableCell>
                  <TableCell sx={{ color: 'success.main', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                     <Tooltip title={log.newValue || 'null'}>
                        <span>{log.newValue || '-'}</span>
                    </Tooltip>
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
        rowsPerPageOptions={[10, 20, 50]}
        component="div"
        count={total}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
      />
    </Box>
  );
};

export default SettingsAuditHistory;
