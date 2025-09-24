import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/micro/ui',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: 'out/ui',
  },
  server: {
    proxy: {
      '/micro/events': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      },
      '/micro/reset': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
