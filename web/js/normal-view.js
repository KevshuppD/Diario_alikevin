/**
 * normal-view.js - Vista de Modo Normal / Álbum, Renderizado de Cuadrícula, Filtros y Estadísticas
 */

import { state } from './state.js';
import { defaultNames, defaultSpiritsList, defaultCategories } from './constants.js';
import { triggerAutoSave } from './firestore.js';
import { sendWsMessage } from './websocket.js';
import { optimizeCloudinaryUrl } from './image-utils.js';
import { openAssignModal, openEditImageModal, deleteFromGallery, removeSpiritFromCategory, deleteCategory, deleteAllUncategorizedSpirits, permanentlyDeleteSpirit, changeSpiritType } from './edit-view.js';

export function getSpiritImgUrl(id) {
  if (state.customImages && state.customImages[id]) {
    const bust = state.imageCacheBusters?.[id] ? `?v=${state.imageCacheBusters[id]}` : '';
    return optimizeCloudinaryUrl(state.customImages[id]) + bust;
  }
  const formattedId = String(id).padStart(2, '0');
  const base = state.currentSeason === 2
    ? `https://res.cloudinary.com/dhaqjw7se/image/upload/spirits_s2/ic_spirit_s2_${formattedId}.png`
    : `https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_${formattedId}.png`;
  const bust = state.imageCacheBusters?.[id] ? `?v=${state.imageCacheBusters[id]}` : '';
  return optimizeCloudinaryUrl(base) + bust;
}

export function handleSpiritImgError(imgEl, id) {
  imgEl.onerror = null;
  imgEl.src = 'https://placehold.co/100x100/1e293b/a855f7?text=?';
}

export function getSpiritName(id) {
  if (state.customNames && state.customNames[id]) return state.customNames[id];
  const idx = parseInt(id, 10) - 1;
  return defaultNames[idx] || `Espíritu #${id}`;
}

export function getSpiritBaseName(id) {
  const currentName = getSpiritName(id);
  const sortedTypes = getSortedTypesWithSuffix();
  for (const t of sortedTypes) {
    if (t.suffix && t.suffix.trim() && currentName.endsWith(t.suffix.trim())) {
      return currentName.slice(0, -t.suffix.length).trim();
    }
  }
  return currentName;
}

export function getSpiritBaseCategoryName(catName) {
  if (!catName || catName === "__uncategorized__") return "";
  return state.customCategories[catName] || catName;
}

export function getSortedTypesWithSuffix() {
  return [...state.spiritTypes]
    .filter(t => t.suffix && t.suffix.trim())
    .sort((a, b) => b.suffix.trim().length - a.suffix.trim().length);
}

export function getSpiritCurrentType(id) {
  const currentName = getSpiritName(id);
  for (let i = state.spiritTypes.length - 1; i >= 0; i--) {
    const t = state.spiritTypes[i];
    if (t.suffix && t.suffix.trim() && currentName.endsWith(t.suffix.trim())) {
      return t.name;
    }
  }
  return "Normal";
}

export function computeSpiritName(id, targetCategoryName, newTypeName) {
  let baseCatName = getSpiritBaseCategoryName(targetCategoryName);
  if (!baseCatName) {
    const foundCat = state.categories.find(c => (c.spiritIds || []).includes(id));
    if (foundCat) {
      baseCatName = getSpiritBaseCategoryName(foundCat.name);
    }
  }
  if (!baseCatName) {
    baseCatName = getSpiritBaseName(id);
  }

  const typeObj = state.spiritTypes.find(t => t.name === newTypeName);
  const suffix = typeObj ? typeObj.suffix : "";

  if (id === "13" && newTypeName === "Normal" && baseCatName.includes("Cacahuete")) {
    return "TheBurntPeanut (Espíritu del Cacahuete)";
  }

  return baseCatName + suffix;
}

export function matchesFilter(id) {
  if (state.searchQuery !== "") {
    const name = getSpiritName(id).toLowerCase();
    const type = getSpiritCurrentType(id).toLowerCase();
    const numStr = String(id).padStart(2, '0');
    const hashNum = `#${numStr}`;
    const plainId = String(parseInt(id, 10));

    const matchesQuery = name.includes(state.searchQuery) ||
                         type.includes(state.searchQuery) ||
                         numStr.includes(state.searchQuery) ||
                         hashNum.includes(state.searchQuery) ||
                         plainId === state.searchQuery;

    if (!matchesQuery) return false;
  }

  const isKevinOwned = state.kevinList.includes(id);
  const isAliOwned = state.aliList.includes(id);
  const isKevinMastered = state.kevinMastery.includes(id);
  const isAliMastered = state.aliMastery.includes(id);

  const isKevinUser = state.currentUser ? state.currentUser.username === "kevin" : true;
  const isOwnedByCurrent = isKevinUser ? isKevinOwned : isAliOwned;
  const isOwnedByOther = isKevinUser ? isAliOwned : isKevinOwned;
  const isMasteredByCurrent = isKevinUser ? isKevinMastered : isAliMastered;
  const isMasteredByOther = isKevinUser ? isAliMastered : isKevinMastered;

  switch (state.currentFilter) {
    case "todos":
    case "all":
      return true;
    case "no_obtenidos":
      return !isOwnedByCurrent;
    case "obtenidos_otro_no_yo":
      return isOwnedByOther && !isOwnedByCurrent;
    case "obtenidos_yo_no_otro":
      return isOwnedByCurrent && !isOwnedByOther;
    case "obtenidos_ambos":
      return isKevinOwned && isAliOwned;
    case "obtenidos_sin_maestria":
      return isOwnedByCurrent && !isMasteredByCurrent;
    case "sin_maestria":
      return !isMasteredByCurrent;
    case "maestria_otro_no_yo":
      return isMasteredByOther && !isMasteredByCurrent;
    case "maestria_yo_no_otro":
      return isMasteredByCurrent && !isMasteredByOther;
    default:
      return true;
  }
}

export function updateStats() {
  const activeSpirits = state.currentSeason === 1 ? (state.spiritsList.length > 0 ? state.spiritsList : defaultSpiritsList) : state.spiritsList;

  const assignedIds = new Set();
  state.categories.forEach(cat => {
    (cat.spiritIds || []).forEach(id => {
      if (activeSpirits.includes(id)) assignedIds.add(id);
    });
  });

  const totalRegistered = assignedIds.size > 0 ? assignedIds.size : activeSpirits.length;
  
  const totalKevin = state.kevinList.filter(id => assignedIds.has(id) || activeSpirits.includes(id)).length;
  const totalAli = state.aliList.filter(id => assignedIds.has(id) || activeSpirits.includes(id)).length;
  const bothCount = activeSpirits.filter(id => state.kevinList.includes(id) && state.aliList.includes(id)).length;

  const kevinMasteryCount = state.kevinMastery.filter(id => assignedIds.has(id) || activeSpirits.includes(id)).length;
  const aliMasteryCount = state.aliMastery.filter(id => assignedIds.has(id) || activeSpirits.includes(id)).length;

  const statKevin = document.getElementById("stat-kevin");
  const statAli = document.getElementById("stat-ali");
  const statBoth = document.getElementById("stat-both");

  if (statKevin) statKevin.innerHTML = `${totalKevin} / ${totalRegistered} <span class="mastery-count-tag" title="Maestría (Estrellas) de Kevin">⭐ ${kevinMasteryCount}</span>`;
  if (statAli) statAli.innerHTML = `${totalAli} / ${totalRegistered} <span class="mastery-count-tag" title="Maestría (Estrellas) de Ali">⭐ ${aliMasteryCount}</span>`;
  if (statBoth) statBoth.textContent = `${bothCount} / ${totalRegistered}`;
}

export function toggleSpiritOwned(id) {
  const isKevin = state.currentUser ? state.currentUser.username === "kevin" : true;
  const userList = isKevin ? state.kevinList : state.aliList;
  const userKey = isKevin ? "kevin_list" : "ali_list";
  const masteryList = isKevin ? state.kevinMastery : state.aliMastery;
  const masteryKey = isKevin ? "kevin_mastery" : "ali_mastery";

  let newOwnedList;
  let updates = {};

  if (userList.includes(id)) {
    newOwnedList = userList.filter(item => item !== id);
    if (isKevin) state.kevinList = newOwnedList; else state.aliList = newOwnedList;

    if (masteryList.includes(id)) {
      const newMastery = masteryList.filter(item => item !== id);
      if (isKevin) state.kevinMastery = newMastery; else state.aliMastery = newMastery;
      updates[masteryKey] = newMastery;
    }
  } else {
    newOwnedList = [...userList, id];
    if (isKevin) state.kevinList = newOwnedList; else state.aliList = newOwnedList;
  }

  updates[userKey] = newOwnedList;
  updateStats();
  renderWorkspace();

  triggerAutoSave(150);
  sendWsMessage({
    type: 'SPIRIT_TOGGLE',
    coupleId: state.coupleId,
    season: state.currentSeason,
    kevinList: state.kevinList,
    aliList: state.aliList,
    kevinMastery: state.kevinMastery,
    aliMastery: state.aliMastery,
    timestamp: Date.now()
  });
}

export function toggleSpiritMastery(id, event) {
  if (event) event.stopPropagation();
  const isKevin = state.currentUser ? state.currentUser.username === "kevin" : true;
  const masteryList = isKevin ? state.kevinMastery : state.aliMastery;
  const masteryKey = isKevin ? "kevin_mastery" : "ali_mastery";
  const userList = isKevin ? state.kevinList : state.aliList;
  const userKey = isKevin ? "kevin_list" : "ali_list";

  let updates = {};

  if (masteryList.includes(id)) {
    const newMastery = masteryList.filter(item => item !== id);
    if (isKevin) state.kevinMastery = newMastery; else state.aliMastery = newMastery;
    updates[masteryKey] = newMastery;
  } else {
    const newMastery = [...masteryList, id];
    if (isKevin) state.kevinMastery = newMastery; else state.aliMastery = newMastery;
    updates[masteryKey] = newMastery;

    if (!userList.includes(id)) {
      const newOwned = [...userList, id];
      if (isKevin) state.kevinList = newOwned; else state.aliList = newOwned;
      updates[userKey] = newOwned;
    }
  }

  updateStats();
  renderWorkspace();
  triggerAutoSave(150);
  sendWsMessage({
    type: 'SPIRIT_TOGGLE',
    coupleId: state.coupleId,
    season: state.currentSeason,
    kevinList: state.kevinList,
    aliList: state.aliList,
    kevinMastery: state.kevinMastery,
    aliMastery: state.aliMastery,
    timestamp: Date.now()
  });
}

export function onSearchInput(val) {
  state.searchQuery = val.trim().toLowerCase();
  const clearBtn = document.getElementById("clear-search-btn");
  if (clearBtn) {
    clearBtn.style.display = state.searchQuery.length > 0 ? "inline-block" : "none";
  }
  renderWorkspace();
}

export function clearSearch() {
  state.searchQuery = "";
  const input = document.getElementById("spirit-search-input");
  if (input) input.value = "";
  const clearBtn = document.getElementById("clear-search-btn");
  if (clearBtn) clearBtn.style.display = "none";
  renderWorkspace();
}

export function setFilter(filterName, btnEl) {
  state.currentFilter = filterName;
  document.querySelectorAll(".filter-bar .filter-btn").forEach(b => b.classList.remove("active"));
  if (btnEl) btnEl.classList.add("active");
  renderWorkspace();
}

export function createSpiritSlot(id, categoryName) {
  const slot = document.createElement("div");
  const displayNumber = String(id).padStart(2, '0');

  if (state.currentMode === "normal") {
    const isKevinOwned = state.kevinList.includes(id);
    const isAliOwned = state.aliList.includes(id);
    const isKevinMastered = state.kevinMastery.includes(id);
    const isAliMastered = state.aliMastery.includes(id);

    let ownedStatusClass = "";
    if (isKevinOwned && isAliOwned) ownedStatusClass = "owned-both";
    else if (isKevinOwned) ownedStatusClass = "owned-kevin";
    else if (isAliOwned) ownedStatusClass = "owned-ali";
    else ownedStatusClass = "owned-none";

    slot.className = `spirit-slot normal-view ${ownedStatusClass}`;
    slot.dataset.id = id;
    slot.onclick = () => toggleSpiritOwned(id);

    const isKevinUser = state.currentUser ? state.currentUser.username === "kevin" : true;
    const isCurrentMastered = isKevinUser ? isKevinMastered : isAliMastered;

    slot.innerHTML = `
      <span class="spirit-id-badge">#${displayNumber}</span>
      <div class="spirit-img-container">
        <img src="${getSpiritImgUrl(id)}" alt="Espíritu ${id}" loading="lazy" decoding="async" onerror="window.handleSpiritImgError(this, '${id}')">
        <span class="mastery-star ${isCurrentMastered ? 'active' : ''}" onclick="window.toggleSpiritMastery('${id}', event)" title="Alternar Maestría ⭐">
          ⭐
        </span>
      </div>
      <div class="spirit-name">${getSpiritName(id)}</div>
      <div class="ownership-badges">
        <span class="owner-tag kevin ${isKevinOwned ? 'active' : ''}" title="${isKevinOwned ? 'Obtenido por Kevin' : 'Faltante'}">
          🔵 K ${isKevinMastered ? '<span style="color:#fbbf24;">⭐</span>' : ''}
        </span>
        <span class="owner-tag ali ${isAliOwned ? 'active' : ''}" title="${isAliOwned ? 'Obtenido por Ali' : 'Faltante'}">
          🔴 A ${isAliMastered ? '<span style="color:#fbbf24;">⭐</span>' : ''}
        </span>
      </div>
    `;
  } else {
    // Modo Edición
    slot.className = "spirit-slot edit-mode-slot";
    slot.draggable = true;
    slot.dataset.id = id;
    slot.addEventListener("dragstart", (e) => {
      e.dataTransfer.setData("text/plain", id);
      e.dataTransfer.setData("spirit-id", id);
      e.dataTransfer.effectAllowed = "move";
    });

    let optionsHtml = `<option value="" disabled selected>Mover a...</option>`;
    state.categories.forEach(c => {
      const isCurrent = c.name === categoryName;
      optionsHtml += `<option value="${c.name}" ${isCurrent ? 'selected' : ''}>${state.customCategories[c.name] || c.name}</option>`;
    });
    optionsHtml += `<option value="__uncategorized__" ${categoryName === "__uncategorized__" ? 'selected' : ''}>Sin Categoría / Suelto</option>`;

    const currentType = getSpiritCurrentType(id);
    let typesHtml = "";
    state.spiritTypes.forEach(t => {
      typesHtml += `<option value="${t.name}" ${t.name === currentType ? 'selected' : ''}>${t.name}</option>`;
    });

    const deleteBtnHtml = categoryName === "__uncategorized__" 
      ? `<button class="remove-btn" onclick="window.permanentlyDeleteSpirit('${id}')" title="Eliminar de BD e imagen">🗑</button>`
      : `<button class="remove-btn" onclick="window.removeSpiritFromCategory('${id}', '${categoryName}')" title="Quitar de categoría">×</button>`;

    slot.innerHTML = `
      ${deleteBtnHtml}
      <span class="spirit-id-badge">#${displayNumber}</span>
      <img src="${getSpiritImgUrl(id)}" alt="Espíritu ${id}" loading="lazy" decoding="async" draggable="false" style="cursor: pointer;" onclick="window.openEditImageModal('${id}')" title="Haz clic para cambiar la imagen" onerror="window.handleSpiritImgError(this, '${id}')">
      <button type="button" class="btn" style="padding: 2px 4px; font-size: 9px; margin-bottom: 4px; width: 100%; font-family: inherit; justify-content: center;" onclick="window.openEditImageModal('${id}')">🖼️ Cambiar Imagen</button>
      <input type="text" value="${getSpiritName(id)}" placeholder="Nombre de Espíritu" data-id="${id}" style="margin-bottom: 4px;">
      <select class="type-select" data-id="${id}" style="width: 100%; font-size: 10px; border-radius: 4px; padding: 2px; margin-bottom: 4px;">
        ${typesHtml}
      </select>
      <select class="category-select" data-id="${id}" style="width: 100%; font-size: 10px; border-radius: 4px; padding: 2px;">
        ${optionsHtml}
      </select>
    `;

    const nameInput = slot.querySelector("input");
    const handleNameUpdate = (e) => {
      const newVal = e.target.value.trim();
      const sid = e.target.dataset.id;
      const defName = defaultNames[parseInt(sid, 10) - 1] || "";
      if (newVal === "" || newVal === defName) {
        delete state.customNames[sid];
      } else {
        state.customNames[sid] = newVal;
      }
      triggerAutoSave(400);
    };
    nameInput.addEventListener("input", handleNameUpdate);
    nameInput.addEventListener("change", handleNameUpdate);

    const typeSelectEl = slot.querySelector(".type-select");
    typeSelectEl.addEventListener("change", (e) => {
      changeSpiritType(id, e.target.value);
      triggerAutoSave(50);
    });

    const selectEl = slot.querySelector(".category-select");
    selectEl.addEventListener("change", (e) => {
      window.moveSpiritToCategory(id, e.target.value);
      triggerAutoSave(50);
    });
  }

  return slot;
}

export function renderWorkspace() {
  const container = document.getElementById("categories-container");
  if (!container) return;

  const activeSpirits = state.currentSeason === 1 ? (state.spiritsList.length > 0 ? state.spiritsList : defaultSpiritsList) : state.spiritsList;

  container.innerHTML = "";

  const assignedIds = new Set();
  state.categories.forEach(cat => {
    (cat.spiritIds || []).forEach(id => assignedIds.add(id));
  });

  const workspaceFragment = document.createDocumentFragment();

  // Render Categories
  state.categories.forEach((cat, catIdx) => {
    const matchingSpirits = (cat.spiritIds || []).filter(id => activeSpirits.includes(id) && matchesFilter(id));
    
    if (state.currentMode === "normal" && matchingSpirits.length === 0) return;

    const card = document.createElement("div");
    card.className = "category-card";
    
    const header = document.createElement("div");
    header.className = "category-header";
    
    const customTitle = state.customCategories[cat.name] || cat.name;
    
    if (state.currentMode === "edit") {
      header.innerHTML = `
        <div class="category-title-edit">
          <span style="font-size: 20px;">📂</span>
          <input type="text" value="${customTitle}" placeholder="Nombre de categoría" data-original="${cat.name}">
        </div>
        <button class="btn btn-danger" style="padding: 4px 8px; font-size: 11px;" onclick="window.deleteCategory(${catIdx})">Eliminar</button>
      `;

      const titleInput = header.querySelector("input");
      titleInput.addEventListener("input", (e) => {
        const rawVal = e.target.value.trim();
        const orig = e.target.dataset.original;
        if (rawVal === "" || rawVal === orig) {
          delete state.customCategories[orig];
        } else {
          state.customCategories[orig] = rawVal;
        }
        triggerAutoSave(600);
      });
    } else {
      const catKevinCount = (cat.spiritIds || []).filter(id => state.kevinList.includes(id)).length;
      const catAliCount = (cat.spiritIds || []).filter(id => state.aliList.includes(id)).length;
      const catKevinMastery = (cat.spiritIds || []).filter(id => state.kevinMastery.includes(id)).length;
      const catAliMastery = (cat.spiritIds || []).filter(id => state.aliMastery.includes(id)).length;

      header.innerHTML = `
        <div class="category-title-edit" style="flex-wrap: wrap; gap: 8px 12px;">
          <span style="font-size: 20px;">📂</span>
          <h3 style="font-family: 'Outfit', sans-serif; font-size: 18px; font-weight: 700; color: var(--text-color);">${customTitle}</h3>
          <span style="font-size: 12px; color: var(--text-muted);">(${matchingSpirits.length}/${(cat.spiritIds || []).length})</span>
          <div style="display: inline-flex; gap: 6px; align-items: center;">
            <span class="cat-stat-badge kevin" title="Obtenidos / Maestría de Kevin">🔵 ${catKevinCount} <span style="color:#fbbf24; font-weight:700;">⭐${catKevinMastery}</span></span>
            <span class="cat-stat-badge ali" title="Obtenidos / Maestría de Ali">🔴 ${catAliCount} <span style="color:#fbbf24; font-weight:700;">⭐${catAliMastery}</span></span>
          </div>
        </div>
      `;
    }

    const slotsGrid = document.createElement("div");
    slotsGrid.className = "spirit-slots-grid";
    slotsGrid.dataset.categoryName = cat.name;

    const slotsFragment = document.createDocumentFragment();
    matchingSpirits.forEach(id => {
      const slot = createSpiritSlot(id, cat.name);
      slotsFragment.appendChild(slot);
    });
    slotsGrid.appendChild(slotsFragment);

    if (state.currentMode === "edit") {
      slotsGrid.addEventListener("dragover", (e) => {
        e.preventDefault();
        slotsGrid.classList.add("drag-over");
      });

      slotsGrid.addEventListener("dragleave", () => {
        slotsGrid.classList.remove("drag-over");
      });

      slotsGrid.addEventListener("drop", (e) => {
        e.preventDefault();
        slotsGrid.classList.remove("drag-over");
        const rawData = e.dataTransfer.getData("text/plain") || e.dataTransfer.getData("text/uri-list");
        if (rawData) {
          window.moveSpiritToCategory(rawData, cat.name);
        }
      });
    }

    card.appendChild(header);
    card.appendChild(slotsGrid);
    workspaceFragment.appendChild(card);
  });

  // Render Uncategorized / Loose Spirits
  const uncategorizedIds = activeSpirits.filter(id => !assignedIds.has(id) && matchesFilter(id));
  
  if (state.currentMode === "edit" || uncategorizedIds.length > 0) {
    const looseCard = document.createElement("div");
    looseCard.className = "category-card uncategorized-bin";
    const uncatKevinCount = uncategorizedIds.filter(id => state.kevinList.includes(id)).length;
    const uncatAliCount = uncategorizedIds.filter(id => state.aliList.includes(id)).length;
    const uncatKevinMastery = uncategorizedIds.filter(id => state.kevinMastery.includes(id)).length;
    const uncatAliMastery = uncategorizedIds.filter(id => state.aliMastery.includes(id)).length;

    const deleteMassBtn = (state.currentMode === "edit" && uncategorizedIds.length > 0)
      ? `<button class="btn btn-danger" style="padding: 4px 10px; font-size: 11px; margin-left: auto; display: inline-flex; align-items: center; gap: 4px; font-weight: 600;" onclick="window.deleteAllUncategorizedSpirits()" title="Eliminar espíritus sueltos">🗑️ Eliminar Sueltos (${uncategorizedIds.length})</button>`
      : '';

    looseCard.innerHTML = `
      <div class="category-header">
        <div class="category-title-edit" style="flex-wrap: wrap; gap: 8px 12px; width: 100%; align-items: center;">
          <span style="font-size: 20px;">📦</span>
          <span style="font-family: 'Outfit', sans-serif; font-size: 16px; font-weight: 700; color: var(--error-color);">Sin Categoría / Sueltos</span>
          <span style="font-size: 12px; color: var(--text-muted);">(${uncategorizedIds.length})</span>
          ${state.currentMode === 'normal' ? `
            <div style="display: inline-flex; gap: 6px; align-items: center;">
              <span class="cat-stat-badge kevin" title="Obtenidos / Maestría de Kevin">🔵 ${uncatKevinCount} <span style="color:#fbbf24; font-weight:700;">⭐${uncatKevinMastery}</span></span>
              <span class="cat-stat-badge ali" title="Obtenidos / Maestría de Ali">🔴 ${uncatAliCount} <span style="color:#fbbf24; font-weight:700;">⭐${uncatAliMastery}</span></span>
            </div>
          ` : ''}
          ${deleteMassBtn}
        </div>
      </div>
    `;

    const looseGrid = document.createElement("div");
    looseGrid.className = "spirit-slots-grid";
    looseGrid.dataset.categoryName = "__uncategorized__";

    const looseSlotsFragment = document.createDocumentFragment();
    uncategorizedIds.forEach(id => {
      const slot = createSpiritSlot(id, "__uncategorized__");
      looseSlotsFragment.appendChild(slot);
    });
    looseGrid.appendChild(looseSlotsFragment);

    looseCard.appendChild(looseGrid);
    workspaceFragment.appendChild(looseCard);
  }

  container.appendChild(workspaceFragment);
}

// Window bindings
window.renderWorkspace = renderWorkspace;
window.renderNormalGrid = renderWorkspace;
window.renderNormalStats = updateStats;
window.toggleSpiritOwned = toggleSpiritOwned;
window.toggleSpiritMastery = toggleSpiritMastery;
window.onSearchInput = onSearchInput;
window.clearSearch = clearSearch;
window.setNormalFilter = setFilter;
window.setFilter = setFilter;
window.handleSpiritImgError = handleSpiritImgError;
