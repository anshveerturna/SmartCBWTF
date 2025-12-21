import React, { useState } from 'react';
import { Box, Tabs, Tab, Typography, Paper } from '@mui/material';
import {
  Person as PersonIcon,
  Group as GroupIcon,
  History as HistoryIcon,
} from '@mui/icons-material';
import MyProfile from './MyProfile';
import SuperAdminUsers from './SuperAdminUsers';
import AuditLogs from './AuditLogs';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div hidden={value !== index} role="tabpanel">
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const ProfilePage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        SuperAdmin Identity Center
      </Typography>

      <Paper sx={{ borderRadius: 2 }}>
        <Tabs
          value={tabValue}
          onChange={(_, newValue) => setTabValue(newValue)}
          variant="fullWidth"
          sx={{
            borderBottom: 1,
            borderColor: 'divider',
            '& .MuiTab-root': {
              py: 2,
              fontWeight: 600,
            },
          }}
        >
          <Tab icon={<PersonIcon />} iconPosition="start" label="My Profile" />
          <Tab icon={<GroupIcon />} iconPosition="start" label="SuperAdmin Users" />
          <Tab icon={<HistoryIcon />} iconPosition="start" label="Audit Logs" />
        </Tabs>

        <Box sx={{ p: 3 }}>
          <TabPanel value={tabValue} index={0}>
            <MyProfile />
          </TabPanel>
          <TabPanel value={tabValue} index={1}>
            <SuperAdminUsers />
          </TabPanel>
          <TabPanel value={tabValue} index={2}>
            <AuditLogs />
          </TabPanel>
        </Box>
      </Paper>
    </Box>
  );
};

export default ProfilePage;
