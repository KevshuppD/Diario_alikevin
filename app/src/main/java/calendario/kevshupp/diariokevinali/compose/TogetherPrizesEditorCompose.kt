package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import calendario.kevshupp.diariokevinali.Pet
import calendario.kevshupp.diariokevinali.RoulettePrize

// Rarezas del editor → "valor" del premio (peso 1/valor en la ruleta) y color de su etiqueta
private class PrizeRarity(val label: String, val value: Int, val color: Color, val stars: String)

private val PRIZE_RARITIES = listOf(
    PrizeRarity("Común", 20, Color(0xFF16A34A), "★"),
    PrizeRarity("Normal", 45, Color(0xFF2563EB), "★★"),
    PrizeRarity("Rara", 90, Color(0xFF9333EA), "★★★"),
    PrizeRarity("Muy rara", 180, Color(0xFFD97706), "★★★★")
)

private fun rarityFor(value: Int): PrizeRarity =
    PRIZE_RARITIES.minByOrNull { kotlin.math.abs(it.value - value) } ?: PRIZE_RARITIES[1]

private val EMOJI_SUGGESTIONS = listOf(
    "🎁", "💋", "🤗", "💆", "🍕", "🍫", "🎬", "🌹", "🛁", "🍷", "💃", "🌶️", "🔥", "😘", "🧸", "☕"
)

private const val PRIZE_FILTER_ALL = 0
private const val PRIZE_FILTER_NORMAL = 1
private const val PRIZE_FILTER_SPICY = 2

private const val MAX_PRIZES_UI = 30

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
    val cardBg = if (isDark) Color(0xFF222222) else Color(0xFFFFF7FB)
    val tileBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDF6)

    // Probabilidad real de cada premio (el "Otra vez" vuelve a girar, así que no cuenta)
    val chances = remember(prizes) {
        val total = prizes.sumOf { 1.0 / it.value.coerceAtLeast(1) }
        prizes.associate { it.id to if (total > 0) (1.0 / it.value.coerceAtLeast(1)) / total * 100 else 0.0 }
    }
    val spicyCount = remember(prizes) { prizes.count { it.spicy } }

    var expanded by remember { mutableStateOf(false) }
    var filter by remember { mutableIntStateOf(PRIZE_FILTER_ALL) }
    var showForm by remember { mutableStateOf(false) }
    var newEmoji by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newValue by remember { mutableIntStateOf(PRIZE_RARITIES[1].value) }
    var newSpicy by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RoulettePrize?>(null) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Sombra sólida pixel-art
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(pink.copy(alpha = 0.55f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg, RectangleShape)
                .border(2.dp, pink)
        ) {
            // --- Cabecera ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pink)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💞", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "PREMIOS RULETA JUNTOS",
                        fontFamily = Vt323,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${prizes.size}/$MAX_PRIZES_UI premios • ${prizes.size - spicyCount} normales • $spicyCount 🌶️",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(if (expanded) "▲" else "▼", fontFamily = Vt323, fontSize = 22.sp, color = Color.White)
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Se ganan girando la ruleta 💞 cuando el radar confirma que están juntos. El % es la probabilidad real de cada premio.",
                        fontFamily = Vt323,
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.75f),
                        lineHeight = 17.sp
                    )

                    // --- Filtro ---
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        PrizeFilterChip("TODOS (${prizes.size})", filter == PRIZE_FILTER_ALL, pink, textColor, borderColor, Modifier.weight(1f)) {
                            filter = PRIZE_FILTER_ALL
                        }
                        PrizeFilterChip("💗 (${prizes.size - spicyCount})", filter == PRIZE_FILTER_NORMAL, pink, textColor, borderColor, Modifier.weight(1f)) {
                            filter = PRIZE_FILTER_NORMAL
                        }
                        PrizeFilterChip("🌶️ ($spicyCount)", filter == PRIZE_FILTER_SPICY, pink, textColor, borderColor, Modifier.weight(1f)) {
                            filter = PRIZE_FILTER_SPICY
                        }
                    }

                    // --- Lista de premios ---
                    val visible = when (filter) {
                        PRIZE_FILTER_NORMAL -> prizes.filter { !it.spicy }
                        PRIZE_FILTER_SPICY -> prizes.filter { it.spicy }
                        else -> prizes
                    }
                    if (visible.isEmpty()) {
                        Text(
                            "No hay premios en este filtro.",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    visible.forEach { prize ->
                        key(prize.id) {
                            PrizeTile(
                                prize = prize,
                                chance = chances[prize.id] ?: 0.0,
                                tileBg = tileBg,
                                textColor = textColor,
                                borderColor = borderColor,
                                onDelete = { toDelete = prize }
                            )
                        }
                    }

                    // --- Agregar premio ---
                    if (!showForm) {
                        PixelActionButton(
                            text = if (prizes.size >= MAX_PRIZES_UI) "LÍMITE DE $MAX_PRIZES_UI PREMIOS" else "➕ NUEVO PREMIO",
                            enabled = prizes.size < MAX_PRIZES_UI,
                            bg = pink,
                            shadow = borderColor
                        ) { showForm = true }
                    } else {
                        NewPrizeForm(
                            emoji = newEmoji,
                            onEmojiChange = { newEmoji = it },
                            title = newTitle,
                            onTitleChange = { newTitle = it },
                            value = newValue,
                            onValueChange = { newValue = it },
                            spicy = newSpicy,
                            onSpicyChange = { newSpicy = it },
                            pink = pink,
                            tileBg = tileBg,
                            textColor = textColor,
                            borderColor = borderColor,
                            onCancel = { showForm = false },
                            onAdd = {
                                if (activity.viewModel.addTogetherPrize(newEmoji, newTitle, newValue, newSpicy)) {
                                    newEmoji = ""
                                    newTitle = ""
                                    newSpicy = false
                                    showForm = false
                                }
                            }
                        )
                    }
                }
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

@Composable
private fun PrizeFilterChip(
    text: String,
    selected: Boolean,
    pink: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) pink else Color.Transparent)
            .border(2.dp, if (selected) pink else borderColor.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = Vt323, fontSize = 15.sp, color = if (selected) Color.White else textColor, maxLines = 1)
    }
}

@Composable
private fun PrizeTile(
    prize: RoulettePrize,
    chance: Double,
    tileBg: Color,
    textColor: Color,
    borderColor: Color,
    onDelete: () -> Unit
) {
    val rarity = rarityFor(prize.value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tileBg)
            .border(2.dp, borderColor.copy(alpha = 0.35f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Franja de color de la rareza
        Box(
            Modifier
                .width(6.dp)
                .height(56.dp)
                .background(rarity.color)
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .border(2.dp, rarity.color)
                .background(rarity.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(prize.emoji, fontSize = 20.sp)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                prize.title,
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${rarity.stars} ${rarity.label.uppercase()}",
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = rarity.color,
                    fontWeight = FontWeight.Bold
                )
                if (prize.spicy) {
                    Text(" • 🌶️", fontSize = 12.sp)
                }
                Text(
                    " • ${"%.1f".format(chance)}%",
                    fontFamily = Vt323,
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
        Box(
            modifier = Modifier
                .clickable { onDelete() }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🗑️", fontSize = 18.sp)
        }
    }
}

@Composable
private fun NewPrizeForm(
    emoji: String,
    onEmojiChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    spicy: Boolean,
    onSpicyChange: (Boolean) -> Unit,
    pink: Color,
    tileBg: Color,
    textColor: Color,
    borderColor: Color,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = pink,
        unfocusedBorderColor = borderColor.copy(alpha = 0.6f),
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        cursorColor = pink,
        focusedLabelColor = pink,
        unfocusedLabelColor = textColor.copy(alpha = 0.7f)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tileBg)
            .border(2.dp, pink)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("➕ NUEVO PREMIO", fontFamily = Vt323, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = pink)

        // Vista previa de cómo se verá en la lista
        val rarity = rarityFor(value)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, rarity.color)
                .background(rarity.color.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji.ifBlank { "🎁" }, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                title.ifBlank { "Nombre del premio..." },
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = if (title.isBlank()) textColor.copy(alpha = 0.45f) else textColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (spicy) Text("🌶️", fontSize = 16.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = emoji,
                onValueChange = { if (it.length <= 8) onEmojiChange(it) },
                label = { Text("Emoji", fontFamily = Vt323) },
                singleLine = true,
                shape = RectangleShape,
                colors = fieldColors,
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = textColor),
                modifier = Modifier.width(80.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 60) onTitleChange(it) },
                label = { Text("Premio", fontFamily = Vt323) },
                singleLine = true,
                shape = RectangleShape,
                colors = fieldColors,
                textStyle = LocalTextStyle.current.copy(fontFamily = Vt323, fontSize = 18.sp, color = textColor),
                modifier = Modifier.weight(1f)
            )
        }

        // Emojis rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EMOJI_SUGGESTIONS.forEach { e ->
                val selected = emoji == e
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (selected) pink.copy(alpha = 0.25f) else Color.Transparent)
                        .border(2.dp, if (selected) pink else borderColor.copy(alpha = 0.3f))
                        .clickable { onEmojiChange(e) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(e, fontSize = 18.sp)
                }
            }
        }

        Text("Qué tan seguido sale:", fontFamily = Vt323, fontSize = 15.sp, color = textColor.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            PRIZE_RARITIES.forEach { r ->
                val selected = value == r.value
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) r.color else Color.Transparent)
                        .border(2.dp, if (selected) r.color else borderColor.copy(alpha = 0.5f))
                        .clickable { onValueChange(r.value) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(r.stars, fontFamily = Vt323, fontSize = 12.sp, color = if (selected) Color.White else r.color)
                    Text(r.label, fontFamily = Vt323, fontSize = 15.sp, color = if (selected) Color.White else textColor, maxLines = 1)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (spicy) Color(0xFFDC2626).copy(alpha = 0.12f) else Color.Transparent)
                .border(2.dp, if (spicy) Color(0xFFDC2626) else borderColor.copy(alpha = 0.3f))
                .clickable { onSpicyChange(!spicy) }
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = spicy,
                onCheckedChange = onSpicyChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDC2626))
            )
            Column {
                Text("🌶️ Picante", fontFamily = Vt323, fontSize = 17.sp, color = textColor, fontWeight = FontWeight.Bold)
                Text(
                    "En la notificación sale como \"vale secreto\"",
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                PixelActionButton(text = "CANCELAR", enabled = true, bg = Color(0xFF6B7280), shadow = borderColor, onClick = onCancel)
            }
            Box(Modifier.weight(1.6f)) {
                PixelActionButton(text = "AGREGAR 💞", enabled = title.isNotBlank(), bg = pink, shadow = borderColor, onClick = onAdd)
            }
        }
    }
}

@Composable
private fun PixelActionButton(text: String, enabled: Boolean, bg: Color, shadow: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .offset(y = 4.dp)
                .background(shadow)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(2.dp, shadow)
                .background(if (enabled) bg else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontFamily = Vt323, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
