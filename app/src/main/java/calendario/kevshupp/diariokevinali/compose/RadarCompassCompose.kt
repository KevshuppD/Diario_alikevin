package calendario.kevshupp.diariokevinali.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RadarCompassView(
    bearingDegrees: Float,
    distanceMeters: Float,
    directionName: String,
    isTogether: Boolean,
    partnerName: String,
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    accentColor: Color
) {
    val isDark = theme == "Pixel Oscuro"
    val animatedRotation by animateFloatAsState(
        targetValue = bearingDegrees,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "compassRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, borderColor)
            .background(cardBg)
            .padding(16.dp)
            .clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧭 BRÚJULA DE AMOR",
            fontFamily = Vt323,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = if (isTogether) "¡Están juntos! ❤️" else "Apuntando hacia $partnerName",
            fontFamily = Vt323,
            fontSize = 18.sp,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dial de Brújula Pixel Art
        Box(
            modifier = Modifier
                .size(230.dp)
                .border(4.dp, borderColor)
                .background(if (isDark) Color(0xFF141414) else Color(0xFFFFF9E6)),
            contentAlignment = Alignment.Center
        ) {
            // Puntos Cardinales
            Text("N", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            Text("S", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            Text("E", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
            Text("O", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))

            // Círculo central con marcas
            Canvas(modifier = Modifier.size(190.dp)) {
                drawCircle(
                    color = borderColor.copy(alpha = 0.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Aguja Giratoria hacia la Pareja
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedRotation),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Flecha Norte / Pareja (Rosa Neón con Corazón)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(text = "❤️", fontSize = 22.sp)
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(46.dp)
                                .background(accentColor)
                        )
                    }

                    // Centro de la brújula
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(2.dp, borderColor)
                            .background(Color.White)
                    )

                    // Cola de la flecha
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(42.dp)
                                .background(textColor.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lecturas de Distancia y Ángulo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(if (isDark) Color(0xFF2C1E26) else Color(0xFFFFEEF5))
                .padding(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RUMBO: ${bearingDegrees.roundToInt()}° $directionName",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                val distStr = if (distanceMeters >= 1000f) "${String.format(Locale.US, "%.2f", distanceMeters / 1000f)} Kilómetros" else "${distanceMeters.roundToInt()} Metros"
                Text(
                    text = "DISTANCIA: $distStr",
                    fontFamily = Vt323,
                    fontSize = 17.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
