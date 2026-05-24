import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/eventus/',
  build: {
    outDir: '../resources/static/eventus',
    emptyOutDir: true,
  }
})
