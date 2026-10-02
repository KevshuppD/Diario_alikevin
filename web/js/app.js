/**
 * app.js - Punto de Entrada Principal y Orquestador de la Aplicación Web
 */

import { state, getStoredTheme, updateUserBadge, switchUserProfile, logoutSession, USERS } from './state.js';
import { initWebSocket } from './websocket.js';
import { initFirestore } from './firestore.js';
import { initRouter, switchModeSPA, switchSeason } from './router.js';
import { renderWorkspace, updateStats, toggleSpiritOwned, toggleSpiritMastery, setFilter, onSearchInput, clearSearch } from './normal-view.js';
import { renderGallery, toggleGalleryDrawer, moveSpiritToCategory, removeSpiritFromCategory, permanentlyDeleteSpirit, deleteAllUncategorizedSpirits, deleteCategory, openAssignModal, closeAssignModal, openNewSpiritModal, closeNewSpiritModal, openEditImageModal, closeEditImageModal } from './edit-view.js';
import { renderCategoriesManager } from './categories-view.js';
import { renderConfigView, applyUserFont, changeFont } from './config-view.js';
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

export function showLoginModal() {
  let modal = document.getElementById("login-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "login-modal";
    modal.className = "modal-overlay";
    document.body.appendChild(modal);
  }

  modal.innerHTML = `
    <div class="modal-content" style="max-width: 420px; text-align: center; gap: 16px; padding: 28px 24px; border: 2px solid var(--card-border); border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.6);">
      <div style="font-size: 48px; line-height: 1; margin-bottom: 2px;">🔐</div>
      <div>
        <h2 style="font-family: 'VT323', monospace; font-size: 28px; font-weight: 700; margin: 0 0 6px 0; color: var(--text-color);">Iniciar Sesión</h2>
        <p style="font-size: 16px; color: var(--text-muted); margin: 0; line-height: 1.3;">
          Selecciona tu perfil para acceder al Gestor del Diario y registrar tus espíritus.
        </p>
      </div>

      <div style="display: flex; flex-direction: column; gap: 12px; margin: 10px 0;">
        <button type="button" class="btn" onclick="window.performLogin('kevin')" style="padding: 14px 20px; font-size: 15px; font-weight: 800; border-color: #3b82f6; color: #60a5fa; background: rgba(59, 130, 246, 0.18); display: flex; align-items: center; justify-content: center; gap: 10px; border-radius: 12px; cursor: pointer;">
          <span style="font-size: 20px;">🔵</span> Ingresar como Kevin
        </button>
        <button type="button" class="btn" onclick="window.performLogin('ali')" style="padding: 14px 20px; font-size: 15px; font-weight: 800; border-color: #ec4899; color: #f472b6; background: rgba(236, 72, 153, 0.18); display: flex; align-items: center; justify-content: center; gap: 10px; border-radius: 12px; cursor: pointer;">
          <span style="font-size: 20px;">🔴</span> Ingresar como Ali
        </button>
      </div>

      <div style="background: rgba(0,0,0,0.25); border: 1px solid var(--card-border); border-radius: 8px; padding: 8px 12px; font-size: 12px; color: var(--text-muted);">
        🔗 Vínculo: <strong style="color: var(--text-color);">${state.coupleId}</strong>
      </div>
    </div>
  `;

  modal.classList.add("show");
  modal.onclick = (e) => {
    if (e.target === modal && state.currentUser) {
      modal.classList.remove("show");
    }
  };
}

export function closeLoginModal() {
  const modal = document.getElementById("login-modal");
  if (modal) modal.classList.remove("show");
}

export function performLogin(username) {
  const user = USERS[username] || USERS.kevin;
  state.currentUser = user;
  localStorage.setItem("logged_user", user.username);
  
  closeLoginModal();
  updateUserBadge();
  showToast(`👋 ¡Bienvenido/a, ${user.name}!`);

  if (typeof window.renderWorkspace === 'function') {
    window.renderWorkspace();
  }
  if (typeof window.renderConfigEditor === 'function') {
    window.renderConfigEditor();
  }
}

export function openUserModal() {
  let modal = document.getElementById("user-session-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "user-session-modal";
    modal.className = "modal-overlay";
    document.body.appendChild(modal);
  }

  const currentUsername = state.currentUser ? state.currentUser.username : "kevin";

  modal.innerHTML = `
    <div class="modal-content" style="max-width: 440px; text-align: center; gap: 14px;">
      <div style="font-size: 38px; margin-bottom: 2px;">👤</div>
      <h3 style="font-family: 'VT323', monospace; font-size: 26px; font-weight: 700; margin: 0; color: var(--text-color);">Sesión Activa</h3>
      <p style="font-size: 16px; color: var(--text-muted); margin: 0;">
        Conectado como <strong style="color: ${currentUsername === 'kevin' ? '#60a5fa' : '#f472b6'};">${state.currentUser ? state.currentUser.name : 'Ninguno'}</strong>
      </p>

      <div style="background: rgba(0,0,0,0.25); border: 1px solid var(--card-border); border-radius: 8px; padding: 10px 14px; text-align: left;">
        <div style="font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 4px;">🔗 Vínculo Activo</div>
        <div style="font-size: 13px; font-weight: 600; color: var(--text-color);">${state.coupleId}</div>
      </div>

      <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 6px;">
        <button type="button" class="btn btn-secondary" onclick="window.closeUserModal()" style="font-size: 13px;">Cerrar</button>
        <button type="button" class="btn" onclick="window.closeUserModal(); window.logoutSession();" style="background: rgba(239, 68, 68, 0.2); border-color: #ef4444; color: #f87171; font-size: 13px;">
          🚪 Cerrar Sesión
        </button>
      </div>
    </div>
  `;

  modal.classList.add("show");
  modal.onclick = (e) => {
    if (e.target === modal) closeUserModal();
  };
}

export function closeUserModal() {
  const modal = document.getElementById("user-session-modal");
  if (modal) modal.classList.remove("show");
}

// Window global assignments
window.customAlert = customAlert;
window.customConfirm = customConfirm;
window.customPrompt = customPrompt;
window.showToast = showToast;
window.customToast = showToast;
window.showLoginModal = showLoginModal;
window.closeLoginModal = closeLoginModal;
window.performLogin = performLogin;
window.openUserModal = openUserModal;
window.closeUserModal = closeUserModal;
window.switchUserProfile = switchUserProfile;
window.logoutSession = logoutSession;

// Inicialización general al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Iniciando Gestor de Diario de Ali y Kevin...');
  
  const savedTheme = getStoredTheme();
  document.body.className = `theme-${savedTheme}`;
  applyUserFont(state.userFont);
  updateUserBadge();

  if (!state.currentUser) {
    showLoginModal();
  }

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

