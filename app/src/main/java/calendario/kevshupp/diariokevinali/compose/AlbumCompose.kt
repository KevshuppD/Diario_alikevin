package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.LocalPhoto
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

private val AlbumVt323 = FontFamily(Font(R.font.vt323))

@Composable
fun AlbumScreen(
    moments: List<Message>,
    localPhotos: List<LocalPhoto>,
    localFolderConfigured: Boolean,
    isLocalPhotosLoading: Boolean = false,
    onReloadLocalPhotos: () -> Unit = {},
    theme: String = "Pixel Claro",
    onAddMoment: () -> Unit,
    onOpenMoment: (Message) -> Unit,
    onEditMoment: (Message) -> Unit,
    onDeleteMoment: (Message) -> Unit,
    onOpenLocalPhoto: (String) -> Unit,
    isOwner: (Message) -> Boolean
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val backgroundColor = getAppBackgroundColor(theme)
    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    val subtitleColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.DarkGray
        else -> Color(0xFF8B4513)
    }
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    val cardBgColor = when {
        isDark -> Color(0xFF1A1A2E)
        isMono -> Color.White
        else -> Color(0xFFFFFDF9)
    }
    val buttonBgColor = when {
        isDark -> Color(0xFF1A1A2E)
        isMono -> Color.White
        else -> Color(0xFF8B4513)
    }
    val buttonContentColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color.White
    }

    // Navegación interna: null = selector de álbumes, "diario" = momentos, "general" = fotos drive
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var gridColumnsDiario by remember { mutableStateOf(2) }
    var gridColumnsGeneral by remember { mutableStateOf(2) }
    var showColumnsMenu by remember { mutableStateOf(false) }
    var showColsSubMenu by remember { mutableStateOf(false) }
    var showSortSubMenu by remember { mutableStateOf(false) }
    var showFilterSubMenu by remember { mutableStateOf(false) }

    // Estados de orden y filtrado
    var sortBy by remember { mutableStateOf("fecha_desc") }
    var filterBy by remember { mutableStateOf("todo") }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        if (selectedAlbum == null) {
            // --- VISTA DE CARPETAS / ÁLBUMES ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBgColor)
                    .border(3.dp, borderColor)
                    .drawBehind {
                        val shadowOffset = 4.dp.toPx()
                        drawRect(
                            color = borderColor.copy(alpha = 0.2f),
                            topLeft = Offset(shadowOffset, shadowOffset),
                            size = this.size
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "📸 GALE-DIARIO PIXEL",
                        fontFamily = AlbumVt323,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "SELECCIONA UN ÁLBUM DE FOTOS",
                        fontFamily = AlbumVt323,
                        fontSize = 14.sp,
                        color = subtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Carpeta 1: Diario de Recuerdos
                FolderItem(
                    title = "Diario de Recuerdos",
                    description = "Fotos escritas asociadas a cartas de amor diarias",
                    badgeText = "${moments.size} ítems",
                    iconResId = R.drawable.ic_album_pixel,
                    theme = theme,
                    borderColor = borderColor,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { selectedAlbum = "diario" }
                )

                // Carpeta 2: Álbum General
                val driveBadge = if (localFolderConfigured) "${localPhotos.size} fotos" else "No config."
                FolderItem(
                    title = "Álbum General (Drive)",
                    description = "Galería completa de fotos sincronizadas desde tu carpeta",
                    badgeText = driveBadge,
                    iconResId = R.drawable.ic_settings_pixel,
                    theme = theme,
                    borderColor = borderColor,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { selectedAlbum = "general" }
                )
            }
        } else {
            // --- VISTA DE GALERÍA DE UN ÁLBUM ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    fontFamily = AlbumVt323,
                    fontSize = 24.sp,
                    color = textColor,
                    modifier = Modifier
                        .clickable { selectedAlbum = null }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedAlbum == "diario") "DIARIO DE FOTOS" else "ÁLBUM GENERAL",
                    fontFamily = AlbumVt323,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                if (selectedAlbum == "general" && localFolderConfigured) {
                    IconButton(
                        onClick = onReloadLocalPhotos
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Recargar",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box {
                    Text(
                        text = "⋮",
                        fontFamily = AlbumVt323,
                        fontSize = 24.sp,
                        color = textColor,
                        modifier = Modifier
                            .clickable { showColumnsMenu = true }
                            .padding(8.dp)
                    )
                    DropdownMenu(
                        expanded = showColumnsMenu,
                        onDismissRequest = { showColumnsMenu = false },
                        modifier = Modifier
                            .background(cardBgColor)
                            .border(2.dp, borderColor)
                    ) {
                        // 1. Submenú de Columnas (Ancho de cuadrícula)
                        Box {
                            DropdownMenuItem(
                                text = { Text("Ancho cuadrícula ▸", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                onClick = { showColsSubMenu = true }
                            )
                            DropdownMenu(
                                expanded = showColsSubMenu,
                                onDismissRequest = { showColsSubMenu = false },
                                modifier = Modifier
                                    .background(cardBgColor)
                                    .border(2.dp, borderColor)
                            ) {
                                val currentCols = if (selectedAlbum == "diario") gridColumnsDiario else gridColumnsGeneral
                                (2..6).forEach { cols ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (currentCols == cols) "✓ $cols columnas" else "  $cols columnas",
                                                fontFamily = AlbumVt323,
                                                fontSize = 16.sp,
                                                color = textColor
                                            )
                                        },
                                        onClick = {
                                            if (selectedAlbum == "diario") {
                                                gridColumnsDiario = cols
                                            } else {
                                                gridColumnsGeneral = cols
                                            }
                                            showColsSubMenu = false
                                            showColumnsMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedAlbum == "general") {
                            // 2. Submenú de Ordenación
                            Box {
                                DropdownMenuItem(
                                    text = { Text("Ordenar por ▸", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                    onClick = { showSortSubMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showSortSubMenu,
                                    onDismissRequest = { showSortSubMenu = false },
                                    modifier = Modifier
                                        .background(cardBgColor)
                                        .border(2.dp, borderColor)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (sortBy == "fecha_desc") "✓ Más recientes" else "  Más recientes", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { sortBy = "fecha_desc"; showSortSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (sortBy == "fecha_asc") "✓ Más antiguos" else "  Más antiguos", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { sortBy = "fecha_asc"; showSortSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (sortBy == "size_desc") "✓ Tamaño (Más grandes)" else "  Tamaño (Más grandes)", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { sortBy = "size_desc"; showSortSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (sortBy == "name_asc") "✓ Nombre (A-Z)" else "  Nombre (A-Z)", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { sortBy = "name_asc"; showSortSubMenu = false; showColumnsMenu = false }
                                    )
                                }
                            }

                            // 3. Submenú de Filtrado
                            Box {
                                DropdownMenuItem(
                                    text = { Text("Filtrar por ▸", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                    onClick = { showFilterSubMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showFilterSubMenu,
                                    onDismissRequest = { showFilterSubMenu = false },
                                    modifier = Modifier
                                        .background(cardBgColor)
                                        .border(2.dp, borderColor)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (filterBy == "todo") "✓ Mostrar todo" else "  Mostrar todo", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { filterBy = "todo"; showFilterSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (filterBy == "hoy") "✓ Agregado hoy" else "  Agregado hoy", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { filterBy = "hoy"; showFilterSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (filterBy == "semana") "✓ Agregado esta semana" else "  Agregado esta semana", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { filterBy = "semana"; showFilterSubMenu = false; showColumnsMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (filterBy == "grandes") "✓ Archivos grandes (>2MB)" else "  Archivos grandes (>2MB)", fontFamily = AlbumVt323, fontSize = 16.sp, color = textColor) },
                                        onClick = { filterBy = "grandes"; showFilterSubMenu = false; showColumnsMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input de búsqueda para el Álbum General
            if (selectedAlbum == "general" && localFolderConfigured && localPhotos.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar foto por nombre...", fontFamily = AlbumVt323, fontSize = 16.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = AlbumVt323, fontSize = 18.sp, color = textColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBgColor,
                        unfocusedContainerColor = cardBgColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = subtitleColor,
                        cursorColor = textColor
                    )
                )
            }

            if (selectedAlbum == "diario") {
                // Galería del Diario de Recuerdos (Estilo polaroid con separación)
                if (moments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "¡Aún no hay momentos! Agrega uno ❤️",
                            fontFamily = AlbumVt323,
                            fontSize = 20.sp,
                            color = subtitleColor
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumnsDiario),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = moments,
                            key = { it.messageId ?: "moment_${it.timestamp}" }
                        ) { moment ->
                            AlbumGridItem(
                                moment = moment,
                                theme = theme,
                                cardBgColor = cardBgColor,
                                borderColor = borderColor,
                                textColor = textColor,
                                subtitleColor = subtitleColor,
                                onOpenMoment = onOpenMoment,
                                onEditMoment = onEditMoment,
                                onDeleteMoment = onDeleteMoment,
                                isOwner = isOwner
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddMoment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .drawBehind {
                            val shadowOffset = 4.dp.toPx()
                            drawRect(
                                color = borderColor.copy(alpha = 0.2f),
                                topLeft = Offset(shadowOffset, shadowOffset),
                                size = this.size
                            )
                        },
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonContentColor
                    )
                ) {
                    Text(
                        text = "✨ AÑADIR NUEVO RECUERDO ✨",
                        fontFamily = AlbumVt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Galería del Álbum General (Drive / local) - TIGHT GRID como detalle del momento (pegadas)
                if (!localFolderConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Por favor, configura la sincronización y la carpeta de fotos local en Configuración -> Sincronización para ver este álbum ☁️",
                            fontFamily = AlbumVt323,
                            fontSize = 18.sp,
                            color = subtitleColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (isLocalPhotosLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Buscando fotos locales... 🔄",
                            fontFamily = AlbumVt323,
                            fontSize = 20.sp,
                            color = subtitleColor
                        )
                    }
                } else if (localPhotos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron fotos en la carpeta local. Espera a que termine la sincronización o agrega fotos a tu carpeta compartida 📸",
                            fontFamily = AlbumVt323,
                            fontSize = 18.sp,
                            color = subtitleColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Procesar fotos locales (buscar + filtrar + ordenar)
                    val filteredPhotos = remember(localPhotos, sortBy, filterBy, searchQuery) {
                        var list = localPhotos
                        
                        // 1. Filtrar por búsqueda de nombre
                        if (searchQuery.isNotEmpty()) {
                            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        
                        // 2. Filtrar por categorías
                        val now = System.currentTimeMillis()
                        list = when (filterBy) {
                            "hoy" -> {
                                val oneDay = 24 * 60 * 60 * 1000L
                                list.filter { now - it.lastModified < oneDay }
                            }
                            "semana" -> {
                                val oneWeek = 7 * 24 * 60 * 60 * 1000L
                                list.filter { now - it.lastModified < oneWeek }
                            }
                            "grandes" -> {
                                val twoMB = 2 * 1024 * 1024L
                                list.filter { it.size > twoMB }
                            }
                            else -> list
                        }
                        
                        // 3. Ordenar
                        when (sortBy) {
                            "fecha_desc" -> list.sortedByDescending { it.lastModified }
                            "fecha_asc" -> list.sortedBy { it.lastModified }
                            "size_desc" -> list.sortedByDescending { it.size }
                            "name_asc" -> list.sortedBy { it.name.lowercase() }
                            else -> list
                        }
                    }

                    if (filteredPhotos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ninguna foto coincide con el filtro o búsqueda 🔍",
                                fontFamily = AlbumVt323,
                                fontSize = 18.sp,
                                color = subtitleColor
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumnsGeneral),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 8.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = filteredPhotos,
                                key = { it.uri }
                            ) { photo ->
                                LocalPhotoGridItem(
                                    photo = photo,
                                    borderColor = borderColor,
                                    onClick = { onOpenLocalPhoto(photo.uri) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(
    title: String,
    description: String,
    badgeText: String,
    iconResId: Int,
    theme: String,
    borderColor: Color,
    cardBgColor: Color,
    textColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val shadowOffset = 6.dp.toPx()
                drawRect(
                    color = borderColor.copy(alpha = 0.2f),
                    topLeft = Offset(shadowOffset, shadowOffset),
                    size = this.size
                )
            }
            .background(cardBgColor)
            .border(3.dp, borderColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(2.dp, borderColor)
                    .background(if (theme == "Pixel Oscuro") Color(0xFF0F0F3D) else Color(0x11000000))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = AlbumVt323,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontFamily = AlbumVt323,
                    fontSize = 14.sp,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = borderColor,
                shape = RectangleShape
            ) {
                Text(
                    text = badgeText,
                    fontFamily = AlbumVt323,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LocalPhotoGridItem(
    photo: LocalPhoto,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(0.5.dp, borderColor.copy(alpha = 0.4f))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .size(300)
                .crossfade(true)
                .build(),
            contentDescription = photo.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
    moment: Message,
    theme: String,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    subtitleColor: Color,
    onOpenMoment: (Message) -> Unit,
    onEditMoment: (Message) -> Unit,
    onDeleteMoment: (Message) -> Unit,
    isOwner: (Message) -> Boolean
) {
    val urls = moment.imageUrls ?: emptyList()
    val displayUrl = urls.firstOrNull()
    var menuExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(moment.timestamp) { dateFormat.format(Date(moment.timestamp)) }

    val cleanCaption = remember(moment.content) {
        val content = moment.content ?: ""
        val raw = if (content.startsWith("[ALBUM]")) {
            content.substringAfter("[ALBUM]").trim()
        } else {
            content
        }
        if (raw.contains("<") || raw.contains("&")) {
            android.text.Html.fromHtml(raw, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } else {
            raw
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val shadowOffset = 4.dp.toPx()
                drawRect(
                    color = borderColor.copy(alpha = 0.2f),
                    topLeft = Offset(shadowOffset, shadowOffset),
                    size = this.size
                )
            }
            .background(cardBgColor)
            .border(2.dp, borderColor)
            .combinedClickable(
                onClick = { onOpenMoment(moment) },
                onLongClick = {
                    if (isOwner(moment)) menuExpanded = true
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            if (displayUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(if (theme == "Pixel Oscuro") Color(0xFF070714) else Color(0xFFEFE6D5))
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷 Sin foto",
                        fontFamily = AlbumVt323,
                        fontSize = 16.sp,
                        color = subtitleColor
                    )
                }
            } else {
                AsyncImage(
                    model = displayUrl.optimizeCloudinary(400),
                    contentDescription = "Foto del album",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .border(1.dp, borderColor),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = cleanCaption.ifBlank { "Un hermoso recuerdo ❤️" },
                fontFamily = AlbumVt323,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "De: ${moment.authorName?.take(8) ?: "Amor"}",
                    fontFamily = AlbumVt323,
                    fontSize = 11.sp,
                    color = subtitleColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    fontFamily = AlbumVt323,
                    fontSize = 11.sp,
                    color = subtitleColor.copy(alpha = 0.8f)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier
                .background(cardBgColor)
                .border(1.dp, borderColor)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Editar momento",
                        fontFamily = AlbumVt323,
                        fontSize = 16.sp,
                        color = textColor
                    )
                },
                onClick = {
                    menuExpanded = false
                    onEditMoment(moment)
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Eliminar momento",
                        fontFamily = AlbumVt323,
                        fontSize = 16.sp,
                        color = Color.Red
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDeleteMoment(moment)
                }
            )
        }
    }
}
