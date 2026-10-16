package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.RoulettePrize

// Rarezas del editor → "valor" del premio (peso 1/valor en la ruleta)
private val PRIZE_RARITIES = listOf(
    "Común" to 20,
    "Normal" to 45,
    "Rara" to 90,
    "Muy rara" to 180
)

private fun rarityLabel(value: Int): String =
    PRIZE_RARITIES.minByOrNull { kotlin.math.abs(it.second - value) }?.first ?: "Normal"

/**
 * Editor de los premios de la ruleta Juntos (Configuración → Avanzado).
 * Lee los premios del documento de la mascota (sin lecturas extra) y cada cambio es 1 escritura.
 */
@Composable
fun TogetherPrizesEditor(isDark: Boolean, textColor: Color, borderColor: Color) {
    val activity = LocalContext.current.findMainActivity() ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    var pet by remember { mutableStateOf(activity.viewModel.petState.value ?: Pet()) }
    DisposableEffect(lifecycleOwner) {
        val observer = Observer<Pet> { if (it != null) pet = it }
        activity.viewModel.petState.observe(lifecycleOwner, observer)
        onDispose { activity.viewModel.petState.removeObserver(observer) }
    }
    val prizes = pet.getActiveTogetherPrizes()
    val pink = Color(0xFFEC4899)
    val cardBg = if (isDark) Color(0xFF222222) else Color(0xFFF9F5EC)

    var expanded by remember { mutableStateOf(false) }
    var newEmoji by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf(PRIZE_RARITIES[1].second) }
    var newSpicy by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RoulettePrize?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RectangleShape)
            .border(2.dp, pink)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "💞 PREMIOS DE LA RULETA JUNTOS (${prizes.size})",
                fontFamily = Vt323,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = pink,
                modifier = Modifier.weight(1f)
            )
            Text(if (expanded) "▲" else "▼", fontFamily = Vt323, fontSize = 19.sp, color = pink)
        }

        if (expanded) {
            prizes.forEach { prize ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prize.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(prize.title, fontFamily = Vt323, fontSize = 17.sp, color = textColor)
                        Text(
                            text = rarityLabel(prize.value) + if (prize.spicy) " • 🌶️ picante" else "",
                            fontFamily = Vt323,
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        "🗑️",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable { toDelete = prize }
                            .padding(6.dp)
                    )
                }
            }

            // --- Agregar premio ---
            Text("➕ AGREGAR PREMIO", fontFamily = Vt323, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newEmoji,
                    onValueChange = { if (it.length <= 8) newEmoji = it },
                    label = { Text("Emoji", fontFamily = Vt323) },
                    singleLine = true,
                    shape = RectangleShape,
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = textColor),
                    modifier = Modifier.width(80.dp)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { if (it.length <= 60) newTitle = it },
                    label = { Text("Premio", fontFamily = Vt323) },
                    singleLine = true,
                    shape = RectangleShape,
                    textStyle = LocalTextStyle.current.copy(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Qué tan seguido sale:", fontFamily = Vt323, fontSize = 15.sp, color = textColor.copy(alpha = 0.8f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                PRIZE_RARITIES.forEach { (label, value) ->
                    val selected = newValue == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) pink else Color.Transparent)
                            .border(2.dp, if (selected) Color.Black else borderColor)
                            .clickable { newValue = value }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontFamily = Vt323, fontSize = 15.sp, color = if (selected) Color.White else textColor)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { newSpicy = !newSpicy },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = newSpicy, onCheckedChange = { newSpicy = it })
                Text(
                    "🌶️ Picante (en la notificación sale como \"vale secreto\")",
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    color = textColor
                )
            }

            val canAdd = newTitle.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (canAdd) pink else Color.Gray)
                    .border(2.dp, Color.Black)
                    .clickable(enabled = canAdd) {
                        if (activity.viewModel.addTogetherPrize(newEmoji, newTitle, newValue, newSpicy)) {
                            newEmoji = ""
                            newTitle = ""
                            newSpicy = false
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("AGREGAR A LA RULETA 💞", fontFamily = Vt323, fontSize = 18.sp, color = Color.White)
            }
        }
    }

    toDelete?.let { prize ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            shape = RectangleShape,
            title = { Text("¿Quitar premio?", fontFamily = Vt323, fontSize = 22.sp) },
            text = {
                Text(
                    "${prize.emoji} ${prize.title}\nDejará de salir en la ruleta Juntos. Los vales ya ganados no se tocan.",
                    fontFamily = Vt323,
                    fontSize = 17.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    activity.viewModel.deleteTogetherPrize(prize.id)
                    toDelete = null
                }) { Text("QUITAR", fontFamily = Vt323, fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("CANCELAR", fontFamily = Vt323, fontSize = 18.sp) }
            }
        )
    }
}
