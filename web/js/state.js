// ==========================================
// CENTRAL APPLICATION STATE - DIARIO ALI Y KEVIN
// ==========================================

import { 
  defaultSpiritTypesT1, 
  defaultSpiritTypesT2, 
  defaultSpiritsListT1, 
  defaultSpiritsListT2, 
  defaultCategoriesT1, 
  defaultCategoriesT2 
} from './constants.js';

function normalizeCoupleId(id) {
  const clean = (id || "").trim();
  if (!clean || clean === "vinculo_unico_123" || clean === "vínculo_único_123") {
    return "vínculo_único_123";
  }
  return clean;
}

export const state = {
  coupleId: normalizeCoupleId(localStorage.getItem("coupleId")),
  currentSeason: parseInt(localStorage.getItem("current_season") || "2", 10),
  currentMode: "normal",
  currentFilter: "todos",
  searchQuery: "",
  currentUser: null,
  
  // Spirit lists & mappings
  spiritsList: [],
  categories: [],
  customNames: {},
  customCategories: {},
  customImages: {},
  spiritTypes: [],
  
  // User checks & mastery
  kevinList: [],
  aliList: [],
  kevinMastery: [],
  aliMastery: [],
  
  // Image cache busters
  imageCacheBusters: {},
  editingSpiritImageId: null,
  activeModalSpiritId: null,
  isGalleryOpen: false,
  
  // Radar data
  radarUsersData: { kevin: null, ali: null },
  radarZonesData: [],
  radarPingLoading: { kevin: false, ali: false },
  radarLiveInterval: null,
  
  // User styling preferences
  userTheme: localStorage.getItem("userTheme") || "Pixel Oscuro",
  userLightColor: localStorage.getItem("userLightColor") || "#D1C4E9",
  userDarkColor: localStorage.getItem("userDarkColor") || "#4A148C",
  userUseCustomBg: localStorage.getItem("userUseCustomBg") === "true",
  userRefreshRate: parseInt(localStorage.getItem("userRefreshRate")) || 90,
  
  // Status tracking
  latestDbStatus: { isOnline: true, isFromCache: true },
  latestWsStatus: { connected: false, text: "WS: Conectando...", count: 1, tooltip: "" },
  hasLoadedFirestoreOnce: false
};

if (state.currentSeason !== 1 && state.currentSeason !== 2) state.currentSeason = 2;

export function getFirestoreCollection() {
  return state.currentSeason === 1 ? "fortnite_spirits" : "fortnite_spirits_s2";
}

export function mergeSpiritTypes(existingTypes, defaultTypes) {
  if (!existingTypes || !Array.isArray(existingTypes) || existingTypes.length === 0) {
    return [...defaultTypes];
  }
  const result = [...existingTypes];
  defaultTypes.forEach(dt => {
    if (!result.some(t => t.name.toLowerCase() === dt.name.toLowerCase())) {
      result.push(dt);
    }
  });
  return result;
}

// Load cached data on boot
try {
  const cachedData = JSON.parse(localStorage.getItem(`spirits_cache_${state.coupleId}_s${state.currentSeason}`));
  if (cachedData && cachedData.spirits_list && cachedData.spirits_list.length > 0) {
    state.spiritsList = cachedData.spirits_list;
    state.categories = (cachedData.categories && cachedData.categories.length > 0) 
      ? cachedData.categories 
      : (state.currentSeason === 1 ? JSON.parse(JSON.stringify(defaultCategoriesT1)) : JSON.parse(JSON.stringify(defaultCategoriesT2)));
    state.customNames = cachedData.custom_names || {};
    state.customCategories = cachedData.custom_categories || {};
    state.customImages = cachedData.custom_images || {};
    state.spiritTypes = mergeSpiritTypes(cachedData.spirit_types, state.currentSeason === 1 ? defaultSpiritTypesT1 : defaultSpiritTypesT2);
    state.kevinList = cachedData.kevin_list || [];
    state.aliList = cachedData.ali_list || [];
    state.kevinMastery = cachedData.kevin_mastery || [];
    state.aliMastery = cachedData.ali_mastery || [];
  } else {
    state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsListT1] : [...defaultSpiritsListT2];
    state.categories = state.currentSeason === 1 ? JSON.parse(JSON.stringify(defaultCategoriesT1)) : JSON.parse(JSON.stringify(defaultCategoriesT2));
    state.spiritTypes = state.currentSeason === 1 ? [...defaultSpiritTypesT1] : [...defaultSpiritTypesT2];
  }
} catch(e) {
  state.spiritsList = state.currentSeason === 1 ? [...defaultSpiritsListT1] : [...defaultSpiritsListT2];
  state.categories = state.currentSeason === 1 ? JSON.parse(JSON.stringify(defaultCategoriesT1)) : JSON.parse(JSON.stringify(defaultCategoriesT2));
  state.spiritTypes = state.currentSeason === 1 ? [...defaultSpiritTypesT1] : [...defaultSpiritTypesT2];
}

export function getStoredTheme() {
  const theme = localStorage.getItem("userTheme") || "Pixel Oscuro";
  if (theme === "Pixel Claro") return "pixel-claro";
  if (theme === "Pixel Monocromático") return "pixel-mono";
  return "pixel-oscuro";
}

