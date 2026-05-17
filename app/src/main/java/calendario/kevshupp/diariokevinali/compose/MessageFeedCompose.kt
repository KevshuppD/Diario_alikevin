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
import androidx.compose.ui.graphics.RectangleShape
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

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.window.Dialog

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
                onEquipAccessory = onEquipAccessory
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
    onEquipAccessory: (String) -> Unit
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
            onEquipAccessory = onEquipAccessory
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
                        .background(Color.White.copy(alpha = 0.1f))
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
                        else -> R.drawable.ic_thor_base_trans
                    }
                    
                    Image(
                        painter = painterResource(id = thorImageRes), 
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
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
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Barra de felicidad
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("😊", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Barra de Experiencia
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
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
    onEquipAccessory: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var newName by remember { mutableStateOf(pet.name) }
    
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header con 3 pestañas: INFO, TIENDA, GUÍA
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabItem("INFO", selectedTab == 0, isDark, borderColor) { selectedTab = 0 }
                    TabItem("TIENDA", selectedTab == 1, isDark, borderColor) { selectedTab = 1 }
                    TabItem("GUÍA 📖", selectedTab == 2, isDark, borderColor) { selectedTab = 2 }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Pestaña de Información
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(2.dp, borderColor)
                                .background(Color.White.copy(alpha = 0.05f))
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
                                else -> R.drawable.ic_thor_base_trans
                            }

                            Image(
                                painter = painterResource(id = dThorImageRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
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
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoStat("Nivel", pet.level.toString(), contentColor)
                            InfoStat("Racha", "${pet.streakDays}d", accentColor)
                            InfoStat("Amor", pet.lovePoints.toString(), Color(0xFFFF4081))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = { onUpdateName(newName) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                            shape = RectangleShape
                        ) {
                            Text("Guardar Cambios", fontFamily = Vt323, color = Color.White, fontSize = 18.sp)
                        }
                    }
                } else if (selectedTab == 1) {
                    // Pestaña de Tienda / Accesorios
                    val items = listOf(
                        Triple(Pet.ACC_COLLAR, "Collar Cascabel 🔔", 10),
                        Triple(Pet.ACC_MUSTACHE, "Bigote Retro 🥸", 30),
                        Triple(Pet.ACC_BALLOON, "Globo Corazón 🎈", 60),
                        Triple(Pet.ACC_BOW, "Lazo Rosa 🎀", 80),
                        Triple(Pet.ACC_HAT, "Gorrito Pixel 🎩", 100),
                        Triple(Pet.ACC_BANDANA, "Pañuelo Pirata 🏴‍☠️", 120),
                        Triple(Pet.ACC_GLASSES, "Lentes Cool 🕶️", 150),
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

                        // Card Felicidad
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("😊 FELICIDAD", fontFamily = Vt323, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("• Cada carta enviada le da +10% de felicidad.\n• Si no envías nada en 24h, su felicidad cae un 20% al día.\n• Si su felicidad baja de 40%, se pondrá triste 😢.", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
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
                                Text("• Canjea tus puntos en la TIENDA por accesorios.\n• Los accesorios equipados se superpondrán a tu mascota en tiempo real. ¡Haz que Thor luzca fabuloso!", fontFamily = Vt323, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                ) {
                    Text("Cerrar", fontFamily = Vt323, color = contentColor, fontSize = 18.sp)
                }
            }
        }
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
