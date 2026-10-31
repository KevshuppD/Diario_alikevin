package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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

    // Pestaña visible: "diario" = momentos, "general" = fotos drive
    var selectedAlbum by remember { mutableStateOf("diario") }
    var gridColumnsDiario by remember { mutableStateOf(2) }
    var gridColumnsGeneral by remember { mutableStateOf(2) }

    // Estados de orden y filtrado
    var sortBy by remember { mutableStateOf("fecha_desc") }
    var filterBy by remember { mutableStateOf("todo") }
    var searchQuery by remember { mutableStateOf("") }

    val isDiario = selectedAlbum == "diario"
    val generalBadge = if (localFolderConfigured) "${localPhotos.size}" else "No config."
    val chipColors = AlbumChipColors(
        text = textColor,
        border = borderColor,
        background = cardBgColor,
        selected = when {
            isDark -> Color(0xFFFF4081)
            isMono -> Color.Black
            else -> Color(0xFF8B4513)
        },
        onSelected = when {
            isDark -> Color(0xFF14061A)
            else -> Color.White
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Álbum",
                fontFamily = AlbumVt323,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (!isDiario && localFolderConfigured) {
                IconButton(onClick = onReloadLocalPhotos, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Recargar",
                        tint = textColor
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AlbumChip("Recuerdos · ${moments.size}", isDiario, chipColors, Modifier.weight(1f)) { selectedAlbum = "diario" }
            AlbumChip("General · $generalBadge", !isDiario, chipColors, Modifier.weight(1f)) { selectedAlbum = "general" }
        }

        // Input de búsqueda para el Álbum General
        if (!isDiario && localFolderConfigured && localPhotos.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar foto por nombre...", fontFamily = AlbumVt323, fontSize = 16.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = AlbumVt323, fontSize = 18.sp, color = textColor),
                modifier = Modifier.fillMaxWidth(),
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
            AlbumChipRow(ALBUM_SORTS, sortBy, chipColors) { sortBy = it }
            AlbumChipRow(ALBUM_FILTERS, filterBy, chipColors) { filterBy = it }
        }

        AlbumChipRow(
            options = ALBUM_COLUMNS,
            selected = if (isDiario) gridColumnsDiario else gridColumnsGeneral,
            colors = chipColors
        ) { cols -> if (isDiario) gridColumnsDiario = cols else gridColumnsGeneral = cols }

        if (isDiario) {
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
                val groups = remember(moments) { groupByMonth(moments) { it.timestamp } }
                MonthGroupedGrid(
                    groups = groups,
                    columns = gridColumnsDiario,
                    spacing = 8.dp,
                    itemKey = { it.messageId ?: "moment_${it.timestamp}" },
                    backgroundColor = backgroundColor,
                    labelColor = subtitleColor,
                    borderColor = borderColor,
                    modifier = Modifier.weight(1f)
                ) { moment ->
                    AlbumGridItem(
                        moment = moment,
                        columns = gridColumnsDiario,
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

            Button(
                onClick = onAddMoment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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
            // Galería del Álbum General (Drive / local) - fotos pegadas
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
                    // Los títulos por mes solo tienen sentido cuando el orden es por fecha
                    val groups = remember(filteredPhotos, sortBy) {
                        if (sortBy.startsWith("fecha")) groupByMonth(filteredPhotos) { it.lastModified }
                        else listOf(MonthGroup("", filteredPhotos))
                    }
                    MonthGroupedGrid(
                        groups = groups,
                        columns = gridColumnsGeneral,
                        spacing = 2.dp,
                        itemKey = { it.uri },
                        backgroundColor = backgroundColor,
                        labelColor = subtitleColor,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1f)
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

private val ALBUM_SORTS = listOf("fecha_desc" to "Recientes", "fecha_asc" to "Antiguos", "size_desc" to "Tamaño", "name_asc" to "A-Z")
private val ALBUM_FILTERS = listOf("todo" to "Todo", "hoy" to "Hoy", "semana" to "Semana", "grandes" to "+2 MB")
private val ALBUM_COLUMNS = (2..6).map { it to "$it col" }

private class AlbumChipColors(
    val text: Color,
    val border: Color,
    val background: Color,
    val selected: Color,
    val onSelected: Color
)

@Composable
private fun AlbumChip(label: String, selected: Boolean, colors: AlbumChipColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = AlbumVt323,
        fontSize = 17.sp,
        maxLines = 1,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = if (selected) colors.onSelected else colors.text,
        modifier = modifier
            .border(2.dp, colors.border)
            .background(if (selected) colors.selected else colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
private fun <T> AlbumChipRow(options: List<Pair<T, String>>, selected: T, colors: AlbumChipColors, onSelect: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            AlbumChip(label, value == selected, colors, Modifier.weight(1f)) { onSelect(value) }
        }
    }
}

private class MonthGroup<T>(val label: String, val items: List<T>)

/** Agrupa una lista ya ordenada por fecha en bloques consecutivos del mismo mes. */
private fun <T> groupByMonth(items: List<T>, timeOf: (T) -> Long): List<MonthGroup<T>> {
    val locale = Locale("es", "CL")
    val format = SimpleDateFormat("MMMM yyyy", locale)
    val groups = mutableListOf<MonthGroup<T>>()
    var label: String? = null
    var current = mutableListOf<T>()
    for (item in items) {
        val itemLabel = format.format(Date(timeOf(item))).replaceFirstChar { it.uppercase(locale) }
        if (itemLabel != label) {
            label?.let { groups.add(MonthGroup(it, current)) }
            label = itemLabel
            current = mutableListOf()
        }
        current.add(item)
    }
    label?.let { groups.add(MonthGroup(it, current)) }
    return groups
}

/** Grilla con títulos por mes; el mes que se está viendo queda fijo arriba al hacer scroll. */
@Composable
private fun <T : Any> MonthGroupedGrid(
    groups: List<MonthGroup<T>>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    itemKey: (T) -> Any,
    backgroundColor: Color,
    labelColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    val state = rememberLazyGridState()
    // Índice en la grilla donde empieza cada mes
    val starts = remember(groups) {
        var index = 0
        groups.map { group ->
            val start = index
            index += group.items.size + 1
            start
        }
    }
    val currentGroup by remember(groups, starts) {
        derivedStateOf {
            val first = state.firstVisibleItemIndex
            groups.getOrNull(starts.indexOfLast { it <= first })
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            groups.forEachIndexed { index, group ->
                item(key = "month_$index", span = { GridItemSpan(maxLineSpan) }) {
                    if (group.label.isEmpty()) Spacer(Modifier.height(1.dp))
                    else MonthHeader(group, backgroundColor, labelColor, borderColor)
                }
                items(items = group.items, key = itemKey) { item -> itemContent(item) }
            }
        }
        currentGroup?.takeIf { it.label.isNotEmpty() }?.let { group ->
            MonthHeader(group, backgroundColor, labelColor, borderColor)
        }
    }
}

@Composable
private fun <T> MonthHeader(group: MonthGroup<T>, backgroundColor: Color, labelColor: Color, borderColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .drawBehind {
                drawRect(borderColor, topLeft = Offset(0f, size.height - 2.dp.toPx()))
            }
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(group.label, fontFamily = AlbumVt323, fontSize = 20.sp, color = labelColor)
        Text("${group.items.size}", fontFamily = AlbumVt323, fontSize = 18.sp, color = labelColor)
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
    columns: Int,
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
            modifier = Modifier.padding(if (columns >= 4) 3.dp else 6.dp)
        ) {
            if (displayUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
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
                val optimizedDisplayUrl = remember(displayUrl) { displayUrl.optimizeCloudinary(400) }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(optimizedDisplayUrl)
                        .size(400)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto del album",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.dp, borderColor),
                    contentScale = ContentScale.Crop
                )
            }

            // Con 4 o más columnas no cabe texto legible: se ve al abrir el recuerdo
            if (columns < 4) {
            Spacer(modifier = Modifier.height(6.dp))

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
                    text = "De: ${moment.authorName ?: "Amor"}",
                    fontFamily = AlbumVt323,
                    fontSize = 14.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    fontFamily = AlbumVt323,
                    fontSize = 14.sp,
                    color = subtitleColor
                )
            }
            }
        }

        if (isOwner(moment) && columns < 4) {
            Text(
                text = "⋮",
                fontFamily = AlbumVt323,
                fontSize = 20.sp,
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .background(cardBgColor)
                    .border(1.dp, borderColor)
                    .clickable { menuExpanded = true }
            )
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
