# Diario Kevin & Ali (Diario Alikevin) - Contexto General del Proyecto

Este documento sirve como la **Fuente Única de Verdad (Single Source of Truth)** para el proyecto Android **Diario**, diseñado como un espacio privado, interactivo y gamificado para parejas a la distancia. Proporciona una explicación detallada de la arquitectura, base de datos, flujos de trabajo clave, guías de desarrollo y reglas de negocio para que cualquier desarrollador o inteligencia artificial pueda entender el proyecto al 100% al instante.

---

## 1. Propósito y Visión General
**Diario** es una aplicación móvil nativa para Android diseñada exclusivamente para parejas. Resuelve la falta de espacios íntimos y compartidos al digitalizar recuerdos de amor mediante cuatro pilares funcionales:
1. **Diario compartido:** Envío de cartas y mensajes con imágenes con paginación progresiva y visualización en grilla (álbum).
2. **Calendario y Recetas:** Un calendario común para recordar aniversarios/citas (con alertas de aviso) y un recetario culinario de cocina compartido.
3. **Mascota Virtual (Thor):** Un sistema de gamificación en formato pixel-art en el que un gato virtual reacciona a la interacción diaria de la pareja, subiendo de nivel y desbloqueando ropa/accesorios en una tienda 3D.
4. **Sincronización Local-Nube (Google Drive)**: Respaldo y replicación automática bidireccional en segundo plano de la carpeta de fotos local seleccionada por cada usuario, sincronizando incluso eliminaciones entre dispositivos.
5. **Ficha Médica de Emergencia (Datos Vitales)**: Módulo interactivo dentro de la pantalla de Perfil con sincronización Firestore en tiempo real (`medical_records/<coupleId>`) que permite consultar y editar grupo sanguíneo, alergias, enfermedades/condiciones, medicación diaria, seguro médico y contacto de emergencia (con marcación directa `ACTION_DIAL`) de ambos integrantes de la pareja.

---

## 2. Stack Tecnológico y Arquitectura

- **Plataforma / Lenguajes:** Android Nativo.
  - **Kotlin:** Utilizado en prácticamente la totalidad de la lógica, ViewModel, pantallas de Jetpack Compose, Workers y utilidades en segundo plano.
  - **Java:** Utilizado en el controlador de la interfaz principal (`MainActivity.java`), que interactúa con la lógica moderna de Kotlin mediante `MainViewModel`.
- **UI Framework:**
  - **Jetpack Compose:** Sistema declarativo moderno para la totalidad de la interfaz de configuración, perfiles, cartas y grillas.
  - **Vanilla XML / ViewBinding:** En desuso, restringido a ciertos layouts legados.
- **Base de Datos y Backend:**
  - **Firebase Auth:** Gestión de inicio de sesión de los usuarios.
  - **Cloud Firestore:** Almacenamiento en tiempo real con persistencia offline integrada (caching local SQLite automática) para cartas, recetas, eventos e información de la mascota.
  - **Firebase Cloud Messaging (FCM):** Notificaciones push utilizando la API v1 mediante autenticación OAuth2.
- **Servicios Externos / APIs:**
  - **Google Drive API (v3):** Respaldo directo en la nube en una carpeta oculta (`DiarioAliKevin_Album`).
  - **Cloudinary:** Hosting cloud multimedia. Las fotos de las cartas se suben de forma firmada directamente a Cloudinary.
  - **GitHub API:** Localizada en [UpdateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/UpdateManager.kt) para verificar actualizaciones del APK e instalarlas automáticamente.
- **Colección de Espíritus / Coleccionables:**
  - **Checklist de Espíritus:** Un listado interactivo en el juego compuesto por **117 espíritus**. Su registro en código reside en `MiscCompose.kt` y sus activos de imagen pixel-art están almacenados en los recursos drawables. Además, el **Modo Edición** (activable desde el menú de 3 puntos) permite renombrar espíritus y categorías, mover espíritus a diferentes categorías (mediante un diálogo interactivo) y eliminar tanto espíritus como categorías de forma persistentemente y compartida guardando los cambios en Firestore (campos `custom_names`, `custom_categories`, `categories` y `spirits_list` dentro del documento `fortnite_spirits/<coupleId>`).
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
- [MainActivity.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.java): Contenedor principal. Orquesta las vistas de Compose observando los estados del ViewModel y delega la lógica de negocio.
- [LoginActivity.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/LoginActivity.kt): Gestión del login y asociación del `coupleId`.
- [MainViewModel.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainViewModel.kt): ViewModel central en Kotlin. Administra el estado global, listeners de Firestore en tiempo real, alarmas de calendario y toda la lógica de interacción/decay del pet Thor.

### 📁 Sincronización en Segundo Plano y Gestión de Archivos
- [SyncDriveWorker.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncDriveWorker.kt): Trabajador de primer plano (`CoroutineWorker` promovido a Foreground Service). Maneja la lógica de subir/descargar fotos pendientes de forma optimizada y control de borrados bidireccionales.
- [SyncScheduler.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncScheduler.kt): Orquestador de WorkManager. Agenda sincronizaciones periódicas (con restricciones de red Wi-Fi y carga eléctrica) o inmediatas bajo demanda.
- [DuplicateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DuplicateManager.kt): Gestor de detección y limpieza de imágenes duplicadas mediante pre-filtrado por tamaño y comparación MD5.

### 📁 Pantallas en Jetpack Compose (`compose/`)
- [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/MessageFeedCompose.kt): Pantalla principal del feed con paginación de 5 en 5 cartas, tarjeta interactiva de **Thor** (gato con animaciones y latido de corazón) y diálogo de la tienda de ropa.
- [SettingsSyncCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/SettingsSyncCompose.kt): Interfaz retro para gestionar la vinculación con Google Drive, configurar líneas de subida paralelas (1, 2, 3, 5) con barras de progreso concurrentes por slot de carga y visualización de contadores de archivos.
- [ProfileSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ProfileSettingsCompose.kt): Editor de perfil de la pareja (Kevin & Ali) con contador de tiempo juntos dinámico e interactivo.
- [MedicalCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MedicalCompose.kt): Módulo y diálogo retro de Ficha Médica de Emergencia con datos de salud, grupo sanguíneo, alergias y llamadas directas de emergencia.
- [RecipeCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RecipeCompose.kt) & [RecipeDetailCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/RecipeDetailCompose.kt): Creación y visualización del libro de recetas culinarias compartido.
- [CalendarCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/CalendarCompose.kt): Vista mensual de citas y eventos recurrentes (semanales/anuales) de la pareja.

---

## 4. Modelos de Datos y Entidades

Las entidades principales se guardan en Firebase Firestore y se representan como clases de datos en Kotlin:

### A. Mascota (`Pet.kt`)
Clase que define el estado de la mascota **Thor**:
- `happiness: Int` (Felicidad de 0 a 100).
- `level: Int` (Nivel actual, inicia en 1).
- `lovePoints: Int` (Monedas o puntos acumulados para gastar).
- `experience: Int` (Experiencia acumulada de 0 a 100).
- `streakDays: Int` (Racha de días interactuando).
- `lastInteraction: Long` (Timestamp del último contacto).
- `equippedAccessory: String?` (ID del accesorio activo).
- `unlockedAccessories: List<String>` (Colección de accesorios comprados).

### B. Cartas (`Message.kt`)
Publicaciones en la pantalla de inicio:
- `messageId: String?` (Clave en Firestore).
- `authorId: String?` / `authorName: String?` (Detalles de quién la redactó).
- `content: String?` (Texto principal de la carta).
- `imageUrls: MutableList<String>?` (Direcciones de Cloudinary para fotos adjuntas).
- `timestamp: Long` (Fecha de publicación).
- `liked: Boolean` (Estado de favorito de la carta mapeado a Firestore. Kotlin expone la propiedad delegada compatible `isLiked` y la de respaldo `isLikedFallback` para soportar esquemas antiguos).
- `type: String?` (`MESSAGE` o `ALBUM` si tiene imágenes).

### C. Recetas (`Recipe.kt`)
Recetas compartidas:
- `recipeId: String?` / `coupleId: String?` (Identificadores).
- `title: String?` (Nombre de la receta).
- `ingredients: String?` / `steps: String?` (Textos descriptivos).
- `imageUrl: String?` (Foto de la comida en Cloudinary).

### D. Eventos (`CalendarEvent.kt`)
Citas compartidas con soporte de alertas automáticas:
- `eventId: String` / `partnerId: String` (Claves de enlace).
- `title: String` / `description: String` (Textos del evento).
- `date: Long` (Timestamp del día).
- `recurrence: String` (`NONE`, `WEEKLY`, `YEARLY`).

### E. Metadatos de Sincronización (`SyncMetadata` en `SyncDriveWorker.kt`)
Documentos guardados en `pets/<coupleId>/drive_sync_metadata/`:
- `idLocal: String` (Nombre de la foto local).
- `idDrive: String` (ID del archivo en Google Drive).
- `nombreArchivo: String` (Clave identificatoria).
- `uriLocal: String` (Ruta SAF en el dispositivo).
- `md5Checksum: String` (Valor MD5 para comprobar cambios en el binario).
- `fechaModificacion: Long` (Timestamp local de última escritura).
- `sincronizadoPor: String` (UID del usuario).
- `eliminado: Boolean` (Marca *tombstone* de borrado bidireccional).

### F. Colección de Espíritus Fortnite (`fortnite_spirits/<coupleId>`)
Documento único en Firestore que almacena el progreso, catálogo y estado completo de espíritus:
- `schema_version: Int` (Versión del esquema, actualmente `4`).
- `categories: Map<String, SpiritCategory>` u `Array` (Estructura de categorías. Cada clave es el nombre de la categoría y contiene `{ name: String, spiritIds: List<String> }`).
- `spirits_list: List<String>` (Lista de IDs registrados en el sistema, ej: `["01", "02", ..., "145", "146"]`).
- `kevin_list: List<String>` / `ali_list: List<String>` (Espíritus obtenidos por Kevin y Ali).
- `kevin_mastery: List<String>` / `ali_mastery: List<String>` (Espíritus con maestría completada por Kevin y Ali).
- `custom_names: Map<String, String>` (Renombrados de espíritus por ID, ej: `{ "13": "Cacahuete", "119": "Pollo" }`).
- `custom_categories: Map<String, String>` (Alias personalizados para nombres de categorías).
- `custom_images: Map<String, String>` (URLs personalizadas de Cloudinary para imágenes de espíritus, ej: `{ "143": "https://..." }`).
- `spirit_types: List<Map<String, String>>` (Tipos o variantes de espíritus, ej: `{ name: "Dorado", suffix: " Dorado" }`).

---

## 5. El Sistema de Gamificación de "Thor"

El motor de fidelización gira en torno a la mascota virtual de la pareja:

```mermaid
graph TD
    User([Interacciones de Usuario]) -->|Enviar Mensajes / Abrir App| XP[+10 XP]
    User -->|Interacción Manual| LP[+5 Puntos de Amor]
    XP -->|Cada 100 XP| LevelUp[Subir de Nivel +50 Puntos de Amor]
    LP -->|Comprar Accesorios| Shop[Tienda de Thor]
    Shop -->|Desbloquear| Equip[Equipar Accesorio]
    Time([Transcurso del Tiempo]) -->|Cada 24 horas| Decay[-20% Felicidad]
```

### A. Sistema de Progresión y Puntos
- **Interacciones:** Cada carta enviada o interacción con Thor otorga **+10 XP** y **+5 Puntos de Amor**.
- **Racha Diaria:** Mantener la interacción consecutiva incrementa el multiplicador de racha, otorgando un bono de `Racha * 2` Puntos de Amor adicionales por día.
- **Subida de Nivel:** Al acumular **100 XP**, Thor sube de nivel (`level++`), la EXP se reinicia y se premia a los usuarios con un bono de **+50 Puntos de Amor**.

### B. Tienda de Accesorios (Shop UI)
Desde el diálogo de Thor, los usuarios gastan Puntos de Amor para desbloquear y equipar 11 accesorios pixel-art. Las imágenes finales están generadas a la medida y con transparencia completa para superponerse perfectamente sobre el cuerpo del gato:
- **Collar Cascabel 🔔** (Coste: 10 Puntos)
- **Bigote Retro 🥸** (Coste: 30 Puntos)
- **Globo Corazón 🎈** (Coste: 60 Puntos)
- **Lazo Rosa 🎀** (Coste: 80 Puntos)
- **Gorrito Pixel 🎩** (Coste: 100 Puntos)
- **Pañuelo Pirata 🏴‍☠️** (Coste: 120 Puntos)
- **Lentes Cool 🕶️** (Coste: 150 Puntos)
- **Corona Real 👑** (Coste: 500 Puntos)
- **Plátano Nano 🍌** (ID: `banana`)
- **Calcetas y Botitas 🧦🥾** (ID: `socks`)

### C. Algoritmo de Decay y Temporizador en Tiempo Real
- **Desgaste de Felicidad:** Si la pareja no interactúa en un periodo de 24 horas, la felicidad de Thor disminuye en un **20%**. 
- **Prevención de Bucle Recursivo:** El sistema matemático en [MainViewModel.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainViewModel.kt) compensa el timestamp de la última interacción (`lastInteraction`) sumando bloques exactos de 24 horas por cada decaimiento aplicado. Esto previene llamadas en cascada infinitas del Snapshot Listener de Firebase.
- **Temporizador en Tiempo Real:** Implementado con una corrutina y `LaunchedEffect` en [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt), calcula y muestra una cuenta regresiva dinámica en formato `"Próxima baja en: hh:mm"` / `"mm:ss ⏳"` que avisa cuándo ocurrirá el próximo decremento, o muestra un dulce aviso `"¡Dale amor! ❤️"` si Thor ha alcanzado el 0% de felicidad.

### D. Guía de Creación e Integración de Nuevos Accesorios / Ropa
Para añadir nuevos accesorios (como el *Plátano Nano* o las *Calcetas y Botitas*), se debe seguir un protocolo estructurado de 5 pasos para garantizar la integridad visual e interactiva:

1. **Generación del Asset Visual (AI + Transparencia Flood-Fill):**
   - **Imagen de Thor Vestido:** Generar la imagen del gato utilizando el diseño base [ic_thor_base_trans.png](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/res/drawable/ic_thor_base_trans.png) sobre un fondo blanco sólido (`#FFFFFF`) con el prompt deseado (ej. suéter, calcetas, sombrero).
   - **Icono de Tienda:** Generar una miniatura del accesorio individual (`ic_acc_<nombre>_raw.png`) sobre fondo blanco.
   - **Procesamiento de Transparencia:** Ejecutar un script de Python con algoritmo *Flood Fill* (partiendo de las 4 esquinas de la imagen) para remover el fondo blanco sin afectar las partes blancas del cuerpo de Thor, guardando los resultados PNG transparentes finales como `ic_thor_<nombre>.png` y `ic_acc_<nombre>.png` en `app/src/main/res/drawable/`.
2. **Definición de Constantes:**
   - Registrar la nueva constante en el companion object de [Pet.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/Pet.kt):
     ```kotlin
     const val ACC_SOCKS = "socks"
     ```
3. **Mapeo de Recursos en Compose:**
   - Añadir el recurso de imagen correspondiente en los bloques `when (pet.equippedAccessory)` ubicados en [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt) (tanto en el renderizador principal de Thor como en el diálogo de información):
     ```kotlin
     Pet.ACC_SOCKS -> R.drawable.ic_thor_socks
     ```
4. **Registro en el Listado de la Tienda:**
   - Agregar la tupla descriptiva a la lista `items` en la pestaña de tienda dentro de [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/MessageFeedCompose.kt) definiendo el ID, nombre para mostrar y precio en puntos de amor:
     ```kotlin
     Triple(Pet.ACC_SOCKS, "Calcetas y Botitas 🧦🥾", 15)
     ```
5. **Enlace en el Widget de Pantalla de Inicio:**
   - Mapear el nuevo accesorio en [ThorWidgetProvider.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorWidgetProvider.kt) para mantener paridad visual fuera de la app:
     ```kotlin
     "socks" -> R.drawable.ic_thor_socks
     ```

---

## 6. Flujos de Trabajo y Sincronización (Workflows)

### A. Flujo de Gamificación (Thor Decay & Loop)
- Cada 24 horas sin mensajes de la pareja, la felicidad de Thor baja 20%. Para evitar llamadas consecutivas que sobrecarguen Firestore, al aplicar la penalización se compensa la fecha de la última interacción sumando exactamente bloques de 24 horas.

### B. Flujo de Sincronización Optimizada de Fotos (Google Drive)
1. **Resolución de la carpeta**: Se lee `syncDriveFolderId` de `DiarioPrefs`. Si no está en caché, se hace el query a Drive y se guarda.
2. **Consulta Rápida**: Se listan las fotos locales usando el `ContentResolver` (milisegundos) y se descargan los metadatos de Firestore. **Se omite el listado total de Drive** por rendimiento.
3. **Reconciliación de Borrados**: 
   - Si una foto en metadatos no existe localmente, se borra de Drive y se marca `eliminado = true` in Firestore (Borrado local replicado en la nube).
   - Si un archivo local existe pero sus metadatos tienen `eliminado == true`, la app borra la foto física localmente (Borrado remoto replicado en el local).
4. **Carga en Slots Paralelos**: Las subidas y descargas pendientes se procesan de acuerdo al selector de líneas concurrentes (1, 2, 3, o 5) usando coroutines y un `Semaphore`.
5. **Cálculo Perezoso de MD5 (Lazy MD5 Checksum)**: Para evitar cuellos de botella de I/O, el hash MD5 local solo se calcula cuando se sospecha un cambio (comparando timestamps locales contra los metadatos de Firestore) o para archivos totalmente nuevos, omitiendo el proceso en archivos que ya están sincronizados o marcados para eliminación.

---

## 7. Reglas de Negocio y Restricciones Técnicas Importantes
- **Optimizaciones SAF**: La API por defecto `DocumentFile.listFiles()` de Android es extremadamente lenta porque inicializa objetos pesados para cada archivo en la carpeta. En su lugar, el proyecto utiliza consultas directas por cursor a través de `contentResolver.query`, solicitando la proyección de datos mínimos. El listado se ejecuta una sola vez al inicio y se opera en memoria.
- **Detección Eficiente de Duplicados**: Para acelerar la búsqueda de duplicados locales, se realiza un pre-filtrado agrupando archivos por tamaño (`size`). Solo se calcula el hash MD5 para aquellos archivos que comparten un tamaño idéntico con algún otro, reduciendo drásticamente las lecturas I/O de disco.
- **Seguridad en Descriptores de Archivos**: Todas las funciones de hashing MD5 (`DuplicateManager` y `SyncDriveWorker`) implementan cierres automáticos de recursos utilizando bloques `.use` para asegurar la liberación de descriptores de archivos (`InputStream`) y evitar fugas de memoria o bloqueos.
- **Firma Digital y Credenciales de Google**: La firma de la aplicación reside en el almacén de llaves `diario_keystore.jks` con alias `diario_alias`. En caso de regeneración de llaves, se deben registrar las nuevas huellas SHA-1 y SHA-256 en la Consola de Firebase y en la consola de APIs de Google Cloud para que la sincronización con Google Drive y la autenticación funcionen.
- **Conciliación de Contadores y Autodetección de Sincronización**: Si el conteo de fotos locales difiere del conteo en la nube (Firestore), la interfaz de sincronización muestra **ESTADO: NO SINCRONIZADO 🔴**. En cambio, cuando los conteos de la carpeta local coinciden reactivamente con los metadatos de Firestore y no existen errores ni procesos activos, Compose evalúa dinámicamente el estado como **ESTADO: SINCRONIZADO 🟢** evitando falsas alertas al reabrir la app.
- **Tienda de Mascota**: Cada accesorio comprado en la tienda de Thor se descuenta de los `lovePoints` acumulados en Firestore y se agrega al array de compras permanentes. Los assets visuales se superponen de forma transparente sobre la imagen base de la mascota.

---

## 8. Control de Versiones y Despliegues (CI/CD Automatizado)

Cada cambio que deba publicarse a producción sigue este proceso de despliegue automatizado mediante **GitHub Actions**:
* **Paso 1 (Versionado Obligatorio):** Siempre que el usuario solicite subir una release, es **OBLIGATORIO** incrementar las propiedades `versionCode` y `versionName` en [app/build.gradle.kts](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/build.gradle.kts) antes de crear el tag. Esto garantiza que la comprobación de actualizaciones de la app no entre en un bucle infinito de solicitud de actualización.
* **Paso 2 (Confirmación de Cambios):** Hacer commit y push de los cambios de código y versionado a la rama `master`.
* **Paso 3 (Disparador de Compilación via GitHub Actions):** Crear y empujar el tag `v<versionName>` (ej. `v1.7.30`) a `origin`. Esto activa automáticamente el workflow `.github/workflows/android.yml`.
* **Paso 4 (Ejecución en GitHub Actions):** GitHub Actions compila y firma la APK en la nube (`app-release.apk`) y crea la GitHub Release automáticamente.

---

## 9. Conexión de Pruebas Multidispositivo (Script ADB + Gradle)

Para simplificar el proceso de pruebas locales, el script interactivo [conectar_adb.sh](file:///home/kevin/Escritorio/conectar_adb.sh) en el Escritorio gestiona el ciclo completo de conexión y despliegue:
* **Detección Automática y Vinculación por Código QR:** Escanea dispositivos USB activos, descubre servicios mDNS (Avahi) y permite vincular dispositivos inalámbricos escaneando un **Código QR generado directamente en la terminal** (o mediante código tradicional de 6 dígitos).
* **Multiselección:** Mediante una lista de selección múltiple (Zenity Checklist), el usuario puede marcar exactamente qué dispositivos (USB o red) quiere desplegar en paralelo.
* **Bucle de Instalación Directa:** El script exporta de forma secuencial la variable `ANDROID_SERIAL` para cada dispositivo seleccionado y ejecuta la compilación con `./gradlew`. La primera ejecución compila la app y las siguientes se benefician de la caché de Gradle, resultando en instalaciones casi instantáneas.

---

## 10. Web de Gestión de Espíritus, Servidor Node.js y Despliegue en Vercel

Se incluye una **Web de Gestión** (`web/index.html`) para permitir la administración visual, masiva y cómoda de los 117 espíritus desde una computadora o navegador móvil:
* **Interactividad y Diseño:** Diseñada con estética premium oscura (`glassmorphic` y tipografías Outfit/Inter). Dispone de un listado lateral con las imágenes de los espíritus y un panel central con la estructura de categorías y tipos (Normal, Dorado, Gomita, etc.).
* **Cambio de Categoría y Tipo:** Permite arrastrar fotos (`drag and drop`) o usar los desplegables de cada tarjeta para mover un espíritu a cualquier categoría o alternar su variante.
* **Arquitectura de Servidor Local y Producción (Node.js / Express / Vercel Serverless):**
  * **Entorno Local (`web/server.js`):** Servidor Express de Node.js corriendo localmente (`npm start` en la carpeta `web/`) que sirve la web estática en `http://localhost:8000` y gestiona las subidas de imágenes a Cloudinary usando el SDK de Node y `.env`.
  * **Producción en Vercel (`web/api/upload-spirit-image.js`):** Función Serverless nativa de Vercel en Node.js que recibe las subidas de imágenes en Base64, las firma y sube de forma segura a Cloudinary mediante las variables de entorno de Vercel (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`).
* **Compresión Inteligente en Cliente y Feedback Visual:**
  * **Compresión Canvas:** Las imágenes seleccionadas se comprimen automáticamente en el navegador mediante HTML5 Canvas (máximo 1200px de ancho y calidad JPEG al 82%), reduciendo archivos pesados a ~300-800KB para mantenerse holgadamente por debajo del límite de payload de Vercel (4.5MB).
  * **Overlay de Carga:** Durante la subida y guardado, se muestra un indicador visual animado (Spinner con estado por pasos: *Comprimiendo -> Subiendo -> Guardando en Firestore*) que bloquea interacciones hasta confirmar que los datos se persistieron correctamente en la nube.
* **Sincronización Estricta de Orden de Categorías:**
  * El guardado en Firestore almacena la lista `categories` como un arreglo JSON ordenado (`Array [...]`), eliminando desordenamientos por objetos asociativos.
  * El algoritmo de lectura en la web y en la app Android concilian la lista manteniendo exactamente el orden secuencial del arreglo definido por la app.

