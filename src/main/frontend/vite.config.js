import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  base: '/p2proto/',  // Adjust this to match your context path
  plugins: [react()]
})