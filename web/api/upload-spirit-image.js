const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

module.exports = async function handler(req, res) {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, error: 'Method not allowed' });
  }

  // Verifica que las variables de entorno estén cargadas
  if (!process.env.CLOUDINARY_CLOUD_NAME || !process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
    console.error('❌ Faltan variables de entorno de Cloudinary');
    return res.status(500).json({ success: false, error: 'Configuración de Cloudinary incompleta en el servidor' });
  }

  const { spiritId, imageBase64 } = req.body || {};

  if (!spiritId || !imageBase64) {
    return res.status(400).json({ success: false, error: 'Faltan spiritId o imageBase64' });
  }

  const twoDigitId = String(spiritId).padStart(2, '0');
  const publicId = `spirits/ic_spirit_${twoDigitId}`;

  try {
    console.log(`📤 Subiendo espíritu ${twoDigitId} a Cloudinary...`);

    const result = await cloudinary.uploader.upload(imageBase64, {
      public_id: publicId,
      overwrite: true,
      resource_type: 'image',
    });

    console.log(`✅ Subida exitosa: ${result.secure_url}`);
    return res.status(200).json({ success: true, url: result.secure_url, publicId });
  } catch (error) {
    console.error('❌ Error Cloudinary:', error);
    return res.status(500).json({ success: false, error: error.message || 'Error desconocido al subir imagen' });
  }
};
