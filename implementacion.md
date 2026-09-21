# 🧭 Plan de Implementación: Firebase Realtime Database para Thor Radar

Este documento detalla la arquitectura, configuración, estructura de datos y pasos exactos para migrar la transmisión de telemetría y coordenadas en tiempo real de **Thor Radar** desde **Cloud Firestore** a **Firebase Realtime Database (RTDB)**.

---

## 🎯 1. Fundamento Técnico y Beneficios

| Característica | 🟧 Cloud Firestore | 🟨 Firebase Realtime Database (RTDB) |
| :--- | :--- | :--- |
| **Métrica de Límite (Spark)** | Por Operación (**Máx 20.000 escrituras / día**) | Por Ancho de Banda (**Máx 10 GB / mes**, escrituras **ilimitadas**) |
| **Consumo GPS Continuo** | Alto riesgo de agotar la cuota diaria de la app | **Insignificante** (~1.5 MB en 4h de rastreo continuo) |
| **Latencia de transmisión** | 200 - 500 ms | **< 100 ms** (WebSocket persistente nativo) |
| **Detección de Desconexión** | Requiere heartbeats por tiempo / timeouts | **Presencia Nativa** con `.info/connected` y `onDisconnect()` |

### 💡 Arquitectura Híbrida Adoptada:
- **Cloud Firestore**: Permanece para datos estáticos y persistentes (Cartas, Mascotas, Horario, Medicamentos, Recetas, Ficha Médica y **Zonas Seguras**).
- **Firebase Realtime Database**: Maneja exclusivamente la telemetría en vivo, coordenadas, velocidad, batería, alertas SOS instantáneas y pings remotos.

---

## 🔑 2. Credenciales y Configuración de Firebase

- **URL Oficial de Realtime Database:**
  ```text
  https://diario-ali-kevin-default-rtdb.firebaseio.com/
  ```
- **Reglas de Seguridad (Pestaña "Rules" en Firebase Console):**
  > [!IMPORTANT]
  > Dado que la app utiliza autenticación propia con `userId` y vínculo de pareja (sin Firebase Auth), las reglas en la consola de Firebase Realtime Database deben permitir lectura y escritura para el nodo `locations`:
  ```json
  {
    "rules": {
      "locations": {
        ".read": true,
        ".write": true
      }
    }
  }
  ```

---

## 📦 3. Dependencias en Gradle (Ya preparadas)

1. **`gradle/libs.versions.toml`:**
   ```toml
   [libraries]
   firebase-database = { group = "com.google.firebase", name = "firebase-database" }
   ```
2. **`app/build.gradle.kts`:**
   ```kotlin
   dependencies {
       implementation(platform(libs.firebase.bom))
       implementation(libs.firebase.firestore)
       implementation(libs.firebase.database)
       // ...
   }
   ```

---

## 🌳 4. Esquema de Datos en Realtime Database

```text
locations/
  └── <coupleId>/                     (ej. vínculo_único_123)
        ├── users/
        │     ├── kevin/
        │     │     ├── userId: "user_kevin_01"
        │     │     ├── userName: "Kevin"
        │     │     ├── profileImageUrl: "..."
        │     │     ├── latitude: -33.4372
        │     │     ├── longitude: -70.6506
        │     │     ├── accuracy: 8.5
        │     │     ├── speedKmh: 24.5
        │     │     ├── batteryLevel: 85
        │     │     ├── isCharging: false
        │     │     ├── activity: "IN_VEHICLE"
        │     │     ├── currentZone: "Casa 🏠"
        │     │     ├── address: "Av. Providencia 1234"
        │     │     ├── timestamp: 1726865000000
        │     │     ├── isSharing: true
        │     │     ├── sosActive: false
        │     │     ├── sosTimestamp: 0
        │     │     └── isOnline: true
        │     └── ali/
        │           └── { ... }
        └── pings/
              ├── kevin/
              │     ├── requestedAt: 1726865000000
              │     ├── requestedBy: "Ali"
              │     └── senderId: "user_ali_02"
              └── ali/
                    └── { ... }
```

---

## 🛠️ 5. Pasos Concretos para Retomar

### Paso 1: `ThorRadarManager.kt`
1. **Instancia de Realtime Database:**
   ```kotlin
   const val RTDB_URL = "https://diario-ali-kevin-default-rtdb.firebaseio.com"

   fun getDatabase(): FirebaseDatabase {
       return try {
           FirebaseDatabase.getInstance(RTDB_URL)
       } catch (e: Exception) {
           FirebaseDatabase.getInstance()
       }
   }
   ```
2. **Parser en `RadarLocationData`:**
   Agregar `fromDataSnapshot(snapshot: DataSnapshot?)` para deserializar `Map<*, *>` o propiedades directas desde RTDB.
3. **Emisión de Coordenadas (`publishHeartbeat`):**
   Reemplazar `db.collection("locations")...set()` por:
   ```kotlin
   val rtdb = getDatabase()
   val userRef = rtdb.reference.child("locations").child(safeCoupleId).child("users").child(docName)
   userRef.updateChildren(firestoreMap)
   ```
4. **Presencia Automática (`setupPresence`):**
   ```kotlin
   val connectedRef = rtdb.reference.child(".info/connected")
   connectedRef.addValueEventListener(object : ValueEventListener {
       override fun onDataChange(snapshot: DataSnapshot) {
           val connected = snapshot.getValue(Boolean::class.java) ?: false
           if (connected) {
               userRef.child("isOnline").onDisconnect().setValue(false)
               userRef.child("isOnline").setValue(true)
           }
       }
       override fun onCancelled(error: DatabaseError) {}
   })
   ```
5. **SOS y Pings:**
   - `triggerSos` / `cancelSos`: Actualizar `sosActive` y `sosTimestamp` en el nodo de usuario de RTDB.
   - `sendLocationRequestPing`: Escribir en `locations/<coupleId>/pings/<targetDoc>`.

---

### Paso 2: `ThorRadarService.kt`
Reemplazar `setupFirestoreListeners()` con listeners de Realtime Database:
- `pingsRef = rtdb.reference.child("locations").child(coupleId).child("pings").child(myDocName)` ➔ `ValueEventListener`
- `userRef = rtdb.reference.child("locations").child(coupleId).child("users").child(myDocName)` ➔ `ValueEventListener` (para control remoto de encendido/apagado).

---

### Paso 3: `MainActivity.kt`
- Actualizar `setupSosListener()` para escuchar cambios de `sosActive` en RTDB (`locations/<coupleId>/users/<partnerDocName>`).
- Actualizar `setupRadarRemoteControlListener()` para escuchar el switch remoto y solicitudes de ping en RTDB.

---

### Paso 4: `ThorRadarCompose.kt`
- En el `DisposableEffect`, suscribirse mediante `ValueEventListener` a:
  - `locations/<coupleId>/users/<myDocName>`
  - `locations/<coupleId>/users/<partnerDocName>`
  - `locations/<coupleId>/pings/<myDocName>`
- Mantener las Zonas Seguras en Firestore (`locations/<coupleId>/zones`).

---

### Paso 5: Web de Gestión (`web/`)
1. En `web/index.html` (y demás páginas):
   ```html
   <script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-database-compat.js"></script>
   ```
2. En `web/js/firebase-config.js`:
   ```javascript
   export const rtdb = firebase.database();
   ```
3. En `web/js/radar-view.js`:
   Cambiar `db.collection("locations")...` por `rtdb.ref("locations/" + state.coupleId + "/users")`.

---

> [!NOTE]
> Todo está preparado y documentado para continuar con la implementación en cualquier momento.
