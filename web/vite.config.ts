import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    reportCompressedSize: false,
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('monaco-editor') || id.includes('@monaco-editor')) return 'monaco-editor';
          if (id.includes('@mui/icons-material')) return 'mui-icons';
          if (id.includes('@mui/x-data-grid') || id.includes('@mui/x-date-pickers')) return 'mui-x';
          if (id.includes('@mui/')) return 'mui';
          if (id.includes('@tanstack/react-query')) return 'query';
          if (id.includes('country-state-city')) return 'geography';
          if (id.includes('react-hook-form') || id.includes('@hookform/') || id.includes('zod')) return 'forms';
          if (id.includes('date-fns') || id.includes('dayjs')) return 'dates';
          if (id.includes('leaflet')) return 'maps';
          if (id.includes('recharts')) return 'charts';
          if (id.includes('jspdf') || id.includes('qrcode')) return 'document-tools';
          if (id.includes('axios') || id.includes('uuid')) return 'api-vendor';
          return 'vendor';
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    }
  }
})
