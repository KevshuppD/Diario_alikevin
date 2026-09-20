// ==========================================
// FIRESTORE ENGINE & REALTIME LISTENERS
// ==========================================

import { db } from './firebase-config.js';
import { state, getFirestoreCollection, mergeSpiritTypes } from './state.js';
import { defaultSpiritsList, defaultCategories, defaultSpiritTypesT1, defaultSpiritTypesT2, defaultNames } from './constants.js';
import { sendWsMessage } from './websocket.js';

let firestoreUnsubscribe = null;
let radarUnsubscribe = null;
let radarZonesUnsubscribe = null;
let userThemeUnsubscribe = null;
let autoSaveTimer = null;
export let isAutoSaving = false;

export function updateAutoSaveIndicator(statusState, message) {
  const pills = [
    document.getElementById("edit-autosave-pill"),
    document.getElementById("categories-autosave-pill"),
    document.getElementById("config-autosave-pill")
  ];
  pills.forEach(pill => {
    if (!pill) return;
    pill.className = `autosave-status-pill ${statusState}`;
    const icon = pill.querySelector(".autosave-icon");
    const text = pill.querySelector(".autosave-text");
    if (icon) {
      icon.textContent = statusState === 'saving' ? '🔄' : statusState === 'error' ? '⚠️' : '✓';
    }
    if (text) {
      text.textContent = message || (statusState === 'saving' ? 'Guardando...' : statusState === 'error' ? 'Error al guardar' : 'Guardado automáticamente');
    }
  });
}

export function updateDbStatusBadge(isOnline, isFromCache) {
  state.latestDbStatus = { isOnline, isFromCache };
  const dot = document.getElementById("db-status-dot");
  const text = document.getElementById("db-status-text");
  if (!dot || !text) return;

  if (!isOnline) {
    dot.className = "status-dot offline";
    text.textContent = "Offline (Sin red)";
  } else {
    dot.className = "status-dot online";
  }
}

export function collectInputsFromDOM() {
  document.querySelectorAll(".category-title-edit input[data-original]").forEach(input => {
    const orig = input.dataset.original;
    const val = input.value.trim();
    if (val === "" || val === orig) {
      delete state.customCategories[orig];
    } else {
      state.customCategories[orig] = val;
    }
  });

  document.querySelectorAll(".spirit-slot input[data-id]").forEach(input => {
    const sid = input.dataset.id;
    const val = input.value.trim();
    const defName = defaultNames[parseInt(sid, 10) - 1] || "";
    if (val === "" || val === defName) {
      delete state.customNames[sid];
    } else {
      state.customNames[sid] = val;
    }
  });
}

export function triggerAutoSave(delay = 400, onSavedCallback) {
  updateAutoSaveIndicator('saving', 'Guardando cambios...');
  if (autoSaveTimer) clearTimeout(autoSaveTimer);

  autoSaveTimer = setTimeout(async () => {
    try {
      isAutoSaving = true;
      collectInputsFromDOM();
      await saveChangesToFirestoreAsync();
      
      sendWsMessage({
        type: 'DATA_SYNC',
        coupleId: state.coupleId,
        season: state.currentSeason,
        timestamp: Date.now()
      });

      try {
        localStorage.setItem(`spirits_cache_${state.coupleId}_s${state.currentSeason}`, JSON.stringify({
          spirits_list: state.spiritsList,
          categories: state.categories,
          custom_names: state.customNames,
          custom_categories: state.customCategories,
          custom_images: state.customImages,
          spirit_types: state.spiritTypes,
          kevin_list: state.kevinList,
          ali_list: state.aliList,
          kevin_mastery: state.kevinMastery,
          ali_mastery: state.aliMastery
        }));
      } catch(e) {}

      updateAutoSaveIndicator('saved', 'Guardado automáticamente');
      if (onSavedCallback) onSavedCallback();
    } catch (err) {
      console.error("Error en autoguardado:", err);
      updateAutoSaveIndicator('error', 'Error al sincronizar');
    } finally {
      isAutoSaving = false;
    }
  }, delay);
}

export async function saveChangesToFirestoreAsync() {
  const collectionName = getFirestoreCollection();
  const docRef = db.collection(collectionName).doc(state.coupleId);

  const cleanCategories = state.categories.map(c => ({
    name: c.name,
    spiritIds: c.spiritIds || []
  }));

  const cleanCustomNames = {};
  for (const [k, v] of Object.entries(state.customNames)) {
    if (v && typeof v === 'string' && v.trim() !== '') {
      cleanCustomNames[k] = v;
    }
  }

  const cleanCustomCategories = {};
  for (const [k, v] of Object.entries(state.customCategories)) {
    if (v && typeof v === 'string' && v.trim() !== '') {
      cleanCustomCategories[k] = v;
    }
  }

  const cleanCustomImages = {};
  for (const [k, v] of Object.entries(state.customImages)) {
    if (v && typeof v === 'string' && v.trim() !== '') {
      cleanCustomImages[k] = v;
    }
  }

  const dataToSave = {
    categories: cleanCategories,
    custom_names: cleanCustomNames,
    custom_categories: cleanCustomCategories,
    custom_images: cleanCustomImages,
    spirit_types: state.spiritTypes,
    spirits_list: state.spiritsList,
    kevin_list: state.kevinList,
    ali_list: state.aliList,
    kevin_mastery: state.kevinMastery,
    ali_mastery: state.aliMastery,
    last_updated: firebase.firestore.FieldValue.serverTimestamp()
  };

  await docRef.set(dataToSave, { merge: true });
}

export function listenFirestore(onDataUpdated) {
  if (firestoreUnsubscribe) firestoreUnsubscribe();
  updateDbStatusBadge(true, true);

  firestoreUnsubscribe = db.collection(getFirestoreCollection()).doc(state.coupleId)
    .onSnapshot({ includeMetadataChanges: true }, snapshot => {
      const isFromCache = snapshot.metadata.hasPendingWrites || snapshot.metadata.fromCache;
      updateDbStatusBadge(true, isFromCache);

      if (snapshot.exists) {
        const data = snapshot.data();
        state.kevinList = data.kevin_list || [];
        state.aliList = data.ali_list || [];
        state.kevinMastery = data.kevin_mastery || [];
        state.aliMastery = data.ali_mastery || [];

        const isInitialLoad = !state.hasLoadedFirestoreOnce;
        state.hasLoadedFirestoreOnce = true;

        const activeEl = document.activeElement;
        const isActivelyTyping = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');

        if ((!isActivelyTyping && !snapshot.metadata.hasPendingWrites) || isInitialLoad) {
          state.customNames = data.custom_names || {};
          state.customCategories = data.custom_categories || {};
          state.customImages = data.custom_images || {};
          state.spiritTypes = mergeSpiritTypes(data.spirit_types, state.currentSeason === 1 ? defaultSpiritTypesT1 : defaultSpiritTypesT2);

          const dbCategoriesData = data.categories || [];
          let dbCategoriesList = [];
          if (Array.isArray(dbCategoriesData)) {
            dbCategoriesList = dbCategoriesData;
          } else if (typeof dbCategoriesData === 'object' && dbCategoriesData !== null) {
            dbCategoriesList = Object.values(dbCategoriesData);
          }
          const parsedCategories = dbCategoriesList.map(cat => ({
            name: cat.name,
            spiritIds: cat.spiritIds || []
          })).filter(cat => cat && cat.name);

          if (parsedCategories.length > 0) {
            state.categories = parsedCategories;
          } else {
            state.categories = state.currentSeason === 1 ? JSON.parse(JSON.stringify(defaultCategories)) : [];
          }

          const idsInCategories = new Set();
          state.categories.forEach(cat => cat.spiritIds.forEach(id => idsInCategories.add(id)));

          if (data.spirits_list && Array.isArray(data.spirits_list)) {
            state.spiritsList = Array.from(new Set([...data.spirits_list, ...idsInCategories])).sort((a, b) => parseInt(a) - parseInt(b));
          } else {
            state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsList] : [];
          }
        }
      } else {
        state.kevinList = [];
        state.aliList = [];
        state.kevinMastery = [];
        state.aliMastery = [];
        if (state.currentMode !== "edit") {
          state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsList] : [];
          state.categories = state.currentSeason === 1 ? JSON.parse(JSON.stringify(defaultCategories)) : [];
          state.customNames = {};
          state.customCategories = {};
        }
      }

      try {
        localStorage.setItem(`spirits_cache_${state.coupleId}_s${state.currentSeason}`, JSON.stringify({
          spirits_list: state.spiritsList,
          categories: state.categories,
          custom_names: state.customNames,
          custom_categories: state.customCategories,
          custom_images: state.customImages,
          spirit_types: state.spiritTypes,
          kevin_list: state.kevinList,
          ali_list: state.aliList,
          kevin_mastery: state.kevinMastery,
          ali_mastery: state.aliMastery
        }));
      } catch(e) {}

      if (onDataUpdated) onDataUpdated();
    }, err => {
      console.error("Error al escuchar Firestore:", err);
      updateDbStatusBadge(false, false);
    });

  // Escuchar radar
  if (radarUnsubscribe) radarUnsubscribe();
  radarUnsubscribe = db.collection("locations").doc(state.coupleId).collection("users")
    .onSnapshot({ includeMetadataChanges: true }, snapshot => {
      snapshot.forEach(doc => {
        if (doc.id === "kevin" || doc.id === "ali") {
          state.radarUsersData[doc.id] = doc.data();
        }
      });
      if (onDataUpdated && state.currentMode === "radar") onDataUpdated();
    }, err => console.warn("Aviso radar users:", err));

  if (radarZonesUnsubscribe) radarZonesUnsubscribe();
  radarZonesUnsubscribe = db.collection("locations").doc(state.coupleId).collection("zones")
    .onSnapshot({ includeMetadataChanges: true }, snapshot => {
      state.radarZonesData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
      if (onDataUpdated && state.currentMode === "radar") onDataUpdated();
    }, err => console.warn("Aviso radar zones:", err));
}

export function listenUserTheme(onThemeChanged) {
  if (!state.currentUser) return;
  const username = state.currentUser.username;
  if (userThemeUnsubscribe) userThemeUnsubscribe();

  userThemeUnsubscribe = db.collection("users").doc(state.currentUser.docId)
    .onSnapshot(doc => {
      if (doc.exists) {
        const d = doc.data();
        if (d.appTheme) {
          state.userTheme = d.appTheme;
          localStorage.setItem("userTheme", d.appTheme);
        }
        if (d.barColorLight) {
          state.userLightColor = d.barColorLight;
          localStorage.setItem("userLightColor", d.barColorLight);
        }
        if (d.barColorDark) {
          state.userDarkColor = d.barColorDark;
          localStorage.setItem("userDarkColor", d.barColorDark);
        }
        if (d.useCustomBackground !== undefined) {
          state.userUseCustomBg = !!d.useCustomBackground;
          localStorage.setItem("userUseCustomBg", String(d.useCustomBackground));
        }
        if (d.refreshRate !== undefined) {
          state.userRefreshRate = d.refreshRate;
          localStorage.setItem("userRefreshRate", String(d.refreshRate));
        }
        if (onThemeChanged) onThemeChanged();
      }
    }, err => console.error("Error escuchando tema de usuario:", err));
}

export const saveChanges = triggerAutoSave;
export const initFirestore = listenFirestore;

