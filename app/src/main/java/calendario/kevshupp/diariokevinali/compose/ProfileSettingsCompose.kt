package calendario.kevshupp.diariokevinali.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.DuplicateGroup
import calendario.kevshupp.diariokevinali.LocalPhoto
import calendario.kevshupp.diariokevinali.R

@Composable
fun SettingsScreen(
    currentTheme: String,
    useCustomBg: Boolean,
    onBgPreferenceChange: (Boolean) -> Unit,
    showTopBar: Boolean = true,
    onShowTopBarChange: (Boolean) -> Unit = {},
    versionName: String,
    onThemeChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onColorSelect: (String) -> Unit,
    currentLightColor: String = "#D1C4E9",
    currentDarkColor: String = "#4A148C",
    currentCacheLimit: Long,
    onCacheLimitChange: (Long) -> Unit,
    onTestNotification: () -> Unit,
    updateInterval: Long,
    onUpdateIntervalChange: (Long) -> Unit,
    appointmentLeadTime: Long,
    onAppointmentLeadTimeChange: (Long) -> Unit,
    refreshRate: Int = 90,
    onRefreshRateChange: (Int) -> Unit = {},
    googleAccountEmail: String?,
    selectedFolderUri: String?,
    syncIntervalMinutes: Long,
    wifiOnly: Boolean,
    chargingOnly: Boolean,
    syncState: String,
    syncMaxRetries: Int,
    syncLastError: String?,
    onMaxRetriesChange: (Int) -> Unit,
    onClearLastError: () -> Unit,
    onLinkGoogleDrive: () -> Unit,
    onUnlinkGoogleDrive: () -> Unit,
    onSelectLocalFolder: () -> Unit,
    onIntervalChange: (Long) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onChargingOnlyChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onStopSync: () -> Unit,
    isSyncing: Boolean,
    syncProgress: Int = -1,
    syncStatus: String = "",
    localFilesCount: Int = 0,
    cloudFilesCount: Int = 0,
    syncParallelLines: Int = 3,
    activeSyncSlots: List<Pair<String, Int>> = emptyList(),
    onParallelLinesChange: (Int) -> Unit = {},
    isScanningDuplicates: Boolean = false,
    duplicateGroups: List<DuplicateGroup> = emptyList(),
    scanCompleted: Boolean = false,
    scannedCount: Int = 0,
    totalToScan: Int = 0,
    deletedPhotosCount: Int = 0,
    spaceFreedBytes: Long = 0L,
    isDeleting: Boolean = false,
    onScanDuplicates: () -> Unit = {},
    onDeleteDuplicates: (List<LocalPhoto>) -> Unit = {},
    onResetDuplicateState: () -> Unit = {},
    syncDirection: String = "BIDIRECTIONAL",
    onDirectionChange: (String) -> Unit = {},
    onResetDrive: () -> Unit = {},
    onIncorrectPassword: () -> Unit = {},
    onTestFirestore: ( (String) -> Unit ) -> Unit = {},
    onTestGoogleDrive: ( (String) -> Unit ) -> Unit = {},
    onRenamePhotosByDate: () -> Unit = {},
    coupleId: String? = null,
    onAdminResetRanking: () -> Unit = {},
    onAdminResetMinigames: () -> Unit = {},
    onAdminResetPets: () -> Unit = {}
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val isMono = currentTheme == "Pixel Monocromático"
    val activeColorHex = if (isDark) currentDarkColor else currentLightColor
    val backgroundColor = getAppBackgroundColor(currentTheme, useCustomBg, activeColorHex)
    
    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val btnBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    var appSettingsSubView by remember { mutableStateOf("menu") }

    // Interceptar el botón Atrás del sistema cuando se esté dentro de una subcategoría
    BackHandler(enabled = appSettingsSubView != "menu") {
        if (appSettingsSubView == "duplicates") {
            appSettingsSubView = "cache"
        } else {
            appSettingsSubView = "menu"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        if (appSettingsSubView == "menu") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_pixel),
                        contentDescription = "Volver",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Configuración",
                    fontFamily = Vt323,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) 
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GeneralSettingsMenuButton("🎨 Diseño y Tema", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "theme"
                }
                GeneralSettingsMenuButton("🔔 Alertas y Tiempos", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "alerts"
                }
                GeneralSettingsMenuButton("☁️ Sincronización (Google Drive)", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "sync"
                }
                GeneralSettingsMenuButton("💾 Almacenamiento", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "cache"
                }
                GeneralSettingsMenuButton("⚙️ Sistema", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "system"
                }
                GeneralSettingsMenuButton("🛠️ Avanzado (Diagnóstico)", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "advanced"
                }
                GeneralSettingsMenuButton("🔒 Panel Administrador", btnBackground, textColor, borderColor) {
                    appSettingsSubView = "admin"
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Versión actual: $versionName",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        } else {
            val subTitleText = when (appSettingsSubView) {
                "theme" -> "Diseño y Tema"
                "alerts" -> "Alertas y Tiempos"
                "sync" -> "Sincronización"
                "cache" -> "Almacenamiento"
                "duplicates" -> "Duplicados"
                "advanced" -> "Avanzado"
                "admin" -> "Panel Administrador"
                else -> "Sistema"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { appSettingsSubView = "menu" }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_pixel),
                        contentDescription = "Volver",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = subTitleText,
                    fontFamily = Vt323,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) 
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (appSettingsSubView) {
                "theme" -> {
                    Text(text = "TEMA VISUAL", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentTheme == "Pixel Claro", onClick = { onThemeChange("Pixel Claro") })
                        Text("Pixel Claro", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onThemeChange("Pixel Claro") })
                        Spacer(modifier = Modifier.width(20.dp))
                        RadioButton(selected = currentTheme == "Pixel Oscuro", onClick = { onThemeChange("Pixel Oscuro") })
                        Text("Pixel Oscuro", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onThemeChange("Pixel Oscuro") })
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isDark) "COLOR DE BARRAS (OSCURO - POR TONALIDADES)" else "COLOR DE BARRAS (CLARO PASTEL - POR TONALIDADES)",
                        fontFamily = Vt323, fontSize = 18.sp, color = textColor
                    )
                    
                    val colorFamilies = if (isDark) {
                        listOf(
                            "❤️ ROJOS Y VINOS" to listOf("#B71C1C", "#C2185B", "#880E4F", "#BF360C"),
                            "💜 PÚRPURAS Y VIOLETAS" to listOf("#4A148C", "#6A1B9A", "#4A0E4E"),
                            "💙 AZULES E ÍNDIGOS" to listOf("#0D47A1", "#1A237E", "#37474F"),
                            "🌲 VERDES Y TEALS" to listOf("#006064", "#004D40", "#1B5E20", "#33691E"),
                            "🔥 CÁLIDOS Y NEUTROS" to listOf("#E65100", "#3E2723", "#263238")
                        )
                    } else {
                        listOf(
                            "🌸 ROSAS Y CORALES" to listOf("#F8BBD0", "#FFCDD2", "#FCE4EC", "#F3E5F5"),
                            "💜 PÚRPURAS Y LILAS" to listOf("#D1C4E9", "#E1BEE7", "#C5CAE9"),
                            "🩵 AZULES Y AQUA" to listOf("#E1F5FE", "#B3E5FC", "#B2EBF2"),
                            "🍃 VERDES Y MATCHA" to listOf("#C8E6C9", "#DCEDC8", "#DCE775"),
                            "☀️ CÁLIDOS Y LATTE" to listOf("#FFF59D", "#FFE57F", "#FFE0B2", "#D7CCC8")
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorFamilies.forEach { (familyLabel, familyColors) ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = familyLabel,
                                    fontFamily = Vt323,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    familyColors.forEach { colorHex ->
                                        val isSelected = colorHex.equals(activeColorHex, ignoreCase = true)
                                        val circleColor = Color(android.graphics.Color.parseColor(colorHex))
                                        val isLightCircle = isColorLight(colorHex)
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSelected) 38.dp else 32.dp)
                                                .background(circleColor, CircleShape)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.5.dp,
                                                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF4A2511)) else borderColor.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    if (!isMono) {
                                                        onColorSelect(colorHex)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Text(
                                                    text = "✓",
                                                    fontFamily = Vt323,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLightCircle) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBgPreferenceChange(!useCustomBg) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useCustomBg,
                            onCheckedChange = { onBgPreferenceChange(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FONDO CON TONO PERSONALIZADO",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                            Text(
                                text = "Aplica un suave tinte del color seleccionado al fondo general de la app.",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                "alerts" -> {
                    Text(text = "NOTIFICACIONES", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onTestNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF6A1B9A) else Color(0xFFE91E63)),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    ) {
                        Text("🔔 ENVIAR NOTIFICACIÓN DE PRUEBA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "FRECUENCIA DE ACTUALIZACIÓN", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Text(text = "(Mínimo 15 min por sistema Android)", fontFamily = Vt323, fontSize = 14.sp, color = textColor.copy(alpha = 0.6f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = updateInterval == 15L, onClick = { onUpdateIntervalChange(15L) })
                        Text("15m", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = updateInterval == 60L, onClick = { onUpdateIntervalChange(60L) })
                        Text("1h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = updateInterval == 360L, onClick = { onUpdateIntervalChange(360L) })
                        Text("6h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = updateInterval == 720L, onClick = { onUpdateIntervalChange(720L) })
                        Text("12h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "AVISO DE CITAS (CALENDARIO)", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = appointmentLeadTime == 15L, onClick = { onAppointmentLeadTimeChange(15L) })
                        Text("15m", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = appointmentLeadTime == 60L, onClick = { onAppointmentLeadTimeChange(60L) })
                        Text("1h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = appointmentLeadTime == 180L, onClick = { onAppointmentLeadTimeChange(180L) })
                        Text("3h", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = appointmentLeadTime == 1440L, onClick = { onAppointmentLeadTimeChange(1440L) })
                        Text("1d", fontFamily = Vt323, fontSize = 16.sp, color = textColor)
                    }
                }
                "sync" -> {
                    SettingsSyncCompose(
                        currentTheme = currentTheme,
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncIntervalMinutes = syncIntervalMinutes,
                        wifiOnly = wifiOnly,
                        chargingOnly = chargingOnly,
                        syncState = syncState,
                        syncMaxRetries = syncMaxRetries,
                        syncLastError = syncLastError,
                        onMaxRetriesChange = onMaxRetriesChange,
                        onClearLastError = onClearLastError,
                        onLinkGoogleDrive = onLinkGoogleDrive,
                        onUnlinkGoogleDrive = onUnlinkGoogleDrive,
                        onSelectLocalFolder = onSelectLocalFolder,
                        onIntervalChange = onIntervalChange,
                        onWifiOnlyChange = onWifiOnlyChange,
                        onChargingOnlyChange = onChargingOnlyChange,
                        onSyncNow = onSyncNow,
                        onStopSync = onStopSync,
                        isSyncing = isSyncing,
                        syncProgress = syncProgress,
                        syncStatus = syncStatus,
                        localFilesCount = localFilesCount,
                        cloudFilesCount = cloudFilesCount,
                        syncParallelLines = syncParallelLines,
                        activeSyncSlots = activeSyncSlots,
                        onParallelLinesChange = onParallelLinesChange,
                        syncDirection = syncDirection,
                        onDirectionChange = onDirectionChange,
                        onResetDrive = onResetDrive,
                        onIncorrectPassword = onIncorrectPassword
                    )
                }
                "cache" -> {
                    Text(text = "LÍMITE DE CACHÉ", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentCacheLimit == 100L, onClick = { onCacheLimitChange(100L) })
                        Text("100MB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(100L) })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = currentCacheLimit == 500L, onClick = { onCacheLimitChange(500L) })
                        Text("500MB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(500L) })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = currentCacheLimit == 1024L, onClick = { onCacheLimitChange(1024L) })
                        Text("1GB", fontFamily = Vt323, fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onCacheLimitChange(1024L) })
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(text = "LIMPIEZA DE DUPLICADOS", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Encuentra y elimina fotos idénticas en tu carpeta local para liberar espacio:",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { appSettingsSubView = "duplicates" }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF00796B) else Color(0xFF00897B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍 BUSCAR FOTOS REPETIDAS", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(text = "ORGANIZACIÓN POR FECHA", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Renombra automáticamente tus fotos al formato fecha (ej: 2026-03-24_15-30-00.jpg) para ordenarlas perfectamente:",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onRenamePhotosByDate() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF6A1B9A) else Color(0xFF8E24AA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅 ORGANIZAR Y RENOMBRAR FOTOS", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "duplicates" -> {
                    DuplicateRemoverScreen(
                        currentTheme = currentTheme,
                        selectedFolderUri = selectedFolderUri,
                        isScanning = isScanningDuplicates,
                        duplicateGroups = duplicateGroups,
                        scanCompleted = scanCompleted,
                        scannedCount = scannedCount,
                        totalToScan = totalToScan,
                        deletedCount = deletedPhotosCount,
                        freedSpaceBytes = spaceFreedBytes,
                        isDeleting = isDeleting,
                        onScan = onScanDuplicates,
                        onDelete = onDeleteDuplicates,
                        onReset = onResetDuplicateState,
                        textColor = textColor,
                        borderColor = borderColor,
                        isDark = isDark
                    )
                }
                "system" -> {
                    Text(text = "TASA DE REFRESCO (PANTALLA)", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configura la fluidez visual en pantalla (90 Hz recomendado por defecto):",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(60, 90, 100, 120).forEach { hz ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onRefreshRateChange(hz) }
                            ) {
                                RadioButton(
                                    selected = refreshRate == hz,
                                    onClick = { onRefreshRateChange(hz) }
                                )
                                Text(
                                    text = "${hz}Hz",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = if (refreshRate == hz) FontWeight.Bold else FontWeight.Normal,
                                    color = if (refreshRate == hz) (if (isDark) Color(0xFFFF80AB) else Color(0xFFC2185B)) else textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    val updatesBtnBg = if (isDark) Color(0xFF00796B) else Color(0xFF673AB7)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { onCheckUpdates() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(3.dp, borderColor)
                                .background(updatesBtnBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔄 BUSCAR ACTUALIZACIONES", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { onLogout() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .offset(y = 6.dp)
                                .background(borderColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(3.dp, borderColor)
                                .background(Color(0xFF795548)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚪 CERRAR SESIÓN", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
                        }
                    }
                }
                "advanced" -> {
                    AdvancedSettingsCompose(
                        currentTheme = currentTheme,
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncState = syncState,
                        coupleId = coupleId,
                        onTestFirestore = onTestFirestore,
                        onTestGoogleDrive = onTestGoogleDrive
                    )
                }
                "admin" -> {
                    AdminSettingsCompose(
                        currentTheme = currentTheme,
                        onResetRanking = onAdminResetRanking,
                        onResetMinigames = onAdminResetMinigames,
                        onResetPets = onAdminResetPets,
                        onIncorrectPassword = onIncorrectPassword
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsMenuButton(
    text: String,
    btnBackground: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = 6.dp)
                .background(borderColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(2.dp, borderColor)
                .background(btnBackground)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontFamily = Vt323, fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Bold)
            Text("▶", fontFamily = Vt323, fontSize = 16.sp, color = borderColor)
        }
    }
}
