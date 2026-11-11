import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development (npm run dev) /api is forwarded to the Spring Boot API.
// In Docker, nginx does the same job (see nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
