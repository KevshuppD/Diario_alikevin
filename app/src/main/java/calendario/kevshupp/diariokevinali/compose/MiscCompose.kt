package calendario.kevshupp.diariokevinali.compose

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient

@Composable
fun MiscScreen(
    theme: String,
    initialView: String = "grid",
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val backgroundColor = getAppBackgroundColor(theme)
    val textColor = if (isDark) Color.White else if (isMono) Color.Black else Color(0xFF4A2511)
    val borderColor = if (isDark) Color(0xFF91465F) else if (isMono) Color.Black else Color(0xFF4A2511)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else if (isMono) Color.White else Color(0xFFFFFBEA)

    // View state: "grid", "checklist", "anime", "meds", "web", "schedule", "radar", "mood"
    var currentView by remember(initialView) { mutableStateOf(initialView) }

    // Interceptar el botón Atrás del sistema cuando se esté dentro de una sub-sección de Misceláneos
    BackHandler(enabled = currentView != "grid") {
        currentView = "grid"
    }

    if (currentView == "grid") {
        MiscGridView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = onBack,
            onSelectSpirits = { currentView = "checklist" },
            onSelectAnime = { currentView = "anime" },
            onSelectMeds = { currentView = "meds" },
            onSelectWeb = { currentView = "web" },
            onSelectSchedule = { currentView = "schedule" },
            onSelectRadar = { currentView = "radar" },
            onSelectMood = { currentView = "mood" }
        )
    } else if (currentView == "checklist") {
        SpiritsChecklistView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else if (currentView == "anime") {
        AnimeDashboardView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else if (currentView == "meds") {
        MedsDashboardView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else if (currentView == "schedule") {
        ScheduleDashboardView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else if (currentView == "mood") {
        MoodLogDashboardView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    } else if (currentView == "radar") {
        ThorRadarScreen(
            theme = theme,
            onBack = { currentView = "grid" }
        )
    } else {
        // WebView integrada para la Web de Gestión
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "< Volver",
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    color = textColor,
                    modifier = Modifier.clickable { currentView = "grid" }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "WEB DE GESTIÓN",
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl("https://diario-alikevin.vercel.app/")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MiscGridView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit,
    onSelectSpirits: () -> Unit,
    onSelectAnime: () -> Unit,
    onSelectMeds: () -> Unit,
    onSelectWeb: () -> Unit,
    onSelectSchedule: () -> Unit,
    onSelectRadar: () -> Unit,
    onSelectMood: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    // Guardar el icono del espíritu en SharedPreferences para que persista en la sesión y se cargue al 100% de la caché local
    val spiritIconUrl = remember(context) {
        val prefs = context.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
        var cachedUrl = prefs.getString("misc_spirit_cached_icon", null)
        if (cachedUrl.isNullOrBlank()) {
            val randomNum = (1..141).random()
            val formattedNum = String.format("%02d", randomNum)
            cachedUrl = "https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_$formattedNum.png"
            prefs.edit().putString("misc_spirit_cached_icon", cachedUrl).apply()
        }
        cachedUrl
    }

    val spiritImageModel = remember(spiritIconUrl, context) {
        coil.request.ImageRequest.Builder(context)
            .data(spiritIconUrl)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar/Title Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "MISCELÁNEO",
                fontFamily = Vt323,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // Lista por temas: todo cabe en pantalla, igual que el menú de Configuración
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val row: @Composable (String, String, String, () -> Unit) -> Unit = { icon, title, description, onClick ->
                SettingsToolRow(icon, title, description, cardBg, textColor, borderColor, onClick)
            }
            SettingsSectionLabel("NOSOTROS", textColor)
            row("🧭", "Thor Radar", "Ubicación, zonas y alerta SOS", onSelectRadar)
            row("📔", "Bitácora de Ánimo", "Cómo nos sentimos cada día", onSelectMood)
            row("📚", "Horario", "Clases de Kevin y Ali", onSelectSchedule)
            SettingsSectionLabel("SALUD", textColor, Modifier.padding(top = 6.dp))
            row("💊", "Medicamentos", "Tomas y recordatorios", onSelectMeds)
            SettingsSectionLabel("ENTRETENIMIENTO", textColor, Modifier.padding(top = 6.dp))
            row("📺", "Anime", "Lo que vemos juntos", onSelectAnime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, borderColor)
                    .background(cardBg)
                    .clickable { onSelectSpirits() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(model = spiritImageModel, contentDescription = "Espíritus Fortnite", modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Espíritus", fontFamily = Vt323, fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Bold)
                    Text("Colección de Fortnite", fontFamily = Vt323, fontSize = 15.sp, color = textColor.copy(alpha = 0.75f))
                }
                Text("▶", fontFamily = Vt323, fontSize = 16.sp, color = borderColor)
            }
            SettingsSectionLabel("HERRAMIENTAS", textColor, Modifier.padding(top = 6.dp))
            row("🌐", "Web Gestión", "Abre la página de administración", onSelectWeb)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
