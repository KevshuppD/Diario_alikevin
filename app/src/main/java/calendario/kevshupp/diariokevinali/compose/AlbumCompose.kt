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
import calendario.kevshupp.diariokevinali.Message
import calendario.kevshupp.diariokevinali.R
import coil.compose.AsyncImage

private val AlbumVt323 = FontFamily(Font(R.font.vt323))

@Composable
fun AlbumScreen(
    moments: List<Message>,
    isDark: Boolean,
    onAddMoment: () -> Unit,
    onOpenMoment: (Message) -> Unit,
    onEditMoment: (Message) -> Unit,
    onDeleteMoment: (Message) -> Unit,
    isOwner: (Message) -> Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF0D0D2B) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF333333)
    val subtitleColor = if (isDark) Color.LightGray else Color(0xFF666666)
    val borderColor = if (isDark) Color(0xFF30304A) else Color(0xFFDDDDDD)
    val buttonColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFF8B4513)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Galeria de imagenes",
            fontFamily = AlbumVt323,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = "MOMENTOS COMPARTIDOS",
            fontFamily = AlbumVt323,
            fontSize = 14.sp,
            color = subtitleColor,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(borderColor),
            color = borderColor,
            shape = RectangleShape
        ) {}

        Spacer(modifier = Modifier.height(12.dp))

        if (moments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay momentos aun",
                    fontFamily = AlbumVt323,
                    fontSize = 20.sp,
                    color = subtitleColor
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = moments,
                    key = { it.messageId ?: "moment_${it.timestamp}" }
                ) { moment ->
                    AlbumGridItem(
                        moment = moment,
                        isDark = isDark,
                        borderColor = borderColor,
                        onOpenMoment = onOpenMoment,
                        onEditMoment = onEditMoment,
                        onDeleteMoment = onDeleteMoment,
                        isOwner = isOwner
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAddMoment,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Anadir Momento",
                fontFamily = AlbumVt323,
                fontSize = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
    moment: Message,
    isDark: Boolean,
    borderColor: Color,
    onOpenMoment: (Message) -> Unit,
    onEditMoment: (Message) -> Unit,
    onDeleteMoment: (Message) -> Unit,
    isOwner: (Message) -> Boolean
) {
    val urls = moment.imageUrls ?: emptyList()
    val displayUrl = urls.firstOrNull()
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .border(1.dp, borderColor)
            .combinedClickable(
                onClick = { onOpenMoment(moment) },
                onLongClick = {
                    if (isOwner(moment)) menuExpanded = true
                }
            )
    ) {
        if (displayUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF1A1A2E) else Color(0xFFEEEEEE))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin foto",
                    fontFamily = AlbumVt323,
                    fontSize = 12.sp,
                    color = if (isDark) Color.LightGray else Color(0xFF888888)
                )
            }
        } else {
            AsyncImage(
                model = displayUrl.optimizeCloudinary(250),
                contentDescription = "Foto del album",
                modifier = Modifier
                    .fillMaxWidth()
                    .size(80.dp),
                contentScale = ContentScale.Crop
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Editar momento",
                        fontFamily = AlbumVt323,
                        fontSize = 16.sp
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
                        color = MaterialTheme.colorScheme.error
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
