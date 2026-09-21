/**
 * radar-view.js - Vista de Radar GPS en Tiempo Real, Telemetría y Control Remoto Magic Packet
 */

import { state } from './state.js';
import { db, rtdb } from './firebase-config.js';
import { sendWsMessage } from './websocket.js';

let radarMap = null;
let kevinMarker = null;
let aliMarker = null;
let radarUnsubscribe = null;
let radarZonesUnsubscribe = null;

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
    return { cls: 'offline', text: 'Desconectado' };
  }
  const tsMillis = parseTimestampMillis(data.timestamp);
  const diffSecs = Math.floor((Date.now() - tsMillis) / 1000);
  
  const isSharing = data.isSharing !== undefined ? data.isSharing : data.isSharingLocation !== false;
  if (!isSharing) {
    return { cls: 'offline', text: 'Radar Apagado' };
  }
  if (diffSecs < 120) {
    return { cls: 'online', text: 'En Línea 🟢' };
  }
  if (diffSecs < 900) {
    return { cls: 'stale', text: `Inactivo (${Math.floor(diffSecs / 60)}m) 🟡` };
  }
  return { cls: 'offline', text: `Desconectado (${formatTimeAgo(tsMillis)}) ⚪` };
}

export function initRadarView() {
  listenToRadarLocations();
  renderRadarManager();
}

export function renderRadarManager() {
  const container = document.getElementById("radar-container") || document.getElementById("radarView");
  if (!container) return;

  const kevinData = state.radarUsersData?.kevin || {};
  const aliData = state.radarUsersData?.ali || {};
  const radarZonesData = state.radarZonesData || [];
  const coupleId = state.coupleId;

  container.innerHTML = `
    <div class="radar-dashboard">
      <!-- Hero Header -->
      <div class="radar-hero">
        <div>
          <h2 style="font-family:'Outfit',sans-serif; font-size:22px; font-weight:800; background:linear-gradient(135deg, #00e5ff, #e040fb); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
            📡 Panel de Dispositivos Conectados & Control de Thor Radar
          </h2>
          <p style="font-size:12px; color:var(--text-muted); margin-top:4px;">
            Supervisa el estado en tiempo real de Kevin y Ali con telemetría de batería, velocidad, actividad y soporte Magic Packet Wake-on-LAN.
          </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <span style="display:inline-flex; align-items:center; gap:6px; font-size:11px; font-weight:700; color:#10b981; background:rgba(16,185,129,0.1); border:1px solid rgba(16,185,129,0.3); padding:6px 10px; border-radius:20px;">
            <span style="width:7px; height:7px; border-radius:50%; background:#10b981; box-shadow:0 0 8px #10b981;"></span> Sincronización en vivo (1s)
          </span>
          <button class="btn btn-secondary" onclick="window.renderRadarManager()" style="font-size:12px; padding:8px 12px;">
            🔄 Refrescar Vista
          </button>
        </div>
      </div>

      <!-- Cards Grid -->
      <div class="radar-cards-grid">
        ${renderUserCard('kevin', 'Celular de Kevin', '#3b82f6', '🔵', kevinData)}
        ${renderUserCard('ali', 'Celular de Ali', '#ec4899', '🔴', aliData)}
      </div>

      <!-- Registered Safe Zones -->
      <div class="category-card" style="margin-bottom: 24px;">
        <div class="category-header">
          <div style="display:flex; align-items:center; gap:8px;">
            <span style="font-size:20px;">🏠</span>
            <h3 style="font-family:'Outfit',sans-serif; font-size:16px; font-weight:700; color:var(--text-color);">
              Zonas Seguras Registradas (${radarZonesData.length})
            </h3>
          </div>
        </div>
        <div style="padding: 16px; display:flex; flex-wrap:wrap; gap:10px;">
          ${radarZonesData.length === 0 ? `
            <div style="font-size:12px; color:var(--text-muted);">No hay zonas seguras creadas aún en Firestore (locations/${coupleId}/zones).</div>
          ` : radarZonesData.map(z => `
            <div style="background:rgba(0,0,0,0.3); border:1px solid var(--card-border); border-radius:10px; padding:10px 14px; display:flex; align-items:center; gap:10px;">
              <span style="font-size:22px;">${z.icon || '📍'}</span>
              <div>
                <div style="font-weight:700; font-size:13px; color:var(--text-color);">${z.name}</div>
                <div style="font-size:11px; color:var(--text-muted);">${Math.round(z.radiusMeters || 100)}m de radio • ${z.latitude ? z.latitude.toFixed(4) + ', ' + z.longitude.toFixed(4) : ''}</div>
              </div>
            </div>
          `).join('')}
        </div>
      </div>

      <!-- Raw Telemetry Debugger -->
      <details style="background:rgba(0,0,0,0.4); border:1px solid var(--card-border); border-radius:12px; padding:12px 16px; cursor:pointer;">
        <summary style="font-size:12px; font-weight:700; color:var(--text-muted); user-select:none;">
          🔍 Ver Telemetría JSON Cruda en Vivo (Debug)
        </summary>
        <pre style="margin-top:12px; padding:12px; background:#0d0e15; border-radius:8px; font-size:11px; color:#a78bfa; overflow-x:auto; font-family:monospace;">${JSON.stringify({ coupleId: coupleId, users: state.radarUsersData, zones: radarZonesData }, null, 2)}</pre>
      </details>
    </div>
  `;
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
            <h3 style="font-family:'Outfit',sans-serif; font-size:18px; font-weight:700; color:${color}; margin:0;">
              ${name} <span style="font-size:12px; color:var(--text-muted); font-weight:500;">(${data.userName || (userKey === 'kevin' ? 'Kevin' : 'Ali')})</span>
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
            <div style="margin-top: 4px;">
              <a href="${mapsUrl}" target="_blank" style="font-size: 11px; color: #60a5fa; text-decoration: none; display: inline-flex; align-items: center; gap: 4px;">
                🗺️ Abrir en Google Maps (${lat.toFixed(4)}, ${lon.toFixed(4)} ±${accuracy}m) ↗
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
window.sendRemoteMagicPing = sendRemoteMagicPing;
window.toggleRemoteRadar = toggleRemoteRadar;
window.toggleRemoteSos = toggleRemoteSos;
window.resetDevicePresence = resetDevicePresence;
