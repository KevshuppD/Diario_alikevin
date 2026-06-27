package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
    
    // View state: "grid" or "checklist"
    var currentView by remember { mutableStateOf("grid") }

    if (currentView == "grid") {
        MiscGridView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = onBack,
            onSelectSpirits = { currentView = "checklist" }
        )
    } else {
        SpiritsChecklistView(
            theme = theme,
            textColor = textColor,
            borderColor = borderColor,
            cardBg = cardBg,
            onBack = { currentView = "grid" }
        )
    }
}

@Composable
fun MiscGridView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit,
    onSelectSpirits: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current

    // Dynamically retrieve the first spirit icon for the button logo
    val spiritIconId = remember(context) {
        val id = context.resources.getIdentifier("ic_spirit_01", "drawable", context.packageName)
        if (id != 0) id else android.R.drawable.ic_menu_gallery
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
                text = "◀",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "MISCELÁNEO 🌟",
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
                        Image(
                            painter = painterResource(id = spiritIconId),
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

            // Other 3 slots: placeholders for future activities (locked)
            items(3) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(
                            width = 2.dp,
                            color = borderColor.copy(alpha = 0.4f)
                        )
                        .background(cardBg.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bloqueado",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpiritsChecklistView(
    theme: String,
    textColor: Color,
    borderColor: Color,
    cardBg: Color,
    onBack: () -> Unit
) {
    val isDark = theme == "Pixel Oscuro"
    val isMono = theme == "Pixel Monocromático"
    val context = LocalContext.current
    
    // User Session Configuration
    val prefs = remember(context) { context.getSharedPreferences("DiarioPrefs", Context.MODE_PRIVATE) }
    val currentUserId = remember(prefs) { prefs.getString("userId", "user_kevin_01") ?: "user_kevin_01" }
    val coupleId = remember(prefs) { prefs.getString("coupleId", "vínculo_único_123") ?: "vínculo_único_123" }
    
    // Deterministic identification of roles
    val isKevin = currentUserId == "user_kevin_01"
    
    // Firebase references
    val db = FirebaseFirestore.getInstance()
    var kevinList by remember { mutableStateOf(emptyList<String>()) }
    var aliList by remember { mutableStateOf(emptyList<String>()) }

    // Read real-time values from Firestore
    DisposableEffect(coupleId) {
        val listener = db.collection("fortnite_spirits").document(coupleId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val kList = snapshot.get("kevin_list") as? List<*>
                    val aList = snapshot.get("ali_list") as? List<*>
                    kevinList = kList?.filterIsInstance<String>() ?: emptyList()
                    aliList = aList?.filterIsInstance<String>() ?: emptyList()
                } else {
                    kevinList = emptyList()
                    aliList = emptyList()
                }
            }
        onDispose {
            listener.remove()
        }
    }

    // Toggle check state function
    val onToggleCheck: (String, Boolean, String) -> Unit = { spiritId, currentlyChecked, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_list") kevinList else aliList
        val newList = if (currentlyChecked) {
            currentList.filter { it != spiritId }
        } else {
            currentList + spiritId
        }
        db.collection("fortnite_spirits").document(coupleId)
            .set(mapOf(targetUserKey to newList), SetOptions.merge())
    }

    val spiritsList = remember { (1..65).map { String.format("%02d", it) } }
    val spiritNames = remember {
        listOf(
            // Fila 1
            "Espíritu de Agua", "Espíritu de Agua Dorado", "Espíritu de Agua Gomita", "Espíritu de Agua Galaxia",
            "Espíritu Dormilón", "Espíritu Dormilón Dorado", "Espíritu Dormilón Gomita", "Espíritu Dormilón Galaxia",
            // Fila 2
            "Espíritu de Tierra", "Espíritu de Tierra Dorado", "Espíritu de Tierra Gomita", "Espíritu de Tierra Galaxia",
            "TheBurntPeanut (Espíritu del Cacahuete)",
            "Espíritu Demoníaco", "Espíritu Demoníaco Dorado", "Espíritu Demoníaco Gomita", "Espíritu Demoníaco Galaxia",
            // Fila 3
            "Espíritu de Fuego", "Espíritu de Fuego Dorado", "Espíritu de Fuego Gomita", "Espíritu de Fuego Galaxia",
            "Espíritu Punk", "Espíritu Punk Dorado", "Espíritu Punk Gomita", "Espíritu Punk Galaxia",
            // Fila 4
            "Espíritu Pato", "Espíritu Pato Dorado", "Espíritu Pato Gomita", "Espíritu Pato Galaxia",
            "Espíritu Rey", "Espíritu Rey Dorado", "Espíritu Rey Gomita", "Espíritu Rey Galaxia",
            // Fila 5
            "Espíritu Fantasma", "Espíritu Fantasma Dorado", "Espíritu Fantasma Gomita", "Espíritu Fantasma Galaxia",
            "Espíritu del Punto Cero", "Espíritu del Punto Cero Dorado", "Espíritu del Punto Cero Gomita", "Espíritu del Punto Cero Galaxia",
            // Fila 6 (Nuevos)
            "Espíritu de Aura", "Espíritu de Aura Dorado", "Espíritu de Aura Galaxia", "Espíritu de Aura Gomita",
            "Espíritu de la Fundación", "Espíritu de la Fundación Dorado", "Espíritu de la Fundación Galaxia", "Espíritu de la Fundación Gomita",
            // Fila 7 (Nuevos)
            "Espíritu de la Parca", "Espíritu de la Parca Dorado", "Espíritu de la Parca Galaxia", "Espíritu de la Parca Gomita",
            "Espíritu Futbolero", "Espíritu Futbolero Dorado", "Espíritu Futbolero Galaxia", "Espíritu Futbolero Gomita",
            // Fila 8 (Nuevos)
            "Espíritu Jefe", "Espíritu Jefe Dorado", "Espíritu Jefe Galaxia", "Espíritu Jefe Gomita",
            "Espíritu Pescado", "Espíritu Pescado Dorado", "Espíritu Pescado Galaxia", "Espíritu Pescado Gomita"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◀",
                fontFamily = Vt323,
                fontSize = 24.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "COLECCIÓN ESPÍRITUS 👻",
                fontFamily = Vt323,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // Gamified Scoreboard Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor)
                .background(cardBg)
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Kevin 🙋‍♂️",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${kevinList.size} / 41",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFBD93F9) else Color(0xFF4A2511)
                        )
                    }
                    
                    Text(
                        text = "VS",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ali 🙋‍♀️",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${aliList.size} / 41",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFFF79C6) else Color(0xFF4A2511)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cute Leaderboard message
                val leaderMsg = when {
                    kevinList.size > aliList.size -> "¡Kevin va a la cabeza! 🏆"
                    aliList.size > kevinList.size -> "¡Ali va a la cabeza! 🏆"
                    kevinList.size > 0 -> "¡Están empatados! 🤜🤛"
                    else -> "¡Empiecen su colección! 💕"
                }

                Text(
                    text = leaderMsg,
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checklist Items List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(spiritsList) { index, spiritId ->
                val hasKevin = kevinList.contains(spiritId)
                val hasAli = aliList.contains(spiritId)
                
                val spiritResId = remember(spiritId, context) {
                    val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                    if (id != 0) id else android.R.drawable.ic_menu_gallery
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, borderColor)
                        .background(cardBg)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spirit Thumbnail Container (Larger)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, borderColor.copy(alpha = 0.5f))
                            .background(if (isDark) Color(0xFF121212) else Color(0xFFFFFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = spiritResId),
                            contentDescription = "Espíritu $spiritId",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title
                    val spiritName = spiritNames.getOrElse(index) { "Espíritu #$spiritId" }
                    Text(
                        text = spiritName,
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    // Kevin's Checkbox & Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "K",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        RetroCheckbox(
                            checked = hasKevin,
                            enabled = isKevin,
                            borderColor = borderColor,
                            onCheckedChange = { onToggleCheck(spiritId, hasKevin, "kevin_list") }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ali's Checkbox & Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "A",
                            fontFamily = Vt323,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        RetroCheckbox(
                            checked = hasAli,
                            enabled = !isKevin, // enabled if current user is not Kevin (meaning they are Ali)
                            borderColor = borderColor,
                            onCheckedChange = { onToggleCheck(spiritId, hasAli, "ali_list") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetroCheckbox(
    checked: Boolean,
    enabled: Boolean,
    borderColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val boxBg = if (checked) Color(0xFF81C784) else Color(0x22000000)
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.5f))
            .background(boxBg)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(
                text = "X",
                fontFamily = Vt323,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
