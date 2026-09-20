/**
 * edit-view.js - Vista de Modo Edición / Gestión de Ranuras y Galería de Espíritus
 */

import { state } from './state.js';
import { getOptimizedCloudinaryUrl } from './image-utils.js';
import { saveChanges } from './firestore.js';
import { sendWebSocketBroadcast } from './websocket.js';

export function renderEditGrid() {
  const container = document.getElementById('editGridContainer');
  if (!container) return;

  const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
  const currentSlots = state.activeSeason === 1 ? state.season1Slots : state.season2Slots;
  const spiritsMap = new Map(spirits.map(s => [s.id, s]));

  let html = '';
  for (let slotNum = 1; slotNum <= 28; slotNum++) {
    const assignedId = currentSlots[slotNum];
    const spirit = assignedId ? spiritsMap.get(assignedId) : null;
    const isSelected = state.selectedSlot === slotNum;

    if (spirit) {
      const imgUrl = getOptimizedCloudinaryUrl(spirit.imageUrl || spirit.image, 160);
      html += `
        <div class="edit-slot filled ${isSelected ? 'selected' : ''}" 
             data-slot="${slotNum}" 
             onclick="window.selectSpiritSlot(${slotNum})">
          <div class="slot-badge">#${slotNum}</div>
          <button class="remove-btn" title="Desasignar espíritu" onclick="event.stopPropagation(); window.removeSpiritFromSlot(${slotNum})">✕</button>
          <div class="slot-img-wrap">
            <img src="${imgUrl}" alt="${spirit.name}" loading="lazy" onerror="this.src='https://placehold.co/120x120/1e293b/a855f7?text=?';">
          </div>
          <div class="slot-info">
            <div class="slot-name">${spirit.name}</div>
            <div class="slot-type">${spirit.type || 'Común'}</div>
          </div>
        </div>
      `;
    } else {
      html += `
        <div class="edit-slot empty ${isSelected ? 'selected' : ''}" 
             data-slot="${slotNum}" 
             onclick="window.selectSpiritSlot(${slotNum})">
          <div class="slot-badge">#${slotNum}</div>
          <div class="empty-icon">➕</div>
          <div class="empty-label">Ranura Vacía</div>
        </div>
      `;
    }
  }

  container.innerHTML = html;
}

export function renderGallery() {
  const container = document.getElementById('galleryGridContainer');
  if (!container) return;

  const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
  const currentSlots = state.activeSeason === 1 ? state.season1Slots : state.season2Slots;
  const assignedIds = new Set(Object.values(currentSlots).filter(Boolean));

  let filtered = spirits;
  if (state.gallerySearch) {
    const q = state.gallerySearch.toLowerCase().trim();
    filtered = filtered.filter(s => 
      s.name.toLowerCase().includes(q) || 
      (s.type && s.type.toLowerCase().includes(q)) || 
      (s.category && s.category.toLowerCase().includes(q))
    );
  }

  if (state.galleryCategoryFilter !== 'ALL') {
    filtered = filtered.filter(s => s.category === state.galleryCategoryFilter);
  }

  if (state.galleryTypeFilter !== 'ALL') {
    filtered = filtered.filter(s => s.type === state.galleryTypeFilter);
  }

  if (state.galleryUsageFilter === 'ASSIGNED') {
    filtered = filtered.filter(s => assignedIds.has(s.id));
  } else if (state.galleryUsageFilter === 'UNASSIGNED') {
    filtered = filtered.filter(s => !assignedIds.has(s.id));
  }

  let html = '';
  filtered.forEach(spirit => {
    const isAssigned = assignedIds.has(spirit.id);
    const imgUrl = getOptimizedCloudinaryUrl(spirit.imageUrl || spirit.image, 140);
    html += `
      <div class="gallery-card ${isAssigned ? 'assigned' : ''}" 
           data-id="${spirit.id}"
           onclick="window.assignSpiritToSlot(${spirit.id})">
        <div class="gallery-img-box">
          <img src="${imgUrl}" alt="${spirit.name}" loading="lazy" onerror="this.src='https://placehold.co/100x100/1e293b/a855f7?text=?';">
          ${isAssigned ? '<span class="assigned-pill">En uso</span>' : ''}
        </div>
        <div class="gallery-card-meta">
          <div class="gallery-name" title="${spirit.name}">${spirit.name}</div>
          <div class="gallery-tags">
            <span class="gallery-cat">${spirit.category || 'Sin Cat.'}</span>
            <span class="gallery-type">${spirit.type || 'Común'}</span>
          </div>
        </div>
        <div class="gallery-actions">
          <button class="gallery-edit-img-btn" title="Editar en Estudio de Arte" onclick="event.stopPropagation(); window.openStudioModal(${spirit.id});">🎨</button>
        </div>
      </div>
    `;
  });

  if (filtered.length === 0) {
    html = `<div class="empty-gallery-msg">No se encontraron espíritus con los filtros seleccionados.</div>`;
  }

  container.innerHTML = html;
}

export function selectSpiritSlot(slotNumber) {
  state.selectedSlot = slotNumber;
  renderEditGrid();

  // Si es pantalla móvil, abrir modal de selección rápida
  if (window.innerWidth <= 768) {
    openMobileAssignModal(slotNumber);
  }
}

export function assignSpiritToSlot(spiritId) {
  if (!state.selectedSlot) {
    if (window.customAlert) {
      window.customAlert('Primero selecciona una ranura (1 al 28) en la cuadrícula de edición.', 'Aviso');
    } else {
      alert('Primero selecciona una ranura (1 al 28).');
    }
    return;
  }

  const currentSlots = state.activeSeason === 1 ? state.season1Slots : state.season2Slots;
  
  // Si el espíritu ya estaba en otra ranura, desasignarlo de la anterior
  for (const [sNum, sId] of Object.entries(currentSlots)) {
    if (sId === spiritId) {
      currentSlots[sNum] = null;
    }
  }

  currentSlots[state.selectedSlot] = spiritId;
  saveChanges();
  sendWebSocketBroadcast({ type: 'SLOTS_UPDATED', season: state.activeSeason });
  renderEditGrid();
  renderGallery();
  closeMobileAssignModal();
}

export function removeSpiritFromSlot(slotNumber) {
  const currentSlots = state.activeSeason === 1 ? state.season1Slots : state.season2Slots;
  currentSlots[slotNumber] = null;
  saveChanges();
  sendWebSocketBroadcast({ type: 'SLOTS_UPDATED', season: state.activeSeason });
  renderEditGrid();
  renderGallery();
}

export function openMobileAssignModal(slotNumber) {
  state.selectedSlot = slotNumber;
  const modal = document.getElementById('mobileAssignModal');
  const modalSlotTitle = document.getElementById('mobileAssignSlotTitle');
  if (modalSlotTitle) modalSlotTitle.innerText = `Asignar a Ranura #${slotNumber}`;
  if (modal) modal.classList.add('active');
  renderMobileAssignGallery();
}

export function closeMobileAssignModal() {
  const modal = document.getElementById('mobileAssignModal');
  if (modal) modal.classList.remove('active');
}

function renderMobileAssignGallery() {
  const container = document.getElementById('mobileAssignGallery');
  if (!container) return;

  const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
  let html = '';
  spirits.forEach(spirit => {
    const imgUrl = getOptimizedCloudinaryUrl(spirit.imageUrl || spirit.image, 120);
    html += `
      <div class="mobile-spirit-opt" onclick="window.assignSpiritToSlot(${spirit.id})">
        <img src="${imgUrl}" alt="${spirit.name}">
        <span>${spirit.name}</span>
      </div>
    `;
  });
  container.innerHTML = html;
}

export function openNewSpiritModal() {
  const modal = document.getElementById('newSpiritModal');
  if (modal) modal.classList.add('active');
}

export function closeNewSpiritModal() {
  const modal = document.getElementById('newSpiritModal');
  if (modal) modal.classList.remove('active');
}

export async function saveNewSpirit() {
  const nameInput = document.getElementById('newSpiritName');
  const catInput = document.getElementById('newSpiritCat');
  const typeInput = document.getElementById('newSpiritType');
  const imgInput = document.getElementById('newSpiritImgUrl');

  const name = nameInput?.value.trim();
  if (!name) {
    if (window.customAlert) window.customAlert('Ingresa un nombre válido para el nuevo espíritu.', 'Error');
    return;
  }

  const spirits = state.activeSeason === 1 ? state.season1Spirits : state.season2Spirits;
  const maxId = spirits.reduce((max, s) => Math.max(max, s.id || 0), 0);
  const nextId = maxId + 1;

  const newSpirit = {
    id: nextId,
    name: name,
    category: catInput?.value || 'Común',
    type: typeInput?.value || 'Común',
    imageUrl: imgInput?.value.trim() || 'https://res.cloudinary.com/dvzjcxvkr/image/upload/v1722448408/spirits/ic_spirit_01.png'
  };

  spirits.push(newSpirit);
  saveChanges();
  sendWebSocketBroadcast({ type: 'SPIRIT_CREATED', season: state.activeSeason, spirit: newSpirit });
  
  if (nameInput) nameInput.value = '';
  if (imgInput) imgInput.value = '';
  closeNewSpiritModal();
  renderGallery();
  if (window.customToast) window.customToast(`Espíritu "${name}" creado con éxito ✨`);
}

// Window bindings
window.renderEditGrid = renderEditGrid;
window.renderGallery = renderGallery;
window.selectSpiritSlot = selectSpiritSlot;
window.assignSpiritToSlot = assignSpiritToSlot;
window.removeSpiritFromSlot = removeSpiritFromSlot;
window.openMobileAssignModal = openMobileAssignModal;
window.closeMobileAssignModal = closeMobileAssignModal;
window.openNewSpiritModal = openNewSpiritModal;
window.closeNewSpiritModal = closeNewSpiritModal;
window.saveNewSpirit = saveNewSpirit;
