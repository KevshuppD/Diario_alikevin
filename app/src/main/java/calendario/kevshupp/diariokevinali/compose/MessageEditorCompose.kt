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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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

    // Categoría abierta de la barra, como las pestañas de Word: "letra", "alinear", "insertar", "stickers"
    var openToolPanel by remember { mutableStateOf<String?>(null) }
    fun togglePanel(panel: String) {
        openToolPanel = if (openToolPanel == panel) null else panel
    }

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
    val accentBorder = if (isDark) Color(0xFF7B1FA2) else Color(0xFF5D2E7A)
    val borderColor = if (isDark) Color(0xFF424254) else Color(0xFF8B4513)
    val toolbarBg = if (isDark) Color(0xFF252736) else Color(0xFFEFE2CB)

    val canSave = contentValue.text.isNotBlank()
    // La hoja donde se escribe es la misma de las cartas del feed
    val letterPaper = rememberLetterPaper(if (isDark) "Pixel Oscuro" else "Pixel Claro")
    val paperColor = letterPaper.paper
    val paperInk = letterPaper.ink
    val paperSubtle = letterPaper.subtle
    val paperRule = letterPaper.rule
    val paperMargin = letterPaper.margin
    val htmlTransformation = remember(letterPaper) {
        HtmlVisualTransformation(if (letterPaper.adjustColors) paperColor else null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // La ventana se ajusta al teclado: encabezado y herramientas nunca salen de la pantalla
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(3.dp, accentBorder),
            color = bgModal,
            shape = RectangleShape,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // 🌟 HEADER MODERNO CON BOTÓN DE GUARDAR SIEMPRE VISIBLE ARRIBA Y BOTÓN DE CERRAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(toolbarBg)
                        .border(1.dp, borderColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
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
                        Text(text = "💌", fontSize = 20.sp)
                        Text(
                            text = if (initialMessage == null) "Escribir Carta" else "Editar Carta",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 🌟 BOTÓN GUARDAR / ENVIAR EN EL HEADER (IMPOSIBLE DE PERDER)
                        Button(
                            onClick = { onSave(title, contentValue.text, imageUrl) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF1B5E20)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp),
                            enabled = canSave
                        ) {
                            Text(
                                text = if (initialMessage == null) "Enviar ✉" else "Guardar 💾",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 🌟 PESTAÑAS (Escribir | Carta | Plantillas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                // 🌟 CUERPO PRINCIPAL SEGÚN PESTAÑA
                when (activeTab) {
                    0 -> {
                        // ==================== TAB 0: ESCRIBIR ====================
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Campo Título
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(paperColor)
                                    .border(2.dp, paperInk)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏷️",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                TextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = {
                                        Text(
                                            "Título o dedicatoria...",
                                            fontFamily = Vt323,
                                            fontSize = 18.sp,
                                            color = paperSubtle.copy(alpha = 0.7f)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        fontFamily = Vt323,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = paperInk
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = paperInk
                                    ),
                                    singleLine = true
                                )
                                if (title.isNotEmpty()) {
                                    IconButton(
                                        onClick = { title = "" },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = paperSubtle,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Área de Texto Principal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(paperColor)
                                    .border(2.dp, paperInk)
                                    .drawBehind {
                                        val step = 22.sp.toPx()
                                        var y = 16.dp.toPx() + step
                                        while (y < size.height) {
                                            drawRect(color = paperRule, topLeft = Offset(0f, y), size = Size(size.width, 1.dp.toPx()))
                                            y += step
                                        }
                                        drawRect(color = paperMargin, topLeft = Offset(10.dp.toPx(), 0f), size = Size(2.dp.toPx(), size.height))
                                    }
                            ) {
                                OutlinedTextField(
                                    value = contentValue,
                                    onValueChange = { updateContentWithHistory(protectHtmlTags(contentValue, it)) },
                                    visualTransformation = htmlTransformation,
                                    placeholder = {
                                        Text(
                                            "Mi querido/a amor...\n\nEscribe aquí tus sentimientos, recuerdos o pensamientos bonitos...",
                                            fontFamily = Vt323,
                                            fontSize = 18.sp,
                                            color = paperSubtle.copy(alpha = 0.6f)
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = TextStyle(
                                        fontFamily = Vt323,
                                        fontSize = 18.sp,
                                        color = paperInk,
                                        lineHeight = 22.sp
                                    ),
                                    shape = RectangleShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = paperInk
                                    )
                                )
                            }

                            // Estadísticas rápidas y botones Undo/Redo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📝 $wordsCount pal • $charsCount car • ⏱️ ~$readTimeMin min",
                                    fontFamily = Vt323,
                                    fontSize = 14.sp,
                                    color = textSubtle
                                )

                            }

                            // Tarjeta de Imagen Adjunta (si existe)
                            if (!imageUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardParchment)
                                        .border(1.dp, borderColor)
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(1.dp, borderColor),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            text = "📸 Foto Adjunta",
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = onPickImage,
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = accentBorder),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Cambiar 🔄", fontFamily = Vt323, fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = { imageUrl = null },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Quitar ❌", fontFamily = Vt323, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ==================== TAB 1: VISTA PREVIA CARTA ====================
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
                                    .border(1.5.dp, borderColor)
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
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
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, accentBorder)
                                                .background(toolbarBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "💌 K&A",
                                                fontFamily = Vt323,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }

                                    if (title.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "« $title »",
                                            fontFamily = Vt323,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "─── ⋆⋅☆⋅⋆ ───",
                                            fontFamily = Vt323,
                                            fontSize = 15.sp,
                                            color = textSubtle,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    AndroidView(
                                        factory = { ctx ->
                                            TextView(ctx).apply {
                                                val typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                                                setTypeface(typeface)
                                                textSize = 20f
                                                setLineSpacing(4f, 1.15f)
                                            }
                                        },
                                        update = { textView ->
                                            textView.setTextColor(textColor.toArgb())
                                            val parsed = android.text.Html.fromHtml(
                                                contentValue.text.ifBlank { "<i>(Carta vacía...)</i>" },
                                                android.text.Html.FROM_HTML_MODE_LEGACY
                                            )
                                            textView.text = if (isDark) adjustLetterColors(parsed, Color(0xFF1B1C26)) else parsed
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (!imageUrl.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .border(1.5.dp, borderColor)
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

                                    Spacer(modifier = Modifier.height(14.dp))

                                    val todayStr = remember {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                    }
                                    Text(
                                        text = "Con mucho cariño • $todayStr ✨",
                                        fontFamily = Vt323,
                                        fontSize = 15.sp,
                                        color = textSubtle,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // ==================== TAB 2: PLANTILLAS ====================
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Elige una plantilla para inspirar tu carta:",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )

                            TemplateCard(
                                icon = "💖",
                                title = "Carta de Amor & Gratitud",
                                subtitle = "Expresa lo importante que es en tu vida.",
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
                                subtitle = "Guarda la memoria de un día o viaje juntos.",
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
                                subtitle = "Un mensaje tierno con nuestra mascota.",
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

                // 🌟 HERRAMIENTAS: categorías que abren su panel justo encima del teclado, sin scroll lateral
                if (activeTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(toolbarBg)
                            .border(1.dp, borderColor)
                            .padding(4.dp)
                    ) {
                        when (openToolPanel) {
                            "letra" -> Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    EditorToolButton("Negrita", isDark, Modifier.weight(1f), onClick = { applyFormat("<b>", "</b>") }) { EditorGlyph("B", fontWeight = FontWeight.Bold) }
                                    EditorToolButton("Cursiva", isDark, Modifier.weight(1f), onClick = { applyFormat("<i>", "</i>") }) { EditorGlyph("I", fontStyle = FontStyle.Italic) }
                                    EditorToolButton("Subrayar", isDark, Modifier.weight(1f), onClick = { applyFormat("<u>", "</u>") }) { EditorGlyph("U", decoration = TextDecoration.Underline) }
                                    EditorToolButton("Tachar", isDark, Modifier.weight(1f), onClick = { applyFormat("<s>", "</s>") }) { EditorGlyph("S", decoration = TextDecoration.LineThrough) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    EDITOR_FONT_SIZES.forEach { (sizeVal, label) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .background(accentBorder)
                                                .clickable {
                                                    applyFormat("<font size='$sizeVal'>", "</font>")
                                                    openToolPanel = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontFamily = Vt323, fontSize = 16.sp, color = Color.White, maxLines = 1)
                                        }
                                    }
                                }
                                EDITOR_TEXT_COLORS.chunked(7).forEach { rowColors ->
                                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                        rowColors.forEach { hex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                                    .border(2.dp, if (hex == "#FFFFFF") Color.Gray else borderColor, CircleShape)
                                                    .clickable {
                                                        applyFormat("<font color='$hex'>", "</font>")
                                                        openToolPanel = null
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            "alinear" -> Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                EditorToolButton("Izquierda", isDark, Modifier.weight(1f), onClick = { applyFormat("<p align=\"left\">", "</p>") }) { AlignGlyph(0) }
                                EditorToolButton("Centro", isDark, Modifier.weight(1f), onClick = { applyFormat("<p align=\"center\">", "</p>") }) { AlignGlyph(1) }
                                EditorToolButton("Derecha", isDark, Modifier.weight(1f), onClick = { applyFormat("<p align=\"right\">", "</p>") }) { AlignGlyph(2) }
                            }
                            "insertar" -> Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                EditorToolButton("Foto", isDark, Modifier.weight(1f), onClick = onPickImage) { EditorGlyph("📸") }
                                EditorToolButton("Cita", isDark, Modifier.weight(1f), onClick = { applyFormat("<blockquote><i>«", "»</i></blockquote>") }) { EditorGlyph("❝") }
                                EditorToolButton("Separador", isDark, Modifier.weight(1f), onClick = { insertTextAtCursor("\n<p align=\"center\">─── ♡ ───</p>\n") }) { EditorGlyph("♡") }
                            }
                            "stickers" -> Column(modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
                                EDITOR_STICKERS.chunked(8).forEach { rowEmojis ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowEmojis.forEach { emoji ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clickable { insertTextAtCursor(emoji) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = emoji, fontSize = 22.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            EditorToolButton("Letra", isDark, Modifier.weight(1f), isActive = openToolPanel == "letra", onClick = { togglePanel("letra") }) { EditorGlyph("A", fontWeight = FontWeight.Bold) }
                            EditorToolButton("Alinear", isDark, Modifier.weight(1f), isActive = openToolPanel == "alinear", onClick = { togglePanel("alinear") }) { AlignGlyph(0) }
                            EditorToolButton("Insertar", isDark, Modifier.weight(1f), isActive = openToolPanel == "insertar", onClick = { togglePanel("insertar") }) { EditorGlyph("＋") }
                            EditorToolButton("Stickers", isDark, Modifier.weight(1f), isActive = openToolPanel == "stickers", onClick = { togglePanel("stickers") }) { EditorGlyph("✨") }
                            EditorToolButton("Deshacer", isDark, Modifier.weight(1f), enabled = undoStack.isNotEmpty(), onClick = { undo() }) { EditorGlyph("↩") }
                            EditorToolButton("Rehacer", isDark, Modifier.weight(1f), enabled = redoStack.isNotEmpty(), onClick = { redo() }) { EditorGlyph("↪") }
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
            .height(32.dp)
            .background(bg)
            .border(1.5.dp, borderCol)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Vt323,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textCol,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Botón de herramienta con dibujo y nombre, para que se entienda sin adivinar. */
@Composable
private fun EditorToolButton(
    caption: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit
) {
    val bg = when {
        isActive -> if (isDark) Color(0xFFAB47BC) else Color(0xFF8E24AA)
        else -> if (isDark) Color(0xFF37394E) else Color(0xFFDFCEB2)
    }
    val contentCol = if (isActive || isDark) Color.White else Color(0xFF3D2314)
    val borderCol = if (isActive) Color.White else if (isDark) Color(0xFF5C5E77) else Color(0xFF8B4513)
    CompositionLocalProvider(LocalContentColor provides contentCol) {
        Column(
            modifier = modifier
                .height(50.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
                .background(bg)
                .border(1.dp, borderCol)
                .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) { glyph() }
            Text(text = caption, fontFamily = Vt323, fontSize = 13.sp, maxLines = 1, color = contentCol)
        }
    }
}

@Composable
private fun EditorGlyph(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    decoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        fontFamily = Vt323,
        fontSize = 22.sp,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = decoration,
        color = LocalContentColor.current
    )
}

private val EDITOR_FONT_SIZES = listOf("2" to "Pequeño", "4" to "Normal", "6" to "Grande", "7" to "Titular")
private val EDITOR_TEXT_COLORS = listOf(
    "#E91E63", "#FF4081", "#9C27B0", "#673AB7", "#2196F3", "#00BCD4", "#4CAF50",
    "#8BC34A", "#FF9800", "#FF5722", "#FFD700", "#8D6E63", "#FFFFFF", "#111111"
)
private val EDITOR_STICKERS = listOf(
    "❤️", "💖", "💕", "💌", "🌹", "💍", "💐", "🍫",
    "🐱", "🐾", "☕", "🧁", "🍓", "🧸", "🎀", "🍰",
    "✨", "⭐", "🌙", "🌸", "🍀", "🦋", "🕊️", "☁️",
    "🎮", "👾", "🏰", "🕯️", "📜", "💎", "🗝️", "🔮"
)
private val ALIGN_LINE_WIDTHS = floatArrayOf(1f, 0.6f, 0.85f, 0.5f)

/** Líneas de texto alineadas: 0 izquierda, 1 centro, 2 derecha. */
@Composable
private fun AlignGlyph(align: Int) {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(width = 24.dp, height = 18.dp)) {
        val lineH = 2.dp.toPx()
        val gap = (size.height - lineH * ALIGN_LINE_WIDTHS.size) / (ALIGN_LINE_WIDTHS.size - 1)
        for (i in ALIGN_LINE_WIDTHS.indices) {
            val w = size.width * ALIGN_LINE_WIDTHS[i]
            val x = when (align) {
                1 -> (size.width - w) / 2f
                2 -> size.width - w
                else -> 0f
            }
            drawRect(color = color, topLeft = Offset(x, i * (lineH + gap)), size = Size(w, lineH))
        }
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Vt323,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textMain
            )
            Text(
                text = subtitle,
                fontFamily = Vt323,
                fontSize = 13.sp,
                color = textSub,
                lineHeight = 15.sp
            )
        }
        Button(
            onClick = onSelect,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF6A1B9A) else Color(0xFF8E24AA)
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.height(26.dp)
        ) {
            Text("Usar ✍️", fontFamily = Vt323, fontSize = 13.sp, color = Color.White)
        }
    }
}
