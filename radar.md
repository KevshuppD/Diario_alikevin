# 🧭 Thor Radar — Diagnóstico del Error "Error de red al sincronizar"

## 🔍 ¿Qué pasa exactamente?

Cuando pulsas **"🔄 ACTUALIZAR"** en la `PartnerLiveCard` del radar, se ejecuta `sendLocationRequestPing()` en [`ThorRadarManager.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorRadarManager.kt#L1316). Esta función tiene **dos pasos en paralelo**:

1. ✅ **Firestore Ping** (L1329-1348): Escribe `pings/{targetDoc}` en Firestore → normalmente funciona.
2. ❌ **FCM Magic Packet** (L1350-1407): Llama a `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send` con un token OAuth2 → **aquí falla**.

Si la llamada FCM falla por cualquier motivo, entra al bloque `catch (e: Exception)` en L1401, llama a `onComplete?.invoke(false)`, y en [`ThorRadarCompose.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ThorRadarCompose.kt#L717) se muestra el toast `"⚠️ Error de red al sincronizar"`.

---

## 🎯 Causas Probables (Ordenadas por Probabilidad)

### 1. 🏆 El Token OAuth2 tiene el `accessToken = null` justo al arrancar

En [`MainActivity.kt` L121](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.kt#L121-L128):

```kotlin
if (creds!!.accessToken == null) {
    creds!!.refresh()   // Necesita internet para obtener el primer token
} else {
    creds!!.refreshIfExpired()
}
```

Si en el momento del `refresh()` la conexión es lenta o el teléfono acaba de salir de modo avión, el refresh puede fallar con una excepción de red antes de llegar a la llamada FCM.

**Pista clave**: `creds.accessToken` devuelve `null` al primer uso si las credenciales son recién inicializadas y nunca se ha refrescado. La credencial de `service_account.json` no tiene un token precargado — necesita hacer una petición HTTP a Google para obtenerlo.

---

### 2. 🔐 Token OAuth2 vencido / fallo silencioso de `refreshIfExpired()`

`refreshIfExpired()` puede no lanzar excepción pero dejar el token expirado si el token tiene la hora de expiración al límite. En L1354:

```kotlin
val token = creds.accessToken.tokenValue
```

Si `creds.accessToken` tiene un token vencido que `refreshIfExpired()` no renovó por razón de timing, el HTTP 401 de FCM haría que `response.isSuccessful == false` → `onComplete(false)` → toast de error.

---

### 3. 📡 El nombre del topic FCM no coincide entre dispositivos

En [`ThorRadarManager.kt` L1358-1360](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/ThorRadarManager.kt#L1358):

```kotlin
val topicName = "diario_" + safeCoupleId.lowercase()
    .replace("á", "a").replace("é", "e")...
    .replace("ñ", "n").replace(" ", "_")
```

El `coupleId` guardado en `DiarioPrefs` es `"vínculo_único_123"` (con tilde). Tras la normalización en `normalizeCoupleId()` siempre devuelve `"vínculo_único_123"`, y el topic resultante es `"diario_vinculo_unico_123"`.

En `MainActivity.setupFirebaseMessaging()` L947, la suscripción al topic aplica exactamente la misma normalización. **El problema es que si alguno de los dos dispositivos se suscribió al topic con una normalización diferente** (ej.: por un bug previo donde `coupleId` era distinto), el mensaje llegaría a un topic al que el receptor no está suscrito → FCM responde 200 OK pero nadie recibe nada.

> Este caso no provoca el toast de error (porque FCM responde OK), pero puede ser la razón de fondo por la que el botón parece no funcionar aunque no muestre error.

---

### 4. 🌐 Timeout de OkHttp demasiado corto en red lenta

En [`DiarioApp.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DiarioApp.kt#L181), el `OkHttpClient` podría tener un timeout de conexión/lectura demasiado corto. Si el celular está en 4G con señal débil o en transición de WiFi → datos móviles, la llamada a `google.com/oauth2/token` (durante el `refresh()`) o a `fcm.googleapis.com` puede superar ese timeout → `SocketTimeoutException` → catch → `onComplete(false)`.

---

## 🔧 Opciones de Solución

### Opción A ⭐ — Suprimir el Toast de error del FCM (Recomendada, mínimo cambio)

El Firestore Ping (paso 1) ya funciona independientemente del FCM. Si la app de la pareja está abierta, recibirá el ping de Firestore sin necesidad del FCM. El error solo afecta al caso de "app cerrada / background".

**Cambio en [`ThorRadarCompose.kt` L713-718](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/compose/ThorRadarCompose.kt#L713):**

```kotlin
// ANTES — Muestra error si el FCM falla aunque Firestore ping ya fue:
} else {
    Toast.makeText(context, "⚠️ Error de red al sincronizar", Toast.LENGTH_SHORT).show()
}

// DESPUÉS — No mostrar error si el FCM falla; el Firestore ping es suficiente:
// (simplemente eliminar el bloque else o cambiarlo por un log silencioso)
```

**✅ Pros:** Mínimo impacto, no marea al usuario con errores cuando la operación parcialmente funcionó.  
**⚠️ Contras:** El usuario no sabe si el wake-up llegó al celular apagado de la pareja.

---

### Opción B — Manejo robusto del token con retry en `getGoogleCredentials`

Modificar [`MainActivity.getGoogleCredentials`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/MainActivity.kt#L111) para reintentar el refresh hasta 2 veces si falla:

```kotlin
@Synchronized
fun getGoogleCredentials(context: Context): GoogleCredentials {
    var creds = cachedGoogleCredentials
    if (creds == null) {
        context.assets.open("service-account.json").use { `is` ->
            creds = GoogleCredentials.fromStream(`is`)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            cachedGoogleCredentials = creds
        }
    }
    // Retry hasta 2 veces con backoff leve
    var lastException: Exception? = null
    repeat(2) { attempt ->
        try {
            if (creds!!.accessToken == null || creds!!.accessToken.expirationTime == null) {
                creds!!.refresh()
            } else {
                creds!!.refreshIfExpired()
            }
            return creds!!
        } catch (e: Exception) {
            lastException = e
            if (attempt == 0) Thread.sleep(500L)
        }
    }
    throw lastException ?: IOException("No se pudo refrescar el token OAuth2")
}
```

**✅ Pros:** Más robusto en condiciones de red intermitente.  
**⚠️ Contras:** Puede bloquear el hilo de IO hasta ~1s extra en el peor caso.

---

### Opción C — Timeout más generoso en OkHttp

Revisar la configuración del cliente en [`DiarioApp.kt`](file:///home/kevin/Escritorio/Proyectos/Diario_alikevin/app/src/main/java/calendario/kevshupp/diariokevinali/DiarioApp.kt#L181) y asegurarse de que los timeouts sean suficientes:

```kotlin
OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()
```

**✅ Pros:** Soluciona timeouts en redes lentas sin cambiar la lógica.  
**⚠️ Contras:** No soluciona errores de token. Fácil de aplicar en paralelo a otras opciones.

---

### Opción D — Eliminar FCM del botón Actualizar y usarlo solo en background

Dado que la arquitectura AGENTS.md establece **"0 escrituras en segundo plano periódicas"** y el **Firestore ping ya es el canal primario cuando la app está abierta**, el FCM solo aporta valor cuando el celular de la pareja está con la pantalla apagada.

Se puede separar la lógica: el botón "Actualizar" solo dispara el Firestore ping (sin FCM, sin toast de error), y el FCM se envía solo si se detecta que la pareja lleva >5 min sin señal.

**✅ Pros:** Más simple, cero errores de red, respeta la arquitectura on-demand.  
**⚠️ Contras:** No despierta el celular de la pareja si tiene la app cerrada.

---

## 🧪 Cómo Verificar la Causa Real con ADB

Conectar el celular con ADB y filtrar logs en tiempo real al pulsar "Actualizar":

```bash
adb logcat -s ThorRadarManager | grep -E "MAGIC PACKET|FCM|token|Error"
```

Los mensajes clave a buscar:

| Mensaje en Logcat | Causa | Solución |
|---|---|---|
| `Push FCM status code: 200` | FCM OK, el token funcionó | Problema puede ser del lado receptor |
| `Push FCM status code: 401` | Token OAuth2 expirado/inválido | Opción B |
| `Push FCM status code: 404` | ProjectId mal formado | Verificar `google-services.json` |
| `Error enviando ping` + `SocketTimeoutException` | Timeout OkHttp | Opción C |
| `Error enviando ping` + `IOException` | Sin red en ese momento | Opción A |
| `Error enviando ping` + `NullPointerException` | `creds.accessToken` es null | Opción B urgente |

---

## ✅ Recomendación Final

Aplicar **Opción A + Opción C** en ese orden:

1. **Opción A** (inmediata, 1 línea): Suprimir el toast de error — el Firestore ping es suficiente cuando la app está abierta.
2. **Opción C** (fácil): Aumentar timeouts de OkHttp si no están configurados explícitamente.
3. Luego con calma diagnosticar el token con ADB para decidir si aplicar **Opción B**.

---

*Generado el 2026-09-19 | Proyecto: Diario Ali & Kevin*
