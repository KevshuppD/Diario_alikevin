/**
 * app.js - Punto de Entrada Principal y Orquestador de la Aplicación Web
 */

import { state, getStoredTheme } from './state.js';
import { initWebSocket } from './websocket.js';
import { initFirestore } from './firestore.js';
import { initRouter, switchModeSPA, switchSeason } from './router.js';
import { renderNormalGrid, renderNormalStats, toggleSpiritOwned, toggleSpiritMastery, setNormalFilter, setNormalSort } from './normal-view.js';
import { renderEditGrid, renderGallery, selectSpiritSlot, assignSpiritToSlot, removeSpiritFromSlot, openNewSpiritModal, closeNewSpiritModal, saveNewSpirit } from './edit-view.js';
import { renderCategoriesManager } from './categories-view.js';
import { renderConfigView } from './config-view.js';
import { initRadarView, sendRemoteMagicPing } from './radar-view.js';
import { openStudioModal, closeStudioModal, studioUndo, studioRedo, studioAutoTrim, studioSaveAndUpload, setStudioTool } from './studio-view.js';

// Modal and Toast System
export function customAlert(message, title = 'Notificación') {
  return new Promise((resolve) => {
    const modal = document.getElementById('customModal');
    const titleEl = document.getElementById('customModalTitle');
    const bodyEl = document.getElementById('customModalBody');
    const footerEl = document.getElementById('customModalFooter');

    if (!modal || !titleEl || !bodyEl || !footerEl) {
      alert(message);
      return resolve();
    }

    titleEl.innerText = title;
    bodyEl.innerHTML = `<p>${message}</p>`;
    footerEl.innerHTML = `<button class="btn-primary" id="customModalOkBtn">Aceptar</button>`;

    modal.classList.add('active');

    document.getElementById('customModalOkBtn').onclick = () => {
      modal.classList.remove('active');
      resolve();
    };
  });
}

export function customConfirm(message, title = 'Confirmación') {
  return new Promise((resolve) => {
    const modal = document.getElementById('customModal');
    const titleEl = document.getElementById('customModalTitle');
    const bodyEl = document.getElementById('customModalBody');
    const footerEl = document.getElementById('customModalFooter');

    if (!modal || !titleEl || !bodyEl || !footerEl) {
      return resolve(confirm(message));
    }

    titleEl.innerText = title;
    bodyEl.innerHTML = `<p>${message}</p>`;
    footerEl.innerHTML = `
      <button class="btn-secondary" id="customModalCancelBtn">Cancelar</button>
      <button class="btn-primary" id="customModalConfirmBtn">Confirmar</button>
    `;

    modal.classList.add('active');

    document.getElementById('customModalCancelBtn').onclick = () => {
      modal.classList.remove('active');
      resolve(false);
    };

    document.getElementById('customModalConfirmBtn').onclick = () => {
      modal.classList.remove('active');
      resolve(true);
    };
  });
}

export function customPrompt(message, defaultValue = '', title = 'Entrada de datos') {
  return new Promise((resolve) => {
    const modal = document.getElementById('customModal');
    const titleEl = document.getElementById('customModalTitle');
    const bodyEl = document.getElementById('customModalBody');
    const footerEl = document.getElementById('customModalFooter');

    if (!modal || !titleEl || !bodyEl || !footerEl) {
      return resolve(prompt(message, defaultValue));
    }

    titleEl.innerText = title;
    bodyEl.innerHTML = `
      <p style="margin-bottom: 10px;">${message}</p>
      <input type="text" id="customModalInput" class="pixel-input" value="${defaultValue}" style="width: 100%;">
    `;
    footerEl.innerHTML = `
      <button class="btn-secondary" id="customModalCancelBtn">Cancelar</button>
      <button class="btn-primary" id="customModalConfirmBtn">Aceptar</button>
    `;

    modal.classList.add('active');
    const input = document.getElementById('customModalInput');
    input.focus();
    input.select();

    input.onkeydown = (e) => {
      if (e.key === 'Enter') {
        const val = input.value;
        modal.classList.remove('active');
        resolve(val);
      }
    };

    document.getElementById('customModalCancelBtn').onclick = () => {
      modal.classList.remove('active');
      resolve(null);
    };

    document.getElementById('customModalConfirmBtn').onclick = () => {
      const val = input.value;
      modal.classList.remove('active');
      resolve(val);
    };
  });
}

export function customToast(message, duration = 3000) {
  let toast = document.getElementById('globalToast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'globalToast';
    toast.className = 'global-toast';
    document.body.appendChild(toast);
  }

  toast.innerText = message;
  toast.classList.add('visible');

  setTimeout(() => {
    toast.classList.remove('visible');
  }, duration);
}

// Window global assignments
window.customAlert = customAlert;
window.customConfirm = customConfirm;
window.customPrompt = customPrompt;
window.customToast = customToast;

// Setup UI Search and Filter Listeners
function setupEventListeners() {
  const normalSearchInput = document.getElementById('normalSearchInput');
  if (normalSearchInput) {
    normalSearchInput.addEventListener('input', (e) => {
      state.normalSearch = e.target.value;
      renderNormalGrid();
    });
  }

  const gallerySearchInput = document.getElementById('gallerySearchInput');
  if (gallerySearchInput) {
    gallerySearchInput.addEventListener('input', (e) => {
      state.gallerySearch = e.target.value;
      renderGallery();
    });
  }
}

// Inicialización general al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Iniciando Gestor de Diario de Ali y Kevin...');
  
  // Cargar tema guardado
  const savedTheme = getStoredTheme();
  document.body.className = `theme-${savedTheme}`;

  setupEventListeners();
  initFirestore();
  initWebSocket();
  initRouter();
});
