import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    clearMocks: true,
    include: ['src/**/*.test.{ts,tsx}'],
  },
    server: {
        watch: {
            ignored: ['**/playwright-report/**', '**/test-results/**'],
        },
        proxy: {
      '/api': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
      '/oauth2/authorization': 'http://localhost:8080',
      '/login/oauth2': 'http://localhost:8080',
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
