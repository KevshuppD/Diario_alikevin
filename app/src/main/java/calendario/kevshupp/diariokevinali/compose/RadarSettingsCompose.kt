package calendario.kevshupp.diariokevinali.compose

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import calendario.kevshupp.diariokevinali.PermissionHelper
import calendario.kevshupp.diariokevinali.RadarLocationData
import calendario.kevshupp.diariokevinali.ThorRadarManager
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RadarSettingsView(
    isSharing: Boolean,
    isBatterySaver: Boolean,
    myLocation: RadarLocationData,
    currentUserId: String,
    myDisplayName: String,
    partnerName: String,
    coupleId: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color,
    onToggleSharing: (Boolean) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    isGpsEnabled: Boolean,
    hasBgLoc: Boolean,
    isIgnoringBattery: Boolean,
    onOpenSetupWizard: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = theme == "Pixel Oscuro"
    val activity = context as? android.app.Activity
    var localIgnoringBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(10.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Tarjeta Maestra de Control de Encendido / Apagado
        RadarMasterPowerCard(
            isSharing = isSharing,
            onToggleSharing = onToggleSharing,
            theme = theme,
            textColor = textColor,
            borderColor = borderColor
        )

        // 2. Toggle Modo Ahorro de Batería
        SettingToggleCard(
            title = "MODO AHORRO DE BATERÍA",
            description = "Actualiza cada 60s en vez de 15s para reducir el consumo en viajes largos.",
            checked = isBatterySaver,
            onCheckedChange = onToggleBatterySaver,
            textColor = textColor,
            borderColor = borderColor,
            theme = theme
        )

        // 3. Panel Consolidado de Requisitos y Permisos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF252028) else Color(0xFFF3E5F5))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🩺", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REQUISITOS EN TU CELULAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onOpenSetupWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("ASISTENTE 🛠️", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                    }
                }

                // GPS
                RadarRequirementRow(
                    icon = if (isGpsEnabled) "✅" else "⚠️",
                    title = "Sensor GPS",
                    subtitle = if (isGpsEnabled) "Ubicación del sistema encendida" else "Ubicación desactivada en Android",
                    isOk = isGpsEnabled,
                    buttonText = if (isGpsEnabled) null else "ACTIVAR",
                    onButtonClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    textColor = textColor,
                    isDark = isDark
                )

                // Segundo Plano
                RadarRequirementRow(
                    icon = if (hasBgLoc) "✅" else "⚠️",
                    title = "Segundo plano (Todo el tiempo)",
                    subtitle = if (hasBgLoc) "Permiso concedido para app cerrada" else "Se requiere 'Permitir todo el tiempo'",
                    isOk = hasBgLoc,
                    buttonText = if (hasBgLoc) null else "ACTIVAR",
                    onButtonClick = {
                        if (activity != null) {
                            PermissionHelper.requestBackgroundLocationPermission(activity)
                        } else {
                            PermissionHelper.openAppSettings(context)
                        }
                    },
                    textColor = textColor,
                    isDark = isDark
                )

                // Batería
                RadarRequirementRow(
                    icon = if (localIgnoringBattery) "✅" else "⚠️",
                    title = "Batería sin restricciones",
                    subtitle = if (localIgnoringBattery) "Optimización desactivada" else "Android podría pausar el radar",
                    isOk = localIgnoringBattery,
                    buttonText = if (localIgnoringBattery) null else "QUITAR LÍMITE",
                    onButtonClick = {
                        PermissionHelper.requestIgnoreBatteryOptimizations(context)
                        localIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                    },
                    textColor = textColor,
                    isDark = isDark
                )
            }
        }

        // 4. Tarjeta de Diagnóstico / Telemetría GPS e Identidad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF222222) else Color(0xFFFFF7DB))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📡 DISPOSITIVO Y TELEMETRÍA GPS",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "Usuario: $myDisplayName ($currentUserId) | Pareja: $partnerName",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = "Lat: ${String.format(Locale.US, "%.5f", myLocation.latitude)} | Lng: ${String.format(Locale.US, "%.5f", myLocation.longitude)} | ±${myLocation.accuracy.roundToInt()}m | 🔋 ${myLocation.batteryLevel}%",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }

        // 5. Botón Forzar Actualización Manual
        Button(
            onClick = {
                ThorRadarManager.forceLocationUpdate(context) { success ->
                    if (success) {
                        Toast.makeText(context, "Ubicación actualizada con éxito 📍", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Verifica que el GPS y los permisos estén activados", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text(text = "🔄 ACTUALIZAR MI UBICACIÓN AHORA", fontFamily = Vt323, fontSize = 17.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun RadarRequirementRow(
    icon: String,
    title: String,
    subtitle: String,
    isOk: Boolean,
    buttonText: String?,
    onButtonClick: () -> Unit,
    textColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isOk) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFFF9800))
            .background(if (isDark) (if (isOk) Color(0xFF1E281E) else Color(0xFF332005)) else (if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Vt323,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
        if (buttonText != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(buttonText, fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    borderColor: Color,
    theme: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(if (theme == "Pixel Oscuro") Color(0xFF282828) else Color(0xFFFFF7DB))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF80AB),
                    checkedTrackColor = Color(0xFF91465F)
                )
            )
        }
    }
}

@Composable
fun RadarMasterPowerCard(
    isSharing: Boolean,
    onToggleSharing: (Boolean) -> Unit,
    theme: String,
    textColor: Color,
    borderColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val cardBorder = if (isSharing) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val cardBg = if (isSharing) {
        if (isDark) Color(0xFF1B3B1D) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF3B1B1B) else Color(0xFFFFEBEE)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, cardBorder)
            .background(cardBg)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSharing) "🟢" else "🛑",
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSharing) "RADAR ENCENDIDO (TRANSMITIENDO)" else "RADAR APAGADO (AHORRO 100%)",
                        fontFamily = Vt323,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSharing) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFE57373) else Color(0xFFD32F2F))
                    )
                    Text(
                        text = if (isSharing) {
                            "Rastreo en segundo plano activo. Tu pareja puede ver tu ubicación."
                        } else {
                            "Servicio en segundo plano y GPS 100% detenidos. Cero consumo de batería."
                        },
                        fontFamily = Vt323,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            }

            // Botón Maestro de Encendido / Apagado
            Button(
                onClick = { onToggleSharing(!isSharing) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSharing) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (isSharing) "🛑 APAGAR THOR RADAR" else "⚡ ENCENDER THOR RADAR",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = if (isSharing) {
                    "💡 Apagar el radar detiene por completo el servicio en segundo plano (Foreground Service) y el GPS para ahorrar batería."
                } else {
                    "💡 Enciende el radar cuando salgas o quieras que tu pareja vea tu ubicación y zonas seguras en tiempo real."
                },
                fontFamily = Vt323,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun RadarSetupWizardDialog(
    isSharing: Boolean,
    isGpsEnabled: Boolean,
    hasBgPermission: Boolean,
    isIgnoringBattery: Boolean,
    onToggleSharing: (Boolean) -> Unit,
    onEnableGps: () -> Unit,
    onRequestBgPermission: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onDismiss: () -> Unit,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val allReady = isSharing && isGpsEnabled && hasBgPermission && isIgnoringBattery

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🛠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIGURACIÓN DEL RADAR",
                        fontFamily = Vt323,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Text(
                    text = if (allReady) {
                        "✨ ¡Excelente! Todos los requisitos están activos para que tu pareja vea tu ubicación en tiempo real sin interrupciones."
                    } else {
                        "Para que el radar funcione en segundo plano y Android no congele la ubicación con la app cerrada, activa estos 4 elementos:"
                    },
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.85f)
                )

                HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

                // Item 1: Thor Radar Encendido
                SetupCheckItem(
                    title = "1. THOR RADAR ENCENDIDO",
                    subtitle = if (isSharing) "Radar activo en la app" else "El radar está apagado",
                    isReady = isSharing,
                    buttonText = "ENCENDER",
                    onAction = { onToggleSharing(true) },
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 2: Sensor GPS del Teléfono
                SetupCheckItem(
                    title = "2. SENSOR GPS DEL TELÉFONO",
                    subtitle = if (isGpsEnabled) "Ubicación del sistema encendida" else "GPS apagado en Android",
                    isReady = isGpsEnabled,
                    buttonText = "ACTIVAR GPS",
                    onAction = onEnableGps,
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 3: Permiso Todo el Tiempo
                SetupCheckItem(
                    title = "3. PERMISO 'TODO EL TIEMPO'",
                    subtitle = if (hasBgPermission) "Rastreo con pantalla bloqueada activo" else "Solo 'Mientras la app está en uso'",
                    isReady = hasBgPermission,
                    buttonText = "ACTIVAR",
                    onAction = onRequestBgPermission,
                    textColor = textColor,
                    isDark = isDark
                )

                // Item 4: Batería Sin Restricciones
                SetupCheckItem(
                    title = "4. BATERÍA: SIN RESTRICCIONES",
                    subtitle = if (isIgnoringBattery) "Android no congelará el proceso" else "Optimización activa (puede suspender el GPS)",
                    isReady = isIgnoringBattery,
                    buttonText = "QUITAR LÍMITE",
                    onAction = onRequestIgnoreBattery,
                    textColor = textColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (allReady) Color(0xFF2E7D32) else accentColor),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = if (allReady) "✅ TODO LISTO • CONTINUAR AL MAPA" else "ENTENDIDO • IR AL MAPA",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SetupCheckItem(
    title: String,
    subtitle: String,
    isReady: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    textColor: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isReady) Color(0xFF4CAF50) else Color(0xFFFF9800))
            .background(
                if (isReady) {
                    if (isDark) Color(0xFF1B3B1D) else Color(0xFFE8F5E9)
                } else {
                    if (isDark) Color(0xFF332005) else Color(0xFFFFF3E0)
                }
            )
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isReady) "✅" else "⚠️",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReady) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                )
                Text(
                    text = subtitle,
                    fontFamily = Vt323,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            if (!isReady) {
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(buttonText, fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
