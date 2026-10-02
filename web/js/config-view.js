// ==========================================
// CONFIGURATION & SYSTEM DASHBOARD (/configuracion)
// ==========================================

import { state } from './state.js';
import { LIGHT_COLOR_FAMILIES, DARK_COLOR_FAMILIES, MONO_COLOR_FAMILIES } from './constants.js';
import { db } from './firebase-config.js';

let showTechnicalSettings = false;

export function toggleTechnicalSettings() {
  showTechnicalSettings = !showTechnicalSettings;
  renderConfigEditor();
}

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
          <h2 style="font-family:'VT323',monospace; font-size:28px; font-weight:700; background:linear-gradient(135deg, #00e5ff, #e040fb); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
            ⚙️ Centro de Configuración & Preferencias
          </h2>
          <p style="font-size:16px; color:var(--text-muted); margin-top:4px;">
            Personaliza la apariencia visual, administra tu perfil de usuario y configura las opciones del sistema.
          </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <div class="autosave-status-pill" id="config-autosave-pill" title="Los cambios se guardan automáticamente en Firestore y se sincronizan en vivo">
            <span class="autosave-icon" id="config-autosave-icon">✓</span>
            <span class="autosave-text" id="config-autosave-text">Autoguardado Activo</span>
          </div>
          <button class="btn btn-secondary" onclick="window.renderConfigEditor()" style="font-size:14px; padding:8px 12px;">
            🔄 Refrescar
          </button>
        </div>
      </div>

      <!-- SECCIÓN 1: PERFIL & SESIÓN DE USUARIO -->
      <div class="db-inspector-card" style="margin-bottom: 24px; border-left: 4px solid #ec4899;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
          <div class="db-inspector-title" style="margin-bottom: 0;">
            <span>👤</span> Perfil y Sesión Activa
          </div>
          <button type="button" class="btn" onclick="window.logoutSession()" style="background: rgba(239, 68, 68, 0.2); border-color: #ef4444; color: #f87171; font-size: 14px; padding: 6px 14px; border-radius: 8px;">
            🚪 Cerrar Sesión
          </button>
        </div>
        <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 16px;">
          El perfil seleccionado define a quién se asignan tus espíritus marcados, tus maestrías y tus solicitudes de radar.
        </p>

        <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
          <button type="button" class="btn ${state.currentUser?.username === 'kevin' ? '' : 'btn-secondary'}" onclick="window.switchUserProfile('kevin')" style="padding: 10px 18px; font-size: 15px; font-weight: 700; border-color: #3b82f6; color: #60a5fa; background: ${state.currentUser?.username === 'kevin' ? 'rgba(59, 130, 246, 0.25)' : 'rgba(59, 130, 246, 0.08)'};">
            🔵 Perfil Kevin ${state.currentUser?.username === 'kevin' ? '✓ (Activo)' : ''}
          </button>
          <button type="button" class="btn ${state.currentUser?.username === 'ali' ? '' : 'btn-secondary'}" onclick="window.switchUserProfile('ali')" style="padding: 10px 18px; font-size: 15px; font-weight: 700; border-color: #ec4899; color: #f472b6; background: ${state.currentUser?.username === 'ali' ? 'rgba(236, 72, 153, 0.25)' : 'rgba(236, 72, 153, 0.08)'};">
            🔴 Perfil Ali ${state.currentUser?.username === 'ali' ? '✓ (Activo)' : ''}
          </button>
        </div>
      </div>

      <!-- SECCIÓN 2: PERSONALIZACIÓN VISUAL & ESTILO -->
      <div class="db-inspector-card" style="margin-bottom: 24px; border-left: 4px solid #a855f7;">
        <div class="db-inspector-title" style="margin-bottom: 14px;">
          <span>🎨</span> Personalización Visual y Estilo
        </div>
        <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 18px;">
          Personaliza la tipografía, los temas y la paleta de colores para una experiencia a tu medida.
        </p>

        <!-- Typography / Font Preference -->
        <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 16px; margin-bottom: 16px;">
          <div style="font-size: 15px; font-weight: 700; color: var(--text-color); margin-bottom: 8px; display: flex; align-items: center; gap: 8px;">
            <span>🔤</span> Estilo de Tipografía y Letra:
          </div>
          <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;">
            Elige si prefieres la fuente pixel-art idéntica a la app Android (VT323) o una tipografía moderna estilizada (Inter / Outfit).
          </p>
          <div style="display: flex; gap: 12px; flex-wrap: wrap;">
            <button type="button" onclick="window.changeFont('pixel')" class="btn ${state.userFont !== 'modern' ? '' : 'btn-secondary'}" style="flex: 1; min-width: 180px; padding: 12px; font-size: 16px; font-weight: 700; border-color: #a855f7; color: ${state.userFont !== 'modern' ? '#fff' : '#c084fc'}; background: ${state.userFont !== 'modern' ? 'rgba(168, 85, 247, 0.25)' : 'rgba(168, 85, 247, 0.08)'};">
              👾 Pixel-Art (VT323) ${state.userFont !== 'modern' ? '✓' : ''}
            </button>
            <button type="button" onclick="window.changeFont('modern')" class="btn ${state.userFont === 'modern' ? '' : 'btn-secondary'}" style="flex: 1; min-width: 180px; padding: 12px; font-size: 14px; font-weight: 700; border-color: #3b82f6; color: ${state.userFont === 'modern' ? '#fff' : '#60a5fa'}; background: ${state.userFont === 'modern' ? 'rgba(59, 130, 246, 0.25)' : 'rgba(59, 130, 246, 0.08)'};">
              🔤 Moderna (Inter / Outfit) ${state.userFont === 'modern' ? '✓' : ''}
            </button>
          </div>
        </div>

        <!-- Themes Grid -->
        <div style="font-size: 14px; font-weight: 700; color: var(--text-color); margin-bottom: 10px;">
          <span>🎭</span> Tema de Interfaz:
        </div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 20px;">
          <!-- Pixel Oscuro Box -->
          <div onclick="window.changeTheme('Pixel Oscuro')" style="background: ${isDark ? 'rgba(156, 39, 176, 0.25)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isDark ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 15px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>🌙 Pixel Oscuro</span>
              ${isDark ? '<span style="color:#e040fb;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 12px; color: var(--text-muted);">Fondo oscuro arcade con destellos neón y acentos vibrantes.</div>
          </div>

          <!-- Pixel Claro Box -->
          <div onclick="window.changeTheme('Pixel Claro')" style="background: ${isLight ? 'rgba(124, 77, 255, 0.25)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isLight ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 15px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>☀️ Pixel Claro</span>
              ${isLight ? '<span style="color:#7c4dff;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 12px; color: var(--text-muted);">Estilo pergamino retro cálido y suave a la vista de día.</div>
          </div>

          <!-- Pixel Monocromatico Box -->
          <div onclick="window.changeTheme('Pixel Monocromático')" style="background: ${isMono ? 'rgba(255, 255, 255, 0.15)' : 'rgba(255,255,255,0.03)'}; border: 2px solid ${isMono ? 'var(--accent-color)' : 'var(--card-border)'}; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s;">
            <div style="font-weight: 700; font-size: 15px; color: var(--text-color); margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
              <span>🏁 Monocromático</span>
              ${isMono ? '<span style="color:#fff;">✓ Activo</span>' : ''}
            </div>
            <div style="font-size: 12px; color: var(--text-muted);">Paleta pura 8-bit inspirada en pantallas retro Game Boy.</div>
          </div>
        </div>

        <!-- Color Palette Families -->
        <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 16px; margin-bottom: 16px;">
          <div style="font-size: 13px; font-weight: 700; color: var(--text-color); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">
            🎨 Color de Acento & Barra:
          </div>
          ${paletteFamiliesHtml}
        </div>

        <!-- Refresh Rate & Background Preferences -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px;" class="config-grid-layout">
          <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 14px;">
            <div style="font-size: 13px; font-weight: 700; color: var(--text-color); margin-bottom: 10px;">⚡ Tasa de Refresco (App Android)</div>
            <div style="display: flex; gap: 8px;">
              ${[60, 90, 100, 120].map(hz => `
                <button type="button" onclick="window.changeRefreshRate(${hz})" class="btn ${state.userRefreshRate === hz ? '' : 'btn-secondary'}" style="flex: 1; padding: 6px 4px; font-size: 13px; font-weight: 700; justify-content: center;">
                  ${hz}Hz
                </button>
              `).join('')}
            </div>
          </div>

          <div style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--card-border); border-radius: 10px; padding: 14px; display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 13px; font-weight: 700; color: var(--text-color);">✨ Fondo Adaptativo Suave</div>
              <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">Armoniza el fondo con el color de acento</div>
            </div>
            <button type="button" onclick="window.toggleCustomBgPreference()" class="btn ${state.userUseCustomBg ? 'btn-success' : 'btn-secondary'}" style="padding: 6px 12px; font-size: 13px; font-weight: 700;">
              ${state.userUseCustomBg ? '✓ Activado' : '○ Desactivado'}
            </button>
          </div>
        </div>
      </div>

      <!-- SECCIÓN 3: AJUSTES TÉCNICOS & CONEXIÓN (COLAPSABLE / HABILITABLE) -->
      <div class="db-inspector-card" style="margin-bottom: 24px; border-left: 4px solid #3b82f6;">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
          <div>
            <div class="db-inspector-title" style="margin-bottom: 2px;">
              <span>🛠️</span> Opciones Técnicas y Métricas Avanzadas
            </div>
            <p style="font-size: 13px; color: var(--text-muted); margin: 0;">
              Cuotas de Cloud Firestore, estado de WebSocket y herramientas de almacenamiento.
            </p>
          </div>
          <button type="button" class="btn btn-secondary" onclick="window.toggleTechnicalSettings()" style="font-size: 14px; padding: 8px 16px; border-color: rgba(59, 130, 246, 0.4); color: #60a5fa;">
            ${showTechnicalSettings ? '▲ Ocultar Opciones Técnicas' : '▼ Mostrar Opciones Técnicas'}
          </button>
        </div>

        ${showTechnicalSettings ? `
          <div style="margin-top: 20px; border-top: 1px solid var(--card-border); padding-top: 18px;">
            
            <!-- Estado de Conexión en Vivo -->
            <div style="margin-bottom: 18px;">
              <div style="font-size: 13px; font-weight: 700; color: var(--text-color); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px;">
                ⚡ Estado de Conexión en Vivo:
              </div>
              <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
                <div class="connection-status-pill" id="db-status-pill" style="padding: 8px 14px; font-size: 13px; background: rgba(0,0,0,0.3);">
                  <span class="status-dot ${dbStatusClass}" id="db-status-dot"></span>
                  <span class="status-text" id="db-status-text" style="font-weight: 700;">${dbStatusText}</span>
                </div>
                <div class="connection-status-pill ws-pill" id="ws-status-pill" style="padding: 8px 14px; font-size: 13px; background: rgba(0,0,0,0.3);">
                  <span class="status-dot ${wsStatusClass}" id="ws-status-dot"></span>
                  <span class="status-text" id="ws-status-text" style="font-weight: 700;">${wsStatusText}</span>
                </div>
              </div>
            </div>

            <!-- Vínculo & Temporada -->
            <div style="font-size: 12px; color: var(--text-muted); background: rgba(0,0,0,0.25); border-radius: 8px; padding: 10px 14px; display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 18px;">
              <div>🔗 Vínculo Firestore: <strong style="color: var(--text-color);">${state.coupleId}</strong></div>
              <div>🌟 Colección Activa: <strong style="color: #e040fb;">${state.currentSeason === 1 ? 'fortnite_spirits' : 'fortnite_spirits_s2'}</strong></div>
            </div>

            <!-- Cuotas Spark Plan -->
            <div style="margin-bottom: 18px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
                <div style="font-size: 13px; font-weight: 700; color: var(--text-color); text-transform: uppercase; letter-spacing: 0.5px;">
                  📊 Consumo Cloud Firestore (Spark Plan Gratuito)
                </div>
                <a href="https://console.firebase.google.com/project/diario-ali-kevin/firestore/usage" target="_blank" class="btn" style="background: linear-gradient(135deg, #1e40af, #3b82f6); color: #fff; text-decoration: none; padding: 6px 14px; font-size: 12px; font-weight: 700; border-radius: 8px;">
                  Ver Consumo en Firebase Console ↗
                </a>
              </div>

              <div class="db-stats-grid" style="margin-bottom: 12px;">
                <div class="db-stat-box" style="border-top: 2px solid #10b981;">
                  <div class="db-stat-number" style="color: #10b981; font-size: 24px;">50,000</div>
                  <div class="db-stat-name">Lecturas / Día</div>
                </div>
                <div class="db-stat-box" style="border-top: 2px solid #3b82f6;">
                  <div class="db-stat-number" style="color: #60a5fa; font-size: 24px;">20,000</div>
                  <div class="db-stat-name">Escrituras / Día</div>
                </div>
                <div class="db-stat-box" style="border-top: 2px solid #a855f7;">
                  <div class="db-stat-number" style="color: #c084fc; font-size: 24px;">20,000</div>
                  <div class="db-stat-name">Eliminaciones / Día</div>
                </div>
                <div class="db-stat-box" style="border-top: 2px solid #f59e0b;">
                  <div class="db-stat-number" style="color: #fbbf24; font-size: 24px;">1.0 GiB</div>
                  <div class="db-stat-name">Almacenamiento</div>
                </div>
              </div>
            </div>

            <!-- Mantenimiento y Cloudinary -->
            <div>
              <div style="font-size: 13px; font-weight: 700; color: var(--text-color); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px;">
                💾 Mantenimiento & Caché:
              </div>
              <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                <button class="btn btn-secondary" onclick="window.openCloudinarySettingsModal()" style="padding: 8px 16px; font-size: 13px;">
                  <span>⚙️</span> Limpieza Cloudinary
                </button>
                <button class="btn btn-secondary" onclick="window.openPersistentStorageModalManual()" style="padding: 8px 16px; font-size: 13px; border-color: #10b981; color: #10b981;">
                  <span>🛡️</span> Caché Local Persistente
                </button>
              </div>
            </div>

          </div>
        ` : ''}
      </div>

    </div>
  `;
}

function hexToRgba(hex, alpha) {
  if (!hex || typeof hex !== 'string') return `rgba(156, 39, 176, ${alpha})`;
  let c = hex.replace('#', '');
  if (c.length === 3) c = c.split('').map(x => x + x).join('');
  const num = parseInt(c, 16);
  if (isNaN(num)) return `rgba(156, 39, 176, ${alpha})`;
  return `rgba(${(num >> 16) & 255}, ${(num >> 8) & 255}, ${num & 255}, ${alpha})`;
}

export function applyTheme(themeName, lightColor, darkColor, useCustomBg) {
  const theme = themeName || state.userTheme || "Pixel Oscuro";
  document.body.classList.remove('theme-pixel-claro', 'theme-pixel-oscuro', 'theme-pixel-monocromatico');
  if (theme === "Pixel Claro") {
    document.body.classList.add('theme-pixel-claro');
  } else if (theme === "Pixel Monocromático") {
    document.body.classList.add('theme-pixel-monocromatico');
  } else {
    document.body.classList.add('theme-pixel-oscuro');
  }

  const isLight = theme === "Pixel Claro";
  const activeColor = isLight ? (lightColor || state.userLightColor || "#D1C4E9") : (darkColor || state.userDarkColor || "#4A148C");
  if (activeColor) {
    document.documentElement.style.setProperty('--accent-color', activeColor);
    document.documentElement.style.setProperty('--accent-glow', hexToRgba(activeColor, 0.35));
  }
}

export function changeTheme(themeName) {
  state.userTheme = themeName;
  localStorage.setItem("userTheme", themeName);
  applyTheme(themeName, state.userLightColor, state.userDarkColor, state.userUseCustomBg);
  renderConfigEditor();

  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      theme: themeName,
      appTheme: themeName
    }, { merge: true }).catch(err => console.error("Error guardando tema en Firestore:", err));
  }
  if (window.showToast) {
    window.showToast(`🎭 Tema cambiado a: ${themeName}`);
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
    const updateObj = isLight 
      ? { lightColor: hexColor, barColorLight: hexColor }
      : { darkColor: hexColor, barColorDark: hexColor };
    db.collection("users").doc(state.currentUser.docId).set(updateObj, { merge: true })
      .catch(err => console.error("Error guardando color en Firestore:", err));
  }
  if (window.showToast) {
    window.showToast(`🎨 Color actualizado: ${hexColor}`);
  }
}

export function changeRefreshRate(hz) {
  state.userRefreshRate = hz;
  localStorage.setItem("userRefreshRate", String(hz));
  renderConfigEditor();
  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      refreshRate: hz
    }, { merge: true }).catch(err => console.error("Error guardando hz en Firestore:", err));
  }
  if (window.showToast) {
    window.showToast(`⚡ Frecuencia de refresco configurada a: ${hz}Hz`);
  }
}

export function toggleCustomBgPreference() {
  state.userUseCustomBg = !state.userUseCustomBg;
  localStorage.setItem("userUseCustomBg", String(state.userUseCustomBg));
  applyTheme(state.userTheme, state.userLightColor, state.userDarkColor, state.userUseCustomBg);
  renderConfigEditor();
  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      useCustomBg: state.userUseCustomBg,
      useCustomBackground: state.userUseCustomBg
    }, { merge: true }).catch(err => console.error("Error guardando preferencia de fondo:", err));
  }
  if (window.showToast) {
    window.showToast(`✨ Fondo adaptativo ${state.userUseCustomBg ? 'Activado' : 'Desactivado'}`);
  }
}

export function applyUserFont(fontName) {
  const font = fontName || state.userFont || "pixel";
  document.body.classList.remove('font-pixel', 'font-modern');
  if (font === 'modern') {
    document.body.classList.add('font-modern');
  } else {
    document.body.classList.add('font-pixel');
  }
}

export function changeFont(fontName) {
  state.userFont = fontName;
  localStorage.setItem("userFont", fontName);
  applyUserFont(fontName);
  renderConfigEditor();
  if (window.showToast) {
    window.showToast(`🔤 Tipografía cambiada a: ${fontName === 'modern' ? 'Moderna (Inter/Outfit)' : 'Pixel-Art (VT323)'}`);
  }
  if (state.currentUser) {
    db.collection("users").doc(state.currentUser.docId).set({
      fontPreference: fontName
    }, { merge: true }).catch(err => console.error("Error guardando fuente en Firestore:", err));
  }
}

export const renderConfigView = renderConfigEditor;

// Window global assignments
window.renderConfigEditor = renderConfigEditor;
window.renderConfigView = renderConfigView;
window.changeTheme = changeTheme;
window.changeFont = changeFont;
window.applyUserFont = applyUserFont;
window.applyTheme = applyTheme;
window.changeBarColor = changeBarColor;
window.changeRefreshRate = changeRefreshRate;
window.toggleCustomBgPreference = toggleCustomBgPreference;
window.toggleTechnicalSettings = toggleTechnicalSettings;


