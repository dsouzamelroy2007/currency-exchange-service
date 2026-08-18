import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client references the Node `global` object; Vite's browser build doesn't polyfill it.
    global: 'globalThis',
  },
  server: {
    proxy: {
      '/btc': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
