package calendario.kevshupp.diariokevinali.compose

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                onUpdatePetName = onUpdatePetName
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
    onUpdatePetName: (String) -> Unit
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
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "pet_card") {
                PetCard(pet = pet, theme = theme, onClick = { showPetDialog = true })
            }

            items(
                items = messages,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(2.dp, borderColor)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, borderColor)
                    .padding(4.dp)
            ) {
                // Imagen de la mascota (Thor el gato)
                AsyncImage(
                    model = R.drawable.ic_thor_pixel, 
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "${pet.name} (Nivel ${pet.level})",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "Estado: ${pet.status}",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Barra de felicidad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
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
        }
    }
}

@Composable
fun PetMenuDialog(
    pet: Pet,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit
) {
    var newName by remember { mutableStateOf(pet.name) }
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = {
            Text("Cuidar a ${pet.name}", fontFamily = Vt323, color = contentColor, fontSize = 24.sp)
        },
        text = {
            Column {
                Text("Cambiar nombre:", fontFamily = Vt323, color = contentColor)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Vt323, fontSize = 18.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("¿Cómo hacerlo feliz? ❤️", fontFamily = Vt323, color = borderColor, fontSize = 18.sp)
                Text("• Envía una carta (+10%)", fontFamily = Vt323, color = contentColor)
                Text("• Sube un momento al álbum (+10%)", fontFamily = Vt323, color = contentColor)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("¿Por qué baja su felicidad? 💧", fontFamily = Vt323, color = Color.Gray, fontSize = 18.sp)
                Text("• Si pasan 24h sin actividad (-20%)", fontFamily = Vt323, color = contentColor)
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateName(newName) },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor)
            ) {
                Text("Guardar", fontFamily = Vt323, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontFamily = Vt323, color = contentColor)
            }
        },
        modifier = Modifier.border(3.dp, borderColor)
    )
}
