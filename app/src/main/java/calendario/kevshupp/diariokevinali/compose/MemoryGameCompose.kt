package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import calendario.kevshupp.diariokevinali.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class MemoryCard(
    val id: Int,
    val value: Int, // Identificador de recurso Drawable
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
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
    val images = remember {
        listOf(
            R.drawable.ic_acc_balloon,
            R.drawable.ic_acc_banana,
            R.drawable.ic_acc_bandana,
            R.drawable.ic_acc_bow,
            R.drawable.ic_acc_crown,
            R.drawable.ic_acc_glasses,
            R.drawable.ic_acc_hat,
            R.drawable.ic_acc_socks
        )
    }

    val cardsList = remember {
        val list = (images + images).mapIndexed { index, imgId ->
            MemoryCard(id = index, value = imgId)
        }
        mutableStateListOf(*list.shuffled().toTypedArray())
    }

    val selectedIndices = remember { mutableStateListOf<Int>() }
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
                                                        delay(1000)
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
