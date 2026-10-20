/**
 * letters-view.js - Cartas compartidas con la app (colección messages)
 *
 * Mismo esquema que Message.kt. Lecturas acotadas para la cuota Spark:
 * - Listener en tiempo real solo para la página más reciente (PAGE_SIZE cartas).
 * - Las cartas anteriores se piden bajo demanda con getDocs + startAfter.
 */

import { state, USERS } from './state.js';
import {
  db, doc, collection, setDoc, onSnapshot, query, where, orderBy, limit, startAfter, getDocs, getDoc, updateDoc, deleteDoc
} from './firebase-config.js';
import { LETTER_SIZES, LETTER_COLORS, letterToSafeHtml, letterToEditorHtml, editorToLetterHtml, letterToPlainText, getLetterInlineImageUrls, getImageWidth } from './rich-text.js';

const PAGE_SIZE = 5;
const MAX_IMAGE_SIDE = 1600;

let liveUnsubscribe = null;
let liveCoupleId = null;
let liveLetters = [];          // Página más reciente (tiempo real)
let olderLetters = [];         // Páginas anteriores (cargadas bajo demanda)
let oldestLiveSnapshot = null; // Cursor para pedir la siguiente página
let olderCursor = null;
let hasMoreOlder = true;
let isLoadingOlder = false;
let editingLetterId = null;
let isSending = false;

function escapeHtml(text) {
  return String(text ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function getLetterImages(letter) {
  if (Array.isArray(letter.imageUrls) && letter.imageUrls.length > 0) return letter.imageUrls;
  return letter.imageUrl ? [letter.imageUrl] : [];
}

function letterAuthorIsMe(existing) {
  return !existing || existing.authorId === state.currentUser?.docId;
}

function isOwnLetter(letter) {
  return !!state.currentUser && letter.authorId === state.currentUser.docId;
}

function authorColorFor(letter) {
  const user = Object.values(USERS).find(u => u.docId === letter.authorId);
  return user ? user.color : "var(--text-muted)";
}

// Igual que sendNotificationV1 en MainActivity.kt
export function getPushTopic() {
  return "diario_" + state.coupleId.toLowerCase()
    .replace(/á/g, "a").replace(/é/g, "e").replace(/í/g, "i").replace(/ó/g, "o").replace(/ú/g, "u")
    .replace(/ñ/g, "n").replace(/ /g, "_");
}

// Push solo de datos, como la app: MyFirebaseMessagingService descarta las que llevan el authorId propio
async function sendLetterPush(title, body, imageUrl, clickType) {
  if (!state.currentUser) return;
  try {
    const res = await fetch("/api/send-push", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        topic: getPushTopic(),
        data: {
          title,
          body,
          authorId: state.currentUser.docId,
          authorName: state.currentUser.name,
          click_type: clickType,
          ...(imageUrl ? { imageUrl } : {})
        }
      })
    });
    if (!res.ok) console.warn("No se pudo enviar la notificación:", await res.text());
  } catch (err) {
    console.warn("No se pudo enviar la notificación:", err);
  }
}

function formatLetterDate(ts) {
  if (!ts) return "";
  const d = new Date(ts);
  return d.toLocaleDateString("es-CL", { day: "2-digit", month: "short", year: "numeric" }) +
    " · " + d.toLocaleTimeString("es-CL", { hour: "2-digit", minute: "2-digit" });
}

function getAllLetters() {
  const seen = new Set();
  return [...liveLetters, ...olderLetters].filter(l => {
    if (seen.has(l.messageId)) return false;
    seen.add(l.messageId);
    return true;
  });
}

function findLetter(id) {
  return getAllLetters().find(l => l.messageId === id);
}

// ---------- Suscripción ----------

function lettersQuery(...extra) {
  return query(
    collection(db, "messages"),
    where("partnerId", "==", state.coupleId),
    orderBy("timestamp", "desc"),
    ...extra
  );
}

function subscribeLive() {
  if (liveUnsubscribe && liveCoupleId === state.coupleId) return;
  if (liveUnsubscribe) liveUnsubscribe();
  liveCoupleId = state.coupleId;
  liveLetters = [];
  olderLetters = [];
  olderCursor = null;
  hasMoreOlder = true;

  liveUnsubscribe = onSnapshot(lettersQuery(limit(PAGE_SIZE)), snapshot => {
    liveLetters = snapshot.docs.map(d => ({ ...d.data(), messageId: d.id }));
    oldestLiveSnapshot = snapshot.docs[snapshot.docs.length - 1] || null;
    if (snapshot.docs.length < PAGE_SIZE && olderLetters.length === 0) hasMoreOlder = false;
    renderLetterList();
  }, err => {
    console.error("Error al escuchar cartas:", err);
    const list = document.getElementById("letters-list");
    if (list) list.innerHTML = `<div class="letters-empty">⚠️ No se pudieron cargar las cartas: ${escapeHtml(err.message)}</div>`;
  });
}

export async function loadOlderLetters() {
  if (isLoadingOlder || !hasMoreOlder) return;
  const cursor = olderCursor || oldestLiveSnapshot;
  if (!cursor) return;
  isLoadingOlder = true;
  renderLoadMore();
  try {
    const snap = await getDocs(lettersQuery(startAfter(cursor), limit(PAGE_SIZE)));
    olderLetters = [...olderLetters, ...snap.docs.map(d => ({ ...d.data(), messageId: d.id }))];
    if (snap.docs.length > 0) olderCursor = snap.docs[snap.docs.length - 1];
    if (snap.docs.length < PAGE_SIZE) hasMoreOlder = false;
  } catch (err) {
    console.error("Error cargando cartas anteriores:", err);
    if (window.showToast) window.showToast("No se pudieron cargar más cartas", true);
  } finally {
    isLoadingOlder = false;
    renderLetterList();
  }
}

// ---------- Render ----------

export function renderLettersView() {
  const container = document.getElementById("letters-container");
  if (!container) return;

  // setMode() se vuelve a llamar con cada snapshot de espíritus: no reconstruir si ya está montada
  if (!container.querySelector(".letters-view")) {
    container.innerHTML = `
      <div class="letters-view">
        <section class="letter-composer" id="letter-composer">
          <div class="letter-composer-header">
            <h2 id="letter-composer-title">✍️ Escribir una carta</h2>
            <button type="button" class="btn btn-secondary" id="letter-cancel-edit" style="display:none;" onclick="window.cancelLetterEdit()">Cancelar edición</button>
          </div>
          <input type="text" id="letter-title-input" maxlength="120" placeholder="Título (opcional)">
          ${renderToolbar()}
          <div class="letter-editor-wrap">
            <div id="letter-content-input" class="letter-editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="Escribe tu carta..."></div>
            <div class="letter-image-bar" id="letter-image-bar" hidden>
              <button type="button" class="letter-tool" data-img="w" data-value="33">Pequeña</button>
              <button type="button" class="letter-tool" data-img="w" data-value="50">Mediana</button>
              <button type="button" class="letter-tool" data-img="w" data-value="100">Completa</button>
              <span class="letter-image-bar-sep"></span>
              <button type="button" class="letter-tool" data-img="align" data-value="left" title="Izquierda">◀</button>
              <button type="button" class="letter-tool" data-img="align" data-value="center" title="Centro">◀▶</button>
              <button type="button" class="letter-tool" data-img="align" data-value="right" title="Derecha">▶</button>
              <span class="letter-image-bar-sep"></span>
              <button type="button" class="letter-tool" data-img="up" title="Subir">⬆</button>
              <button type="button" class="letter-tool" data-img="down" title="Bajar">⬇</button>
              <button type="button" class="letter-tool" data-img="delete" title="Quitar foto">🗑️</button>
            </div>
          </div>
          <div class="letter-image-picker">
            <label class="btn btn-secondary letter-image-btn">
              📷 Insertar foto
              <input type="file" id="letter-image-input" accept="image/*" multiple hidden onchange="window.onLetterImageSelected(this)">
            </label>
            <span class="letter-composer-hint">Toca una foto para cambiar su tamaño, alinearla o moverla. También puedes arrastrarla.</span>
          </div>
          <div class="letter-composer-actions">
            <span class="letter-composer-hint" id="letter-composer-hint"></span>
            <button type="button" class="btn letter-send-btn" id="letter-send-btn" onclick="window.submitLetter()">💌 Enviar carta</button>
          </div>
        </section>
        <div id="letters-list" class="letters-list">
          <div class="letters-empty">Cargando cartas...</div>
        </div>
        <div id="letters-load-more"></div>
      </div>
    `;
    initEditor();
    // Fotos dentro del texto de una carta: se amplían al tocarlas
    document.getElementById("letters-list").addEventListener("click", (e) => {
      const img = e.target.closest(".letter-content img");
      if (img) openLetterImage(img.getAttribute("src").split("#")[0]);
    });
  }

  updateComposerHint();
  subscribeLive();
  renderLetterList();
}

function updateComposerHint() {
  const hint = document.getElementById("letter-composer-hint");
  if (!hint) return;
  hint.textContent = state.currentUser
    ? `Se enviará como ${state.currentUser.name}`
    : "Inicia sesión para escribir cartas";
}

function renderLetterCard(letter) {
  const inline = new Set(getLetterInlineImageUrls(letter.content || ""));
  const images = getLetterImages(letter).filter(url => !inline.has(String(url).split("#")[0]));
  const own = isOwnLetter(letter);
  const liked = !!(letter.liked || letter.isLiked);
  const imagesHtml = images.length
    ? `<div class="letter-images ${images.length > 1 ? "multi" : ""}">${images.map(url =>
        `<img src="${escapeHtml(url)}" alt="Foto de la carta" loading="lazy" decoding="async" onclick="window.openLetterImage('${escapeHtml(url)}')">`
      ).join("")}</div>`
    : "";

  return `
    <article class="letter-card" data-id="${escapeHtml(letter.messageId)}">
      <header class="letter-card-header">
        <span class="letter-author" style="color:${authorColorFor(letter)};">
          ${/^https:\/\//i.test(letter.authorImageUrl || "") ? `<img class="letter-avatar" src="${escapeHtml(letter.authorImageUrl)}" alt="" loading="lazy">` : ""}
          ${escapeHtml(letter.authorName || "Anónimo")}
        </span>
        <span class="letter-date">${formatLetterDate(letter.timestamp)}</span>
      </header>
      ${letter.title ? `<h3 class="letter-title">${escapeHtml(letter.title)}</h3>` : ""}
      ${letter.content ? `<div class="letter-content">${letterToSafeHtml(letter.content)}</div>` : ""}
      ${imagesHtml}
      <footer class="letter-card-footer">
        ${own
          // En las cartas propias el corazón solo muestra si la otra persona dio me gusta
          ? `<span class="letter-like-btn readonly ${liked ? "active" : ""}" title="${liked ? "A tu pareja le encantó" : "Aún sin me gusta"}">${liked ? "❤️" : "🤍"}</span>`
          : `<button type="button" class="letter-like-btn ${liked ? "active" : ""}" onclick="window.toggleLetterLike('${escapeHtml(letter.messageId)}')" title="Me gusta">${liked ? "❤️" : "🤍"}</button>`}
        ${own ? `
          <button type="button" class="btn btn-secondary letter-action-btn" onclick="window.editLetter('${escapeHtml(letter.messageId)}')">✏️ Editar</button>
          <button type="button" class="btn btn-danger letter-action-btn" onclick="window.deleteLetter('${escapeHtml(letter.messageId)}')">🗑️</button>
        ` : ""}
      </footer>
    </article>
  `;
}

function renderLetterList() {
  const list = document.getElementById("letters-list");
  if (!list) return;
  const letters = getAllLetters();
  list.innerHTML = letters.length
    ? letters.map(renderLetterCard).join("")
    : `<div class="letters-empty">Todavía no hay cartas. ¡Escribe la primera! 💌</div>`;
  renderLoadMore();
}

function renderLoadMore() {
  const box = document.getElementById("letters-load-more");
  if (!box) return;
  if (!hasMoreOlder || getAllLetters().length === 0) {
    box.innerHTML = "";
    return;
  }
  box.innerHTML = `<button type="button" class="btn btn-secondary letters-load-more-btn" ${isLoadingOlder ? "disabled" : ""} onclick="window.loadOlderLetters()">
    ${isLoadingOlder ? "Cargando..." : "📜 Ver cartas anteriores"}
  </button>`;
}

// ---------- Editor con formato ----------

// Los botones usan onmousedown + preventDefault para no quitarle la selección al editor
function toolbarButton(label, title, action, extraClass = "") {
  return `<button type="button" class="letter-tool ${extraClass}" title="${title}" data-action="${action}">${label}</button>`;
}

function renderToolbar() {
  const sizes = LETTER_SIZES.map(s =>
    `<button type="button" class="letter-tool letter-size-opt" data-action="size" data-value="${s.value}"><font size="${s.value === "7" ? "5" : s.value}">${s.label}</font></button>`
  ).join("");
  const colors = LETTER_COLORS.map(c =>
    `<button type="button" class="letter-color-swatch" data-action="color" data-value="${c.hex}" title="${c.label}" style="background:${c.hex};"></button>`
  ).join("");
  return `
    <div class="letter-toolbar" id="letter-toolbar">
      <div class="letter-tool-group">
        ${toolbarButton("<b>B</b>", "Negrita (Ctrl+B)", "bold", "fmt-state")}
        ${toolbarButton("<i>I</i>", "Cursiva (Ctrl+I)", "italic", "fmt-state")}
        ${toolbarButton("<u>U</u>", "Subrayado (Ctrl+U)", "underline", "fmt-state")}
        ${toolbarButton("<s>S</s>", "Tachado", "strikeThrough", "fmt-state")}
      </div>
      <div class="letter-tool-group">
        ${toolbarButton("Tamaño ▾", "Tamaño de letra", "toggle-sizes")}
        ${toolbarButton("Color 🎨", "Color de letra", "toggle-colors")}
      </div>
      <div class="letter-tool-group">
        ${toolbarButton("◀", "Alinear a la izquierda", "justifyLeft")}
        ${toolbarButton("◀▶", "Centrar", "justifyCenter")}
        ${toolbarButton("▶", "Alinear a la derecha", "justifyRight")}
      </div>
      <div class="letter-tool-group">
        ${toolbarButton("❝ Cita", "Cita", "quote")}
        ${toolbarButton("♡ Separador", "Separador", "separator")}
        ${toolbarButton("⌫ Formato", "Quitar formato", "removeFormat")}
      </div>
      <div class="letter-tool-group">
        ${toolbarButton("↶", "Deshacer (Ctrl+Z)", "undo")}
        ${toolbarButton("↷", "Rehacer (Ctrl+Y)", "redo")}
      </div>
      <div class="letter-tool-panel" id="letter-sizes-panel" hidden>${sizes}</div>
      <div class="letter-tool-panel" id="letter-colors-panel" hidden>${colors}</div>
    </div>
  `;
}

function getEditor() {
  return document.getElementById("letter-content-input");
}

function runEditorCommand(action, value) {
  const editor = getEditor();
  if (!editor) return;
  editor.focus();
  // <font color/size> y align en vez de estilos CSS: es lo que entiende Html.fromHtml en la app
  document.execCommand("styleWithCSS", false, false);
  switch (action) {
    case "size":
      document.execCommand("fontSize", false, value);
      break;
    case "color":
      document.execCommand("foreColor", false, value);
      break;
    case "quote": {
      const selected = window.getSelection()?.toString() || "";
      document.execCommand("insertHTML", false, `<blockquote><i>«${escapeHtml(selected) || "&nbsp;"}»</i></blockquote><br>`);
      break;
    }
    case "separator":
      document.execCommand("insertHTML", false, `<p align="center" style="text-align:center">─── ♡ ───</p><br>`);
      break;
    default:
      document.execCommand(action, false, null);
  }
  updateToolbarState();
}

function updateToolbarState() {
  document.querySelectorAll("#letter-toolbar .fmt-state").forEach(btn => {
    let active = false;
    try { active = document.queryCommandState(btn.dataset.action); } catch (e) {}
    btn.classList.toggle("active", active);
  });
}

function togglePanel(id) {
  ["letter-sizes-panel", "letter-colors-panel"].forEach(panelId => {
    const panel = document.getElementById(panelId);
    if (panel) panel.hidden = panelId === id ? !panel.hidden : true;
  });
}

function initEditor() {
  const toolbar = document.getElementById("letter-toolbar");
  const editor = getEditor();
  if (!toolbar || !editor) return;

  toolbar.addEventListener("mousedown", (e) => {
    const btn = e.target.closest("[data-action]");
    if (!btn) return;
    e.preventDefault();
    const action = btn.dataset.action;
    if (action === "toggle-sizes") return togglePanel("letter-sizes-panel");
    if (action === "toggle-colors") return togglePanel("letter-colors-panel");
    runEditorCommand(action, btn.dataset.value);
    if (action === "size" || action === "color") togglePanel(null);
  });

  // Pegar desde Word u otras páginas: se limpia al mismo subconjunto de etiquetas que usa la app
  editor.addEventListener("paste", (e) => {
    const data = e.clipboardData;
    if (!data) return;
    e.preventDefault();
    const html = data.getData("text/html");
    if (html) {
      document.execCommand("insertHTML", false, letterToSafeHtml(html));
    } else {
      document.execCommand("insertText", false, data.getData("text/plain"));
    }
  });

  editor.addEventListener("keyup", updateToolbarState);
  editor.addEventListener("mouseup", updateToolbarState);
  initImageEditing();
}

// ---------- Fotos ----------

// PNG/WebP/GIF pueden tener transparencia: se mantienen en PNG (JPEG la rellena de negro)
function resizeImageToDataUrl(file) {
  const keepAlpha = /^image\/(png|webp|gif)$/i.test(file.type);
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      const scale = Math.min(1, MAX_IMAGE_SIDE / Math.max(img.naturalWidth, img.naturalHeight));
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(img.naturalWidth * scale);
      canvas.height = Math.round(img.naturalHeight * scale);
      canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(url);
      resolve(keepAlpha ? canvas.toDataURL("image/png") : canvas.toDataURL("image/jpeg", 0.85));
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("No se pudo leer la imagen"));
    };
    img.src = url;
  });
}

// Cada foto va en su propio párrafo: así se alinea con text-align y se mueve entre párrafos
function imageBlockHtml(src, width = 100, align = "center") {
  return `<p align="${align}" style="text-align:${align}"><img src="${escapeHtml(src)}" data-w="${width}" width="${width}%"></p>`;
}

export async function onLetterImageSelected(input) {
  const files = Array.from(input.files || []);
  input.value = "";
  if (files.length === 0) return;
  const editor = getEditor();
  try {
    let html = "";
    for (const file of files) {
      html += imageBlockHtml(await resizeImageToDataUrl(file));
    }
    editor.focus();
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0 || !editor.contains(sel.anchorNode)) {
      // Sin cursor dentro del editor: al final de la carta
      const range = document.createRange();
      range.selectNodeContents(editor);
      range.collapse(false);
      sel.removeAllRanges();
      sel.addRange(range);
    }
    document.execCommand("insertHTML", false, html + "<br>");
  } catch (err) {
    if (window.showToast) window.showToast(err.message, true);
  }
}

let selectedImage = null;

function getImageBlock(img) {
  const editor = getEditor();
  let node = img;
  while (node.parentElement && node.parentElement !== editor) node = node.parentElement;
  return node.parentElement === editor ? node : null;
}

// Asegura que la foto esté sola en su párrafo (si se arrastró dentro de una línea de texto)
function isolateImage(img) {
  let block = getImageBlock(img);
  if (block === img || !block || block.textContent.trim() !== "" || block.querySelectorAll("img").length > 1) {
    const wrapper = document.createElement("p");
    wrapper.setAttribute("align", "center");
    wrapper.style.textAlign = "center";
    const ref = block && block !== img ? block : img;
    ref.parentNode.insertBefore(wrapper, block && block !== img ? block.nextSibling : img);
    wrapper.appendChild(img);
    block = wrapper;
  }
  return block;
}

function selectImage(img) {
  if (selectedImage) selectedImage.classList.remove("selected");
  selectedImage = img;
  const bar = document.getElementById("letter-image-bar");
  if (!img) {
    if (bar) bar.hidden = true;
    return;
  }
  img.classList.add("selected");
  if (bar) {
    bar.hidden = false;
    positionImageBar();
  }
}

function positionImageBar() {
  const bar = document.getElementById("letter-image-bar");
  const wrap = bar?.parentElement;
  if (!bar || !wrap || !selectedImage || !selectedImage.isConnected) return selectImage(null);
  const wrapRect = wrap.getBoundingClientRect();
  const imgRect = selectedImage.getBoundingClientRect();
  const top = Math.max(4, imgRect.top - wrapRect.top - bar.offsetHeight - 6);
  bar.style.top = `${top}px`;
  bar.style.left = `${Math.max(4, Math.min(imgRect.left - wrapRect.left, wrapRect.width - bar.offsetWidth - 4))}px`;
  bar.querySelectorAll('[data-img="w"]').forEach(b => b.classList.toggle("active", Number(b.dataset.value) === getImageWidth(selectedImage)));
}

function applyImageAction(action, value) {
  const img = selectedImage;
  if (!img || !img.isConnected) return selectImage(null);
  const editor = getEditor();
  if (action === "w") {
    img.setAttribute("data-w", value);
    img.setAttribute("width", `${value}%`);
  } else if (action === "align") {
    const block = isolateImage(img);
    block.setAttribute("align", value);
    block.style.textAlign = value;
  } else if (action === "up" || action === "down") {
    const block = isolateImage(img);
    // Salta los <br> y espacios sueltos: la foto se mueve al párrafo anterior/siguiente visible
    const isFiller = (n) => n && ((n.nodeType === Node.TEXT_NODE && !n.nodeValue.trim()) || n.nodeName === "BR");
    let target = action === "up" ? block.previousSibling : block.nextSibling;
    while (isFiller(target)) target = action === "up" ? target.previousSibling : target.nextSibling;
    if (target) editor.insertBefore(block, action === "up" ? target : target.nextSibling);
    block.scrollIntoView({ block: "nearest" });
  } else if (action === "delete") {
    const block = getImageBlock(img);
    if (block && block !== img && block.textContent.trim() === "" && block.querySelectorAll("img").length === 1) block.remove();
    else img.remove();
    return selectImage(null);
  }
  positionImageBar();
}

function initImageEditing() {
  const editor = getEditor();
  const bar = document.getElementById("letter-image-bar");
  if (!editor || !bar) return;

  editor.addEventListener("click", (e) => {
    selectImage(e.target.tagName === "IMG" ? e.target : null);
  });
  editor.addEventListener("input", () => { if (selectedImage) positionImageBar(); });
  // Tras arrastrar una foto, el navegador crea un nodo nuevo: se selecciona el que quedó
  editor.addEventListener("drop", () => setTimeout(() => {
    const img = editor.querySelector("img.selected") || null;
    if (img) {
      isolateImage(img);
      selectImage(img);
    } else {
      selectImage(null);
    }
  }, 0));
  bar.addEventListener("mousedown", (e) => {
    const btn = e.target.closest("[data-img]");
    if (!btn) return;
    e.preventDefault();
    applyImageAction(btn.dataset.img, btn.dataset.value);
  });
  window.addEventListener("resize", () => { if (selectedImage) positionImageBar(); });
}

async function uploadLetterImage(dataUrl) {
  const res = await fetch("/api/upload-letter-image", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ imageBase64: dataUrl })
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success) throw new Error(json.error || `Error ${res.status} al subir la foto`);
  return json.url;
}

// Foto de perfil que la app guarda en users/<docId>.profileImageUrl (MainActivity.upload)
let cachedProfileImage = { docId: null, url: null };
async function getMyProfileImageUrl() {
  const docId = state.currentUser?.docId;
  if (!docId) return null;
  if (cachedProfileImage.docId === docId) return cachedProfileImage.url;
  try {
    const snap = await getDoc(doc(db, "users", docId));
    const url = snap.exists() ? (snap.data().profileImageUrl || null) : null;
    cachedProfileImage = { docId, url };
    return url;
  } catch (err) {
    console.warn("No se pudo leer la foto de perfil:", err);
    return null;
  }
}

// ---------- Acciones ----------

function resetComposer() {
  editingLetterId = null;
  selectImage(null);
  const titleInput = document.getElementById("letter-title-input");
  const contentInput = getEditor();
  if (titleInput) titleInput.value = "";
  if (contentInput) contentInput.innerHTML = "";
  togglePanel(null);
  const heading = document.getElementById("letter-composer-title");
  if (heading) heading.textContent = "✍️ Escribir una carta";
  const sendBtn = document.getElementById("letter-send-btn");
  if (sendBtn) sendBtn.textContent = "💌 Enviar carta";
  const cancelBtn = document.getElementById("letter-cancel-edit");
  if (cancelBtn) cancelBtn.style.display = "none";
}

export async function submitLetter() {
  if (isSending) return;
  if (!state.currentUser) {
    if (window.showLoginModal) window.showLoginModal();
    return;
  }
  const title = (document.getElementById("letter-title-input")?.value || "").trim();
  const editor = getEditor();
  if (!editor) return;
  const hasImages = editor.querySelector("img") !== null;
  const plainBefore = letterToPlainText(editor.innerHTML);
  if (!plainBefore && !hasImages) {
    if (window.customAlert) window.customAlert("Escribe algo o agrega una foto antes de enviar.", "✍️");
    return;
  }

  const sendBtn = document.getElementById("letter-send-btn");
  isSending = true;
  if (sendBtn) { sendBtn.disabled = true; sendBtn.textContent = "Enviando..."; }

  try {
    selectImage(null);
    // Las fotos se suben recién al enviar: si se cancela la carta no quedan archivos huérfanos
    const localImages = Array.from(editor.querySelectorAll('img[src^="data:"]'));
    for (let i = 0; i < localImages.length; i++) {
      if (sendBtn) sendBtn.textContent = `Subiendo foto ${i + 1}/${localImages.length}...`;
      localImages[i].setAttribute("src", await uploadLetterImage(localImages[i].getAttribute("src")));
    }
    if (sendBtn) sendBtn.textContent = "Enviando...";
    const content = editorToLetterHtml(editor);
    const plainContent = letterToPlainText(content);
    const images = getLetterInlineImageUrls(content);
    const imageUrl = images[0] || null;

    const isEdit = !!editingLetterId;
    const existing = isEdit ? findLetter(editingLetterId) : null;
    const ref = isEdit ? doc(db, "messages", editingLetterId) : doc(collection(db, "messages"));

    // Campos de Message.kt; al editar se conservan autor, fecha y me gusta
    const letter = {
      messageId: ref.id,
      partnerId: state.coupleId,
      authorId: existing ? existing.authorId : state.currentUser.docId,
      authorName: existing ? existing.authorName : state.currentUser.name,
      authorImageUrl: existing?.authorImageUrl
        || (letterAuthorIsMe(existing) ? await getMyProfileImageUrl() : null),
      title,
      content: plainContent || images.length ? content : "",
      imageUrls: images,
      timestamp: existing ? existing.timestamp : Date.now(),
      liked: existing ? !!existing.liked : false,
      isLiked: existing ? !!existing.liked : false,
      // imageUrls lleva las fotos del texto: versiones antiguas de la app muestran la primera debajo
      type: "MESSAGE"
    };
    await setDoc(ref, letter);
    // Las páginas anteriores no se escuchan en tiempo real: reflejar la edición localmente
    olderLetters = olderLetters.map(l => l.messageId === letter.messageId ? letter : l);

    if (!isEdit) {
      const name = state.currentUser.name;
      sendLetterPush(`Nuevo mensaje de ${name} 💌`, title ? `«${title}»: ${plainContent}` : (plainContent || "Te han enviado una foto 📸"), imageUrl, "carta");
    }
    if (window.showToast) window.showToast(isEdit ? "Carta actualizada ✨" : "Carta enviada ❤️");
    resetComposer();
  } catch (err) {
    console.error("Error enviando carta:", err);
    if (window.customAlert) window.customAlert(`No se pudo enviar la carta: ${err.message}`, "⚠️");
  } finally {
    isSending = false;
    if (sendBtn) {
      sendBtn.disabled = false;
      sendBtn.textContent = editingLetterId ? "💾 Guardar cambios" : "💌 Enviar carta";
    }
  }
}

export async function toggleLetterLike(id) {
  const letter = findLetter(id);
  if (!letter || isOwnLetter(letter)) return;
  const newLiked = !(letter.liked || letter.isLiked);
  letter.liked = newLiked;
  letter.isLiked = newLiked;
  renderLetterList();
  try {
    await updateDoc(doc(db, "messages", id), { liked: newLiked, isLiked: newLiked });
    if (newLiked && state.currentUser) {
      const letterTitle = (letter.title || "").trim() || "una carta";
      sendLetterPush(`¡A ${state.currentUser.name} le encantó! ❤️`, `Le dio me gusta a tu carta: "${letterTitle}"`, getLetterImages(letter)[0] || null, "like");
    }
  } catch (err) {
    console.error("Error al dar me gusta:", err);
    letter.liked = !newLiked;
    letter.isLiked = !newLiked;
    renderLetterList();
    if (window.showToast) window.showToast("No se pudo guardar el me gusta", true);
  }
}

export function editLetter(id) {
  const letter = findLetter(id);
  if (!letter || !isOwnLetter(letter)) return;
  editingLetterId = id;
  selectImage(null);
  document.getElementById("letter-title-input").value = letter.title || "";
  // Las fotos que la carta tenía debajo (cartas de la app) pasan al final del texto para poder moverlas
  const inline = new Set(getLetterInlineImageUrls(letter.content || ""));
  const extraImages = getLetterImages(letter).filter(url => !inline.has(String(url).split("#")[0]));
  getEditor().innerHTML = letterToEditorHtml(letter.content || "") + extraImages.map(url => imageBlockHtml(url)).join("");
  document.getElementById("letter-composer-title").textContent = "✏️ Editando carta";
  document.getElementById("letter-send-btn").textContent = "💾 Guardar cambios";
  document.getElementById("letter-cancel-edit").style.display = "inline-flex";
  document.getElementById("letter-composer").scrollIntoView({ behavior: "smooth", block: "start" });
  document.getElementById("letter-content-input").focus({ preventScroll: true });
}

export function cancelLetterEdit() {
  resetComposer();
}

export function deleteLetter(id) {
  const letter = findLetter(id);
  if (!letter || !isOwnLetter(letter)) return;
  const doDelete = async () => {
    try {
      await deleteDoc(doc(db, "messages", id));
      olderLetters = olderLetters.filter(l => l.messageId !== id);
      if (editingLetterId === id) resetComposer();
      renderLetterList();
      if (window.showToast) window.showToast("Carta eliminada");
    } catch (err) {
      if (window.customAlert) window.customAlert(`No se pudo eliminar: ${err.message}`, "⚠️");
    }
  };
  if (window.customConfirm) window.customConfirm("¿Eliminar esta carta? No se puede deshacer.", "🗑️", doDelete);
}

export function openLetterImage(url) {
  const overlay = document.createElement("div");
  overlay.className = "letter-lightbox";
  overlay.innerHTML = `<img src="${escapeHtml(url)}" alt="Foto de la carta">`;
  const close = () => {
    overlay.remove();
    document.removeEventListener("keydown", onKey);
  };
  const onKey = (e) => { if (e.key === "Escape") close(); };
  overlay.addEventListener("click", close);
  document.addEventListener("keydown", onKey);
  document.body.appendChild(overlay);
}

// Window bindings (los botones usan onclick inline como el resto de la web)
window.renderLettersView = renderLettersView;
window.loadOlderLetters = loadOlderLetters;
window.submitLetter = submitLetter;
window.toggleLetterLike = toggleLetterLike;
window.editLetter = editLetter;
window.cancelLetterEdit = cancelLetterEdit;
window.deleteLetter = deleteLetter;
window.onLetterImageSelected = onLetterImageSelected;
window.openLetterImage = openLetterImage;
