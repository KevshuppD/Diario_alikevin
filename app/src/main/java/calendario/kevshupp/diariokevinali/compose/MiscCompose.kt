package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiscScreen(
    theme: String,
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val backgroundColor = getAppBackgroundColor(theme)
    val textColor = if (isDark) Color.White else if (isMono) Color.Black else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else if (isMono) Color.Black else Color(0xFF4A2511)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else if (isMono) Color.White else Color(0xFFFFFBEA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título Retro
        Text(
            text = "MISCELÁNEO 🌟",
            fontFamily = Vt323,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        // Tarjeta central de proximamente
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(3.dp, borderColor)
                .background(cardBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Próximamente...",
                    fontFamily = Vt323,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nuevas miniactividades, juegos y sorpresas se desbloquearán aquí muy pronto. ¡Mantente atento! 💕",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
