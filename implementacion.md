# 📋 Tareas Pendientes y Estado de Implementación (Thor Radar & Dispositivos)

Fecha: 2026-09-20 (v1.7.65)

---

## 📌 1. Problemas Reportados por el Usuario para Próxima Sesión

### A. Fallo en "Ping Magic Packet"
- **Síntoma**: Al presionar `⚡ Ping Magic Packet` desde la web (`/radar`), el celular no reacciona ni actualiza el fix GPS a pesar de indicar que el radar está activo.
- **Causas Identificadas a Investigar y Resolver**:
  1. **Notificación Desechable (No Persistente / No Ongoing)**: 
     - La notificación del servicio `ThorRadarService` actualmente permite ser deslizada/borrada (`dismissible`), lo que sugiere que no está configurada como `setOngoing(true)` / Foreground Service persistente estricto.
     - Al ser descartable o no estar anclada, el sistema Android (Doze Mode / Memory Killer) congela o mata el proceso en segundo plano, impidiendo que escuche `locations/<coupleId>/pings/<user>` o que reciba intents locales.
  2. **Wake-on-LAN vía FCM Data Push**:
     - Cuando la app está cerrada o en segundo plano profundo, Firestore snapshots no se ejecutan porque el socket de Firestore se suspende.
     - El envío de FCM Magic Packet (`radar_ping` / `WOL_LOCATION_WAKEUP`) debe ser emitido desde el backend web / Cloud Function hacia el topic de FCM con `priority: HIGH` y sin clave `notification` (solo `data` pura) para que `MyFirebaseMessagingService.onMessageReceived` despierte el dispositivo silenciosamente, adquiera WakeLock y ejecute `handleMagicLocationPing(context)`.
  3. **Escuchador de Pings en Foreground y Background**:
     - Asegurar que `ThorRadarService` mantenga su propio `pingListener` activo mientras el servicio en primer plano esté vivo, además del listener de `MainActivity`.

---

## 🛠️ 2. Plan de Acción Técnico para la Próxima Sesión

1. **Blindaje de `ThorRadarService` (Foreground Service Persistente)**:
   - Configurar la notificación de `ThorRadarService` con:
     - `setOngoing(true)`
     - `setAutoCancel(false)`
     - `setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)`
     - `START_STICKY` en `onStartCommand`
   - Agregar `pingListener` dentro del propio ciclo de vida de `ThorRadarService` para responder a pings de Firestore mientras el servicio esté activo, independientemente de si la UI (`MainActivity`) está visible.

2. **Integración de FCM Push Magic Packet desde la Web (`/api/send-magic-ping`)**:
   - Agregar un endpoint en `web/server.js` (o Firebase Admin / FCM HTTP v1) que emita el paquete FCM `priority: "HIGH"` directo al topic `diario_<coupleId>` con `{ "magic_packet": "WOL_LOCATION_WAKEUP", "targetDoc": userKey }`.
   - Así, si la app está en sueño profundo (Doze mode) o cerrada, FCM despierta el teléfono en 0ms y `MyFirebaseMessagingService` ejecuta `ThorRadarManager.handleMagicLocationPing(this)` con WakeLock.

3. **Verificación de la Notificación de Radar**:
   - Asegurar que la notificación muestre en vivo el estado real del radar (`🟢 Thor Radar: Transmitiendo ubicación` vs `🛑 Thor Radar: En pausa`) y que no pueda ser eliminada accidentalmente mientras el radar esté encendido.

---

## 📦 3. Cambios Realizados en v1.7.65

- **Web**:
  - Panel de control y monitoreo de dispositivos (`/radar`) con estado de Kevin y Ali.
  - Sincronización en vivo constante cada 1 segundo (0 lag / sin necesidad de refrescar manualmente).
  - Escuchadores Firestore con `includeMetadataChanges: true` y mutación de estado optimista en 0ms para botones de encendido/apagado, desconexión y alertas SOS.
  - Diseño responsivo adaptativo para pantalla dividida (50% / media pantalla) y móviles: badges sin desbordamiento (`white-space: nowrap`), botones balanceados con `flex-grow` y tarjetas apilables en $< 1080$px.
- **Android**:
  - `MainActivity.kt`: Control remoto de `isSharing` y escuchador de `pings` de Firestore a nivel global.
  - Bump de versión a `1.7.65` (Build `110`).
