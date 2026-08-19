package calendario.kevshupp.diariokevinali.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

    // View state: "grid", "checklist", "anime", "meds", "web", "schedule"
    var currentView by remember(initialView) { mutableStateOf(initialView) }

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
            onSelectSchedule = { currentView = "schedule" }
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
    onSelectSchedule: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    var showWebDialog by remember { mutableStateOf(false) }

    // Genera una imagen aleatoria de espíritu de Cloudinary cada vez que se entra al menú
    val spiritIconUrl = remember {
        val randomNum = (1..141).random()
        val formattedNum = String.format("%02d", randomNum)
        "https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_$formattedNum.png"
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

        // 2x2 Grid Layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First item: active Fortnite Spirits Checklist button
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectSpirits() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = spiritIconUrl,
                            contentDescription = "Espíritus Fortnite",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Espíritus",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Second item: Anime section
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectAnime() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📺",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Anime",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Third item: Web manager section (abre web directamente en WebView integrado)
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectWeb() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🌐",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Web Gestión",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Fourth item: Medicamentos
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectMeds() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💊",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Medicamentos",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Fifth item: Horario de Clases
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(3.dp, borderColor)
                        .background(cardBg)
                        .clickable { onSelectSchedule() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📚",
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Horario",
                            fontFamily = Vt323,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }

    // showWebDialog fue eliminado
}
