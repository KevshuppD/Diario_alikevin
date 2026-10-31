package calendario.kevshupp.diariokevinali.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    onAdminResetPets: () -> Unit = {},
    onAdminResetVouchers: () -> Unit = {}
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GeneralSettingsMenuButton("🎨 Diseño y Tema", btnBackground, textColor, borderColor, "Tema, colores, tono de fondo y barra superior") {
                    appSettingsSubView = "theme"
                }
                GeneralSettingsMenuButton("🔔 Alertas y Tiempos", btnBackground, textColor, borderColor, "Notificación de prueba, actualizaciones y citas") {
                    appSettingsSubView = "alerts"
                }
                GeneralSettingsMenuButton("☁️ Sincronización (Google Drive)", btnBackground, textColor, borderColor, "Respaldar y compartir tus fotos") {
                    appSettingsSubView = "sync"
                }
                GeneralSettingsMenuButton("💾 Almacenamiento", btnBackground, textColor, borderColor, "Caché, fotos repetidas y orden por fecha") {
                    appSettingsSubView = "cache"
                }
                GeneralSettingsMenuButton("⚙️ Sistema", btnBackground, textColor, borderColor, "Fluidez, actualizaciones y sesión") {
                    appSettingsSubView = "system"
                }
                SettingsSectionLabel(
                    "ZONA DE CUIDADO",
                    if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                    Modifier.padding(top = 6.dp)
                )
                GeneralSettingsMenuButton("🛠️ Avanzado y Admin", btnBackground, textColor, if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828), "Diagnóstico, premios y panel con clave") {
                    appSettingsSubView = "advanced"
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
                "advanced" -> "Avanzado y Admin"
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
                    // Vista previa: el color elegido se ve aquí mismo, sin salir de la pantalla
                    val previewBar = Color(android.graphics.Color.parseColor(activeColorHex))
                    Column(modifier = Modifier.fillMaxWidth().border(2.dp, borderColor)) {
                        Text(
                            text = "Nuestro Diario ♥",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = if (isColorLight(activeColorHex)) Color.Black else Color.White,
                            modifier = Modifier.fillMaxWidth().background(previewBar).padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Vista previa del tema",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth().background(btnBackground).padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSectionLabel("TEMA VISUAL", textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    PixelSegmented(
                        options = listOf("Pixel Claro" to "Pixel Claro", "Pixel Oscuro" to "Pixel Oscuro"),
                        selected = currentTheme,
                        onSelect = onThemeChange,
                        textColor = textColor,
                        borderColor = borderColor,
                        background = btnBackground
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isDark) "COLOR DE BARRAS (OSCURO)" else "COLOR DE BARRAS (CLARO PASTEL)",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        colorFamilies.forEach { (familyLabel, familyColors) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = familyLabel.substringBefore(' '), fontSize = 18.sp, modifier = Modifier.width(30.dp))
                                familyColors.forEach { colorHex ->
                                    val isSelected = colorHex.equals(activeColorHex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.5.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF4A2511)) else borderColor.copy(alpha = 0.5f)
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
                                                color = if (isColorLight(colorHex)) Color.Black else Color.White
                                            )
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
                                text = "Suave tinte del color elegido en el fondo de la app.",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowTopBarChange(!showTopBar) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showTopBar,
                            onCheckedChange = { onShowTopBarChange(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MOSTRAR BARRA SUPERIOR ('NUESTRO DIARIO')",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                            Text(
                                text = "Oculta o muestra el recuadro superior con el título.",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                "alerts" -> {
                    SettingsSectionLabel("FRECUENCIA DE ACTUALIZACIÓN", textColor)
                    Text(text = "(Mínimo 15 min por sistema Android)", fontFamily = Vt323, fontSize = 14.sp, color = textColor.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelSegmented(
                        options = listOf(15L to "15m", 60L to "1h", 360L to "6h", 720L to "12h"),
                        selected = updateInterval,
                        onSelect = onUpdateIntervalChange,
                        textColor = textColor,
                        borderColor = borderColor,
                        background = btnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Busca actualizaciones de la app cada ${longTimeLabel(updateInterval)}",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SettingsSectionLabel("AVISO DE CITAS (CALENDARIO)", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelSegmented(
                        options = listOf(15L to "15m", 60L to "1h", 180L to "3h", 1440L to "1d"),
                        selected = appointmentLeadTime,
                        onSelect = onAppointmentLeadTimeChange,
                        textColor = textColor,
                        borderColor = borderColor,
                        background = btnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Te avisa ${longTimeLabel(appointmentLeadTime)} antes de la cita",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsSectionLabel("NOTIFICACIONES", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onTestNotification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF6A1B9A) else Color(0xFFE91E63)),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    ) {
                        Text("🔔 ENVIAR NOTIFICACIÓN DE PRUEBA", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
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
                    SettingsSectionLabel("LÍMITE DE CACHÉ", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelSegmented(
                        options = listOf(100L to "100MB", 500L to "500MB", 1024L to "1GB"),
                        selected = currentCacheLimit,
                        onSelect = onCacheLimitChange,
                        textColor = textColor,
                        borderColor = borderColor,
                        background = btnBackground
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsSectionLabel("HERRAMIENTAS DE FOTOS", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsToolRow(
                        icon = "🔍",
                        title = "Buscar fotos repetidas",
                        description = "Encuentra y elimina fotos idénticas en tu carpeta local para liberar espacio",
                        background = btnBackground,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { appSettingsSubView = "duplicates" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToolRow(
                        icon = "📅",
                        title = "Organizar y renombrar fotos",
                        description = "Las renombra por fecha, por ejemplo 2026-03-24_15-30-00.jpg",
                        background = btnBackground,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = onRenamePhotosByDate
                    )
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
                    SettingsSectionLabel("TASA DE REFRESCO (PANTALLA)", textColor)
                    Text(
                        text = "Configura la fluidez visual (90 Hz recomendado por defecto).",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelSegmented(
                        options = listOf(60, 90, 100, 120).map { it to "${it}Hz" },
                        selected = refreshRate,
                        onSelect = onRefreshRateChange,
                        textColor = textColor,
                        borderColor = borderColor,
                        background = btnBackground
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsSectionLabel("APLICACIÓN", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsToolRow(
                        icon = "🔄",
                        title = "Buscar actualizaciones",
                        description = "Versión actual: $versionName",
                        background = btnBackground,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = onCheckUpdates
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsSectionLabel("CUENTA", textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    PixelLogoutButton(isDark = isDark, onClick = onLogout)
                }
                "advanced" -> {
                    AdvancedSettingsCompose(
                        currentTheme = currentTheme,
                        googleAccountEmail = googleAccountEmail,
                        selectedFolderUri = selectedFolderUri,
                        syncState = syncState,
                        coupleId = coupleId,
                        onTestFirestore = onTestFirestore,
                        onTestGoogleDrive = onTestGoogleDrive,
                        onResetRanking = onAdminResetRanking,
                        onResetMinigames = onAdminResetMinigames,
                        onResetPets = onAdminResetPets,
                        onResetVouchers = onAdminResetVouchers,
                        onIncorrectPassword = onIncorrectPassword
                    )
                }
            }
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

/** 15 → "15 minutos", 60 → "1 hora", 1440 → "1 día". */
private fun longTimeLabel(minutes: Long): String = when {
    minutes >= 1440L && minutes % 1440L == 0L -> (minutes / 1440L).let { "$it ${if (it == 1L) "día" else "días"}" }
    minutes >= 60L && minutes % 60L == 0L -> (minutes / 60L).let { "$it ${if (it == 1L) "hora" else "horas"}" }
    else -> "$minutes minutos"
}

@Composable
fun GeneralSettingsMenuButton(
    text: String,
    btnBackground: Color,
    textColor: Color,
    borderColor: Color,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(btnBackground)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontFamily = Vt323, fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Bold)
            Text(description, fontFamily = Vt323, fontSize = 15.sp, color = textColor.copy(alpha = 0.75f))
        }
        Text("▶", fontFamily = Vt323, fontSize = 16.sp, color = borderColor, modifier = Modifier.padding(start = 8.dp))
    }
}
