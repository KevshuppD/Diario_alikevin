import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: '.',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    minify: 'terser',
    cssMinify: true,
    rollupOptions: {
      input: {
        normal: resolve(__dirname, 'normal.html'),
        edit: resolve(__dirname, 'edit.html'),
        db: resolve(__dirname, 'db.html'),
        config: resolve(__dirname, 'config.html'),
        index: resolve(__dirname, 'index.html'),
      },
    },
  },
  server: {
    port: 3000,
  },
});
