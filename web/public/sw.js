// Service worker del Gestor de Diario: guarda HTML, JS, CSS y fuentes para visitas repetidas y uso sin conexión.
// No intercepta Firestore, RTDB, la API ni los WebSockets: esos van siempre directo a la red.

const CACHE = 'diario-static-v1';
const IMMUTABLE_PATHS = /^\/(assets|fonts)\//;
const CACHEABLE_CDN_HOSTS = ['unpkg.com', 'www.gstatic.com'];

self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

async function putInCache(request, response) {
  // Solo respuestas completas y legibles; las opacas (no-cors) inflan la cuota de almacenamiento
  if (response && response.ok && response.type !== 'opaque') {
    const cache = await caches.open(CACHE);
    await cache.put(request, response.clone());
  }
  return response;
}

async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  return putInCache(request, await fetch(request));
}

async function networkFirst(request, fallbackUrl) {
  try {
    return await putInCache(request, await fetch(request));
  } catch (err) {
    const cached = await caches.match(request) || (fallbackUrl && await caches.match(fallbackUrl));
    if (cached) return cached;
    throw err;
  }
}

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  const sameOrigin = url.origin === self.location.origin;

  if (!sameOrigin) {
    // Librerías versionadas de CDN (Leaflet, Firebase en modo desarrollo): nunca cambian para una misma URL
    if (CACHEABLE_CDN_HOSTS.includes(url.hostname) && request.destination !== 'image') {
      event.respondWith(cacheFirst(request));
    }
    return;
  }

  if (url.pathname.startsWith('/api/') || url.pathname === '/ws' || url.pathname === '/sw.js') return;

  if (request.mode === 'navigate') {
    // Todas las rutas sirven la misma SPA: sin red se responde con cualquier copia guardada
    event.respondWith(networkFirst(request, '/album'));
    return;
  }

  // Los archivos con hash de Vite y las fuentes son inmutables; el resto se revalida contra la red
  event.respondWith(IMMUTABLE_PATHS.test(url.pathname) ? cacheFirst(request) : networkFirst(request));
});
