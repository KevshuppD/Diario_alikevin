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
- **Smart Throttling Obligatorio**:
  - **Prohibido el polling agresivo de escritura**: Nunca ejecutar bucles de escritura a Firestore con intervalos menores a 15–30 segundos (`ThorRadarCompose`, `ThorRadarService`).
  - **Filtro de movimiento y batería**: Solo emitir escrituras automáticas a Firestore si hubo desplazamiento significativo ($\ge 20\text{ metros}$), cambio de batería significativo ($\ge 3\%$), o si han pasado al menos 60 segundos en reposo.
  - **Listeners GPS**: Configurar siempre `minUpdateDistanceMeters` $\ge 10\text{m}$ para evitar que el ruido/jitter del GPS dispare escrituras cuando el dispositivo está quieto.
  - **Bypass en acciones manuales**: Las acciones explícitas del usuario ("Actualizar ahora", pings remotos, Magic Packet WOL) **DEBEN** usar `force = true` para ejecutarse de inmediato saltándose el throttling.
  - **Historial acotado**: Solo registrar puntos de historial (`history_`) si hubo desplazamiento real $\ge 40\text{ metros}$ y al menos 2 minutos de diferencia.

