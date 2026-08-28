package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calendario.kevshupp.diariokevinali.Message
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CartasScreen(
    messages: List<Message>,
    isDark: Boolean,
    onSendMessage: (String) -> Unit,
    onExpandEditor: () -> Unit,
    onMessageClick: (Message) -> Unit
) {
    val backgroundColor = if (isDark) Color(0xFF1E392A) else Color(0xFF2E6132) // Fondo verde oscuro del feed
    val cardBg = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5E6BE)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4A2511)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Barra de entrada (Input Container)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .background(if (isDark) Color(0xFF132A1D) else Color(0xFFFFFFFF), RectangleShape)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "✎", fontSize = 20.sp, modifier = Modifier.clickable { onExpandEditor() }.padding(end = 8.dp))
            
            var textState by remember { mutableStateOf("") }
            
            Box(modifier = Modifier.weight(1f)) {
                if (textState.isEmpty()) {
                    Text("Escribe una carta...", color = Color.Gray, fontFamily = Vt323)
                }
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Text(
                text = "➤", 
                fontSize = 24.sp, 
                color = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32),
                modifier = Modifier.clickable { 
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                }.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                CartaItem(message, cardBg, textColor, borderColor, onMessageClick)
            }
        }
    }
}

@Composable
fun CartaItem(message: Message, cardBg: Color, textColor: Color, borderColor: Color, onClick: (Message) -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = message.timestamp?.let { sdf.format(Date(it)) } ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, borderColor)
            .background(cardBg)
            .clickable { onClick(message) }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "De: ${message.authorName}",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (textColor == Color.White) Color.White else Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                
                // Miniatura de perfil
                if (!message.authorImageUrl.isNullOrEmpty()) {
                    Box(modifier = Modifier.size(40.dp).border(2.dp, borderColor)) {
                        AsyncImage(
                            model = message.authorImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Text(
                text = message.content ?: "",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (!message.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = message.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(top = 10.dp)
                        .border(1.dp, borderColor),
                    contentScale = ContentScale.Fit
                )
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                Text(
                    text = dateStr,
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
