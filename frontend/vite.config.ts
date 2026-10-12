import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import { env } from 'process';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],

  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },

  server: {
    proxy: {
      "/delivery-api": {
        target:
          env.DELIVERY_SERVICE_URL ??
          "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/delivery-api/, ""),
      },

      "/fleet-api": {
        target:
          env.FLEET_SERVICE_URL ??
          "http://localhost:8082",
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/fleet-api/, ""),
      },

      "/dispatch-api": {
        target:
          env.DISPATCH_SERVICE_URL ??
          "http://localhost:8083",
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/dispatch-api/, ""),
      },

      "/analytics-api": {
        target:
          env.ANALYTICS_SERVICE_URL ??
          "http://localhost:8085",
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/analytics-api/, ""),
      },

      "/agent-api": {
        target:
          env.AGENT_SERVICE_URL ??
          "http://localhost:8086",
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/agent-api/, ""),
      },
    },
    port: 3000
  },
})
