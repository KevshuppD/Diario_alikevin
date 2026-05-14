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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.ComposeView

fun setFeedContent(
    composeView: ComposeView,
    messagesState: MutableState<List<Message>>,
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
    onPickImage: () -> Unit
) {
    composeView.setContent {
        val isDark = themeState.value == "Pixel Oscuro"
        
        Box(modifier = Modifier.fillMaxSize()) {
            MessageFeedScreen(
                messages = messagesState.value,
                currentUserId = userId,
                theme = themeState.value,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
                onDeleteClick = onDeleteClick,
                onLikeClick = onLikeClick
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
    currentUserId: String,
    theme: String,
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onDeleteClick: (Message) -> Unit,
    onLikeClick: (Message) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.messageId ?: UUID.randomUUID().toString() }) { message ->
            MessageCard(
                message = message,
                currentUserId = currentUserId,
                theme = theme,
                onClick = { onMessageClick(message) },
                onLongClick = { onMessageLongClick(message) },
                onDelete = { onDeleteClick(message) },
                onLike = { onLikeClick(message) }
            )
        }
    }
}

@Composable
fun MessageCard(
    message: Message,
    currentUserId: String,
    theme: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit
) {
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
    
    val authorColor = when {
        isDark -> Color(0xFFFF4081)
        isMono -> Color.Black
        else -> Color(0xFF1A5D1A)
    }
    
    val contentColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val timeColor = when {
        isDark -> Color.LightGray
        isMono -> Color.DarkGray
        else -> Color(0xFF8B4513)
    }

    val isAlbum = message.content?.startsWith("[ALBUM]") == true
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var showMenu by remember { mutableStateOf(false) }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(3.dp, borderColor)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        // Menú contextual (Editar/Borrar)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(bgColor).border(1.dp, borderColor)
        ) {
            DropdownMenuItem(
                text = { Text("Editar", fontFamily = Vt323, fontSize = 18.sp, color = contentColor) },
                onClick = {
                    showMenu = false
                    onLongClick() // Llamamos al callback original para manejar la lógica de edición
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

        // Notches pixelados en las esquinas (replicando el LayerDrawable)
        val notchSize = 8.dp
        Box(modifier = Modifier.size(notchSize).background(borderColor).align(Alignment.TopStart))
        Box(modifier = Modifier.size(notchSize).background(borderColor).align(Alignment.TopEnd))
        Box(modifier = Modifier.size(notchSize).background(borderColor).align(Alignment.BottomStart))
        Box(modifier = Modifier.size(notchSize).background(borderColor).align(Alignment.BottomEnd))

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
                    AsyncImage(
                        model = message.authorImageUrl ?: R.drawable.ic_profile_pixel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido (Procesar HTML para eliminar etiquetas y códigos raros)
            val cleanContent = remember(message.content) {
                if (message.content != null) {
                    val htmlSpanned = android.text.Html.fromHtml(message.content, android.text.Html.FROM_HTML_MODE_COMPACT)
                    htmlSpanned.toString().trim()
                } else ""
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
                    items(message.imageUrls!!) { url ->
                        AsyncImage(
                            model = url,
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
                AsyncImage(
                    model = message.imageUrls!![0],
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
