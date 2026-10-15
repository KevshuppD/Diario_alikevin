const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// Sube la foto de una carta escrita desde la web. Cada foto recibe un public_id nuevo (no sobrescribe).
module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, error: 'Method not allowed' });
  }

  if (!process.env.CLOUDINARY_CLOUD_NAME || !process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
    console.error('❌ Faltan variables de entorno de Cloudinary');
    return res.status(500).json({ success: false, error: 'Configuración de Cloudinary incompleta en el servidor' });
  }

  const { imageBase64 } = req.body || {};
  if (!imageBase64 || !String(imageBase64).startsWith('data:image/')) {
    return res.status(400).json({ success: false, error: 'Falta imageBase64 (data URL de imagen)' });
  }

  try {
    const result = await cloudinary.uploader.upload(imageBase64, {
      folder: 'letters',
      resource_type: 'image',
    });
    return res.status(200).json({ success: true, url: result.secure_url, publicId: result.public_id });
  } catch (error) {
    console.error('❌ Error Cloudinary (carta):', error);
    return res.status(500).json({ success: false, error: error.message || 'Error desconocido al subir imagen' });
  }
};
