// ==========================================
// NORMAL VIEW - SPIRITS GRID & FILTERS
// ==========================================

import { state } from './state.js';
import { defaultNames, defaultSpiritsList } from './constants.js';
import { triggerAutoSave, saveChangesToFirestoreAsync } from './firestore.js';
import { sendWsMessage } from './websocket.js';
import { optimizeCloudinaryUrl } from './image-utils.js';

export function getSpiritImgUrl(id) {
  if (state.customImages && state.customImages[id]) {
    const bust = state.imageCacheBusters[id] ? `?v=${state.imageCacheBusters[id]}` : '';
    return optimizeCloudinaryUrl(state.customImages[id]) + bust;
  }
  const formattedId = String(id).padStart(2, '0');
  const base = state.currentSeason === 2
    ? `https://res.cloudinary.com/dhaqjw7se/image/upload/spirits_s2/ic_spirit_s2_${formattedId}.png`
    : `https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_${formattedId}.png`;
  const bust = state.imageCacheBusters[id] ? `?v=${state.imageCacheBusters[id]}` : '';
  return optimizeCloudinaryUrl(base) + bust;
}

export function getSpiritName(id) {
  if (state.customNames && state.customNames[id]) return state.customNames[id];
  const idx = parseInt(id, 10) - 1;
  return defaultNames[idx] || `Espíritu #${id}`;
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

export function updateStats() {
  const activeSpirits = state.currentSeason === 1 ? (state.spiritsList.length > 0 ? state.spiritsList : defaultSpiritsList) : state.spiritsList;

  const assignedIds = new Set();
  state.categories.forEach(cat => {
    cat.spiritIds.forEach(id => {
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

export function setFilter(filterName, btnEl) {
  state.currentFilter = filterName;
  document.querySelectorAll(".filter-bar .filter-btn").forEach(b => b.classList.remove("active"));
  if (btnEl) btnEl.classList.add("active");
  renderWorkspace();
}

export function onSearchInput(val) {
  state.searchQuery = val.trim().toLowerCase();
  const clearBtn = document.getElementById("clear-search-btn");
  if (clearBtn) clearBtn.style.display = state.searchQuery ? "inline-flex" : "none";
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

export function toggleSpiritOwned(id) {
  if (!state.currentUser) return;
  const isKevin = state.currentUser.username === "kevin";
  const userList = isKevin ? state.kevinList : state.aliList;
  const userKey = state.currentUser.key;
  const masteryList = isKevin ? state.kevinMastery : state.aliMastery;
  const masteryKey = state.currentUser.masteryKey;

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
}

export function toggleSpiritMastery(id, event) {
  if (event) event.stopPropagation();
  if (!state.currentUser) return;
  const isKevin = state.currentUser.username === "kevin";
  const masteryList = isKevin ? state.kevinMastery : state.aliMastery;
  const masteryKey = state.currentUser.masteryKey;
  const userList = isKevin ? state.kevinList : state.aliList;
  const userKey = state.currentUser.key;

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
}

export function renderWorkspace() {
  const container = document.getElementById("categories-container");
  if (!container) return;

  if (state.currentMode !== "normal" && state.currentMode !== "edit") {
    container.innerHTML = "";
    return;
  }

  const isEdit = state.currentMode === "edit";
  let html = "";

  state.categories.forEach((cat, catIndex) => {
    let filteredSpiritIds = cat.spiritIds || [];

    if (!isEdit) {
      if (state.searchQuery) {
        filteredSpiritIds = filteredSpiritIds.filter(id => {
          const sName = getSpiritName(id).toLowerCase();
          const sNum = String(id).toLowerCase();
          const sFormatted = String(id).padStart(2, '0').toLowerCase();
          return sName.includes(state.searchQuery) || sNum.includes(state.searchQuery) || sFormatted.includes(state.searchQuery);
        });
      }

      if (state.currentFilter !== "todos") {
        const isKevin = state.currentUser && state.currentUser.username === "kevin";
        const mySpirits = isKevin ? state.kevinList : state.aliList;
        const partnerSpirits = isKevin ? state.aliList : state.kevinList;
        const myMastery = isKevin ? state.kevinMastery : state.aliMastery;
        const partnerMastery = isKevin ? state.aliMastery : state.kevinMastery;

        filteredSpiritIds = filteredSpiritIds.filter(id => {
          const hasMe = mySpirits.includes(id);
          const hasPartner = partnerSpirits.includes(id);
          const hasMyMastery = myMastery.includes(id);
          const hasPartnerMastery = partnerMastery.includes(id);

          switch (state.currentFilter) {
            case "no_obtenidos": return !hasMe;
            case "obtenidos_otro_no_yo": return hasPartner && !hasMe;
            case "obtenidos_yo_no_otro": return hasMe && !hasPartner;
            case "obtenidos_ambos": return hasMe && hasPartner;
            case "obtenidos_sin_maestria": return hasMe && !hasMyMastery;
            case "sin_maestria": return !hasMyMastery;
            case "maestria_otro_no_yo": return hasPartnerMastery && !hasMyMastery;
            case "maestria_yo_no_otro": return hasMyMastery && !hasPartnerMastery;
            default: return true;
          }
        });
      }
    }

    if (!isEdit && filteredSpiritIds.length === 0 && (state.searchQuery || state.currentFilter !== "todos")) {
      return;
    }

    const catTitle = state.customCategories[cat.name] || cat.name;

    html += `
      <div class="category-card" data-category-name="${cat.name}">
        <div class="category-header">
          <div class="category-title">${catTitle}</div>
          <span class="badge" style="background: rgba(255,255,255,0.08); padding: 3px 8px; border-radius: 6px; font-size: 11px;">
            ${filteredSpiritIds.length} ${filteredSpiritIds.length === 1 ? 'espíritu' : 'espíritus'}
          </span>
        </div>
        <div class="spirit-slots-grid" data-category-index="${catIndex}">
          ${filteredSpiritIds.map(id => renderSpiritSlot(id, isEdit, cat.name)).join('')}
        </div>
      </div>
    `;
  });

  container.innerHTML = html;
}

function renderSpiritSlot(id, isEdit, categoryName) {
  const hasKevin = state.kevinList.includes(id);
  const hasAli = state.aliList.includes(id);
  const hasKevinMastery = state.kevinMastery.includes(id);
  const hasAliMastery = state.aliMastery.includes(id);
  const isKevinUser = state.currentUser && state.currentUser.username === "kevin";
  const myMastery = isKevinUser ? hasKevinMastery : hasAliMastery;
  const isOwnedByMe = isKevinUser ? hasKevin : hasAli;

  const spiritName = getSpiritName(id);
  const imgUrl = getSpiritImgUrl(id);
  const formattedId = String(id).padStart(2, '0');

  let ownershipClass = "";
  if (hasKevin && hasAli) ownershipClass = "both";
  else if (hasKevin) ownershipClass = "kevin";
  else if (hasAli) ownershipClass = "ali";

  if (isEdit) {
    return `
      <div class="spirit-slot edit-mode ${ownershipClass}" data-spirit-id="${id}" draggable="true">
        <div class="spirit-id-badge">#${formattedId}</div>
        <button type="button" class="btn-slot-remove" onclick="removeSpiritFromCategory('${id}')" title="Quitar espíritu">✕</button>
        <img class="spirit-img" src="${imgUrl}" alt="${spiritName}" loading="lazy">
        <div class="spirit-name" title="${spiritName}">${spiritName}</div>
      </div>
    `;
  }

  return `
    <div class="spirit-slot ${ownershipClass} ${isOwnedByMe ? 'owned' : ''}" onclick="window.toggleSpiritOwned('${id}')">
      <div class="spirit-id-badge">#${formattedId}</div>
      <button type="button" class="star-mastery-btn ${myMastery ? 'active' : ''}" onclick="window.toggleSpiritMastery('${id}', event)" title="Alternar Maestría ⭐">
        ${myMastery ? '⭐' : '☆'}
      </button>
      <img class="spirit-img" src="${imgUrl}" alt="${spiritName}" loading="lazy">
      <div class="spirit-name" title="${spiritName}">${spiritName}</div>
    </div>
  `;
}

export const renderNormalGrid = renderWorkspace;
export const renderNormalStats = updateStats;
export function setNormalFilter(filter) {
  setFilter(filter.toLowerCase());
}
export function setNormalSort(sort) {
  state.currentSort = sort;
  renderWorkspace();
}

window.toggleSpiritOwned = toggleSpiritOwned;
window.toggleSpiritMastery = toggleSpiritMastery;
window.setNormalFilter = setNormalFilter;
window.setNormalSort = setNormalSort;

