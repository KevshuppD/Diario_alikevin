package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage

private val AlbumVt323 = FontFamily(Font(R.font.vt323))

@Composable
fun AlbumScreen(
    moments: List<Message>,
    theme: String = "Pixel Claro",
    onAddMoment: () -> Unit,
    onOpenMoment: (Message) -> Unit,
    onEditMoment: (Message) -> Unit,
    onDeleteMoment: (Message) -> Unit,
    isOwner: (Message) -> Boolean
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"

    val backgroundColor = when {
        isDark -> Color(0xFF0D0D2B)
        isMono -> Color.White
        else -> Color(0xFFF5E6BE)
    }
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
        else -> Color(0xFFFFFDF9) // polaroid look!
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Banner header
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
                    text = "NUESTROS MOMENTOS MÁS HERMOSOS JUNTOS",
                    fontFamily = AlbumVt323,
                    fontSize = 14.sp,
                    color = subtitleColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Caption of Polaroid
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
            modifier = Modifier.background(cardBgColor).border(1.dp, borderColor)
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
