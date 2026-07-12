# Diario Kevin & Ali (Diario Alikevin) - Contexto General del Proyecto

Este documento sirve como la **Fuente Única de Verdad (Single Source of Truth)** para el proyecto Android **Diario**, diseñado como un espacio privado, interactivo y gamificado para parejas a la distancia. Proporciona una explicación detallada de la arquitectura, base de datos, flujos de trabajo clave, guías de desarrollo y reglas de negocio para que cualquier desarrollador o inteligencia artificial pueda entender el proyecto al 100% al instante.

---

## 1. Propósito y Visión General
**Diario** es una aplicación móvil nativa para Android diseñada exclusivamente para parejas. Resuelve la falta de espacios íntimos y compartidos al digitalizar recuerdos de amor mediante cuatro pilares funcionales:
1. **Diario compartido:** Envío de cartas y mensajes con imágenes con paginación progresiva y visualización en grilla (álbum).
2. **Calendario y Recetas:** Un calendario común para recordar aniversarios/citas (con alertas de aviso) y un recetario culinario de cocina compartido.
3. **Mascota Virtual (Thor):** Un sistema de gamificación en formato pixel-art en el que un gato virtual reacciona a la interacción diaria de la pareja, subiendo de nivel y desbloqueando ropa/accesorios en una tienda 3D.
4. **Sincronización Local-Nube (Google Drive)**: Respaldo y replicación automática bidireccional en segundo plano de la carpeta de fotos local seleccionada por cada usuario, sincronizando incluso eliminaciones entre dispositivos.

---

## 2. Stack Tecnológico y Arquitectura

- **Plataforma / Lenguajes:** Android Nativo.
  - **Kotlin:** Utilizado en el 100% de la lógica moderna, pantallas de Jetpack Compose y Workers en segundo plano.
  - **Java:** Utilizado en inicializadores de infraestructura heredados (ej. `MainActivity`, `DiarioApp`, `LoginActivity`).
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
  - **Checklist de Espíritus:** Un listado interactivo en el juego compuesto por **97 espíritus** (originalmente 65, expandido a 97 con las variantes Gema y Holofoil). Su registro en código reside en `MiscCompose.kt` y sus activos de imagen pixel-art están almacenados como `ic_spirit_1.png` a `ic_spirit_97.png` en los recursos drawables.
- **Estilos y UI:**
  - Estética inmersiva **Retro Pixel-Art de 8 y 16 bits**.
  - Tipografía pixelada `vt323` importada globalmente.
  - Soporte a tres temas visuales dinámicos: **Pixel Claro** (crema y chocolate), **Pixel Oscuro** (gris profundo y neón rosa) y **Pixel Monocromático** (blanco y negro puro).

---

## 3. Estructura del Proyecto y Archivos Clave

El código fuente está localizado en `app/src/main/java/calendario/kevshupp/diariokevinali/`.

### 📁 Inicialización e Infraestructura
- [DiarioApp.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DiarioApp.java): Punto de entrada. Configura las instancias globales de Cloudinary, WorkManager (para tareas en segundo plano), Firebase y Coil (carga de imágenes).
- [MainActivity.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.java): Contenedor principal. Orquesta los listeners en tiempo real de Firebase y hace de puente entre los Fragments de Java y el entorno de Jetpack Compose.
- [LoginActivity.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/LoginActivity.java): Gestión del login y asociación del `coupleId`.

### 📁 Sincronización en Segundo Plano y Gestión de Archivos
- [SyncDriveWorker.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncDriveWorker.kt): Trabajador de primer plano (`CoroutineWorker` promovido a Foreground Service). Maneja la lógica de subir/descargar fotos pendientes de forma optimizada y control de borrados bidireccionales.
- [SyncScheduler.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/SyncScheduler.kt): Orquestador de WorkManager. Agenda sincronizaciones periódicas (con restricciones de red Wi-Fi y carga eléctrica) o inmediatas bajo demanda.
- [DuplicateManager.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DuplicateManager.kt): Gestor de detección y limpieza de imágenes duplicadas mediante pre-filtrado por tamaño y comparación MD5.

### 📁 Pantallas en Jetpack Compose (`compose/`)
- [MessageFeedCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/MessageFeedCompose.kt): Pantalla principal del feed con paginación de 5 en 5 cartas, tarjeta interactiva de **Thor** (gato con animaciones y latido de corazón) y diálogo de la tienda de ropa.
- [SettingsSyncCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/SettingsSyncCompose.kt): Interfaz retro para gestionar la vinculación con Google Drive, configurar líneas de subida paralelas (1, 2, 3, 5) con barras de progreso concurrentes por slot de carga y visualización de contadores de archivos.
- [ProfileSettingsCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/ProfileSettingsCompose.kt): Editor de perfil de la pareja (Kevin & Ali) con contador de tiempo juntos dinámico e interactivo.
- [RecipeCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/RecipeCompose.kt) & [RecipeDetailCompose.kt](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/compose/RecipeDetailCompose.kt): Creación y visualización del libro de recetas culinarias compartido.
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
- `isLiked: Boolean` (Estado de favorito de la carta).
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
- **Prevención de Bucle Recursivo:** El sistema matemático en [MainActivity.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.java) compensa el timestamp de la última interacción (`lastInteraction`) sumando bloques exactos de 24 horas por cada decaimiento aplicado. Esto previene llamadas en cascada infinitas del Snapshot Listener de Firebase.
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
   - Mapear el nuevo accesorio en [ThorWidgetProvider.java](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorWidgetProvider.java) para mantener paridad visual fuera de la app:
     ```java
     } else if ("socks".equals(accessory)) {
         thorImageRes = R.drawable.ic_thor_socks;
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
- **Conciliación de Contadores**: Si el conteo de fotos locales difiere del conteo en la nube (Firestore), la interfaz de sincronización invalida automáticamente el estado y muestra **ESTADO: NO SINCRONIZADO 🔴** para advertir al usuario.
- **Tienda de Mascota**: Cada accesorio comprado en la tienda de Thor se descuenta de los `lovePoints` acumulados en Firestore y se agrega al array de compras permanentes. Los assets visuales se superponen de forma transparente sobre la imagen base de la mascota.

---

## 8. Control de Versiones y Despliegues (CI/CD Automatizado)

Cada cambio que deba publicarse a producción sigue este proceso de despliegue automatizado mediante **GitHub Actions**:
* **Paso 1 (Versionado):** Incrementar las propiedades `versionCode` y `versionName` en el archivo [app/build.gradle.kts](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/build.gradle.kts).
* **Paso 2 (Confirmación de Cambios):** Hacer commit y push de todos los cambios de código a la rama `master`.
* **Paso 3 (Disparador de Compilación - Solo a demanda):** Para un uso eficiente de los corredores de Actions, los tags de versión que comienzan con `v` se empujan únicamente cuando se completa un hito importante y a petición explícita del usuario, evitando disparar lanzamientos continuos para cambios mínimos:
  ```bash
  git tag v1.7.11
  git push origin v1.7.11
  ```
* **Paso 4 (Ejecución en GitHub Actions):** El empuje de la etiqueta activará automáticamente el workflow en la nube definido en `.github/workflows/android.yml`.
  - El pipeline decodifica de forma segura las credenciales Base64 guardadas en los secretos de GitHub (`DIARIO_KEYSTORE_BASE64` y `GOOGLE_SERVICES_JSON_BASE64` actualizados en julio de 2026), limpiando automáticamente posibles saltos de línea con `tr -d '\r\n '`.
  - Compila la aplicación, firma el APK de lanzamiento (Release APK), crea un lanzamiento oficial en GitHub con el nombre de la versión y adjunta el archivo `app-release.apk` firmado listo para descargar.

---

## 9. Conexión Inalámbrica de Pruebas (ADB sobre Wi-Fi)

Para conectar inalámbricamente el entorno de desarrollo al celular del usuario durante las pruebas locales (ya sea para instalar la versión Debug o Release), se debe ejecutar el siguiente comando que autodetecta y conecta el dispositivo por red local utilizando mDNS (Avahi):
```bash
adb connect $(avahi-browse -rtp _adb-tls-connect._tcp -t 2>/dev/null | grep ^= | cut -d';' -f8,9 --output-delimiter=: | head -n1)
```
