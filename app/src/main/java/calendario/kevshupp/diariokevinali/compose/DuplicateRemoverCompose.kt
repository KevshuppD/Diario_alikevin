package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.DuplicateGroup
import calendario.kevshupp.diariokevinali.LocalPhoto
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun DuplicateRemoverScreen(
    currentTheme: String,
    selectedFolderUri: String?,
    isScanning: Boolean,
    duplicateGroups: List<DuplicateGroup>,
    scanCompleted: Boolean,
    scannedCount: Int,
    totalToScan: Int,
    deletedCount: Int,
    freedSpaceBytes: Long,
    isDeleting: Boolean,
    onScan: () -> Unit,
    onDelete: (List<LocalPhoto>) -> Unit,
    onReset: () -> Unit,
    textColor: Color,
    borderColor: Color,
    isDark: Boolean
) {
    val Vt323 = FontFamily(Font(R.font.vt323))
    
    val cardBgColor = when {
        isDark -> Color(0xFF1E1E1E)
        currentTheme == "Pixel Monocromático" -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    if (selectedFolderUri.isNullOrEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(cardBgColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ CONFIGURACIÓN INCOMPLETA",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Por favor, selecciona primero tu carpeta local de fotos en la pestaña de Sincronización.",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor)
            .background(cardBgColor)
            .padding(16.dp)
    ) {
        if (!isScanning && !scanCompleted && deletedCount == 0 && !isDeleting) {
            Text(
                text = "ESCANEAR EN BUSCA DE REPETIDOS",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Se analizarán todas las imágenes en la carpeta del álbum para buscar archivos idénticos mediante su firma de contenido MD5.",
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onScan() }
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
                    Text(
                        text = "🔍 INICIAR ESCANEO",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (isScanning) {
            Text(
                text = "ESCANEANDO CARPETA DE FOTOS...",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progressText = if (totalToScan > 0) "Procesando $scannedCount de $totalToScan archivos..." else "Buscando fotos locales..."
            Text(
                text = progressText,
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            val progressVal = if (totalToScan > 0) scannedCount.toFloat() / totalToScan else 0f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .border(2.dp, borderColor),
                color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C),
                trackColor = Color.Transparent
            )
        } else if (isDeleting) {
            Text(
                text = "ELIMINANDO IMÁGENES DUPLICADAS...",
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progressText = if (scannedCount > 0) "Eliminando $scannedCount de $totalToScan..." else "Borrando archivos..."
            Text(
                text = progressText,
                fontFamily = Vt323,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            val progressVal = if (totalToScan > 0) scannedCount.toFloat() / totalToScan else 0f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .border(2.dp, borderColor),
                color = Color(0xFFD32F2F),
                trackColor = Color.Transparent
            )
        } else if (deletedCount > 0) {
            Text(
                text = "✨ LIMPIEZA COMPLETADA",
                fontFamily = Vt323,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val sizeMb = freedSpaceBytes / (1024.0 * 1024.0)
            val sizeText = if (sizeMb >= 0.1) String.format("%.1f MB", sizeMb) else String.format("%.1f KB", freedSpaceBytes / 1024.0)
            Text(
                text = "Se han eliminado con éxito $deletedCount imágenes duplicadas, liberando $sizeText de espacio local.",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onReset() }
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
                    Text(
                        text = "ENTENDIDO",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (scanCompleted) {
            if (duplicateGroups.isEmpty()) {
                Text(
                    text = "🎉 ¡TODO LIMPIO!",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF00E676) else Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No se encontraron imágenes duplicadas en la carpeta seleccionada.",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { onReset() }
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
                        Text(
                            text = "VOLVER",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val totalDuplicates = duplicateGroups.sumOf { it.duplicates.size }
                val totalSize = duplicateGroups.sumOf { g -> g.duplicates.sumOf { it.size } }
                val sizeMb = totalSize / (1024.0 * 1024.0)
                val sizeText = if (sizeMb >= 0.1) String.format("%.1f MB", sizeMb) else String.format("%.1f KB", totalSize / 1024.0)

                Text(
                    text = "DUPLICADOS DETECTADOS",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Se encontraron $totalDuplicates imágenes repetidas (Ahorro estimado: $sizeText).",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { 
                            val allDuplicates = duplicateGroups.flatMap { it.duplicates }
                            onDelete(allDuplicates)
                        }
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
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗑️ ELIMINAR TODOS LOS DUPLICADOS",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Lista de duplicados encontrados:",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    duplicateGroups.forEach { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor.copy(alpha = 0.4f))
                                .background(textColor.copy(alpha = 0.03f))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .border(1.dp, borderColor)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(group.original.uri)
                                            .size(120)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = group.original.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Original: ${group.original.name}",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val origMb = group.original.size / (1024.0 * 1024.0)
                                    val origSizeText = if (origMb >= 0.1) String.format("%.1f MB", origMb) else "${group.original.size / 1024} KB"
                                    Text(
                                        text = "Tamaño: $origSizeText",
                                        fontFamily = Vt323,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            group.duplicates.forEach { dup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "↳ Repetida: ${dup.name}",
                                        fontFamily = Vt323,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDelete(listOf(dup)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🗑️", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
