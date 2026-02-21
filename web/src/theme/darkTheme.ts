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
// Dark theme palette - Enterprise Clean
const darkPalette = {
  mode: 'dark' as const,
  primary: {
    main: '#818CF8',     // Indigo 400 - Readable on dark
    light: '#A5B4FC',    // Indigo 300
    dark: '#6366F1',     // Indigo 500
    contrastText: '#FFFFFF',
  },
  secondary: {
    main: '#94A3B8',     // Slate 400
    light: '#CBD5E1',    // Slate 300
    dark: '#64748B',     // Slate 500
    contrastText: '#0F172A',
  },
  ...semanticColors,
  background: {
    default: '#0F172A',  // Slate 900 - Deep background
    paper: '#1E293B',    // Slate 800 - Lighter surfaces
  },
  text: {
    primary: '#F8FAFC',  // Slate 50
    secondary: '#94A3B8', // Slate 400
  },
  divider: '#334155',    // Slate 700
  waste: wasteColors,
};

// Dark theme component overrides
const darkComponents = {
  MuiCssBaseline: {
    styleOverrides: {
      body: {
        scrollbarColor: '#475569 #0F172A',
        '&::-webkit-scrollbar, & *::-webkit-scrollbar': {
          width: '8px',
          height: '8px',
        },
        '&::-webkit-scrollbar-thumb, & *::-webkit-scrollbar-thumb': {
          borderRadius: 8,
          backgroundColor: '#475569',
          minHeight: 24,
        },
        '&::-webkit-scrollbar-track, & *::-webkit-scrollbar-track': {
          backgroundColor: '#0F172A',
        },
      },
    },
  },
  MuiButton: {
    styleOverrides: {
      root: {
        borderRadius: 6,
        textTransform: 'none',
        fontWeight: 600,
        boxShadow: 'none',
        '&:hover': {
          boxShadow: 'none',
        },
      },
      containedPrimary: {
        backgroundColor: '#6366F1',
        color: '#FFFFFF',
        '&:hover': {
          backgroundColor: '#4F46E5',
        },
      },
      outlined: {
        borderWidth: '1px !important',
      },
    },
  },
  MuiCard: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: '#1E293B',
        border: '1px solid #334155',
        boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.2)',
        borderRadius: 8,
      },
    },
  },
  MuiPaper: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: '#1E293B',
      },
      elevation1: {
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
      },
    },
  },
  MuiAppBar: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: '#1E293B', // Match paper
        borderBottom: '1px solid #334155',
        boxShadow: 'none',
      },
    },
  },
  MuiDrawer: {
    styleOverrides: {
      paper: {
        backgroundColor: '#0F172A', // Darker than main content
        borderRight: '1px solid #334155',
      },
    },
  },
  MuiTableCell: {
    styleOverrides: {
      root: {
        borderBottom: '1px solid #334155',
        padding: '12px 16px',
      },
      head: {
        backgroundColor: '#1E293B',
        color: '#CBD5E1',
        fontWeight: 600,
        textTransform: 'uppercase',
        fontSize: '0.75rem',
        letterSpacing: '0.05em',
        borderBottom: '1px solid #334155',
      },
    },
  },
  MuiTableRow: {
    styleOverrides: {
      root: {
        '&:hover': {
          backgroundColor: '#334155', // Slate 700
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
      filledDefault: {
        border: '1px solid transparent',
        backgroundColor: '#334155',
      },
      outlined: {
        border: '1px solid #475569',
      }
    },
  },
  MuiTextField: {
    styleOverrides: {
      root: {
        '& .MuiOutlinedInput-root': {
          backgroundColor: '#0F172A', // Darker input bg
          '& fieldset': {
            borderColor: '#334155',
            borderWidth: 1,
          },
          '&:hover fieldset': {
            borderColor: '#475569',
          },
          '&.Mui-focused fieldset': {
            borderColor: '#818CF8',
            borderWidth: 2,
          },
        },
      },
    },
  },
  MuiInputLabel: {
    styleOverrides: {
      root: {
        color: '#94A3B8',
        '&.Mui-focused': {
          color: '#818CF8',
        },
      },
    },
  },
  MuiDialog: {
    styleOverrides: {
      paper: {
        backgroundColor: '#1E293B',
        border: '1px solid #334155',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
      },
    },
  },
  MuiListItemButton: {
    styleOverrides: {
      root: {
        borderRadius: 6,
        margin: '2px 8px',
        '&.Mui-selected': {
          backgroundColor: alpha('#818CF8', 0.15),
          color: '#818CF8',
          '&:hover': {
            backgroundColor: alpha('#818CF8', 0.25),
          },
          '& .MuiListItemIcon-root': {
            color: '#818CF8',
          },
        },
        '&:hover': {
          backgroundColor: '#334155',
        },
      },
    },
  },
  MuiListItemIcon: {
    styleOverrides: {
      root: {
        minWidth: 36,
        color: '#94A3B8',
      },
    },
  },
  MuiTooltip: {
    styleOverrides: {
      tooltip: {
        backgroundColor: '#0F172A',
        border: '1px solid #334155',
        color: '#F8FAFC',
        fontSize: '0.75rem',
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
