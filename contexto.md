# Diario Kevin & Ali (Diario Alikevin) - Contexto General del Proyecto

Este documento es el **centro de la documentación**: reúne lo general y lo que comparten la app y la web (propósito, stack, modelos de Firestore, despliegue e inventario de opciones). El detalle de cada parte está en su propio archivo.

## 🧭 Qué archivo leer según la tarea

| Si la tarea es sobre… | Lee primero | Contiene |
|:---|:---|:---|
| **La web** (`web/`, Vercel, álbum, temas web) | [web/web.md](web/web.md) | Estructura, temas y colores, rutas, caché, cartas, hitos web |
| **La app Android** (`app/`, mascotas, radar, minijuegos) | [app/app.md](app/app.md) | Archivos clave, mascotas, sincronización Drive, radar, rendimiento, backlog |
| **Espíritus** (catálogo o datos en Firestore) | [espiritus.md](espiritus.md) y la sección *I* de este archivo | Catálogo y reglas de salvaguarda |
| **Datos compartidos, tags o releases** | Este archivo | Modelos de Firestore y CI/CD |
| **Reglas de desarrollo** | [AGENTS.md](AGENTS.md) | Reglas obligatorias |

Si un cambio afecta a la app y a la web (por ejemplo, un campo de Firestore o una temporada nueva), lee y actualiza ambos archivos. Actualiza el archivo que corresponda cuando cambien la arquitectura o los flujos.

---

## 1. Propósito y Visión General
**Diario** es una aplicación móvil nativa para Android diseñada exclusivamente para parejas. Resuelve la falta de espacios íntimos y compartidos al digitalizar recuerdos de amor mediante pilares funcionales integrados:
1. **Diario Compartido:** Envío de cartas y mensajes con imágenes con paginación progresiva de 5 en 5, likes, visualización en grilla (álbum) y subidas directas a Cloudinary.
2. **Calendario y Recetas:** Un calendario común para recordar aniversarios/citas (con alertas automáticas) y un recetario culinario de cocina compartido con fotos e ingredientes.
3. **Mascota Virtual (Thor & Cuky):** Sistema de gamificación multiespecie en formato pixel-art en el que un gato (Thor) o una gallinita (Cuky) reacciona a la interacción diaria de la pareja con estadísticas, niveles, necesidades y guardarropas 100% independientes, acumulando Puntos de Amor, subiendo de nivel y desbloqueando ropa/fondos temáticos en una tienda interactiva.
4. **Sincronización Local-Nube (Google Drive)**: Respaldo y replicación automática bidireccional en segundo plano de la carpeta de fotos local seleccionada por cada usuario mediante `SyncDriveWorker` (Foreground Service), sincronizando incluso eliminaciones entre dispositivos con tombstones.
5. **Ficha Médica de Emergencia (Datos Vitales)**: Módulo interactivo dentro de la pantalla de Perfil con sincronización Firestore en tiempo real (`medical_records/<coupleId>`) que permite consultar datos médicos (grupo sanguíneo, alergias, enfermedades, seguro, remedios activos de la app y contacto de emergencia con marcación `ACTION_DIAL`) en una vista limpia de tarjetas por defecto tanto para tu ficha como para la de tu pareja, incluyendo un botón destacado `✏️ EDITAR` para modificar la información en cualquier momento.
6. **Horario de Clases Compartido (Misc -> Horario)**: Módulo interactivo dentro del menú Misceláneo con sincronización Firestore en tiempo real (`schedules/<coupleId>`). Permite registrar, editar y consultar clases de Kevin, Ali o Ambos de Lunes a Viernes en una grilla retro por horas de 145dp con arquitectura de superposición unificada (Overlay), posicionamiento proporcional exacto por minuto, tarjetas continuas sin líneas de corte, margen de horas automático, soporte de solapamientos simultáneos y diseño adaptable para rotación horizontal (Landscape).
7. **Gestión de Medicamentos de Rutina (Misc -> Medicamentos)**: Módulo interactivo para programar tomas diarias/periódicas de remedios con alarmas exactas gestionadas vía `AlarmManager` y notificación push persistente (`MedicationReceiver`).
8. **Lista de Anime Compartida (Misc -> Anime)**: Dashboard interactivo para llevar el registro de animes vistos o por ver juntos, episodios actuales, calificación y estado de emisión.
9. **Checklist de Espíritus Fortnite & Web de Gestión (Misc -> Espíritus / Web)**: Coleccionable interactivo de 117 espíritus con maestrías, categorías y renombrado en tiempo real, sincronizado mediante Firestore (`fortnite_spirits/<coupleId>`) con una Web de Gestión externa en Node.js / Vercel Serverless.
10. **Thor Radar (Ubicación en Tiempo Real, Brújula & Geocercas - Misc -> Thor Radar)**: Módulo interactivo estilo Life360 con estética retro. Integra mapa OpenStreetMap con Osmdroid, marcadores pixelados personalizados, brújula de amor giratoria con distancia (km/m) y rumbo, monitoreo de batería (%), velocidad y actividad (caminando/auto/reposo), zonas seguras (geocercas con radio ajustable), historial del día, botón de pánico SOS con notificación push FCM y servicio en background (`ThorRadarService` / `ThorRadarManager`).

---

## 2. Stack Tecnológico y Arquitectura

- **Plataforma / Lenguajes:** Android Nativo.
  - **Kotlin:** Utilizado en el 100% del código fuente (Activities, ViewModels, pantallas de Jetpack Compose, Workers y utilidades en segundo plano).
- **UI Framework:**
  - **Jetpack Compose:** Sistema declarativo moderno utilizado en la totalidad de las pantallas (cartas, álbum, calendario, recetas, ficha médica, medicamentos, horario, espíritus, anime, perfil y configuración de sincronización).
  - **Vanilla XML / ViewBinding:** En desuso, restringido a ciertos componentes legados y layouts de Widgets de pantalla de inicio.
- **Base de Datos y Backend:**
  - **Firebase Auth:** Gestión de inicio de sesión de los usuarios.
  - **Cloud Firestore:** Almacenamiento NoSQL en tiempo real con persistencia offline integrada (caching local SQLite automática) para cartas, recetas, eventos, fichas médicas, medicamentos, animes, horarios de clases e información de la mascota.
  - **Firebase Cloud Messaging (FCM):** Notificaciones push utilizando la API v1 mediante autenticación OAuth2.
- **Servicios Externos / APIs:**
  - **Google Drive API (v3):** Respaldo directo en la nube en una carpeta oculta (`DiarioAliKevin_Album`).
  - **Cloudinary:** Hosting cloud multimedia. Las fotos de las cartas se suben de forma firmada directamente a Cloudinary.
  - **GitHub API:** Localizada en [UpdateManager.kt](app/src/main/java/calendario/kevshupp/diariokevinali/UpdateManager.kt) para verificar actualizaciones del APK e instalarlas automáticamente.
- **Colección de Espíritus / Coleccionables:**
  - **Checklist de Espíritus (Temporadas 1, 2 y 3):** Listado interactivo en el juego y en la web sincronizado mediante Firestore (`fortnite_spirits` para T1, `fortnite_spirits_s2` para T2 y `fortnite_spirits_s3` para T3) y respaldado por Cloudinary. Toda la lista detallada y catálogo de enlaces oficiales a Fortnite.GG reside en [espiritus.md](espiritus.md). Cuenta con estricta salvaguarda de preservación de datos (`kevin_list`, `ali_list`, `kevin_mastery`, `ali_mastery`) y copias de seguridad automáticas (`scripts/backup_firestore_spirits.py`).
- **Estilos y UI:**
  - Estética inmersiva **Retro Pixel-Art de 8 y 16 bits**.
  - Tipografía pixelada `vt323` importada globalmente.
  - Soporte a tres temas visuales dinámicos: **Pixel Claro** (crema y chocolate), **Pixel Oscuro** (gris profundo y neón rosa) y **Pixel Monocromático** (blanco y negro puro).
  - **Sincronización de Tema Web-App:** El selector de temas en la pestaña *Configuración* de la web sincroniza en tiempo real el campo `theme` dentro del documento `users/<userId>` de Firestore. Al cambiar el tema desde la web o desde la app, ambos entornos adaptan sus colores e interfaz al instante.

---

## 4. Modelos de Datos y Entidades en Firestore

### A. Mascota (`pets/<coupleId>`) - `Pet.kt`
- **Mascota Activa:** `petType: String` (`"thor"` o `"cuky"`).
- **Estadísticas de Thor (Gatito):**
  - `name: String` ("Thor"), `customName: String?`
  - `happiness: Int` (0 a 100), `hunger: Int` (0 a 100), `cleanliness: Int` (0 a 100)
  - `level: Int`, `experience: Int` (0 a 100), `streakDays: Int`, `lastInteraction: Long`
  - `isSleeping: Boolean`, `sleepStartTimestamp: Long?`
  - `equippedAccessory: String?`, `unlockedAccessories: List<String>`
  - `equippedBackground: String?`, `unlockedBackgrounds: List<String>`
- **Estadísticas de Cuky (Gallinita Café) - 100% Independientes:**
  - `cukyName: String` ("Cuky"), `cukyCustomName: String?`
  - `cukyHappiness: Int` (0 a 100), `cukyHunger: Int` (0 a 100), `cukyCleanliness: Int` (0 a 100)
  - `cukyLevel: Int`, `cukyExperience: Int` (0 a 100), `cukyStreakDays: Int`, `cukyLastInteraction: Long`
  - `cukyIsSleeping: Boolean`, `cukySleepStartTimestamp: Long?`
  - `cukyEquippedAccessory: String?`, `cukyUnlockedAccessories: List<String>`
  - `cukyEquippedBackground: String?`, `cukyUnlockedBackgrounds: List<String>`
- **Monedas Globales de Pareja:** `lovePoints: Int` (Monedas compartidas para la tienda de ambas mascotas).
- **Ranking de Cuidadores por Mascota:**
  - `carePointsKevinThor: Int`, `carePointsAliThor: Int`, `carePointsKevinCuky: Int`, `carePointsAliCuky: Int`
  - Contadores individuales de alimentación (`feedCount...`), baño (`bathCount...`) y juego (`playCount...`) por cuidador y mascota.
- **Getters Dinámicos Obligatorios en UI:** `pet.getActiveName()`, `pet.getActiveLevel()`, `pet.getActiveHappiness()`, `pet.getActiveExperience()`, `pet.getActiveHunger()`, `pet.getActiveCleanliness()`, `pet.getActiveStreak()`, `pet.getActiveIsSleeping()`, `pet.getActiveEquippedAccessory()`, `pet.getActiveEquippedBackground()`, etc. (Garantizan que ninguna pantalla lea accidentalmente el estado de Thor al estar Cuky seleccionada).

### B. Cartas (`messages/<messageId>`) - `Message.kt`
- `messageId: String?` (Clave en Firestore).
- `authorId: String?` / `authorName: String?` (Detalles del creador).
- `content: String?` (Texto de la carta).
- `imageUrls: MutableList<String>?` (URLs de Cloudinary).
- `timestamp: Long` (Fecha de publicación).
- `liked: Boolean` (Estado de me gusta / favorito).
  - **Reglas de likes:** solo la pareja puede dar o quitar like; en las cartas propias el corazón está deshabilitado (`enabled = message.authorId != currentUserId` en `MessageCard`) pero se ve rosado si la pareja dio like. `MainActivity` también ignora el toque en cartas propias y envía la notificación push "¡le encantó!" solo al dar like.
  - **No mutar `Message` en sitio:** `toggleLikeMessage` debe crear una copia (`it.copy(liked = ...)`) sin modificar el objeto original. Mutarlo hace que la lista nueva sea `equals` a la anterior y el `MutableState` del feed no recompone (bug corregido en v1.7.82).
  - **Texto de carta (`AndroidView` + `TextView`) en `LazyColumn`:** el HTML se aplica de inmediato dentro de un `BoxWithConstraints`, pasando `constraints.maxWidth` a `LetterImageGetter(textView, knownWidth)`. No usar `textView.post { ... }`: al reciclar el ítem Compose no vuelve a medir y la carta quedaba cortada a una línea al volver a subir (corregido en v1.7.88).
  - **Listeners y caché:** los listeners de Firestore viven de `onStart` (`startAllListeners`) a `onStop` (`stopAllListeners`); `MainActivity` ya no los reinicia en `onPause`/`onResume`, así el feed muestra las cartas en caché sin volver a pedirlas cada vez (v1.7.88).
- `type: String?` (`MESSAGE` o `ALBUM`).

### C. Horario de Clases (`schedules/<coupleId>`) - `ScheduleCompose.kt`
- `classes: List<Map<String, Any>>`
  - `id: String`, `name: String`, `teacher: String`, `room: String`
  - `dayOfWeek: Int` (1: Lunes, 2: Martes, 3: Miércoles, 4: Jueves, 5: Viernes)
  - `startHour: Int`, `startMinute: Int`, `endHour: Int`, `endMinute: Int`
  - `owner: String` (`"kevin"`, `"ali"`, `"both"`)
  - `colorHex: String`

### D. Ficha Médica (`medical_records/<coupleId>`) - `MedicalCompose.kt`
- `bloodType: String`, `allergies: String`, `conditions: String`
- `dailyMeds: String`, `insurance: String`, `emergencyContactName: String`, `emergencyContactPhone: String`

### E. Medicamentos (`medications/<coupleId>`) - `MedsCompose.kt`
- `name: String`, `dosage: String`, `frequencyHours: Int`, `nextTakeTimestamp: Long`, `owner: String`

### F. Anime (`anime_list/<coupleId>`) - `AnimeCompose.kt`
- `title: String`, `episodesWatched: Int`, `totalEpisodes: Int`, `rating: Float`, `status: String` (`"WATCHING"`, `"COMPLETED"`, `"PLAN_TO_WATCH"`)

### F2. Bitácora de Ánimo (`mood_logs/<coupleId>/entries/<id>`) - `MoodLogCompose.kt`
- `who: String` (`"Ali"`/`"Kevin"`), `mood: String` (emoji), `moodLabel: String`, `intensity: Int` (1-5), `note: String`, `timestamp: Long`, `dayKey: String` (`yyyy-MM-dd`), `author: String` (userName de quien anotó), `isGeneral: Boolean`
- Registros **generales** (casilla *Sin fecha*): `dayKey = "general"`, se muestran en la pestaña 📝 GENERAL sin fecha ni hora; el `timestamp` real de creación solo se usa para ordenar (más recientes primero).
- Listener solo sobre `whereEqualTo("dayKey", día visible)` (lecturas acotadas al día, sin índice compuesto); 1 escritura por registro. Solo el autor puede borrar su registro.

### G. Ubicación y Geocercas de Thor Radar (`locations/<coupleId>`) - `ThorRadarCompose.kt`
- **Usuarios (`locations/<coupleId>/users/<kevin|ali>`)**:
  - `userId: String`, `userName: String`, `profileImageUrl: String` (Foto de perfil del usuario cargada con Coil en marcadores)
  - `latitude: Double`, `longitude: Double`, `accuracy: Float`, `speedKmh: Float`
  - `batteryLevel: Int`, `isCharging: Boolean`, `activity: String` (`"STILL"`, `"WALKING"`, `"IN_VEHICLE"`)
  - `currentZone: String`, `address: String`, `timestamp: Long`, `isSharing: Boolean`, `sosActive: Boolean`, `sosTimestamp: Long`
- **Zonas Seguras (`locations/<coupleId>/zones/<zoneId>`)**:
  - `id: String`, `name: String`, `icon: String`, `latitude: Double`, `longitude: Double`, `radiusMeters: Float` (30m a 800m ajustable con slider y chips rápidos 50m, 100m, 200m, 350m, 500m), `addedBy: String`
  - **Selector de Zonas con Retículo Central Fijo**: Modelo estilo Uber / Life360 donde el mapa se desplaza suavemente bajo una mira central con el emoji del lugar y `CenterZoneOverlay` dibuja el círculo de geocerca en tiempo real en la GPU a 60/120 FPS sin recomposiciones ni saltos de puntero.
  - **Edición Completa**: Soporte para crear y editar zonas existentes (cambiar nombre, icono, radio y ubicación) mediante el botón `✏️ EDITAR`.
  - **Filtrado de Notificaciones Push FCM**: `MyFirebaseMessagingService` y `ThorRadarManager` envían y procesan Data-Only messages filtrando para que solo lleguen alertas de entrada/salida y SOS emitidas por la pareja, suprimiendo las autogeneradas.

### H. Metadatos de Sincronización Drive (`pets/<coupleId>/drive_sync_metadata/<docId>`)
- `idLocal: String`, `idDrive: String`, `nombreArchivo: String`, `uriLocal: String`, `md5Checksum: String`, `fechaModificacion: Long`, `sincronizadoPor: String`, `eliminado: Boolean`.

### I. Colección de Espíritus Fortnite (`fortnite_spirits/<coupleId>` para Temporada 1 / `fortnite_spirits_s2/<coupleId>` para Temporada 2 / `fortnite_spirits_s3/<coupleId>` para Temporada 3)
- **Documento Maestro de Referencia:** Ver archivo dedicado [espiritus.md](espiritus.md) para el desglose 1:1 de nombres, categorías, IDs y enlaces directos a `fortnite.gg`.
- **🛡️ REGLA ESTRICTA DE SALVAGUARDA DE DATOS Y COPIAS DE SEGURIDAD (OBLIGATORIA):**
  - **Prohibición Total de Sobreescritura Destructiva:** Queda terminantemente prohibido vaciar o sobreescribir las listas de selección y maestrías de los usuarios (`kevin_list`, `ali_list`, `kevin_mastery`, `ali_mastery`). Cualquier actualización a nivel de esquema, categorías o nombres debe preservar intactos los datos de usuario mediante operaciones no destructivas (`SetOptions.merge()` o lectura previa con inyección obligatoria de las listas existentes).
  - **Backup Previo Obligatorio:** Antes de ejecutar cualquier script que interactúe o modifique la colección de espíritus en Firestore, es **OBLIGATORIO** ejecutar:
    ```bash
    python3 scripts/backup_firestore_spirits.py
    ```
    Dicho script guarda copias timestamped e inmutables de `fortnite_spirits` y `fortnite_spirits_s2` en `scripts/backups/`.
- **Soporte Multitemporada:**
  - **Temporada 1**: Contiene la colección original de espíritus (1 a 141), categorías y estado histórico de checks y maestrías.
  - **Temporada 2**: Espíritus de Fortnite Chapter 7 Season 4 "Override" (174 según `espiritus.md`), con registro independiente de checks y maestrías.
  - **Temporada 3 (Por defecto)**: 35 espíritus de la sección "C7 MS" de Fortnite.GG (IDs `01` a `35`, 13 familias, tipos Normal/Afortunado/Maldito), con imágenes en Cloudinary `spirits_s3/ic_spirit_s3_<id>.png` y registro independiente. Se crea con `scripts/create_s3_spirits.py`, que lee la página guardada desde el navegador porque `fortnite.gg/sprites` responde 403 a los scripts y rechaza la escritura si el documento ya existe. Selector: píldora de 3 botones en la web; en la app, el botón `T<n>` rota entre las tres.
- `schema_version: Int` (Versión 4).
- `categories: List<SpiritCategory>` / `Map`
- `spirits_list: List<String>`
- `kevin_list: List<String>` / `ali_list: List<String>`
- `kevin_mastery: List<String>` / `ali_mastery: List<String>`
- `custom_names: Map<String, String>`, `custom_categories: Map<String, String>`, `custom_images: Map<String, String>`, `spirit_types: List<SpiritType>`.

---

## 7. Despliegue Automatizado y Pruebas Multidispositivo

### CI/CD en GitHub Actions & Regla de Versionado
- **Repo de releases público (octubre 2026):** el código vive en `KevshuppD/Diario_alikevin` (puede ser privado) y el workflow publica el APK en el repo público `KevshuppD/Diario_alikevin-releases` con el secret `RELEASES_REPO_TOKEN` (PAT fine-grained con *Contents: Read and write* solo sobre ese repo). `UpdateManager.kt` y `send_update_notification.py` apuntan a ese repo. Las versiones ≤ v1.7.95 consultan el repo de código: hay que instalar una versión nueva en ambos teléfonos **antes** de hacer privado el repo de código.
- **Incremento de Versión Obligatorio al Crear Tag (`versionCode` & `versionName`):**
  - **REGLA ESTRICTA**: Siempre que el usuario solicite subir, crear o actualizar un `tag` (ej. "sube tag", "crea tag", "actualiza versión"), es **OBLIGATORIO** incrementar previamente `versionCode` (+1) y `versionName` (ej: `1.7.76` -> `1.7.77`) en [app/build.gradle.kts](app/build.gradle.kts) antes de hacer el commit y generar el tag en git.
  - Esto garantiza que Android y el instalador in-app reconozcan el APK resultante como una actualización superior y no rechacen o ignoren la instalación por tener el mismo `versionCode`.
- **Creación de Tag:** Empujar el tag `v<versionName>` a `master` dispara el workflow [`.github/workflows/android.yml`](.github/workflows/android.yml), el cual compila, firma y publica `app-release.apk` en GitHub Releases.
- **Notas de la versión (`changelog/v<versionName>.md`):** Si existe ese archivo, el workflow lo usa como cuerpo del release y de la notificación FCM (si no, usa el mensaje del tag/commit). Se escribe en Markdown pensado para el usuario (viñetas `- **Título:** explicación`, sub-viñetas con 2 espacios); `UpdateDialogCompose.formatChangelog()` lo muestra con negritas y viñetas `•`/`◦`.
- **Notificación Push Push Automática (FCM v1):** Tras crear la Release, GitHub Actions ejecuta [`.github/scripts/send_update_notification.py`](.github/scripts/send_update_notification.py) usando el `service-account.json` para emitir un push instantáneo al topic `diario_app_updates`. Todos los dispositivos con la app instalada reciben la alerta de actualización en tiempo real con enlace directo de descarga.


### Inventario de Opciones Visibles (no eliminar sin autorización — ver regla 8 de AGENTS.md)
- **Configuración → Diseño y Tema:** selector de tema, paleta de colores por familias, casilla *Fondo con tono personalizado* (`useCustomBg`), casilla *Mostrar barra superior ('Nuestro Diario')* (`showTopBar`, se guarda en `DiarioPrefs` y en Firestore y la aplica `MainActivity.updateTopBarVisibility()`).
- **Mascota (`PetMenuDialog`):** pestañas INFO, RANK 🏆 (15 niveles), ESTILO 👑, COMIDA, VALES 🎟️ (ruleta a la distancia o ruleta Juntos según el radar) y ⚙️.
- **Avanzado y Admin:** reiniciar ranking, minijuegos, mascotas y vales de las ruletas; editor de premios Juntos.
- **Cartas:** like / quitar like (solo en cartas de la pareja), menú de mantener presionado (solo en cartas propias, por pedido del usuario, 2026-10-31), borrar carta propia, paginación de 5 en 5.
- **Web → Tipos y Categorías:** botón *🪄 Normalizar Nombres* (respalda, renumera IDs y normaliza nombres) y botón *⚠️ Restablecer Respaldo* (contraseña `123`).
- **Calendario:** cambiar de mes, Hoy, elegir día, agendar, editar tocando la cita, borrar con confirmación, 5 repeticiones.
- **Álbum:** pestañas Recuerdos y General, añadir recuerdo, abrir, editar y eliminar propios, recargar, buscar, 4 órdenes, 4 filtros, 2 a 6 columnas.
- **Misceláneo:** Espíritus, Anime, Web Gestión, Medicamentos, Horario, Thor Radar y 📔 Bitácora Ánimo (pestañas 📅 POR DÍA / 📝 GENERAL, selector de día ◀▶, filtro Ali/Kevin/Ambos, registros agrupados por hora, ➕ registrar, ✕ borrar propio).
- **Espíritus → Celebración de temporada (`SpiritCelebrationCompose.kt`):** botón sobre la colección, oculto hasta que Kevin y Ali tengan maestría en todos los espíritus de la temporada visible (el recuadro bloqueado y el marcador con VS y líder se quitaron en noviembre de 2026 por pedido del usuario). Genera una imagen de 1080×1350 con Canvas (*Fuegos*, *Trofeo* o *Arcade*) y permite Guardar (Imágenes/DiarioAliKevin) o Compartir. Solo lee datos: no escribe en Firestore. La fecha de la imagen es la del día en que se genera.
- **Historial:** la casilla de barra superior se perdió por accidente en el commit `586863a` (v1.7.54, reorganización de Configuración) y se restauró en v1.7.82. El cambio de nombre de la mascota desde `PetMenuDialog` se quitó en `70c004f` (v1.7.52); el parámetro `onUpdateName` sigue sin usarse.
