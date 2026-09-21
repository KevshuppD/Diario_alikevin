/**
 * app.js - Punto de Entrada Principal y Orquestador de la Aplicación Web
 */

import { state, getStoredTheme } from './state.js';
import { initWebSocket } from './websocket.js';
import { initFirestore } from './firestore.js';
import { initRouter, switchModeSPA, switchSeason } from './router.js';
import { renderWorkspace, updateStats, toggleSpiritOwned, toggleSpiritMastery, setFilter, onSearchInput, clearSearch } from './normal-view.js';
import { renderGallery, toggleGalleryDrawer, moveSpiritToCategory, removeSpiritFromCategory, permanentlyDeleteSpirit, deleteAllUncategorizedSpirits, deleteCategory, openAssignModal, closeAssignModal, openNewSpiritModal, closeNewSpiritModal, openEditImageModal, closeEditImageModal } from './edit-view.js';
import { renderCategoriesManager } from './categories-view.js';
import { renderConfigView } from './config-view.js';
import { initRadarView, sendRemoteMagicPing } from './radar-view.js';

// Custom Dialog & Toast System
function _showDialog(icon, msg, inputVisible, inputPlaceholder, buttons) {
  const overlay = document.getElementById("custom-dialog-overlay");
  const iconEl = document.getElementById("custom-dialog-icon");
  const msgEl = document.getElementById("custom-dialog-msg");
  const inputEl = document.getElementById("custom-dialog-input");
  const btnsEl = document.getElementById("custom-dialog-btns");

  if (!overlay || !msgEl || !btnsEl) {
    if (inputVisible) return prompt(msg, inputPlaceholder);
    if (buttons.length > 1) return confirm(msg);
    return alert(msg);
  }

  if (iconEl) iconEl.textContent = icon || "ℹ️";
  msgEl.textContent = msg;

  if (inputVisible) {
    inputEl.style.display = "block";
    inputEl.value = "";
    inputEl.placeholder = inputPlaceholder || "";
    setTimeout(() => inputEl.focus(), 80);
  } else {
    inputEl.style.display = "none";
  }

  btnsEl.innerHTML = "";
  buttons.forEach(b => {
    const btn = document.createElement("button");
    btn.textContent = b.label;
    btn.className = b.primary ? "btn" : "btn btn-secondary";
    if (b.danger) {
      btn.style.background = "rgba(239,68,68,0.15)";
      btn.style.borderColor = "#ef4444";
      btn.style.color = "#ef4444";
    }
    btn.onclick = () => {
      overlay.classList.remove("show");
      if (b.action) b.action(inputEl.value);
    };
    btnsEl.appendChild(btn);
  });

  inputEl.onkeydown = (e) => {
    if (e.key === "Enter") {
      const primary = buttons.find(b => b.primary);
      if (primary) {
        overlay.classList.remove("show");
        if (primary.action) primary.action(inputEl.value);
      }
    }
  };

  overlay.classList.add("show");
}

export function customAlert(msg, icon = "ℹ️") {
  _showDialog(icon, msg, false, "", [
    { label: "Aceptar", primary: true, action: null }
  ]);
}

export function customConfirm(msg, icon = "❓", onConfirm) {
  _showDialog(icon, msg, false, "", [
    { label: "Cancelar", primary: false, action: null },
    { label: "Confirmar", primary: true, danger: false, action: () => { if (onConfirm) onConfirm(); } }
  ]);
}

export function customPrompt(msg, icon = "✏️", onConfirm) {
  _showDialog(icon, msg, true, "", [
    { label: "Cancelar", primary: false, action: null },
    { label: "Aceptar", primary: true, action: (val) => { if (onConfirm) onConfirm(val); } }
  ]);
}

export function showToast(msg, isError = false) {
  let toast = document.getElementById('global-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'global-toast';
    toast.className = 'global-toast';
    document.body.appendChild(toast);
  }

  toast.textContent = msg;
  toast.style.borderColor = isError ? '#ef4444' : 'var(--accent-color)';
  toast.classList.add('show');

  setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
}

// Window global assignments
window.customAlert = customAlert;
window.customConfirm = customConfirm;
window.customPrompt = customPrompt;
window.showToast = showToast;
window.customToast = showToast;

// Inicialización general al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Iniciando Gestor de Diario de Ali y Kevin...');
  
  const savedTheme = getStoredTheme();
  document.body.className = `theme-${savedTheme}`;

  const overlay = document.getElementById("custom-dialog-overlay");
  if (overlay) {
    overlay.addEventListener("click", function(e) {
      if (e.target === this) this.classList.remove("show");
    });
  }

  const renderCurrent = () => {
    if (typeof window.setMode === 'function') {
      window.setMode(state.currentMode || 'normal');
    }
  };

  initFirestore(renderCurrent);
  initWebSocket(renderCurrent);
  initRouter();
});
