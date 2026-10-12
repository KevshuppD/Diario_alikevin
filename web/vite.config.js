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
      output: {
        // Firebase en su propio archivo: su hash no cambia entre despliegues y el navegador lo conserva en caché
        manualChunks(id) {
          if (id.includes('node_modules/firebase') || id.includes('node_modules/@firebase')) return 'firebase';
        },
      },
    },
  },
  server: {
    port: 3000,
  },
});
