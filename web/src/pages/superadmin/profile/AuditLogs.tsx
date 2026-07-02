import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Chip,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Avatar,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef } from '@mui/x-data-grid';
import { getAuditLogs } from '../../../api/superadminProfile';
import type { AuditLog } from '../../../api/superadminProfile';

const ACTION_COLORS: Record<string, 'success' | 'error' | 'warning' | 'info' | 'primary' | 'default'> = {
  LOGIN_SUCCESS: 'success',
  LOGIN_FAILURE: 'error',
  PASSWORD_CHANGED: 'warning',
  PROFILE_UPDATED: 'info',
  PHOTO_UPDATED: 'info',
  SUPERADMIN_CREATED: 'primary',
  SUPERADMIN_UPDATED: 'info',
  SUPERADMIN_DISABLED: 'error',
  SUPERADMIN_ENABLED: 'success',
  PASSWORD_RESET_FORCED: 'warning',
  LOGOUT: 'default',
  CREATED: 'primary',
  SUSPENDED: 'error',
  REACTIVATED: 'success',
  FEATURE_CHANGED: 'info',
  USER_CREATED: 'primary',
  USER_DISABLED: 'error',
  USER_ENABLED: 'success',
  ACCOUNT_LOCKED: 'error',
  ACCOUNT_UNLOCKED: 'success',
};

const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [totalRows, setTotalRows] = useState(0);
  const [actionFilter, setActionFilter] = useState<string>('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');

  const loadLogs = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAuditLogs({
        action: actionFilter || undefined,
        entityType: entityTypeFilter || undefined,
        page,
        size: pageSize
      });
      setLogs(response.content);
      setTotalRows(response.totalElements);
    } catch {
      setError('Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  }, [actionFilter, entityTypeFilter, page, pageSize]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  const columns: GridColDef[] = [
    {
      field: 'actor',
      headerName: 'Actor',
      width: 150,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, overflow: 'hidden' }}>
          <Avatar sx={{ width: 28, height: 28, fontSize: '0.75rem', flexShrink: 0 }}>
            {params.row.actorUsername?.charAt(0) || '?'}
          </Avatar>
          <Typography 
            variant="body2" 
            sx={{ 
              overflow: 'hidden', 
              textOverflow: 'ellipsis', 
              whiteSpace: 'nowrap' 
            }}
            title={`${params.row.actorUsername || 'SYSTEM'} (${params.row.actorRole || ''})`}
          >
            {params.row.actorUsername || 'SYSTEM'}
          </Typography>
        </Box>
      )
    },
    {
      field: 'action',
      headerName: 'Action',
      width: 180,
      renderCell: (params) => (
        <Chip
          label={params.value?.replace(/_/g, ' ')}
          color={ACTION_COLORS[params.value] || 'default'}
          size="small"
          sx={{ fontWeight: 500 }}
        />
      )
    },
    {
      field: 'entityType',
      headerName: 'Entity',
      width: 100,
      renderCell: (params) => (
        <Chip label={params.value} variant="outlined" size="small" />
      )
    },
    {
      field: 'oldValue',
      headerName: 'Old Value',
      flex: 1,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ 
          color: 'text.secondary',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}>
          {params.value || '-'}
        </Typography>
      )
    },
    {
      field: 'newValue',
      headerName: 'New Value',
      flex: 1,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ 
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}>
          {params.value || '-'}
        </Typography>
      )
    },
    {
      field: 'createdAt',
      headerName: 'Timestamp',
      width: 170,
      valueFormatter: (value) => value ? new Date(value).toLocaleString() : ''
    },
    {
      field: 'notes',
      headerName: 'Notes',
      flex: 0.8,
      renderCell: (params) => (
        <Typography variant="body2" color="text.secondary" sx={{ 
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}>
          {params.value || '-'}
        </Typography>
      )
    }
  ];

  const actionTypes = [
    'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT',
    'PASSWORD_CHANGED', 'PASSWORD_RESET_FORCED',
    'PROFILE_UPDATED', 'PHOTO_UPDATED',
    'SUPERADMIN_CREATED', 'SUPERADMIN_UPDATED', 'SUPERADMIN_DISABLED', 'SUPERADMIN_ENABLED',
    'USER_CREATED', 'USER_DISABLED', 'USER_ENABLED',
    'CREATED', 'SUSPENDED', 'REACTIVATED', 'FEATURE_CHANGED',
    'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED'
  ];

  const entityTypes = ['USER', 'FACILITY', 'CONFIG'];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Audit Logs (Read-Only)</Typography>
        <Alert severity="info" sx={{ py: 0 }}>
          Audit logs are immutable and cannot be modified or deleted.
        </Alert>
      </Box>

      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Action Type</InputLabel>
          <Select
            value={actionFilter}
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            label="Action Type"
          >
            <MenuItem value="">All Actions</MenuItem>
            {actionTypes.map((action) => (
              <MenuItem key={action} value={action}>{action.replace(/_/g, ' ')}</MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Entity Type</InputLabel>
          <Select
            value={entityTypeFilter}
            onChange={(e) => { setEntityTypeFilter(e.target.value); setPage(0); }}
            label="Entity Type"
          >
            <MenuItem value="">All Entities</MenuItem>
            {entityTypes.map((type) => (
              <MenuItem key={type} value={type}>{type}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ width: '100%' }}>
        <DataGrid
          rows={logs}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={totalRows}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => setPage(m.page)}
          pageSizeOptions={[20]}
          disableRowSelectionOnClick
          autoHeight
          rowHeight={52}
          getRowId={(row) => row.id}
          sx={{ 
            bgcolor: 'background.paper', 
            borderRadius: 2,
            width: '100%',
            '& .MuiDataGrid-columnHeaders': {
              bgcolor: 'background.default',
            },
            '& .MuiDataGrid-cell': {
              display: 'flex',
              alignItems: 'center',
              py: 1,
            },
            '& .MuiDataGrid-row': {
              cursor: 'default'
            }
          }}
        />
      </Box>
    </Box>
  );
};

export default AuditLogs;
