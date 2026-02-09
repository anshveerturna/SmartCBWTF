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

// Light theme palette - matches Android app design
// From androidapp colors.xml: Teal primary, clean whites, soft grays
// Light theme palette - Enterprise Clean
const lightPalette = {
  mode: 'light' as const,
  primary: {
    main: '#0F766E',     // Teal 700 - Deep, professional teal
    light: '#14B8A6',    // Teal 500
    dark: '#0D5E56',     // Teal 800
    contrastText: '#FFFFFF',
  },
  secondary: {
    main: '#475569',     // Slate 600 - Professional secondary
    light: '#94A3B8',    // Slate 400
    dark: '#334155',     // Slate 700
    contrastText: '#FFFFFF',
  },
  ...semanticColors,
  background: {
    default: '#F1F5F9',  // Slate 100 - Crisp light gray background
    paper: '#FFFFFF',    // Pure white surfaces
  },
  text: {
    primary: '#0F172A',  // Slate 900 - High contrast text
    secondary: '#64748B', // Slate 500 - Subtler secondary text
  },
  divider: '#E2E8F0',    // Slate 200 - Subtle dividers
  waste: wasteColors,
};

// Light theme component overrides
const lightComponents = {
  MuiCssBaseline: {
    styleOverrides: {
      body: {
        scrollbarColor: '#CBD5E1 #F1F5F9',
        '&::-webkit-scrollbar, & *::-webkit-scrollbar': {
          width: '8px',
          height: '8px',
        },
        '&::-webkit-scrollbar-thumb, & *::-webkit-scrollbar-thumb': {
          borderRadius: 8,
          backgroundColor: '#CBD5E1',
          minHeight: 24,
        },
        '&::-webkit-scrollbar-track, & *::-webkit-scrollbar-track': {
          backgroundColor: '#F1F5F9',
        },
      },
    },
  },
  MuiButton: {
    styleOverrides: {
      root: {
        borderRadius: 6, // Tighter radius
        textTransform: 'none',
        fontWeight: 600,
        boxShadow: 'none',
        '&:hover': {
          boxShadow: 'none',
        },
      },
      containedPrimary: {
        backgroundColor: '#0F766E',
        '&:hover': {
          backgroundColor: '#0D5E56',
        },
      },
      outlined: {
        borderWidth: '1px !important', // Ensure visible border
      },
    },
  },
  MuiCard: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: '#FFFFFF',
        border: '1px solid #E2E8F0', // Crisp border
        boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', // Very subtle shadow
        borderRadius: 8,
      },
    },
  },
  MuiPaper: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
      },
      elevation1: {
        boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
      },
    },
  },
  MuiAppBar: {
    styleOverrides: {
      root: {
        backgroundColor: '#FFFFFF',
        color: '#0F172A',
        borderBottom: '1px solid #E2E8F0',
        boxShadow: 'none',
      },
    },
  },
  MuiDrawer: {
    styleOverrides: {
      paper: {
        backgroundColor: '#FFFFFF',
        borderRight: '1px solid #E2E8F0',
      },
    },
  },
  MuiTableCell: {
    styleOverrides: {
      root: {
        borderBottom: '1px solid #E2E8F0',
        padding: '12px 16px',
      },
      head: {
        backgroundColor: '#F8FAFC',
        color: '#475569',
        fontWeight: 600,
        textTransform: 'uppercase',
        fontSize: '0.75rem',
        letterSpacing: '0.05em',
      },
    },
  },
  MuiTableRow: {
    styleOverrides: {
      root: {
        '&:hover': {
          backgroundColor: '#F8FAFC',
        },
      },
    },
  },
  MuiChip: {
    styleOverrides: {
      root: {
        borderRadius: 4,
        fontWeight: 500,
      },
      filled: {
        border: '1px solid transparent',
      },
      outlined: {
        border: '1px solid #E2E8F0',
        backgroundColor: '#FFFFFF',
      }
    },
  },
  MuiTextField: {
    styleOverrides: {
      root: {
        '& .MuiOutlinedInput-root': {
          backgroundColor: '#FFFFFF',
          '& fieldset': {
            borderColor: '#E2E8F0',
            borderWidth: 1,
          },
          '&:hover fieldset': {
            borderColor: '#CBD5E1',
          },
          '&.Mui-focused fieldset': {
            borderColor: '#0F766E',
            borderWidth: 1,
            boxShadow: '0 0 0 1px #0F766E', // Focus ring
          },
        },
      },
    },
  },
  MuiInputLabel: {
    styleOverrides: {
      root: {
        color: '#64748B',
        '&.Mui-focused': {
          color: '#0F766E',
        },
      },
    },
  },
  MuiDialog: {
    styleOverrides: {
      paper: {
        borderRadius: 8,
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
        border: '1px solid #E2E8F0',
      },
    },
  },
  MuiListItemButton: {
    styleOverrides: {
      root: {
        borderRadius: 6,
        margin: '2px 8px',
        '&.Mui-selected': {
          backgroundColor: alpha('#0F766E', 0.1),
          color: '#0F766E',
          '&:hover': {
            backgroundColor: alpha('#0F766E', 0.15),
          },
          '& .MuiListItemIcon-root': {
            color: '#0F766E',
          },
        },
        '&:hover': {
          backgroundColor: '#F1F5F9',
        },
      },
    },
  },
  MuiListItemIcon: {
    styleOverrides: {
      root: {
        minWidth: 36,
        color: '#64748B',
      },
    },
  },
  MuiTooltip: {
    styleOverrides: {
      tooltip: {
        backgroundColor: '#1E293B',
        color: '#FFFFFF',
        borderRadius: 4,
        fontSize: '0.75rem',
      },
    },
  },
};

export const lightTheme = createTheme({
  palette: lightPalette,
  typography,
  shape,
  components: lightComponents,
});
