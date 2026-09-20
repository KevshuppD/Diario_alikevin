/**
 * router.js - Enrutador SPA de Vistas y Gestión de Temporadas
 */

import { state } from './state.js';
import { renderNormalGrid, renderNormalStats } from './normal-view.js';
import { renderEditGrid, renderGallery } from './edit-view.js';
import { renderCategoriesManager } from './categories-view.js';
import { renderConfigView } from './config-view.js';
import { initRadarView } from './radar-view.js';

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

export function switchModeSPA(mode, updateUrl = true) {
  state.currentView = mode;

  // Actualizar estado activo en la barra de navegación superior
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.classList.toggle('active', tab.dataset.mode === mode);
  });

  // Ocultar todas las secciones de vista
  const views = ['normalView', 'editView', 'categoriesView', 'configView', 'radarView'];
  views.forEach(vId => {
    const el = document.getElementById(vId);
    if (el) el.style.display = 'none';
  });

  // Mostrar la vista activa y ejecutar su renderizador correspondiente
  const targetId = `${mode}View`;
  const targetEl = document.getElementById(targetId);
  if (targetEl) targetEl.style.display = 'block';

  // Título de la pestaña del navegador
  document.title = 'Gestor de diario de ali y kevin';

  if (mode === 'normal') {
    renderNormalGrid();
    renderNormalStats();
  } else if (mode === 'edit') {
    renderEditGrid();
    renderGallery();
  } else if (mode === 'categories') {
    renderCategoriesManager();
  } else if (mode === 'config') {
    renderConfigView();
  } else if (mode === 'radar') {
    initRadarView();
  }

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

  state.activeSeason = sNum;

  document.querySelectorAll('.season-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.season, 10) === sNum);
  });

  if (state.currentView === 'normal') {
    renderNormalGrid();
    renderNormalStats();
  } else if (state.currentView === 'edit') {
    renderEditGrid();
    renderGallery();
  } else if (state.currentView === 'categories') {
    renderCategoriesManager();
  }
}

export function initRouter() {
  window.addEventListener('popstate', (e) => {
    const route = e.state?.mode || getRouteFromPath(window.location.pathname);
    switchModeSPA(route, false);
  });

  // Enrutamiento inicial
  const initialRoute = getRouteFromPath(window.location.pathname);
  switchModeSPA(initialRoute, false);
}

// Window bindings
window.switchModeSPA = switchModeSPA;
window.setMode = (mode) => switchModeSPA(mode, true);
window.switchSeason = switchSeason;
