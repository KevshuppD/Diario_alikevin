const express = require('express');
const cloudinary = require('cloudinary').v2;
const path = require('path');
require('dotenv').config();

const app = express();
app.use(express.json({ limit: '20mb' }));

// Configura Cloudinary (usa .env en local, variables de entorno en Vercel)
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// Sirve los archivos estáticos de la carpeta web/
app.use(express.static(__dirname));

// Redirige la raíz al index.html
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

// API: Subir imagen de espíritu a Cloudinary
app.post('/api/upload-spirit-image', async (req, res) => {
  const { spiritId, imageBase64 } = req.body;

  if (!spiritId || !imageBase64) {
    return res.status(400).json({ success: false, error: 'Faltan spiritId o imageBase64' });
  }

  const twoDigitId = String(spiritId).padStart(2, '0');
  const publicId = `spirits/ic_spirit_${twoDigitId}`;

  try {
    const result = await cloudinary.uploader.upload(imageBase64, {
      public_id: publicId,
      overwrite: true,
    });

    console.log(`✅ Imagen subida: ${result.secure_url}`);
    return res.json({ success: true, url: result.secure_url, publicId });
  } catch (error) {
    console.error('❌ Error al subir a Cloudinary:', error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
});

// Inicia el servidor local
const PORTS = [8000, 8080, 8008, 8888];
const tryListen = (ports) => {
  if (ports.length === 0) {
    console.error('❌ No se pudo iniciar el servidor en ningún puerto disponible.');
    process.exit(1);
  }
  const port = ports[0];
  app.listen(port)
    .on('listening', () => {
      console.log(`\n🚀 Servidor iniciado en http://localhost:${port}`);
      console.log('   Abre esa URL en tu navegador para usar la app.\n');
    })
    .on('error', () => {
      console.log(`⚠️  Puerto ${port} ocupado, probando el siguiente...`);
      tryListen(ports.slice(1));
    });
};

tryListen(PORTS);
