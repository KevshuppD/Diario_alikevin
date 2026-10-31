package calendario.kevshupp.diariokevinali.compose

import android.text.Html
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.MainActivity
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import androidx.compose.foundation.BorderStroke

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign

fun setFeedContent(
    composeView: ComposeView,
    messagesState: MutableState<List<Message>>,
    petState: MutableState<Pet>,
    userId: String,
    themeState: MutableState<String>,
    editingMessageState: MutableState<Message?>,
    showEditorState: MutableState<Boolean>,
    showPetDialogState: MutableState<Boolean>,
    currentSelectedImageUrlState: MutableState<String?>,
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onDeleteClick: (Message) -> Unit,
    onLikeClick: (Message) -> Unit,
    onSaveMessage: (title: String, content: String, imageUrl: String?) -> Unit,
    onUpdatePetName: (newName: String) -> Unit,
    onSwitchPet: (newPetType: String) -> Unit = {},
    onBuyAccessory: (accessoryId: String, cost: Int) -> Unit,
    onEquipAccessory: (accessoryId: String) -> Unit,
    onBuyBackground: (backgroundId: String, cost: Int) -> Unit,
    onEquipBackground: (backgroundId: String) -> Unit,
    onFeedPet: (foodId: String, cost: Int, happinessGain: Int) -> Unit,
    onRewardPet: (points: Int, exp: Int) -> Unit,
    onToggleSleep: () -> Unit,
    onPickImage: () -> Unit,
    onBathPet: () -> Unit,
    onPlayBallPet: (points: Int, happinessGain: Int) -> Unit,
    onPlayMinigame: (gameType: String, points: Int, exp: Int, score: Int) -> Unit
) {
    composeView.setContent {
        val isDark = themeState.value == "Pixel Oscuro"
        
        Box(modifier = Modifier.fillMaxSize()) {
            MessageFeedScreen(
                messages = messagesState.value,
                pet = petState.value,
                currentUserId = userId,
                theme = themeState.value,
                showPetDialogState = showPetDialogState,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
                onDeleteClick = onDeleteClick,
                onLikeClick = onLikeClick,
                onUpdatePetName = onUpdatePetName,
                onSwitchPet = onSwitchPet,
                onBuyAccessory = onBuyAccessory,
                onEquipAccessory = onEquipAccessory,
                onBuyBackground = onBuyBackground,
                onEquipBackground = onEquipBackground,
                onFeedPet = onFeedPet,
                onRewardPet = onRewardPet,
                onToggleSleep = onToggleSleep,
                onBathPet = onBathPet,
                onPlayBallPet = onPlayBallPet,
                onPlayMinigame = onPlayMinigame
            )

            if (showEditorState.value) {
                MessageEditorDialog(
                    initialMessage = editingMessageState.value,
                    isDark = isDark,
                    currentSelectedImageUrl = currentSelectedImageUrlState.value,
                    onDismiss = { 
                        showEditorState.value = false 
                        editingMessageState.value = null
                        currentSelectedImageUrlState.value = null
                    },
                    onPickImage = onPickImage,
                    onSave = { title, content, imageUrl ->
                        onSaveMessage(title, content, imageUrl)
                        showEditorState.value = false
                        editingMessageState.value = null
                        currentSelectedImageUrlState.value = null
                    }
                )
            }
        }
    }
}

@Composable
fun MessageFeedScreen(
    messages: List<Message>,
    pet: Pet,
    currentUserId: String,
    theme: String,
    showPetDialogState: MutableState<Boolean>,
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onDeleteClick: (Message) -> Unit,
    onLikeClick: (Message) -> Unit,
    onUpdatePetName: (String) -> Unit,
    onSwitchPet: (String) -> Unit = {},
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onBuyBackground: (String, Int) -> Unit,
    onEquipBackground: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit,
    onBathPet: () -> Unit,
    onPlayBallPet: (Int, Int) -> Unit,
    onPlayMinigame: (String, Int, Int, Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    if (showPetDialogState.value) {
        PetMenuDialog(
            pet = pet,
            isDark = theme == "Pixel Oscuro",
            onDismiss = { showPetDialogState.value = false },
            onUpdateName = { 
                onUpdatePetName(it)
                showPetDialogState.value = false
            },
            onSwitchPet = onSwitchPet,
            onBuyAccessory = onBuyAccessory,
            onEquipAccessory = onEquipAccessory,
            onBuyBackground = onBuyBackground,
            onEquipBackground = onEquipBackground,
            onFeedPet = onFeedPet,
            onRewardPet = onRewardPet,
            onToggleSleep = onToggleSleep,
            onBathPet = onBathPet,
            onPlayBallPet = onPlayBallPet,
            onPlayMinigame = onPlayMinigame
        )
    }

    if (messageToDelete != null) {
        DeleteConfirmationDialog(
            theme = theme,
            onDismiss = { messageToDelete = null },
            onConfirm = {
                onDeleteClick(messageToDelete!!)
                messageToDelete = null
            }
        )
    }

    var readingMessage by remember { mutableStateOf<Message?>(null) }
    readingMessage?.let { letter ->
        LetterReaderDialog(
            message = letter,
            currentUserId = currentUserId,
            theme = theme,
            dateFormat = dateFormat,
            onDismiss = { readingMessage = null },
            onEdit = {
                readingMessage = null
                onMessageLongClick(letter)
            }
        )
    }

    var visibleCount by remember { mutableStateOf(5) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "pet_card") {
                PetCard(pet = pet, theme = theme, onClick = { showPetDialogState.value = true })
            }

            val visibleMessages = messages.take(visibleCount)
            items(
                items = visibleMessages,
                key = { it.messageId ?: "msg_${it.timestamp}_${it.authorId}" }
            ) { message ->
                MessageCard(
                    message = message,
                    isLiked = message.isLiked,
                    currentUserId = currentUserId,
                    theme = theme,
                    dateFormat = dateFormat,
                    onClick = {
                        // Las cartas se leen en su hoja; los momentos del álbum siguen con su pantalla
                        if (message.content?.startsWith("[ALBUM]") == true || message.type == "ALBUM") onMessageClick(message)
                        else readingMessage = message
                    },
                    onEdit = { onMessageLongClick(message) },
                    onDelete = { messageToDelete = message },
                    onLike = { onLikeClick(message) }
                )
            }

            if (visibleCount < messages.size) {
                item(key = "show_more_button") {
                    val isDark = theme == "Pixel Oscuro"
                    val isMono = theme == "Pixel Monocromático"
                    val btnBg = when {
                        isDark -> Color(0xFF1A1A1A)
                        isMono -> Color.White
                        else -> Color(0xFFF3E5AB)
                    }
                    val btnBorder = when {
                        isDark -> Color(0xFF91465F)
                        isMono -> Color.Black
                        else -> Color(0xFF4A2511)
                    }
                    val btnContent = when {
                        isDark -> Color.White
                        isMono -> Color.Black
                        else -> Color(0xFF4A2511)
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { visibleCount += 5 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = btnBg,
                                contentColor = btnContent
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, btnBorder),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Mostrar más cartas ✉",
                                fontFamily = Vt323,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }

    }
}

private const val LETTER_PREVIEW_LINES = 6

/** Carta a pantalla completa: siempre muestra el texto, con sus fotos debajo. */
@Composable
private fun LetterReaderDialog(
    message: Message,
    currentUserId: String,
    theme: String,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val letterPaper = rememberLetterPaper(theme)
    val paper = letterPaper.paper
    val ink = letterPaper.ink
    val context = LocalContext.current
    val html = message.content ?: ""
    val photos = remember(message.imageUrls, message.imageUrl) {
        (message.imageUrls.orEmpty() + listOfNotNull(message.imageUrl)).distinct()
    }
    val inlinePhotos = remember(html) { LetterImageGetter.inlineImageUrls(html) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(paper).border(3.dp, letterPaper.frame)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(letterPaper.frame).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "✕ Cerrar",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = letterPaper.onFrame,
                    modifier = Modifier.clickable { onDismiss() }.padding(6.dp)
                )
                Row {
                    if (photos.isNotEmpty()) {
                        Text(
                            text = "🖼 Fotos",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            color = letterPaper.onFrame,
                            modifier = Modifier
                                .clickable { context.findMainActivity()?.getAlbumManager()?.showAlbumDetail(message) }
                                .padding(6.dp)
                        )
                    }
                    if (message.authorId == currentUserId) {
                        Text(
                            text = "✏️ Editar",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            color = letterPaper.onFrame,
                            modifier = Modifier.clickable { onEdit() }.padding(6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("De: ${message.authorName}", fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = letterPaper.author)
                        Text(dateFormat.format(Date(message.timestamp)), fontFamily = Vt323, fontSize = 16.sp, color = letterPaper.subtle)
                    }
                    AsyncImage(
                        model = (message.authorImageUrl ?: R.drawable.ic_profile_pixel).toString().optimizeCloudinary(160),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).border(2.dp, letterPaper.frame),
                        contentScale = ContentScale.Crop
                    )
                }
                if (!message.title.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message.title!!, fontFamily = Vt323, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ink)
                }
                Spacer(modifier = Modifier.height(12.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val widthPx = constraints.maxWidth
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                                textSize = 22f
                                setTextColor(ink.toArgb())
                                setTextIsSelectable(true)
                                val parsed = android.text.Html.fromHtml(
                                    LetterImageGetter.isolateImages(html),
                                    android.text.Html.FROM_HTML_MODE_LEGACY,
                                    LetterImageGetter(this, widthPx),
                                    null
                                )
                                text = if (letterPaper.adjustColors) adjustLetterColors(parsed, paper) else parsed
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Fotos adjuntas que no están ya dentro del texto; al tocarlas se abren en grande
                photos.filter { it.substringBefore('#') !in inlinePhotos }.forEach { url ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = url.optimizeCloudinary(1000),
                        contentDescription = "Foto de la carta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, letterPaper.frame)
                            .clickable { context.findMainActivity()?.getAlbumManager()?.showFullScreenImage(url, message) },
                        contentScale = ContentScale.FillWidth
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun MessageCard(
    message: Message,
    isLiked: Boolean,
    currentUserId: String,
    theme: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit
) {
    android.util.Log.d("DIARIO_DEBUG", "MessageCard msgId: ${message.messageId}, parameter isLiked: $isLiked, liked: ${message.liked}")
    val isDark = remember(theme) { theme == "Pixel Oscuro" }
    val isMono = remember(theme) { theme == "Pixel Monocromático" }
    
    // La carta es una hoja: pergamino en claro, papel de noche en oscuro, blanca en monocromático
    val letterPaper = rememberLetterPaper(theme)
    val bgColor = letterPaper.paper
    val borderColor = letterPaper.frame
    val authorColor = letterPaper.author
    val contentColor = letterPaper.ink
    val timeColor = letterPaper.subtle
    val ruleColor = letterPaper.rule
    val marginColor = letterPaper.margin

    val isAlbum = remember(message.content) { message.content?.startsWith("[ALBUM]") == true }

    // El menú de mantener presionado aparece donde está el dedo
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset.Zero) }
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(message.messageId) { mutableStateOf(false) }
    var hasMoreLines by remember(message.content) { mutableStateOf(false) }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(3.dp, borderColor)
            .background(bgColor)
            .drawBehind {
                // Renglones y margen de la hoja
                val step = 24.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawRect(color = ruleColor, topLeft = Offset(0f, y), size = Size(size.width, 1.dp.toPx()))
                    y += step
                }
                drawRect(color = marginColor, topLeft = Offset(12.dp.toPx(), 0f), size = Size(2.dp.toPx(), size.height))
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressOffset = androidx.compose.ui.unit.IntOffset(down.position.x.toInt(), down.position.y.toInt())
                }
            }
            .combinedClickable(
                onClick = onClick,
                // Editar y Borrar solo en las cartas propias
                onLongClick = if (message.authorId == currentUserId) ({ showMenu = true }) else null
            )
    ) {
        Box(modifier = Modifier.offset { pressOffset }) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(if (isDark) Color(0xFF1A1A1A) else if (isMono) Color.White else Color(0xFFF3E5AB))
                    .border(1.dp, if (isDark) Color(0xFF91465F) else if (isMono) Color.Black else Color(0xFF4A2511))
            ) {
                DropdownMenuItem(
                    text = { Text("Editar", fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else if (isMono) Color.Black else Color(0xFF4A2511)) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Borrar", fontFamily = Vt323, fontSize = 18.sp, color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }


        Column(modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 12.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "De: ${message.authorName}",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = authorColor
                    )
                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = timeColor
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(2.dp, borderColor)
                        .background(bgColor)
                        .padding(3.dp)
                ) {
                    val avatarUrl = remember(message.authorImageUrl) {
                        (message.authorImageUrl ?: R.drawable.ic_profile_pixel).toString().optimizeCloudinary(120)
                    }
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RectangleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (!message.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.title!!,
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido (Procesar HTML para eliminar etiquetas y códigos raros)
            val cleanContent = remember(message.content) {
                if (message.content != null && (message.content!!.contains("<") || message.content!!.contains("&"))) {
                    // Volvemos a usar Html.fromHtml para manejar entidades correctamente (como acentos)
                    // pero solo si el contenido realmente tiene HTML o entidades.
                    val spanned = android.text.Html.fromHtml(message.content, android.text.Html.FROM_HTML_MODE_LEGACY)
                    spanned.toString().replace("\uFFFC", "").trim()
                } else message.content ?: ""
            }
            val inlineImageUrls = remember(message.content) { LetterImageGetter.inlineImageUrls(message.content) }

            if (isAlbum) {
                Text(
                    text = "📸 Momento: $cleanContent",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = contentColor,
                    lineHeight = 22.sp
                )
            } else {
                // Ancho del texto medido por Compose: así las fotos se dimensionan sin esperar a que el TextView
                // se mida (antes se usaba post {}, y al volver a la carta tras scrollear el alto quedaba en 1 línea)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val textWidthPx = constraints.maxWidth
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            val typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                            setTypeface(typeface)
                            textSize = 20f
                            // Sin ellipsize, lineCount da el total aunque maxLines recorte la carta
                            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                                hasMoreLines = (v as TextView).lineCount > LETTER_PREVIEW_LINES
                            }
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(contentColor.toArgb())
                        textView.maxLines = if (expanded) Int.MAX_VALUE else LETTER_PREVIEW_LINES
                        val html = message.content ?: ""
                        // El tag evita volver a parsear (y a pedir las fotos) en cada recomposición
                        val parseKey = html to letterPaper
                        if (textView.tag != parseKey) {
                            textView.tag = parseKey
                            val parsed = android.text.Html.fromHtml(
                                LetterImageGetter.isolateImages(html),
                                android.text.Html.FROM_HTML_MODE_LEGACY,
                                LetterImageGetter(textView, textWidthPx),
                                null
                            )
                            textView.text = if (letterPaper.adjustColors) adjustLetterColors(parsed, bgColor) else parsed
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                }
            }

            // Imagen o Galería
            if (isAlbum && !message.imageUrls.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = message.imageUrls ?: emptyList(),
                        key = { it }
                    ) { url ->
                        val optimizedUrl = remember(url) { url.optimizeCloudinary(400) }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(optimizedUrl)
                                .size(400)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.5f)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else if (!message.imageUrls.isNullOrEmpty() && message.imageUrls!!.isNotEmpty() &&
                message.imageUrls!![0].substringBefore('#') !in inlineImageUrls) {
                // Las fotos insertadas dentro del texto (editor web) ya se muestran en su lugar
                Spacer(modifier = Modifier.height(12.dp))
                val firstUrl = message.imageUrls!![0]
                val optimizedUrl = remember(firstUrl) { firstUrl.optimizeCloudinary(800) }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(optimizedUrl)
                        .size(800)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, borderColor.copy(alpha = 0.5f)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Like y Fecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // El autor no puede darse like a su propia carta; solo ve si su pareja le dio like
                IconButton(
                    onClick = onLike,
                    enabled = message.authorId != currentUserId,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_pixel),
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFD6246E) else Color(0xFFA58B66),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasMoreLines) {
                        Text(
                            text = if (expanded) "Cerrar carta ▲" else "Leer carta ▼",
                            fontFamily = Vt323,
                            fontSize = 17.sp,
                            color = contentColor,
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .clickable { expanded = !expanded }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    // Solo en cartas propias; ya no comparte esquina con el avatar
                    if (message.authorId == currentUserId) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.padding(start = 6.dp).size(32.dp)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_delete_pixel),
                                contentDescription = "Borrar",
                                tint = borderColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** false mientras el feed está oculto detrás de otra pantalla (lo actualiza MainActivity). */
val feedAnimationsActive = mutableStateOf(true)

@Composable
fun rememberTimeUntilDecay(lastInteraction: Long, happiness: Int): String {
    if (happiness <= 0) return "¡Dale amor! ❤️"
    
    var remainingTime by remember(lastInteraction) { mutableStateOf("") }
    
    LaunchedEffect(lastInteraction) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = now - lastInteraction
            val period = 24 * 60 * 60 * 1000L
            val nextDecayTime = lastInteraction + ((diff / period) + 1) * period
            val timeLeft = nextDecayTime - now
            var nextDelay = 1000L
            
            if (timeLeft <= 0) {
                remainingTime = "Baja inminente... ⏳"
            } else {
                val hours = timeLeft / (1000 * 60 * 60)
                val minutes = (timeLeft % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (timeLeft % (1000 * 60)) / 1000
                if (hours > 0) {
                    remainingTime = "Próxima baja en: ${hours}h ${minutes}m"
                    nextDelay = 5000L // Actualizar cada 5s cuando aún quedan horas
                } else if (minutes > 0) {
                    remainingTime = "Próxima baja en: ${minutes}m ${seconds}s"
                    nextDelay = 1000L
                } else {
                    remainingTime = "Próxima baja en: ${seconds}s ⏳"
                    nextDelay = 1000L
                }
            }
            kotlinx.coroutines.delay(nextDelay)
        }
    }
    return remainingTime
}

@Composable
fun PetCard(pet: Pet, theme: String, onClick: () -> Unit) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val bgColor = when {
        isDark -> Color(0xFF1A1A1A)
        isMono -> Color.White
        else -> Color(0xFFF3E5AB)
    }
    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    // Animaciones de bamboleo y respiración: se pausan mientras el feed está oculto
    // (Configuración y otras pantallas lo tapan con GONE pero sigue compuesto)
    val animationsActive = feedAnimationsActive.value
    val bobbing = remember { Animatable(0f) }
    val breathing = remember { Animatable(1f) }
    val wiggle = remember { Animatable(-3f) }
    LaunchedEffect(animationsActive) {
        if (!animationsActive) return@LaunchedEffect
        launch { bobbing.animateTo(-6f, infiniteRepeatable(tween(1400, easing = EaseInOutQuad), RepeatMode.Reverse)) }
        launch { breathing.animateTo(1.04f, infiniteRepeatable(tween(1000, easing = EaseInOutQuad), RepeatMode.Reverse)) }
        launch { wiggle.animateTo(3f, infiniteRepeatable(tween(850, easing = EaseInOutQuad), RepeatMode.Reverse)) }
    }
    val bobbingOffset by bobbing.asState()
    val breathingScale by breathing.asState()
    val wiggleRotation by wiggle.asState()

    var isClicked by remember { mutableStateOf(false) }
    val clickScale by animateFloatAsState(
        targetValue = if (isClicked) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { isClicked = false }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(2.dp, borderColor)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        val isSleeping = pet.getActiveIsSleeping()
        val activeName = pet.getActiveName()
        val activeLevel = pet.getActiveLevel()
        val activeStatus = pet.getActiveStatus()
        val activeSleepPercent = pet.getActiveSleepPercent()
        val activeHappiness = pet.getActiveHappiness()
        val activeExp = pet.getActiveExperience()
        val activeHunger = pet.getActiveHunger()
        val activeCleanliness = pet.getActiveCleanliness()
        val activeLastInteraction = pet.getActiveLastInteraction()
        val activeLastInteractionDate = pet.getActiveLastInteractionDate()
        val activeStreak = pet.getActiveStreak()

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, borderColor)
                        .background(if (isSleeping) Color(0xFF0F0F3D) else Color.White.copy(alpha = 0.1f))
                        .clickable { isClicked = true }
                        .padding(4.dp)
                ) {
                    if (pet.isCuky()) {
                        // Cuky procedural: respira, parpadea y muestra su suciedad y accesorio
                        CukyPixelStage(
                            pet = pet,
                            strollScene = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = clickScale
                                    scaleY = clickScale
                                }
                        )
                    } else {
                        ThorPixelStage(
                            pet = pet,
                            strollScene = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = clickScale
                                    scaleY = clickScale
                                }
                        )
                    }

                    if (isSleeping) {
                        Text(
                            text = "💤",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                        )
                    } else if (activeStatus == Pet.STATUS_HUNGRY) {
                        Text(
                            text = "🍖",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeName,
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        // Badge de Nivel
                        Surface(
                            color = borderColor,
                            shape = RectangleShape,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "LVL $activeLevel",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // Puntos de Amor
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_pixel),
                            contentDescription = null,
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " ${pet.lovePoints} Puntos",
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF4A2511)
                        )
                    }

                    val stateText = if (isSleeping) {
                        val remainingMinutes = (100 - activeSleepPercent) * 4
                        val hours = remainingMinutes / 60
                        val minutes = remainingMinutes % 60
                        if (activeSleepPercent >= 100) {
                            "Estado: $activeStatus (¡Descansado!)"
                        } else {
                            "Estado: $activeStatus (Falta ${hours}h ${minutes}m)"
                        }
                    } else {
                        "Estado: $activeStatus"
                    }
                    Text(
                        text = stateText,
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PetMiniStat("Feliz", "$activeHappiness%", activeHappiness, if (activeHappiness > 50) Color(0xFF4CAF50) else Color(0xFFF44336), isDark, borderColor, Modifier.weight(1f))
                PetMiniStat("Exp", "$activeExp/100", activeExp, Color(0xFF2196F3), isDark, borderColor, Modifier.weight(1f))
                PetMiniStat("Hambre", "$activeHunger%", activeHunger, if (activeHunger >= 70) Color(0xFFF44336) else Color(0xFFFF9800), isDark, borderColor, Modifier.weight(1f))
                PetMiniStat("Limpio", "$activeCleanliness%", activeCleanliness, if (activeCleanliness < 30) Color(0xFFF44336) else Color(0xFF0EA5E9), isDark, borderColor, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Próxima baja de felicidad y racha diaria
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeUntilDecay = rememberTimeUntilDecay(activeLastInteraction, activeHappiness)
                Text(
                    text = timeUntilDecay,
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = if (activeHappiness <= 0) Color(0xFFFF4081) else if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color.DarkGray.copy(alpha = 0.8f)
                )

                // Racha Diaria (Verificación activa de vigencia)
                val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                val yesterdayStr = remember {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                }
                val isStreakActive = activeLastInteractionDate != null &&
                        (activeLastInteractionDate == todayStr || activeLastInteractionDate == yesterdayStr)
                val effectiveStreak = if (isStreakActive) activeStreak else 0

                if (effectiveStreak > 0) {
                    val dayText = if (effectiveStreak == 1) "día" else "días"
                    Text(
                        text = "🔥 Racha: $effectiveStreak $dayText",
                        fontFamily = Vt323,
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
private fun PetMiniStat(label: String, value: String, percent: Int, color: Color, isDark: Boolean, borderColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "$label $value",
            fontFamily = Vt323,
            fontSize = 13.sp,
            maxLines = 1,
            color = if (isDark) Color.LightGray else Color.DarkGray
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .border(1.dp, borderColor)
                .background(Color.Gray.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    theme: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val bgColor = when {
        isDark -> Color(0xFF1E1E1E)
        isMono -> Color.White
        else -> Color(0xFFFFFDF5)
    }
    val borderColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }
    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿ELIMINAR CARTA?",
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "¿Estás seguro de que quieres borrar esta carta? Esta acción no se puede deshacer.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
                        ),
                        border = BorderStroke(2.dp, borderColor),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "Cancelar",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        border = BorderStroke(2.dp, borderColor),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "Eliminar",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
