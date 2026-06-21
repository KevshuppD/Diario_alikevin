package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import calendario.kevshupp.diariokevinali.R

// Si Vt323 no está en este archivo, se asume que está en el mismo paquete (RecipeCompose.kt)
// val Vt323 = FontFamily(Font(R.font.vt323))

@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    message: String = "Cargando..."
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2D2D2D)), // Fondo oscuro sólido
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF80AB), // Rosa pixel
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontFamily = Vt323,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun setOverlayContent(
    composeView: androidx.compose.ui.platform.ComposeView,
    isUploadingState: androidx.compose.runtime.MutableState<Boolean>,
    messageState: androidx.compose.runtime.MutableState<String>? = null
) {
    composeView.setContent {
        val msg = messageState?.value ?: "Cargando..."
        LoadingOverlay(isVisible = isUploadingState.value, message = msg)
    }
}

@Composable
fun getAppBackgroundColor(theme: String): Color {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE)
    val useCustomBg = prefs.getBoolean("useCustomBg", false)
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    
    return when {
        isMono -> Color.White
        isDark -> {
            if (useCustomBg) {
                val darkColorStr = prefs.getString("darkColor", "#7C3AED") ?: "#7C3AED"
                try {
                    val baseColor = Color(android.graphics.Color.parseColor(darkColorStr))
                    baseColor.toDarkVariant()
                } catch (e: Exception) {
                    Color(0xFF0D0D2B)
                }
            } else {
                Color(0xFF0D0D2B) // Default midnight blue
            }
        }
        else -> {
            if (useCustomBg) {
                val lightColorStr = prefs.getString("lightColor", "#4A148C") ?: "#4A148C"
                try {
                    val baseColor = Color(android.graphics.Color.parseColor(lightColorStr))
                    baseColor.toPastelVariant()
                } catch (e: Exception) {
                    Color(0xFFF5E6BE)
                }
            } else {
                Color(0xFFF5E6BE) // Cream / Stardew Valley color
            }
        }
    }
}

fun Color.toPastelVariant(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] = 0.12f // Saturation
    hsv[2] = 0.98f // Value
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Color.toDarkVariant(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] = 0.6f  // Saturation
    hsv[2] = 0.10f // Value
    return Color(android.graphics.Color.HSVToColor(hsv))
}

