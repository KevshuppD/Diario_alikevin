// ==========================================
// CONFIGURATION & SYSTEM DASHBOARD (/configuracion)
// ==========================================

import { state } from './state.js';
import { LIGHT_COLOR_FAMILIES, DARK_COLOR_FAMILIES, MONO_COLOR_FAMILIES } from './constants.js';
import { db } from './firebase-config.js';

export function renderConfigEditor() {
  const container = document.getElementById("config-container") || document.getElementById("configManagerContainer");
  if (!container) return;

  const isDark = state.userTheme === "Pixel Oscuro";
  const isLight = state.userTheme === "Pixel Claro";
  const isMono = state.userTheme === "Pixel Monocromático";
  const families = isMono ? MONO_COLOR_FAMILIES : (isLight ? LIGHT_COLOR_FAMILIES : DARK_COLOR_FAMILIES);
  const activeColor = isLight ? state.userLightColor : state.userDarkColor;

  let paletteFamiliesHtml = families.map(fam => `
    <div style="margin-bottom: 12px; width: 100%;">
      <div style="font-size: 11px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.5px;">${fam.title}</div>
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
        ${fam.colors.map(hex => `
          <div onclick="window.changeBarColor('${hex}')" style="width: 32px; height: 32px; border-radius: 50%; background-color: ${hex}; border: 3px solid ${activeColor.toUpperCase() === hex.toUpperCase() ? '#fff' : 'transparent'}; cursor: pointer; box-shadow: 0 3px 8px rgba(0,0,0,0.3); transition: transform 0.2s; position: relative;" title="${hex}">
            ${activeColor.toUpperCase() === hex.toUpperCase() ? '<span style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 11px; color: ' + (hex === '#FFFFFF' || hex === '#FFF59D' || hex === '#E1F5FE' ? '#000' : '#fff') + ';">✓</span>' : ''}
          </div>
        `).join('')}
      </div>
    </div>
  `).join('');

  const dbStatusText = state.latestDbStatus ? (state.latestDbStatus.isFromCache ? "Firestore (Caché Local) 🟢" : (state.latestDbStatus.isOnline ? "Firestore Conectado 🟢" : "Offline (Sin red)")) : "Firestore Conectado 🟢";
  const dbStatusClass = state.latestDbStatus && !state.latestDbStatus.isOnline ? "offline" : "online";
  const wsStatusText = state.latestWsStatus ? state.latestWsStatus.text : "WS: En vivo (1)";
  const wsStatusClass = state.latestWsStatus && state.latestWsStatus.connected === false ? "offline" : "online";

  container.innerHTML = `
    <div class="radar-dashboard" style="padding-top: 0;">
      
      <!-- Master Header -->
      <div class="radar-hero" style="margin-bottom: 24px;">
        <div>
          <h2 style="font-family:'Outfit',sans-serif; font-size:22px; font-weight:800; background:linear-gradient(135deg, #00e5ff, #e040fb); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
            ⚙️ Centro de Configuración & Estado del Sistema
          </h2>
          <p style="font-size:12px; color:var(--text-muted); margin-top:4px;">
            Supervisa el estado de la conexión, el consumo de cuotas de Firestore, personaliza temas pixel-art y realiza mantenimiento.
          </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <div class="autosave-status-pill" id="config-autosave-pill" title="Los cambios se guardan automáticamente en Firestore y se sincronizan en vivo">
            <span class="autosave-icon" id="config-autosave-icon">✓</span>
            <span class="autosave-text" id="config-autosave-text">Autoguardado Activo</span>
          </div>
          <button class="btn btn-secondary" onclick="window.renderConfigEditor()" style="font-size:12px; padding:8px 12px;">
            🔄 Refrescar Configuración
          </button>
        </div>
      </div>

      <!-- SECTION 1: SYSTEM CONNECTIVITY & REAL-TIME STATUS BADGES -->
      <div class="db-inspector-card" style="margin-bottom: 24px; border-left: 4px solid #10b981;">
        <div class="db-inspector-title" style="margin-bottom: 14px;">
          <span>⚡</span> Estado de Conexión en Tiempo Real
        </div>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 16px;">
          Indicadores en vivo de la conexión con Cloud Firestore y el canal de sincronización WebSockets.
        </p>

        <div style="display: flex; gap: 14px; flex-wrap: wrap; align-items: center; margin-bottom: 14px;">
          <!-- Firestore Status Pill -->
          <div class="connection-status-pill" id="db-status-pill" style="padding: 10px 16px; font-size: 13px; background: rgba(0,0,0,0.3);">
            <span class="status-dot ${dbStatusClass}" id="db-status-dot"></span>
            <span class="status-text" id="db-status-text" style="font-weight: 700;">${dbStatusText}</span>
          </div>

          <!-- WebSocket Live Status Pill -->
          <div class="connection-status-pill ws-pill" id="ws-status-pill" style="padding: 10px 16px; font-size: 13px; background: rgba(0,0,0,0.3);" title="Sincronización en tiempo real por WebSockets">
            <span class="status-dot ${wsStatusClass}" id="ws-status-dot"></span>
            <span class="status-text" id="ws-status-text" style="font-weight: 700;">${wsStatusText}</span>
          </div>
        </div>

        <div style="font-size: 12px; color: var(--text-muted); background: rgba(0,0,0,0.2); border-radius: 8px; padding: 10px 14px; display: flex; gap: 16px; flex-wrap: wrap;">
          <div>🔗 Vínculo de Pareja: <strong style="color: var(--text-color);">${state.coupleId}</strong></div>
          <div>🌟 Temporada Activa: <strong style="color: #e040fb;">Temporada ${state.currentSeason}</strong></div>
          <div>👤 Usuario Activo: <strong style="color: ${state.currentUser?.username === 'kevin' ? '#60a5fa' : '#f472b6'};">${state.currentUser ? state.currentUser.name : 'Kevin'}</strong></div>
        </div>
      </div>

      <!-- SECTION 2: FIRESTORE SPARK QUOTA METRICS DASHBOARD -->
      <div class="db-inspector-card" style="margin-bottom: 24px; border-left: 4px solid #3b82f6;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
          <div class="db-inspector-title" style="margin-bottom: 0;">
            <span>📊</span> Monitoreo de Cuotas Cloud Firestore (Spark Plan Gratuito)
          </div>
          <a href="https://console.firebase.google.com/project/diario-ali-kevin/firestore/usage" target="_blank" class="btn" style="background: linear-gradient(135deg, #1e40af, #3b82f6); color: #fff; text-decoration: none; padding: 8px 16px; font-size: 13px; font-weight: 700; border-radius: 8px; box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4); display: inline-flex; align-items: center; gap: 8px;" title="Abre el panel de métricas en tiempo real en la consola oficial de Google Firebase">
            <span>📊</span> Ver Consumo Exacto en Firebase Console ↗
          </a>
        </div>

        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 18px; line-height: 1.5;">
          Las cuotas gratuitas del plan Spark se reinician automáticamente todos los días a las <strong>00:00 PDT (04:00 AM hora de Chile / UTC-3)</strong>. Thor Radar utiliza <strong>Smart Throttling adaptativo</strong> para garantizar que el consumo se mantenga siempre dentro del rango seguro (&lt;6% de la cuota diaria).
        </p>

        <div class="db-stats-grid" style="margin-bottom: 16px;">
          <div class="db-stat-box" style="border-top: 2px solid #10b981;">
            <div class="db-stat-number" style="color: #10b981;">50,000</div>
            <div class="db-stat-name">Lecturas / Día</div>
            <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Optimizado por caché local</div>
          </div>
          <div class="db-stat-box" style="border-top: 2px solid #3b82f6;">
            <div class="db-stat-number" style="color: #60a5fa;">20,000</div>
            <div class="db-stat-name">Escrituras / Día</div>
            <div style="font-size: 10px; color: #10b981; margin-top: 4px;">~800-1,200/día uso (&lt;6%)</div>
          </div>
          <div class="db-stat-box" style="border-top: 2px solid #a855f7;">
            <div class="db-stat-number" style="color: #c084fc;">20,000</div>
            <div class="db-stat-name">Eliminaciones / Día</div>
            <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Consumo casi 0</div>
          </div>
          <div class="db-stat-box" style="border-top: 2px solid #f59e0b;">
            <div class="db-stat-number" style="color: #fbbf24;">1.0 GiB</div>
            <div class="db-stat-name">Almacenamiento Libre</div>
            <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Base de datos ultra ligera</div>
          </div>
        </div>

        <div style="background: rgba(0, 0, 0, 0.25); border: 1px solid var(--card-border); border-radius: 8px; padding: 10px 14px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px;">
          <div style="display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-color);">
            <span style="font-size: 16px;">⚡</span>
            <span><strong>Thor Radar Smart Throttling:</strong> En movimiento sube cada 1 min (≥40m) • En reposo emite latido cada 6 min • Con Radar Apagado gasto es estrictamente 0.</span>
          </div>
          <span style="font-size: 11px; font-weight: 700; color: #10b981; background: rgba(16, 185, 129, 0.15); padding: 4px 8px; border-radius: 6px;">Estado: Seguro (&lt;6% consumo)</span>
        </div>
      </div>

      <!-- SECTION 3: THEME & PIXEL ART STYLING -->
      <div class="db-inspector-card" style="margin-bottom: 24px;">
        <div class="db-inspector-title" style="margin-bottom: 14px;">
          <span>🎨</span> Personalización Visual y Tema Pixel-Art
        </div>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 18px;">
          Elige el estilo visual y colores preferidos. Los cambios se sincronizan en tiempo real con tu usuario en la aplicación Android.
        </p>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 20px;">
          
          <!-- Pixel Oscuro Box -->
          <div onclick="window.changeTheme('Pixel Oscuro')" style="background: ${isDark ? 'rgba(156, 39, 176, 0.25)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isDark ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 14px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>🌙 Pixel Oscuro</span>
              ${isDark ? '<span style="color:#e040fb;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 11px; color: var(--text-muted);">Fondo oscuro arcade con destellos neón y acentos vibrantes.</div>
          </div>

          <!-- Pixel Claro Box -->
          <div onclick="window.changeTheme('Pixel Claro')" style="background: ${isLight ? 'rgba(124, 77, 255, 0.25)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isLight ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 14px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>☀️ Pixel Claro</span>
              ${isLight ? '<span style="color:#7c4dff;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 11px; color: var(--text-muted);">Estilo pergamino retro cálido y suave a la vista de día.</div>
          </div>

          <!-- Pixel Monocromatico Box -->
          <div onclick="window.changeTheme('Pixel Monocromático')" style="background: ${isMono ? 'rgba(255, 255, 255, 0.15)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isMono ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 14px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>🏁 Monocromático</span>
              ${isMono ? '<span style="color:#fff;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 11px; color: var(--text-muted);">Paleta pura 8-bit inspirada en pantallas retro Game Boy.</div>
          </div>
        </div>

        <!-- Color Palette Families -->
        <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 16px; margin-bottom: 16px;">
          <div style="font-size: 12px; font-weight: 700; color: var(--text-color); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">
            🎨 Color de Acento & Barra:
          </div>
          ${paletteFamiliesHtml}
        </div>

        <!-- Refresh Rate & Background Preferences -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px;" class="config-grid-layout">
          <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 14px;">
            <div style="font-size: 12px; font-weight: 700; color: var(--text-color); margin-bottom: 10px;">⚡ Tasa de Refresco (App Android)</div>
            <div style="display: flex; gap: 8px;">
              ${[60, 90, 100, 120].map(hz => `
                <button type="button" onclick="window.changeRefreshRate(${hz})" class="btn ${state.userRefreshRate === hz ? '' : 'btn-secondary'}" style="flex: 1; padding: 6px 4px; font-size: 12px; font-weight: 700; justify-content: center;">
                  ${hz}Hz
                </button>
              `).join('')}
            </div>
          </div>

          <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 14px; display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 12px; font-weight: 700; color: var(--text-color);">✨ Fondo Adaptativo Suave</div>
              <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">Armoniza el fondo con el color de acento</div>
            </div>
            <button type="button" onclick="window.toggleCustomBgPreference()" class="btn ${state.userUseCustomBg ? 'btn-success' : 'btn-secondary'}" style="padding: 6px 12px; font-size: 12px; font-weight: 700;">
              ${state.userUseCustomBg ? '✓ Activado' : '○ Desactivado'}
            </button>
          </div>
        </div>
      </div>

      <!-- SECTION 4: CLOUDINARY & SYSTEM MAINTENANCE -->
      <div class="db-inspector-card">
        <div class="db-inspector-title" style="margin-bottom: 12px;">
          <span>💾</span> Mantenimiento y Almacenamiento
        </div>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 16px;">
          Herramientas de optimización de imágenes en Cloudinary y configuración de persistencia del navegador.
        </p>
        <div style="display: flex; gap: 12px; flex-wrap: wrap;">
          <button class="btn btn-secondary" onclick="window.openCloudinarySettingsModal()" style="padding: 10px 18px; font-size: 13px;">
            <span>⚙️</span> Asistente de Cloudinary & Limpieza
          </button>
          <button class="btn btn-secondary" onclick="window.openPersistentStorageModalManual()" style="padding: 10px 18px; font-size: 13px; border-color: #10b981; color: #10b981;">
            <span>🛡️</span> Protección de Caché Local Persistente
          </button>
        </div>
      </div>

    </div>
  `;
}

export function applyTheme(themeName, lightColor, darkColor, useCustomBg) {
  document.body.classList.remove('theme-pixel-claro', 'theme-pixel-oscuro', 'theme-pixel-monocromatico');
  if (themeName === "Pixel Claro") {
    document.body.classList.add('theme-pixel-claro');
  } else if (themeName === "Pixel Monocromático") {
    document.body.classList.add('theme-pixel-monocromatico');
  } else {
    document.body.classList.add('theme-pixel-oscuro');
  }

  const isLight = themeName === "Pixel Claro";
  const activeColor = isLight ? (lightColor || state.userLightColor) : (darkColor || state.userDarkColor);
  if (activeColor) {
    document.documentElement.style.setProperty('--accent-color', activeColor);
  }
}

export function changeTheme(themeName) {
  state.userTheme = themeName;
  localStorage.setItem("userTheme", themeName);
  applyTheme(themeName, state.userLightColor, state.userDarkColor, state.userUseCustomBg);
  renderConfigEditor();

  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      appTheme: themeName
    }, { merge: true }).catch(err => console.error("Error guardando tema:", err));
  }
}

export function changeBarColor(hexColor) {
  const isLight = state.userTheme === "Pixel Claro";
  if (isLight) {
    state.userLightColor = hexColor;
    localStorage.setItem("userLightColor", hexColor);
  } else {
    state.userDarkColor = hexColor;
    localStorage.setItem("userDarkColor", hexColor);
  }
  applyTheme(state.userTheme, state.userLightColor, state.userDarkColor, state.userUseCustomBg);
  renderConfigEditor();

  if (state.currentUser) {
    const field = isLight ? "barColorLight" : "barColorDark";
    db.collection("users").doc(state.currentUser.docId).set({
      [field]: hexColor
    }, { merge: true }).catch(err => console.error("Error guardando color:", err));
  }
}

export function changeRefreshRate(hz) {
  state.userRefreshRate = hz;
  localStorage.setItem("userRefreshRate", String(hz));
  renderConfigEditor();
  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      refreshRate: hz
    }, { merge: true }).catch(err => console.error("Error guardando hz:", err));
  }
}

export function toggleCustomBgPreference() {
  state.userUseCustomBg = !state.userUseCustomBg;
  localStorage.setItem("userUseCustomBg", String(state.userUseCustomBg));
  renderConfigEditor();
  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      useCustomBackground: state.userUseCustomBg
    }, { merge: true }).catch(err => console.error("Error guardando bg pref:", err));
  }
}

export const renderConfigView = renderConfigEditor;

