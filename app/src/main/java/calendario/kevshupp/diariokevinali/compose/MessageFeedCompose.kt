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
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import androidx.compose.foundation.BorderStroke

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.compose.ui.platform.ComposeView
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
    onPlayMinigame: (gameType: String, points: Int, exp: Int) -> Unit
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
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onBuyBackground: (String, Int) -> Unit,
    onEquipBackground: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit,
    onBathPet: () -> Unit,
    onPlayBallPet: (Int, Int) -> Unit,
    onPlayMinigame: (String, Int, Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var showMenu by remember { mutableStateOf(false) }
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
                    onClick = { onMessageClick(message) },
                    onLongClick = { 
                        selectedMessageForMenu = message
                        showMenu = true
                    },
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

        // Menú contextual centralizado
        if (showMenu && selectedMessageForMenu != null) {
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
            val contentColor = when {
                isDark -> Color.White
                isMono -> Color.Black
                else -> Color(0xFF4A2511)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(bgColor).border(1.dp, borderColor)
            ) {
                DropdownMenuItem(
                    text = { Text("Editar", fontFamily = Vt323, fontSize = 18.sp, color = contentColor) },
                    onClick = {
                        showMenu = false
                        onMessageLongClick(selectedMessageForMenu!!)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Borrar", fontFamily = Vt323, fontSize = 18.sp, color = Color.Red) },
                    onClick = {
                        showMenu = false
                        messageToDelete = selectedMessageForMenu
                    }
                )
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
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit
) {
    android.util.Log.d("DIARIO_DEBUG", "MessageCard msgId: ${message.messageId}, parameter isLiked: $isLiked, liked: ${message.liked}")
    val isDark = remember(theme) { theme == "Pixel Oscuro" }
    val isMono = remember(theme) { theme == "Pixel Monocromático" }
    
    val bgColor = remember(theme) {
        when {
            isDark -> Color(0xFF1A1A1A)
            isMono -> Color.White
            else -> Color(0xFFF3E5AB)
        }
    }
    
    val borderColor = remember(theme) {
        when {
            isDark -> Color(0xFF91465F)
            isMono -> Color.Black
            else -> Color(0xFF4A2511)
        }
    }
    
    val authorColor = remember(theme) {
        when {
            isDark -> Color(0xFFFF4081)
            isMono -> Color.Black
            else -> Color(0xFF1A5D1A)
        }
    }
    
    val contentColor = remember(theme) {
        when {
            isDark -> Color.White
            isMono -> Color.Black
            else -> Color(0xFF4A2511)
        }
    }

    val timeColor = remember(theme) {
        when {
            isDark -> Color.LightGray
            isMono -> Color.DarkGray
            else -> Color(0xFF8B4513)
        }
    }

    val isAlbum = remember(message.content) { message.content?.startsWith("[ALBUM]") == true }

    var showMenu by remember { mutableStateOf(false) }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(3.dp, borderColor)
            .drawBehind {
                val sizePx = 8.dp.toPx()
                // Dibujar solo los rectángulos de las esquinas para el efecto pixel
                drawRect(color = borderColor, topLeft = Offset.Zero, size = Size(sizePx, sizePx))
                drawRect(color = borderColor, topLeft = Offset(size.width - sizePx, 0f), size = Size(sizePx, sizePx))
                drawRect(color = borderColor, topLeft = Offset(0f, size.height - sizePx), size = Size(sizePx, sizePx))
                drawRect(color = borderColor, topLeft = Offset(size.width - sizePx, size.height - sizePx), size = Size(sizePx, sizePx))
            }
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "De: ${message.authorName}",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = authorColor,
                    modifier = Modifier.weight(1f)
                )

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
                    spanned.toString().trim()
                } else message.content ?: ""
            }

            if (isAlbum) {
                Text(
                    text = "📸 Momento: $cleanContent",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = contentColor,
                    lineHeight = 22.sp
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            val typeface = ResourcesCompat.getFont(ctx, R.font.vt323)
                            setTypeface(typeface)
                            textSize = 20f
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(contentColor.toArgb())
                        textView.text = android.text.Html.fromHtml(
                            message.content ?: "",
                            android.text.Html.FROM_HTML_MODE_LEGACY
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Imagen o Galería
            if (isAlbum && !message.imageUrls.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(message.imageUrls ?: emptyList()) { url ->
                        val optimizedUrl = remember(url) { url.optimizeCloudinary(400) }
                        AsyncImage(
                            model = optimizedUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.5f)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else if (!message.imageUrls.isNullOrEmpty() && message.imageUrls!!.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val optimizedUrl = remember(message.imageUrls) { message.imageUrls!![0].optimizeCloudinary(800) }
                AsyncImage(
                    model = optimizedUrl,
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
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_pixel),
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF4081) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = timeColor
                )
            }
        }
        
        // Botón eliminar (arriba a la derecha)
        if (message.authorId == currentUserId) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(36.dp).padding(4.dp)
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

    // Infinite transition for bobbing and breathing animations
    val infiniteTransition = rememberInfiniteTransition(label = "petCardTransition")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )
    val wiggleRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    var isClicked by remember { mutableStateOf(false) }
    val clickScale by animateFloatAsState(
        targetValue = if (isClicked) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { isClicked = false }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(2.dp, borderColor)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(2.dp, borderColor)
                        .background(if (pet.isSleeping) Color(0xFF0F0F3D) else Color.White.copy(alpha = 0.1f))
                        .clickable { isClicked = true }
                        .padding(4.dp)
                ) {
                    val thorImageRes = when {
                        pet.isSleeping -> R.drawable.ic_thor_sleep
                        else -> when (pet.equippedAccessory) {
                            Pet.ACC_COLLAR -> R.drawable.ic_thor_collar
                            Pet.ACC_MUSTACHE -> R.drawable.ic_thor_mustache
                            Pet.ACC_BALLOON -> R.drawable.ic_thor_balloon
                            Pet.ACC_BOW -> R.drawable.ic_thor_bow
                            Pet.ACC_HAT -> R.drawable.ic_thor_hat
                            Pet.ACC_BANDANA -> R.drawable.ic_thor_bandana
                            Pet.ACC_GLASSES -> R.drawable.ic_thor_glasses
                            Pet.ACC_CROWN -> R.drawable.ic_thor_crown
                            Pet.ACC_BANANA -> R.drawable.ic_thor_banana
                            Pet.ACC_SOCKS -> R.drawable.ic_thor_socks
                            else -> R.drawable.ic_thor_base_trans
                        }
                    }
                    
                    val publicId = when {
                        pet.isSleeping -> "spirits/ic_thor_sleep"
                        else -> {
                            val acc = pet.equippedAccessory
                            if (acc.isNullOrBlank() || acc == "none") {
                                "spirits/ic_thor_base_trans"
                            } else {
                                "spirits/ic_thor_$acc"
                            }
                        }
                    }

                    val cloudinaryUrl = remember(publicId) {
                        try {
                            com.cloudinary.android.MediaManager.get().url().generate(publicId)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    AsyncImage(
                        model = cloudinaryUrl,
                        contentDescription = null,
                        placeholder = painterResource(id = thorImageRes),
                        error = painterResource(id = thorImageRes),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = breathingScale * clickScale,
                                scaleY = breathingScale * clickScale,
                                translationY = if (pet.isSleeping) 0f else bobbingOffset,
                                rotationZ = if (pet.isSleeping) 0f else wiggleRotation
                            )
                    )

                    if (pet.isSleeping) {
                        Text(
                            text = "💤",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                        )
                    } else if (pet.status == Pet.STATUS_HUNGRY) {
                        Text(
                            text = "🍖",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pet.name,
                            fontFamily = Vt323,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        
                        // Badge de Nivel
                        Surface(
                            color = borderColor,
                            shape = RectangleShape,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "LVL ${pet.level}",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    val stateText = if (pet.isSleeping) {
                        val remainingMinutes = (100 - pet.sleepPercent) * 4
                        val hours = remainingMinutes / 60
                        val minutes = remainingMinutes % 60
                        if (pet.sleepPercent >= 100) {
                            "Estado: ${pet.status} (¡Descansado!)"
                        } else {
                            "Estado: ${pet.status} (Falta ${hours}h ${minutes}m)"
                        }
                    } else {
                        "Estado: ${pet.status}"
                    }
                    Text(
                        text = stateText,
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Barra de felicidad
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Felicidad 😊",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Text(
                                text = "${pet.happiness}%",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        
                        val timeUntilDecay = rememberTimeUntilDecay(pet.lastInteraction, pet.happiness)
                        Text(
                            text = timeUntilDecay,
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            color = if (pet.happiness <= 0) Color(0xFFFF4081) else if (isDark) Color.LightGray.copy(alpha = 0.7f) else Color.DarkGray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .border(1.dp, borderColor)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pet.happiness / 100f)
                                    .fillMaxHeight()
                                    .background(if (pet.happiness > 50) Color(0xFF4CAF50) else Color(0xFFF44336))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de Experiencia
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Experiencia ✨",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Text(
                                text = "${pet.experience}/100 XP",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.5f))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pet.experience / 100f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF2196F3))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de Hambre
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hambre 🍖",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Text(
                                text = "${pet.hunger}%",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.5f))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pet.hunger / 100f)
                                    .fillMaxHeight()
                                    .background(if (pet.hunger >= 70) Color(0xFFF44336) else Color(0xFFFF9800))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de Limpieza
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Limpieza 🧼",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Text(
                                text = "${pet.cleanliness}%",
                                fontFamily = Vt323,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.5f))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pet.cleanliness / 100f)
                                    .fillMaxHeight()
                                    .background(if (pet.cleanliness < 30) Color(0xFFF44336) else Color(0xFF0EA5E9))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer de la tarjeta con Puntos y Racha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Puntos de Amor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_pixel),
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pet.lovePoints} Puntos",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF4A2511)
                    )
                }

                // Racha Diaria (Verificación activa de vigencia)
                val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                val yesterdayStr = remember {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                }
                val isStreakActive = pet.lastInteractionDate != null &&
                        (pet.lastInteractionDate == todayStr || pet.lastInteractionDate == yesterdayStr)
                val effectiveStreak = if (isStreakActive) pet.streakDays else 0

                if (effectiveStreak > 0) {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            val dayText = if (effectiveStreak == 1) "día" else "días"
                            Text(
                                text = "Racha: $effectiveStreak $dayText",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
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
