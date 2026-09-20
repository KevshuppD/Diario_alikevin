/**
 * router.js - Enrutador SPA de Vistas, Modos y Gestión de Temporadas
 */

import { state } from './state.js';
import { renderWorkspace, updateStats } from './normal-view.js';
import { renderGallery, isGalleryOpen } from './edit-view.js';
import { renderCategoriesManager } from './categories-view.js';
import { renderConfigView } from './config-view.js';
import { renderRadarManager } from './radar-view.js';
import { listenFirestore } from './firestore.js';

export function getRouteFromPath(pathname) {
  const cleanPath = pathname.toLowerCase().replace(/^\/web\/?/, '/').replace(/\/$/, '') || '/';
  if (cleanPath === '/' || cleanPath === '/normal') return 'normal';
  if (cleanPath === '/edit') return 'edit';
  if (cleanPath === '/categorias' || cleanPath === '/categories') return 'categories';
  if (cleanPath === '/configuracion' || cleanPath === '/config') return 'config';
  if (cleanPath === '/radar') return 'radar';
  return 'normal';
}

export function getPathFromRoute(route) {
  switch (route) {
    case 'normal': return '/normal';
    case 'edit': return '/edit';
    case 'categories': return '/categorias';
    case 'config': return '/configuracion';
    case 'radar': return '/radar';
    default: return '/';
  }
}

export function setMode(mode) {
  if (mode === "db") mode = "normal";
  state.currentMode = mode;
  try { localStorage.setItem("spirit_mode", mode); } catch(e) {}

  // Actualizar botones de modo en la barra superior
  const btnNormal = document.getElementById("btn-mode-normal");
  const btnEdit = document.getElementById("btn-mode-edit");
  const btnCategories = document.getElementById("btn-mode-categories");
  const btnRadar = document.getElementById("btn-mode-radar");
  const btnConfig = document.getElementById("btn-mode-config");

  if (btnNormal) btnNormal.classList.toggle("active", mode === "normal");
  if (btnEdit) btnEdit.classList.toggle("active", mode === "edit");
  if (btnCategories) btnCategories.classList.toggle("active", mode === "categories");
  if (btnRadar) btnRadar.classList.toggle("active", mode === "radar");
  if (btnConfig) btnConfig.classList.toggle("active", mode === "config");

  // Mostrar / Ocultar componentes según el modo activo
  const searchBar = document.getElementById("shared-search-bar-container");
  const normalToolbar = document.getElementById("normal-toolbar");
  const editActionsBar = document.getElementById("edit-actions-bar");
  const categoriesContainer = document.getElementById("categories-container");
  const categoriesManagerContainer = document.getElementById("categoriesManagerContainer");
  const radarContainer = document.getElementById("radar-container");
  const configContainer = document.getElementById("config-container");
  const sidebar = document.getElementById("sidebar");

  if (searchBar) searchBar.style.display = (mode === "normal" || mode === "edit") ? "block" : "none";
  if (normalToolbar) normalToolbar.style.display = mode === "normal" ? "flex" : "none";
  if (editActionsBar) editActionsBar.style.display = mode === "edit" ? "flex" : "none";
  if (categoriesContainer) categoriesContainer.style.display = (mode === "normal" || mode === "edit") ? "flex" : "none";
  if (categoriesManagerContainer) categoriesManagerContainer.style.display = mode === "categories" ? "block" : "none";
  if (radarContainer) radarContainer.style.display = mode === "radar" ? "block" : "none";
  if (configContainer) configContainer.style.display = mode === "config" ? "block" : "none";

  if (sidebar) {
    sidebar.classList.toggle("hidden", !isGalleryOpen || mode !== "edit");
  }

  // Título del documento
  document.title = 'Gestor de diario de ali y kevin';

  // Ejecutar renderizadores correspondientes
  if (mode === "categories") {
    renderCategoriesManager();
  } else if (mode === "radar") {
    renderRadarManager();
  } else if (mode === "config") {
    renderConfigView();
  } else {
    updateStats();
    renderWorkspace();
    if (mode === "edit" && isGalleryOpen) {
      renderGallery();
    }
  }
}

export function switchModeSPA(mode, updateUrl = true) {
  if (mode === "db") mode = "normal";
  if (state.currentMode === mode && !updateUrl) return;

  setMode(mode);

  if (updateUrl) {
    const targetUrl = getPathFromRoute(mode);
    if (window.location.pathname !== targetUrl) {
      window.history.pushState({ mode }, '', targetUrl);
    }
  }
}

export function switchSeason(season) {
  const sNum = parseInt(season, 10);
  if (sNum !== 1 && sNum !== 2) return;
  if (state.currentSeason === sNum) return;

  state.currentSeason = sNum;
  try { localStorage.setItem("current_season", String(sNum)); } catch(e) {}

  const btnS1 = document.getElementById("btn-season-1");
  const btnS2 = document.getElementById("btn-season-2");
  if (btnS1) btnS1.classList.toggle("active", sNum === 1);
  if (btnS2) btnS2.classList.toggle("active", sNum === 2);

  // Re-escuchar Firestore para la nueva temporada
  listenFirestore();
}

export function initRouter() {
  window.addEventListener('popstate', (e) => {
    const route = e.state?.mode || getRouteFromPath(window.location.pathname);
    switchModeSPA(route, false);
  });

  // Enrutamiento inicial al cargar la página
  const initialRoute = getRouteFromPath(window.location.pathname);
  switchModeSPA(initialRoute, false);
}

// Window bindings
window.switchModeSPA = switchModeSPA;
window.setMode = setMode;
window.switchSeason = switchSeason;
window.initRouter = initRouter;
