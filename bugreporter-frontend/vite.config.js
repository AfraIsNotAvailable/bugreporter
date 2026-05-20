import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const isDemo = process.env.VITE_DEMO_MODE === 'true'
  return {
    plugins: [react(), tailwindcss()],
    base: isDemo ? '/bugreporter/app/' : '/',
  }
})
