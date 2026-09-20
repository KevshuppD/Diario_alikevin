// ==========================================
// CATEGORIES & TYPES MANAGER VIEW (/categorias)
// ==========================================

import { state } from './state.js';
import { defaultNames } from './constants.js';
import { triggerAutoSave } from './firestore.js';
import { getSpiritCurrentType, renderWorkspace } from './normal-view.js';

let draggedCategoryIndex = null;
let draggedTypeIndex = null;

export function renderCategoriesConfigEditor() {
  const container = document.getElementById("categories-config-container") || document.getElementById("categoriesManagerContainer");
  if (!container) return;

  let categoriesHtml = "";
  state.categories.forEach((cat, index) => {
    const catDisplayName = state.customCategories[cat.name] || cat.name;
    categoriesHtml += `
      <div class="draggable-config-row" draggable="true" data-cat-drag-idx="${index}">
        <span style="color: var(--text-muted); cursor: grab; font-size: 14px; user-select: none; margin-right: 4px;">⋮⋮</span>
        <span style="font-family: monospace; color: var(--text-muted); font-size: 13px; margin-right: 4px;">#${String(index + 1).padStart(2, '0')}</span>
        <input type="text" value="${catDisplayName}" data-cat-index="${index}" onchange="window.updateConfigCategoryName(${index}, this.value)" style="flex: 1; padding: 6px; border-radius: 6px; background: #12131a; color: #fff; border: 1px solid var(--card-border); font-size: 14px; cursor: text;">
        <span class="badge" style="background: rgba(255, 255, 255, 0.1); padding: 4px 8px; border-radius: 4px; font-size: 11px; white-space: nowrap;">${cat.spiritIds ? cat.spiritIds.length : 0} esp.</span>
        <button class="remove-btn" onclick="window.deleteConfigCategory(${index})" style="background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); padding: 6px 10px; border-radius: 6px; cursor: pointer; transition: all 0.2s;">🗑</button>
      </div>
    `;
  });

  let typesHtml = "";
  state.spiritTypes.forEach((t, index) => {
    const isNormal = t.name === "Normal";
    typesHtml += `
      <div class="draggable-config-row ${isNormal ? 'unmovable' : ''}" draggable="${!isNormal}" data-type-drag-idx="${index}">
        <span style="color: var(--text-muted); cursor: ${isNormal ? 'default' : 'grab'}; font-size: 14px; user-select: none; margin-right: 4px; opacity: ${isNormal ? 0.2 : 1};">⋮⋮</span>
        <input type="text" value="${t.name}" data-type-index="${index}" data-type-field="name" onchange="window.updateConfigTypeName(${index}, this.value)" ${isNormal ? 'disabled' : ''} placeholder="Nombre (Ej: Dorado)" style="flex: 1; padding: 6px; border-radius: 6px; background: ${isNormal ? '#1b1d24' : '#12131a'}; color: ${isNormal ? '#888' : '#fff'}; border: 1px solid var(--card-border); font-size: 14px; cursor: ${isNormal ? 'default' : 'text'};">
        <input type="text" value="${t.suffix}" data-type-index="${index}" data-type-field="suffix" onchange="window.updateConfigTypeSuffix(${index}, this.value)" ${isNormal ? 'disabled' : ''} placeholder="Sufijo (Ej:  Dorado)" style="flex: 1; padding: 6px; border-radius: 6px; background: ${isNormal ? '#1b1d24' : '#12131a'}; color: ${isNormal ? '#888' : '#fff'}; border: 1px solid var(--card-border); font-size: 14px; cursor: ${isNormal ? 'default' : 'text'};">
        <button class="remove-btn" onclick="window.deleteConfigType(${index})" ${isNormal ? 'disabled style="opacity: 0.3; cursor: not-allowed;"' : 'style="background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); padding: 6px 10px; border-radius: 6px; cursor: pointer; transition: all 0.2s;"'}>🗑</button>
      </div>
    `;
  });

  container.innerHTML = `
    <div class="radar-dashboard" style="padding-top: 0;">
      
      <!-- Master Header -->
      <div class="radar-hero" style="margin-bottom: 24px;">
        <div>
          <h2 style="font-family:'Outfit',sans-serif; font-size:22px; font-weight:800; background:linear-gradient(135deg, #00e5ff, #e040fb); -webkit-background-clip:text; -webkit-text-fill-color:transparent;">
            🏷️ Gestión de Tipos y Categorías de Espíritus
          </h2>
          <p style="font-size:12px; color:var(--text-muted); margin-top:4px;">
            Organiza las categorías de espíritus, crea nuevas variantes/tipos y mantén sincronizados todos los nombres con la base de datos de Firestore.
          </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <button class="btn btn-secondary" onclick="window.normalizeAllSpiritNames()" style="padding: 8px 14px; font-size: 12px; display: inline-flex; align-items: center; gap: 6px;" title="Normaliza los nombres de todos los espíritus según su categoría y variante activa">
            <span>🪄</span> Normalizar Nombres
          </button>
          <div class="autosave-status-pill" id="categories-autosave-pill" title="Los cambios se guardan automáticamente en Firestore y se sincronizan en vivo">
            <span class="autosave-icon">✓</span>
            <span class="autosave-text">Autoguardado Activo</span>
          </div>
        </div>
      </div>

      <!-- SECTION: SPIRIT CATEGORIES & TYPES MANAGER -->
      <div class="db-inspector-card" style="margin-bottom: 24px;">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 30px;" class="config-grid-layout">
          
          <!-- Column 1: Categories -->
          <div style="display: flex; flex-direction: column;">
            <h3 style="font-family: 'Outfit', sans-serif; font-size: 16px; margin-bottom: 8px; color: var(--text-color); display: flex; align-items: center; gap: 8px;">
              <span>📁</span> Categorías de Espíritus
            </h3>
            <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px;">Arrastra para reordenar, edita los nombres o agrega nuevas categorías.</p>
            
            <div style="max-height: 440px; overflow-y: auto; padding-right: 8px; margin-bottom: 14px;" id="config-categories-list-box">
              ${categoriesHtml}
            </div>

            <!-- Add Category Form -->
            <div style="display: flex; gap: 10px; margin-top: auto; padding: 12px; background: rgba(255,255,255,0.02); border: 1px dashed rgba(255,255,255,0.1); border-radius: 8px;">
              <input type="text" id="config-add-category-input" placeholder="Nueva Categoría (Ej: Espíritu de Viento)" style="flex: 1; padding: 8px; border-radius: 6px; background: #12131a; color: #fff; border: 1px solid var(--card-border); font-size: 13px;">
              <button class="btn" onclick="window.addConfigCategory()" style="padding: 8px 16px; font-size: 12px;">Agregar</button>
            </div>
          </div>

          <!-- Column 2: Types -->
          <div style="display: flex; flex-direction: column;">
            <h3 style="font-family: 'Outfit', sans-serif; font-size: 16px; margin-bottom: 8px; color: var(--text-color); display: flex; align-items: center; gap: 8px;">
              <span>✨</span> Tipos / Variantes de Espíritus
            </h3>
            <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px;">Configura variantes (ej. Dorado, Gomita). El sufijo se autocompleta.</p>
            
            <div style="max-height: 440px; overflow-y: auto; padding-right: 8px; margin-bottom: 14px;" id="config-types-list-box">
              <div style="display: flex; gap: 10px; padding: 4px 8px; margin-bottom: 8px; font-size: 11px; color: var(--text-muted); font-weight: 600;">
                <div style="flex: 1;">Nombre del Tipo</div>
                <div style="flex: 1;">Sufijo (Ej: " Dorado")</div>
                <div style="width: 32px;"></div>
              </div>
              ${typesHtml}
            </div>

            <!-- Add Type Form -->
            <div style="display: flex; flex-direction: column; gap: 8px; margin-top: auto; padding: 12px; background: rgba(255,255,255,0.02); border: 1px dashed rgba(255,255,255,0.1); border-radius: 8px;">
              <div style="display: flex; gap: 10px;">
                <input type="text" id="config-add-type-name" placeholder="Nombre (Ej: Quack)" style="flex: 1; padding: 8px; border-radius: 6px; background: #12131a; color: #fff; border: 1px solid var(--card-border); font-size: 13px;">
                <input type="text" id="config-add-type-suffix" placeholder="Sufijo (Ej:  Quack)" style="flex: 1; padding: 8px; border-radius: 6px; background: #12131a; color: #fff; border: 1px solid var(--card-border); font-size: 13px;">
              </div>
              <button class="btn" onclick="window.addConfigType()" style="width: 100%; padding: 8px; font-size: 12px;">Agregar Nuevo Tipo</button>
            </div>
          </div>

        </div>
      </div>

    </div>
  `;

  attachDragListeners();
}

function attachDragListeners() {
  document.querySelectorAll("[data-cat-drag-idx]").forEach(row => {
    const idx = parseInt(row.dataset.catDragIdx, 10);
    row.ondragstart = (e) => {
      draggedCategoryIndex = idx;
      e.dataTransfer.effectAllowed = "move";
      row.style.opacity = "0.5";
    };
    row.ondragover = (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = "move";
    };
    row.ondrop = (e) => {
      e.preventDefault();
      if (draggedCategoryIndex === null || draggedCategoryIndex === idx) return;
      const item = state.categories.splice(draggedCategoryIndex, 1)[0];
      state.categories.splice(idx, 0, item);
      draggedCategoryIndex = null;
      renderCategoriesConfigEditor();
      triggerAutoSave(50);
    };
    row.ondragend = () => { row.style.opacity = "1"; };
  });

  document.querySelectorAll("[data-type-drag-idx]").forEach(row => {
    const idx = parseInt(row.dataset.typeDragIdx, 10);
    if (idx === 0) return;
    row.ondragstart = (e) => {
      draggedTypeIndex = idx;
      e.dataTransfer.effectAllowed = "move";
      row.style.opacity = "0.5";
    };
    row.ondragover = (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = "move";
    };
    row.ondrop = (e) => {
      e.preventDefault();
      if (idx === 0 || draggedTypeIndex === null || draggedTypeIndex === idx) return;
      const item = state.spiritTypes.splice(draggedTypeIndex, 1)[0];
      state.spiritTypes.splice(idx, 0, item);
      draggedTypeIndex = null;
      renderCategoriesConfigEditor();
      triggerAutoSave(50);
    };
    row.ondragend = () => { row.style.opacity = "1"; };
  });
}

export function computeSpiritName(id, targetCategoryName, newTypeName) {
  let baseCatName = targetCategoryName && targetCategoryName !== "__uncategorized__" ? (state.customCategories[targetCategoryName] || targetCategoryName) : "";
  if (!baseCatName) {
    const foundCat = state.categories.find(c => c.spiritIds && c.spiritIds.includes(id));
    if (foundCat) {
      baseCatName = state.customCategories[foundCat.name] || foundCat.name;
    }
  }
  if (!baseCatName) {
    const defIdx = parseInt(id, 10) - 1;
    baseCatName = defaultNames[defIdx] || `Espíritu #${id}`;
  }

  const typeObj = state.spiritTypes.find(t => t.name === newTypeName);
  const suffix = typeObj ? typeObj.suffix : "";

  if (id === "13" && newTypeName === "Normal" && baseCatName.includes("Cacahuete")) {
    return "TheBurntPeanut (Espíritu del Cacahuete)";
  }

  return baseCatName + suffix;
}

export function updateConfigCategoryName(index, newValue) {
  if (!state.categories[index]) return;
  const cleanVal = newValue.trim();
  const cat = state.categories[index];
  const originalName = cat.name;
  if (!cleanVal || cleanVal === originalName) {
    delete state.customCategories[originalName];
  } else {
    state.customCategories[originalName] = cleanVal;
  }

  (cat.spiritIds || []).forEach(sid => {
    const currentType = getSpiritCurrentType(sid);
    const newFullName = computeSpiritName(sid, cat.name, currentType);
    const defName = defaultNames[parseInt(sid, 10) - 1] || "";
    if (newFullName === defName) {
      delete state.customNames[sid];
    } else {
      state.customNames[sid] = newFullName;
    }
  });

  triggerAutoSave(400);
}

export function deleteConfigCategory(index) {
  const cat = state.categories[index];
  if (!cat) return;
  const displayName = state.customCategories[cat.name] || cat.name;
  const msg = cat.spiritIds && cat.spiritIds.length > 0
    ? `La categoría "${displayName}" tiene ${cat.spiritIds.length} espíritus. Si la eliminas, quedarán sueltos. ¿Continuar?`
    : `¿Eliminar la categoría vacía "${displayName}"?`;

  if (window.customConfirm) {
    window.customConfirm(msg, "🗑️", () => {
      delete state.customCategories[cat.name];
      state.categories.splice(index, 1);
      renderCategoriesConfigEditor();
      triggerAutoSave(50);
      if (window.showToast) window.showToast(`Categoría "${displayName}" eliminada.`, false);
    });
  }
}

export function addConfigCategory() {
  const input = document.getElementById("config-add-category-input");
  if (!input) return;
  const name = input.value.trim();
  if (!name) {
    if (window.customAlert) window.customAlert("Por favor ingresa un nombre válido para la categoría.", "⚠️");
    return;
  }

  if (state.categories.some(c => c.name === name)) {
    if (window.customAlert) window.customAlert("Ya existe una categoría con ese nombre.", "⚠️");
    return;
  }

  state.categories.push({ name: name, spiritIds: [] });
  input.value = "";
  renderCategoriesConfigEditor();
  triggerAutoSave(50);
  if (window.showToast) window.showToast(`Categoría "${name}" agregada con éxito.`, false);
}

export function updateConfigTypeName(index, newValue) {
  const cleanVal = newValue.trim();
  if (!cleanVal) return;
  const oldType = state.spiritTypes[index];
  const oldName = oldType.name;
  const oldSuffix = oldType.suffix;
  
  state.spiritTypes[index].name = cleanVal;
  
  if (!oldSuffix || oldSuffix.trim() === oldName) {
    state.spiritTypes[index].suffix = ` ${cleanVal}`;
  }
  
  const newSuffix = state.spiritTypes[index].suffix;
  
  if (oldName !== "Normal" && oldName !== cleanVal) {
    Object.keys(state.customNames).forEach(id => {
      let currentName = state.customNames[id];
      if (oldSuffix && oldSuffix.trim() && currentName.endsWith(oldSuffix)) {
        state.customNames[id] = currentName.slice(0, -oldSuffix.length) + newSuffix;
      } else if (currentName.endsWith(oldName)) {
        state.customNames[id] = currentName.slice(0, -oldName.length) + newSuffix;
      }
    });
  }
  
  renderCategoriesConfigEditor();
  triggerAutoSave(400);
}

export function updateConfigTypeSuffix(index, newValue) {
  if (newValue && !newValue.startsWith(" ")) {
    newValue = ` ${newValue.trim()}`;
  }
  state.spiritTypes[index].suffix = newValue;
  triggerAutoSave(400);
}

export function deleteConfigType(index) {
  const t = state.spiritTypes[index];
  if (window.customConfirm) {
    window.customConfirm(`¿Eliminar el tipo "${t.name}"?`, "🗑️", () => {
      state.spiritTypes.splice(index, 1);
      renderCategoriesConfigEditor();
      triggerAutoSave(50);
      if (window.showToast) window.showToast(`Tipo "${t.name}" eliminado.`, false);
    });
  }
}

export function addConfigType() {
  const nameInput = document.getElementById("config-add-type-name");
  const suffixInput = document.getElementById("config-add-type-suffix");
  if (!nameInput || !suffixInput) return;

  const name = nameInput.value.trim();
  const suffix = suffixInput.value;

  if (!name) {
    if (window.customAlert) window.customAlert("Por favor ingresa un nombre para el tipo.", "⚠️");
    return;
  }

  if (state.spiritTypes.some(t => t.name === name)) {
    if (window.customAlert) window.customAlert("Ya existe un tipo con ese nombre.", "⚠️");
    return;
  }

  state.spiritTypes.push({ name: name, suffix: suffix });
  nameInput.value = "";
  suffixInput.value = "";
  renderCategoriesConfigEditor();
  triggerAutoSave(50);
  if (window.showToast) window.showToast(`Tipo "${name}" agregado con éxito.`, false);
}

export function normalizeAllSpiritNames() {
  if (window.customConfirm) {
    window.customConfirm("¿Deseas normalizar automáticamente los nombres de todos los espíritus según su categoría asignada y su tipo/variante activa?", "🪄", () => {
      let updatedCount = 0;

      state.categories.forEach(cat => {
        const catBaseTitle = state.customCategories[cat.name] || cat.name;
        
        (cat.spiritIds || []).forEach(sid => {
          const currentType = getSpiritCurrentType(sid);
          const typeObj = state.spiritTypes.find(t => t.name === currentType);
          const suffix = typeObj ? typeObj.suffix : "";
          
          let targetFullName = "";
          if (sid === "13" && currentType === "Normal" && catBaseTitle.includes("Cacahuete")) {
            targetFullName = "TheBurntPeanut (Espíritu del Cacahuete)";
          } else {
            targetFullName = catBaseTitle + suffix;
          }

          const defName = defaultNames[parseInt(sid, 10) - 1] || "";
          if (targetFullName === defName) {
            if (state.customNames[sid]) {
              delete state.customNames[sid];
              updatedCount++;
            }
          } else {
            if (state.customNames[sid] !== targetFullName) {
              state.customNames[sid] = targetFullName;
              updatedCount++;
            }
          }
        });
      });

      const assignedIds = new Set();
      state.categories.forEach(c => (c.spiritIds || []).forEach(id => assignedIds.add(id)));
      
      state.spiritsList.forEach(sid => {
        if (!assignedIds.has(sid)) {
          const currentName = state.customNames[sid];
          if (currentName) {
            const defName = defaultNames[parseInt(sid, 10) - 1] || "";
            if (currentName.trim() === "" || currentName === defName) {
              delete state.customNames[sid];
              updatedCount++;
            }
          }
        }
      });

      renderWorkspace();
      renderCategoriesConfigEditor();
      triggerAutoSave(50);
      if (window.showToast) window.showToast(`✨ Se normalizaron los nombres (${updatedCount} actualizados)`, false);
    });
  }
}

export const renderCategoriesManager = renderCategoriesConfigEditor;

