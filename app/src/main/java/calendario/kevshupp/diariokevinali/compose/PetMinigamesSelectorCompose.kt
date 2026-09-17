package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮 SELECCIONAR JUEGO 🎮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Gana ❤️ y EXP en tu 1ª partida del día y juega libremente.",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Botón Flappy Pet
                Button(
                    onClick = onPlayFlappy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flappyIconRes = if (pet.isCuky()) R.drawable.ic_cuky_balloon else R.drawable.ic_thor_balloon
                        val flappyTitle = if (pet.isCuky()) "🐔 FLAPPY CUKY 🪽" else "🐱 FLAPPY THOR 🪽"
                        Image(
                            painter = painterResource(id = flappyIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(flappyTitle, fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedFlappyToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ Libre", fontFamily = Vt323, fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedFlappyToday) "¡Juega por diversión! (Premio de hoy listo)"
                                else "¡Vuela y esquiva obstáculos! ✨ Recompensa diaria",
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "🏆 Récords: Kevin: ${pet.flappyHighScoreKevin} pts | Ali: ${pet.flappyHighScoreAli} pts",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botón Retro Memory
                Button(
                    onClick = onPlayMemory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_heart_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧠 RETRO MEMORY", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedMemoryToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ Libre", fontFamily = Vt323, fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedMemoryToday) "¡Juega por diversión! (Premio de hoy listo)"
                                else "¡Encuentra ropa pixel-art! ✨ Recompensa diaria",
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Botón Retro Snake
                Button(
                    onClick = onPlaySnake,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(2.dp, borderColor),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDD0)),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_recipe_pixel),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🐍 LA SERPIENTE", fontFamily = Vt323, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                if (playedSnakeToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐ Libre", fontFamily = Vt323, fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                if (playedSnakeToday) "¡Juega por diversión! (Premio de hoy listo)"
                                else "¡Come manzanas y bate récords! ✨ Recompensa diaria",
                                fontFamily = Vt323,
                                fontSize = 13.sp,
                                color = contentColor.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "🏆 Récords: Kevin: ${pet.snakeHighScoreKevin} pts | Ali: ${pet.snakeHighScoreAli} pts",
                                fontFamily = Vt323,
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Cerrar ❌", fontFamily = Vt323, fontSize = 17.sp, color = Color.White)
                }
            }
        }
    }
}
