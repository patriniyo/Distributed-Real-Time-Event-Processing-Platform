import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../dashboard-service/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8101',
      '/admin': 'http://localhost:8101',
      '/ws': {
        target: 'ws://localhost:8101',
        ws: true,
      },
    },
  },
});
