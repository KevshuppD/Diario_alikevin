/**
 * edit-view.js - Vista de Modo Edición, Gestión de Ranuras/Categorías, Galería y Modales
 */

import { state } from './state.js';
import { defaultNames, defaultSpiritsList, defaultCategories, defaultSpiritsListT1, defaultSpiritsListT2 } from './constants.js';
import { triggerAutoSave } from './firestore.js';
import { getSpiritImgUrl, getSpiritName, getSpiritCurrentType, computeSpiritName, handleSpiritImgError, renderWorkspace, updateStats, matchesFilter } from './normal-view.js';
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

  let unassignedCount = 0;
  const galleryFragment = document.createDocumentFragment();

  listToRender.forEach((id, index) => {
    if (state.searchQuery !== "" && !matchesFilter(id)) return;

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
      <img src="${getSpiritImgUrl(id)}" alt="Espíritu ${id}" loading="lazy" decoding="async" draggable="false" onerror="window.handleSpiritImgError(this, '${id}')">
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
    const defName = defaultNames[parseInt(formattedId, 10) - 1] || "";
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
  const defName = defaultNames[parseInt(id, 10) - 1] || "";

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

export function openNewSpiritModal() {
  const modal = document.getElementById("new-spirit-modal") || document.getElementById("newSpiritModal");
  if (modal) {
    modal.classList.add("active");
    modal.classList.add("show");
  }
}

export function closeNewSpiritModal() {
  const modal = document.getElementById("new-spirit-modal") || document.getElementById("newSpiritModal");
  if (modal) {
    modal.classList.remove("active");
    modal.classList.remove("show");
  }
}

export function openEditImageModal(id) {
  state.editingSpiritImageId = id;
  const modal = document.getElementById("edit-image-modal");
  if (!modal) return;
  const title = document.getElementById("edit-image-modal-title");
  if (title) title.textContent = `🖼️ Cambiar Imagen - ${getSpiritName(id)} (#${id})`;
  const preview = document.getElementById("edit-image-preview");
  if (preview) preview.src = getSpiritImgUrl(id);
  modal.classList.add("show");
}

export function closeEditImageModal() {
  const modal = document.getElementById("edit-image-modal");
  if (modal) modal.classList.remove("show");
  state.editingSpiritImageId = null;
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
window.openEditImageModal = openEditImageModal;
window.closeEditImageModal = closeEditImageModal;
