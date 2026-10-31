package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Controles compartidos de Configuración: mismos botones segmentados y filas en todas las secciones.

@Composable
fun SettingsSectionLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text = text, fontFamily = Vt323, fontSize = 18.sp, color = color, modifier = modifier)
}

/** Selector de una opción: reemplaza a los RadioButton pequeños con botones fáciles de tocar. */
@Composable
fun <T> PixelSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    textColor: Color,
    borderColor: Color,
    background: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(2.dp, borderColor)
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            if (index > 0) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(borderColor))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) Color(0xFFD97706) else background)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else textColor
                )
            }
        }
    }
}

/** Fila de acción con ícono, título y una frase corta. */
@Composable
fun SettingsToolRow(
    icon: String,
    title: String,
    description: String,
    background: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = Vt323, fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Bold)
            Text(description, fontFamily = Vt323, fontSize = 15.sp, color = textColor.copy(alpha = 0.75f))
        }
        Text("▶", fontFamily = Vt323, fontSize = 16.sp, color = borderColor, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Cerrar sesión con el mismo aspecto en Perfil y Sistema: visible, pero sin relleno rojo. */
@Composable
fun PixelLogoutButton(isDark: Boolean, onClick: () -> Unit) {
    val red = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(2.dp, red)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("🚪 CERRAR SESIÓN", fontFamily = Vt323, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = red)
    }
}
