/**
 * radar-view.js - Vista de Radar GPS en Tiempo Real, Telemetría, Mapa Interactivo Google Maps y Control Remoto Magic Packet
 */

import { state } from './state.js';
import { db, rtdb } from './firebase-config.js';
import { sendWsMessage } from './websocket.js';

let radarMap = null;
let currentTileLayer = null;
let currentTileTheme = 'google_road';
let kevinMarker = null;
let kevinCircle = null;
let aliMarker = null;
let aliCircle = null;
let zoneLayers = [];
let radarUnsubscribe = null;
let radarZonesUnsubscribe = null;
let hasAutoFitted = false;

export function getUserPreferredMapTheme() {
  const userKey = state.currentUser?.username || localStorage.getItem("logged_user") || "kevin";
  return state.googleMapTheme 
    || localStorage.getItem("google_map_theme_" + userKey)
    || localStorage.getItem("google_map_theme")
    || "google_road";
}

function parseTimestampMillis(timestamp) {
  if (!timestamp) return 0;
  if (typeof timestamp === 'number') return timestamp;
  if (timestamp.toMillis && typeof timestamp.toMillis === 'function') return timestamp.toMillis();
  if (timestamp.toDate && typeof timestamp.toDate === 'function') return timestamp.toDate().getTime();
  const parsed = Number(timestamp);
  return isNaN(parsed) ? 0 : parsed;
}

function formatTimeAgo(timestamp) {
  const tsMillis = parseTimestampMillis(timestamp);
  if (!tsMillis) return "Sin datos";
  try {
    const diffSecs = Math.floor((Date.now() - tsMillis) / 1000);
    if (diffSecs < 10) return "Justo ahora";
    if (diffSecs < 60) return `Hace ${diffSecs}s`;
    const diffMins = Math.floor(diffSecs / 60);
    if (diffMins < 60) return `Hace ${diffMins} min`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `Hace ${diffHours} h`;
    const d = new Date(tsMillis);
    return d.toLocaleDateString([], { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  } catch (e) {
    return "Fecha inválida";
  }
}

function getDeviceStatus(data) {
  if (!data || !data.timestamp) {
    return { cls: 'offline', text: 'Desconectado', isOnline: false };
  }
  const tsMillis = parseTimestampMillis(data.timestamp);
  const diffSecs = Math.floor((Date.now() - tsMillis) / 1000);
  
  const isSharing = data.isSharing !== undefined ? data.isSharing : data.isSharingLocation !== false;
  if (!isSharing) {
    return { cls: 'offline', text: 'Radar Apagado', isOnline: false };
  }
  if (diffSecs < 120) {
    return { cls: 'online', text: 'En Línea 🟢', isOnline: true };
  }
  if (diffSecs < 900) {
    return { cls: 'stale', text: `Inactivo (${Math.floor(diffSecs / 60)}m) 🟡`, isOnline: false };
  }
  return { cls: 'offline', text: `Desconectado (${formatTimeAgo(tsMillis)}) ⚪`, isOnline: false };
}

export function initRadarView() {
  currentTileTheme = getUserPreferredMapTheme();
  listenToRadarLocations();
  renderRadarManager();
}

export function renderRadarManager() {
  const container = document.getElementById("radar-container") || document.getElementById("radarView");
  if (!container) return;

  currentTileTheme = getUserPreferredMapTheme();

  const kevinData = state.radarUsersData?.kevin || {};
  const aliData = state.radarUsersData?.ali || {};
  const radarZonesData = state.radarZonesData || [];
  const coupleId = state.coupleId;

  // Si ya existe la estructura del dashboard en el DOM, actualizamos sólo las secciones dinámicas
  // para no interrumpir la interacción del usuario con el mapa de Google Maps
  const existingDashboard = container.querySelector(".radar-dashboard");
  if (existingDashboard && document.getElementById("radar-live-map")) {
    const cardsGrid = document.getElementById("radar-cards-grid");
    if (cardsGrid) {
      cardsGrid.innerHTML = `
        ${renderUserCard('kevin', 'Celular de Kevin', '#3b82f6', '🔵', kevinData)}
        ${renderUserCard('ali', 'Celular de Ali', '#ec4899', '🔴', aliData)}
      `;
    }

    const zonesList = document.getElementById("radar-zones-list");
    const zonesCount = document.getElementById("radar-zones-count");
    if (zonesCount) zonesCount.textContent = `(${radarZonesData.length})`;
    if (zonesList) {
      zonesList.innerHTML = radarZonesData.length === 0 ? `
        <div style="font-size:16px; color:var(--text-muted);">No hay zonas seguras creadas aún en Firestore (locations/${coupleId}/zones).</div>
      ` : radarZonesData.map(z => `
        <div style="background:rgba(0,0,0,0.3); border:1px solid var(--card-border); border-radius:10px; padding:10px 14px; display:flex; align-items:center; gap:10px;">
          <span style="font-size:22px;">${z.icon || '📍'}</span>
          <div>
            <div style="font-weight:700; font-size:16px; color:var(--text-color);">${z.name}</div>
            <div style="font-size:14px; color:var(--text-muted);">${Math.round(z.radiusMeters || 100)}m de radio • ${z.latitude ? z.latitude.toFixed(4) + ', ' + z.longitude.toFixed(4) : ''}</div>
          </div>
        </div>
      `).join('');
    }

    const selectEl = document.getElementById("radar-tile-select");
    if (selectEl && selectEl.value !== currentTileTheme) {
      selectEl.value = currentTileTheme;
    }

    const debugPre = document.getElementById("radar-debug-pre");
    if (debugPre) {
      debugPre.textContent = JSON.stringify({ coupleId: coupleId, users: state.radarUsersData, zones: radarZonesData }, null, 2);
    }

    initOrUpdateRadarMap();
    return;
  }

  container.innerHTML = `
    <div class="radar-dashboard">
      <!-- Hero Header -->
      <div class="radar-hero">
        <div>
          <h2 style="font-family:'VT323',monospace; font-size:28px; font-weight:700; background:linear-gradient(135deg, #00e5ff, #e040fb); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
            📡 Panel de Dispositivos Conectados & Control de Thor Radar
          </h2>
          <p style="font-size:16px; color:var(--text-muted); margin-top:4px;">
            Supervisa el estado en tiempo real de Kevin y Ali con mapa interactivo de Google Maps, telemetría de batería, velocidad, actividad y soporte Magic Packet.
          </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <span style="display:inline-flex; align-items:center; gap:6px; font-size:14px; font-weight:700; color:#10b981; background:rgba(16,185,129,0.1); border:1px solid rgba(16,185,129,0.3); padding:6px 10px; border-radius:20px;">
            <span style="width:7px; height:7px; border-radius:50%; background:#10b981; box-shadow:0 0 8px #10b981;"></span> Sincronización en vivo (1s)
          </span>
          <button class="btn btn-secondary" onclick="window.renderRadarManager()" style="font-size:16px; padding:8px 12px;">
            🔄 Refrescar Vista
          </button>
        </div>
      </div>

      <!-- Interactive Radar Google Map Card -->
      <div class="radar-map-card">
        <div class="radar-map-header">
          <div style="display:flex; align-items:center; gap:10px;">
            <span style="font-size:22px;">🗺️</span>
            <div>
              <h3 style="font-family:'VT323',monospace; font-size:24px; font-weight:700; color:var(--text-color); margin:0;">
                Mapa GPS Google & Zonas Seguras en Vivo
              </h3>
              <div style="font-size:12px; color:var(--text-muted);">
                Seguimiento satelital Google Maps con precisión y zonas seguras
              </div>
            </div>
          </div>

          <div class="radar-map-actions">
            <button class="btn btn-secondary" onclick="window.focusRadarUser('kevin')" style="font-size:12px; padding:6px 10px; border-color:rgba(59,130,246,0.5); color:#60a5fa;">
              🔵 Kevin
            </button>
            <button class="btn btn-secondary" onclick="window.focusRadarUser('ali')" style="font-size:12px; padding:6px 10px; border-color:rgba(236,72,153,0.5); color:#f472b6;">
              🔴 Ali
            </button>
            <button class="btn btn-secondary" onclick="window.fitRadarMapBounds()" style="font-size:12px; padding:6px 10px;">
              🧭 Ajustar Todo
            </button>
            <select id="radar-tile-select" onchange="window.changeRadarTileLayer(this.value, true)" style="background:#151824; color:#fff; border:1px solid var(--card-border); border-radius:8px; padding:6px 8px; font-size:12px; font-family:'Inter',sans-serif; cursor:pointer;">
              <option value="google_road" ${currentTileTheme === 'google_road' ? 'selected' : ''}>🗺️ Google Maps (Estándar)</option>
              <option value="google_dark" ${currentTileTheme === 'google_dark' ? 'selected' : ''}>🌙 Google Maps (Modo Oscuro)</option>
              <option value="google_hybrid" ${currentTileTheme === 'google_hybrid' ? 'selected' : ''}>🛰️ Google Maps (Satélite Híbrido)</option>
              <option value="google_terrain" ${currentTileTheme === 'google_terrain' ? 'selected' : ''}>⛰️ Google Maps (Relieve / Terreno)</option>
              <option value="google_traffic" ${currentTileTheme === 'google_traffic' ? 'selected' : ''}>🚦 Google Maps (Tráfico en Vivo)</option>
            </select>
          </div>
        </div>

        <!-- Leaflet Map Container with Google Maps Tiles -->
        <div id="radar-live-map" class="radar-map-container ${currentTileTheme === 'google_dark' ? 'dark-tiles' : ''}"></div>
      </div>

      <!-- Cards Grid -->
      <div class="radar-cards-grid" id="radar-cards-grid">
        ${renderUserCard('kevin', 'Celular de Kevin', '#3b82f6', '🔵', kevinData)}
        ${renderUserCard('ali', 'Celular de Ali', '#ec4899', '🔴', aliData)}
      </div>

      <!-- Registered Safe Zones -->
      <div class="category-card" style="margin-bottom: 24px;">
        <div class="category-header">
          <div style="display:flex; align-items:center; gap:8px;">
            <span style="font-size:20px;">🏠</span>
            <h3 style="font-family:'VT323',monospace; font-size:22px; font-weight:700; color:var(--text-color);">
              Zonas Seguras Registradas <span id="radar-zones-count">(${radarZonesData.length})</span>
            </h3>
          </div>
        </div>
        <div id="radar-zones-list" style="padding: 16px; display:flex; flex-wrap:wrap; gap:10px;">
          ${radarZonesData.length === 0 ? `
            <div style="font-size:16px; color:var(--text-muted);">No hay zonas seguras creadas aún en Firestore (locations/${coupleId}/zones).</div>
          ` : radarZonesData.map(z => `
            <div style="background:rgba(0,0,0,0.3); border:1px solid var(--card-border); border-radius:10px; padding:10px 14px; display:flex; align-items:center; gap:10px;">
              <span style="font-size:22px;">${z.icon || '📍'}</span>
              <div>
                <div style="font-weight:700; font-size:16px; color:var(--text-color);">${z.name}</div>
                <div style="font-size:14px; color:var(--text-muted);">${Math.round(z.radiusMeters || 100)}m de radio • ${z.latitude ? z.latitude.toFixed(4) + ', ' + z.longitude.toFixed(4) : ''}</div>
              </div>
            </div>
          `).join('')}
        </div>
      </div>

      <!-- Raw Telemetry Debugger -->
      <details style="background:rgba(0,0,0,0.4); border:1px solid var(--card-border); border-radius:12px; padding:12px 16px; cursor:pointer;">
        <summary style="font-size:15px; font-weight:700; color:var(--text-muted); user-select:none;">
          🔍 Ver Telemetría JSON Cruda en Vivo (Debug)
        </summary>
        <pre id="radar-debug-pre" style="margin-top:12px; padding:12px; background:#0d0e15; border-radius:8px; font-size:14px; color:#a78bfa; overflow-x:auto; font-family:'VT323', monospace;">${JSON.stringify({ coupleId: coupleId, users: state.radarUsersData, zones: radarZonesData }, null, 2)}</pre>
      </details>
    </div>
  `;

  initOrUpdateRadarMap();
}

function createCustomMarkerIcon(userKey, name, color, iconEmoji, avatarUrl, battery, isOnline) {
  const battText = (battery !== undefined && battery !== null && battery !== '--') ? `${battery}%` : '';
  const pulseClass = isOnline ? 'pulse' : '';
  const avatarHtml = avatarUrl 
    ? `<img src="${avatarUrl}" style="width:100%; height:100%; border-radius:50%; object-fit:cover;">`
    : iconEmoji;

  const html = `
    <div class="radar-map-marker ${userKey}">
      <div class="radar-marker-bubble ${userKey} ${pulseClass}" style="border-color:${color}; color:${color};">
        ${avatarHtml}
      </div>
      <div class="radar-marker-label" style="border-color:${color};">
        ${userKey === 'kevin' ? '🔵 Kevin' : '🔴 Ali'} ${battText ? `(${battText})` : ''}
      </div>
    </div>
  `;

  return L.divIcon({
    html: html,
    className: 'custom-radar-div-icon',
    iconSize: [60, 64],
    iconAnchor: [30, 56],
    popupAnchor: [0, -50]
  });
}

function createMarkerPopupContent(userKey, name, color, data) {
  const status = getDeviceStatus(data);
  const battery = (data.batteryLevel !== undefined && data.batteryLevel !== null) ? data.batteryLevel : "--";
  const isCharging = data.isCharging === true;
  const activity = data.activity || "Estacionario";
  const speed = (data.speedKmh !== undefined && data.speedKmh !== null)
    ? Math.round(data.speedKmh)
    : ((data.speed !== undefined && data.speed !== null) ? Math.round(data.speed * 3.6) : 0);
  const accuracy = (data.accuracy !== undefined && data.accuracy !== null) ? Math.round(data.accuracy) : "--";
  const zone = data.currentZone ? `📍 ${data.currentZone}` : (data.address || (data.latitude ? `${data.latitude.toFixed(5)}, ${data.longitude.toFixed(5)}` : "Sin ubicación reciente"));
  const mapsUrl = (data.latitude && data.longitude) ? `https://www.google.com/maps?q=${data.latitude},${data.longitude}` : "#";

  return `
    <div style="padding:4px; font-family:'Inter', sans-serif; min-width: 210px;">
      <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; border-bottom:1px solid rgba(255,255,255,0.1); padding-bottom:6px;">
        <strong style="font-size:16px; color:${color}; font-family:'VT323',monospace;">${name}</strong>
        <span style="font-size:11px; padding:2px 6px; border-radius:4px; background:rgba(255,255,255,0.1);">${status.text}</span>
      </div>
      <div style="display:flex; flex-direction:column; gap:4px; font-size:12px; color:#cbd5e1;">
        <div>🔋 <strong>Batería:</strong> ${battery}% ${isCharging ? '⚡ (Cargando)' : ''}</div>
        <div>🏃 <strong>Actividad:</strong> ${activity} (${speed} km/h)</div>
        <div>🎯 <strong>Precisión:</strong> ±${accuracy}m</div>
        <div>📍 <strong>Ubicación:</strong> ${zone}</div>
        <div>🕒 <strong>Última señal:</strong> ${formatTimeAgo(data.timestamp)}</div>
      </div>
      ${(data.latitude && data.longitude) ? `
        <div style="margin-top:10px; text-align:right; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 6px;">
          <a href="${mapsUrl}" target="_blank" style="color:#60a5fa; font-size:11px; text-decoration:none; font-weight:600; display:inline-flex; align-items:center; gap:3px;">
            🗺️ Abrir en Google Maps ↗
          </a>
        </div>
      ` : ''}
    </div>
  `;
}

export function initOrUpdateRadarMap() {
  const mapEl = document.getElementById("radar-live-map");
  if (!mapEl || !window.L) return;

  const preferredTheme = getUserPreferredMapTheme();
  currentTileTheme = preferredTheme;

  // Inicializar mapa si no existe
  if (!radarMap) {
    radarMap = L.map(mapEl, {
      zoomControl: true,
      attributionControl: true
    }).setView([-33.4489, -70.6693], 13);

    changeRadarTileLayer(preferredTheme, false);
  } else if (!currentTileLayer) {
    changeRadarTileLayer(preferredTheme, false);
  }

  const kevinData = state.radarUsersData?.kevin || {};
  const aliData = state.radarUsersData?.ali || {};
  const kevinStatus = getDeviceStatus(kevinData);
  const aliStatus = getDeviceStatus(aliData);

  // Actualizar Kevin Marker & Accuracy Circle
  if (kevinData.latitude && kevinData.longitude) {
    const kPos = [kevinData.latitude, kevinData.longitude];
    const kIcon = createCustomMarkerIcon('kevin', 'Kevin', '#3b82f6', '🔵', kevinData.profileImageUrl, kevinData.batteryLevel, kevinStatus.isOnline);
    const kPopup = createMarkerPopupContent('kevin', 'Celular de Kevin', '#3b82f6', kevinData);

    if (!kevinMarker) {
      kevinMarker = L.marker(kPos, { icon: kIcon, zIndexOffset: 1000 }).addTo(radarMap);
      kevinMarker.bindPopup(kPopup);
    } else {
      kevinMarker.setLatLng(kPos);
      kevinMarker.setIcon(kIcon);
      kevinMarker.setPopupContent(kPopup);
    }

    const kAcc = Math.max(15, Number(kevinData.accuracy) || 30);
    if (!kevinCircle) {
      kevinCircle = L.circle(kPos, {
        radius: kAcc,
        color: '#3b82f6',
        fillColor: '#3b82f6',
        fillOpacity: 0.12,
        weight: 1,
        dashArray: '4, 4'
      }).addTo(radarMap);
    } else {
      kevinCircle.setLatLng(kPos);
      kevinCircle.setRadius(kAcc);
    }
  }

  // Actualizar Ali Marker & Accuracy Circle
  if (aliData.latitude && aliData.longitude) {
    const aPos = [aliData.latitude, aliData.longitude];
    const aIcon = createCustomMarkerIcon('ali', 'Ali', '#ec4899', '🔴', aliData.profileImageUrl, aliData.batteryLevel, aliStatus.isOnline);
    const aPopup = createMarkerPopupContent('ali', 'Celular de Ali', '#ec4899', aliData);

    if (!aliMarker) {
      aliMarker = L.marker(aPos, { icon: aIcon, zIndexOffset: 1000 }).addTo(radarMap);
      aliMarker.bindPopup(aPopup);
    } else {
      aliMarker.setLatLng(aPos);
      aliMarker.setIcon(aIcon);
      aliMarker.setPopupContent(aPopup);
    }

    const aAcc = Math.max(15, Number(aliData.accuracy) || 30);
    if (!aliCircle) {
      aliCircle = L.circle(aPos, {
        radius: aAcc,
        color: '#ec4899',
        fillColor: '#ec4899',
        fillOpacity: 0.12,
        weight: 1,
        dashArray: '4, 4'
      }).addTo(radarMap);
    } else {
      aliCircle.setLatLng(aPos);
      aliCircle.setRadius(aAcc);
    }
  }

  // Actualizar Zonas Seguras
  zoneLayers.forEach(layer => {
    try { radarMap.removeLayer(layer); } catch(e) {}
  });
  zoneLayers = [];

  const radarZonesData = state.radarZonesData || [];
  radarZonesData.forEach(z => {
    if (z.latitude && z.longitude) {
      const radius = Number(z.radiusMeters) || 100;
      const zoneCircle = L.circle([z.latitude, z.longitude], {
        radius: radius,
        color: '#10b981',
        weight: 2,
        dashArray: '6, 6',
        fillColor: '#10b981',
        fillOpacity: 0.15
      }).addTo(radarMap);

      zoneCircle.bindTooltip(`${z.icon || '🏠'} ${z.name}`, {
        permanent: false,
        direction: 'top',
        className: 'radar-zone-tooltip'
      });

      zoneLayers.push(zoneCircle);
    }
  });

  // Ajustar vista inicial automáticamente una sola vez si hay coordenadas
  if (!hasAutoFitted && (kevinData.latitude || aliData.latitude || radarZonesData.length > 0)) {
    hasAutoFitted = true;
    setTimeout(() => {
      fitRadarMapBounds();
    }, 150);
  }

  setTimeout(() => {
    if (radarMap) radarMap.invalidateSize();
  }, 100);
}

export function focusRadarUser(userKey) {
  const data = state.radarUsersData?.[userKey];
  if (!radarMap) return;
  if (data && data.latitude && data.longitude) {
    radarMap.flyTo([data.latitude, data.longitude], 16, { animate: true, duration: 1 });
    const marker = userKey === 'kevin' ? kevinMarker : aliMarker;
    if (marker) {
      setTimeout(() => marker.openPopup(), 900);
    }
  } else {
    if (window.showToast) window.showToast(`Sin coordenadas GPS recientes para ${userKey === 'kevin' ? 'Kevin' : 'Ali'}`);
  }
}

export function fitRadarMapBounds() {
  if (!radarMap) return;
  const points = [];
  const k = state.radarUsersData?.kevin;
  const a = state.radarUsersData?.ali;
  if (k && k.latitude && k.longitude) points.push([k.latitude, k.longitude]);
  if (a && a.latitude && a.longitude) points.push([a.latitude, a.longitude]);
  (state.radarZonesData || []).forEach(z => {
    if (z.latitude && z.longitude) points.push([z.latitude, z.longitude]);
  });

  if (points.length === 1) {
    radarMap.flyTo(points[0], 15, { animate: true, duration: 0.8 });
  } else if (points.length > 1) {
    const bounds = L.latLngBounds(points);
    radarMap.fitBounds(bounds, { padding: [50, 50], maxZoom: 16, animate: true });
  } else {
    radarMap.setView([-33.4489, -70.6693], 13);
  }
}

export async function changeRadarTileLayer(theme = 'google_road', saveToDb = true) {
  currentTileTheme = theme;
  state.googleMapTheme = theme;

  const userKey = state.currentUser?.username || localStorage.getItem("logged_user") || 'kevin';
  try {
    localStorage.setItem('google_map_theme_' + userKey, theme);
    localStorage.setItem('google_map_theme', theme);
  } catch(e) {}

  const selectEl = document.getElementById("radar-tile-select");
  if (selectEl && selectEl.value !== theme) {
    selectEl.value = theme;
  }

  const mapContainer = document.getElementById("radar-live-map");
  if (mapContainer) {
    if (theme === 'google_dark') {
      mapContainer.classList.add('dark-tiles');
    } else {
      mapContainer.classList.remove('dark-tiles');
    }
  }

  if (radarMap && window.L) {
    if (currentTileLayer) {
      try { radarMap.removeLayer(currentTileLayer); } catch(e) {}
    }

    // Google Maps Tiles Oficiales (igual que en Android app con mt0..mt3 subdominios)
    let lyrs = 'm';
    if (theme === 'google_hybrid') lyrs = 'y';
    else if (theme === 'google_terrain') lyrs = 'p';
    else if (theme === 'google_traffic') lyrs = 'm,traffic';

    const tileUrl = `https://mt{s}.google.com/vt/lyrs=${lyrs}&hl=es&x={x}&y={y}&z={z}`;

    currentTileLayer = L.tileLayer(tileUrl, {
      subdomains: ['0', '1', '2', '3'],
      maxZoom: 20,
      attribution: '© Google Maps'
    }).addTo(radarMap);
  }

  // Guardar en la Base de Datos la preferencia de este usuario
  if (saveToDb && state.currentUser) {
    try {
      // 1. Guardar en Firestore colección users/{userId}
      if (state.currentUser.docId) {
        await db.collection("users").doc(state.currentUser.docId).set({
          googleMapTheme: theme,
          radarMapTheme: theme,
          updatedAt: Date.now()
        }, { merge: true });
      }

      // 2. Guardar en Realtime Database locations/{coupleId}/users/{userKey}
      await rtdb.ref("locations/" + state.coupleId + "/users/" + userKey).update({
        googleMapTheme: theme
      });

      if (window.showToast) {
        const themeLabels = {
          google_road: 'Estándar',
          google_dark: 'Modo Oscuro',
          google_hybrid: 'Satélite Híbrido',
          google_terrain: 'Relieve',
          google_traffic: 'Tráfico'
        };
        window.showToast(`🗺️ Mapa guardado para ${state.currentUser.name}: ${themeLabels[theme] || theme}`);
      }
    } catch(err) {
      console.warn("Aviso guardando tema de mapa en BD:", err);
    }
  }
}

function renderUserCard(userKey, name, color, icon, data) {
  const isSharing = (data.isSharing !== undefined) ? data.isSharing : (data.isSharingLocation !== false);
  const status = getDeviceStatus(data);
  const battery = (data.batteryLevel !== undefined && data.batteryLevel !== null) ? data.batteryLevel : "--";
  const isCharging = data.isCharging === true;
  const activityText = data.activity || "Estacionario";
  const speed = (data.speedKmh !== undefined && data.speedKmh !== null)
    ? Math.round(data.speedKmh)
    : ((data.speed !== undefined && data.speed !== null) ? Math.round(data.speed * 3.6) : 0);
  const accuracy = (data.accuracy !== undefined && data.accuracy !== null) ? Math.round(data.accuracy) : "--";
  const zone = data.currentZone ? `📍 ${data.currentZone}` : (data.address || (data.latitude ? `${data.latitude.toFixed(5)}, ${data.longitude.toFixed(5)}` : "Sin ubicación reciente"));
  const lat = data.latitude || 0;
  const lon = data.longitude || 0;
  const mapsUrl = (lat && lon) ? `https://www.google.com/maps?q=${lat},${lon}` : "#";
  const isSos = data.sosActive === true;

  let batteryColor = "#10b981";
  if (battery !== "--") {
    if (battery <= 20) batteryColor = "#ef4444";
    else if (battery <= 40) batteryColor = "#f59e0b";
  }

  const isLoadingPing = state.radarPingLoading?.[userKey];

  return `
    <div class="radar-user-card ${userKey}">
      <div class="radar-user-header">
        <div class="radar-user-info">
          <div class="radar-user-avatar" style="display:flex; align-items:center; justify-content:center; font-size:24px; border-color:${color};">
            ${data.profileImageUrl ? `<img src="${data.profileImageUrl}" style="width:100%; height:100%; border-radius:50%; object-fit:cover;">` : icon}
          </div>
          <div>
            <h3 style="font-family:'VT323',monospace; font-size:24px; font-weight:700; color:${color}; margin:0;">
              ${name} <span style="font-size:16px; color:var(--text-muted); font-weight:500;">(${data.userName || (userKey === 'kevin' ? 'Kevin' : 'Ali')})</span>
            </h3>
            <div style="font-size:11px; color:var(--text-muted); margin-top:2px;">ID: <code>locations/${state.coupleId}/users/${userKey}</code></div>
          </div>
        </div>
        <span class="radar-status-badge ${status.cls}">${status.text}</span>
      </div>

      <!-- Thor Radar Master Power Switch Box -->
      <div class="radar-power-box ${isSharing ? 'active' : 'disabled'}">
        <div style="display:flex; flex-direction:column; gap:2px;">
          <span style="font-size:13px; font-weight:700; color:${isSharing ? 'var(--success-color)' : 'var(--error-color)'};">
            ${isSharing ? '⚡ THOR RADAR ACTIVO' : '🛑 THOR RADAR DESACTIVADO'}
          </span>
          <span style="font-size:11px; color:var(--text-muted);">
            ${isSharing ? 'El celular transmite y responde a peticiones' : 'GPS y servicio en segundo plano suspendidos (Gasto 0)'}
          </span>
        </div>
        <button class="btn ${isSharing ? 'btn-danger' : 'btn-success'}" onclick="window.toggleRemoteRadar('${userKey}', ${!isSharing})" style="padding: 8px 14px; font-size: 12px; font-weight: 700; white-space: nowrap;">
          ${isSharing ? '🛑 APAGAR RADAR' : '⚡ ENCENDER RADAR'}
        </button>
      </div>

      <!-- Telemetry Matrix -->
      <div class="radar-telemetry-grid">
        <div class="radar-telemetry-item">
          <div class="radar-telemetry-label">🔋 BATERÍA</div>
          <div class="radar-telemetry-value" style="color:${batteryColor};">
            ${battery}% ${isCharging ? '⚡ Cargando' : ''}
          </div>
          <div class="radar-battery-track">
            <div class="radar-battery-fill" style="width:${battery !== '--' ? Math.min(100, battery) : 0}%; background:${batteryColor};"></div>
          </div>
        </div>

        <div class="radar-telemetry-item">
          <div class="radar-telemetry-label">🏃 ACTIVIDAD & VELOCIDAD</div>
          <div class="radar-telemetry-value">
            ${activityText} (${speed} km/h)
          </div>
        </div>

        <div class="radar-telemetry-item" style="grid-column: span 2;">
          <div class="radar-telemetry-label">📍 UBICACIÓN / ZONA SEGURA</div>
          <div class="radar-telemetry-value" style="font-size:12px;" title="${zone}">
            ${zone}
          </div>
          ${(lat && lon) ? `
            <div style="margin-top: 4px; display:flex; gap:10px; align-items:center;">
              <button onclick="window.focusRadarUser('${userKey}')" style="background:transparent; border:none; color:#60a5fa; cursor:pointer; font-size:11px; padding:0; display:inline-flex; align-items:center; gap:3px;">
                🎯 Ver en Mapa
              </button>
              <span style="color:var(--text-muted); font-size:10px;">•</span>
              <a href="${mapsUrl}" target="_blank" style="font-size: 11px; color: #a78bfa; text-decoration: none; display: inline-flex; align-items: center; gap: 4px;">
                🗺️ Google Maps (${lat.toFixed(4)}, ${lon.toFixed(4)} ±${accuracy}m) ↗
              </a>
            </div>
          ` : ''}
        </div>

        <div class="radar-telemetry-item">
          <div class="radar-telemetry-label">🕒 ÚLTIMA SEÑAL</div>
          <div class="radar-telemetry-value" style="font-size:12px;">
            ${formatTimeAgo(data.timestamp)}
          </div>
        </div>

        <div class="radar-telemetry-item">
          <div class="radar-telemetry-label">🚨 ESTADO SOS</div>
          <div class="radar-telemetry-value" style="color:${isSos ? 'var(--error-color)' : 'var(--success-color)'};">
            ${isSos ? '🚨 ¡SOS ACTIVO!' : '🟢 Normal'}
          </div>
        </div>
      </div>

      <!-- Quick Action Buttons for Testing -->
      <div style="border-top: 1px solid var(--card-border); padding-top: 12px;">
        <span style="font-size:11px; color:var(--text-muted); font-weight:600; display:block; margin-bottom:8px;">🛠️ ACCIONES MAGIC PACKET & ALERTAS:</span>
        <div class="radar-actions-grid">
          <button class="btn btn-secondary" onclick="window.sendRemoteMagicPing('${userKey}', true)" ${isLoadingPing ? 'disabled style="opacity:0.6; pointer-events:none; font-size:11px; padding:6px 10px;"' : 'style="font-size:11px; padding:6px 10px;"'} title="Dispara Wake-on-LAN silencioso sin emitir sonido en el celular">
            ${isLoadingPing ? '⏳ Solicitando GPS...' : '⚡ Ping Silencioso'}
          </button>
          <button class="btn btn-secondary" onclick="window.sendRemoteMagicPing('${userKey}', false)" style="font-size:11px; padding:6px 10px;" title="Dispara Wake-on-LAN con alerta visible y sonora en el celular">
            🔔 Ping con Notificación
          </button>
          <button class="btn ${isSos ? 'btn-success' : 'btn-danger'}" onclick="window.toggleRemoteSos('${userKey}', ${!isSos})" style="font-size:11px; padding:6px 10px;" title="Simular alarma SOS para pruebas">
            ${isSos ? '✅ Desactivar SOS' : '🚨 Probar Alerta SOS'}
          </button>
          <button class="btn btn-secondary" onclick="window.resetDevicePresence('${userKey}')" style="font-size:11px; padding:6px 10px; color: #f87171;" title="Marcar como desconectado">
            🧹 Marcar Desconectado
          </button>
        </div>
      </div>
    </div>
  `;
}

export function listenToRadarLocations() {
  if (radarUnsubscribe) {
    if (typeof radarUnsubscribe === 'function') radarUnsubscribe();
    else if (radarUnsubscribe.off) radarUnsubscribe.off();
  }
  if (radarZonesUnsubscribe) radarZonesUnsubscribe();

  try {
    // Escuchar usuarios en Realtime Database
    const usersRef = rtdb.ref("locations/" + state.coupleId + "/users");
    usersRef.on("value", snapshot => {
      const val = snapshot.val() || {};
      ['kevin', 'ali'].forEach(uKey => {
        if (val[uKey]) {
          state.radarUsersData[uKey] = val[uKey];
        }
      });
      const activeUser = state.currentUser?.username || localStorage.getItem("logged_user") || "kevin";
      if (val[activeUser]?.googleMapTheme) {
        state.googleMapTheme = val[activeUser].googleMapTheme;
        localStorage.setItem("google_map_theme_" + activeUser, val[activeUser].googleMapTheme);
        changeRadarTileLayer(val[activeUser].googleMapTheme, false);
      }
      renderRadarManager();
    }, err => console.warn("Aviso RTDB users:", err));
    radarUnsubscribe = usersRef;

    // Escuchar Zonas Seguras en Firestore
    radarZonesUnsubscribe = db.collection("locations").doc(state.coupleId).collection("zones")
      .onSnapshot({ includeMetadataChanges: true }, snapshot => {
        state.radarZonesData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderRadarManager();
      }, err => console.warn("Aviso radar zones:", err));
  } catch (err) {
    console.warn("Error escuchando radar:", err);
  }
}

export async function sendRemoteMagicPing(targetUser, silent = true) {
  if (!state.radarPingLoading) state.radarPingLoading = {};
  state.radarPingLoading[targetUser] = true;
  renderRadarManager();

  try {
    const now = Date.now();
    await rtdb.ref("locations/" + state.coupleId + "/pings/" + targetUser).set({
      targetDoc: targetUser,
      silent: silent,
      requestedAt: now,
      requestedBy: state.currentUser ? state.currentUser.username : "web_manager"
    });

    sendWsMessage({
      type: 'MAGIC_PACKET_PING',
      target: targetUser,
      silent: silent,
      timestamp: now
    });

    const pingText = silent ? '⚡ Magic Packet Silencioso enviado' : '🔔 Magic Packet con Notificación enviado';
    if (window.showToast) {
      window.showToast(`${pingText} a ${targetUser === 'kevin' ? 'Kevin' : 'Ali'}`);
    }
  } catch (err) {
    console.error("Error enviando magic ping:", err);
    if (window.customAlert) {
      window.customAlert(`Error al enviar paquete: ${err.message}`, "Error");
    }
  } finally {
    setTimeout(() => {
      state.radarPingLoading[targetUser] = false;
      renderRadarManager();
    }, 2500);
  }
}

export async function toggleRemoteRadar(targetUser, enable) {
  try {
    await rtdb.ref("locations/" + state.coupleId + "/users/" + targetUser).update({
      isSharing: enable,
      timestamp: Date.now()
    });

    if (window.showToast) {
      window.showToast(enable ? `⚡ Radar activado para ${targetUser}` : `🛑 Radar desactivado para ${targetUser}`);
    }
    renderRadarManager();
  } catch (err) {
    console.error("Error cambiando estado de radar:", err);
  }
}

export async function toggleRemoteSos(targetUser, active) {
  try {
    await rtdb.ref("locations/" + state.coupleId + "/users/" + targetUser).update({
      sosActive: active,
      sosTimestamp: active ? Date.now() : 0
    });

    if (window.showToast) {
      window.showToast(active ? `🚨 SOS activado para ${targetUser}` : `✅ SOS desactivado`);
    }
    renderRadarManager();
  } catch (err) {
    console.error("Error cambiando estado SOS:", err);
  }
}

export async function resetDevicePresence(targetUser) {
  try {
    await rtdb.ref("locations/" + state.coupleId + "/users/" + targetUser).update({
      timestamp: 0,
      isOnline: false
    });

    if (window.showToast) window.showToast(`🧹 Estado desconectado para ${targetUser}`);
    renderRadarManager();
  } catch (err) {
    console.error("Error al resetear presencia:", err);
  }
}

// Window bindings
window.initRadarView = initRadarView;
window.renderRadarManager = renderRadarManager;
window.initOrUpdateRadarMap = initOrUpdateRadarMap;
window.focusRadarUser = focusRadarUser;
window.fitRadarMapBounds = fitRadarMapBounds;
window.changeRadarTileLayer = changeRadarTileLayer;
window.sendRemoteMagicPing = sendRemoteMagicPing;
window.toggleRemoteRadar = toggleRemoteRadar;
window.toggleRemoteSos = toggleRemoteSos;
window.resetDevicePresence = resetDevicePresence;
