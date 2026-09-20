// ==========================================
// WEBSOCKET CLIENT & REALTIME EVENT DISPATCHER
// ==========================================

import { state } from './state.js';

let wsClient = null;
let wsReconnectTimer = null;

export function initWebSocket(onSyncCallback, onToggleCallback, onImageCallback) {
  if (wsClient && (wsClient.readyState === WebSocket.OPEN || wsClient.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  const wsUrl = `${protocol}//${host}/ws`;

  try {
    wsClient = new WebSocket(wsUrl);

    wsClient.onopen = () => {
      console.log("🔌 Conectado a WebSocket en tiempo real");
      updateWsStatus(true, `WS: En vivo (${state.latestWsStatus.count || 1})`, "Sincronización instantánea activa");
      if (wsReconnectTimer) {
        clearTimeout(wsReconnectTimer);
        wsReconnectTimer = null;
      }
    };

    wsClient.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        handleWsMessage(msg, onSyncCallback, onToggleCallback, onImageCallback);
      } catch (err) {
        console.error("Error parseando mensaje WebSocket:", err);
      }
    };

    wsClient.onclose = () => {
      updateWsStatus(false, "WS: Reconectando...", "Intentando reconectar...");
      scheduleWsReconnect(onSyncCallback, onToggleCallback, onImageCallback);
    };

    wsClient.onerror = (err) => {
      console.warn("Aviso en conexión WebSocket:", err);
      updateWsStatus(false, "WS: Sin conexión", "Error de red en WebSockets");
    };
  } catch (err) {
    console.error("No se pudo iniciar WebSocket:", err);
    scheduleWsReconnect(onSyncCallback, onToggleCallback, onImageCallback);
  }
}

function scheduleWsReconnect(onSync, onToggle, onImg) {
  if (!wsReconnectTimer) {
    wsReconnectTimer = setTimeout(() => initWebSocket(onSync, onToggle, onImg), 4000);
  }
}

export function sendWsMessage(data) {
  if (wsClient && wsClient.readyState === WebSocket.OPEN) {
    try {
      wsClient.send(JSON.stringify(data));
    } catch(e) {
      console.warn("Error enviando mensaje WS:", e);
    }
  }
}

export const sendWebSocketBroadcast = sendWsMessage;

export function updateWsStatus(connected, text, tooltip) {
  state.latestWsStatus = {
    connected,
    text: text || (connected ? `WS: En vivo (${state.latestWsStatus.count || 1})` : "WS: Desconectado"),
    count: state.latestWsStatus.count || 1,
    tooltip: tooltip || ""
  };
  const dot = document.getElementById("ws-status-dot");
  const label = document.getElementById("ws-status-text");
  const pill = document.getElementById("ws-status-pill");
  if (dot) {
    dot.className = `status-dot ${connected ? 'online' : 'offline'}`;
  }
  if (label) {
    label.textContent = state.latestWsStatus.text;
  }
  if (pill && tooltip) {
    pill.title = tooltip;
  }
}

function handleWsMessage(msg, onSyncCallback, onToggleCallback, onImageCallback) {
  if (!msg || !msg.type) return;

  if (msg.type === 'WELCOME' || msg.type === 'CLIENTS_COUNT') {
    if (msg.count !== undefined) {
      state.latestWsStatus.count = msg.count;
      state.latestWsStatus.text = `WS: En vivo (${msg.count})`;
      const label = document.getElementById("ws-status-text");
      if (label) label.textContent = `WS: En vivo (${msg.count})`;
    }
  } else if (msg.type === 'DATA_SYNC') {
    if (msg.coupleId === state.coupleId && (!msg.season || msg.season === state.currentSeason)) {
      const activeEl = document.activeElement;
      const isTyping = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');
      if (!isTyping && onSyncCallback) {
        onSyncCallback();
      }
    }
  } else if (msg.type === 'SPIRIT_TOGGLE') {
    if (msg.coupleId === state.coupleId && (!msg.season || msg.season === state.currentSeason)) {
      if (msg.kevinList) state.kevinList = msg.kevinList;
      if (msg.aliList) state.aliList = msg.aliList;
      if (msg.kevinMastery) state.kevinMastery = msg.kevinMastery;
      if (msg.aliMastery) state.aliMastery = msg.aliMastery;
      if (onToggleCallback) onToggleCallback();
    }
  } else if (msg.type === 'IMAGE_UPLOADED') {
    if (msg.spiritId && msg.url) {
      state.customImages[msg.spiritId] = msg.url;
      if (onImageCallback) onImageCallback(msg.spiritId, msg.url);
    }
  }
}
