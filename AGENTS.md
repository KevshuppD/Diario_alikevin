# Reglas y Guías de Desarrollo del Proyecto (Diario Ali & Kevin)

## 📱 1. Reglas de UI, Layouts y Prevención de Desbordamiento (Overflow)
- **Evitar desbordamiento de pantalla (Anti-Overflow)**: 
  - Todo contenedor desplazable (`verticalScroll`, `LazyColumn`, etc.) dentro de un `Dialog`, `BottomSheet` o `Column` principal **DEBE** usar `Modifier.weight(1f)` (o estar debidamente acotado) para asegurar que el contenido se desplace dentro del viewport visible y no se desborde fuera de la pantalla.
- **Márgenes de seguridad para Barras del Sistema (Safe Insets)**:
  - Siempre contemplar `navigationBarsPadding()` y `statusBarsPadding()` en pantallas completas y diálogos a pantalla completa.
  - **Margen inferior obligatorio en scrolls**: Al final de cualquier vista desplazable o diálogo interactivo, añadir siempre un espaciado inferior generoso (ej: `Spacer(modifier = Modifier.height(70.dp..80.dp))` o `contentPadding = PaddingValues(bottom = 70.dp)` en `LazyColumn`) para garantizar que ningún botón, tarjeta o texto quede tapado por la barra de navegación, gestos o botones físicos del celular del usuario.
- **Sin botones o acciones redundantes**: Mantener las vistas limpias; si una acción o navegación ya existe en la barra de pestañas superior o en el flujo principal, no duplicar botones redundantes en pestañas secundarias.

---

## 🎨 2. Estilo Visual y Estética Pixel-Art
- **Tipografía**: Usar la fuente `Vt323` para elementos retro / Tamagotchi / arcade.
- **Geometría y Bordes**: Utilizar `RectangleShape`, bordes pixelados marcados (`border(2.dp, borderColor)`) y sombras sólidas con offset.
- **Temas y Colores**: Paleta retro pastel / arcade cálida (dorado/ámbar `#D97706`, rosa vibrante `#EC4899`, azul vibrante `#2563EB`, fondos cálidos pergamino / dark theme `#1A1A1A`).

---

## 🐾 3. Sistema de Mascotas Virtuales (Thor & Cuky)
- **Estadísticas 100% Independientes y Getters Dinámicos**: 
  - Las mascotas (**Thor** y **Cuky**) tienen estados, niveles, puntos de experiencia (`experience` vs `cukyExperience`), hambre, sueño, felicidad e interacción independientes.
  - **Regla obligatoria de lectura de estados**: En toda la UI (`PetCard`, `PetMenuDialog`, minijuegos, widgets) **NUNCA** acceder a propiedades directas de Thor (`pet.name`, `pet.isSleeping`, `pet.level`, `pet.happiness`, `pet.hunger`, `pet.cleanliness`, `pet.streakDays`). **SIEMPRE** utilizar los getters de mascota activa: `pet.getActiveName()`, `pet.getActiveIsSleeping()`, `pet.getActiveLevel()`, `pet.getActiveHappiness()`, `pet.getActiveExperience()`, `pet.getActiveHunger()`, `pet.getActiveCleanliness()`, `pet.getActiveStreak()`, `pet.getActiveEquippedAccessory()`, `pet.getActiveEquippedBackground()`, etc.
  - Al alimentar, bañar, jugar o subir de nivel a una mascota, las modificaciones solo deben impactar a la mascota activa (`pet.isCuky()` vs Thor).
- **Ranking de Cuidadores Separado**:
  - El sistema de cuidados registra individualmente los puntos y contadores por mascota (`...Thor` y `...Cuky`) y total global, permitiendo a Kevin y Ali filtrar su historial tanto globalmente como por cada mascota.
- **Minijuegos Generales y Adaptativos**:
  - Los títulos, íconos y textos de minijuegos (Flappy Pet, Snake, Memory) deben ser adaptativos a la mascota activa (`"🐔 FLAPPY CUKY 🪽"` vs `"🐱 FLAPPY THOR 🪽"`, `"${pet.getActiveName().uppercase()} SNAKE"`, `"${pet.getActiveName().uppercase()} POCKET™"`).
  - Los sprites deben cargarse según el tipo de mascota activa (`getPetDrawableRes(pet)` o `pet.isCuky()`).

---

## ⚡ 4. Rendimiento en Animaciones y Minijuegos (Anti-Lag)
- **Animaciones en Draw Phase**:
  - Para animaciones continuas en Compose (respiración, bamboleo, traslaciones periódicas), usar **siempre** la versión lambda `Modifier.graphicsLayer { ... }` en lugar de `Modifier.graphicsLayer(...)`. Esto evita recomposiciones del árbol Compose en cada frame (60/120 FPS) delegando la transformación directamente a la GPU.
- **Cero Alojamientos en `DrawScope`**:
  - En funciones de dibujo Canvas o `withFrameNanos`, **nunca** instanciar arrays, listas o matrices (`arrayOf(intArrayOf(...))`) dentro del ciclo de renderizado. Definirlos siempre como constantes estáticas a nivel superior (`private val ..._MATRIX`) para no saturar el Garbage Collector (GC).

---

## ☁️ 5. Gestión de Cuotas y Límites de Base de Datos (Cloud Firestore)
- **Límites Spark Plan**: Firebase Spark permite un máximo de **20.000 escrituras y 50.000 lecturas por día**. Se reinicia automáticamente a las **00:00 PDT (04:00 AM hora de Chile / UTC-3)**.
- **Arquitectura On-Demand Estricta (Gasto 0 en Reposo)**:
  - **Cero escrituras en segundo plano periódicas**: Prohibido ejecutar bucles de latido por tiempo (`delay`), receivers de batería o rastreadores GPS de alta frecuencia en segundo plano en `ThorRadarService`.
  - **Magic Packet Wake-on-LAN vía FCM push**: La solicitud de sincronización se realiza mediante FCM Push prioritario de alta velocidad (0 escrituras y 0 costo en Firestore).
  - **1 sola escritura por evento**: Al abrir el radar o recibir un ping Magic Packet, el dispositivo despierta, adquiere fix GPS fresco y emite exactamente **1 escritura** a Firestore.
  - **Filtro de movimiento pasivo**: Solo emitir escrituras pasivas si hubo desplazamiento real significativo ($\ge 300\text{ metros}$) y pasaron al menos 10 minutos.
  - **Historial acotado**: Solo registrar puntos de historial (`history_`) con desplazamiento real $\ge 300\text{ metros}$ y al menos 10 minutos.

---

## 🏷️ 6. Despliegues y Creación de Tags en Git
- **Incremento Obligatorio de Versión (`versionCode` & `versionName`)**:
  - Cada vez que el usuario indique subir o crear un tag (ej: `"sube tag"`, `"crea tag"`, `"nueva versión"`), es **OBLIGATORIO** incrementar previamente `versionCode` (+1) y `versionName` en `app/build.gradle.kts`.
  - Primero se modifican las versiones en Gradle, luego se hace commit y finalmente se crea y empuja el tag (`v<versionName>`). Esto evita que Android no detecte la actualización o vuelva a pedir instalarla.

---

## 🛡️ 7. Salvaguarda Estricta de Espíritus (Checklist & Firestore)
- **Cero Sobreescritura Destructiva de Selecciones y Maestrías**:
  - Queda terminantemente prohibido vaciar o sobreescribir `kevin_list`, `ali_list`, `kevin_mastery`, `ali_mastery` en `fortnite_spirits` y `fortnite_spirits_s2`.
  - Toda modificación de categorías o nombres debe preservar intactos los datos de usuario existentes.
- **Backup Obligatorio Pre-Escritura**:
  - Antes de ejecutar cualquier script que modifique Firestore en colecciones de espíritus, es **OBLIGATORIO** ejecutar `python3 scripts/backup_firestore_spirits.py`.
- **Catálogo Maestro**:
  - Consultar siempre `espiritus.md` como la Fuente Única de Verdad de nombres, categorías, IDs y enlaces directos a Fortnite.GG.

---

## 🚫 8. Prohibido Eliminar Funciones sin Autorización Explícita
- **Ninguna IA puede quitar, ocultar ni reemplazar una opción, botón, pantalla o función existente** a menos que el usuario lo pida explícitamente en ese mismo mensaje. Refactorizar, "limpiar", reorganizar u optimizar **no** autoriza a eliminar funcionalidad.
- **Al reorganizar una pantalla** (ej. `ProfileSettingsCompose.kt`), cada elemento de la UI anterior debe seguir existiendo en la nueva versión. Antes de hacer commit, revisar `git diff` buscando líneas `Text(`, `Checkbox(`, `Switch(`, `Button(` eliminadas y confirmar que cada una fue movida, no borrada.
- **Señal de alerta**: un parámetro de un Composable que ya no se usa en su cuerpo (ej. `showTopBar` / `onShowTopBarChange` que quedaron sin casilla en la v1.7.54–1.7.81) suele indicar una función perdida. No borrar el parámetro "por limpieza": restaurar la UI o preguntar al usuario.
- **Si una eliminación parece necesaria**, proponerla y esperar confirmación; luego documentarla en `contexto.md` (qué se quitó, cuándo y por pedido de quién).
- **Inventario de funciones**: `contexto.md` (sección *Inventario de Opciones Visibles*) lista las opciones que deben existir. Si se agrega o quita una con autorización, actualizar ese inventario en el mismo commit.

