// ==========================================
// FIRESTORE ENGINE & REALTIME LISTENERS
// ==========================================

import { db, rtdb } from './firebase-config.js';
import { state, getFirestoreCollection, mergeSpiritTypes } from './state.js';
import { 
  defaultSpiritsList, 
  defaultCategories, 
  defaultSpiritsListT1, 
  defaultSpiritsListT2, 
  defaultCategoriesT1, 
  defaultCategoriesT2, 
  defaultSpiritTypesT1, 
  defaultSpiritTypesT2, 
  defaultNames 
} from './constants.js';
import { sendWsMessage } from './websocket.js';
import { getDefaultSpiritName, clearSpiritCache } from './normal-view.js';

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
  // Inputs de título de categoría en Modo Edición (normal-view.js)
  document.querySelectorAll(".category-title-edit input[data-original]").forEach(input => {
    const orig = input.dataset.original;
    const val = input.value.trim();
    if (val === "" || val === orig) {
      delete state.customCategories[orig];
    } else {
      state.customCategories[orig] = val;
    }
  });

  // Inputs de categoría en Tipos y Categorías (categories-view.js)
  document.querySelectorAll("input[data-cat-index]").forEach(input => {
    const idx = parseInt(input.dataset.catIndex, 10);
    const cat = state.categories[idx];
    if (cat) {
      const orig = cat.name;
      const val = input.value.trim();
      if (val === "" || val === orig) {
        delete state.customCategories[orig];
      } else {
        state.customCategories[orig] = val;
      }
    }
  });

  // Inputs de tipos de espíritus en Tipos y Categorías (categories-view.js)
  document.querySelectorAll("input[data-type-index]").forEach(input => {
    const idx = parseInt(input.dataset.typeIndex, 10);
    const field = input.dataset.typeField;
    if (state.spiritTypes[idx] && field) {
      const val = input.value;
      if (field === "name" && val.trim() !== "") {
        state.spiritTypes[idx].name = val.trim();
      } else if (field === "suffix") {
        state.spiritTypes[idx].suffix = val;
      }
    }
  });

  // Inputs de nombres individuales de espíritus
  document.querySelectorAll(".spirit-slot input[data-id]").forEach(input => {
    const sid = input.dataset.id;
    const val = input.value.trim();
    const defName = getDefaultSpiritName(sid, state.currentSeason);
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
    autoSaveTimer = null;
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

let lastListenedSeason = null;

export function listenFirestore(onDataUpdated) {
  if (firestoreUnsubscribe) firestoreUnsubscribe();
  updateDbStatusBadge(true, true);

  const currentSeasonForListener = state.currentSeason;
  const isSeasonChange = lastListenedSeason !== currentSeasonForListener;
  lastListenedSeason = currentSeasonForListener;

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

        const isInitialLoad = !state.hasLoadedFirestoreOnce || isSeasonChange;
        state.hasLoadedFirestoreOnce = true;

        const activeEl = document.activeElement;
        const isActivelyTyping = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');
        const isAutoSavePending = !isSeasonChange && (autoSaveTimer !== null || isAutoSaving);

        if ((!isActivelyTyping && !isAutoSavePending && !snapshot.metadata.hasPendingWrites) || isInitialLoad) {
          clearSpiritCache();
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
            state.categories = state.currentSeason === 1 
              ? JSON.parse(JSON.stringify(defaultCategoriesT1)) 
              : JSON.parse(JSON.stringify(defaultCategoriesT2));
          }

          const idsInCategories = new Set();
          state.categories.forEach(cat => cat.spiritIds.forEach(id => idsInCategories.add(id)));

          if (data.spirits_list && Array.isArray(data.spirits_list) && data.spirits_list.length > 0) {
            state.spiritsList = Array.from(new Set([...data.spirits_list, ...idsInCategories])).sort((a, b) => parseInt(a) - parseInt(b));
          } else {
            state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsListT1] : [...defaultSpiritsListT2];
          }
        }
      } else {
        state.kevinList = [];
        state.aliList = [];
        state.kevinMastery = [];
        state.aliMastery = [];
        if (state.currentMode !== "edit" || isSeasonChange) {
          state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsListT1] : [...defaultSpiritsListT2];
          state.categories = state.currentSeason === 1 
            ? JSON.parse(JSON.stringify(defaultCategoriesT1)) 
            : JSON.parse(JSON.stringify(defaultCategoriesT2));
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

      // Notificar reactivamente a la interfaz de usuario en cualquier cambio
      if (typeof onDataUpdated === 'function') {
        onDataUpdated();
      } else if (state.currentMode === "normal" && typeof window.renderWorkspace === 'function') {
        window.renderWorkspace();
      } else if (state.currentMode === "normal" && typeof window.syncAllSpiritSlotsDOM === 'function') {
        window.syncAllSpiritSlotsDOM();
      }
    }, err => {
      console.error("Error al escuchar Firestore:", err);
      updateDbStatusBadge(false, false);
    });

  // Escuchar radar en Realtime Database
  if (radarUnsubscribe) {
    if (typeof radarUnsubscribe === 'function') radarUnsubscribe();
    else if (radarUnsubscribe.off) radarUnsubscribe.off();
  }
  const usersRef = rtdb.ref(`locations/${state.coupleId}/users`);
  usersRef.on("value", snapshot => {
    const val = snapshot.val() || {};
    ['kevin', 'ali'].forEach(uKey => {
      if (val[uKey]) {
        state.radarUsersData[uKey] = val[uKey];
      }
    });
    if (onDataUpdated && state.currentMode === "radar") onDataUpdated();
  }, err => console.warn("Aviso RTDB users en firestore.js:", err));
  radarUnsubscribe = usersRef;

  if (radarZonesUnsubscribe) radarZonesUnsubscribe();
  radarZonesUnsubscribe = db.collection("locations").doc(state.coupleId).collection("zones")
    .onSnapshot({ includeMetadataChanges: true }, snapshot => {
      state.radarZonesData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
      if (onDataUpdated && state.currentMode === "radar") onDataUpdated();
    }, err => console.warn("Aviso radar zones:", err));

  // Escuchar configuración del usuario activo en Firestore
  listenUserTheme(onDataUpdated);
}

export function listenUserTheme(onThemeChanged) {
  if (!state.currentUser) return;
  if (userThemeUnsubscribe) userThemeUnsubscribe();

  userThemeUnsubscribe = db.collection("users").doc(state.currentUser.docId)
    .onSnapshot(doc => {
      if (doc.exists) {
        const d = doc.data();
        const theme = d.theme || d.appTheme;
        if (theme) {
          state.userTheme = theme;
          localStorage.setItem("userTheme", theme);
        }
        const lightColor = d.lightColor || d.barColorLight;
        if (lightColor) {
          state.userLightColor = lightColor;
          localStorage.setItem("userLightColor", lightColor);
        }
        const darkColor = d.darkColor || d.barColorDark;
        if (darkColor) {
          state.userDarkColor = darkColor;
          localStorage.setItem("userDarkColor", darkColor);
        }
        const customBg = d.useCustomBg !== undefined ? d.useCustomBg : d.useCustomBackground;
        if (customBg !== undefined) {
          state.userUseCustomBg = !!customBg;
          localStorage.setItem("userUseCustomBg", String(state.userUseCustomBg));
        }
        const refreshRate = d.refreshRate;
        if (refreshRate !== undefined) {
          state.userRefreshRate = refreshRate;
          localStorage.setItem("userRefreshRate", String(refreshRate));
        }
        const font = d.fontPreference;
        if (font) {
          state.userFont = font;
          localStorage.setItem("userFont", font);
          if (typeof window.applyUserFont === 'function') {
            window.applyUserFont(font);
          }
        }

        const mapTheme = d.googleMapTheme || d.radarMapTheme || d.mapTileTheme;
        if (mapTheme) {
          state.googleMapTheme = mapTheme;
          localStorage.setItem("google_map_theme_" + state.currentUser.username, mapTheme);
          if (typeof window.changeRadarTileLayer === 'function') {
            window.changeRadarTileLayer(mapTheme, false);
          }
        }

        if (typeof window.applyTheme === 'function') {
          window.applyTheme(state.userTheme, state.userLightColor, state.userDarkColor, state.userUseCustomBg);
        }

        if (typeof window.renderConfigEditor === 'function' && state.currentMode === 'config') {
          window.renderConfigEditor();
        }

        if (onThemeChanged) onThemeChanged();
      }
    }, err => console.error("Error escuchando tema de usuario:", err));
}

export const saveChanges = triggerAutoSave;
export const initFirestore = listenFirestore;
window.listenUserTheme = listenUserTheme;

