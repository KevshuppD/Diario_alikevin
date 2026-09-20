/**
 * studio-view.js - Estudio de Arte de Espíritus (Editor de Píxeles, Quitar Fondo y Subir a Cloudinary)
 */

import { state } from './state.js';
import { trimCanvasTransparency } from './image-utils.js';
import { saveChanges } from './firestore.js';
import { sendWebSocketBroadcast } from './websocket.js';
import { renderGallery } from './edit-view.js';
import { renderNormalGrid } from './normal-view.js';

let currentEditingSpiritId = null;
let studioCanvas = null;
let studioCtx = null;
let undoStack = [];
let redoStack = [];
let currentTool = 'wand'; // wand, brush, eraser, crop
let brushSize = 10;
let tolerance = 30;
let isDrawing = false;

export function openStudioModal(spiritId) {
  currentEditingSpiritId = spiritId;
  const modal = document.getElementById('studioModal');
  if (!modal) return;

  const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
  const spirit = spirits.find(s => s.id === spiritId);
  if (!spirit) return;

  const titleEl = document.getElementById('studioSpiritTitle');
  if (titleEl) titleEl.innerText = `🎨 Estudio de Arte - ${spirit.name} (S${state.activeSeason} #${spirit.id})`;

  modal.classList.add('active');
  initStudioCanvas(spirit.imageUrl || spirit.image);
}

export function closeStudioModal() {
  const modal = document.getElementById('studioModal');
  if (modal) modal.classList.remove('active');
  currentEditingSpiritId = null;
}

function initStudioCanvas(imageUrl) {
  studioCanvas = document.getElementById('studioCanvas');
  if (!studioCanvas) return;
  studioCtx = studioCanvas.getContext('2d', { willReadFrequently: true });

  undoStack = [];
  redoStack = [];

  const img = new Image();
  img.crossOrigin = 'Anonymous';
  img.onload = () => {
    studioCanvas.width = img.naturalWidth || 256;
    studioCanvas.height = img.naturalHeight || 256;
    studioCtx.clearRect(0, 0, studioCanvas.width, studioCanvas.height);
    studioCtx.drawImage(img, 0, 0);
    saveState();
  };
  img.onerror = () => {
    // Si falla por CORS, usar un canvas básico
    studioCanvas.width = 256;
    studioCanvas.height = 256;
    studioCtx.fillStyle = '#334155';
    studioCtx.fillRect(0, 0, 256, 256);
    saveState();
  };
  img.src = imageUrl;

  setupCanvasEvents();
}

function saveState() {
  if (!studioCanvas || !studioCtx) return;
  if (undoStack.length >= 20) undoStack.shift();
  undoStack.push(studioCtx.getImageData(0, 0, studioCanvas.width, studioCanvas.height));
  redoStack = [];
}

export function studioUndo() {
  if (undoStack.length <= 1) return;
  redoStack.push(undoStack.pop());
  const prevState = undoStack[undoStack.length - 1];
  studioCtx.putImageData(prevState, 0, 0);
}

export function studioRedo() {
  if (redoStack.length === 0) return;
  const nextState = redoStack.pop();
  undoStack.push(nextState);
  studioCtx.putImageData(nextState, 0, 0);
}

function setupCanvasEvents() {
  if (!studioCanvas) return;

  studioCanvas.onmousedown = (e) => {
    const rect = studioCanvas.getBoundingClientRect();
    const scaleX = studioCanvas.width / rect.width;
    const scaleY = studioCanvas.height / rect.height;
    const x = Math.floor((e.clientX - rect.left) * scaleX);
    const y = Math.floor((e.clientY - rect.top) * scaleY);

    if (currentTool === 'wand') {
      floodFillTransparent(x, y, tolerance);
      saveState();
    } else {
      isDrawing = true;
      applyBrush(x, y);
    }
  };

  studioCanvas.onmousemove = (e) => {
    if (!isDrawing) return;
    const rect = studioCanvas.getBoundingClientRect();
    const scaleX = studioCanvas.width / rect.width;
    const scaleY = studioCanvas.height / rect.height;
    const x = Math.floor((e.clientX - rect.left) * scaleX);
    const y = Math.floor((e.clientY - rect.top) * scaleY);
    applyBrush(x, y);
  };

  window.onmouseup = () => {
    if (isDrawing) {
      isDrawing = false;
      saveState();
    }
  };
}

function applyBrush(x, y) {
  if (!studioCtx) return;
  studioCtx.save();
  if (currentTool === 'eraser') {
    studioCtx.globalCompositeOperation = 'destination-out';
    studioCtx.beginPath();
    studioCtx.arc(x, y, brushSize / 2, 0, Math.PI * 2);
    studioCtx.fill();
  }
  studioCtx.restore();
}

function floodFillTransparent(startX, startY, tol) {
  const imgData = studioCtx.getImageData(0, 0, studioCanvas.width, studioCanvas.height);
  const data = imgData.data;
  const width = studioCanvas.width;
  const height = studioCanvas.height;

  const startIndex = (startY * width + startX) * 4;
  const targetR = data[startIndex];
  const targetG = data[startIndex + 1];
  const targetB = data[startIndex + 2];
  const targetA = data[startIndex + 3];

  if (targetA === 0) return; // ya transparente

  const queue = [[startX, startY]];
  const visited = new Uint8Array(width * height);

  function colorMatch(idx) {
    const r = data[idx];
    const g = data[idx + 1];
    const b = data[idx + 2];
    const a = data[idx + 3];
    return Math.abs(r - targetR) <= tol &&
           Math.abs(g - targetG) <= tol &&
           Math.abs(b - targetB) <= tol &&
           Math.abs(a - targetA) <= tol;
  }

  while (queue.length > 0) {
    const [cx, cy] = queue.pop();
    const idx = (cy * width + cx) * 4;
    const vIdx = cy * width + cx;

    if (visited[vIdx]) continue;
    visited[vIdx] = 1;

    if (colorMatch(idx)) {
      data[idx + 3] = 0; // Hacer transparente

      if (cx > 0 && !visited[vIdx - 1]) queue.push([cx - 1, cy]);
      if (cx < width - 1 && !visited[vIdx + 1]) queue.push([cx + 1, cy]);
      if (cy > 0 && !visited[vIdx - width]) queue.push([cx, cy - 1]);
      if (cy < height - 1 && !visited[vIdx + width]) queue.push([cx, cy + 1]);
    }
  }

  studioCtx.putImageData(imgData, 0, 0);
}

export function studioAutoTrim() {
  if (!studioCanvas) return;
  const trimmed = trimCanvasTransparency(studioCanvas);
  studioCanvas.width = trimmed.width;
  studioCanvas.height = trimmed.height;
  studioCtx.drawImage(trimmed, 0, 0);
  saveState();
  if (window.customToast) window.customToast('Espacios transparentes recortados');
}

export async function studioSaveAndUpload() {
  if (!studioCanvas || !currentEditingSpiritId) return;

  const btn = document.getElementById('studioSaveBtn');
  if (btn) {
    btn.disabled = true;
    btn.innerText = '⏳ Subiendo a Cloudinary...';
  }

  try {
    const base64Data = studioCanvas.toDataURL('image/png');
    const response = await fetch('/api/upload-spirit-image', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        spiritId: currentEditingSpiritId,
        imageBase64: base64Data,
        season: state.activeSeason
      })
    });

    const result = await response.json();
    if (result.success && result.url) {
      const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
      const spirit = spirits.find(s => s.id === currentEditingSpiritId);
      if (spirit) {
        spirit.imageUrl = result.url;
        spirit.image = result.url;
      }

      saveChanges();
      sendWebSocketBroadcast({
        type: 'SPIRIT_UPDATED',
        season: state.activeSeason,
        spiritId: currentEditingSpiritId,
        imageUrl: result.url
      });

      renderGallery();
      renderNormalGrid();
      closeStudioModal();
      if (window.customToast) window.customToast('¡Imagen guardada y sincronizada en tiempo real! ✨');
    } else {
      throw new Error(result.error || 'Error al procesar subida');
    }
  } catch (err) {
    console.error('Error subiendo imagen:', err);
    if (window.customAlert) {
      window.customAlert(`No se pudo subir la imagen: ${err.message}`, 'Error de Subida');
    }
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = '💾 Guardar & Subir a Cloudinary';
    }
  }
}

export function setStudioTool(tool) {
  currentTool = tool;
  document.querySelectorAll('.studio-tool-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.tool === tool);
  });
}

// Window bindings
window.openStudioModal = openStudioModal;
window.closeStudioModal = closeStudioModal;
window.studioUndo = studioUndo;
window.studioRedo = studioRedo;
window.studioAutoTrim = studioAutoTrim;
window.studioSaveAndUpload = studioSaveAndUpload;
window.setStudioTool = setStudioTool;
