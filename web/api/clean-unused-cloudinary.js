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

  if (!process.env.CLOUDINARY_CLOUD_NAME || !process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
    console.error('❌ Faltan variables de entorno de Cloudinary');
    return res.status(500).json({ success: false, error: 'Configuración de Cloudinary incompleta en el servidor' });
  }

  const { activeUrls } = req.body || {};

  if (!Array.isArray(activeUrls)) {
    return res.status(400).json({ success: false, error: 'activeUrls debe ser un arreglo' });
  }

  try {
    const resourcesResult = await cloudinary.api.resources({
      type: 'upload',
      prefix: 'spirits/',
      max_results: 500
    });

    const activeSet = new Set(activeUrls);
    const toDelete = [];

    resourcesResult.resources.forEach(resItem => {
      const isUsedUrl = activeSet.has(resItem.secure_url) || activeSet.has(resItem.url);
      const isUsedPublicId = activeSet.has(resItem.public_id);
      
      if (!isUsedUrl && !isUsedPublicId) {
        toDelete.push(resItem.public_id);
      }
    });

    if (toDelete.length === 0) {
      return res.status(200).json({ success: true, deletedCount: 0, message: 'No hay imágenes huérfanas o sin usar en Cloudinary.' });
    }

    const deleteResult = await cloudinary.api.delete_resources(toDelete);
    return res.status(200).json({
      success: true,
      deletedCount: toDelete.length,
      deletedPublicIds: toDelete,
      result: deleteResult
    });
  } catch (error) {
    console.error('❌ Error Cloudinary clean:', error);
    return res.status(500).json({ success: false, error: error.message || 'Error al limpiar Cloudinary' });
  }
};
