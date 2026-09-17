package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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

@Composable
fun AccessoryRow(
    name: String,
    cost: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isPreviewed: Boolean,
    isDark: Boolean,
    borderColor: Color,
    onPreview: () -> Unit,
    onAction: () -> Unit
) {
    val rowBorderColor = if (isPreviewed) borderColor else borderColor.copy(alpha = 0.3f)
    val rowBg = if (isPreviewed) {
        if (isDark) Color(0xFF2C2C35) else Color(0xFFFFF9E6)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isPreviewed) 2.dp else 1.dp, rowBorderColor)
            .background(rowBg)
            .clickable { onPreview() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontFamily = Vt323, fontSize = 18.sp, color = if (isDark) Color.White else Color.Black)
                if (isPreviewed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("👁️", fontSize = 14.sp)
                }
            }
            if (!isUnlocked) {
                Text("${cost} ❤️", fontFamily = Vt323, fontSize = 14.sp, color = Color(0xFFFF4081))
            } else if (isEquipped) {
                Text("Equipado actualmente", fontFamily = Vt323, fontSize = 13.sp, color = Color(0xFF4CAF50))
            } else {
                Text("Desbloqueado", fontFamily = Vt323, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
            }
        }

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isEquipped -> Color(0xFF4CAF50)
                    isUnlocked -> borderColor
                    else -> Color(0xFF8B4513)
                }
            ),
            shape = RectangleShape,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = when {
                    isEquipped -> "EQUIPADO"
                    isUnlocked -> "EQUIPAR"
                    else -> "COMPRAR"
                },
                fontFamily = Vt323,
                fontSize = 14.sp
            )
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
