/**
 * rich-text.js - Formato de las cartas
 *
 * La app muestra las cartas con Html.fromHtml (MessageFeedCompose.kt), que entiende
 * <b> <i> <u> <s> <font color> <big> <small> <blockquote> <br> y <p style="text-align:...">,
 * pero ignora <font size> y <p align>. Este módulo convierte cualquier HTML (el del editor del
 * navegador o el que viene de Firestore) a ese subconjunto, descartando todo lo demás. Sirve para guardar y para mostrar
 * de forma segura: nunca se inserta HTML de una carta sin pasar por aquí.
 */

// Mismas opciones que el editor de la app
export const LETTER_SIZES = [
  { value: "2", label: "Pequeño" },
  { value: "4", label: "Normal" },
  { value: "6", label: "Grande" },
  { value: "7", label: "Titular" }
];

export const LETTER_COLORS = [
  { hex: "#E91E63", label: "Rosa Pasión" },
  { hex: "#FF4081", label: "Rosa Claro" },
  { hex: "#9C27B0", label: "Morado" },
  { hex: "#673AB7", label: "Violeta" },
  { hex: "#2196F3", label: "Azul Cielo" },
  { hex: "#00BCD4", label: "Cian" },
  { hex: "#4CAF50", label: "Verde" },
  { hex: "#8BC34A", label: "Lima" },
  { hex: "#FF9800", label: "Naranja" },
  { hex: "#FF5722", label: "Coral" },
  { hex: "#FFD700", label: "Dorado" },
  { hex: "#8D6E63", label: "Marrón" },
  { hex: "#FFFFFF", label: "Blanco" },
  { hex: "#111111", label: "Negro" }
];

const SIMPLE_TAGS = { B: "b", STRONG: "b", I: "i", EM: "i", U: "u", S: "s", STRIKE: "s", DEL: "s", BIG: "big", SMALL: "small" };
const BLOCK_TAGS = new Set(["DIV", "P", "H1", "H2", "H3", "H4", "H5", "H6", "LI"]);

function escapeText(text) {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function rgbToHex(value) {
  const m = /^rgba?\((\d+),\s*(\d+),\s*(\d+)/i.exec(value || "");
  if (!m) return null;
  return "#" + [m[1], m[2], m[3]].map(n => Number(n).toString(16).padStart(2, "0")).join("").toUpperCase();
}

function safeColor(value) {
  const v = (value || "").trim();
  if (/^#[0-9a-f]{3}([0-9a-f]{3})?$/i.test(v)) return v.toUpperCase();
  return rgbToHex(v);
}

function safeSize(value) {
  const v = String(value || "").trim();
  return /^[1-7]$/.test(v) ? v : null;
}

function getAlign(el) {
  // El estilo manda: los botones de alineación de Chrome cambian text-align pero dejan el atributo align
  const a = (el.style?.textAlign || (el.getAttribute && el.getAttribute("align")) || "").toLowerCase();
  return a === "center" || a === "right" ? a : null;
}

// Envuelve el contenido con <font> según los estilos en línea que pone el navegador (color, tamaño)
function wrapInlineStyles(el, inner) {
  let out = inner;
  const style = el.style;
  if (!style) return out;
  const color = safeColor(style.color);
  if (color) out = `<font color="${color}">${out}</font>`;
  const weight = style.fontWeight;
  if (weight === "bold" || Number(weight) >= 600) out = `<b>${out}</b>`;
  if (style.fontStyle === "italic") out = `<i>${out}</i>`;
  const deco = style.textDecorationLine || style.textDecoration || "";
  if (deco.includes("underline")) out = `<u>${out}</u>`;
  if (deco.includes("line-through")) out = `<s>${out}</s>`;
  return out;
}

function isBlockElement(node) {
  return !!node && node.nodeType === Node.ELEMENT_NODE && (BLOCK_TAGS.has(node.tagName) || node.tagName === "BLOCKQUOTE");
}

// Html.fromHtml ignora <font size>; <small> y <big> sí cambian el tamaño en la app
function wrapSize(size, html) {
  const n = Number(size);
  if (!n || n === 3 || n === 4) return html;
  if (n <= 2) return `<small>${html}</small>`;
  if (n === 5 || n === 6) return `<big>${html}</big>`;
  return `<big><big>${html}</big></big>`;
}

// Ancho de una foto dentro del texto, en % (solo los tres tamaños del editor)
export const LETTER_IMAGE_WIDTHS = [33, 50, 100];

export function getImageWidth(img) {
  const fromAttr = Number(img.getAttribute("data-w"));
  const fromSrc = Number((/#w=(\d{1,3})/.exec(img.getAttribute("src") || "") || [])[1]);
  const w = fromAttr || fromSrc || 100;
  return LETTER_IMAGE_WIDTHS.reduce((best, x) => Math.abs(x - w) < Math.abs(best - w) ? x : best, 100);
}

let allowDataImages = false;

// <img src="https://...#w=50" width="50%">: la app lee el ancho del fragmento #w (LetterImageGetter.kt)
function serializeImage(img) {
  const rawSrc = (img.getAttribute("src") || "").trim();
  const src = rawSrc.split("#")[0];
  const isHttps = /^https:\/\/[^\s"'<>]+$/i.test(src);
  const isData = allowDataImages && /^data:image\/(png|jpe?g|gif|webp);base64,[a-z0-9+/=]+$/i.test(src);
  if (!isHttps && !isData) return "";
  const w = getImageWidth(img);
  return `<img src="${src.replace(/&/g, "&amp;")}${isHttps ? `#w=${w}` : ""}" data-w="${w}" width="${w}%">`;
}

function serializeChildren(node) {
  let out = "";
  node.childNodes.forEach(child => {
    const part = serializeNode(child);
    // Un bloque (párrafo, línea del editor) siempre empieza en una línea nueva, aunque no tenga alineación
    if (part && isBlockElement(child) && out && !/(<br>|<\/p>|<\/blockquote>)$/.test(out) && !part.startsWith("<p ")) {
      out += "<br>";
    }
    out += part;
  });
  return out;
}

function serializeNode(node) {
  if (node.nodeType === Node.TEXT_NODE) {
    // Espacios entre párrafos (formato del HTML, no del texto): Html.fromHtml también los ignora
    if (/^\s*$/.test(node.nodeValue) && (isBlockElement(node.previousSibling) || isBlockElement(node.nextSibling))) {
      return "";
    }
    // Los saltos de línea del texto plano (cartas antiguas de la app) se muestran como <br>
    return escapeText(node.nodeValue).replace(/\r?\n/g, "<br>");
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return "";

  const tag = node.tagName;
  if (tag === "SCRIPT" || tag === "STYLE" || tag === "IFRAME" || tag === "OBJECT" || tag === "TEMPLATE") return "";
  if (tag === "BR") return "<br>";
  if (tag === "IMG") return serializeImage(node);

  const inner = serializeChildren(node);

  if (SIMPLE_TAGS[tag]) {
    const t = SIMPLE_TAGS[tag];
    return inner ? wrapInlineStyles(node, `<${t}>${inner}</${t}>`) : "";
  }

  if (tag === "FONT") {
    if (!inner) return "";
    const color = safeColor(node.getAttribute("color"));
    let out = wrapSize(safeSize(node.getAttribute("size")), wrapInlineStyles(node, inner));
    return color ? `<font color="${color}">${out}</font>` : out;
  }

  if (tag === "BLOCKQUOTE") {
    return inner ? `<blockquote>${inner}</blockquote>` : "";
  }

  if (BLOCK_TAGS.has(tag)) {
    // Una foto siempre va en su propio párrafo (también alineada a la izquierda) para no quedar pegada al texto
    const align = getAlign(node) || (node.querySelector && node.querySelector("img") ? "left" : null);
    const content = wrapInlineStyles(node, inner);
    // style para Html.fromHtml (Android 7+), align para las cartas antiguas y los navegadores
    if (align) return `<p align="${align}" style="text-align:${align}">${content || "<br>"}</p>`;
    // Bloque sin alineación (cada Enter del editor crea uno): equivale a una línea nueva
    if (!content || content === "<br>") return "<br>";
    return content.endsWith("<br>") ? content : content + "<br>";
  }

  // Cualquier otra etiqueta (span, a, img, etc.): se conserva solo el texto y los estilos permitidos
  return wrapInlineStyles(node, inner);
}

function parseHtml(html) {
  const tpl = document.createElement("template");
  tpl.innerHTML = html || "";
  return tpl.content;
}

function trimBreaks(html) {
  return html.replace(/^(<br>)+/, "").replace(/(<br>)+$/, "");
}

/** HTML seguro para mostrar una carta (texto plano o HTML de la app). */
export function letterToSafeHtml(content) {
  return trimBreaks(serializeChildren(parseHtml(content)));
}

/** HTML a guardar en Firestore a partir del editor visual (las fotos ya deben estar subidas). */
export function editorToLetterHtml(editorEl) {
  return trimBreaks(serializeChildren(editorEl));
}

/** Igual que letterToSafeHtml, pero conserva las fotos locales (data:) aún no subidas. Solo para el editor. */
export function letterToEditorHtml(content) {
  allowDataImages = true;
  try {
    return letterToSafeHtml(content);
  } finally {
    allowDataImages = false;
  }
}

/** URLs (sin #w) de las fotos insertadas en el contenido. */
export function getLetterInlineImageUrls(content) {
  const tpl = parseHtml(content);
  return Array.from(tpl.querySelectorAll("img"))
    .map(img => (img.getAttribute("src") || "").split("#")[0])
    .filter(src => /^https:\/\//i.test(src));
}

/** Texto sin formato (vista previa de notificaciones, validación de vacío). */
export function letterToPlainText(content) {
  const tmp = document.createElement("div");
  tmp.innerHTML = letterToSafeHtml(content).replace(/<br>/g, "\n").replace(/<\/p>/g, "</p>\n");
  return (tmp.textContent || "").replace(/\n{3,}/g, "\n\n").trim();
}
