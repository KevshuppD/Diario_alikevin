/**
 * edit-view.js - Vista de Modo Edición, Gestión de Ranuras/Categorías, Galería y Modales
 */

import { state } from './state.js';
import { defaultNames, defaultSpiritsList, defaultCategories, defaultSpiritsListT1, defaultSpiritsListT2 } from './constants.js';
import { triggerAutoSave } from './firestore.js';
import { getSpiritImgUrl, getSpiritName, getSpiritCurrentType, computeSpiritName, handleSpiritImgError, renderWorkspace, updateStats, matchesFilter, getDefaultSpiritName } from './normal-view.js';
import { processSpiritImage, optimizeCloudinaryUrl } from './image-utils.js';

export let isGalleryOpen = false;
window.isGalleryOpen = false;
let activeModalSpiritId = null;

export function toggleGalleryDrawer() {
  isGalleryOpen = !isGalleryOpen;
  window.isGalleryOpen = isGalleryOpen;
  const sidebar = document.getElementById("sidebar");
  const label = document.getElementById("btn-gallery-label");
  
  if (isGalleryOpen) {
    if (sidebar) sidebar.classList.remove("hidden");
    if (label) label.textContent = "Ocultar Galería";
    renderGallery();
  } else {
    if (sidebar) sidebar.classList.add("hidden");
    if (label) label.textContent = "Mostrar Galería";
  }
}

export function renderGallery() {
  const container = document.getElementById("gallery-container");
  if (!container) return;
  container.innerHTML = "";
  
  const defaultList = state.currentSeason === 1 ? defaultSpiritsListT1 : defaultSpiritsListT2;
  const activeList = (state.spiritsList && state.spiritsList.length > 0) ? state.spiritsList : defaultList;
  const listToRender = [...activeList].sort((a, b) => parseInt(a, 10) - parseInt(b, 10));

  const sidebarTitle = document.getElementById("sidebar-title");
  if (sidebarTitle) {
    sidebarTitle.textContent = `Galería T${state.currentSeason} (${listToRender.length})`;
  }

  const assignedIds = new Set();
  state.categories.forEach(cat => {
    (cat.spiritIds || []).forEach(id => assignedIds.add(id));
  });

  const precomputedSets = {
    kevinOwnedSet: new Set(state.kevinList),
    aliOwnedSet: new Set(state.aliList),
    kevinMasterySet: new Set(state.kevinMastery),
    aliMasterySet: new Set(state.aliMastery)
  };

  let unassignedCount = 0;
  const galleryFragment = document.createDocumentFragment();

  listToRender.forEach((id, index) => {
    if (state.searchQuery !== "" && !matchesFilter(id, precomputedSets)) return;

    const isUnassigned = !assignedIds.has(id);
    if (isUnassigned) unassignedCount++;

    const displayNumber = String(index + 1).padStart(2, '0');

    const item = document.createElement("div");
    item.className = `gallery-item ${isUnassigned ? 'unassigned' : ''}`;
    item.draggable = true;
    item.dataset.id = id;
    item.innerHTML = `
      ${isUnassigned ? '<span class="unassigned-tag">SUELTO</span>' : ''}
      <button class="gallery-delete-btn" onclick="window.deleteFromGallery(event, '${id}')" title="Eliminar espíritu">🗑</button>
      <img src="${getSpiritImgUrl(id)}" alt="Espíritu ${id}" width="44" height="44" loading="lazy" decoding="async" draggable="false" onerror="window.handleSpiritImgError(this, '${id}')">
      <span class="badge">#${displayNumber}</span>
    `;
    
    item.addEventListener("dragstart", (e) => {
      e.dataTransfer.setData("text/plain", id);
    });

    item.addEventListener("click", () => {
      openAssignModal(id);
    });
    
    galleryFragment.appendChild(item);
  });

  container.appendChild(galleryFragment);

  const sidebarSub = document.getElementById("sidebar-subtext");
  if (sidebarSub) {
    if (unassignedCount > 0) {
      sidebarSub.innerHTML = `<span style="color: #ff4455; font-weight: 700;">⚠️ ${unassignedCount} espíritu(s) sin asignar (etiqueta SUELTO)</span>`;
    } else {
      sidebarSub.textContent = "Todos los espíritus están asignados a categorías.";
    }
  }
}

export function deleteFromGallery(event, spiritId) {
  if (event) event.stopPropagation();
  permanentlyDeleteSpirit(spiritId);
}

export function moveSpiritToCategory(spiritId, targetCatName) {
  if (!spiritId && spiritId !== 0) return;
  const idStr = String(spiritId).trim();
  const formattedId = idStr.padStart(2, '0');
  const numId = String(parseInt(idStr, 10));

  state.categories.forEach(cat => {
    cat.spiritIds = (cat.spiritIds || []).filter(id => {
      const s = String(id).trim();
      return s !== idStr && s !== formattedId && s !== numId;
    });
  });

  if (targetCatName !== "__uncategorized__") {
    const cat = state.categories.find(c => c.name === targetCatName);
    if (cat) {
      if (!cat.spiritIds) cat.spiritIds = [];
      if (!cat.spiritIds.some(id => String(id).padStart(2, '0') === formattedId)) {
        cat.spiritIds.push(formattedId);
      }
    }

    const currentType = getSpiritCurrentType(formattedId);
    const newFullName = computeSpiritName(formattedId, targetCatName, currentType);
    const defName = getDefaultSpiritName(formattedId, state.currentSeason);
    if (newFullName && newFullName !== defName) {
      state.customNames[formattedId] = newFullName;
    }
    state.customCategories[formattedId] = targetCatName;
  } else {
    delete state.customCategories[formattedId];
  }

  if (!state.spiritsList.some(id => String(id).padStart(2, '0') === formattedId)) {
    state.spiritsList.push(formattedId);
  }
  state.spiritsList = Array.from(new Set(state.spiritsList.map(id => String(id).padStart(2, '0')))).sort((a, b) => parseInt(a, 10) - parseInt(b, 10));

  renderWorkspace();
  if (isGalleryOpen) renderGallery();
  triggerAutoSave(50);
}

export function removeSpiritFromCategory(spiritId, categoryName) {
  if (!spiritId && spiritId !== 0) return;
  const formattedId = String(spiritId).padStart(2, '0');
  const cat = state.categories.find(c => c.name === categoryName);
  if (cat && cat.spiritIds) {
    cat.spiritIds = cat.spiritIds.filter(id => String(id).padStart(2, '0') !== formattedId);
  }
  renderWorkspace();
  if (isGalleryOpen) renderGallery();
  triggerAutoSave(50);
}

export function permanentlyDeleteSpirit(spiritId) {
  if (window.customConfirm) {
    window.customConfirm(`¿Eliminar permanentemente el espíritu ID ${spiritId}?`, "⚠️", () => {
      executeDeleteSpirit(spiritId);
    });
  } else {
    if (confirm(`¿Eliminar espíritu #${spiritId}?`)) {
      executeDeleteSpirit(spiritId);
    }
  }
}

function executeDeleteSpirit(spiritId) {
  state.spiritsList = state.spiritsList.filter(id => id !== spiritId);
  state.categories.forEach(cat => {
    cat.spiritIds = (cat.spiritIds || []).filter(id => id !== spiritId);
  });
  state.kevinList = state.kevinList.filter(id => id !== spiritId);
  state.aliList = state.aliList.filter(id => id !== spiritId);
  state.kevinMastery = state.kevinMastery.filter(id => id !== spiritId);
  state.aliMastery = state.aliMastery.filter(id => id !== spiritId);
  delete state.customNames[spiritId];

  updateStats();
  renderGallery();
  renderWorkspace();
  triggerAutoSave(50);
}

export function deleteAllUncategorizedSpirits() {
  const assignedIds = new Set();
  state.categories.forEach(cat => {
    (cat.spiritIds || []).forEach(id => assignedIds.add(id));
  });
  const uncategorizedIds = state.spiritsList.filter(id => !assignedIds.has(id));

  if (uncategorizedIds.length === 0) {
    if (window.customAlert) window.customAlert("No hay espíritus sueltos para eliminar.", "ℹ️");
    return;
  }

  const confirmAction = () => {
    const deletedSet = new Set(uncategorizedIds);
    state.spiritsList = state.spiritsList.filter(id => !deletedSet.has(id));
    state.kevinList = state.kevinList.filter(id => !deletedSet.has(id));
    state.aliList = state.aliList.filter(id => !deletedSet.has(id));
    state.kevinMastery = state.kevinMastery.filter(id => !deletedSet.has(id));
    state.aliMastery = state.aliMastery.filter(id => !deletedSet.has(id));

    uncategorizedIds.forEach(id => {
      delete state.customNames[id];
      delete state.customImages[id];
    });

    updateStats();
    renderGallery();
    renderWorkspace();
    triggerAutoSave(50);
  };

  if (window.customConfirm) {
    window.customConfirm(`¿Eliminar los ${uncategorizedIds.length} espíritus sueltos?`, "🗑️", confirmAction);
  } else {
    if (confirm(`¿Eliminar los ${uncategorizedIds.length} espíritus sueltos?`)) confirmAction();
  }
}

export function deleteCategory(index) {
  const cat = state.categories[index];
  if (!cat) return;
  const displayName = state.customCategories[cat.name] || cat.name;

  const confirmAction = () => {
    delete state.customCategories[cat.name];
    state.categories.splice(index, 1);
    renderWorkspace();
    triggerAutoSave(50);
  };

  if (window.customConfirm) {
    window.customConfirm(`¿Eliminar la categoría "${displayName}"? Los espíritus pasarán a Sueltos.`, "🗑️", confirmAction);
  } else {
    if (confirm(`¿Eliminar categoría "${displayName}"?`)) confirmAction();
  }
}

export function changeSpiritType(id, newTypeName) {
  const foundCat = state.categories.find(c => (c.spiritIds || []).includes(id));
  const catName = foundCat ? foundCat.name : "__uncategorized__";
  const newFullName = computeSpiritName(id, catName, newTypeName);
  const defName = getDefaultSpiritName(id, state.currentSeason);

  if (newFullName === defName) {
    delete state.customNames[id];
  } else {
    state.customNames[id] = newFullName;
  }
  renderWorkspace();
  triggerAutoSave(50);
}

export function openAssignModal(id) {
  if (state.currentMode !== "edit") return;
  activeModalSpiritId = id;
  const titleEl = document.getElementById("assign-modal-title");
  if (titleEl) titleEl.textContent = `Asignar Espíritu #${id} (${getSpiritName(id)})`;
  
  let currentCatName = "__uncategorized__";
  const foundCat = state.categories.find(c => (c.spiritIds || []).includes(id));
  if (foundCat) currentCatName = foundCat.name;
  
  let assignOptionsHtml = "";
  state.categories.forEach(c => {
    const isCurrent = c.name === currentCatName;
    assignOptionsHtml += `<option value="${c.name}" ${isCurrent ? 'selected' : ''}>${state.customCategories[c.name] || c.name}</option>`;
  });
  assignOptionsHtml += `<option value="__uncategorized__" ${currentCatName === "__uncategorized__" ? 'selected' : ''}>Sin Categoría / Suelto</option>`;
  
  const selectEl = document.getElementById("assign-category-select");
  if (selectEl) selectEl.innerHTML = assignOptionsHtml;
  
  const modal = document.getElementById("assign-modal");
  if (modal) modal.classList.add("show");
}

export function closeAssignModal() {
  const modal = document.getElementById("assign-modal");
  if (modal) modal.classList.remove("show");
  activeModalSpiritId = null;
}

let pendingNewSpiritImgData = null;
let pendingEditImgData = null;

export function openNewSpiritModal() {
  let modal = document.getElementById("new-spirit-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "new-spirit-modal";
    modal.className = "modal-overlay";
    document.body.appendChild(modal);
  }

  const defaultList = state.currentSeason === 1 ? defaultSpiritsListT1 : defaultSpiritsListT2;
  const activeList = (state.spiritsList && state.spiritsList.length > 0) ? state.spiritsList : defaultList;
  const maxNum = activeList.reduce((max, s) => Math.max(max, parseInt(s, 10) || 0), 0);
  const nextId = String(maxNum + 1).padStart(2, '0');

  let catOptionsHtml = `<option value="__uncategorized__">Sin Categoría / Suelto</option>`;
  state.categories.forEach(c => {
    const dName = state.customCategories[c.name] || c.name;
    catOptionsHtml += `<option value="${c.name}">${dName}</option>`;
  });

  let typeOptionsHtml = "";
  state.spiritTypes.forEach(t => {
    typeOptionsHtml += `<option value="${t.name}">${t.name}</option>`;
  });

  pendingNewSpiritImgData = null;

  modal.innerHTML = `
    <div class="modal-content" style="max-width: 480px; gap: 14px;">
      <h3 style="margin: 0; font-size: 24px; color: var(--text-color); display: flex; align-items: center; gap: 8px;">
        <span>✨</span> Crear Nuevo Espíritu
      </h3>
      <p style="font-size: 14px; color: var(--text-muted); margin: 0;">
        Registra un nuevo espíritu en la colección activa (Temporada ${state.currentSeason}).
      </p>

      <div style="display: flex; gap: 10px;">
        <div style="width: 100px;">
          <label style="display: block; font-size: 13px; color: var(--text-muted); margin-bottom: 4px; font-weight: 600;">ID Ranura:</label>
          <input type="text" id="new-spirit-id" value="${nextId}" style="width: 100%; padding: 8px; border-radius: 8px; font-size: 15px; font-weight: 700; text-align: center;">
        </div>
        <div style="flex: 1;">
          <label style="display: block; font-size: 13px; color: var(--text-muted); margin-bottom: 4px; font-weight: 600;">Nombre del Espíritu:</label>
          <input type="text" id="new-spirit-name" placeholder="Ej: Espíritu Dragón" style="width: 100%; padding: 8px; border-radius: 8px; font-size: 15px;">
        </div>
      </div>

      <div style="display: flex; gap: 10px;">
        <div style="flex: 1;">
          <label style="display: block; font-size: 13px; color: var(--text-muted); margin-bottom: 4px; font-weight: 600;">Categoría:</label>
          <select id="new-spirit-category" style="width: 100%; padding: 8px; border-radius: 8px; font-size: 14px;">
            ${catOptionsHtml}
          </select>
        </div>
        <div style="flex: 1;">
          <label style="display: block; font-size: 13px; color: var(--text-muted); margin-bottom: 4px; font-weight: 600;">Variante / Tipo:</label>
          <select id="new-spirit-type" style="width: 100%; padding: 8px; border-radius: 8px; font-size: 14px;">
            ${typeOptionsHtml}
          </select>
        </div>
      </div>

      <div style="background: rgba(0,0,0,0.05); border: 1px solid var(--card-border); border-radius: 10px; padding: 12px;">
        <label style="display: block; font-size: 13px; color: var(--text-color); margin-bottom: 8px; font-weight: 700;">🖼️ Imagen / Sprite:</label>
        <div style="display: flex; gap: 12px; align-items: center;">
          <div class="img-preview-box" style="width: 64px; height: 64px; border-radius: 8px; border: 1px solid var(--card-border); background: repeating-conic-gradient(#1f2233 0% 25%, #131520 0% 50%) 50% / 12px 12px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
            <img id="new-spirit-preview" src="${getSpiritImgUrl(nextId)}" alt="Vista previa" style="width: 52px; height: 52px; object-fit: contain; image-rendering: pixelated;">
          </div>
          <div style="flex: 1; display: flex; flex-direction: column; gap: 6px;">
            <input type="file" id="new-spirit-file-input" accept="image/*" style="font-size: 12px; color: var(--text-muted);">
            <input type="text" id="new-spirit-url-input" placeholder="O ingresa URL directa de imagen..." style="width: 100%; padding: 6px 8px; border-radius: 6px; font-size: 13px;">
          </div>
        </div>
      </div>

      <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 6px;">
        <button type="button" class="btn btn-secondary" onclick="window.closeNewSpiritModal()">Cancelar</button>
        <button type="button" class="btn" onclick="window.saveNewSpirit()" style="background: linear-gradient(135deg, #00e5ff, #e040fb); color: #fff; font-weight: 700;">
          ✨ Crear Espíritu
        </button>
      </div>
    </div>
  `;

  modal.classList.add("show");

  const fileInput = document.getElementById("new-spirit-file-input");
  const urlInput = document.getElementById("new-spirit-url-input");
  const previewImg = document.getElementById("new-spirit-preview");

  if (fileInput) {
    fileInput.onchange = async (e) => {
      const file = e.target.files?.[0];
      if (file) {
        try {
          const res = await processSpiritImage(file);
          pendingNewSpiritImgData = res.dataUrl;
          if (previewImg) previewImg.src = res.dataUrl;
        } catch(err) {
          console.error("Error leyendo imagen:", err);
        }
      }
    };
  }

  if (urlInput) {
    urlInput.oninput = (e) => {
      const val = e.target.value.trim();
      if (val && previewImg) {
        previewImg.src = val;
        pendingNewSpiritImgData = val;
      }
    };
  }
}

export function closeNewSpiritModal() {
  const modal = document.getElementById("new-spirit-modal");
  if (modal) modal.classList.remove("show");
  pendingNewSpiritImgData = null;
}

export async function saveNewSpirit() {
  const idInput = document.getElementById("new-spirit-id");
  const nameInput = document.getElementById("new-spirit-name");
  const catSelect = document.getElementById("new-spirit-category");
  const typeSelect = document.getElementById("new-spirit-type");

  let id = (idInput ? idInput.value : "").trim();
  let name = (nameInput ? nameInput.value : "").trim();
  const categoryName = catSelect ? catSelect.value : "__uncategorized__";
  const typeName = typeSelect ? typeSelect.value : "Normal";

  if (!id) {
    if (window.customAlert) window.customAlert("Por favor ingresa un ID para el espíritu.", "⚠️");
    return;
  }
  id = String(parseInt(id, 10) || id).padStart(2, '0');

  if (!name) {
    name = computeSpiritName(id, categoryName, typeName) || `Espíritu #${id}`;
  }

  // Handle image upload if provided
  if (pendingNewSpiritImgData) {
    if (pendingNewSpiritImgData.startsWith("data:")) {
      try {
        const resp = await fetch('/api/upload-spirit-image', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            spiritId: id,
            imageBase64: pendingNewSpiritImgData,
            season: state.currentSeason
          })
        });
        const data = await resp.json();
        if (data.success && data.url) {
          state.customImages[id] = data.url;
        } else {
          state.customImages[id] = pendingNewSpiritImgData;
        }
      } catch (err) {
        state.customImages[id] = pendingNewSpiritImgData;
      }
    } else {
      state.customImages[id] = pendingNewSpiritImgData;
    }
  }

  // Add to spirit list
  if (!state.spiritsList.includes(id)) {
    state.spiritsList.push(id);
    state.spiritsList.sort((a, b) => parseInt(a, 10) - parseInt(b, 10));
  }

  // Assign to category
  if (categoryName !== "__uncategorized__") {
    state.categories.forEach(c => {
      c.spiritIds = (c.spiritIds || []).filter(sid => sid !== id);
    });
    const targetCat = state.categories.find(c => c.name === categoryName);
    if (targetCat) {
      if (!targetCat.spiritIds) targetCat.spiritIds = [];
      if (!targetCat.spiritIds.includes(id)) targetCat.spiritIds.push(id);
    }
    state.customCategories[id] = categoryName;
  }

  // Save custom name
  state.customNames[id] = name;

  closeNewSpiritModal();
  renderWorkspace();
  if (isGalleryOpen) renderGallery();
  updateStats();
  triggerAutoSave(50);

  if (window.showToast) {
    window.showToast(`✨ Espíritu #${id} "${name}" creado con éxito!`);
  }
}

export function openEditImageModal(id) {
  state.editingSpiritImageId = id;
  let modal = document.getElementById("edit-image-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "edit-image-modal";
    modal.className = "modal-overlay";
    document.body.appendChild(modal);
  }

  pendingEditImgData = null;
  const currentImgUrl = getSpiritImgUrl(id);
  const spiritName = getSpiritName(id);

  modal.innerHTML = `
    <div class="modal-content" style="max-width: 440px; gap: 14px;">
      <h3 style="margin: 0; font-size: 22px; color: var(--text-color); display: flex; align-items: center; gap: 8px;">
        <span>🖼️</span> Cambiar Imagen (#${id})
      </h3>
      <p style="font-size: 14px; color: var(--text-muted); margin: 0;">
        ${spiritName}
      </p>

      <div style="text-align: center; margin: 8px 0;">
        <div class="img-preview-box" style="display: inline-block; padding: 12px; border-radius: 12px; border: 2px solid var(--card-border); background: repeating-conic-gradient(#1f2233 0% 25%, #131520 0% 50%) 50% / 16px 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.4);">
          <img id="edit-image-preview" src="${currentImgUrl}" alt="Vista previa" style="width: 100px; height: 100px; object-fit: contain; image-rendering: pixelated; display: block;">
        </div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 8px;">
        <label style="display: block; font-size: 13px; font-weight: bold; color: var(--text-color);">Subir Archivo de Sprite (PNG / WEBP):</label>
        <input type="file" id="edit-image-file-input" accept="image/*" style="width: 100%; font-size: 13px; padding: 8px; border-radius: 8px;">
        <input type="text" id="edit-image-url-input" placeholder="O pega enlace directo de imagen..." style="width: 100%; font-size: 13px; padding: 8px; border-radius: 8px;">
      </div>

      <div style="display: flex; gap: 10px; justify-content: space-between; align-items: center; margin-top: 10px;">
        <button type="button" class="btn btn-secondary" onclick="window.resetSpiritImage('${id}')" style="font-size: 13px; border-color: rgba(239,68,68,0.4); color: #f87171;" title="Restablece la imagen a la original por defecto">
          🔄 Restablecer
        </button>
        <div style="display: flex; gap: 8px;">
          <button type="button" class="btn btn-secondary" onclick="window.closeEditImageModal()">Cerrar</button>
          <button type="button" class="btn" onclick="window.saveEditedSpiritImage()" style="font-weight: 700;">
            💾 Guardar Imagen
          </button>
        </div>
      </div>
    </div>
  `;

  modal.classList.add("show");

  const fileInput = document.getElementById("edit-image-file-input");
  const urlInput = document.getElementById("edit-image-url-input");
  const previewImg = document.getElementById("edit-image-preview");

  if (fileInput) {
    fileInput.onchange = async (e) => {
      const file = e.target.files?.[0];
      if (file) {
        try {
          const res = await processSpiritImage(file);
          pendingEditImgData = res.dataUrl;
          if (previewImg) previewImg.src = res.dataUrl;
        } catch(err) {
          console.error("Error al procesar imagen:", err);
        }
      }
    };
  }

  if (urlInput) {
    urlInput.oninput = (e) => {
      const val = e.target.value.trim();
      if (val && previewImg) {
        previewImg.src = val;
        pendingEditImgData = val;
      }
    };
  }
}

export function closeEditImageModal() {
  const modal = document.getElementById("edit-image-modal");
  if (modal) modal.classList.remove("show");
  state.editingSpiritImageId = null;
  pendingEditImgData = null;
}

export async function saveEditedSpiritImage() {
  const id = state.editingSpiritImageId;
  if (!id) {
    closeEditImageModal();
    return;
  }

  if (!pendingEditImgData) {
    closeEditImageModal();
    return;
  }

  let finalUrl = pendingEditImgData;
  if (pendingEditImgData.startsWith("data:")) {
    try {
      if (window.showToast) window.showToast("⏳ Subiendo imagen...", false);
      const resp = await fetch('/api/upload-spirit-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          spiritId: id,
          imageBase64: pendingEditImgData,
          season: state.currentSeason
        })
      });
      const data = await resp.json();
      if (data.success && data.url) {
        finalUrl = data.url;
      }
    } catch(err) {
      console.warn("No se pudo conectar con el servidor de subida, guardando localmente:", err);
    }
  }

  state.customImages[id] = finalUrl;
  state.imageCacheBusters[id] = Date.now();
  
  closeEditImageModal();
  renderWorkspace();
  if (isGalleryOpen) renderGallery();
  triggerAutoSave(50);

  if (window.showToast) {
    window.showToast(`🖼️ Imagen del espíritu #${id} actualizada con éxito!`);
  }
}

export function resetSpiritImage(id) {
  if (!id) return;
  delete state.customImages[id];
  state.imageCacheBusters[id] = Date.now();
  
  closeEditImageModal();
  renderWorkspace();
  if (isGalleryOpen) renderGallery();
  triggerAutoSave(50);

  if (window.showToast) {
    window.showToast(`🔄 Imagen del espíritu #${id} restablecida por defecto`);
  }
}

// Window bindings
window.toggleGalleryDrawer = toggleGalleryDrawer;
window.renderGallery = renderGallery;
window.deleteFromGallery = deleteFromGallery;
window.moveSpiritToCategory = moveSpiritToCategory;
window.removeSpiritFromCategory = removeSpiritFromCategory;
window.permanentlyDeleteSpirit = permanentlyDeleteSpirit;
window.deleteAllUncategorizedSpirits = deleteAllUncategorizedSpirits;
window.deleteCategory = deleteCategory;
window.changeSpiritType = changeSpiritType;
window.openAssignModal = openAssignModal;
window.closeAssignModal = closeAssignModal;
window.openNewSpiritModal = openNewSpiritModal;
window.closeNewSpiritModal = closeNewSpiritModal;
window.saveNewSpirit = saveNewSpirit;
window.openEditImageModal = openEditImageModal;
window.closeEditImageModal = closeEditImageModal;
window.saveEditedSpiritImage = saveEditedSpiritImage;
window.resetSpiritImage = resetSpiritImage;

