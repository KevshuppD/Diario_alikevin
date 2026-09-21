package calendario.kevshupp.diariokevinali.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.AppUpdateInfo

@Composable
fun UpdateDialogCompose(
    updateInfo: AppUpdateInfo?,
    downloadProgress: Int?, // null = idle, 0..99 = downloading, 100 = complete/installing
    theme: String,
    onDownloadAndInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    if (updateInfo == null) return

    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val bgColor = when {
        isMono -> Color.White
        isDark -> Color(0xFF1E1E2E)
        else -> Color(0xFFFFF8E7)
    }

    val textColor = when {
        isMono -> Color.Black
        isDark -> Color(0xFFE0E0E0)
        else -> Color(0xFF4A2511)
    }

    val borderColor = when {
        isMono -> Color.Black
        isDark -> Color(0xFFFF80AB)
        else -> Color(0xFF4A2511)
    }

    val headerBg = when {
        isMono -> Color.Black
        isDark -> Color(0xFF311B92)
        else -> Color(0xFF673AB7)
    }

    val accentColor = when {
        isMono -> Color.Black
        isDark -> Color(0xFFFF80AB)
        else -> Color(0xFFE91E63)
    }

    val isDownloading = downloadProgress != null && downloadProgress < 100
    val isInstalling = downloadProgress != null && downloadProgress >= 100

    Dialog(
        onDismissRequest = {
            if (!isDownloading && !isInstalling) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading && !isInstalling,
            dismissOnClickOutside = !isDownloading && !isInstalling,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Sombra retro 3D
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(Color(0xFF111111), RectangleShape)
            )

            // Contenedor principal del diálogo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, borderColor, RectangleShape)
                    .background(bgColor, RectangleShape)
            ) {
                // Header del Diálogo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚀 ACTUALIZACIÓN DISPONIBLE",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Línea divisoria
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(borderColor)
                )

                // Cuerpo del Diálogo (Scrollable acotado para prevenir overflow)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Comparador de versiones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Versión Actual
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VERSIÓN ACTUAL",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .border(1.5.dp, borderColor.copy(alpha = 0.5f))
                                    .background(if (isDark) Color(0xFF2A2A3C) else Color(0xFFE8DCB8))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "v${updateInfo.currentVersion.replace("v", "")}",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }

                        Text(
                            text = "➔",
                            fontFamily = Vt323,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        // Versión Nueva
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NUEVA VERSIÓN",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .border(2.dp, accentColor)
                                    .background(if (isDark) Color(0xFF3D1B4D) else Color(0xFFFFD54F))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (updateInfo.versionName.startsWith("v")) updateInfo.versionName else "v${updateInfo.versionName}",
                                    fontFamily = Vt323,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFF80AB) else Color(0xFF2E1C0C)
                                )
                            }
                        }
                    }

                    if (updateInfo.apkSizeBytes > 0L) {
                        val mbSize = String.format(java.util.Locale.US, "%.1f MB", updateInfo.apkSizeBytes / (1024.0 * 1024.0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📦 Tamaño del paquete: $mbSize",
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Caja de Novedades / Release Notes
                    Text(
                        text = "📝 NOVEDADES:",
                        fontFamily = Vt323,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val notes = if (updateInfo.releaseNotes.isNotBlank()) {
                        updateInfo.releaseNotes
                    } else {
                        "• Mejoras de rendimiento y optimizaciones generales.\n• Corrección de errores y mayor estabilidad en sincronización."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 150.dp)
                            .border(1.5.dp, borderColor.copy(alpha = 0.6f))
                            .background(if (isDark) Color(0xFF141420) else Color(0xFFF0E5CE))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = notes,
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sección de Progreso o Botones de Acción
                    if (isDownloading) {
                        val progress = downloadProgress ?: 0
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏳ DESCARGANDO ACTUALIZACIÓN: $progress%",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Barra de progreso Pixel Art
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .border(2.dp, borderColor)
                                    .background(if (isDark) Color(0xFF222233) else Color(0xFFE0D4B8))
                            ) {
                                val fraction = (progress / 100f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .background(if (isDark) Color(0xFFFF4081) else Color(0xFF4CAF50))
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Botón Cancelar descarga
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clickable { onCancelDownload() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .offset(y = 4.dp)
                                        .background(borderColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .border(2.dp, borderColor)
                                        .background(Color(0xFFD32F2F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✖ CANCELAR DESCARGA",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else if (isInstalling) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚡ INSTALANDO ACTUALIZACIÓN...",
                                fontFamily = Vt323,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Si el sistema te solicita confirmación, pulsa Actualizar.",
                                fontFamily = Vt323,
                                fontSize = 15.sp,
                                color = textColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Botones de acción (Descargar / Más tarde)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Botón Principal Descargar
                            val btnBg = if (isDark) Color(0xFF00796B) else Color(0xFF43A047)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable { onDownloadAndInstall() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .offset(y = 5.dp)
                                        .background(borderColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .border(2.5.dp, borderColor)
                                        .background(btnBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚡ DESCARGAR E INSTALAR",
                                        fontFamily = Vt323,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botón Secundario Más tarde
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { onDismiss() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .offset(y = 4.dp)
                                        .background(borderColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .border(2.dp, borderColor)
                                        .background(if (isDark) Color(0xFF424242) else Color(0xFF8D6E63)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "MÁS TARDE",
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
