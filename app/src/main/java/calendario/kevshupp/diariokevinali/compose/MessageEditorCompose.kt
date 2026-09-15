package calendario.kevshupp.diariokevinali.compose

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageEditorDialog(
    initialMessage: Message?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, imageUrl: String?) -> Unit,
    onPickImage: () -> Unit,
    currentSelectedImageUrl: String? = null
) {
    var title by remember { mutableStateOf(initialMessage?.title ?: "") }
    var contentValue by remember {
        val initialText = initialMessage?.content ?: ""
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var imageUrl by remember { mutableStateOf(initialMessage?.imageUrl ?: currentSelectedImageUrl) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Escribir, 1 = Vista Previa, 2 = Plantillas

    var showColors by remember { mutableStateOf(false) }
    var showSizes by remember { mutableStateOf(false) }
    var showEmojis by remember { mutableStateOf(false) }

    // Historial para Deshacer / Rehacer
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun updateContentWithHistory(newValue: TextFieldValue) {
        if (newValue.text != contentValue.text) {
            if (undoStack.size > 25) undoStack.removeAt(0)
            undoStack.add(contentValue)
            redoStack.clear()
        }
        contentValue = newValue
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeLast()
            redoStack.add(contentValue)
            contentValue = previous
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.add(contentValue)
            contentValue = next
        }
    }

    // Insertar texto o aplicar formato
    fun applyFormat(tagStart: String, tagEnd: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val start = Math.max(0, Math.min(selection.start, text.length))
        val end = Math.max(0, Math.min(selection.end, text.length))

        val selectedText = text.substring(start, end)
        val newText = text.substring(0, start) + tagStart + selectedText + tagEnd + text.substring(end)
        val newCursorPos = if (start == end) start + tagStart.length else start + tagStart.length + selectedText.length + tagEnd.length

        updateContentWithHistory(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos)
            )
        )
    }

    fun insertTextAtCursor(snippet: String) {
        val text = contentValue.text
        val selection = contentValue.selection
        val start = Math.max(0, Math.min(selection.start, text.length))
        val end = Math.max(0, Math.min(selection.end, text.length))

        val newText = text.substring(0, start) + snippet + text.substring(end)
        val newCursorPos = start + snippet.length

        updateContentWithHistory(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos)
            )
        )
    }

    // Actualizar imageUrl si cambia externamente
    LaunchedEffect(currentSelectedImageUrl) {
        if (currentSelectedImageUrl != null) {
            imageUrl = currentSelectedImageUrl
        }
    }

    // Estadísticas del texto
    val wordsCount = remember(contentValue.text) {
        val plain = contentValue.text.replace(Regex("<[^>]*>"), " ").trim()
        if (plain.isBlank()) 0 else plain.split(Regex("\\s+")).size
    }
    val charsCount = remember(contentValue.text) {
        contentValue.text.replace(Regex("<[^>]*>"), "").length
    }
    val readTimeMin = remember(wordsCount) {
        Math.max(1, (wordsCount / 130))
    }

    // Paleta de Colores Retro & Romántica
    val bgModal = if (isDark) Color(0xFF1E1F29) else Color(0xFFFAF4E8)
    val cardParchment = if (isDark) Color(0xFF161720) else Color(0xFFFFFDF8)
    val textColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF3D2314)
    val textSubtle = if (isDark) Color(0xFF9E9EA7) else Color(0xFF7C5C4C)
    val accentPrimary = if (isDark) Color(0xFFBA68C8) else Color(0xFF8E24AA)
    val accentBorder = if (isDark) Color(0xFF7B1FA2) else Color(0xFF5D2E7A)
    val borderColor = if (isDark) Color(0xFF424254) else Color(0xFF8B4513)
    val toolbarBg = if (isDark) Color(0xFF252736) else Color(0xFFEFE2CB)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .border(3.dp, accentBorder),
                color = bgModal,
                shape = RectangleShape,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // 🌟 HEADER MODERNO PIXEL-ART
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(toolbarBg)
                            .border(1.dp, borderColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "💌",
                                fontSize = 22.sp
                            )
                            Text(
                                text = if (initialMessage == null) "Escribir Carta de Amor" else "Editar Carta",
                                fontFamily = Vt323,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🌟 PESTAÑAS (Escribir | Vista Previa | Plantillas)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabItem(
                            label = "Escribir ✍️",
                            selected = activeTab == 0,
                            isDark = isDark,
                            accentColor = accentBorder,
                            modifier = Modifier.weight(1f)
                        ) { activeTab = 0 }

                        TabItem(
                            label = "Carta 📜",
                            selected = activeTab == 1,
                            isDark = isDark,
                            accentColor = accentBorder,
                            modifier = Modifier.weight(1f)
                        ) { activeTab = 1 }

                        TabItem(
                            label = "Plantillas 💖",
                            selected = activeTab == 2,
                            isDark = isDark,
                            accentColor = accentBorder,
                            modifier = Modifier.weight(1f)
                        ) { activeTab = 2 }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🌟 CUERPO PRINCIPAL SEGÚN PESTAÑA
                    when (activeTab) {
                        0 -> {
                            // ==================== TAB 0: ESCRIBIR & FORMATO ====================
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Campo Título con diseño de pergamino
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardParchment)
                                        .border(1.dp, borderColor)
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏷️",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    TextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        placeholder = {
                                            Text(
                                                "Título o dedicatoria de la carta...",
                                                fontFamily = Vt323,
                                                fontSize = 20.sp,
                                                color = textSubtle.copy(alpha = 0.7f)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(
                                            fontFamily = Vt323,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = accentBorder
                                        ),
                                        singleLine = true
                                    )
                                    if (title.isNotEmpty()) {
                                        IconButton(
                                            onClick = { title = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar título",
                                                tint = textSubtle,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Campo de Contenido Principal (Pergamino)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(cardParchment)
                                        .border(2.dp, borderColor)
                                ) {
                                    OutlinedTextField(
                                        value = contentValue,
                                        onValueChange = { updateContentWithHistory(it) },
                                        placeholder = {
                                            Text(
                                                "Mi querido/a amor...\n\nEscribe aquí tus sentimientos, recuerdos o pensamientos bonitos...",
                                                fontFamily = Vt323,
                                                fontSize = 20.sp,
                                                color = textSubtle.copy(alpha = 0.6f)
                                            )
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        textStyle = TextStyle(
                                            fontFamily = Vt323,
                                            fontSize = 20.sp,
                                            color = textColor,
                                            lineHeight = 24.sp
                                        ),
                                        shape = RectangleShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            cursorColor = accentBorder
                                        )
                                    )
                                }

                                // Barra de información / Estadísticas de palabras
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📝 $wordsCount palabras  •  $charsCount caracteres  •  ⏱️ ~$readTimeMin min",
                                        fontFamily = Vt323,
                                        fontSize = 15.sp,
                                        color = textSubtle
                                    )

                                    // Controles Undo / Redo
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ToolMiniButton(
                                            label = "↩ Deshacer",
                                            enabled = undoStack.isNotEmpty(),
                                            isDark = isDark,
                                            onClick = { undo() }
                                        )
                                        ToolMiniButton(
                                            label = "↪ Rehacer",
                                            enabled = redoStack.isNotEmpty(),
                                            isDark = isDark,
                                            onClick = { redo() }
                                        )
                                    }
                                }

                                // 🌟 BARRA DE HERRAMIENTAS DE FORMATO (RICH TEXT)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(toolbarBg)
                                        .border(1.dp, borderColor)
                                        .padding(4.dp)
                                ) {
                                    // Fila 1: Formato básico + Alineación + Desplegables
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FormatButton("B", isDark) { applyFormat("<b>", "</b>") }
                                        FormatButton("I", isDark) { applyFormat("<i>", "</i>") }
                                        FormatButton("U", isDark) { applyFormat("<u>", "</u>") }
                                        FormatButton("S", isDark) { applyFormat("<s>", "</s>") }

                                        Text("|", color = borderColor, modifier = Modifier.padding(horizontal = 2.dp))

                                        FormatButton("◀", isDark) { applyFormat("<p align=\"left\">", "</p>") }
                                        FormatButton("◀▶", isDark) { applyFormat("<p align=\"center\">", "</p>") }
                                        FormatButton("▶", isDark) { applyFormat("<p align=\"right\">", "</p>") }

                                        Text("|", color = borderColor, modifier = Modifier.padding(horizontal = 2.dp))

                                        FormatButton("❝ Cita", isDark) {
                                            applyFormat("<blockquote><i>«", "»</i></blockquote>")
                                        }
                                        FormatButton("♡ Separador", isDark) {
                                            insertTextAtCursor("\n<p align=\"center\">─── ♡ ───</p>\n")
                                        }

                                        FormatButton(
                                            text = if (showSizes) "Tamaño ▲" else "Tamaño ▼",
                                            isDark = isDark,
                                            isActive = showSizes
                                        ) {
                                            showSizes = !showSizes
                                            showColors = false
                                            showEmojis = false
                                        }

                                        FormatButton(
                                            text = if (showColors) "Color ▲" else "Color 🎨",
                                            isDark = isDark,
                                            isActive = showColors
                                        ) {
                                            showColors = !showColors
                                            showSizes = false
                                            showEmojis = false
                                        }

                                        FormatButton(
                                            text = if (showEmojis) "Stickers ▲" else "Stickers ✨",
                                            isDark = isDark,
                                            isActive = showEmojis
                                        ) {
                                            showEmojis = !showEmojis
                                            showColors = false
                                            showSizes = false
                                        }
                                    }

                                    // Sub-panel: Selector de Tamaños
                                    AnimatedVisibility(
                                        visible = showSizes,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .background(cardParchment)
                                                .border(1.dp, borderColor)
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            listOf(
                                                "2" to "Pequeño (14)",
                                                "4" to "Normal (18)",
                                                "6" to "Grande (24)",
                                                "7" to "Titular (32)"
                                            ).forEach { (sizeVal, label) ->
                                                Button(
                                                    onClick = {
                                                        applyFormat("<font size='$sizeVal'>", "</font>")
                                                        showSizes = false
                                                    },
                                                    shape = RectangleShape,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = accentBorder,
                                                        contentColor = Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(label, fontFamily = Vt323, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }

                                    // Sub-panel: Paleta de Colores
                                    AnimatedVisibility(
                                        visible = showColors,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .background(cardParchment)
                                                .border(1.dp, borderColor)
                                                .padding(6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val colorPalette = listOf(
                                                "#E91E63" to "Rosa Pasión",
                                                "#FF4081" to "Rosa Claro",
                                                "#9C27B0" to "Morado Místico",
                                                "#673AB7" to "Violeta",
                                                "#2196F3" to "Azul Cielo",
                                                "#00BCD4" to "Cian",
                                                "#4CAF50" to "Verde Esmeralda",
                                                "#8BC34A" to "Verde Lima",
                                                "#FF9800" to "Naranja",
                                                "#FF5722" to "Coral",
                                                "#FFD700" to "Dorado",
                                                "#8D6E63" to "Marrón",
                                                "#FFFFFF" to "Blanco",
                                                "#111111" to "Negro Tinta"
                                            )
                                            colorPalette.forEach { (hex, name) ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                                        .border(2.dp, if (hex == "#FFFFFF") Color.Gray else borderColor, CircleShape)
                                                        .clickable {
                                                            applyFormat("<font color='$hex'>", "</font>")
                                                            showColors = false
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    // Sub-panel: Emojis & Stickers Románticos / Pixel
                                    AnimatedVisibility(
                                        visible = showEmojis,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .background(cardParchment)
                                                .border(1.dp, borderColor)
                                                .padding(6.dp)
                                        ) {
                                            val emojiList = listOf(
                                                "❤️", "💖", "💕", "💌", "🌹", "💍", "💐", "🍫",
                                                "🐱", "🐾", "☕", "🧁", "🍓", "🧸", "🎀", "🍰",
                                                "✨", "⭐", "🌙", "🌸", "🍀", "🦋", "🕊️", "☁️",
                                                "🎮", "👾", "🏰", "🕯️", "📜", "💎", "🗝️", "🔮"
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                emojiList.forEach { emoji ->
                                                    Text(
                                                        text = emoji,
                                                        fontSize = 20.sp,
                                                        modifier = Modifier
                                                            .background(toolbarBg, RoundedCornerShape(4.dp))
                                                            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                            .clickable { insertTextAtCursor(emoji) }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tarjeta de Imagen Adjunta (si existe)
                                if (!imageUrl.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(cardParchment)
                                            .border(1.dp, borderColor)
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .border(1.dp, borderColor),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column {
                                                Text(
                                                    text = "📸 Foto o Recuerdo Adjunto",
                                                    fontFamily = Vt323,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    text = "Se mostrará dentro de la carta",
                                                    fontFamily = Vt323,
                                                    fontSize = 13.sp,
                                                    color = textSubtle
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = onPickImage,
                                                shape = RectangleShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = accentBorder),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Cambiar 🔄", fontFamily = Vt323, fontSize = 14.sp)
                                            }
                                            Button(
                                                onClick = { imageUrl = null },
                                                shape = RectangleShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Quitar ❌", fontFamily = Vt323, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ==================== TAB 1: VISTA PREVIA PERGAMINO REALISTA ====================
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) Color(0xFF1B1C26) else Color(0xFFFFF9E6))
                                        .border(2.dp, borderColor)
                                        .padding(16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Estampilla / Sello vintage en la esquina
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "📜 CARTA DIARIO",
                                                fontFamily = Vt323,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textSubtle
                                            )
                                            // Sello postal retro
                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, accentBorder)
                                                    .background(toolbarBg)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "💌 K&A",
                                                    fontFamily = Vt323,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                        }

                                        // Título con flores o adorno
                                        if (title.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "« $title »",
                                                fontFamily = Vt323,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "─── ⋆⋅☆⋅⋆ ───",
                                                fontFamily = Vt323,
                                                fontSize = 16.sp,
                                                color = textSubtle,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Contenido renderizado en HTML con tipografía VT323
                                        AndroidView(
                                            factory = { ctx ->
                                                TextView(ctx).apply {
                                                    val typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                                                    setTypeface(typeface)
                                                    textSize = 21f
                                                    setLineSpacing(4f, 1.15f)
                                                }
                                            },
                                            update = { textView ->
                                                textView.setTextColor(textColor.toArgb())
                                                val parsed = android.text.Html.fromHtml(
                                                    contentValue.text.ifBlank { "<i>(Carta vacía...)</i>" },
                                                    android.text.Html.FROM_HTML_MODE_LEGACY
                                                )
                                                textView.text = parsed
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Imagen embebida en la carta si existe
                                        if (!imageUrl.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .border(2.dp, borderColor)
                                                    .background(Color.Black.copy(alpha = 0.2f))
                                            ) {
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = "Foto adjunta",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Firma al pie de la carta
                                        val todayStr = remember {
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                        }
                                        Text(
                                            text = "Con mucho cariño • $todayStr ✨",
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = textSubtle,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ==================== TAB 2: PLANTILLAS ROMÁNTICAS ====================
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Elige una plantilla para inspirar tu carta y edítala a tu gusto:",
                                    fontFamily = Vt323,
                                    fontSize = 17.sp,
                                    color = textColor
                                )

                                TemplateCard(
                                    icon = "💖",
                                    title = "Carta de Amor & Gratitud",
                                    subtitle = "Expresa lo importante que es en tu vida y cuánto la/lo amas.",
                                    isDark = isDark,
                                    onSelect = {
                                        title = "Para mi persona favorita ❤️"
                                        val templateText = "Mi amor,\n\n" +
                                                "Quería tomarme este momento para recordarte lo mucho que significas para mí. " +
                                                "Cada día a tu lado es una aventura maravillosa llena de risas y momentos únicos.\n\n" +
                                                "<p align=\"center\">─── ♡ ───</p>\n\n" +
                                                "<blockquote><i>«En cualquier mundo o dimensión, siempre te elegiría a ti una y mil veces.»</i></blockquote>\n\n" +
                                                "Gracias por tu ternura, tu paciencia y por iluminar mis días. Te amo con todo mi corazón ✨"
                                        updateContentWithHistory(TextFieldValue(templateText, TextRange(templateText.length)))
                                        activeTab = 0
                                    }
                                )

                                TemplateCard(
                                    icon = "🌸",
                                    title = "Recuerdo Inolvidable",
                                    subtitle = "Guarda la memoria de un día especial, salida o viaje juntos.",
                                    isDark = isDark,
                                    onSelect = {
                                        title = "Un recuerdo que guardo en el corazón ✨"
                                        val templateText = "Hoy estuve pensando en ese momento tan especial...\n\n" +
                                                "<b>¿Te acuerdas de cuando estuvimos juntos y no parábamos de reír?</b> " +
                                                "Ese instante quedó grabado en mi memoria como uno de los más felices.\n\n" +
                                                "<p align=\"center\">─── ⋆⋅☆⋅⋆ ───</p>\n\n" +
                                                "Prometo que crearemos muchísimos más recuerdos como este. ¡Por más momentos a tu lado! 🥂"
                                        updateContentWithHistory(TextFieldValue(templateText, TextRange(templateText.length)))
                                        activeTab = 0
                                    }
                                )

                                TemplateCard(
                                    icon = "🌙",
                                    title = "Buenas Noches & Dulces Sueños",
                                    subtitle = "Un mensajito tierno antes de dormir.",
                                    isDark = isDark,
                                    onSelect = {
                                        title = "Que sueñes con los angelitos 🌙"
                                        val templateText = "Hola mi vida,\n\n" +
                                                "Antes de que cierres tus ojitos quería desearte la noche más linda y tranquila del mundo. " +
                                                "Descansa mucho, recarga energías y recuerda que eres lo primero y lo último en lo que pienso cada día.\n\n" +
                                                "<p align=\"center\"><font color=\"#BA68C8\">⭐ Que tengas dulces sueños mi amor ⭐</font></p>\n\n" +
                                                "Nos vemos en mis sueños ☁️💤"
                                        updateContentWithHistory(TextFieldValue(templateText, TextRange(templateText.length)))
                                        activeTab = 0
                                    }
                                )

                                TemplateCard(
                                    icon = "🐾",
                                    title = "Nota Divertida con Cuky",
                                    subtitle = "Un mensaje tierno con la energía de nuestra mascota.",
                                    isDark = isDark,
                                    onSelect = {
                                        title = "¡Mensaje especial de Cuky y mío! 🐾"
                                        val templateText = "¡Pio pio! 🐥\n\n" +
                                                "Cuky y yo nos unimos para mandarte este recordatorio urgente:\n\n" +
                                                "<b>1.</b> Eres la persona más linda del universo.\n" +
                                                "<b>2.</b> Te mereces todo el amor y apapachos del mundo.\n" +
                                                "<b>3.</b> ¡No olvides sonreír hoy!\n\n" +
                                                "<p align=\"center\">🐾 Te mandamos un abrazo gigante 🐾</p>"
                                        updateContentWithHistory(TextFieldValue(templateText, TextRange(templateText.length)))
                                        activeTab = 0
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🌟 BOTONES DE ACCIÓN (Inferior)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickImage,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFF6A1B9A)
                            ),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Text("📸 Foto", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF37474F) else Color(0xFF795548)
                            ),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Text("Cancelar ❌", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
                        }

                        Button(
                            onClick = { onSave(title, contentValue.text, imageUrl) },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(44.dp),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF1B5E20)),
                            enabled = contentValue.text.isNotBlank()
                        ) {
                            Text(
                                text = if (initialMessage == null) "Enviar Carta ✉" else "Guardar 💾",
                                fontFamily = Vt323,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabItem(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) accentColor else if (isDark) Color(0xFF252736) else Color(0xFFEFE2CB)
    val textCol = if (selected) Color.White else if (isDark) Color(0xFFB0B0C0) else Color(0xFF5D2E7A)
    val borderCol = if (selected) Color.White.copy(alpha = 0.6f) else if (isDark) Color(0xFF424254) else Color(0xFF8B4513)

    Box(
        modifier = modifier
            .height(36.dp)
            .background(bg)
            .border(1.5.dp, borderCol)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Vt323,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textCol,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FormatButton(
    text: String,
    isDark: Boolean,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (isActive) {
        if (isDark) Color(0xFFAB47BC) else Color(0xFF8E24AA)
    } else {
        if (isDark) Color(0xFF37394E) else Color(0xFFDFCEB2)
    }
    val contentCol = if (isActive) Color.White else if (isDark) Color.White else Color(0xFF3D2314)
    val borderCol = if (isActive) Color.White else if (isDark) Color(0xFF5C5E77) else Color(0xFF8B4513)

    Box(
        modifier = Modifier
            .height(30.dp)
            .background(bg)
            .border(1.dp, borderCol)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Vt323,
            fontSize = 16.sp,
            fontWeight = if (text == "B" || isActive) FontWeight.Bold else FontWeight.Normal,
            color = contentCol
        )
    }
}

@Composable
fun ToolMiniButton(
    label: String,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bg = if (!enabled) {
        if (isDark) Color(0xFF1E1F29) else Color(0xFFECE5D8)
    } else {
        if (isDark) Color(0xFF2E3144) else Color(0xFFE5D5BC)
    }
    val textCol = if (!enabled) {
        if (isDark) Color(0xFF555566) else Color(0xFFAAAAAA)
    } else {
        if (isDark) Color(0xFFDDDDEE) else Color(0xFF3D2314)
    }

    Box(
        modifier = Modifier
            .height(24.dp)
            .background(bg)
            .border(1.dp, if (enabled) if (isDark) Color(0xFF555570) else Color(0xFF8B4513) else Color.Transparent)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Vt323,
            fontSize = 13.sp,
            color = textCol
        )
    }
}

@Composable
fun TemplateCard(
    icon: String,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onSelect: () -> Unit
) {
    val bg = if (isDark) Color(0xFF1F212E) else Color(0xFFFFFBF0)
    val borderCol = if (isDark) Color(0xFF4A4B60) else Color(0xFF8B4513)
    val textMain = if (isDark) Color(0xFFF3F4F6) else Color(0xFF3D2314)
    val textSub = if (isDark) Color(0xFFA0A0B0) else Color(0xFF7C5C4C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .border(1.5.dp, borderCol)
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Vt323,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textMain
            )
            Text(
                text = subtitle,
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = textSub,
                lineHeight = 16.sp
            )
        }
        Button(
            onClick = onSelect,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF6A1B9A) else Color(0xFF8E24AA)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("Usar ✍️", fontFamily = Vt323, fontSize = 14.sp, color = Color.White)
        }
    }
}
