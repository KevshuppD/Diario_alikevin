package calendario.kevshupp.diariokevinali.compose

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Vt323, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontFamily = Vt323, fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CompactStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = Vt323, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(1.dp))
        Text(value, fontFamily = Vt323, fontSize = 16.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

/** Probador: cada toque viste a la mascota en la vista previa y la barra muestra qué se puede hacer con lo elegido. */
@Composable
fun ColumnScope.ShopItemPicker(
    items: List<Triple<String, String, Int>>,
    isUnlocked: (id: String, cost: Int) -> Boolean,
    isEquipped: (id: String) -> Boolean,
    previewId: String?,
    emptyHint: String,
    isDark: Boolean,
    borderColor: Color,
    accentColor: Color,
    onPreview: (String) -> Unit,
    onAction: (id: String, cost: Int) -> Unit,
    thumb: @Composable (id: String) -> Unit,
    lockText: (id: String) -> String? = { null },
    wonBy: (id: String) -> String? = { null },
    equippedActionLabel: String = "EQUIPADO"
) {
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)
    val selected = items.firstOrNull { it.first == previewId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .border(2.dp, borderColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected == null) {
            Text(emptyHint, fontFamily = Vt323, fontSize = 18.sp, color = textColor)
        } else {
            val (id, name, cost) = selected
            val unlocked = isUnlocked(id, cost)
            val equipped = isEquipped(id)
            val lock = if (unlocked) null else lockText(id)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontFamily = Vt323, fontSize = 19.sp, color = textColor)
                Text(
                    text = when {
                        equipped -> "Equipado actualmente"
                        lock != null -> lock
                        wonBy(id) != null -> "Ganado por ${wonBy(id)}"
                        unlocked -> "Desbloqueado"
                        else -> "${cost} ❤️"
                    },
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = when {
                        equipped -> Color(0xFF4CAF50)
                        unlocked || lock != null -> textColor.copy(alpha = 0.75f)
                        else -> Color(0xFFFF4081)
                    }
                )
            }
            Button(
                onClick = { onAction(id, cost) },
                enabled = lock == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> Color(0xFF4CAF50)
                        unlocked -> borderColor
                        else -> Color(0xFF8B4513)
                    },
                    disabledContainerColor = Color(0xFF55505C)
                ),
                shape = RectangleShape,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = when {
                        lock != null -> "🔒 BLOQUEADO"
                        equipped -> equippedActionLabel
                        unlocked -> "EQUIPAR"
                        else -> "COMPRAR"
                    },
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        items(items, key = { it.first }) { (id, name, cost) ->
            val unlocked = isUnlocked(id, cost)
            val equipped = isEquipped(id)
            val previewed = previewId == id
            val lock = if (unlocked) null else lockText(id)
            Column(
                modifier = Modifier
                    .border(if (previewed) 3.dp else 1.dp, if (previewed) accentColor else borderColor)
                    .clickable { onPreview(id) }
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clipToBounds()
                        .graphicsLayer { alpha = if (lock != null) 0.45f else 1f }
                ) {
                    thumb(id)
                }
                Text(
                    text = name,
                    fontFamily = Vt323,
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = when {
                        equipped -> "✓ Puesto"
                        lock != null -> "🔒 $lock"
                        unlocked -> "Tuyo"
                        else -> "${cost} ❤️"
                    },
                    fontFamily = Vt323,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        equipped -> Color(0xFF4CAF50)
                        lock != null -> textColor.copy(alpha = 0.75f)
                        unlocked -> textColor
                        else -> Color(0xFFFF4081)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FoodRow(
    name: String,
    cost: Int,
    gain: Int,
    isDark: Boolean,
    borderColor: Color,
    onFeed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
            Text("${cost} ❤️ (Felicidad +${gain}%)", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
        }

        Button(
            onClick = onFeed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "ALIMENTAR",
                fontFamily = Vt323,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun PixelArrowButton(
    arrow: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    borderColor: Color
) {
    val buttonBg = if (isDark) Color(0xFF3E3E3E) else Color(0xFFFFFDD0)
    val shadowColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD7CCC8)
    val textColor = if (isDark) Color.White else Color(0xFF4A2511)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationOffset = if (isPressed) 3.dp else 0.dp
    val shadowOffset = if (isPressed) 0.dp else 3.dp

    Box(
        modifier = modifier
            .size(54.dp, 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (shadowOffset > 0.dp) {
                    drawRect(
                        color = shadowColor,
                        topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = Size(size.width, size.height)
                    )
                }
            }
            .offset(x = translationOffset, y = translationOffset)
            .background(buttonBg)
            .border(2.dp, borderColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = arrow,
            fontFamily = Vt323,
            fontSize = 24.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
