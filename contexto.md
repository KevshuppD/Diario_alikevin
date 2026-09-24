# Diario Kevin & Ali (Diario Alikevin) - Contexto General del Proyecto

Este documento sirve como la **Fuente Única de Verdad (Single Source of Truth)** para el proyecto Android **Diario**, diseñado como un espacio privado, interactivo y gamificado para parejas a la distancia. Proporciona una explicación detallada de la arquitectura, base de datos (Firestore), flujos de trabajo clave, guías de desarrollo, estructura de código y reglas de negocio para que cualquier desarrollador o inteligencia artificial pueda entender el proyecto al 100% al instante.

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
  - **GitHub API:** Localizada en [UpdateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/UpdateManager.kt) para verificar actualizaciones del APK e instalarlas automáticamente.
- **Colección de Espíritus / Coleccionables:**
  - **Checklist de Espíritus:** Un listado interactivo en el juego compuesto por **117 espíritus**. Su registro en código reside en `SpiritsCompose.kt` y `MiscCompose.kt` y sus activos de imagen pixel-art están almacenados en los recursos drawables y Cloudinary. El **Modo Edición** permite renombrar espíritus y categorías, mover espíritus a diferentes categorías y eliminar tanto espíritus como categorías de forma compartida guardando los cambios en Firestore (`fortnite_spirits/<coupleId>`).
- **Estilos y UI:**
  - Estética inmersiva **Retro Pixel-Art de 8 y 16 bits**.
  - Tipografía pixelada `vt323` importada globalmente.
  - Soporte a tres temas visuales dinámicos: **Pixel Claro** (crema y chocolate), **Pixel Oscuro** (gris profundo y neón rosa) y **Pixel Monocromático** (blanco y negro puro).
  - **Sincronización de Tema Web-App:** El selector de temas en la pestaña *Configuración* de la web sincroniza en tiempo real el campo `theme` dentro del documento `users/<userId>` de Firestore. Al cambiar el tema desde la web o desde la app, ambos entornos adaptan sus colores e interfaz al instante.

---

## 3. Estructura del Proyecto y Archivos Clave

El código fuente está localizado en `app/src/main/java/calendario/kevshupp/diariokevinali/`.

### 📁 Inicialización e Infraestructura
- [DiarioApp.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DiarioApp.kt): Punto de entrada en Kotlin. Configura las instancias globales de Cloudinary, WorkManager, Firebase y Coil.
- [MainActivity.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.kt): Contenedor principal en Kotlin (`singleTop`). Orquesta las vistas de Compose observando los estados del ViewModel, enrutador universal de intents de notificación (`handleUpdateIntent` / `navigateToClickType`) y gestión limpia del backstack de fragmentos.
- [LoginActivity.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/LoginActivity.kt): Gestión del login y asociación del `coupleId`, con reenvío transparente de extras de notificación al redirigir a MainActivity.
- [MainViewModel.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainViewModel.kt): ViewModel central en Kotlin. Administra el estado global, listeners de Firestore en tiempo real, alarmas de calendario y toda la lógica de interacción/decay del pet Thor.

### 📁 Sincronización en Segundo Plano y Gestión de Archivos
- [SyncDriveWorker.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncDriveWorker.kt): Trabajador de primer plano (`CoroutineWorker` promovido a Foreground Service). Maneja la lógica de subir/descargar fotos pendientes de forma optimizada y control de borrados bidireccionales con notificación navegable.
- [SyncScheduler.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncScheduler.kt): Orquestador de WorkManager. Agenda sincronizaciones periódicas (con restricciones de red Wi-Fi y carga eléctrica) o inmediatas bajo demanda.
- [DuplicateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DuplicateManager.kt): Gestor de detección y limpieza de imágenes duplicadas mediante pre-filtrado por tamaño y comparación MD5.
- [ThorRadarManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorRadarManager.kt) & [ThorRadarService.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorRadarService.kt): Gestor y Foreground Service (`location`) para rastreo en tiempo real de alta frecuencia (bucle de 4s en primer plano mientras se visualiza el mapa, 10s en segundo plano y 30s en modo ahorro de batería), cálculo de distancias Haversine, rumbo (bearing), batería, velocidad, geocodificación inversa en tiempo real, detección de zonas seguras con sincronización y actualización inmediata de nombres/emojis editados en notificaciones de salida/llegada, y alertas SOS navegables (`radar`/`sos`).

### 📁 Notificaciones y Widgets
- [MyFirebaseMessagingService.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MyFirebaseMessagingService.kt): Receptor FCM con resolución exhaustiva de destinos (`click_type`), forward de datos completos en extras e inferencia de rutas según contenido.
- [PetCareWorker.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/PetCareWorker.kt): Worker periódico de cuidado de Thor que despacha alertas locales navegables directo al diálogo de la mascota.
- [NotificationReceiver.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/NotificationReceiver.kt): BroadcastReceiver para alarmas de citas de calendario con PendingIntent navegable directo a la vista de Calendario.
- [MedicationAlarmScheduler.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MedicationAlarmScheduler.kt) & [MedicationReceiver.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MedicationReceiver.kt): Sistema de alarmas exactas para recordatorios de medicamentos con redirección directa a la sección de Medicamentos en Misceláneos.
- [ThorWidgetProvider.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorWidgetProvider.kt): Widget de escritorio que dibuja el estado actual de Thor y sus accesorios equipados.
- [LastMessageWidget.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/LastMessageWidget.kt) / [LastMessageLargeWidget.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/LastMessageLargeWidget.kt): Widgets de escritorio con vista previa de la última carta recibida de la pareja.

### 📁 Actualizaciones Automáticas
- [UpdateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/UpdateManager.kt) & [UpdateWorker.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/UpdateWorker.kt): Consulta de la API de GitHub Releases con modelo `AppUpdateInfo`, descarga gestionada y soporte de cancelación.
- [UpdateDialogCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/UpdateDialogCompose.kt): Diálogo modal estético Retro Pixel-Art con comparador de versiones, notas de lanzamiento scrollables, barra de progreso pixelada en tiempo real y botones ergonómicos.
- [send_update_notification.py](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/.github/scripts/send_update_notification.py): Script en GitHub Actions que despacha notificación push FCM v1 al topic `diario_app_updates` inmediatamente tras compilar y publicar el APK Release.

### 📁 Pantallas en Jetpack Compose (`compose/`)
- [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt): Feed principal con paginación de 5 en 5 cartas, tarjeta de mascota adaptativa (`PetCard` para Thor / Cuky) con animaciones en Draw Phase (`graphicsLayer { ... }`), getters dinámicos (`getActive...()`), estado de racha y diálogo de confirmación de borrado.
- [PetDialogCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/PetDialogCompose.kt): Diálogo interactivo principal de mascotas (**Thor & Cuky**) con selector switchable, habitación 2D / gallinero, animaciones de baño y pelota adaptadas a cada especie.
- [PetShopCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/PetShopCompose.kt): Componentes de tienda de accesorios/ropa y compras de comida para la mascota activa.
- [PetMinigamesSelectorCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/PetMinigamesSelectorCompose.kt): Selector modal de minijuegos con estados de recompensa diaria y récord.
- [PetRankingCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/PetRankingCompose.kt): Vista detallada del Ranking de Cuidadores (Global, Thor, Cuky), amorómetro, medallero y desglose de puntos.
- [MemoryGameCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MemoryGameCompose.kt): Minijuego Retro Memory (Juego de Memoria con cartas pixel-art de los accesorios de la mascota activa).
- [MessageEditorCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageEditorCompose.kt): Editor y redactor de cartas con selección multimedia y subida directa a Cloudinary.
- [AlbumCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AlbumCompose.kt): Grilla de fotos retro con filtros por fecha, visor de pantalla completa e información del archivo.
- [SettingsSyncCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SettingsSyncCompose.kt): Interfaz retro para vincular Google Drive, selector de líneas paralelas de subida (1 a 5) y contadores dinámicos.
- [ProfileSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ProfileSettingsCompose.kt): Pantalla de Configuración principal (menú de navegación a Diseño y Tema, Alertas, Sincronización, Almacenamiento, Sistema, Avanzado y Admin y Limpiador de Duplicados).
- [ProfileScreenCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ProfileScreenCompose.kt): Editor del perfil de la pareja (Kevin & Ali) con cálculo y contador dinámico de tiempo juntos, fotos de perfil y diálogo de aniversario.
- [AdminSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AdminSettingsCompose.kt): Panel de administración integrado y protegido por contraseña para monitoreo de cuotas de Firestore (Spark Plan) y herramientas de reinicio de estadísticas de minijuegos, ranking de cuidadores y mascotas.
- [AdvancedSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AdvancedSettingsCompose.kt): Vista unificada de Avanzado y Admin que integra el diagnóstico de conexiones en tiempo real (Google Drive y Firestore) con el Panel de Administrador.
- [DuplicateRemoverCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/DuplicateRemoverCompose.kt): Limpiador y detector de fotos duplicadas.
- [MedicalCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MedicalCompose.kt): Ficha médica de emergencia con grupo sanguíneo, alergias, seguros y llamadas directas.
- [MedsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MedsCompose.kt): Gestión e historial de la toma de remedios y medicamentos de la pareja.
- [AnimeCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AnimeCompose.kt): Dashboard interactivo de animes compartidos (vistos, en emisión, pendientes).
- [SpiritsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SpiritsCompose.kt): Checklist de 117 espíritus de Fortnite, renombrados, categorías y variantes.
- [RecipeCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RecipeCompose.kt) & [RecipeDetailCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RecipeDetailCompose.kt): Libro de recetas de cocina compartido.
- [CalendarCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/CalendarCompose.kt): Vista mensual de citas y eventos de la pareja.
- [ScheduleCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ScheduleCompose.kt): Grilla de Horario de Clases compartido de Lunes a Viernes con superposición Overlay, tarjetas de 145dp, cálculo proporcional y soporte horizontal.
- [FlappyThorCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/FlappyThorCompose.kt): Minijuego arcade retro Flappy Pet adaptativo (`FLAPPY THOR` / `FLAPPY CUKY`) con selector de modo (Pantalla Completa / Consola Pocket), matrices de dibujo estáticas a nivel superior (cero recolección de basura GC), sprites procedurales adaptados por mascota, física delta-time a 60/90/120 FPS, corazones y recompensas.
- [SnakeGameCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SnakeGameCompose.kt): Minijuego clásico La Serpiente adaptativo (`THOR SNAKE` / `CUKY SNAKE`) con selector de modo (Pantalla Completa con Swipe y D-PAD ergonómico / Consola Pocket), efectos de sonido y puntuación.
- [ThorRadarCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ThorRadarCompose.kt): Pantalla principal del radar (`ThorRadarScreen`), pestañas de navegación, estado reactivo del GPS y orquestación general.
- [RadarMapCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RadarMapCompose.kt): Vista de mapa interactivo satelital Osmdroid (`RadarMapView`), tarjeta en vivo de la pareja (`PartnerLiveCard`), botones flotantes y diálogo de emergencia SOS (`SosCountdownDialog`).
- [RadarZonesCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RadarZonesCompose.kt): Pestaña de Zonas Seguras (`RadarZonesView`), diálogo modal completo para agregar/editar zonas con geocodificación inversa y retículo de cobertura en tiempo real (`AddEditZoneDialog` y `CenterZoneOverlay`).
- [RadarSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RadarSettingsCompose.kt): Pestaña de configuración de radar (`RadarSettingsView`), tarjeta maestra de encendido/apagado, ahorro de batería, diagnóstico GPS y diálogo del asistente de permisos (`RadarSetupWizardDialog`).
- [RadarCompassCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RadarCompassCompose.kt): Brújula de amor giratoria (`RadarCompassView`) con cálculo de rumbo y distancia exacta.
- [MiscCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MiscCompose.kt): Menú principal misceláneo con acceso a Espíritus, Anime, Web de Gestión, Medicamentos, Horario y Thor Radar.

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

### I. Colección de Espíritus Fortnite (`fortnite_spirits/<coupleId>` para Temporada 1 / `fortnite_spirits_s2/<coupleId>` para Temporada 2)
- **Soporte Multitemporada:**
  - **Temporada 1**: Contiene la colección original de espíritus (1 a 141), categorías y estado histórico de checks y maestrías.
  - **Temporada 2 (Por defecto)**: Colección activa para los nuevos espíritus, variantes y categorías creadas en la Web de Gestión con registro independiente de checks y maestrías.
- `schema_version: Int` (Versión 4).
- `categories: List<SpiritCategory>` / `Map`
- `spirits_list: List<String>`
- `kevin_list: List<String>` / `ali_list: List<String>`
- `kevin_mastery: List<String>` / `ali_mastery: List<String>`
- `custom_names: Map<String, String>`, `custom_categories: Map<String, String>`, `custom_images: Map<String, String>`, `spirit_types: List<SpiritType>`.

---

## 5. El Sistema de Gamificación de Mascotas Virtuales ("Thor & Cuky")

```mermaid
graph TD
    User([Interacciones de Usuario]) -->|Enviar Mensajes / Abrir App| XP[+10 XP Mascota Activa]
    User -->|Alimentar / Bañar / Pelota| Care[+Puntos de Cuidado Individuales +EXP/Felicidad]
    User -->|Primera Partida de Minijuego Diaria| MiniReward[Puntos de Amor + EXP Diaria]
    User -->|Partidas Posteriores en el Día| FreePlay[Modo Libre / Felicidad + Diversión Ilimitada]
    XP -->|Cada 100 XP| LevelUp[Subir de Nivel +50 Puntos de Amor]
    Care -->|Ranking Cuidadores| Rank[Top Cuidadores: Kevin vs Ali por Mascota]
    LP[Puntos de Amor Globales] -->|Comprar Accesorios / Fondos| Shop[Tienda de Mascotas]
    Shop -->|Desbloquear| Equip[Equipar Ropa / Fondos Independientes]
    Time([Transcurso del Tiempo]) -->|Cada 24 horas| Decay[-20% Felicidad / Hambre / Higiene]
```

- **Arquitectura de Mascotas 100% Independiente:**
  - **Thor (Gatito)** y **Cuky (Gallinita Café)** cuentan con su propio ciclo de vida: nivel, barra de experiencia, hambre, higiene, felicidad, sueño (`isSleeping` / `cukyIsSleeping`), timestamp de siesta y racha de días activos.
  - Al alimentar, bañar, dormir o jugar con una mascota, las modificaciones de estado se aplican **exclusivamente a la mascota activa** sin alterar los atributos de la otra.
  - **Ranking de Cuidadores Especializado:** Registro individualizado de puntos de cuidado (`carePointsKevinThor`, `carePointsAliThor`, `carePointsKevinCuky`, `carePointsAliCuky`), permitiendo ver quién es el cuidador número 1 de Thor y quién de Cuky, además del acumulado global.
- **Mecánica de Minijuegos Adaptativos (Retro Memory, Flappy Pet, La Serpiente):**
  - **Títulos y Sprites Dinámicos:** Los minijuegos adaptan automáticamente sus nombres, logos y personajes jugables según la mascota activa (`"🐔 FLAPPY CUKY 🪽"` vs `"🐱 FLAPPY THOR 🪽"`, `"${pet.getActiveName().uppercase()} SNAKE"`, `"${pet.getActiveName().uppercase()} POCKET™"`).
  - **Recompensa Diaria (1ª partida del día):** Otorga los Puntos de Amor (❤️) y EXP (✨) correspondientes automáticamente al terminar/perder la partida a la mascota activa, activando el *Modo Libre*.
  - **Modo Libre Ilimitado:** Una vez reclamada la recompensa diaria, los minijuegos **nunca se bloquean**. Los usuarios pueden seguir jugando infinitamente para batir récords y divertirse.
  - **Ranking de Récords de Pareja:** Se persisten y sincronizan en Firestore los mejores récords de Kevin y Ali (`flappyHighScoreKevin`, `flappyHighScoreAli`, `snakeHighScoreKevin`, `snakeHighScoreAli`), mostrándose en el selector y en las pantallas de fin de partida.
  - **Rendimiento Anti-Lag a 60/90/120 FPS:** Matrices de píxeles estáticas a nivel superior (`HEART_PIXEL_MATRIX`, `THOR_PIXEL_MATRIX`, `CUKY_PIXEL_MATRIX`) para cero recolección de basura (GC Churn) en el bucle de dibujo de Compose Canvas, junto con delta-time dinámico.
  - **Selector de Minijuegos Ampliado:** Diálogo con mayor espacio visual, badges de récords de pareja, icono adaptado de mascota (`ic_thor_balloon` / `ic_cuky_balloon`) y estado claro de recompensa diaria vs modo libre.

## 6. Flujos de Sincronización (Google Drive & Firestore)

1. **Resolución SAF en Android**: Se evita `DocumentFile.listFiles()` por lentitud. Se usa `contentResolver.query` con proyecciones mínimas y se opera en memoria.
2. **Reconciliación de Borrados Bidireccional**:
   - Foto borrada localmente -> se elimina en Drive y se marca `eliminado = true` en Firestore (Borrado local replicado en nube).
   - Foto con metadato `eliminado == true` -> se borra el archivo físico local (Borrado remoto replicado en el dispositivo).
3. **Lazy MD5 Hashing**: El cálculo de hash MD5 solo se ejecuta cuando los timestamps difieren o para archivos totalmente nuevos.
4. **Subida Paralela**: Controlada por corroutines y `Semaphore` configurable de 1 a 5 slots.

---

## 7. Despliegue Automatizado y Pruebas Multidispositivo

### CI/CD en GitHub Actions
- **Incrustar versión obligatoria:** Antes de publicar, incrementar `versionCode` y `versionName` en [app/build.gradle.kts](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/build.gradle.kts).
- **Creación de Tag:** Empujar el tag `v<versionName>` a `master` dispara el workflow [`.github/workflows/android.yml`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/.github/workflows/android.yml), el cual compila, firma y publica `app-release.apk` en GitHub Releases.
- **Notificación Push Push Automática (FCM v1):** Tras crear la Release, GitHub Actions ejecuta [`.github/scripts/send_update_notification.py`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/.github/scripts/send_update_notification.py) usando el `service-account.json` para emitir un push instantáneo al topic `diario_app_updates`. Todos los dispositivos con la app instalada reciben la alerta de actualización en tiempo real con enlace directo de descarga.

### Diálogo Retro Pixel-Art y Actualizaciones Silenciosas (Android 12+ / PackageInstaller)
- **Diálogo Modal Pixel-Art (`UpdateDialogCompose.kt`):** Presenta el comparador de versión actual vs nueva, notas de la versión en caja con scroll anti-overflow, barra de progreso pixelada en tiempo real (0 a 100%) y cancelación de descarga.
- **Instalación sin Diálogos en Android 12+ (API 31+):** Se utiliza `PackageInstaller.SessionParams` con `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` y streaming de flujo de entrada (`openInputStream`/`openWrite`). La app se actualiza silenciosamente en segundo plano sin mostrar la ventana del instalador del sistema.
- **Recepción de Estado:** `InstallResultReceiver` escucha el resultado del commit (`STATUS_SUCCESS` o `STATUS_PENDING_USER_ACTION` para fallback con confirmación de usuario).
- **Fallback Automático (Android 11 o inferior):** Si la API nativa de Android 12+ no está disponible o falla, la app abre directamente el instalador con `Intent.ACTION_VIEW`.

### Conexión ADB Multidispositivo (`conectar_adb.sh`)
- Script interactivo en el escritorio (`/home/kevin/Escritorio/sh/conectar_adb.sh`) para mDNS QR code pairing, vinculación automática, selector inteligente multidispositivo (instalar en 1 celular individual o en todos/2 a la vez en 1 solo paso con Gradle cache) y cambio dinámico de dispositivos destino en caliente (`[d]`).

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
- **Sincronización de Tema:** Modificar `theme` en la web actualiza el documento Firestore `users/<userId>` y adapta al instante los colores en la App Android.

---

## 9. Optimizaciones de Memoria y Rendimiento Aplicadas

1. **Protección de Desregistro de NetworkCallback**: Se encapsuló el desregistro de red en `MainActivity.kt` dentro de bloques `try-catch` para evitar fallos por `IllegalArgumentException` al cerrar la Activity.
2. **Downsampling Preventivo de Bitmaps (`ImageUtils.kt`)**: Funciones `calculateInSampleSize` y `decodeSampledBitmapFromUri` añadidas para decodificar fotos pesadas de la cámara en resoluciones máximas optimizadas (1200px), evitando fugas de memoria RAM (`OutOfMemoryError`).
3. **Memorización de Keys y Lambdas en Listas Compose**: Garantizada la estabilidad de listas mediante `key` únicos en `LazyColumn` en [`MessageFeedCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt) para prevenir recomposiciones completas de la lista cuando la app recibe actualizaciones de Firebase.
4. **Caché de `SharedPreferences` vía `remember(context)`**: Apertura del XML de preferencias encapsulada en `remember` en todas las pantallas de Compose ([`AnimeCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AnimeCompose.kt), [`MedsCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MedsCompose.kt), [`ScheduleCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ScheduleCompose.kt), etc.), evitando I/O de disco repetido durante renderizados.
5. **Intervalo Dinámico en Temporizador de Thor**: El temporizador dinámico `rememberTimeUntilDecay` en [`MessageFeedCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt) adapta su intervalo de actualización a 5000ms mientras quedan horas disponibles, reduciendo en un 80% el consumo de CPU y batería.
6. **Límite de Caché Firestore SQLite (100MB)**: Configurado `PersistentCacheSettings` con un límite estricto de 100MB en [`DiarioApp.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DiarioApp.kt) en lugar de `CACHE_SIZE_UNLIMITED`, evitando el crecimiento descontrolado de almacenamiento en disco en sesiones prolongadas.
7. **Modularización de Componentes de MainActivity**: Extracción de responsabilidades en clases helper dedicadas: [`PermissionHelper.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/PermissionHelper.kt) (permisos en Android 13+), [`NetworkStatusTracker.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/NetworkStatusTracker.kt) (conectividad y callbacks seguros) y [`PixelToastHelper.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/PixelToastHelper.kt) (toasts retro estilizados).
8. **Reglas Proguard / R8 para Minificación Segura**: Habilitación de `isMinifyEnabled = true` e `isShrinkResources = true` en `app/build.gradle.kts` con reglas de keep en `proguard-rules.pro` para WorkManager, Room, UCrop y Cloudinary, optimizando el tamaño del APK Release sin cierres inesperados.
9. **Eliminación Total de Glide & Unificación en Coil**: Se eliminó la librería pesada Glide (`glide`, `glide-compiler`, `DiarioGlideModule.kt`) migrando todas las vistas legacy (`AlbumManager.kt`, `RecipeManager.kt`, `MessageEditor.kt`) a `coil.load` y `Coil.ImageLoader`, reduciendo el tamaño del APK y tiempo de compilación.
10. **Optimización de Caché de Imágenes en Espíritus (`SpiritsCompose.kt`)**: Reemplazo de políticas que desactivaban la caché de red (`CachePolicy.DISABLED`) por `remember(spiritImageUrl, imageRefreshKey)` con claves de memoria y disco dinámicas (`memoryCacheKey` / `diskCacheKey`), eliminando recargas continuas durante el desplazamiento y mejorando la fluidez del scroll a 60/90/120 FPS.
11. **Batching de Escrituras en Firestore para Sincronización Drive (`SyncDriveWorker.kt`)**: Las actualizaciones de metadatos y tombstones de eliminación (`eliminado = true`) se agrupan en lotes atómicos de hasta 450 operaciones mediante `db.batch()`, reduciendo las llamadas de red individuales y acelerando drásticamente el proceso de limpieza y sincronización de carpetas de Drive.
12. **Ciclo de Vida y Parada Segura en Motor de Audio Retro (`RetroGameAudioEngine.kt`)**: Refactorización de `stopBgm()` con señalización no bloqueante e interrupción controlada de hilo, delegando la liberación (`stop()` y `release()`) exclusivamente al bloque `finally` del hilo generador para evitar condiciones de carrera o `IllegalStateException`.
13. **Memorización de Cálculos y Formateadores en Compose**: Encapsulación de filtrados de listas (`activeAnimeList`, `watchedAnimeList` en `AnimeCompose.kt`) y formateadores de fecha `SimpleDateFormat` en `remember` (`MedicalCompose.kt`, `MessageFeedCompose.kt`), eliminando allocations redundantes en el Garbage Collector en cada recomposición.
14. **Caché en Memoria de Token OAuth2 para FCM v1 (`MainActivity.kt`)**: Reutilización de la instancia de `GoogleCredentials` en memoria con `refreshIfExpired()`, evitando la lectura y parseo continuo de claves RSA desde `service-account.json` y solicitudes HTTP innecesarias a Google Auth en cada interacción (likes, cartas, recetas).
15. **Compresión y Downsampling Preventivo Pre-Cloudinary (`ImageUtils.kt` / `MainActivity.kt`)**: Función `compressImageForUpload` ejecutada en background antes de enviar cualquier archivo a Cloudinary, reescalando y comprimiendo fotos pesadas (de 15-20MB a <600KB), reduciendo en un 90% el tiempo de subida y el consumo de datos.
16. **Física por Delta-Time Adaptativa para Minijuegos a 90Hz / 120Hz (`FlappyThorCompose.kt`)**: Sincronización del bucle del juego mediante `withFrameNanos` y factor de tiempo delta $\Delta t$, permitiendo renderizado nativo a 90 FPS y 120 FPS sin alterar la calibración ni velocidad de las físicas de salto y obstáculos.
17. **Memorización de Estructuras y Recomposición de Calendario (`CalendarCompose.kt`)**: Extracción y encapsulación de `dayEvents` y cálculos de matriz mensual (`daysInMonth`, `startOffset`, `selectedDayOfMonth`) en bloques `remember`, además de asignación de `key = { it.eventId }` en `LazyColumn`, eliminando docenas de instanciaciones `Calendar.getInstance()` por frame.
18. **Animaciones en Fase de Dibujo (Draw-Phase) con `Modifier.graphicsLayer { ... }` (`MessageFeedCompose.kt` / `PetDialogCompose.kt`)**: Migración de todas las animaciones de mascotas (respiración, waddling, bamboleo y flotación) a la sobrecarga lambda de `graphicsLayer`. Esto transfiere la ejecución directamente al RenderNode de la GPU, previniendo recomposiciones masivas del árbol de Jetpack Compose en cada fotograma a 60/90/120 FPS.
19. **Cero Asignaciones en Dibujo de Minijuegos (Zero GC Allocation Canvas - `FlappyThorCompose.kt`)**: Extracción de todas las matrices de píxeles (`HEART_PIXEL_MATRIX`, `THOR_PIXEL_MATRIX`, `CUKY_PIXEL_MATRIX`) a constantes estáticas de nivel superior en memoria (`private val`), eliminando por completo la creación de arrays en caliente dentro del ciclo `withFrameNanos` y suprimiendo el lag por Garbage Collection.

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
5. **Servidor Local y Enrutamiento SPA Limpio (`web/server.py`):**
   - Servidor Python backend en puerto 8000 con enrutamiento SPA directo sin redirecciones permanentes 301.
   - Normalización de URLs en el cliente (`replaceState`) para mantener rutas limpias (`http://localhost:8000/`, `/edit`, `/config`, `/db`).
6. **Selector de Tasa de Refresco (Hz) Sincronizado en App & Web:**
   - Opciones dinámicas de **60 Hz (Batería), 90 Hz (Por Defecto / Recomendado), 100 Hz y 120 Hz (Ultra Fluido)**.
   - Sincronización en tiempo real en Firestore (`users/<userId>/refreshRate`) con persistencia local en `SharedPreferences`.
   - Aplicación técnica a bajo nivel en Android mediante `preferredDisplayModeId` (API 23+) y coincidencia óptima con la resolución activa de la pantalla.
7. **Release v1.7.46 (Build 91):** Publicada en GitHub Releases vía CI/CD con tag `v1.7.46` (versionCode 91). Corrige falsas notificaciones repetidas de llegada/salida en zonas seguras de Thor Radar mediante caché local instantáneo (0ms) en SharedPreferences, eliminación del reinicio accidental de estado de zona al iniciar la app, sistema de doble confirmación multi-muestra con histéresis aumentada (+45m / +85m en reposo), ventana de cooldown de 5 minutos y descarte de pings de GPS con baja precisión.
8. **Release v1.7.50 (Build 95):** Publicada en GitHub Releases vía CI/CD con tag `v1.7.50` (versionCode 95). Implementa la arquitectura Wake-on-LAN (Magic Packet) silenciosa para la sincronización remota de ubicación en vivo en Thor Radar con `WakeLock` protegido en Doze mode, Fix GPS de alta precisión multicanal (`FusedLocation` + `LocationManager` nativo), botón de refresco rápido `[ 🔄 ACTUALIZAR ]` y purga completa de archivos/recursos en desuso.

---

## 11. Motor de Audio 8-Bit & Minijuegos Retro (Flappy Thor & La Serpiente)

1. **Motor de Sonido Chiptune Unificado (`RetroGameAudioEngine.kt`):**
   - Sintetizador procedural PCM en tiempo real mediante `AudioTrack` multicanal en segundo plano (ondas cuadradas y triangulares NES con envolventes suaves).
   - Melodías extendidas de 4 secciones con bajo melódico independiente para Flappy Thor y tema arcade en escala menor armónica para La Serpiente.
   - Gestión segura de ciclo de vida: parada instantánea en game over/pausa sin bloquear el hilo de UI ni fugas de hilos de audio.
2. **Sprite Dedicado de Thor Pájaro Blanco (`drawWhiteThorBirdSprite`):**
   - Sprite pixel-art limpio de Thor con pelaje blanco esponjoso, orejitas, ojitos expresivos, rubor en las mejillas y alitas batientes animadas en tiempo real según la velocidad y los toques.
   - Integración dinámica con los accesorios equipados (corona, moño, gafas, bandana).
3. **Calibración Accesible y Equilibrada en Flappy Thor:**
   - Espacios generosos entre tubos (0.32 en Fullscreen / 0.38 en Pocket), física de salto balanceada y hitbox justa para una experiencia fluida y entretenida.
4. **Bucle de Juego Robusto y Responsivo en La Serpiente:**
   - Bucle continuo y suave con velocidad adaptable por puntaje, control táctil con Swipe y D-PAD ergonómico, efectos de sonido y puntuación en vivo.

---

## 12. Rediseño y Optimización del Horario de Clases (`ScheduleCompose.kt`)

1. **Selector Dual de Vistas (Agenda Diaria & Grilla Semanal):**
   - Selector en cabecera dedicada `[📋 Día] / [📅 Semana]` organizado en dos filas para evitar que los botones se aprieten o deformen en pantallas angostas.
   - **Vista Agenda Diaria (`📋 Día`):** Diseñada para móviles en vertical con pestañas de lunes a viernes, conteo de materias por día, tarjetas amplias con duración calculada (`1h 30m`), badges claros de dueño, sala y profesor, evitando cualquier corte de texto.
2. **Arquitectura de Columnas Paralelas en Grilla Semanal (`📅 Semana`):**
   - En el modo "👥 Ambos", cada día se divide limpiamente en dos subcolumnas paralelas de 145dp dedicadas (`👦 Kevin` | `👧 Ali`), eliminando el problema de tarjetas solapadas que estiran y aplastan los bloques de clases en todo el día.
   - Al filtrar por persona individual (`👦 Kevin` o `👧 Ali`), el día se expande a columna completa de 200dp.
3. **Cálculo Dinámico de Contraste (`getContrastingTextColor`):**
   - Algoritmo de luminancia sRGB que adapta el color de los textos y badges a negro/oscuro o blanco nítido según la luminosidad del color de la asignatura (`colorHex`), garantizando legibilidad en cualquier color pastel.
4. **Optimización de Grilla Semanal:**
   - Aprovechamiento del 100% del ancho de pantalla de borde a borde.
   - Columnas proporcionales dinámicas según resolución (`screenWidthDp`), permitiendo ver 2 días completos a la vez en modo "Ambos" y más de 3 días en modo individual.
   - Altura de filas compacta (48dp) y columna de horas simplificada (46dp).
5. **Limpieza de Configuración:**
   - Eliminada la tarjeta y lógica en desuso de migración de espíritus a Cloudinary dentro de la sección *Avanzado (Diagnóstico)*.
6. **Manejo Integral del Botón Atrás del Sistema (`BackHandler`):**
   - Integrado `BackHandler` en **Configuración**, **Álbum** (Álbum General / Momentos) y **Misceláneos** (Espíritus, Anime, Medicamentos, Horario, Web).
   - Ahora, presionar el botón físico o gesto de volver del celular regresa limpiamente al menú de carpetas o menú raíz de la sección en lugar de salir abruptamente hacia la vista de Cartas (Home).
7. **Optimización en Diseño y Tema (`SettingsFragment.kt` & `ProfileSettingsCompose.kt`):**
   - **Agrupación por Familias Cromáticas en Filas Dedicadas:**
     - **Modo Claro:**
       - *🌸 Rosas y Corales:* Rosa Bebé (`#F8BBD0`), Flor de Cerezo (`#FFCDD2`), Leche de Fresa (`#FCE4EC`), Rosa Niebla (`#F3E5F5`).
       - *💜 Púrpuras y Lilas:* Lavanda (`#D1C4E9`), Lila Pastel (`#E1BEE7`), Bígaro (`#C5CAE9`).
       - *🩵 Azules y Aqua:* Algodón de Azúcar (`#E1F5FE`), Azul Cielo (`#B3E5FC`), Aqua Turquesa (`#B2EBF2`).
       - *🍃 Verdes y Matcha:* Menta Dulce (`#C8E6C9`), Matcha (`#DCEDC8`), Oliva Suave (`#DCE775`).
       - *☀️ Cálidos y Latte:* Amarillo Mantequilla (`#FFF59D`), Mandarina (`#FFE57F`), Durazno (`#FFE0B2`), Beige Latte (`#D7CCC8`).
     - **Modo Oscuro:**
       - *❤️ Rojos y Vinos:* Carmesí Sangre (`#B71C1C`), Magenta Neón (`#C2185B`), Burdeos (`#880E4F`), Óxido Caoba (`#BF360C`).
       - *💜 Púrpuras y Violetas:* Púrpura Imperial (`#4A148C`), Violeta Synthwave (`#6A1B9A`), Casis Ciruela (`#4A0E4E`).
       - *💙 Azules e Índigos:* Azul Marino Noche (`#0D47A1`), Índigo Profundo (`#1A237E`), Pizarra Denim (`#37474F`).
       - *🌲 Verdes y Teals:* Océano Profundo (`#006064`), Jade Esmeralda (`#004D40`), Verde Bosque (`#1B5E20`), Oliva Oscuro (`#33691E`).
       - *🔥 Cálidos y Neutros:* Ámbar Quemado (`#E65100`), Chocolate Espresso (`#3E2723`), Grafito Carbón (`#263238`).
   - **Indicador Visual de Color Activo:** Resalta nítidamente el color seleccionado con un borde reforzado (3dp), tamaño destacado (38dp vs 32dp) y un checkmark `✓` retro centrado de alto contraste (blanco o negro según la luminosidad del color).
   - **Interruptor para Barra Superior ('Nuestro Diario'):** Nueva opción en *Diseño y Tema* para ocultar o mostrar el recuadro superior del título (`showTopBar`), permitiendo una experiencia inmersiva a pantalla completa o con cabecera tradicional.
   - **Actualización Reactiva Inmediata del Fondo y Contenedores:** El fondo de la pantalla y de toda la app (`getAppBackgroundColor`) y los contenedores de texto se recalculan dinámicamente en tiempo real al instante al cambiar de color o al alternar la casilla *"Aplicar color también al fondo"* (`useCustomBg`).

---

## 13. Thor Radar: Ubicación en Tiempo Real, Geocercas, Edición de Zonas, Notificaciones & Alertas SOS

1. **Arquitectura y Servicios de Rastreo (`ThorRadarManager.kt` & `ThorRadarService.kt`):**
   - **Foreground Service con Notificación Persistente y Bucle Activo de Heartbeat:** `ThorRadarService` opera con tipo `location` y ejecuta una corrutina en segundo plano (`heartbeatJob`) con `WakeLock` protegido que emite pulsos cada 12s (o 30s en modo ahorro) garantizando que el timestamp, porcentaje de batería y estado (`🛋️ En reposo`) se sincronicen en vivo en Firestore incluso cuando el dispositivo lleva horas completamente quieto o con la pantalla apagada.
   - **Doble Proveedor de Ubicación (FusedLocation + LocationManager Nativo):** Registra callbacks con `FusedLocationProviderClient` y `LocationListener` nativo de Android (`GPS_PROVIDER` y `NETWORK_PROVIDER`) para garantizar señal continua en segundo plano y en interiores.
   - **Auto-Inicio al Reiniciar o Actualizar (`BootReceiver.kt`):** Receptor de broadcasts para `BOOT_COMPLETED`, `QUICKBOOT_POWERON` y `MY_PACKAGE_REPLACED` que reanuda automáticamente el rastreo en segundo plano sin requerir abrir la app manualmente.
   - **Tarjeta de Optimización de Batería en Ajustes:** Acceso directo desde la pestaña `⚙️ AJUSTES` para desactivar la optimización agresiva de batería del sistema operativo (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) evitando que Android congele el proceso.
   - **Mapeo Robusto de Identidad:** Detección confiable de usuario (`ali` vs `kevin`) basada en `userId` y `userName` en `SharedPreferences`, normalizando automáticamente la ruta `locations/<coupleId>/users/<docName>`.
   - **Cálculo Preciso de Velocidad (km/h) y Clasificación de Actividad:**
     - Cálculo híbrido continuo: aprovecha la velocidad de hardware del GPS (`loc.speed`) y cálculo matemático de delta de distancia sobre delta de tiempo ($\frac{\Delta\text{dist}}{\Delta t}$) con suavizado de media móvil exponencial (EMA) y filtro de jitter estático.
     - Clasificación inteligente de movimiento:
       - $\ge 20.0\text{ km/h}$: `IN_VEHICLE` (🚗 En auto)
       - $7.5\dots 20.0\text{ km/h}$: `RUNNING` (🚴 En movimiento)
       - $2.0\dots 7.5\text{ km/h}$: `WALKING` (🚶 Caminando)
       - $< 2.0\text{ km/h}$: `STILL` (🛋️ En reposo)
     - Visualización en tiempo real en la tarjeta `PartnerRadarCard` (`🚗 En auto (45 km/h)` / `🚶 Caminando (4 km/h)` / `🛋️ En reposo`), en la barra superior del radar satelital (`🛰️ RADAR SATELITAL • PRECISIÓN: ±8m • 🚀 45 km/h`), y como badge flotante sobre los pines/avatares en el mapa interactivo.
   - **Emisión de Latidos (Heartbeats):** Registro automático de cambios de estado, velocidad, actividad y geocodificación inversa para dirección física (`thoroughfare`, `locality`).
    - **Notificaciones Push Automáticas de Zonas Seguras y Filtro Anti-Spam / Anti-Jitter (FCM v1 Data-Only):**
      - **Caché Local Síncrono de Zonas (`ThorRadarZonePrefs`):** Persistencia instantánea en JSON de las zonas seguras en almacenamiento local para disponibilidad inmediata (0ms) en arranques en frío y reinicios de background service, eliminando falsas salidas o falsas llegadas al abrir la app.
      - **Doble Confirmación Multi-Muestra y Margen de Histéresis:** Margen de salida aumentado ($\ge 45\text{m}$ más error de precisión del GPS), supresión de falso jitter GPS de interior cuando el usuario está en reposo (`STILL` / $\le 85\text{m}$ de buffer), y confirmación de 2 lecturas consecutivas fuera antes de emitir alerta de salida.
      - **Ventana de Cooldown de 5 Minutos y Filtro de Precisión:** Ventana de cooldown de 300s para no re-notificar la misma zona y descarte de pings GPS con precisión deficiente ($> 65\text{m}$) para evitar rebotes de geocerca.
      - Payloads *Data-Only* con `authorId` y `authorName` filtrados en `MyFirebaseMessagingService.kt` para garantizar que la persona que se mueve nunca reciba sus propias notificaciones.

2. **Editor Interactivo de Zonas (`AddEditZoneDialog` & `CenterZoneOverlay`):**
   - **Sistema de Retícula Central Fija (`CenterZoneOverlay`):** El mapa se mueve libremente por debajo del visor manteniendo una retícula central fija con radio de cobertura visible en tiempo real (círculo proporcional con borde punteado y radio en metros `R: 120m`). Al mover el mapa o ajustar el radio con el slider o presets (`50m` a `1km`), la zona se actualiza instantáneamente con las coordenadas del centro del mapa.
   - **Búsqueda en Vivo Tipo Google Maps con Formato Chileno:** Búsqueda reactiva con autocompletado en tiempo real mientras el usuario escribe (debounce de 350ms). Prioriza resultados de Chile (`Locale("es", "CL")` y OpenStreetMap Nominatim con `countrycodes=cl`), formateando explícitamente:
     - **Título:** Calle y Número / Nombre del Lugar (ej. *Av. Providencia 1234*, *Mall Plaza Vespucio*).
     - **Subtítulo:** Comuna, Región y País (ej. *Providencia, Región Metropolitana, Chile*).
   - **Controles de Precisión:** Slider de 30m a 1000m, botones de micro-ajuste fino `[-10]` y `[+10]` metros, chips de radio rápido (`50m` a `1km`), botón `🎯 Centrar Aquí` y botón `📍 Mi GPS`.
   - **Edición Completa de Zonas Creadas:** Capacidad de editar nombre, emoji, coordenadas y radio de zonas existentes directamente desde la lista de `🏠 ZONAS` (botón `✏️ EDITAR` o tocar la tarjeta) y desde el mapa `🗺️ MAPA` al pulsar sobre cualquier zona.
   - **Selector de Iconos Ampliado:** Emojis retro ampliados (`🏠`, `🎓`, `💼`, `🏋️`, `☕`, `🍔`, `🛒`, `❤️`, `🌲`, `🏥`, `🎮`, `🚗`, `✈️`, `🏖️`, `🐾`).

3. **Renderizado de Mapas Limpio & Sin Marcas de Agua:**
   - **Fuente de Teselas Estándar de Google Maps (`GOOGLE_MAPS_TILES`):** Implementada mediante `OnlineTileSourceBase` en Osmdroid sin necesidad de API keys de pago, marcas de agua ni saturación visual de POIs.
   - **Filtro de Modo Oscuro Dinámico:** Aplicación de `ColorMatrixColorFilter` en `overlayManager.tilesOverlay` cuando el tema activo es *Pixel Oscuro*, adaptando las calles y fondos al modo nocturno.
   - **Marcadores con Fotos de Perfil Reales:** Renderizado asíncrono con Coil (`allowHardware(false)`) y `BitmapShader` para recortar en círculo perfecto las fotos de Kevin y Ali dentro de pines vectoriales con anillos temáticos (Azul para ti, Rosa para tu pareja, y Rojo Neón pulsante si SOS está activo).

4. **Sistema de Alerta de Emergencia SOS:**
   - **Notificaciones Push de Alta Prioridad (FCM v1):** Envío directo al proyecto `diario-ali-kevin` y topic `diario_vinculo_unico_123` con canal prioritario `diario_channel`.
   - **Listener en Tiempo Real en la App (`MainActivity.kt`):** Escucha instantánea del documento de la pareja. En cuanto se activa la alerta, el dispositivo receptor vibra y despliega un cuadro emergente de emergencia con el botón directo `[ VER EN MAPA ]`.
   - **Cuenta Regresiva de Seguridad y Cancelación:** Diálogo visual con cuenta atrás de 3 segundos (`[ ❌ CANCELAR ]` / `[ 🚨 ENVIAR YA ]`), banner rojo pulsante y botón de desactivación segura (`[ ✅ DESACTIVAR SOS (ESTOY BIEN) ]`).

5. **Módulos Optimizados (4 Pestañas Claras):**
   - `🗺️ MAPA`: Mapa satelital con auto-centrado inteligente (`BoundingBox`), controles flotantes de zoom, centrado en ambos, centrado en pareja y centrado en uno mismo, y tarjeta emergente de edición al tocar zonas.
   - `🧭 BRÚJULA`: Brújula de amor con rotación animada suave (`spring`), ángulo exacto, distancia calculada (`km`/`m`) y estado "¡Juntos en el mismo lugar!".
   - `🏠 ZONAS`: Listado de geocercas registradas con botón `✏️ EDITAR`, eliminación instantánea y creación.
   - `⚙️ AJUSTES`: **Espacio Ampliado y Rediseño Ergonómico:** En la pestaña de Ajustes se oculta la tarjeta flotante de la pareja y los banners redundantes para otorgar el 100% del alto de la pantalla a la configuración.
     - **Botón Maestro de Encendido y Apagado (`RadarMasterPowerCard`):** Permite encender o apagar el radar con 1 solo toque (`[ 🛑 APAGAR THOR RADAR ]` / `[ ⚡ ENCENDER THOR RADAR ]`), deteniendo el Foreground Service (`ThorRadarService`) y los sensores GPS a cero consumo de batería.
     - **Toggle Modo Ahorro:** Alternar entre 15s y 60s de intervalo para viajes largos.
     - **Panel Consolidado de Requisitos (`RadarRequirementRow`):** Estado en tiempo real de Sensor GPS, Permiso Segundo Plano ("Todo el tiempo") y Batería sin restricciones con botones de acción directa integrados (`[ ACTIVAR ]` / `[ QUITAR LÍMITE ]`) y acceso al asistente modal.
     - **Telemetría GPS e Identidad:** Coordenadas `Lat/Lng`, precisión `±Xm`, porcentaje de batería y botón `[ 🔄 ACTUALIZAR MI UBICACIÓN AHORA ]`.
   - **Caché Instantáneo y Arranque Suave Estilo Life360 (v1.7.47):** `ThorRadarManager.kt` persiste las ubicaciones en disco (`ThorRadarLocationPrefs`). Al abrir `ThorRadarCompose.kt`, `myLocationData`, `partnerLocationData` y `placeZones` se cargan en el **Frame 0**, eliminando pantallas en blanco, saltos de layout o valores en cero.
   - **Diagnóstico Silencioso e Inteligente:** La tarjeta en vivo de la pareja (`PartnerLiveCard`) solo activa el estado `isStale` si ya existen datos válidos y la última señal supera los 15 minutos o si la pareja apagó voluntariamente el radar, evitando el parpadeo de cajas y botones naranjas durante la conexión inicial.
   - **Zonas Seguras de Alto Contraste en Modo Oscuro:** En el mapa satelital oscuro, las geocercas y retículas interactivas (`CenterZoneOverlay`) se dibujan en **Cyan Neón Eléctrico** (`#00E5FF`) con bordes reforzados (`4f`/`4.5f`) y relleno translúcido brillante, garantizando máxima visibilidad sobre los tiles invertidos.
   - **Asistente de Configuración Requisitos (`RadarSetupWizardDialog`):** Diálogo retro interactivo con checklist paso a paso. Se persiste el flag de descarte (`radar_setup_wizard_dismissed`) para no interrumpir al usuario en aperturas posteriores y permanece accesible desde la pestaña Ajustes.
     - **Dual Ping (FCM Wake-on-LAN + Firestore Ping) & Rastreo Híbrido Inteligente (`[ 🔄 ACTUALIZAR ]` / `[ 🔄 ACTUALIZAR AHORA ]`):**
       - **Doble Disparador de Sincronización:** Al pulsar *"Actualizar Ahora"*, se emite simultáneamente un registro en Firestore `locations/<coupleId>/pings/<targetDoc>` (respuesta en 0ms si la pareja tiene la app abierta) y un paquete prioritario FCM Wake-on-LAN (`radar_ping` / `WOL_LOCATION_WAKEUP`) con `WakeLock` seguro (35s) para despertar al teléfono si está en Doze mode o en segundo plano.
       - **Rastreo en Primer Plano Sin Congelamientos (`isForegroundTracking`):** Cuando cualquiera de los dos tiene la pantalla de Thor Radar abierta, la app transmite en vivo cada 8–12 segundos si hay desplazamiento $\ge 10\text{m}$, permitiendo verse caminar y moverse fluidamente en el mapa sin bloqueos.
       - **Ahorro Estricto en Segundo Plano:** Cuando la app está cerrada o la pantalla apagada, solo sube actualizaciones si hubo desplazamiento significativo ($\ge 150\text{m}$ y $\ge 5\text{min}$), manteniendo un consumo de batería y cuota Firestore ultra eficiente.

---

## 14. Optimizaciones de Rendimiento, Batería y UI (Compose, Background & Build)

1. **Eficiencia Energética y Red en Segundo Plano (`ThorRadarManager.kt`):**
   - **Throttling Inteligente de Geocodificación Inversa:** Implementación de caché de coordenadas y tiempo para `Geocoder.getFromLocation`. Solo se dispara una nueva resolución de dirección física si el usuario se desplaza más de 40 metros o si transcurren más de 5 minutos desde la última consulta, eliminando el consumo continuo de red/batería cuando el dispositivo permanece en reposo o con jitter GPS.

2. **Control de Almacenamiento y Caché de Mapas (`DiarioApp.kt`):**
   - **Límite Estricto de Disco para Osmdroid:** Configuración de `tileFileSystemCacheMaxBytes = 100MB` y `tileFileSystemCacheTrimBytes = 80MB` para evitar que el directorio de caché de mapas de Google Maps/OSM crezca de manera descontrolada tras meses de uso continuado.

3. **Jetpack Compose - Estabilidad y Eliminación de Jank/Recomposition Churn:**
    - **Claves Estables (`key`) en Listas y Grillas:** Implementadas en la totalidad de `LazyColumn`, `LazyRow` y `LazyVerticalGrid` ([`ThorRadarCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ThorRadarCompose.kt), [`MessageFeedCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt), [`AlbumCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AlbumCompose.kt), [`MedsCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MedsCompose.kt), [`SpiritsCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SpiritsCompose.kt), [`PetDialogCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/PetDialogCompose.kt), [`AnimeCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/AnimeCompose.kt) y [`CalendarCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/CalendarCompose.kt)).
    - **Memoización de Filtrado de 117 Espíritus:** `remember(sortedSpirits, filterMode, ...)` en [`SpiritsCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/SpiritsCompose.kt), evitando re-filtrar la colección de 117 ítems en cada frame.
    - **Memoización de Bitmaps de Pines de Mapa:** `remember(avatar, badge, ringColor)` para `myMarkerBitmap` y `partnerMarkerBitmap` en `ThorRadarCompose.kt`.
    - **Memoización de Parseo HTML y URLs de Cloudinary:** `remember(message.content) { Html.fromHtml(...) }` en `MessageFeedCompose.kt` y `remember(displayUrl) { displayUrl.optimizeCloudinary(400) }` en `AlbumCompose.kt`.

4. **Arranque en Frío (Cold Start) Optimizado (`DiarioApp.kt`):**
   - Tareas periódicas de WorkManager (`rescheduleUpdateCheck` y `schedulePetCareCheck`) diferidas al hilo secundario `Dispatchers.IO` mediante corrutinas para un inicio de app instantáneo.

5. **Optimización Multimedia y Subida de Imágenes:**
   - **Pre-compresión y Escalado Inteligente:** Uso de `compressImageForUpload` para redimensionar fotos de cámaras de alta resolución a un máximo de 1920px y comprimir a JPEG 85% antes de enviarlas a Cloudinary, acelerando la subida 5x en redes móviles.

6. **Compilación Anticipada ART (Baseline Profiles & ProfileInstaller):**
   - Integración de `androidx.profileinstaller` y definición de [`app/src/main/baseline-prof.txt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/baseline-prof.txt) con las rutas de ejecución críticas (`DiarioApp`, `MainActivity`, `MainViewModel`, `ThorRadar`, `compose/**`). El compilador ART de Android pre-compila el código a binario nativo (AOT) durante la instalación, reduciendo los tiempos de arranque y los cuadros perdidos en Compose un 15-20%.

7. **Purga de Código Legacy y Unificación de Broadcasts (v1.7.48):**
   - Eliminación de 11 archivos de layouts XML huérfanos sin uso tras la migración a Compose.
   - Unificación de `PackageReplacedReceiver` dentro de [`BootReceiver.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/BootReceiver.kt), reduciendo redundancia en el `AndroidManifest.xml`.
   - Limpieza de binarios locales y scripts temporales de desarrollo.

8. **Limpieza Integral de Código Muerto y Recursos Huérfanos (v1.7.49):**
   - Eliminación de archivos obsoletos de Compose y XML legado (`CartasCompose.kt`, `MessageEditor.kt`, `dialog_view_message.xml`).
   - Purgados 11 drawables huérfanos (`color_circle_*.xml`, `bg_pixel_toolbar.xml`, `bg_input_pill.xml`, `ic_time_pixel.xml`, `ic_settings_gear_pixel.xml`).
   - Depuración de estilos obsoletos de `CalendarView` en `styles.xml`.
   - Limpieza del archivo monolítico duplicado `index.html` en la raíz del proyecto.
   - Subida unificada directa a Cloudinary mediante `MediaManager` en `MainActivity.kt`.

9. **Arquitectura Wake-on-LAN (Magic Packet) para Solicitud de Ubicación (v1.7.50):**
   - Payload de alta prioridad FCM v1 (`WOL_LOCATION_WAKEUP`) con `priority: HIGH` y TTL de 120s.
   - Receptor con `PARTIAL_WAKE_LOCK` protegido contra Doze Mode y pantalla apagada (`handleMagicLocationPing`), procesado en segundo plano de manera silenciosa (sin alertas invasivas en el celular receptor).
   - Doble paso de sincronización: Heartbeat instantáneo (<300ms) con batería y última ubicación conocida + Fix GPS fresco forzado (`requestHighAccuracyFix` con `FusedLocation` y `LocationManager` nativo en paralelo).
   - Acceso rápido `[ 🔄 ACTUALIZAR ]` integrado en la cabecera en vivo de la pareja en `PartnerLiveCard` y botón `[ 🔄 ACTUALIZAR AHORA ]` con retroalimentación en pantalla.

10. **Nueva Mascota Multiespecie: Cuky la Gallina Café (Sistema Switchable & Guardarropa Independiente):**
    - Integrado selector de mascota activa en el diálogo de mascotas (`[ 🐱 THOR ]` / `[ 🐔 CUKY ]`) y persistencia en Firestore (`petType: "thor" | "cuky"`).
    - **Guardarropa e Inventario Separado por Mascota:** Cuky y Thor tienen sus propios accesorios desbloqueados/equipados (`cukyUnlockedAccessories`, `cukyEquippedAccessory` vs `unlockedAccessories`, `equippedAccessory`) y sus propios fondos desbloqueados/equipados (`cukyUnlockedBackgrounds`, `cukyEquippedBackground` vs `unlockedBackgrounds`, `equippedBackground`), permitiendo comprar ropas y fondos de forma independiente para cada uno sin sobreescribir lo que lleva puesto la otra mascota.
    - **Nuevos Fondos Temáticos en Pixel-Art (Día / Noche):**
      - `coop`: **Gallinero Acogedor 🛖** (`bg_cuky_coop_day` / `bg_cuky_coop_night`) con nidos de paja, vigas de madera rústica y luz suave (Fondo base por defecto de Cuky).
      - `farm`: **Huerta de Cultivos 🌽🌻** (`bg_cuky_farm_day` / `bg_cuky_farm_night`) con cultivos de maíz, trigo, girasoles, cercas de madera y cielo estrellado de noche.
    - Set completo de sprites pixel-art dedicados generados para Cuky: base transparente (`ic_cuky_base_trans`), durmiendo en nidito (`ic_cuky_sleep`), baño (`ic_cuky_bath`), jugando con pelota (`ic_cuky_play`) y compatibilidad con los 10 accesorios (`ic_cuky_hat`, `crown`, `glasses`, `mustache`, `bow`, `balloon`, `banana`, `bandana`, `collar`, `socks`).
    - Alimentos temáticos de granja cuando Cuky está activa (`🌾 Semillas de Amor`, `🌽 Maíz Dorado`, `🍉 Sandía Fresca`, `🪱 Banquete de Gusano`).
    - Adaptación en minijuego *Flappy* con sprite dedicado aleteando en pixel-art procedural (`drawBrownCukyBirdSprite`) tanto en pantalla completa como en modo consola Pocket LCD.
    - Soporte en Widgets de escritorio (`ThorWidgetProvider`) y Feed de Cartas principal (`MessageFeedCompose`).
13. **Sincronización Atómica y Normalización de Ranking de Cuidadores en Firestore (v1.7.51):**
    - **Operaciones Atómicas en Backend (`FieldValue.increment`):** Todas las acciones de interacción y cuidado (comidas, baños, pelotas, caricias/mimos, minijuegos, cartas y álbum) utilizan `FieldValue.increment(...)` directamente en Cloud Firestore, suprimiendo cualquier condición de carrera o sobreescritura cuando Kevin y Ali interactúan al mismo tiempo.
    - **Normalización y Migración Histórica (`checkAndMigratePetRanking`):** Migración automática de puntos históricos que residían en campos generales (`carePointsKevin` / `carePointsAli` y contadores de categorías) hacia los campos dedicados de Thor (`carePoints...Thor`, `feedCount...Thor`, etc.) directamente en el documento de Firestore, garantizando que ambos dispositivos reciban y muestren exactamente los mismos números.
    - **Corrección de Identidad de Sesión en Minijuegos:** Corrección del acceso a SharedPreferences en `FlappyThorCompose` y `SnakeGameCompose` (migrado de `"diario_prefs"` a `"DiarioPrefs"`), resolviendo el bug donde las puntuaciones de Ali se asignaban erróneamente al perfil de Kevin.
14. **Estrategia y Arquitectura para Repositorio Privado con Actualizaciones OTA:**
    - Documentadas las 3 alternativas para hacer privado el repositorio de código fuente preservando las actualizaciones silenciosas:
      1. **Repositorio Espejo Público para Releases (Recomendada):** Código fuente en repo privado (`Diario_alikevin`) y publicación automática del APK mediante CI/CD GitHub Actions en un repositorio público secundario vacío de releases (ej. `Diario_releases`), sin requerir tokens en la app cliente.
      2. **Token PAT Fine-Grained de Solo Lectura:** Inyección de `Authorization: Bearer <TOKEN>` con permisos exclusivos de lectura de Releases en `UpdateManager.kt`.
      3. **Hosting de Binarios en Cloudinary / Firebase Storage:** Subida del APK al bucket cloud con archivo descriptor `version.json`.
15. **Panel de Administrador en Configuración (`ProfileSettingsCompose.kt` / `SettingsFragment.kt` - v1.7.52):**
    - Sección protegida por contraseña (`"123"`) accesible en el menú de Configuración (`[ 🔒 Panel Administrador ]`).
    - Integración de 3 acciones maestras de reinicio con modal de confirmación previa retro:
      1. **👑 Reiniciar Ranking de Cuidadores (`adminResetRanking`):** Restablece a 0 en Firestore todos los puntos de cuidadores de Kevin y Ali (globales y por mascota), así como todos los contadores de interacción (comida, baño, pelota y minijuegos).
      2. **🎮 Reiniciar Minijuegos (`adminResetMinigames`):** Restablece a 0 los récords máximos (High Scores) de Flappy y Snake de ambos usuarios y limpia las fechas de partidas diarias tanto en Firestore como en SharedPreferences locales (`flappy_thor_prefs`, `snake_game_prefs`).
      3. **🐾 Reiniciar Mascotas (`adminResetPets`):** Restablece a Thor y Cuky al Nivel 1 con 0 EXP, 100% de felicidad, 100% de limpieza, 0 hambre y despiertos.
16. **Calibración de Dificultad y Física Dinámica en Flappy Pet (`FlappyThorCompose.kt`):**
    - **Física Reactiva y Arcade:** Ajuste preciso de impulsos (`jumpForce: -0.0076f`), gravedad balanceada (`0.00038f`) y velocidad base fluida (`0.0034f`).
    - **Dificultad Dinámica Progresiva:** La apertura entre tuberías se reduce de manera orgánica a medida que sube el puntaje (`currentPipeGap = (baseGap - score * 0.0025f).coerceAtLeast(minGap)`), junto con un aumento gradual de la velocidad de avance (`score * 0.00005f`).
    - **Recompensas Balanceadas:** EXP diaria acotada a `(finalScore * 2 + hearts * 3).coerceIn(10, 30)` y Puntos de Amor a `(finalScore * 2 + hearts * 2).coerceIn(5, 40)`.
17. **Optimización del Sistema de Niveles y EXP de Mascotas (`MainViewModel.kt`):**
    - **Corrección de Subida de Niveles Repentina:** Reemplazo de las comprobaciones condicionales simples `if (newExp >= 100)` por bucles de consumo exhaustivo `while (newExp >= 100)` en todas las interacciones (`updatePetOnInteraction`, `rewardPet`, `bathPet`, `playBallPet`, `playMinigame`), asegurando que cualquier excedente de EXP se procese correctamente en múltiples niveles con sus correspondientes bonificaciones de Puntos de Amor (+50 LP por nivel) sin dejar remanentes corruptos.
18. **Unificación de Avanzado y Panel Administrador con Monitoreo de Cuotas Firestore (v1.7.61):**
    - **Unificación de Navegación (`ProfileSettingsCompose.kt`):** Fusión de las opciones separadas de "Avanzado" y "Panel Administrador" en un único botón de acceso intuitivo: `🛠️ Avanzado y Admin`.
    - **Diagnóstico y Control Maestro Integrados (`AdvancedSettingsCompose.kt` & `AdminSettingsCompose.kt`):** La pantalla presenta arriba el diagnóstico de conexiones en vivo (Google Drive y Firestore con botones interactivos de prueba) y abajo el Panel de Control Maestro protegido por contraseña (`"123"`).
    - **Monitoreo de Cuotas Firestore (Spark Plan):** Tarjeta informativa que detalla los límites diarios gratuitos (50.000 lecturas, 20.000 escrituras, 20.000 eliminaciones y reinicio a las 04:00 AM Chile / 00:00 PDT) junto con el botón retro `📈 VER MÉTRICAS EN VIVO EN FIREBASE` que abre con 1 solo toque el panel de estadísticas y gráficos oficiales en tiempo real en la consola de Firebase.
19. **Release v1.7.62 (Build 107) - Modo Híbrido Inteligente & Dual Ping para Thor Radar:**
    - **Dual Ping para 'Actualizar Ahora':** Disparo simultáneo de registro en Firestore (`locations/<coupleId>/pings/<partnerDocName>`) para respuesta instantánea (0ms) si la app está abierta, y paquete prioritario FCM Wake-on-LAN (`radar_ping` / `WOL_LOCATION_WAKEUP`) con `WakeLock` protegido para despertar el dispositivo en segundo plano.
    - **Rastreo Fluido en Primer Plano (`isForegroundTracking`):** Transmisión en vivo cada 8–12 segundos o $\ge 10\text{ metros}$ mientras la pantalla de Thor Radar permanece abierta, eliminando los congelamientos de pantalla.
    - **Ahorro Pasivo en Segundo Plano (`ThorRadarService.kt`):** Modo de bajo consumo con filtrado inteligente ($\ge 150\text{ metros}$ y $\ge 5\text{ minutos}$) y validación de frescura de Fix GPS para evitar coordenadas obsoletas con timestamps nuevos.

20. **Temporada 2 de Espíritus & Unificación de Base de Datos Firestore (v1.7.63):**
    - **Unificación de Base de Datos:** Migración y sincronización de colecciones entre Web y Android al proyecto oficial de Firebase `diario-ali-kevin` (`fortnite_spirits_s2/<coupleId>`), con 105 espíritus y 21 categorías por defecto.
    - **Normalización Unicode NFC de Vínculo:** Aplicación estricta de `Normalizer.normalize(coupleId, Normalizer.Form.NFC)` tanto en Kotlin como en JavaScript para evitar discrepancias de codificación en el ID de documento (`vínculo_único_123`).
    - **Carga de Imágenes Directa HTTPS (Cloudinary T2):** Carga optimizada vía Coil y Web desde Cloudinary (`https://res.cloudinary.com/dhaqjw7se/image/upload/f_auto,q_auto,w_180/spirits_s2/ic_spirit_s2_$id.png`).
    - **Categorías Nativas y Nombres Dinámicos:** Integración en `SpiritsCompose.kt` de `defaultCategoriesT2` (21 categorías) y nombres dinámicos calculados a partir del nombre de la categoría activa (`getSpiritDisplayName`).

21. **Auto-actualización Blindada & Edición Web Fluida (v1.7.64 - Build 109):**
    - **Blindaje del Ciclo de Vida en Android (`DiarioApp.kt`):** Inicialización de `firestoreSettings`, `MediaManager.init` y `Configuration.load(Osmdroid)` envuelta en bloques `try-catch` para prevenir cierres inesperados durante arranques en frío o tras actualizaciones del APK.
    - **Reinicio Automático Post-Actualización (`InstallResultReceiver.kt`):** Relanzamiento transparente de `MainActivity` con flags limpios (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`) tras completarse con éxito la instalación vía `PackageInstaller`.
    - **Edición Web de Categorías Fluida y Sin Pausas:** Protección reactiva en el listener `onSnapshot` de Firestore de la Web para evitar re-renderizados del DOM (`renderWorkspace()`) mientras el usuario está escribiendo (`isActivelyTyping`) o en ecos locales (`hasPendingWrites`), garantizando una experiencia de tipeo continua y sin pérdida de foco.

22. **Panel de Dispositivos Conectados & Control Remoto de Thor Radar (Web & App):**
    - **Pestaña `📡 Dispositivos & Radar` en la Web:** Nuevo panel de monitoreo y telemetría en tiempo real (`radar.html` / `switchModeSPA('radar')`) que visualiza el estado de conexión de los celulares de Kevin y Ali, porcentaje y estado de carga de batería, velocidad, actividad (`STILL`, `WALKING`, `IN_VEHICLE`), zonas seguras registradas, precisión GPS y visor de datos crudos en vivo.
    - **Interruptor Maestro Remoto de Thor Radar (ON / OFF):** Control interactivo desde la web (`toggleRemoteRadar`) para encender o apagar el radar en el celular de Kevin o Ali (`isSharing: true | false`).
    - **Escucha en Tiempo Real en Android (`MainActivity.kt` & `ThorRadarCompose.kt`):** El dispositivo escucha cambios en su documento `locations/<coupleId>/users/<user>` y, si se desactiva remotamente, suspende inmediatamente el Foreground Service (`ThorRadarService`), detiene el tracking GPS y actualiza el switch local sin requerir reiniciar la app.
    - **Herramientas de Pruebas Integradas:** Capacidad de enviar Magic Packets bajo demanda (`pings/<user>`) para forzar fijaciones GPS frescas y disparar/desactivar alertas de prueba SOS (`sosActive`).

23. **Diagnóstico y Blindaje de Thor Radar en Segundo Plano & FCM Magic Packet (v1.7.65):**
    - **Persistencia Robusta del Servicio en Background (`ThorRadarService.kt` / `AndroidManifest.xml`):** Configuración de `android:stopWithTask="false"` e implementación de `onTaskRemoved(rootIntent)` en `ThorRadarService` para asegurar que el Foreground Service continúe en ejecución y responda a pings y transiciones de geocercas aunque el usuario deslice la app de la lista de tareas recientes.
    - **Throttling Inteligente y Heartbeat en Reposo (`ThorRadarManager.kt`):** Se calibra el algoritmo de emisión de latidos para permitir un pulso ligero cada 20 minutos de inactividad o cuando el nivel de batería cambie significativamente ($\ge 15\%$) o se conecte/desconecte el cargador (tras al menos 5 min), evitando que el dispositivo parezca desconectado mientras se preserva estrictamente el cupo de Firestore (~3 escrituras por hora en reposo).
    - **Blindaje del Magic Packet FCM v1 & Recuperación de Token OAuth2:** Manejo robusto de credenciales de servicio con invalidación en caché (`invalidateGoogleCredentials`) y reintento automático si se recibe HTTP 401, corrección del fallback del `projectId` a `diario-ali-kevin`, y confirmación visual clara en la UI al pulsar *"Actualizar"* (`onPingPartner`).
    - **Unificación de Documentación:** Integración total de los diagnósticos y soluciones de `radar.md` directamente en la fuente única de verdad del proyecto.

24. **Migración Completa de Thor Radar a Firebase Realtime Database (RTDB) & Gasto 0 en Firestore (v1.7.67 - v1.7.68):**
    - **Migración de Telemetría a RTDB:** Toda la telemetría de ubicación (`latitude`, `longitude`, `accuracy`, `speedKmh`, `activity`, `address`, `currentZone`), control remoto (`isSharing`, `sosActive`) y pings se migró de Cloud Firestore a **Firebase Realtime Database** (`/locations/<coupleId>/users/{kevin|ali}` y `/locations/<coupleId>/pings/{kevin|ali}`).
    - **Gasto 0 en Cuota de Firestore:** Elimina el 100% de las operaciones de escritura del GPS y pings en Firestore, preservando intacto el límite de 20.000 escrituras diarias del Spark Plan.
    - **Latencia Ultrabaja (<50ms):** Comunicación por WebSockets nativos de Firebase RTDB para movimiento y telemetría en tiempo real sin recargar vistas.
    - **Reglas de Seguridad RTDB:** Configuración de lectura/escritura abierta para el nodo `locations` (`{ "rules": { "locations": { ".read": true, ".write": true } } }`).

25. **Resolución de Pantalla Negra / Congelamiento al Entrar desde Notificaciones (v1.7.69 - Build 114):**
    - **Causa Identificada:** `MainActivity.kt` ejecutaba `popBackStackImmediate` dentro de `window.decorView.post` durante las transiciones de ciclo de vida al recibir intents de notificaciones, y `showFragment` acumulaba fragmentos en la pila con `addToBackStack(null)`, provocando desincronización y desvinculación de `ComposeView` en `fragmentContainer`.
    - **Solución Implementada:**
      - `showFragment` reemplaza directamente el fragmento sin apilar transacciones redundantes de pestañas principales.
      - `navigateToClickType` utiliza `popBackStack` seguro y no bloqueante.
      - `onBackPressedDispatcher` y `btnHome` limpian y remueven explícitamente cualquier fragmento activo al regresar al feed principal, garantizando sincronización total de visibilidad (`fragmentContainer` vs `composeFeed`).

26. **Sincronización de Batería en Tiempo Real, Ping Silencioso Inteligente & Corrección Web RTDB (v1.7.69):**
    - **Receptor de Batería Instantáneo (`ThorRadarService.kt` / `ThorRadarCompose.kt`):** Registro de `BroadcastReceiver` dinámico para eventos de batería (`ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`). Ante cualquier cambio (conectar/desconectar cargador o variación de porcentaje), se actualiza inmediatamente el nodo RTDB mediante `ThorRadarManager.publishBatteryUpdate(context, level, isCharging)`.
    - **Cero Throttling en Cambios de Carga:** El paso de batería a cargando (o viceversa) se emite al instante sin esperar intervalos de tiempo.
    - **Ping Silencioso Inteligente (`isSilent`):** Al abrir el Radar o pulsar "Actualizar" cuando la última señal es reciente ($< 15\text{ min}$), se envía un Magic Packet WOL 100% silencioso (`silent: true`). Solo si la señal de la pareja es obsoleta ($\ge 15\text{ min}$ o desconectado) se despacha una notificación heads-up visible y sonora para solicitarle que despierte la app.
27. **Adaptación de Sincronización Web en Entornos Serverless / Vercel (`Cloud Sync: En vivo`):**
    - **Diagnóstico:** En hosting serverless como Vercel (`*.vercel.app`), no existe un proceso de servidor Node.js persistente para alojar endpoints custom de WebSockets (`/ws`). Esto causaba que la pastilla de estado mostrara permanentemente *"WS: Reconectando..."* con reintentos fallidos en bucle.
    - **Solución Implementada (`web/js/websocket.js` & `web/js/state.js`):**
      - Detección automática de entornos serverless/cloud (`vercel.app`, `web.app`, `firebaseapp.com`, `github.io`, `netlify.app`).
      - En entornos Serverless, la sincronización en tiempo real está 100% garantizada y gestionada por los WebSockets cloud nativos de **Firebase Firestore** y **Firebase Realtime Database (RTDB)**.
      - La UI muestra de forma transparente e inmediata `Cloud Sync: En vivo 🟢` con tooltip explicativo, anulando bucles de reconexión innecesarios en la nube mientras preserva el soporte de WebSockets locales en entornos de desarrollo Node (`localhost`).

28. **Resolución de Alertas de Llegada y Salida a Zonas Seguras en Thor Radar (v1.7.72):**
    - **Diagnóstico del Fallo:**
      1. *Filtro de precisión restrictivo (`accuracy > 65m`):* Al llegar a interiores (casa, trabajo, universidad), la señal GPS satelital cambia a triangulación WiFi/celular (~70–120m). El filtro anterior descartaba el 100% de las lecturas bajo techo impidiendo la detección de entrada a zonas de 100–200m.
      2. *Pérdida de Zonas en Memoria RAM:* Tras reinicios del servicio o suspensiones en segundo plano, `cachedZones` quedaba vacío en memoria sin recurrir al almacenamiento persistente local (`loadCachedZonesFromPrefs`).
      3. *Throttling de Telemetría RTDB:* Las transiciones de geocerca no sobrepasaban el throttling de telemetría pasiva (que requería >= 40m y >= 60s), retrasando la actualización del campo `currentZone` en RTDB.
      4. *Bloqueo de Re-ingreso por Estado Pegado:* Si una salida no alcanzaba los dos ciclos de confirmación antes de entrar en Doze mode, `last_active_zone_id` quedaba registrado en la zona anterior e impedía enviar la notificación al volver a entrar horas después.
    - **Solución Implementada (`ThorRadarManager.kt` & `MyFirebaseMessagingService.kt`):**
      - **Tolerancia Realista de Precisión:** Se incrementó el umbral de precisión a <= 130m, permitiendo la detección fiable de llegadas a interiores.
      - **Respaldo Automático de Caché:** Fallback automático a `loadCachedZonesFromPrefs(context)` en todas las evaluaciones de zonas si la memoria se reinició.
      - **Bypass Inmediato de Throttling:** Toda transición de zona (`ENTER` o `EXIT`) o cambio en `currentZone` dispara una sincronización inmediata forzada hacia Firebase Realtime Database.
      - **Control Robusto de Ciclo Fuera/Dentro (`was_outside_zone`):** Se implementó el flag de estado `was_outside_zone` y re-evaluación tras lapsos de inactividad, asegurando que regresar a una zona segura siempre despache la notificación push FCM prioritario a la pareja.
      - **Canal de Notificación Mejorado:** Configuración del canal con `IMPORTANCE_HIGH`, vibración y luces en `MyFirebaseMessagingService.kt`.

---

## 15. Tareas Pendientes / Backlog




