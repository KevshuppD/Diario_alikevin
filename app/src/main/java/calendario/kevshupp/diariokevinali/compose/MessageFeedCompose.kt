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
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.launch
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

fun setFeedContent(
    composeView: ComposeView,
    messagesState: MutableState<List<Message>>,
    petState: MutableState<Pet>,
    userId: String,
    themeState: MutableState<String>,
    editingMessageState: MutableState<Message?>,
    showEditorState: MutableState<Boolean>,
    currentSelectedImageUrlState: MutableState<String?>,
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onDeleteClick: (Message) -> Unit,
    onLikeClick: (Message) -> Unit,
    onSaveMessage: (title: String, content: String, imageUrl: String?) -> Unit,
    onUpdatePetName: (newName: String) -> Unit,
    onBuyAccessory: (accessoryId: String, cost: Int) -> Unit,
    onEquipAccessory: (accessoryId: String) -> Unit,
    onFeedPet: (foodId: String, cost: Int, happinessGain: Int) -> Unit,
    onRewardPet: (points: Int, exp: Int) -> Unit,
    onToggleSleep: () -> Unit,
    onPickImage: () -> Unit
) {
    composeView.setContent {
        val isDark = themeState.value == "Pixel Oscuro"
        
        Box(modifier = Modifier.fillMaxSize()) {
            MessageFeedScreen(
                messages = messagesState.value,
                pet = petState.value,
                currentUserId = userId,
                theme = themeState.value,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
                onDeleteClick = onDeleteClick,
                onLikeClick = onLikeClick,
                onUpdatePetName = onUpdatePetName,
                onBuyAccessory = onBuyAccessory,
                onEquipAccessory = onEquipAccessory,
                onFeedPet = onFeedPet,
                onRewardPet = onRewardPet,
                onToggleSleep = onToggleSleep
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
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onDeleteClick: (Message) -> Unit,
    onLikeClick: (Message) -> Unit,
    onUpdatePetName: (String) -> Unit,
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showPetDialog by remember { mutableStateOf(false) }

    if (showPetDialog) {
        PetMenuDialog(
            pet = pet,
            isDark = theme == "Pixel Oscuro",
            onDismiss = { showPetDialog = false },
            onUpdateName = { 
                onUpdatePetName(it)
                showPetDialog = false
            },
            onBuyAccessory = onBuyAccessory,
            onEquipAccessory = onEquipAccessory,
            onFeedPet = onFeedPet,
            onRewardPet = onRewardPet,
            onToggleSleep = onToggleSleep
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
                PetCard(pet = pet, theme = theme, onClick = { showPetDialog = true })
            }

            val visibleMessages = messages.take(visibleCount)
            items(
                items = visibleMessages,
                key = { it.messageId ?: "msg_${it.timestamp}_${it.authorId}" }
            ) { message ->
                MessageCard(
                    message = message,
                    currentUserId = currentUserId,
                    theme = theme,
                    dateFormat = dateFormat,
                    onClick = { onMessageClick(message) },
                    onLongClick = { 
                        selectedMessageForMenu = message
                        showMenu = true
                    },
                    onDelete = { onDeleteClick(message) },
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
                        onDeleteClick(selectedMessageForMenu!!)
                    }
                )
            }
        }
    }
}

@Composable
fun MessageCard(
    message: Message,
    currentUserId: String,
    theme: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit
) {
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
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
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

            val displayContent = if (isAlbum) {
                "📸 Momento: $cleanContent"
            } else {
                cleanContent
            }

            Text(
                text = displayContent,
                fontFamily = Vt323,
                fontSize = 20.sp,
                color = contentColor,
                lineHeight = 22.sp
            )

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
                    modifier = Modifier.size(32.dp),
                    enabled = message.authorId != currentUserId
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_pixel),
                        contentDescription = "Like",
                        tint = if (message.isLiked) Color(0xFFFF4081) else Color.Gray,
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
            
            if (timeLeft <= 0) {
                remainingTime = "Baja inminente... ⏳"
            } else {
                val hours = timeLeft / (1000 * 60 * 60)
                val minutes = (timeLeft % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (timeLeft % (1000 * 60)) / 1000
                remainingTime = if (hours > 0) {
                    "Próxima baja en: ${hours}h ${minutes}m"
                } else if (minutes > 0) {
                    "Próxima baja en: ${minutes}m ${seconds}s"
                } else {
                    "Próxima baja en: ${seconds}s ⏳"
                }
            }
            kotlinx.coroutines.delay(1000L)
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
                    // Imagen de la mascota animada con el accesorio ya integrado
                    val thorImageRes = when (pet.equippedAccessory) {
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
                    
                    Image(
                        painter = painterResource(id = thorImageRes), 
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = breathingScale * clickScale,
                                scaleY = breathingScale * clickScale,
                                translationY = bobbingOffset,
                                rotationZ = wiggleRotation
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
                    
                    Text(
                        text = "Estado: ${pet.status}",
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

                // Racha Diaria
                if (pet.streakDays > 0) {
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
                            Text(
                                text = "Racha: ${pet.streakDays} días",
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
fun PetMenuDialog(
    pet: Pet,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onBuyAccessory: (String, Int) -> Unit,
    onEquipAccessory: (String) -> Unit,
    onFeedPet: (String, Int, Int) -> Unit,
    onRewardPet: (Int, Int) -> Unit,
    onToggleSleep: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var newName by remember { mutableStateOf(pet.name) }
    var showGameSelector by remember { mutableStateOf(false) }
    var showMemoryGame by remember { mutableStateOf(false) }
    var showSnakeGame by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val accentColor = Color(0xFFFF4081)

    // Infinite transitions for Dialog mascot animation
    val dialogInfiniteTransition = rememberInfiniteTransition(label = "petDialogTransition")
    val dBobbingOffset by dialogInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dBobbing"
    )
    val dBreathingScale by dialogInfiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dBreathing"
    )
    val dWiggleRotation by dialogInfiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dWiggle"
    )

    var dIsClicked by remember { mutableStateOf(false) }
    val dClickScale by animateFloatAsState(
        targetValue = if (dIsClicked) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { dIsClicked = false }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Encabezado principal estilo Retro con botón Cerrar [X] pegajoso y responsivo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👾 MASCOTA VIRTUAL: ${pet.name.uppercase()} 👾",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )

                    // Botón pixel-art de cerrado rápido [X]
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFE53935), shape = RectangleShape)
                            .border(2.dp, borderColor)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Header con 4 pestañas: INFO, ROPA, COMIDA, GUÍA
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabItem("INFO", selectedTab == 0, isDark, borderColor) { selectedTab = 0 }
                    TabItem("ROPA 👑", selectedTab == 1, isDark, borderColor) { selectedTab = 1 }
                    TabItem("COMIDA 🐟", selectedTab == 3, isDark, borderColor) { selectedTab = 3 }
                    TabItem("GUÍA 📖", selectedTab == 2, isDark, borderColor) { selectedTab = 2 }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Pestaña de Información
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(2.dp, borderColor)
                                .background(if (pet.isSleeping) Color(0xFF0F0F3D) else Color.White.copy(alpha = 0.05f))
                                .clickable { dIsClicked = true }
                                .padding(8.dp)
                        ) {
                            // Imagen de la mascota con el accesorio ya integrado
                            val dThorImageRes = when (pet.equippedAccessory) {
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

                            Image(
                                painter = painterResource(id = dThorImageRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = dBreathingScale * dClickScale,
                                        scaleY = dBreathingScale * dClickScale,
                                        translationY = dBobbingOffset,
                                        rotationZ = dWiggleRotation
                                    )
                            )

                            if (pet.isSleeping) {
                                Text(
                                    text = "Zzz...",
                                    fontFamily = Vt323,
                                    fontSize = 24.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Nombre de tu compañero:", fontFamily = Vt323, color = contentColor, fontSize = 18.sp)
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 20.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor,
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            InfoStat("Nivel", pet.level.toString(), contentColor)
                            InfoStat("Racha", "${pet.streakDays}d", accentColor)
                            InfoStat("Amor", pet.lovePoints.toString(), Color(0xFFFF4081))
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            InfoStat("Felicidad", "${pet.happiness}%", Color(0xFF4CAF50))
                            InfoStat("EXP", "${pet.experience}/100", Color(0xFF2196F3))
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            InfoStat("Hambre 🍖", "${pet.hunger}%", Color(0xFFFF9800))
                            InfoStat("Sueño 💤", if (pet.isSleeping) "Dormido" else "Despierto", Color(0xFF9C27B0))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val timeUntilDecayDialog = rememberTimeUntilDecay(pet.lastInteraction, pet.happiness)
                        Text(
                            text = timeUntilDecayDialog,
                            fontFamily = Vt323,
                            fontSize = 15.sp,
                            color = if (pet.happiness <= 0) Color(0xFFFF4081) else contentColor.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onUpdateName(newName) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                                shape = RectangleShape
                            ) {
                                Text("Guardar Nombre", fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                            }
                            
                            Button(
                                onClick = { 
                                    if (pet.isSleeping) {
                                        android.widget.Toast.makeText(context, "¡Thor está durmiendo profundamente! 💤 Despiértalo primero para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                    } else if (pet.status == Pet.STATUS_HUNGRY) {
                                        android.widget.Toast.makeText(context, "¡Thor tiene demasiada hambre! 🍖 Aliméntalo en la pestaña COMIDA para jugar.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        showGameSelector = true 
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pet.isSleeping || pet.status == Pet.STATUS_HUNGRY) Color.Gray else Color(0xFF4CAF50)
                                ),
                                shape = RectangleShape
                            ) {
                                Text("🎮 JUGAR", fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sleepButtonColor = if (pet.isSleeping) Color(0xFFFFA000) else Color(0xFF673AB7)
                        val sleepButtonText = if (pet.isSleeping) "☀️ Despertar" else "🌙 Poner a dormir"
                        Button(
                            onClick = { onToggleSleep() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = sleepButtonColor),
                            shape = RectangleShape
                        ) {
                            Text(sleepButtonText, fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }
                    }
                } else if (selectedTab == 1) {
                    // Pestaña de Tienda / Accesorios
                    val items = listOf(
                        Triple(Pet.ACC_COLLAR, "Collar Cascabel 🔔", 10),
                        Triple(Pet.ACC_SOCKS, "Calcetas y Botitas 🧦🥾", 15),
                        Triple(Pet.ACC_MUSTACHE, "Bigote Retro 🥸", 30),
                        Triple(Pet.ACC_BALLOON, "Globo Corazón 🎈", 60),
                        Triple(Pet.ACC_BOW, "Lazo Rosa 🎀", 80),
                        Triple(Pet.ACC_HAT, "Gorrito Pixel 🎩", 100),
                        Triple(Pet.ACC_BANDANA, "Pañuelo Pirata 🏴‍☠️", 120),
                        Triple(Pet.ACC_GLASSES, "Lentes Cool 🕶️", 150),
                        Triple(Pet.ACC_BANANA, "Plátano Nano 🍌", 200),
                        Triple(Pet.ACC_CROWN, "Corona Real 👑", 500)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Puntos disponibles: ${pet.lovePoints} ❤️",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items) { (id, name, cost) ->
                                val isUnlocked = pet.unlockedAccessories.contains(id)
                                val isEquipped = pet.equippedAccessory == id

                                AccessoryRow(
                                    name = name,
                                    cost = cost,
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    onAction = {
                                        if (isUnlocked) onEquipAccessory(if (isEquipped) Pet.ACC_NONE else id)
                                        else onBuyAccessory(id, cost)
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 3) {
                    // Pestaña de Tienda de Alimentos / Comida
                    val foods = listOf(
                        Triple("cookie", "Galleta Pescado 🐟", Pair(5, 15)),
                        Triple("milk", "Leche Tibia 🥛", Pair(10, 30)),
                        Triple("catnip", "Catnip Relajante 🌿", Pair(15, 50)),
                        Triple("feast", "Banquete Gourmet 🍣", Pair(25, 80))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Puntos disponibles: ${pet.lovePoints} ❤️",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            "Alimenta a ${pet.name} para subir su felicidad e hidratación:",
                            fontFamily = Vt323,
                            color = contentColor,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(foods) { (id, name, pair) ->
                                val (cost, gain) = pair
                                FoodRow(
                                    name = name,
                                    cost = cost,
                                    gain = gain,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    onFeed = {
                                        if (pet.isSleeping) {
                                            android.widget.Toast.makeText(context, "¡Thor está durmiendo! 💤 No puede comer ahora.", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            onFeedPet(id, cost, gain)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Pestaña de Guía explicativa para la salud y puntos de amor
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "¿Cómo cuidar a ${pet.name}? 🐾",
                            fontFamily = Vt323,
                            color = accentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Card Hambre y Estado Tamagotchi
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🍖 HAMBRE Y NUTRICIÓN", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• El nivel de hambre disminuye gradualmente un 10% cada 6 horas.\n• Si el nivel de hambre cae por debajo del 30%, ¡Thor tendrá demasiada hambre y entrará en estado hambriento! 😢\n• Bloqueo de Minijuegos: No podrás jugar con él si tiene hambre.\n• Compra galletas de pescado 🐟, leche 🥛 o banquetes 🍣 en la pestaña COMIDA para alimentarlo y subir su felicidad.", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Sueño y Descanso
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("💤 SUEÑO Y DESCANSO", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Usa el botón [Poner a dormir 🌙] para apagar la luz de su cuarto y dejarlo descansar.\n• Mientras duerme, Thor soñará plácidamente y no podrá realizar actividades.\n• Restricciones: No puedes alimentarlo ni jugar minijuegos con él mientras esté dormido.\n• ¡Asegúrate de despertarlo [☀️ Despertar] cuando estés listo para interactuar con él!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Felicidad
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("😊 FELICIDAD", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Cada carta enviada le da +10% de felicidad.\n• Si no envías nada en 24h, su felicidad cae un 20% al día.\n• ¡Visita la sección COMIDA 🐟 para restaurar felicidad al instante!\n• Si su felicidad baja de 40%, se pondrá triste 😢.", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Puntos de Amor
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("❤️ PUNTOS DE AMOR", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Cartas: Ganas +5 puntos por cada mensaje enviado.\n• Racha 🔥: ¡Interconecta todos los días! Cada día de racha sumado otorga un bonus de Racha * 2 puntos.\n• Nivel ✨: Cada carta te da +10 EXP. ¡Al llegar a 100 EXP subes de nivel y recibes +50 puntos de amor!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }

                        // Card Tienda
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("👑 ACCESORIOS Y TIENDA", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Canjea tus puntos en la ROPA por accesorios.\n• Los accesorios equipados se superpondrán a tu mascota en tiempo real. ¡Haz que Thor luzca fabuloso!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                ) {
                    Text("Cerrar Menú", fontFamily = Vt323, color = contentColor, fontSize = 18.sp)
                }
            }
        }
    }

    if (showGameSelector) {
        MinigamesSelectorDialog(
            isDark = isDark,
            onDismiss = { showGameSelector = false },
            onPlayMemory = {
                showGameSelector = false
                showMemoryGame = true
            },
            onPlaySnake = {
                showGameSelector = false
                showSnakeGame = true
            }
        )
    }

    if (showMemoryGame) {
        MemoryGameDialog(
            isDark = isDark,
            onDismiss = { showMemoryGame = false },
            onReward = onRewardPet
        )
    }

    if (showSnakeGame) {
        SnakeGameDialog(
            isDark = isDark,
            onDismiss = { showSnakeGame = false },
            onReward = onRewardPet
        )
    }
}

@Composable
fun RowScope.TabItem(text: String, isSelected: Boolean, isDark: Boolean, borderColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .background(if (isSelected) borderColor else Color.Transparent)
            .clickable { onClick() }
            .border(1.dp, borderColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = if (isSelected) Color.White else if (isDark) Color.LightGray else Color.DarkGray
        )
    }
}

@Composable
fun InfoStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Vt323, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontFamily = Vt323, fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccessoryRow(
    name: String,
    cost: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isDark: Boolean,
    borderColor: Color,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
            if (!isUnlocked) {
                Text("${cost} ❤️", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
            }
        }

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isEquipped -> Color(0xFF4CAF50)
                    isUnlocked -> borderColor
                    else -> Color(0xFF8B4513)
                }
            ),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = when {
                    isEquipped -> "EQUIPADO"
                    isUnlocked -> "EQUIPAR"
                    else -> "COMPRAR"
                },
                fontFamily = Vt323,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FoodRow(
    name: String,
    cost: Int,
    gain: Int,
    isDark: Boolean,
    borderColor: Color,
    onFeed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
            Text("${cost} ❤️ (Felicidad +${gain}%)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
        }

        Button(
            onClick = onFeed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "ALIMENTAR",
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

data class MemoryCard(
    val id: Int,
    val value: Int, // Cambiado a Int para almacenar identificadores de recursos Drawable
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

@Composable
fun MemoryGameDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onReward: (points: Int, exp: Int) -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    
    // Lista de drawables pixel-art reales de la ropa/accesorios de Thor
    val images = listOf(
        R.drawable.ic_acc_balloon,
        R.drawable.ic_acc_banana,
        R.drawable.ic_acc_bandana,
        R.drawable.ic_acc_bow,
        R.drawable.ic_acc_crown,
        R.drawable.ic_acc_glasses,
        R.drawable.ic_acc_hat,
        R.drawable.ic_acc_socks
    )

    val cardsList = remember {
        val list = (images + images).mapIndexed { index, imgId ->
            MemoryCard(id = index, value = imgId)
        }
        mutableStateListOf(*list.shuffled().toTypedArray())
    }

    var selectedIndices = remember { mutableStateListOf<Int>() }
    var moves by remember { mutableStateOf(0) }
    var isWaiting by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮 RETRO MEMORY 🎮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Movimientos: $moves",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0 until 4) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 4) {
                                val cardIndex = row * 4 + col
                                val card = cardsList[cardIndex]
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .border(2.dp, borderColor)
                                        .background(
                                            if (card.isMatched || card.isFlipped || selectedIndices.contains(cardIndex)) {
                                                if (isDark) Color(0xFF2E2E2E) else Color(0xFFFFFDD0)
                                            } else {
                                                borderColor.copy(alpha = 0.8f)
                                            }
                                        )
                                        .clickable {
                                            if (isWaiting || card.isMatched || card.isFlipped || selectedIndices.contains(cardIndex) || selectedIndices.size >= 2) {
                                                return@clickable
                                            }

                                            selectedIndices.add(cardIndex)
                                            
                                            if (selectedIndices.size == 2) {
                                                moves++
                                                val firstIndex = selectedIndices[0]
                                                val secondIndex = selectedIndices[1]
                                                
                                                if (cardsList[firstIndex].value == cardsList[secondIndex].value) {
                                                    cardsList[firstIndex] = cardsList[firstIndex].copy(isMatched = true)
                                                    cardsList[secondIndex] = cardsList[secondIndex].copy(isMatched = true)
                                                    selectedIndices.clear()
                                                    
                                                    if (cardsList.all { it.isMatched }) {
                                                        showWinDialog = true
                                                    }
                                                } else {
                                                    isWaiting = true
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1000)
                                                        selectedIndices.clear()
                                                        isWaiting = false
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (card.isMatched || card.isFlipped || selectedIndices.contains(cardIndex)) {
                                        Image(
                                            painter = painterResource(id = card.value),
                                            contentDescription = null,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_heart_pixel),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            cardsList.clear()
                            val list = (images + images).mapIndexed { index, imgId ->
                                MemoryCard(id = index, value = imgId)
                            }
                            cardsList.addAll(list.shuffled())
                            selectedIndices.clear()
                            moves = 0
                            showWinDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reiniciar 🔄", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar ❌", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    if (showWinDialog) {
        Dialog(onDismissRequest = {}) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(4.dp, Color(0xFF4CAF50)),
                color = bgColor,
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "¡VICTORIA! 🎉",
                        fontFamily = Vt323,
                        fontSize = 28.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        "Has resuelto el juego de memoria de Thor en $moves movimientos.",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        "🏆 RECOMPENSA:",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "+15 Puntos de Amor ❤️\n+15 EXP ✨",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = {
                            onReward(15, 15)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RECLAMAR 🏆", fontFamily = Vt323, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MinigamesSelectorDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPlayMemory: () -> Unit,
    onPlaySnake: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮 SELECCIONAR JUEGO 🎮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Botón Retro Memory
                Button(
                    onClick = onPlayMemory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_heart_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("🧠 RETRO MEMORY", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                            Text("¡Encuentra ropa pixel-art de Thor!", fontFamily = Vt323, fontSize = 14.sp, color = contentColor.copy(alpha = 0.7f))
                        }
                    }
                }

                // Botón Retro Snake
                Button(
                    onClick = onPlaySnake,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_recipe_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("🐍 LA SERPIENTE", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                            Text("¡Come manzanas y haz crecer tu cuerpo!", fontFamily = Vt323, fontSize = 14.sp, color = contentColor.copy(alpha = 0.7f))
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar ❌", fontFamily = Vt323, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PixelArrowButton(
    arrow: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    borderColor: Color
) {
    val buttonBg = if (isDark) Color(0xFF3E3E3E) else Color(0xFFFFFDD0)
    val shadowColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD7CCC8)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationOffset = if (isPressed) 3.dp else 0.dp
    val shadowOffset = if (isPressed) 0.dp else 3.dp

    Box(
        modifier = modifier
            .size(54.dp, 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (shadowOffset > 0.dp) {
                    drawRect(
                        color = shadowColor,
                        topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                }
            }
            .offset(x = translationOffset, y = translationOffset)
            .background(buttonBg)
            .border(2.dp, borderColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = arrow,
            fontFamily = Vt323,
            fontSize = 24.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SnakeGameDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onReward: (points: Int, exp: Int) -> Unit
) {
    val gridRows = 12
    val gridCols = 12

    // Estados de Juego
    var snake by remember { mutableStateOf(listOf(6 to 6, 6 to 7)) }
    var direction by remember { mutableStateOf(0 to -1) } // Dirección inicial: ARRIBA
    var food by remember { mutableStateOf(3 to 3) }
    var score by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isGameOver by remember { mutableStateOf(false) }

    // Cargar los bitmaps pixel-art generados
    val appleBitmap = ImageBitmap.imageResource(id = R.drawable.ic_game_apple)
    val snakeHeadBitmap = ImageBitmap.imageResource(id = R.drawable.ic_game_snake_head)

    // Generar comida en un lugar vacío
    fun spawnFood() {
        var newFood: Pair<Int, Int>
        do {
            newFood = (0 until gridCols).random() to (0 until gridRows).random()
        } while (snake.contains(newFood))
        food = newFood
    }

    // Bucle del juego
    LaunchedEffect(isPlaying, isGameOver) {
        while (isPlaying && !isGameOver) {
            val speed = (220 - (score * 6)).coerceAtLeast(100)
            kotlinx.coroutines.delay(speed.toLong())
            
            val head = snake.first()
            val newHead = (head.first + direction.first) to (head.second + direction.second)
            
            // Colisión con paredes
            if (newHead.first < 0 || newHead.first >= gridCols || newHead.second < 0 || newHead.second >= gridRows) {
                isGameOver = true
                break
            }
            
            // Colisión consigo misma
            if (snake.contains(newHead)) {
                isGameOver = true
                break
            }
            
            val newSnake = mutableListOf(newHead)
            if (newHead == food) {
                score++
                newSnake.addAll(snake)
                spawnFood()
            } else {
                newSnake.addAll(snake.dropLast(1))
            }
            snake = newSnake
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
                .padding(8.dp),
            color = Color.Transparent
        ) {
            // Carcasa de la Consola GameBoy Clásica (Gris Retro)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC5C6C0), shape = RoundedCornerShape(16.dp))
                    .border(4.dp, Color(0xFF8E8F88), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Marco de la pantalla (Plástico gris oscuro)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3F403F), shape = RoundedCornerShape(8.dp))
                        .border(3.dp, Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Líneas decorativas superiores y texto de pantalla
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Luz de batería (LED rojo)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isPlaying && !isGameOver) Color(0xFFFF1744) else Color(0xFF3E1E1E), shape = CircleShape)
                                    .border(1.dp, Color.Black, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "POWER",
                                fontFamily = Vt323,
                                color = Color(0xFFC5C6C0),
                                fontSize = 9.sp
                            )
                        }
                        
                        // Líneas de color azul y rosa Gameboy
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(3.dp)
                                .background(Color(0xFF8B1E3F))
                        )

                        Text(
                            "THOR-MATRIX SCREEN",
                            fontFamily = Vt323,
                            color = Color(0xFFC5C6C0),
                            fontSize = 10.sp
                        )
                    }

                    // 2. La Pantalla LCD Verde (Con HUD integrado)
                    Column(
                        modifier = Modifier
                            .size(230.dp)
                            .background(Color(0xFF9BBC0F))
                            .border(2.dp, Color(0xFF0F380F))
                    ) {
                        // HUD Superior de Pantalla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(Color(0xFF0F380F))
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "THOR SNAKE",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = Color(0xFF9BBC0F),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SCORE:${score.toString().padStart(3, '0')}",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = Color(0xFF9BBC0F),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Lienzo de Dibujo del Juego
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGameOver) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F380F).copy(alpha = 0.9f))
                                ) {
                                    Text(
                                        text = "GAME OVER!",
                                        fontFamily = Vt323,
                                        fontSize = 24.sp,
                                        color = Color(0xFF9BBC0F),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val pts = score * 2
                                    val xp = score * 5
                                    Text(
                                        text = "+$pts LOVE  +$xp XP",
                                        fontFamily = Vt323,
                                        fontSize = 16.sp,
                                        color = Color(0xFF9BBC0F)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "PULSA [A] REINICIAR",
                                        fontFamily = Vt323,
                                        fontSize = 14.sp,
                                        color = Color(0xFF9BBC0F).copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cellW = size.width / gridCols
                                    val cellH = size.height / gridRows

                                    // Dibujar matriz de puntos sutil en el fondo
                                    val dotColor = Color(0xFF8BAC0F).copy(alpha = 0.3f)
                                    for (i in 0 until gridCols) {
                                        for (j in 0 until gridRows) {
                                            drawRect(
                                                color = dotColor,
                                                topLeft = androidx.compose.ui.geometry.Offset(i * cellW + 1.dp.toPx(), j * cellH + 1.dp.toPx()),
                                                size = androidx.compose.ui.geometry.Size(2.dp.toPx(), 2.dp.toPx())
                                            )
                                        }
                                    }

                                    // Dibujar la manzana pixel-art generada con tinte LCD monocromático
                                    drawImage(
                                        image = appleBitmap,
                                        dstOffset = androidx.compose.ui.unit.IntOffset(
                                            x = (food.first * cellW + 1.dp.toPx()).toInt(),
                                            y = (food.second * cellH + 1.dp.toPx()).toInt()
                                        ),
                                        dstSize = androidx.compose.ui.unit.IntSize(
                                            width = (cellW - 2.dp.toPx()).toInt(),
                                            height = (cellH - 2.dp.toPx()).toInt()
                                        ),
                                        colorFilter = ColorFilter.tint(Color(0xFF0F380F))
                                    )

                                    // Dibujar la serpiente con diseño procedimental retro 8-bits coherente
                                    snake.forEachIndexed { idx, body ->
                                        val isHead = idx == 0
                                        val segX = body.first * cellW
                                        val segY = body.second * cellH
                                        
                                        if (isHead) {
                                            // 1. Cabeza de Serpiente Procedimental retro
                                            drawRect(
                                                color = Color(0xFF0F380F),
                                                topLeft = androidx.compose.ui.geometry.Offset(segX + 1.dp.toPx(), segY + 1.dp.toPx()),
                                                size = androidx.compose.ui.geometry.Size(cellW - 2.dp.toPx(), cellH - 2.dp.toPx())
                                            )
                                            
                                            // Ojitos pixelados que siguen la dirección de movimiento de forma adorable
                                            val eyeSize = 3.dp.toPx()
                                            val eyeOffset = 4.dp.toPx()
                                            val eye1: androidx.compose.ui.geometry.Offset
                                            val eye2: androidx.compose.ui.geometry.Offset

                                            when (direction) {
                                                0 to -1 -> { // ARRIBA
                                                    eye1 = androidx.compose.ui.geometry.Offset(segX + eyeOffset, segY + eyeOffset)
                                                    eye2 = androidx.compose.ui.geometry.Offset(segX + cellW - eyeOffset - eyeSize, segY + eyeOffset)
                                                }
                                                0 to 1 -> { // ABAJO
                                                    eye1 = androidx.compose.ui.geometry.Offset(segX + eyeOffset, segY + cellH - eyeOffset - eyeSize)
                                                    eye2 = androidx.compose.ui.geometry.Offset(segX + cellW - eyeOffset - eyeSize, segY + cellH - eyeOffset - eyeSize)
                                                }
                                                -1 to 0 -> { // IZQUIERDA
                                                    eye1 = androidx.compose.ui.geometry.Offset(segX + eyeOffset, segY + eyeOffset)
                                                    eye2 = androidx.compose.ui.geometry.Offset(segX + eyeOffset, segY + cellH - eyeOffset - eyeSize)
                                                }
                                                else -> { // DERECHA
                                                    eye1 = androidx.compose.ui.geometry.Offset(segX + cellW - eyeOffset - eyeSize, segY + eyeOffset)
                                                    eye2 = androidx.compose.ui.geometry.Offset(segX + cellW - eyeOffset - eyeSize, segY + cellH - eyeOffset - eyeSize)
                                                }
                                            }

                                            // Los ojos son "píxeles transparentes apagados" (Color del fondo LCD)
                                            drawRect(color = Color(0xFF9BBC0F), topLeft = eye1, size = androidx.compose.ui.geometry.Size(eyeSize, eyeSize))
                                            drawRect(color = Color(0xFF9BBC0F), topLeft = eye2, size = androidx.compose.ui.geometry.Size(eyeSize, eyeSize))
                                        } else {
                                            // 2. Segmentos del Cuerpo: cuentas circulares 8-bit conectadas
                                            drawRoundRect(
                                                color = Color(0xFF306230),
                                                topLeft = androidx.compose.ui.geometry.Offset(segX + 2.dp.toPx(), segY + 2.dp.toPx()),
                                                size = androidx.compose.ui.geometry.Size(cellW - 4.dp.toPx(), cellH - 4.dp.toPx()),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = Color(0xFF0F380F),
                                                topLeft = androidx.compose.ui.geometry.Offset(segX + 4.dp.toPx(), segY + 4.dp.toPx()),
                                                size = androidx.compose.ui.geometry.Size(cellW - 8.dp.toPx(), cellH - 8.dp.toPx()),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Marca de la consola impreso en el plástico
                Text(
                    "THOR POCKET™",
                    fontFamily = Vt323,
                    color = Color(0xFF2C2D2F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Controles Físicos (D-PAD cruz contiguous a la izquierda, botones A/B a la derecha)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // D-PAD CONTIGUO DE PLÁSTICO (Diseño clásico de cruz unificada)
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cuerpo Vertical de la Cruz
                        Box(
                            modifier = Modifier
                                .size(34.dp, 100.dp)
                                .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                        )
                        // Cuerpo Horizontal de la Cruz
                        Box(
                            modifier = Modifier
                                .size(100.dp, 34.dp)
                                .background(Color(0xFF2E2D2D), shape = RoundedCornerShape(4.dp))
                                .border(1.5.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                        )
                        // Centro unificador
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF252424))
                        )

                        // Zonas Clickables sobre la Cruz
                        // Botón Arriba
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.TopCenter)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (direction != (0 to 1)) direction = 0 to -1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = Color(0xFF8E8F88), fontSize = 12.sp)
                        }

                        // Botón Abajo
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.BottomCenter)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (direction != (0 to -1)) direction = 0 to 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▼", color = Color(0xFF8E8F88), fontSize = 12.sp)
                        }

                        // Botón Izquierda
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterStart)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (direction != (1 to 0)) direction = -1 to 0
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", color = Color(0xFF8E8F88), fontSize = 12.sp)
                        }

                        // Botón Derecha
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (direction != (-1 to 0)) direction = 1 to 0
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color(0xFF8E8F88), fontSize = 12.sp)
                        }
                    }

                    // Botones de Acción Redondos (B / A)
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón B (Pausar)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF8B1E3F), shape = CircleShape)
                                    .border(2.dp, Color.Black, shape = CircleShape)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        isPlaying = !isPlaying
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", color = Color.White, fontFamily = Vt323, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPlaying) "PAUSA" else "PLAY",
                                fontFamily = Vt323,
                                fontSize = 9.sp,
                                color = Color(0xFF3F403F),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botón A (Reiniciar)
                        Column(
                            modifier = Modifier.padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF8B1E3F), shape = CircleShape)
                                    .border(2.dp, Color.Black, shape = CircleShape)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (isGameOver) {
                                            snake = listOf(6 to 6, 6 to 7)
                                            direction = 0 to -1
                                            score = 0
                                            isGameOver = false
                                            isPlaying = true
                                            spawnFood()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontFamily = Vt323, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "START",
                                fontFamily = Vt323,
                                fontSize = 9.sp,
                                color = Color(0xFF3F403F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Botones de goma SELECT y START en diagonal al fondo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón SELECT (Salir del juego)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp, 10.dp)
                                .graphicsLayer(rotationZ = -25f)
                                .background(Color(0xFF6B6A68), shape = RoundedCornerShape(3.dp))
                                .border(1.dp, Color.Black, shape = RoundedCornerShape(3.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onDismiss()
                                }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SALIR",
                            fontFamily = Vt323,
                            fontSize = 10.sp,
                            color = Color(0xFF3F403F),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botón START (Guardar Recompensa)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp, 10.dp)
                                .graphicsLayer(rotationZ = -25f)
                                .background(Color(0xFF6B6A68), shape = RoundedCornerShape(3.dp))
                                .border(1.dp, Color.Black, shape = RoundedCornerShape(3.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val rewardedPts = score * 2
                                    val rewardedXp = score * 5
                                    if (score > 0) {
                                        onReward(rewardedPts, rewardedXp)
                                    }
                                    onDismiss()
                                }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GUARDAR",
                            fontFamily = Vt323,
                            fontSize = 10.sp,
                            color = Color(0xFF3F403F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
