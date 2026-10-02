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
        index: resolve(__dirname, 'index.html'),
        normal: resolve(__dirname, 'normal.html'),
        edit: resolve(__dirname, 'edit.html'),
        edicion: resolve(__dirname, 'edicion.html'),
        categories: resolve(__dirname, 'categories.html'),
        categorias: resolve(__dirname, 'categorias.html'),
        db: resolve(__dirname, 'db.html'),
        config: resolve(__dirname, 'config.html'),
        configuracion: resolve(__dirname, 'configuracion.html'),
        radar: resolve(__dirname, 'radar.html'),
        album: resolve(__dirname, 'album.html'),
        coleccion: resolve(__dirname, 'coleccion.html'),
        migrate: resolve(__dirname, 'migrate.html'),
      },
    },
  },
  server: {
    port: 3000,
  },
});
