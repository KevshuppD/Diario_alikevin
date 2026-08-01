const cloudinary = require('cloudinary').v2;

// Configura Cloudinary usando variables de entorno de Vercel
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

module.exports = async function handler(req, res) {
  // Permite CORS para el frontend
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, error: 'Method not allowed' });
  }

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

    return res.status(200).json({
      success: true,
      url: result.secure_url,
      publicId: publicId,
    });
  } catch (error) {
    console.error('Error al subir a Cloudinary:', error);
    return res.status(500).json({ success: false, error: error.message });
  }
};
