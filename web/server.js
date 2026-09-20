const express = require('express');
const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');
const cloudinary = require('cloudinary').v2;
const path = require('path');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

app.use(express.json({ limit: '20mb' }));

const isProduction = process.env.NODE_ENV === 'production';
const distDir = isProduction && require('fs').existsSync(path.join(__dirname, 'dist'))
  ? path.join(__dirname, 'dist')
  : __dirname;

console.log(`📌 Modo de ejecución: ${isProduction ? 'PRODUCTION (Build / Release)' : 'DEVELOPMENT (Dev / Debug)'}`);
console.log(`📁 Sirviendo archivos desde: ${distDir}`);

// Configura Cloudinary
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// Configuración de WebSockets para sincronización en tiempo real
const wss = new WebSocketServer({ server, path: '/ws' });

function broadcast(data, senderWs = null) {
  const payload = typeof data === 'string' ? data : JSON.stringify(data);
  wss.clients.forEach(client => {
    if (client !== senderWs && client.readyState === WebSocket.OPEN) {
      try {
        client.send(payload);
      } catch (err) {
        console.error('Error broadcasting to client:', err);
      }
    }
  });
}

wss.on('connection', (ws, req) => {
  const clientIp = req.socket.remoteAddress;
  console.log(`🔌 Cliente WebSocket conectado (${clientIp}). Total clientes: ${wss.clients.size}`);

  // Mensaje de bienvenida
  ws.send(JSON.stringify({
    type: 'WELCOME',
    clientsCount: wss.clients.size,
    timestamp: Date.now()
  }));

  // Notificar a todos sobre la cantidad de clientes activos
  broadcast({
    type: 'CLIENTS_COUNT',
    count: wss.clients.size
  });

  ws.on('message', (message) => {
    try {
      const parsed = JSON.parse(message.toString());
      console.log(`⚡ WebSocket mensaje recibido [${parsed.type || 'UNKNOWN'}]`);
      // Reenviar a todos los demás clientes
      broadcast(parsed, ws);
    } catch (err) {
      console.error('Error procesando mensaje WebSocket:', err);
    }
  });

  ws.on('close', () => {
    console.log(`🔌 Cliente WebSocket desconectado. Restantes: ${wss.clients.size}`);
    broadcast({
      type: 'CLIENTS_COUNT',
      count: wss.clients.size
    });
  });

  ws.on('error', (err) => {
    console.error('Error en WebSocket client:', err);
  });
});

// Endpoint HTTP para enviar broadcast si se desea desde scripts
app.post('/api/broadcast', (req, res) => {
  const payload = req.body;
  if (!payload || !payload.type) {
    return res.status(400).json({ success: false, error: 'Payload must contain a type' });
  }
  broadcast(payload);
  return res.json({ success: true, clients: wss.clients.size });
});

// Rutas limpias sin extensión .html
app.get('/', (req, res) => res.sendFile(path.join(distDir, 'normal.html')));
app.get('/normal', (req, res) => res.sendFile(path.join(distDir, 'normal.html')));
app.get('/edit', (req, res) => res.sendFile(path.join(distDir, 'edit.html')));
app.get('/db', (req, res) => res.redirect('/'));
app.get('/config', (req, res) => res.sendFile(path.join(distDir, 'config.html')));
app.get('/migrate', (req, res) => res.sendFile(path.join(distDir, 'migrate.html')));

// Sirve los archivos estáticos
app.use(express.static(distDir));

// API: Subir imagen de espíritu a Cloudinary
app.post('/api/upload-spirit-image', async (req, res) => {
  const { spiritId, imageBase64, season = 2 } = req.body;

  if (!spiritId || !imageBase64) {
    return res.status(400).json({ success: false, error: 'Faltan spiritId o imageBase64' });
  }

  const twoDigitId = String(spiritId).padStart(2, '0');
  const publicId = parseInt(season) === 2 ? `spirits_s2/ic_spirit_s2_${twoDigitId}` : `spirits/ic_spirit_${twoDigitId}`;

  try {
    const result = await cloudinary.uploader.upload(imageBase64, {
      public_id: publicId,
      overwrite: true,
    });

    console.log(`✅ Imagen subida: ${result.secure_url}`);
    
    // Notificar en tiempo real por WebSockets
    broadcast({
      type: 'IMAGE_UPLOADED',
      spiritId,
      url: result.secure_url,
      timestamp: Date.now()
    });

    return res.json({ success: true, url: result.secure_url, publicId });
  } catch (error) {
    console.error('❌ Error al subir a Cloudinary:', error.message);
    return res.status(500).json({ success: false, error: error.message });
  }
});

// API: Eliminar imágenes no utilizadas de Cloudinary
app.post('/api/clean-unused-cloudinary', async (req, res) => {
  const { activeUrls } = req.body;

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
      return res.json({ success: true, deletedCount: 0, message: 'No hay imágenes huérfanas o sin usar en Cloudinary.' });
    }

    const deleteResult = await cloudinary.api.delete_resources(toDelete);
    console.log(`🗑️ Eliminadas ${toDelete.length} imágenes huérfanas de Cloudinary:`, toDelete);

    return res.json({
      success: true,
      deletedCount: toDelete.length,
      deletedPublicIds: toDelete,
      result: deleteResult
    });
  } catch (error) {
    console.error('❌ Error al limpiar Cloudinary:', error.message);
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
  server.listen(port)
    .on('listening', () => {
      console.log(`\n🚀 Servidor HTTP + WebSockets iniciado en http://localhost:${port}`);
      console.log(`📡 WebSocket endpoint disponible en ws://localhost:${port}/ws`);
      console.log('   Abre esa URL en tu navegador para usar la app.\n');
    })
    .on('error', () => {
      console.log(`⚠️  Puerto ${port} ocupado, probando el siguiente...`);
      tryListen(ports.slice(1));
    });
};

tryListen(PORTS);
