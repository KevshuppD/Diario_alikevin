package calendario.kevshupp.diariokevinali.compose

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
