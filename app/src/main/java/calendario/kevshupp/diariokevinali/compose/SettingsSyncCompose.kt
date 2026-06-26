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
import calendario.kevshupp.diariokevinali.R

private val Vt323Sync = FontFamily(Font(R.font.vt323))

@Composable
fun SettingsSyncCompose(
    currentTheme: String,
    googleAccountEmail: String?, // Null si no está vinculado
    selectedFolderUri: String?, // Uri.toString() local o null
    syncIntervalMinutes: Long,
    wifiOnly: Boolean,
    chargingOnly: Boolean,
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
    syncStatus: String = ""
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
            Divider(color = borderColor.copy(alpha = 0.3f), thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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
