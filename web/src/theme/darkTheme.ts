import { createTheme, alpha } from '@mui/material/styles';
import { typography, shape, wasteColors, semanticColors } from './baseTheme';

// Declare custom palette extensions
declare module '@mui/material/styles' {
  interface Palette {
    waste: typeof wasteColors;
  }
  interface PaletteOptions {
    waste?: typeof wasteColors;
  }
}

// Dark theme palette - matches existing design
const darkPalette = {
  mode: 'dark' as const,
  primary: {
    main: '#6366F1', // Indigo
    light: '#818CF8',
    dark: '#4F46E5',
    contrastText: '#FFFFFF',
  },
  secondary: {
    main: '#10B981', // Emerald
    light: '#34D399',
    dark: '#059669',
    contrastText: '#FFFFFF',
  },
  ...semanticColors,
  background: {
    default: '#0F172A', // Slate 900
    paper: '#1E293B',   // Slate 800
  },
  text: {
    primary: '#F1F5F9',
    secondary: '#94A3B8',
  },
  divider: alpha('#94A3B8', 0.12),
  waste: wasteColors,
};

// Dark theme component overrides
const darkComponents = {
  MuiButton: {
    styleOverrides: {
      root: {
        borderRadius: 8,
        padding: '10px 20px',
        boxShadow: 'none',
        '&:hover': {
          boxShadow: 'none',
        },
      },
      contained: {
        background: 'linear-gradient(135deg, #6366F1 0%, #4F46E5 100%)',
        '&:hover': {
          background: 'linear-gradient(135deg, #818CF8 0%, #6366F1 100%)',
        },
      },
    },
  },
  MuiCard: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: alpha('#1E293B', 0.8),
        backdropFilter: 'blur(20px)',
        border: `1px solid ${alpha('#94A3B8', 0.1)}`,
        boxShadow: `0 4px 6px -1px ${alpha('#000', 0.1)}, 0 2px 4px -2px ${alpha('#000', 0.1)}`,
      },
    },
  },
  MuiPaper: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
      },
    },
  },
  MuiAppBar: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: alpha('#1E293B', 0.8),
        backdropFilter: 'blur(20px)',
        borderBottom: `1px solid ${alpha('#94A3B8', 0.1)}`,
        boxShadow: 'none',
      },
    },
  },
  MuiDrawer: {
    styleOverrides: {
      paper: {
        backgroundColor: '#0F172A',
        borderRight: `1px solid ${alpha('#94A3B8', 0.1)}`,
      },
    },
  },
  MuiTableCell: {
    styleOverrides: {
      root: {
        borderBottom: `1px solid ${alpha('#94A3B8', 0.1)}`,
      },
    },
  },
  MuiChip: {
    styleOverrides: {
      root: {
        borderRadius: 6,
      },
    },
  },
  MuiTextField: {
    styleOverrides: {
      root: {
        '& .MuiOutlinedInput-root': {
          '& fieldset': {
            borderColor: alpha('#94A3B8', 0.2),
          },
          '&:hover fieldset': {
            borderColor: alpha('#94A3B8', 0.4),
          },
        },
      },
    },
  },
  MuiDialog: {
    styleOverrides: {
      paper: {
        backgroundImage: 'none',
        backgroundColor: '#1E293B',
      },
    },
  },
  MuiTooltip: {
    styleOverrides: {
      tooltip: {
        backgroundColor: '#334155',
        color: '#F1F5F9',
        fontSize: '0.75rem',
      },
    },
  },
  MuiAlert: {
    styleOverrides: {
      root: {
        borderRadius: 8,
      },
    },
  },
};

export const darkTheme = createTheme({
  palette: darkPalette,
  typography,
  shape,
  components: darkComponents,
});
