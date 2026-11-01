# Web de Gestión - Diario Ali & Kevin

Todo lo específico de la web. Lo general y los modelos de Firestore están en [contexto.md](../contexto.md); las reglas, en [AGENTS.md](../AGENTS.md); el catálogo de espíritus, en [espiritus.md](../espiritus.md). Los enlaces de los hitos antiguos son relativos a la raíz del repositorio.

---

## Mapa rápido

- **Stack:** JavaScript sin framework (módulos ES) y HTML, empaquetado con Vite. Firestore y Realtime Database con el SDK modular de Firebase. Local: `web/server.js` (Express y WebSockets). Producción: Vercel, con funciones en `web/api/`.
- **`web/js/`:** `app.js` (arranque), `router.js` (modos y temporadas), `state.js` (estado global), `constants.js` (valores por defecto por temporada y paletas), `firestore.js` (lectura y autoguardado), `normal-view.js` (álbum), `edit-view.js` (edición), `categories-view.js` (tipos y categorías), `config-view.js` (configuración y temas), `letters-view.js` y `rich-text.js` (cartas), `radar-view.js` (radar), `websocket.js`, `image-utils.js`, `firebase-config.js`.
- **`web/api/`:** `upload-spirit-image.js`, `upload-letter-image.js`, `send-push.js`, `clean-unused-cloudinary.js`.
- **Temporadas:** el selector se repite en cada HTML de `web/` (`#btn-season-1..3`). Por defecto, T3. Los valores por temporada salen de `defaultSpiritsListFor`, `defaultCategoriesFor`, `defaultSpiritTypesFor`, `spiritImageFolderFor` y `spiritsCollectionFor` (`constants.js`). La temporada elegida se guarda en `localStorage` (`current_season`).

---

## Diseño Bitácora (noviembre 2026)

Elegido por el usuario. Está en el último bloque de `styles.css`: en pantallas de 900 px o más, el `<header>` pasa a menú lateral fijo de 240 px; el selector de temporada muestra T3, T2 y T1 mediante CSS; y en el Álbum las tarjetas son horizontales. Tipos y Categorías, Cartas, Radar y Configuración usan bordes rectos y sombra sólida con el acento; `border-radius` se fuerza con `!important` porque esas vistas lo traen en línea, salvo avatares y elementos del mapa. Solo cambia la presentación: los elementos y funciones siguen siendo los mismos. El selector de temporada solo aparece en Álbum, Modo Edición y Tipos y Categorías (`html[data-mode]`, asignado en `setMode`), por pedido del usuario. Debajo de 900 px, la barra superior se ordena en filas (logo, temporadas y sesión; contadores; las 6 secciones visibles sin scroll, con nombres cortos definidos en CSS) y el Álbum usa 3 columnas con marcas de 40 px; cada tarjeta muestra solo el tipo (`.name-short`, calculado en `spiritLabelHtml`), sin cambiar el nombre guardado. Excepción pedida por el usuario: se ocultan las tarjetas de progreso (`#normal-toolbar`), y su porcentaje y barra pasan a los contadores del menú lateral (`data-pct` y `--stat-pct`, asignados en `updateStats`), visibles en todas las vistas.

**Carga de rutas:** el HTML siempre trae marcado el Álbum. El contenido y la pestaña activa se ocultan hasta que `initRouter` agrega `route-ready` a `<html>`, con un respaldo de 3 s en el script de `<head>`, para que no aparezca el Álbum al recargar otra ruta.

---

## Temas, colores y fuentes

**Toda pantalla nueva debe verse bien en los tres temas y con cualquier color de acento.** Usa las variables, nunca colores fijos.

| Tema (`userTheme`) | Clase en `<html>` y `<body>` | Fondo | Tarjeta | Texto | Acento por defecto |
|:---|:---|:---|:---|:---|:---|
| Pixel Oscuro (por defecto) | `theme-pixel-oscuro` | `#0d0e15` | `#151826` | `#f3f4f6` | `#4A148C` |
| Pixel Claro | `theme-pixel-claro` | `#f6eedf` | `#fffbf4` | `#3d2314` | `#D1C4E9` |
| Pixel Monocromático | `theme-pixel-monocromatico` | `#000000` | `#101010` | `#ffffff` | `#FFFFFF` (fijo) |

- **Variables (`web/styles.css`):** `--bg-color`, `--card-bg`, `--card-border`, `--accent-color`, `--accent-glow`, `--text-color`, `--text-muted`, `--success-color`, `--error-color`, `--kevin-color`, `--ali-color`, `--both-color`. Los ajustes por componente van en reglas `body.theme-pixel-claro …` y `body.theme-pixel-monocromatico …`.
- **Color de acento:** `applyTheme` (`config-view.js`) lo aplica a `--accent-color` y `--accent-glow` con `!important`. Las opciones están en `DARK_COLOR_FAMILIES`, `LIGHT_COLOR_FAMILIES` y `MONO_COLOR_FAMILIES` (`constants.js`). Oscuro y Claro guardan cada uno su color; Monocromático siempre usa blanco.
- **Contraste:** los acentos van de muy oscuros (`#1A237E`) a muy claros (`#FFD600`), y en Claro son pastel. No des por hecho un color de texto sobre el acento, ni uses el acento como única línea de borde sobre el fondo.
- **Fuente:** `font-pixel` (VT323, por defecto) o `font-modern` (Inter y Outfit), guardada en `userFont`.
- **Sin parpadeo inicial:** cada HTML tiene un script en `<head>` que aplica tema, fuente y `slots-compact` antes de pintar. Si agregas una preferencia visual, actualízalo en todos los HTML.
- **Dónde se guardan:** `localStorage` (`userTheme`, `userLightColor`, `userDarkColor`, `userFont`, `userSlotLayout`, `userRefreshRate`, `userUseCustomBg`) y `users/<userId>` en Firestore (`theme` y `appTheme`, `lightColor` y `barColorLight`, `darkColor` y `barColorDark`, `refreshRate`, `webSlotLayout`). Así el tema se sincroniza con la app.
- **Ojo:** `applyTheme` recibe `useCustomBg` pero no lo usa, y `styles.css` no tiene reglas para esa preferencia: en la web, el botón de fondo personalizado guarda el valor sin cambiar nada visible (revisado en noviembre de 2026).

---

## 8. Web de Gestión & Servidor Vercel

- **Ruta Web:** `web/index.html` (repartido en rutas SPA limpias `/`, `/normal`, `/edit`, `/db`, `/config`).
- **Servidor Local:** `web/server.js` (Express + WebSockets Server `ws` en puerto 8000 / fallback).
- **WebSockets en Tiempo Real:** 
  - Conexión persistente cliente-servidor mediante `new WebSocket('/ws')` con reconexión automática y detección de estado en vivo (`ws-status-pill`).
  - Difusión instantánea (Broadcast) entre pestañas y dispositivos de eventos de autoguardado (`DATA_SYNC`), cambios de checks/maestrías (`SPIRIT_TOGGLE`) y subidas de imágenes a Cloudinary (`IMAGE_UPLOADED`).
- **Autoguardado Inteligente y Eliminación de Botones Manuales:**
  - Sistema de autoguardado automático debounced (`triggerAutoSave`) activo en toda la web.
  - Al editar nombres de categorías, renombrar espíritus, reordenar por Drag & Drop, crear o eliminar categorías y tipos, los cambios se persisten inmediatamente en Firestore y se transmiten vía WebSockets con indicador de estado en tiempo real (`✓ Autoguardado` / `🔄 Guardando...`), eliminando la necesidad de botones manuales de guardar.
- **Vercel Serverless Function:** `web/api/upload-spirit-image.js` (Firmado y subida directa de Base64 comprimido a Cloudinary).
- **Almacenamiento Persistente (`requestPersistentStoragePermission` en `web/js/app.js`):** En Chromium (Edge, Brave, Chrome) `navigator.storage.persist()` no muestra aviso y se decide por heurísticas (permiso de notificaciones, marcador, uso frecuente). Si la primera solicitud es rechazada, se pide el permiso de notificaciones y se reintenta `persist()`. El aviso aparece una sola vez: omitirlo o recibir un rechazo guarda `persistent_storage_dismissed`, y después se abre desde Configuración. En `localhost` Chromium nunca lo concede.
- **Carga y Caché de la Web (octubre 2026):**
  - **Firebase modular 10.8.0 (`web/js/firebase-config.js`):** Se reemplazó el SDK compat por el modular (`firebase/app`, `firebase/firestore`, `firebase/database`). Vite lo empaqueta en un chunk propio `assets/firebase-*.js`; el `<script type="importmap">` de los HTML solo actúa al servir el código fuente sin build (`npm run dev`) y apunta a la misma versión en gstatic. Los demás módulos importan `doc`, `setDoc`, `onSnapshot`, `rtdbRef`, etc. desde `firebase-config.js`. La versión de `package.json` y la del import map deben coincidir.
  - **Leaflet bajo demanda:** `ensureLeaflet()` en `web/js/radar-view.js` inyecta el JS y CSS de Leaflet solo al abrir `/radar`.
  - **Service worker (`web/public/sw.js`):** Red primero para HTML y archivos sin hash (con copia de respaldo sin conexión), caché primero para `/assets`, `/fonts` y librerías de CDN. No intercepta `/api`, `/ws` ni Firestore/RTDB.
  - **Fuentes autoalojadas (`web/public/fonts/`):** Inter y Outfit variables y VT323, subconjunto latin, declaradas con `@font-face` al inicio de `styles.css`. Ya no se usa Google Fonts.
  - **Cabeceras de caché:** `/assets` y `/fonts` se sirven con `immutable` a un año en `web/vercel.json` y en `server.js` (solo modo producción).
  - **Radar como módulo diferido:** `router.js` carga `radar-view.js` con `import()` al entrar al modo radar (chunk `assets/radar-view-*.js`). Las funciones `window.*` del radar no existen hasta ese momento; el resto del código ya las invoca con `typeof ... === 'function'`.
  - **Sin `prefetch` de documentos ni `backdrop-filter`:** Los overlays usan fondos más opacos en lugar de desenfoque.
  - **Compresión en `server.js`:** middleware `compression` para el servidor local.
  - **Salvaguarda de escritura de espíritus (`assertSafeSpiritsWrite` en `web/js/firestore.js`):** `saveChangesToFirestoreAsync` es el único punto que escribe en `fortnite_spirits` / `fortnite_spirits_s2` y se bloquea si (a) todavía no llegó el documento de la temporada activa, o (b) se intenta guardar vacío un campo (`kevin_list`, `ali_list`, `kevin_mastery`, `ali_mastery`, `spirits_list`, `categories`) que en Firestore tiene más de un elemento. Un snapshot "no existe" que viene de la caché local se ignora; solo el servidor puede confirmar que el documento no existe.
  - **Tamaño de espíritus en el álbum (`changeSlotLayout` en `web/js/config-view.js`):** Preferencia Compacto (por defecto) / Expandido en Configuración. Compacto añade la clase `slots-compact` a `<html>` y usa `auto-fill` en `.spirit-slots-grid`; Expandido usa `auto-fit` y estira los slots a todo el ancho. Se guarda en `localStorage` (`userSlotLayout`) y en `users/<userId>.webSlotLayout`; el script anti-FOUC de los HTML la aplica antes del primer pintado.
  - **Edición de tipos y categorías (`categories-view.js`):** Los inputs de nombre de categoría, nombre de tipo y sufijo actualizan el estado en `oninput` sin guardar (`save = false`); el autoguardado solo ocurre en `onchange` (al salir del campo o pulsar Enter). `listenFirestore` no redibuja la interfaz mientras haya un `INPUT`/`TEXTAREA` con foco (salvo la carga inicial), para no quitar el foco al escribir.
  - **Guardado con `mergeFields` (`saveChangesToFirestoreAsync`):** Se usa `setDoc(..., { mergeFields: Object.keys(dataToSave) })` en vez de `merge: true`. Con `merge: true` los mapas (`custom_names`, `custom_categories`, `custom_images`) se fusionaban y una clave borrada en local no se borraba en Firestore: al cambiar un espíritu a un tipo cuyo nombre coincide con el nombre por defecto (ej. #143 a *Dulce o truco*), se borraba su `custom_names` local y el snapshot devolvía el nombre viejo, revirtiendo el tipo.
  - **Caché de nombres (`spiritNameCache` en `normal-view.js`):** `renderWorkspace()` llama a `clearSpiritCache()` al inicio, porque las ediciones locales (cambio de tipo, categoría o nombre en `/edit`) modifican `customNames` sin pasar por Firestore. Sin esto se pintaba el nombre anterior y `collectInputsFromDOM()` lo volvía a guardar, revirtiendo el cambio.
  - **Orden dentro de cada categoría:** `renderWorkspace()` ordena los espíritus de cada categoría según la posición de su tipo en *Tipos y Categorías* (`state.spiritTypes`), con orden estable para espíritus del mismo tipo. Los tipos que no existen en la lista van al final.
  - **Normalizar Nombres renumera IDs (`renumberSpiritIds` en `categories-view.js`):** Antes de normalizar nombres, renumera los IDs del 01 al máximo según el orden visible (categorías en orden, dentro de cada una por tipo, luego sueltos). Remapea `categories`, `spirits_list`, `kevin_list`, `ali_list`, `kevin_mastery`, `ali_mastery`, `custom_names` y `custom_images`; fija el nombre actual y la imagen del ID viejo (URL de Cloudinary) para que no cambien al moverse de ID.
  - **Respaldo de Normalizar y botón ⚠️ Restablecer Respaldo (Tipos y Categorías):** `normalizeAllSpiritNames` primero llama a `saveSpiritsBackupAsync` (en `firestore.js`), que copia el documento de la temporada activa al campo `normalize_backup` del mismo documento (1 lectura + 1 escritura). Si el respaldo falla, no se normaliza. El botón ⚠️ pide la contraseña `123` y `restoreSpiritsBackupAsync` reescribe los campos respaldados. El autoguardado usa `mergeFields`, así que nunca toca `normalize_backup`. Solo se guarda el último respaldo. Mientras normaliza o restablece se muestra una pantalla de carga (`showBusyOverlay` en `categories-view.js`) con el paso actual; se cierra al confirmar el guardado, si falla, o a los 20 s como respaldo.
  - **Filtros por visibilidad (`applyFilterVisibility` en `normal-view.js`):** `renderWorkspace()` dibuja siempre todos los espíritus de cada categoría y al final llama a `applyFilterVisibility()`, que oculta con `hidden` las tarjetas que no coinciden con la búsqueda o filtro, oculta las categorías vacías en modo álbum y actualiza los contadores `.cat-match-count`. Búsqueda (`onSearchInput`, `clearSearch`), filtros (`setFilter`) y marcas (`updateSingleSpiritDOM`, `syncAllSpiritSlotsDOM`) ya no reconstruyen la colección.
  - **`/edicion` sin redibujado completo:** Los listeners de las tarjetas (nombre, tipo, categoría, `dragstart`) están delegados en `#categories-container` (`ensureDelegatedListener`). `changeSpiritType` actualiza el input de esa tarjeta y la recoloca según su nuevo tipo (`repositionSlotByType`); `moveSpiritToCategory` y `removeSpiritFromCategory` mueven la tarjeta a la grilla de destino con `moveSlotInDOM` (si no la encuentran, caen en `renderWorkspace()`).
  - **CSS de scroll:** `.category-card` usa `content-visibility: auto` con `contain-intrinsic-size: auto 420px`; la estrella de maestría activa usa `text-shadow` en vez de `filter: drop-shadow`.
  - **Sin redibujar por el guardado propio (`listenFirestore`):** La firma de estructura usa claves ordenadas (`stableStringify`) y solo un snapshot cuya estructura se aplicó al estado (sin escrituras pendientes ni autoguardado en curso) puede pedir redibujar la colección. En `/edicion`, si solo cambiaron checks o maestrías, se actualizan los contadores (`updateStats`). Cambiar el tipo mueve solo esa tarjeta (`repositionSlotByType` en `edit-view.js`) y conserva el foco.
  - **Snapshots sin redibujado duplicado (`listenFirestore`):** Se compara una firma del estado; si no cambió se omite el render, y si solo cambiaron checks/maestrías en modo álbum se llama a `syncAllSpiritSlotsDOM()` en vez de reconstruir la colección.
- **Cartas en la web (`/cartas`, `web/js/letters-view.js`, octubre 2026):**
  - Pestaña "💌 Cartas" cargada bajo demanda por `router.js` (modo `letters`, chunk `assets/letters-view-*.js`). Lee y escribe la misma colección `messages` que la app, con el mismo esquema de `Message.kt` (`messageId`, `partnerId`, `authorId` = `USERS[x].docId`, `authorName`, `authorImageUrl`, `title`, `content`, `imageUrls`, `timestamp`, `liked` + `isLiked`, `type`).
  - **Lecturas acotadas:** listener en tiempo real solo para las 5 cartas más recientes (`partnerId == coupleId`, `orderBy timestamp desc`, `limit 5`; usa el mismo índice compuesto que la app). Las anteriores se piden con `getDocs` + `startAfter` al pulsar "Ver cartas anteriores".
  - Permite escribir (título, texto y 1 foto), editar y eliminar las cartas propias, dar me gusta y ver las fotos en grande. Las fotos se reescalan a 1600px JPEG en el navegador y se suben con `api/upload-letter-image.js` (carpeta `letters/` de Cloudinary).
  - **Magic Packet desde la web (`sendRemoteMagicPing` en `radar-view.js`):** Además del ping en RTDB (`locations/<coupleId>/pings/<user>`), envía por `/api/send-push` el mismo push FCM solo de datos que `ThorRadarManager.sendLocationRequestPing` (`type`/`click_type` `radar_ping`, `magic_packet: WOL_LOCATION_WAKEUP`, `targetDoc`, `silent`). El título y el cuerpo del ping con notificación van dentro de `data` para que no se genere un bloque `notification` y `MyFirebaseMessagingService` lo reciba en segundo plano. Usa `authorId: web_manager`, así el receptor no lo descarta como propio. Antes solo escribía en RTDB, lo que no despertaba al teléfono dormido. `server.js` también expone `/api/send-push` en local.
  - **Notificaciones:** `api/send-push.js` (FCM v1, requiere `FIREBASE_SERVICE_ACCOUNT` en Vercel / `.env`). La web envía pushes solo de datos al tópico `diario_<coupleId normalizado>` con `authorId`, `authorName` y `click_type` (`carta` / `like`), igual que `sendNotificationV1`, para que `MyFirebaseMessagingService` descarte las propias.
  - **Editor con formato (`web/js/rich-text.js`):** editor `contenteditable` con barra de herramientas (negrita, cursiva, subrayado, tachado, tamaños 2/4/6/7, la paleta de 14 colores de `MessageEditorCompose.kt`, alineación, cita, separador, quitar formato, deshacer/rehacer). Guarda HTML que `Html.fromHtml` de Android sí muestra: `<b> <i> <u> <s> <font color> <blockquote> <br>`, tamaños como `<small>` / `<big>` / `<big><big>` (Android ignora `<font size>`) y alineación como `<p align="x" style="text-align:x">` (Android solo lee el `style`). `letterToSafeHtml()` es el único camino para insertar el contenido de una carta en el DOM: convierte texto plano o HTML a ese subconjunto y descarta cualquier otra etiqueta o atributo (evita XSS). Al pegar se limpia igual.
  - **Fotos dentro del texto:** "📷 Insertar foto" coloca cada foto en su propio párrafo (`<p align style="text-align"><img src="URL#w=N" data-w="N" width="N%"></p>`, N = 33/50/100). Al tocarla aparece una barra flotante para tamaño (Pequeña/Mediana/Completa), alineación, subir/bajar y quitar; también se pueden arrastrar. Las fotos quedan como `data:` en el editor y se suben a Cloudinary recién al enviar. `imageUrls` guarda las URLs de las fotos del texto (las versiones antiguas de la app muestran la primera debajo). Al editar una carta con fotos "de abajo", esas fotos pasan al final del texto. Las fotos alineadas a la izquierda también se guardan en su propio `<p align="left">`, y al mostrar una carta las fotos se ven siempre en su propia línea (`display:block`), incluso en cartas guardadas antes de esta corrección. En la app, `LetterImageGetter.isolateImages()` hace lo mismo antes de `Html.fromHtml` en `MessageFeedCompose`.
  - **App Android (`compose/LetterImageGetter.kt`):** `MessageFeedCompose.kt` pasa un `Html.ImageGetter` a `Html.fromHtml` que descarga las fotos del texto con Coil y las dimensiona según `#w=` sobre el ancho del `TextView`; las fotos que ya están en el texto no se repiten debajo. `LastMessageWidget.kt` y el texto de álbum eliminan el carácter de objeto `\uFFFC` que deja cada `<img>`. Requiere publicar una versión nueva del APK; las versiones anteriores muestran un recuadro "￼" en el lugar de la foto.
  - **Foto de perfil y transparencia:** al enviar, la web lee `users/<docId>.profileImageUrl` (1 lectura, cacheada por sesión) y la guarda en `authorImageUrl`, como hace la app. Las fotos PNG/WebP/GIF se reescalan conservando el canal alfa (PNG); el resto se convierte a JPEG 0.85.
  - A diferencia de la app, escribir desde la web no llama a `updatePetOnInteraction()` (no suma puntos a la mascota).
  - `server.js` expone `/cartas`, `/api/upload-letter-image` y `/api/send-push` reutilizando los handlers de `api/`.
- **Sincronización de Tema:** Modificar `theme` en la web actualiza el documento Firestore `users/<userId>` y adapta al instante los colores en la App Android.

---

## 10. Últimos Hitos Implementados (Temporada 2 Espíritus & Herramientas Web)

1. **Separación Multitemporada en Espíritus:**
   - **Temporada 1:** Preserva los 141 espíritus originales y su historial en `fortnite_spirits/<coupleId>`.
   - **Temporada 2:** Nueva colección activa por defecto en `fortnite_spirits_s2/<coupleId>`.
   - Selector de temporada interactivo integrado en la App (`[T2] / [T1]`) y en la Web (`[🌟 Temporada 2] / [🕰️ Temporada 1]`).
2. **Extracción y Procesamiento de la Planilla Fortnite Override (Capítulo 7 T4) y Fortnite.gg:**
   - **Tanda 1 (01 a 36):** Recorte y limpieza de 36 espíritus iniciales (12 personajes con variantes: *Normal*, *Dorado*, *Hacker*).
   - **Tanda 2 (37 a 61):** Extracción, recorte y limpieza con transparencia de 25 espíritus (Caballero, Onigiri, Científico con variantes Normal/Matrix/Dorado/Hacker de botín, Megabot, variantes Hacker de botín).
   - **Tanda 3 (62 a 109 - 48 nuevos agregados desde Fortnite.gg):** Incorporación de Blinky (62..66), Crash Bandicoot (67..70, 85), 5tas variantes Cazarrecompensas de todos los personajes (71..84), Morgana (86..90), Sobreescudo (91..95), Estanque (96..100), Cumpleaños (101..105) y Colaboraciones Especiales: John Wick (106), Ironmouse (107), Pollo (108) y Vini Jr. (109).
   - **Total Temporada 2:** **109 espíritus** completamente alojados en Cloudinary (`spirits_s2/ic_spirit_s2_01` a `109`), vinculados en Firestore (`fortnite_spirits_s2/<coupleId>`) y compatibles con sincronización en tiempo real en Web y Android Compose.
   - Activos individuales preservados localmente en `scripts/spirits_s2_extracted/`, `scripts/spirits_s2_new_extracted/` y `scripts/spirits_s2_extracted_new_39/`.
3. **Sincronización Automática de Nombres al Renombrar Categorías:**
   - Al cambiar el nombre de cualquier categoría (ej. *"Espíritu de Rex"* ➔ *"Espíritu de Klombo"*), tanto la Web (`web/config.html`, `/edit`) como la App Android (`SpiritsCompose.kt`) detectan todos los espíritus pertenecientes a esa categoría, preservan sus sufijos de tipo (*Normal, Dorado, Hacker, etc.*) y actualizan en tiempo real los registros en `custom_names` y `custom_categories` en Firestore.
4. **Rediseño Completo del Editor de Imágenes Studio (Recorte & Quitar Fondo):**
   - **Caja de Selección Interactiva con 8 Puntos de Ajuste**: Detección sensible adaptable por DPI/pantalla, arrastre de los 4 bordes (`↕️`, `↔️`) y las 4 esquinas (`nwse-resize`, `nesw-resize`), y desplazamiento de la caja completa.
   - **Proporción 1:1 Cuadrada o Libre**: Selector para forzar proporción cuadrada o ajuste libre.
   - **Exportación 100% Limpia sin Artefactos (`getCleanStudioDataUrl`)**: Extracción de píxeles puros directamente de la imagen base original en un lienzo secundario aislado, evitando que las líneas magentas, la cuadrícula o los puntos de control queden estampados en el PNG subido a Cloudinary.
   - **Auto-recorte al Guardar y Subir**: Si una selección está activa al pulsar *"Guardar y Subir Espíritu"*, se recorta y procesa automáticamente sin pasos intermedios.
   - **Historial de Deshacer (`↩️ Deshacer`)**: Posibilidad de revertir recortes, borrados de pincel y extracciones de color.
   - **Soporte Táctil y Móvil**: Eventos Pointer (`pointerdown`, `pointermove`, `pointerup`) para edición fluida en PC, tablets y móviles.
   - **Indicador de Dimensiones en Barra de Herramientas**: Medidas `W × H px` ubicadas en la barra de controles para no obstruir la imagen.
5. **Servidor Local y Enrutamiento SPA Limpio (`web/server.js`, `npm run dev`):**
   - Servidor Node (HTTP + WebSockets) en puerto 8000 con enrutamiento SPA directo sin redirecciones permanentes 301. Reemplazó al antiguo `server.py` (eliminado).
   - Normalización de URLs en el cliente (`replaceState`) para mantener rutas limpias (`http://localhost:8000/`, `/edit`, `/config`, `/db`).
6. **Selector de Tasa de Refresco (Hz) Sincronizado en App & Web:**
   - Opciones dinámicas de **60 Hz (Batería), 90 Hz (Por Defecto / Recomendado), 100 Hz y 120 Hz (Ultra Fluido)**.
   - Sincronización en tiempo real en Firestore (`users/<userId>/refreshRate`) con persistencia local en `SharedPreferences`.
   - Aplicación técnica a bajo nivel en Android mediante `preferredDisplayModeId` (API 23+) y coincidencia óptima con la resolución activa de la pantalla.

---

## Hitos de la web (antes en la sección 14)

22. **Panel de Dispositivos Conectados & Control Remoto de Thor Radar (Web & App):**
    - **Pestaña `📡 Dispositivos & Radar` en la Web:** Nuevo panel de monitoreo y telemetría en tiempo real (`radar.html` / `switchModeSPA('radar')`) que visualiza el estado de conexión de los celulares de Kevin y Ali, porcentaje y estado de carga de batería, velocidad, actividad (`STILL`, `WALKING`, `IN_VEHICLE`), zonas seguras registradas, precisión GPS y visor de datos crudos en vivo.
    - **Interruptor Maestro Remoto de Thor Radar (ON / OFF):** Control interactivo desde la web (`toggleRemoteRadar`) para encender o apagar el radar en el celular de Kevin o Ali (`isSharing: true | false`).
    - **Escucha en Tiempo Real en Android (`MainActivity.kt` & `ThorRadarCompose.kt`):** El dispositivo escucha cambios en su documento `locations/<coupleId>/users/<user>` y, si se desactiva remotamente, suspende inmediatamente el Foreground Service (`ThorRadarService`), detiene el tracking GPS y actualiza el switch local sin requerir reiniciar la app.
    - **Herramientas de Pruebas Integradas:** Capacidad de enviar Magic Packets bajo demanda (`pings/<user>`) para forzar fijaciones GPS frescas y disparar/desactivar alertas de prueba SOS (`sosActive`).

27. **Adaptación de Sincronización Web en Entornos Serverless / Vercel (`Cloud Sync: En vivo`):**
    - **Diagnóstico:** En hosting serverless como Vercel (`*.vercel.app`), no existe un proceso de servidor Node.js persistente para alojar endpoints custom de WebSockets (`/ws`). Esto causaba que la pastilla de estado mostrara permanentemente *"WS: Reconectando..."* con reintentos fallidos en bucle.
    - **Solución Implementada (`web/js/websocket.js` & `web/js/state.js`):**
      - Detección automática de entornos serverless/cloud (`vercel.app`, `web.app`, `firebaseapp.com`, `github.io`, `netlify.app`).
      - En entornos Serverless, la sincronización en tiempo real está 100% garantizada y gestionada por los WebSockets cloud nativos de **Firebase Firestore** y **Firebase Realtime Database (RTDB)**.
      - La UI muestra de forma transparente e inmediata `Cloud Sync: En vivo 🟢` con tooltip explicativo, anulando bucles de reconexión innecesarios en la nube mientras preserva el soporte de WebSockets locales en entornos de desarrollo Node (`localhost`).

30. **Gestión Web de Espíritus, Paridad de Temas y Optimización Zero-Flicker (Web & Vercel):**
    - **Paridad 100% de Temas y Paletas con la App Android:**
      - Sincronización exacta de temas (`Pixel Claro`, `Pixel Oscuro`, `Pixel Monocromático`) y familias de colores (`LIGHT_COLOR_FAMILIES`, `DARK_COLOR_FAMILIES`, `MONO_COLOR_FAMILIES`) con los valores hex y nombres definidos en `ProfileSettingsCompose.kt`.
      - Modificación bidireccional en Firestore (`users/<userId>`) con los campos `theme`, `lightColor`, `darkColor`, `useCustomBg`, `refreshRate`, etc., aplicando las variables CSS `--accent-color` y `--accent-glow` dinámicamente con máxima especificidad (`!important`) en el DOM.
    - **Eliminación Total de Parpadeos (Zero-Flicker DOM Updates):**
      - Se sustituyó la reconstrucción completa del espacio de trabajo (`container.innerHTML = ""`) por actualizaciones quirúrgicas en memoria (`updateSingleSpiritDOM` y `syncAllSpiritSlotsDOM`) tanto al interactuar con las tarjetas como al recibir eventos de snapshot de Firestore y WebSockets.
      - Supresión de filtros de rasterizado GPU (`filter: grayscale`, `brightness`) y saltos `translateY`/`scale` al hacer hover, logrando tarjetas estables con transiciones CSS suaves.
    - **Enrutamiento SPA y Prevención de Error 404 en Vercel:**
      - Configuración de reescrituras exhaustivas en `web/vercel.json` y `vercel.json` para soportar todas las rutas limpias (`/configuracion`, `/categorias`, `/edicion`, `/album`, `/coleccion`, `/radar`, `/normal`, `/edit`, `/db`), evitando errores 404 al recargar el navegador en Vercel.
    - **Ergonomía de Interfaz (Álbum, Búsqueda y HUD):**
      - Renombrado de la vista a **"📖 Álbum"**.
      - Rediseño de la barra de búsqueda (48px de altura, 15px de tipografía, atajos de teclado globales `/` y `Ctrl+K`, insignias de coincidencias en vivo y botón de limpieza).
      - Tarjetas HUD de estadísticas para Kevin, Ali y Ambos con barras de progreso animadas que reflejan el porcentaje de completitud.
      - Segmentación ergonómica de filtros principales y filtros de maestría con pulsaciones táctiles.

32. **Optimizaciones de Fluidez, Prevención de Saltos de Scroll, Colores de Espíritus y Renderizado en Web Álbum (Web & Vercel):**
    - **Resolución Definitiva del Salto de Scroll:** Corrección del reseteo a la parte superior (`scrollTop = 0`) al marcar espíritus obtenidos o con maestría. En vistas filtradas (`updateSingleSpiritDOM`), la tarjeta se remueve de forma individual del DOM sin destruir el contenedor general.
    - **Preservación y Restauración Atómica de Scroll (`renderWorkspace`):** Captura previa de `scrollTop` y `window.scrollY` antes de mutar el DOM y restauración inmediata tras `container.replaceChildren()`.
    - **Importación y Optimización de Imágenes Cloudinary (`optimizeCloudinaryUrl`):** Importación explícita en [`normal-view.js`](web/js/normal-view.js) para evitar caídas en tiempo de ejecución (`ReferenceError`) durante la construcción de las ranuras de espíritus.
    - **Normalización Exhaustiva de IDs y Colores de Estado (`isIdInList` & `buildNormalizedIdSet`):** Soporte bidireccional transparente para IDs con o sin cero a la izquierda (`"1"` vs `"01"`). Garantiza que las ranuras de espíritus reflejen fielmente el color azul (Kevin), rojo/rosa (Ali) o morado (Ambos) junto con las estrellas de maestría tanto desde Firestore como desde la caché local.
    - **Eliminación del Salto Visual y Parpadeo de Fuentes al Recargar:** Inyección de script anti-FOUC en `<head>` de todos los documentos HTML con lectura instantánea de `userTheme` y `userFont`, directiva `display=swap` en Google Fonts y reserva fija de barra de desplazamiento (`overflow-y: scroll`), erradicando cualquier salto de escala o tamaño de letra en recargas.
    - **Cero Cumulative Layout Shift (Zero CLS):** Declaración explícita de `width="88"` y `height="88"` junto con `aspect-ratio: 1/1` en todas las etiquetas `<img>` de espíritus y galería, garantizando cuadrículas fijas sin saltos de carga.
33. **Sincronización Total de Espíritus Temporada 2 (157 Espíritus), Restauración de Categorías y Optimización de Scroll/Layout (Web & App):**
    - **Catálogo Completo Temporada 2 (157 Espíritus en 27 Categorías):**
      - Integración de todas las variantes oficiales desde Fortnite.GG (IDs `132` a `157`), incluyendo las 19 variantes *Trick or Treat (Truco o Trato)* para las familias existentes y las 6 variantes completas de la nueva familia **Espíritu Basurero (Dumpster Dive)**.
      - Descarga local con headers de navegador y subida automatizada a Cloudinary en alta resolución (`spirits_s2/ic_spirit_s2_<id>.png`).
      - Actualización del documento maestro [espiritus.md](file:///home/kevshupp/Escritorio/Proyectos/Diario_alikevin/espiritus.md) y de las constantes en la app Android ([SpiritsCompose.kt](file:///home/kevshupp/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SpiritsCompose.kt)) y Web ([web/js/constants.js](file:///home/kevshupp/Escritorio/Proyectos/Diario_alikevin/web/js/constants.js)).
    - **Salvaguarda Estricta y Preservación de Datos de Usuario (Regla 7):**
      - Generación previa de copia de seguridad en `scripts/backups/`.
      - Actualización no destructiva vía REST API con updateMask en Firestore (`fortnite_spirits_s2/vínculo_único_123`), preservando intactos el 100% de los datos de usuario (`kevin_list` con 96, `ali_list` con 102, `kevin_mastery` con 43, `ali_mastery` con 42).
    - **Corrección y Alineación de Títulos y Nombres Personalizados:**
      - Restauración del orden de las 27 categorías según la organización del usuario (Mapache, Rex/Klombo, Agente/Jonesy, Pícaro/Aventurero, Erizo/Arbustín, Sonic, Tails, Shadow, Game Boy/8-Bits, Conejo, Rey/Victorioso, Táctico/Killswitch, Megaman, Caballero/Protector, Estanque, Científico/Rayos X, Onigiri, Oni/Exploratormentas, Blinky, Cumpleaños, Cash Bandicoot, Morgana, Sobreescudo, Calabaza/Impulso Aterrador, Ciervo, Vampiro, Basurero).
      - Mapeo limpio de `custom_categories` y `custom_names` en español para las nuevas variantes, eliminando sufijos erróneos o cruces de categorías.
    - **Resolución Definitiva de Fluidez en Scroll:**
      - Eliminación de `content-visibility: auto` y `contain-intrinsic-size: auto 200px` en `.category-card`, erradicando el recálculo dinámico de altura al desplazarse hacia arriba (*scroll jank*).
      - Supresión de capas artificiales de composición GPU (`will-change: scroll-position`, `contain: layout paint`, `transform: translateZ(0)` en ranuras) y ajuste de scroll nativo suave (`overscroll-behavior-y: auto`).
    - **Diseño Adaptativo con Grilla Auto-Fit:**
      - Transición a `grid-template-columns: repeat(auto-fit, minmax(136px, 1fr))` en `.spirit-slots-grid`, asegurando que las 6 tarjetas de cada categoría se expandan simétricamente ocupando el 100% del ancho del recuadro sin dejar espacios vacíos a la derecha en resoluciones amplias o medianas.
      - Ampliación de la tipografía de nombres a 13.5px / 31px de altura mínima con mejor contraste y saltos de línea ordenados.
