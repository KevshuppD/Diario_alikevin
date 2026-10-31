package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.R

@Composable
fun MinigamesSelectorDialog(
    pet: Pet,
    isDark: Boolean,
    playedMemoryToday: Boolean,
    playedSnakeToday: Boolean,
    playedFlappyToday: Boolean,
    onDismiss: () -> Unit,
    onPlayMemory: () -> Unit,
    onPlaySnake: () -> Unit,
    onPlayFlappy: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF3E5AB)
    val contentColor = if (isDark) Color.White else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else Color(0xFF4A2511)
    val cardColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)
    val activeName = pet.getActiveName().uppercase()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(3.dp, borderColor),
            color = bgColor,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎮 SELECCIONAR JUEGO 🎮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gana ❤️ y EXP en tu 1ª partida del día y juega libremente.",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                GameCard(
                    iconRes = if (pet.isCuky()) R.drawable.ic_cuky_balloon else R.drawable.ic_thor_balloon,
                    title = if (pet.isCuky()) "🐔 FLAPPY CUKY 🪽" else "🐱 FLAPPY THOR 🪽",
                    description = "¡Vuela y esquiva obstáculos!",
                    playedToday = playedFlappyToday,
                    kevinScore = pet.flappyHighScoreKevin,
                    aliScore = pet.flappyHighScoreAli,
                    contentColor = contentColor,
                    borderColor = borderColor,
                    cardColor = cardColor,
                    onClick = onPlayFlappy
                )
                GameCard(
                    iconRes = R.drawable.ic_heart_pixel,
                    title = "🧠 $activeName MEMORY",
                    description = "¡Encuentra ropa pixel-art!",
                    playedToday = playedMemoryToday,
                    kevinScore = null,
                    aliScore = null,
                    contentColor = contentColor,
                    borderColor = borderColor,
                    cardColor = cardColor,
                    onClick = onPlayMemory
                )
                GameCard(
                    iconRes = R.drawable.ic_recipe_pixel,
                    title = "🐍 $activeName SNAKE",
                    description = "¡Come manzanas y bate récords!",
                    playedToday = playedSnakeToday,
                    kevinScore = pet.snakeHighScoreKevin,
                    aliScore = pet.snakeHighScoreAli,
                    contentColor = contentColor,
                    borderColor = borderColor,
                    cardColor = cardColor,
                    onClick = onPlaySnake
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Cerrar ❌", fontFamily = Vt323, fontSize = 17.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    iconRes: Int,
    title: String,
    description: String,
    playedToday: Boolean,
    kevinScore: Int?,
    aliScore: Int?,
    contentColor: Color,
    borderColor: Color,
    cardColor: Color,
    onClick: () -> Unit
) {
    val statusColor = if (playedToday) Color(0xFFFF9800) else Color(0xFFE91E63)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (playedToday) "⭐ Modo libre: premio de hoy listo" else "❤️ Recompensa diaria disponible",
            fontFamily = Vt323,
            fontSize = 15.sp,
            color = statusColor,
            modifier = Modifier.border(1.dp, statusColor).padding(horizontal = 5.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontFamily = Vt323, fontSize = 22.sp, color = contentColor, fontWeight = FontWeight.Bold)
                Text(description, fontFamily = Vt323, fontSize = 16.sp, color = contentColor.copy(alpha = 0.8f))
            }
        }
        if (kevinScore != null && aliScore != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DuelScore("Kevin", kevinScore, kevinScore > aliScore, contentColor, borderColor, Modifier.weight(1f))
                DuelScore("Ali", aliScore, aliScore > kevinScore, contentColor, borderColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DuelScore(name: String, score: Int, leads: Boolean, contentColor: Color, borderColor: Color, modifier: Modifier) {
    Text(
        text = "${if (leads) "👑 " else ""}$name: $score pts",
        fontFamily = Vt323,
        fontSize = 17.sp,
        color = contentColor,
        textAlign = TextAlign.Center,
        modifier = modifier.border(1.dp, borderColor).padding(vertical = 3.dp)
    )
}
