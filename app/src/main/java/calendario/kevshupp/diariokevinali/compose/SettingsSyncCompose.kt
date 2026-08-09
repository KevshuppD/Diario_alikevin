package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import calendario.kevshupp.diariokevinali.R
import calendario.kevshupp.diariokevinali.SyncLogger

private val Vt323Sync = FontFamily(Font(R.font.vt323))

@Composable
fun SettingsSyncCompose(
    currentTheme: String,
    googleAccountEmail: String?, // Null si no está vinculado
    selectedFolderUri: String?, // Uri.toString() local o null
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
    syncDirection: String = "BIDIRECTIONAL",
    onDirectionChange: (String) -> Unit = {},
    onResetDrive: () -> Unit = {},
    onIncorrectPassword: () -> Unit = {}
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val isMono = currentTheme == "Pixel Monocromático"
    
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

    val boxBgColor = when {
        isDark -> Color(0xFF1E1E1E)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor)
            .background(boxBgColor)
            .padding(16.dp)
    ) {
        // Título de Sección
        Text(
            text = "💾 SINCRONIZACIÓN DE FOTOS (DRIVE)",
            fontFamily = Vt323Sync,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (googleAccountEmail == null) {
            // FASE 1: Botón de Vinculación de Cuenta de Google
            Text(
                text = "Vincula tu Google Drive para respaldar y compartir automáticamente tu carpeta de fotos local con tu pareja a la distancia.",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Retro3DButton(
                text = "VINCULAR GOOGLE DRIVE 👾",
                onClick = onLinkGoogleDrive,
                buttonColor = if (isDark) Color(0xFF00796B) else Color(0xFF4285F4),
                borderColor = borderColor
            )
        } else {
            // FASE 2: Cuenta Vinculada
            Text(
                text = "Cuenta vinculada: $googleAccountEmail",
                fontFamily = Vt323Sync,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Selector de carpeta local
            val folderLabel = if (selectedFolderUri != null) "✓ Carpeta vinculada" else "Seleccionar carpeta local"
            val folderBtnColor = if (selectedFolderUri != null) Color(0xFF388E3C) else Color(0xFFE65100)
            
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "Carpeta Local en el Celular:",
                    fontFamily = Vt323Sync,
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Retro3DButton(
                    text = "$folderLabel 📂",
                    onClick = onSelectLocalFolder,
                    buttonColor = folderBtnColor,
                    borderColor = borderColor
                )
                if (selectedFolderUri != null) {
                    Text(
                        text = "Las fotos de esta carpeta se copiarán y sincronizarán.",
                        fontFamily = Vt323Sync,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // FASE 3: Ajustes de Sincronización
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Frecuencia de Sincronización:",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Frecuencia grid retro
            val options = listOf(
                0L to "Manual",
                30L to "30m",
                60L to "1h",
                720L to "12h",
                1440L to "24h"
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                options.forEach { (mins, label) ->
                    val selected = syncIntervalMinutes == mins
                    val optBg = if (selected) borderColor else Color.Transparent
                    val optText = if (selected) Color.White else textColor

                    Box(
                        modifier = Modifier
                            .border(2.dp, borderColor)
                            .background(optBg)
                            .clickable { onIntervalChange(mins) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = optText
                        )
                    }
                }
            }

            // Switches Retro
            RetroSwitch(
                checked = wifiOnly,
                onCheckedChange = onWifiOnlyChange,
                label = "Sincronizar solo con Wi-Fi 🌐",
                textColor = textColor,
                borderColor = borderColor
            )

            RetroSwitch(
                checked = chargingOnly,
                onCheckedChange = onChargingOnlyChange,
                label = "Sincronizar solo en carga ⚡",
                textColor = textColor,
                borderColor = borderColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Dirección de Sincronización:",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val directionOptions = listOf(
                "BIDIRECTIONAL" to "Bidireccional 🔄",
                "DOWNLOAD_ONLY" to "Solo Bajada ⬇",
                "UPLOAD_ONLY" to "Solo Subida ⬆"
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                directionOptions.forEach { (dir, label) ->
                    val selected = syncDirection == dir
                    val optBg = if (selected) borderColor else Color.Transparent
                    val optText = if (selected) Color.White else textColor

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, borderColor)
                            .background(optBg)
                            .clickable { onDirectionChange(dir) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = Vt323Sync,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = optText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reintentos Automáticos por Error:",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val retryOptions = listOf(0, 1, 2, 3, 5)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                retryOptions.forEach { attempts ->
                    val selected = syncMaxRetries == attempts
                    val optBg = if (selected) borderColor else Color.Transparent
                    val optText = if (selected) Color.White else textColor
                    val label = if (attempts == 0) "Manual" else attempts.toString()

                    Box(
                        modifier = Modifier
                            .border(2.dp, borderColor)
                            .background(optBg)
                            .clickable { onMaxRetriesChange(attempts) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = optText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Líneas de Subida en Paralelo:",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val parallelOptions = listOf(1, 2, 3, 5)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                parallelOptions.forEach { lines ->
                    val selected = syncParallelLines == lines
                    val optBg = if (selected) borderColor else Color.Transparent
                    val optText = if (selected) Color.White else textColor

                    Box(
                        modifier = Modifier
                            .border(2.dp, borderColor)
                            .background(optBg)
                            .clickable { onParallelLinesChange(lines) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lines.toString(),
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = optText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador de Estado de Sincronización
            val isCountsMatching = selectedFolderUri != null && localFilesCount > 0 && localFilesCount == cloudFilesCount
            val effectiveSyncState = when {
                isSyncing -> "SINCRONIZANDO"
                syncState == "SINCRONIZADO" || (isCountsMatching && syncLastError.isNullOrEmpty()) -> "SINCRONIZADO"
                else -> "NO_SINCRONIZADO"
            }
            val stateLabel = when (effectiveSyncState) {
                "SINCRONIZADO" -> "ESTADO: SINCRONIZADO 🟢"
                "SINCRONIZANDO" -> "ESTADO: SINCRONIZANDO... 🔄"
                else -> "ESTADO: NO SINCRONIZADO 🔴"
            }
            val stateColor = when (effectiveSyncState) {
                "SINCRONIZADO" -> if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
                "SINCRONIZANDO" -> if (isDark) Color(0xFF29B6F6) else Color(0xFF1976D2)
                else -> if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
            }
            
            Text(
                text = stateLabel,
                fontFamily = Vt323Sync,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = stateColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Archivos - Celular: $localFilesCount | Nube: $cloudFilesCount",
                fontFamily = Vt323Sync,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (!syncLastError.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F))
                        .background(if (isDark) Color(0x33FF5252) else Color(0xFFFFF0F0))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ ÚLTIMO ERROR REGISTRADO",
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
                        )
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F))
                                .clickable { onClearLastError() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIMPIAR ✖",
                                fontFamily = Vt323Sync,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = syncLastError,
                        fontFamily = Vt323Sync,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSyncing) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        text = if (syncStatus.isNotEmpty()) syncStatus else "Sincronizando...",
                        fontFamily = Vt323Sync,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (syncProgress >= 0) {
                        LinearProgressIndicator(
                            progress = { syncProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .border(2.dp, borderColor),
                            color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C),
                            trackColor = Color.Transparent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$syncProgress%",
                            fontFamily = Vt323Sync,
                            fontSize = 14.sp,
                            color = textColor,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .border(2.dp, borderColor),
                            color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C),
                            trackColor = Color.Transparent
                        )
                    }

                    if (activeSyncSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Líneas de subida/descarga activas:",
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        activeSyncSlots.forEach { (fileName, prog) ->
                            val isUpload = fileName.contains("SUBIDA") || fileName.contains("⬆")
                            val isDownload = fileName.contains("BAJADA") || fileName.contains("⬇")
                            
                            val slotColor = when {
                                isUpload -> if (isDark) Color(0xFF29B6F6) else Color(0xFF0288D1)
                                isDownload -> if (isDark) Color(0xFFFFB74D) else Color(0xFFF57C00)
                                else -> if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.5.dp, slotColor.copy(alpha = 0.8f))
                                    .background(slotColor.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = fileName,
                                    fontFamily = Vt323Sync,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { if (prog >= 0) (prog / 100f).coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(10.dp)
                                            .border(1.dp, borderColor),
                                        color = slotColor,
                                        trackColor = Color.Transparent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (prog >= 0) "$prog%" else "...",
                                        fontFamily = Vt323Sync,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botón Sincronizar Ahora & Desvincular
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val syncBtnBg = if (isSyncing) {
                    Color(0xFFD32F2F) // Rojo retro al sincronizar para botón de detener
                } else {
                    if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                }
                val syncLabel = if (isSyncing) "DETENER SINCRONIZACIÓN ⏹" else "SINCRONIZAR AHORA 🔄"
                
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .clickable { 
                            if (isSyncing) {
                                onStopSync()
                            } else {
                                onSyncNow()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .border(2.dp, borderColor)
                            .background(syncBtnBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = syncLabel, fontFamily = Vt323Sync, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onUnlinkGoogleDrive() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .offset(y = 6.dp)
                            .background(borderColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .border(2.dp, borderColor)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "DESVINCULAR 🚪", fontFamily = Vt323Sync, fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Vaciar Nube
            var showPasswordDialog by remember { mutableStateOf(false) }
            var passwordText by remember { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable {
                        passwordText = ""
                        showPasswordDialog = true
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .offset(y = 6.dp)
                        .background(borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(2.dp, borderColor)
                        .background(Color(0xFFD32F2F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VACIAR GOOGLE DRIVE 🗑",
                        fontFamily = Vt323Sync,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showPasswordDialog) {
                val dialogBg = if (isDark) Color(0xFF1A1A2E) else Color(0xFFFFFBEA)
                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    confirmButton = {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    if (passwordText == "1234") {
                                        showPasswordDialog = false
                                        onResetDrive()
                                    } else {
                                        onIncorrectPassword()
                                    }
                                }
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF00796B) else Color(0xFF00897B))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "CONFIRMAR",
                                fontFamily = Vt323Sync,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        Box(
                            modifier = Modifier
                                .clickable { showPasswordDialog = false }
                                .border(2.dp, borderColor)
                                .background(Color(0xFFD32F2F))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "CANCELAR",
                                fontFamily = Vt323Sync,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "🔐 VACIAR NUBE",
                            fontFamily = Vt323Sync,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Introduce la contraseña para vaciar por completo Google Drive y Firestore:",
                                fontFamily = Vt323Sync,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordText,
                                onValueChange = { passwordText = it },
                                placeholder = { Text("Contraseña", fontFamily = Vt323Sync) },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = borderColor,
                                    unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                    focusedContainerColor = if (isDark) Color(0xFF0D0D2B) else Color.White,
                                    unfocusedContainerColor = if (isDark) Color(0xFF0D0D2B) else Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    shape = RectangleShape,
                    containerColor = dialogBg,
                    tonalElevation = 0.dp
                )
            }

            // Sección de Registros de Sincronización (Logs)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))

            var showLogsDialog by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            LaunchedEffect(Unit) {
                SyncLogger.loadLogs(context)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📋 REGISTRO DE DIAGNÓSTICO Y LOGS",
                    fontFamily = Vt323Sync,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Copiar Logs Directo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, borderColor)
                            .background(if (isDark) Color(0xFF1565C0) else Color(0xFF1976D2))
                            .clickable { SyncLogger.copyToClipboard(context) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "COPIAR LOGS 📋",
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Botón Ver Logs Dialog
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, borderColor)
                            .background(if (isDark) Color(0xFF4A148C) else Color(0xFF6A1B9A))
                            .clickable { showLogsDialog = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VER REGISTROS 👁️",
                            fontFamily = Vt323Sync,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (showLogsDialog) {
                val dialogBg = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
                val logContent = SyncLogger.getFormattedLogs(context)
                val scrollState = rememberScrollState()

                AlertDialog(
                    onDismissRequest = { showLogsDialog = false },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clickable { SyncLogger.copyToClipboard(context) }
                                    .border(2.dp, borderColor)
                                    .background(if (isDark) Color(0xFF1565C0) else Color(0xFF1976D2))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("COPIAR 📋", fontFamily = Vt323Sync, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .clickable { SyncLogger.clearLogs(context) }
                                    .border(2.dp, borderColor)
                                    .background(Color(0xFFD32F2F))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("LIMPIAR 🧹", fontFamily = Vt323Sync, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .clickable { showLogsDialog = false }
                                    .border(2.dp, borderColor)
                                    .background(Color.Gray)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("CERRAR ✖", fontFamily = Vt323Sync, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    title = {
                        Text(
                            text = "📋 HISTORIAL DE LOGS DE SINCRONIZACIÓN",
                            fontFamily = Vt323Sync,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .border(2.dp, borderColor)
                                .background(if (isDark) Color(0xFF000000) else Color(0xFF1E1E1E))
                                .padding(8.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = logContent,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF00FF66)
                            )
                        }
                    },
                    shape = RectangleShape,
                    containerColor = dialogBg,
                    tonalElevation = 0.dp
                )
            }
        }
    }
}

@Composable
fun Retro3DButton(
    text: String,
    onClick: () -> Unit,
    buttonColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = 6.dp)
                .background(borderColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(3.dp, borderColor)
                .background(buttonColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = Vt323Sync,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RetroSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    textColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, borderColor)
                .background(if (checked) Color(0xFF81C784) else Color(0x22000000)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontFamily = Vt323Sync,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontFamily = Vt323Sync,
            fontSize = 18.sp,
            color = textColor
        )
    }
}
