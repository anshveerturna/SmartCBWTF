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
const lightPalette = {
  mode: 'light' as const,
  primary: {
    main: '#00695C',     // Teal 800 - Android primary_color
    light: '#26A69A',    // Teal 400 - secondary_color
    dark: '#004D40',     // Teal 900 - primary_color_variant
    contrastText: '#FFFFFF',
  },
  secondary: {
    main: '#00796B',     // verifyAccent
    light: '#4DB6AC',    // pickupCardGradientEnd
    dark: '#00503D',     // primary_color_dark
    contrastText: '#FFFFFF',
  },
  ...semanticColors,
  background: {
    default: '#F5F7FA',  // Android background_color
    paper: '#FFFFFF',    // Android surface_color
  },
  text: {
    primary: '#102027',  // Android text_primary
    secondary: '#546E7A', // Android text_secondary
  },
  divider: '#E5E7EB',    // Android divider_color
  waste: wasteColors,
};

// Light theme component overrides
const lightComponents = {
  MuiButton: {
    styleOverrides: {
      root: {
        borderRadius: 8,
        padding: '10px 20px',
        boxShadow: 'none',
        '&:hover': {
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        },
      },
      contained: {
        background: 'linear-gradient(135deg, #00695C 0%, #004D40 100%)',
        '&:hover': {
          background: 'linear-gradient(135deg, #00796B 0%, #00695C 100%)',
        },
      },
      outlined: {
        borderColor: '#00695C',
        color: '#00695C',
        '&:hover': {
          backgroundColor: alpha('#00695C', 0.04),
          borderColor: '#004D40',
        },
      },
    },
  },
  MuiCard: {
    styleOverrides: {
      root: {
        backgroundImage: 'none',
        backgroundColor: '#FFFFFF',
        border: `1px solid ${alpha('#000', 0.08)}`,
        boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)',
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
        backgroundColor: '#FFFFFF',
        borderBottom: '1px solid #E5E7EB',
        boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
        color: '#102027',
      },
    },
  },
  MuiDrawer: {
    styleOverrides: {
      paper: {
        backgroundColor: '#F5F7FA',
        borderRight: '1px solid #E5E7EB',
      },
    },
  },
  MuiTableCell: {
    styleOverrides: {
      root: {
        borderBottom: '1px solid #E5E7EB',
      },
      head: {
        backgroundColor: '#F8FAFC',
        fontWeight: 600,
        color: '#374151',
      },
    },
  },
  MuiTableRow: {
    styleOverrides: {
      root: {
        '&:hover': {
          backgroundColor: alpha('#00695C', 0.04),
        },
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
          backgroundColor: '#FFFFFF',
          '& fieldset': {
            borderColor: '#E5E7EB',
          },
          '&:hover fieldset': {
            borderColor: '#00695C',
          },
          '&.Mui-focused fieldset': {
            borderColor: '#00695C',
          },
        },
      },
    },
  },
  MuiDialog: {
    styleOverrides: {
      paper: {
        backgroundImage: 'none',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04)',
      },
    },
  },
  MuiTooltip: {
    styleOverrides: {
      tooltip: {
        backgroundColor: '#374151',
        color: '#FFFFFF',
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
  MuiDataGrid: {
    styleOverrides: {
      root: {
        border: '1px solid #E5E7EB',
        backgroundColor: '#FFFFFF',
        '& .MuiDataGrid-columnHeaders': {
          backgroundColor: '#F8FAFC',
          borderBottom: '1px solid #E5E7EB',
        },
        '& .MuiDataGrid-cell': {
          borderBottom: '1px solid #F3F4F6',
        },
        '& .MuiDataGrid-row:hover': {
          backgroundColor: alpha('#00695C', 0.04),
        },
        '& .MuiDataGrid-row.Mui-selected': {
          backgroundColor: '#E0F2F1', // mint_glow from Android
        },
      },
    },
  },
  MuiListItemButton: {
    styleOverrides: {
      root: {
        '&:hover': {
          backgroundColor: alpha('#00695C', 0.08),
        },
        '&.Mui-selected': {
          backgroundColor: '#E0F2F1',
          '&:hover': {
            backgroundColor: '#B2DFDB',
          },
        },
      },
    },
  },
  MuiTabs: {
    styleOverrides: {
      indicator: {
        backgroundColor: '#00695C',
      },
    },
  },
  MuiTab: {
    styleOverrides: {
      root: {
        '&.Mui-selected': {
          color: '#00695C',
        },
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
