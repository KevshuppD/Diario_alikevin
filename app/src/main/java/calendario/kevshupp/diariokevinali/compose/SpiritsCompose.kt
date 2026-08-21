package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private fun mergeCategories(
    existing: List<SpiritCategory>,
    defaults: List<SpiritCategory>,
    newSpiritIds: List<String>
): List<SpiritCategory> {
    val assignedIds = existing.flatMap { it.spiritIds }.toSet()
    val result = existing.map { cat ->
        val defaultCat = defaults.find { it.name == cat.name }
        val toAdd = defaultCat?.spiritIds?.filter { it in newSpiritIds && !assignedIds.contains(it) } ?: emptyList()
        SpiritCategory(cat.name, cat.spiritIds + toAdd)
    }.toMutableList()
    
    defaults.forEach { defaultCat ->
        if (result.none { it.name == defaultCat.name }) {
            val filteredIds = defaultCat.spiritIds.filter { it in newSpiritIds && !assignedIds.contains(it) }
            if (filteredIds.isNotEmpty()) {
                result.add(SpiritCategory(defaultCat.name, filteredIds))
            }
        }
    }
    return result
}

data class SpiritCategory(
    val name: String,
    val spiritIds: List<String>
)

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

@Composable
fun CategoryHeader(
    category: SpiritCategory,
    displayName: String,
    isExpanded: Boolean,
    borderColor: Color,
    cardBg: Color,
    textColor: Color,
    kevinCount: Int,
    aliCount: Int,
    kevinMasteryCount: Int,
    aliMasteryCount: Int,
    isEditMode: Boolean = false,
    onEditCategoryName: () -> Unit = {},
    onDeleteCategory: () -> Unit = {},
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isExpanded) "v" else ">",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontFamily = Vt323,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = if (isEditMode) Modifier.clickable { onEditCategoryName() } else Modifier
                )
                if (isEditMode) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onEditCategoryName() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✏️",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clickable { onDeleteCategory() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "❌",
                            fontSize = 16.sp
                        )
                    }
                }
            }
            if (!isEditMode) {
                Text(
                    text = "K: $kevinCount ($kevinMasteryCount👑) | A: $aliCount ($aliMasteryCount👑)",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SpiritRow(
    spiritId: String,
    spiritResId: Int,
    spiritName: String,
    hasKevin: Boolean,
    hasAli: Boolean,
    hasKevinMastery: Boolean,
    hasAliMastery: Boolean,
    isKevin: Boolean,
    isDark: Boolean,
    borderColor: Color,
    textColor: Color,
    cardBg: Color,
    isEditMode: Boolean = false,
    onEditName: () -> Unit = {},
    onDeleteSpirit: () -> Unit = {},
    onToggleCheck: (String, Boolean, String) -> Unit,
    onToggleMastery: (String, Boolean, String) -> Unit,
    customImageUrl: String? = null,
    imageRefreshKey: Int = 0
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(cardBg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(1.dp, borderColor.copy(alpha = 0.5f))
                .background(if (isDark) Color(0xFF121212) else Color(0xFFFFFDF5)),
            contentAlignment = Alignment.Center
        ) {
            val formattedId = remember(spiritId) {
                val num = spiritId.toIntOrNull()
                if (num != null) String.format("%02d", num) else spiritId
            }
            val spiritImageUrl = remember(spiritId, customImageUrl, formattedId, imageRefreshKey) {
                if (!customImageUrl.isNullOrBlank()) {
                    customImageUrl
                } else {
                    try {
                        com.cloudinary.android.MediaManager.get().url().generate("spirits/ic_spirit_$formattedId")
                    } catch (e: Exception) {
                        "https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_$formattedId.png"
                    }
                }
            }
            val imageModel = if (!customImageUrl.isNullOrBlank()) {
                ImageRequest.Builder(context)
                    .data(spiritImageUrl)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            } else {
                spiritImageUrl
            }
            AsyncImage(
                model = imageModel,
                contentDescription = "Espíritu $spiritId",
                placeholder = if (spiritResId != 0) painterResource(id = spiritResId) else null,
                error = if (spiritResId != 0) painterResource(id = spiritResId) else null,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spiritName,
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = if (isEditMode) Modifier.clickable { onEditName() } else Modifier
            )
            if (isEditMode) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clickable { onEditName() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "✏️",
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (!isEditMode) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RetroCheckbox(
                        checked = hasKevin,
                        enabled = isKevin,
                        borderColor = borderColor,
                        onCheckedChange = { onToggleCheck(spiritId, hasKevin, "kevin_list") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val crownAlpha = if (hasKevinMastery) 1f else 0.25f
                    Text(
                        text = "👑",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .alpha(crownAlpha)
                            .clickable(enabled = isKevin && hasKevin) {
                                onToggleMastery(spiritId, hasKevinMastery, "kevin_mastery")
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RetroCheckbox(
                        checked = hasAli,
                        enabled = !isKevin,
                        borderColor = borderColor,
                        onCheckedChange = { onToggleCheck(spiritId, hasAli, "ali_list") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val crownAlpha = if (hasAliMastery) 1f else 0.25f
                    Text(
                        text = "👑",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .alpha(crownAlpha)
                            .clickable(enabled = !isKevin && hasAli) {
                                onToggleMastery(spiritId, hasAliMastery, "ali_mastery")
                            }
                    )
                }
            }
        } else {
            // Show Delete button (Cross ❌) on the right edge in Edit Mode!
            Box(
                modifier = Modifier
                    .clickable { onDeleteSpirit() }
                    .padding(8.dp)
            ) {
                Text(
                    text = "❌",
                    fontSize = 20.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpiritGridCard(
    spiritId: String,
    spiritResId: Int,
    spiritName: String,
    hasKevin: Boolean,
    hasAli: Boolean,
    hasKevinMastery: Boolean,
    hasAliMastery: Boolean,
    isKevin: Boolean,
    isDark: Boolean,
    borderColor: Color,
    textColor: Color,
    cardBg: Color,
    isEditMode: Boolean = false,
    onEditName: () -> Unit = {},
    onDeleteSpirit: () -> Unit = {},
    onToggleCheck: (String, Boolean, String) -> Unit,
    onToggleMastery: (String, Boolean, String) -> Unit,
    customImageUrl: String? = null,
    imageRefreshKey: Int = 0
) {
    val hasCurrent = if (isKevin) hasKevin else hasAli
    val hasCurrentMastery = if (isKevin) hasKevinMastery else hasAliMastery
    val currentListKey = if (isKevin) "kevin_list" else "ali_list"
    val currentMasteryKey = if (isKevin) "kevin_mastery" else "ali_mastery"
    val context = LocalContext.current

    val obtainedBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val missingBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
    val finalBg = if (hasCurrent) obtainedBg else missingBg

    val cardBorderColor = if (hasCurrent) borderColor else borderColor.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .border(2.dp, cardBorderColor)
            .background(finalBg)
            .combinedClickable(
                onClick = {
                    if (isEditMode) {
                        onEditName()
                    } else {
                        onToggleCheck(spiritId, hasCurrent, currentListKey)
                    }
                },
                onLongClick = {
                    if (!isEditMode && hasCurrent) {
                        onToggleMastery(spiritId, hasCurrentMastery, currentMasteryKey)
                    }
                }
            )
            .padding(4.dp)
    ) {
        // Indicators Row (Top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kevin indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (hasKevin) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        fontFamily = Vt323,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasKevinMastery) {
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(text = "👑", fontSize = 11.sp)
                }
            }

            // Ali indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasAliMastery) {
                    Text(text = "👑", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (hasAli) Color(0xFFEC4899) else Color.Gray.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontFamily = Vt323,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Spirit Image (Center)
        val formattedId = remember(spiritId) {
            val num = spiritId.toIntOrNull()
            if (num != null) String.format("%02d", num) else spiritId
        }
        val spiritImageUrl = remember(spiritId, customImageUrl, formattedId, imageRefreshKey) {
            if (!customImageUrl.isNullOrBlank()) {
                customImageUrl
            } else {
                try {
                    com.cloudinary.android.MediaManager.get().url().generate("spirits/ic_spirit_$formattedId")
                } catch (e: Exception) {
                    "https://res.cloudinary.com/dhaqjw7se/image/upload/spirits/ic_spirit_$formattedId.png"
                }
            }
        }
        val imageModel = if (!customImageUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(spiritImageUrl)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        } else {
            spiritImageUrl
        }
        AsyncImage(
            model = imageModel,
            contentDescription = spiritName,
            placeholder = if (spiritResId != 0) painterResource(id = spiritResId) else null,
            error = if (spiritResId != 0) painterResource(id = spiritResId) else null,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
        )

        // Delete button if in edit mode (Top Center)
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 18.dp)
                    .size(20.dp)
                    .background(Color.Red)
                    .clickable { onDeleteSpirit() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Spirit Name (Bottom)
        Text(
            text = spiritName,
            fontFamily = Vt323,
            fontSize = 11.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        )
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

    // Season Selection (Default: Temporada 2)
    var currentSeason by remember { mutableStateOf(2) }
    val firestoreCollection = if (currentSeason == 1) "fortnite_spirits" else "fortnite_spirits_s2"
    
    val defaultCategoriesT1 = remember {
        listOf(
            SpiritCategory("Espíritu de Batman", (98..104).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de Agua", (1..4).map { String.format("%02d", it) } + listOf("66", "67", "112")),
            SpiritCategory("Espíritu de Tierra", (9..12).map { String.format("%02d", it) } + listOf("70", "71", "114")),
            SpiritCategory("Espíritu de Fuego", (18..21).map { String.format("%02d", it) } + listOf("74", "75", "116")),
            SpiritCategory("Espíritu Pato", (26..29).map { String.format("%02d", it) } + listOf("78", "79", "118") + listOf("136", "137", "138", "139")),
            SpiritCategory("Espíritu Fantasma", (34..37).map { String.format("%02d", it) } + listOf("82", "83")),
            SpiritCategory("Espíritu Dormilón", (5..8).map { String.format("%02d", it) } + listOf("68", "69", "113")),
            SpiritCategory("Espíritu Demoníaco", (14..17).map { String.format("%02d", it) } + listOf("72", "73", "115")),
            SpiritCategory("Espíritu Punk", (22..25).map { String.format("%02d", it) } + listOf("76", "77", "117")),
            SpiritCategory("Espíritu Monarca", (30..33).map { String.format("%02d", it) } + listOf("80", "81")),
            SpiritCategory("Espíritu del Punto Cero", (38..41).map { String.format("%02d", it) } + listOf("84", "85")),
            SpiritCategory("Espíritu Pescado", (62..65).map { String.format("%02d", it) } + listOf("96", "97")),
            SpiritCategory("Espíritu Futbolero", (54..57).map { String.format("%02d", it) } + listOf("92", "93")),
            SpiritCategory("Espíritu de Aura", (42..45).map { String.format("%02d", it) } + listOf("86", "87")),
            SpiritCategory("Espíritu Jefe", (58..61).map { String.format("%02d", it) } + listOf("94", "95")),
            SpiritCategory("Espíritu de la Parca", (50..53).map { String.format("%02d", it) } + listOf("90", "91") + listOf("134", "135")),
            SpiritCategory("Espíritu de Viento", (105..111).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de la Fundación", (46..49).map { String.format("%02d", it) } + listOf("88", "89")),
            SpiritCategory("Espíritu de Llama", (122..126).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de Bananín", (129..133).map { String.format("%02d", it) }),
            SpiritCategory("Espíritu de Cubo", listOf("127", "128")),
            SpiritCategory("Espíritu Especial/Invitado", listOf("13") + (119..121).map { String.format("%02d", it) } + listOf("140", "141"))
        )
    }
    val defaultSpiritsListT1 = remember { (1..141).map { String.format("%02d", it) } }
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
            "Espíritu Monarca", "Espíritu Monarca Dorado", "Espíritu Monarca Gomita", "Espíritu Monarca Galaxia",
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
            "Espíritu Pescado", "Espíritu Pescado Dorado", "Espíritu Pescado Galaxia", "Espíritu Pescado Gomita",
            // Nuevas Variantes (Gema y Holofoil)
            "Espíritu de Agua Gema", "Espíritu de Agua Holofoil",
            "Espíritu Dormilón Gema", "Espíritu Dormilón Holofoil",
            "Espíritu de Tierra Gema", "Espíritu de Tierra Holofoil",
            "Espíritu Demoníaco Gema", "Espíritu Demoníaco Holofoil",
            "Espíritu de Fuego Gema", "Espíritu de Fuego Holofoil",
            "Espíritu Punk Gema", "Espíritu Punk Holofoil",
            "Espíritu Pato Gema", "Espíritu Pato Holofoil",
            "Espíritu Monarca Gema", "Espíritu Monarca Holofoil",
            "Espíritu Fantasma Gema", "Espíritu Fantasma Holofoil",
            "Espíritu del Punto Cero Gema", "Espíritu del Punto Cero Holofoil",
            "Espíritu de Aura Gema", "Espíritu de Aura Holofoil",
            "Espíritu de la Fundación Gema", "Espíritu de la Fundación Holofoil",
            "Espíritu de la Parca Gema", "Espíritu de la Parca Holofoil",
            "Espíritu Futbolero Gema", "Espíritu Futbolero Holofoil",
            "Espíritu Jefe Gema", "Espíritu Jefe Holofoil",
            "Espíritu Pescado Gema", "Espíritu Pescado Holofoil",
            // Nuevos Espíritus (98..121)
            // Fila 9 (Batman)
            "Espíritu de Batman", "Espíritu de Batman Dorado", "Espíritu de Batman Gomita", "Espíritu de Batman Galaxia", "Espíritu de Batman Gema", "Espíritu de Batman Holofoil", "Espíritu de Batman Cubo",
            // Fila 10 (Viento)
            "Espíritu de Viento", "Espíritu de Viento Dorado", "Espíritu de Viento Gomita", "Espíritu de Viento Galaxia", "Espíritu de Viento Gema", "Espíritu de Viento Holofoil", "Espíritu de Viento Cubo",
            // Fila 11 (Cubo)
            "Espíritu de Agua Cubo", "Espíritu Dormilón Cubo", "Espíritu de Tierra Cubo", "Espíritu Demoníaco Cubo", "Espíritu de Fuego Cubo", "Espíritu Punk Cubo", "Espíritu Pato Cubo",
            // Fila 12 (Especiales / Invitados)
            "Espíritu Pollo", "Espíritu de Vini Jr.", "Espíritu de la Fundación Especial",
            // Nuevos Espíritus (122..141)
            "Espíritu de Llama", "Espíritu de Llama Dorado", "Espíritu de Llama Gomita", "Espíritu de Llama Galaxia", "Espíritu de Llama Gema",
            "Espíritu de Cubo Arcoíris", "Espíritu de Cubo Galaxia Oscura",
            "Espíritu de Bananín", "Espíritu de Bananín Dorado", "Espíritu de Bananín Gomita", "Espíritu de Bananín Arcoíris", "Espíritu de Bananín Galaxia",
            "Espíritu de la Parca Arcoíris", "Espíritu de la Parca Gema",
            "Espíritu de Tierra Quack", "Espíritu de Fuego Quack", "Espíritu de Agua Quack", "Espíritu de Punto Cero Quack",
            "Espíritu de John Wick", "Espíritu de Ironmouse"
        )
    }

    var categories by remember { mutableStateOf(emptyList<SpiritCategory>()) }
    var spiritsList by remember { mutableStateOf(emptyList<String>()) }

    // Firebase references
    val db = FirebaseFirestore.getInstance()
    var kevinList by remember { mutableStateOf(emptyList<String>()) }
    var aliList by remember { mutableStateOf(emptyList<String>()) }
    var kevinMastery by remember { mutableStateOf(emptyList<String>()) }
    var aliMastery by remember { mutableStateOf(emptyList<String>()) }

    var customNames by remember { mutableStateOf(emptyMap<String, String>()) }
    var customCategories by remember { mutableStateOf(emptyMap<String, String>()) }
    var customImages by remember { mutableStateOf(emptyMap<String, String>()) }
    var isEditMode by remember { mutableStateOf(false) }

    var editingSpiritId by remember { mutableStateOf<String?>(null) }
    var editingSpiritInitialName by remember { mutableStateOf("") }
    
    var editingCategoryOriginalName by remember { mutableStateOf<String?>(null) }
    var editingCategoryInitialName by remember { mutableStateOf("") }

    var deletingSpiritId by remember { mutableStateOf<String?>(null) }
    var deletingCategoryName by remember { mutableStateOf<String?>(null) }

    // Read real-time values from Firestore based on coupleId and currentSeason
    DisposableEffect(coupleId, currentSeason) {
        val listener = db.collection(firestoreCollection).document(coupleId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val kList = snapshot.get("kevin_list") as? List<*>
                    val aList = snapshot.get("ali_list") as? List<*>
                    val kMastery = snapshot.get("kevin_mastery") as? List<*>
                    val aMastery = snapshot.get("ali_mastery") as? List<*>
                    kevinList = kList?.filterIsInstance<String>() ?: emptyList()
                    aliList = aList?.filterIsInstance<String>() ?: emptyList()
                    kevinMastery = kMastery?.filterIsInstance<String>() ?: emptyList()
                    aliMastery = aMastery?.filterIsInstance<String>() ?: emptyList()

                    val cNames = snapshot.get("custom_names") as? Map<*, *>
                    val cCategories = snapshot.get("custom_categories") as? Map<*, *>
                    val cImages = snapshot.get("custom_images") as? Map<*, *>
                    customNames = cNames?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
                    customCategories = cCategories?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
                    customImages = cImages?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()

                    val dbSpiritsList = snapshot.get("spirits_list") as? List<*>
                    val parsedSpiritsList = dbSpiritsList?.filterIsInstance<String>() ?: emptyList()

                    val dbCategories = snapshot.get("categories") as? List<*>
                    val parsedCategories = dbCategories?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val name = map["name"] as? String ?: return@mapNotNull null
                        val spiritIds = (map["spiritIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        SpiritCategory(name, spiritIds)
                    } ?: emptyList()

                    var schemaVersion = (snapshot.get("schema_version") as? Number)?.toInt() ?: 1

                    if (parsedSpiritsList.isEmpty()) {
                        if (currentSeason == 1) {
                            categories = defaultCategoriesT1
                            spiritsList = defaultSpiritsListT1
                        } else {
                            categories = emptyList()
                            spiritsList = emptyList()
                        }
                    } else {
                        // Apply selection migration if version is 1 (for Season 1)
                        if (currentSeason == 1 && schemaVersion == 1) {
                            val migrateSelections = { oldList: List<String> ->
                                oldList.map { id ->
                                    val num = id.toIntOrNull() ?: return@map id
                                    val newNum = when {
                                        num in 117..119 -> num + 2
                                        num in 110..116 -> num + 2
                                        num in 104..109 -> num + 1
                                        else -> num
                                    }
                                    String.format("%02d", newNum)
                                }.distinct()
                            }
                            kevinList = migrateSelections(kevinList)
                            aliList = migrateSelections(aliList)
                            kevinMastery = migrateSelections(kevinMastery)
                            aliMastery = migrateSelections(aliMastery)
                            schemaVersion = 3 // Promotion to base v3 for local logic
                        }

                        if (currentSeason == 1 && schemaVersion < 4) {
                            val baseCategories = parsedCategories.ifEmpty { defaultCategoriesT1 }
                            val mergedCategories = mergeCategories(baseCategories, defaultCategoriesT1, (122..141).map { String.format("%02d", it) })
                            categories = mergedCategories
                            spiritsList = defaultSpiritsListT1
                        } else {
                            // Firestore document is up to date, use its values
                            categories = if (currentSeason == 1) parsedCategories.ifEmpty { defaultCategoriesT1 } else parsedCategories
                            spiritsList = if (currentSeason == 1) parsedSpiritsList.ifEmpty { defaultSpiritsListT1 } else parsedSpiritsList
                        }
                    }
                } else {
                    kevinList = emptyList()
                    aliList = emptyList()
                    kevinMastery = emptyList()
                    aliMastery = emptyList()
                    customNames = emptyMap()
                    customCategories = emptyMap()
                    if (currentSeason == 1) {
                        categories = defaultCategoriesT1
                        spiritsList = defaultSpiritsListT1
                    } else {
                        categories = emptyList()
                        spiritsList = emptyList()
                    }
                }
            }
        onDispose {
            listener.remove()
        }
    }

    val onSaveSpiritChanges: (String, String, String) -> Unit = { spiritId, newName, targetCategoryName ->
        val newCustomNames = if (newName.isBlank()) {
            customNames - spiritId
        } else {
            customNames + (spiritId to newName)
        }

        val updatedCategories = categories.map { category ->
            val updatedIds = if (category.name == targetCategoryName) {
                if (!category.spiritIds.contains(spiritId)) {
                    category.spiritIds + spiritId
                } else {
                    category.spiritIds
                }
            } else {
                category.spiritIds.filter { it != spiritId }
            }
            SpiritCategory(category.name, updatedIds)
        }

        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }

        val updates = mapOf(
            "custom_names" to newCustomNames,
            "categories" to categoriesMap,
            "schema_version" to 4
        )
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    val onSaveCategoryName: (String, String) -> Unit = { originalName, newName ->
        val cleanName = newName.trim()
        val newCustomCategories = if (cleanName.isBlank()) {
            customCategories - originalName
        } else {
            customCategories + (originalName to cleanName)
        }
        val targetCat = categories.find { it.name == originalName }
        val updatedCustomNames = customNames.toMutableMap()
        if (targetCat != null && cleanName.isNotBlank()) {
            val typeSuffixes = listOf(" Dorado", " Gomita", " Galaxia", " Gema", " Holofoil", " Cubo", " Extra", " Especial", " Hacker", " Hacker Dorado")
            targetCat.spiritIds.forEach { sid ->
                val currentName = customNames[sid] ?: run {
                    val idx = sid.toIntOrNull()?.minus(1) ?: 0
                    spiritNames.getOrElse(idx) { "Espíritu #$sid" }
                }
                val matchedSuffix = typeSuffixes.findLast { currentName.endsWith(it) } ?: ""
                updatedCustomNames[sid] = cleanName + matchedSuffix
            }
        }
        val updates = mapOf(
            "custom_categories" to newCustomCategories,
            "custom_names" to updatedCustomNames,
            "schema_version" to 4
        )
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    val onDeleteSpirit: (String) -> Unit = { spiritId ->
        val updatedSpiritsList = spiritsList.filter { it != spiritId }
        val updatedCategories = categories.map { category ->
            SpiritCategory(category.name, category.spiritIds.filter { it != spiritId })
        }
        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }
        val newCustomNames = customNames - spiritId
        val newKevinList = kevinList.filter { it != spiritId }
        val newAliList = aliList.filter { it != spiritId }
        val newKevinMastery = kevinMastery.filter { it != spiritId }
        val newAliMastery = aliMastery.filter { it != spiritId }

        val updates = mapOf(
            "spirits_list" to updatedSpiritsList,
            "categories" to categoriesMap,
            "custom_names" to newCustomNames,
            "kevin_list" to newKevinList,
            "ali_list" to newAliList,
            "kevin_mastery" to newKevinMastery,
            "ali_mastery" to newAliMastery,
            "schema_version" to 4
        )
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    val onDeleteCategory: (String) -> Unit = { categoryName ->
        val updatedCategories = categories.filter { it.name != categoryName }
        val categoriesMap = updatedCategories.map {
            mapOf("name" to it.name, "spiritIds" to it.spiritIds)
        }
        val newCustomCategories = customCategories - categoryName
        val updates = mapOf(
            "categories" to categoriesMap,
            "custom_categories" to newCustomCategories,
            "schema_version" to 4
        )
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    // Toggle check state function
    val onToggleCheck: (String, Boolean, String) -> Unit = { spiritId, currentlyChecked, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_list") kevinList else aliList
        val newList = if (currentlyChecked) {
            currentList.filter { it != spiritId }
        } else {
            currentList + spiritId
        }
        val updates = mutableMapOf<String, Any>(
            targetUserKey to newList,
            "schema_version" to 4
        )
        if (currentlyChecked) {
            val masteryKey = if (targetUserKey == "kevin_list") "kevin_mastery" else "ali_mastery"
            val currentMastery = if (targetUserKey == "kevin_list") kevinMastery else aliMastery
            val newMastery = currentMastery.filter { it != spiritId }
            updates[masteryKey] = newMastery
        }
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    // Toggle mastery state function
    val onToggleMastery: (String, Boolean, String) -> Unit = { spiritId, currentlyMastered, targetUserKey ->
        val currentList = if (targetUserKey == "kevin_mastery") kevinMastery else aliMastery
        val newList = if (currentlyMastered) {
            currentList.filter { it != spiritId }
        } else {
            currentList + spiritId
        }
        val updates = mapOf(
            targetUserKey to newList,
            "schema_version" to 4
        )
        db.collection(firestoreCollection).document(coupleId)
            .set(updates, SetOptions.merge())
    }

    var viewMode by remember { mutableStateOf("grupos") } // "grupos", "lista", "fortnite"
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }
    var filterMode by remember { mutableStateOf("todos") }
    var showFiltersMenu by remember { mutableStateOf(false) }
    // Clave de refresco: al incrementarla se invalida el caché de imágenes custom
    var imageRefreshKey by remember { mutableStateOf(0) }
    
    val matchesFilter: (String) -> Boolean = { spiritId ->
        val myMastery = if (isKevin) kevinMastery.contains(spiritId) else aliMastery.contains(spiritId)
        val otherMastery = if (isKevin) aliMastery.contains(spiritId) else kevinMastery.contains(spiritId)
        when (filterMode) {
            "todos" -> true
            "no_obtenidos" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                !ownedByMe
            }
            "obtenidos_otro_no_yo" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                val ownedByOther = if (isKevin) aliList.contains(spiritId) else kevinList.contains(spiritId)
                ownedByOther && !ownedByMe
            }
            "obtenidos_yo_no_otro" -> {
                val ownedByMe = if (isKevin) kevinList.contains(spiritId) else aliList.contains(spiritId)
                val ownedByOther = if (isKevin) aliList.contains(spiritId) else kevinList.contains(spiritId)
                ownedByMe && !ownedByOther
            }
            "sin_maestria" -> {
                !myMastery
            }
            "maestria_otro_no_yo" -> {
                otherMastery && !myMastery
            }
            "maestria_yo_no_otro" -> {
                myMastery && !otherMastery
            }
            else -> true
        }
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
                text = "COLECCIÓN ESPÍRITUS",
                fontFamily = Vt323,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            // Season Switcher Pill
            Box(
                modifier = Modifier
                    .border(2.dp, if (currentSeason == 2) Color(0xFFBD93F9) else borderColor)
                    .background(if (currentSeason == 2) Color(0xFFBD93F9).copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        currentSeason = if (currentSeason == 2) 1 else 2
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "T$currentSeason",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentSeason == 2) Color(0xFFBD93F9) else textColor
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (viewMode) {
                    "grupos" -> "[GRUPOS]"
                    "lista" -> "[LISTA]"
                    else -> "[FORTNITE]"
                },
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier
                    .clickable {
                        viewMode = when (viewMode) {
                            "grupos" -> "lista"
                            "lista" -> "fortnite"
                            else -> "grupos"
                        }
                    }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                Text(
                    text = "⋮",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor,
                    modifier = Modifier
                        .clickable { showFiltersMenu = true }
                        .padding(8.dp)
                )
                DropdownMenu(
                    expanded = showFiltersMenu,
                    onDismissRequest = { showFiltersMenu = false },
                    modifier = Modifier.background(cardBg).border(2.dp, borderColor)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "todos") "✓ TODOS" else "TODOS",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "todos"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "no_obtenidos") "✓ FALTANTES MÍOS" else "FALTANTES MÍOS",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "no_obtenidos"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "obtenidos_otro_no_yo") "✓ DEL OTRO QUE NO TENGO" else "DEL OTRO QUE NO TENGO",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "obtenidos_otro_no_yo"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "obtenidos_yo_no_otro") "✓ MÍOS QUE EL OTRO NO TIENE" else "MÍOS QUE EL OTRO NO TIENE",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "obtenidos_yo_no_otro"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "sin_maestria") "✓ SIN MAESTRÍA 👑" else "SIN MAESTRÍA 👑",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "sin_maestria"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "maestria_otro_no_yo") "✓ MAESTRÍA OTRO QUE NO TENGO 👑" else "MAESTRÍA OTRO QUE NO TENGO 👑",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "maestria_otro_no_yo"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (filterMode == "maestria_yo_no_otro") "✓ MI MAESTRÍA QUE OTRO NO TIENE 👑" else "MI MAESTRÍA QUE OTRO NO TIENE 👑",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            filterMode = "maestria_yo_no_otro"
                            showFiltersMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "RECARGAR IMÁGENES 🔄",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        },
                        onClick = {
                            imageRefreshKey++
                            showFiltersMenu = false
                        }
                    )
                }
            }
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
                            text = "Kevin",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${kevinList.size} / ${spiritsList.size}",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFBD93F9) else Color(0xFF4A2511)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👑 ${kevinMastery.size}",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFFF1FA8C) else Color(0xFFD4AF37)
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
                            text = "Ali",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${aliList.size} / ${spiritsList.size}",
                            fontFamily = Vt323,
                            fontSize = 22.sp,
                            color = if (isDark) Color(0xFFFF79C6) else Color(0xFF4A2511)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👑 ${aliMastery.size}",
                            fontFamily = Vt323,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFFF1FA8C) else Color(0xFFD4AF37)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cute Leaderboard message
                val leaderMsg = when {
                    kevinList.size > aliList.size -> "¡Kevin va a la cabeza!"
                    aliList.size > kevinList.size -> "¡Ali va a la cabeza!"
                    kevinList.size == aliList.size && kevinList.size > 0 -> {
                        when {
                            kevinMastery.size > aliMastery.size -> "¡Kevin lidera en maestría!"
                            aliMastery.size > kevinMastery.size -> "¡Ali lidera en maestría!"
                            else -> "¡Están empatados!"
                        }
                    }
                    else -> "¡Empiecen su colección!"
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
            val totalFiltered = spiritsList.count { matchesFilter(it) }
            if (totalFiltered == 0) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay espíritus que coincidan con el filtro.",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (viewMode == "fortnite") {
                val orderedIds = categories.flatMap { it.spiritIds }.distinct()
                val sortedSpirits = orderedIds.filter { spiritsList.contains(it) } + spiritsList.filter { !orderedIds.contains(it) }
                val filteredSpirits = sortedSpirits.filter { matchesFilter(it) }
                val chunkedSpirits = filteredSpirits.chunked(3)
                items(chunkedSpirits) { rowSpirits ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSpirits.forEach { spiritId ->
                            val nameIndex = spiritId.toInt() - 1
                            val hasKevin = kevinList.contains(spiritId)
                            val hasAli = aliList.contains(spiritId)
                            val hasKevinMastery = kevinMastery.contains(spiritId)
                            val hasAliMastery = aliMastery.contains(spiritId)
                            val spiritResId = remember(spiritId, context) {
                                val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                if (id != 0) id else android.R.drawable.ic_menu_gallery
                            }
                            val currentName = customNames[spiritId] ?: spiritNames.getOrElse(nameIndex) { "Espíritu #$spiritId" }
                            Box(modifier = Modifier.weight(1f)) {
                                SpiritGridCard(
                                    spiritId = spiritId,
                                    spiritResId = spiritResId,
                                    spiritName = currentName,
                                    hasKevin = hasKevin,
                                    hasAli = hasAli,
                                    hasKevinMastery = hasKevinMastery,
                                    hasAliMastery = hasAliMastery,
                                    isKevin = isKevin,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    cardBg = cardBg,
                                    isEditMode = isEditMode,
                                    onEditName = {
                                        editingSpiritId = spiritId
                                        editingSpiritInitialName = currentName
                                    },
                                    onDeleteSpirit = {
                                        deletingSpiritId = spiritId
                                    },
                                    onToggleCheck = onToggleCheck,
                                    onToggleMastery = onToggleMastery,
                                    customImageUrl = customImages[spiritId]
                                )
                            }
                        }
                        for (i in 0 until (3 - rowSpirits.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (viewMode == "lista") {
                val orderedIds = categories.flatMap { it.spiritIds }.distinct()
                val sortedSpirits = orderedIds.filter { spiritsList.contains(it) } + spiritsList.filter { !orderedIds.contains(it) }
                val filteredSpirits = sortedSpirits.filter { matchesFilter(it) }
                items(filteredSpirits, key = { "spirit_$it" }) { spiritId ->
                    val nameIndex = spiritId.toInt() - 1
                    val hasKevin = kevinList.contains(spiritId)
                    val hasAli = aliList.contains(spiritId)
                    val hasKevinMastery = kevinMastery.contains(spiritId)
                    val hasAliMastery = aliMastery.contains(spiritId)
                    val spiritResId = remember(spiritId, context) {
                        val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                        if (id != 0) id else android.R.drawable.ic_menu_gallery
                    }
                    val currentName = customNames[spiritId] ?: spiritNames.getOrElse(nameIndex) { "Espíritu #$spiritId" }
                    SpiritRow(
                        spiritId = spiritId,
                        spiritResId = spiritResId,
                        spiritName = currentName,
                        hasKevin = hasKevin,
                        hasAli = hasAli,
                        hasKevinMastery = hasKevinMastery,
                        hasAliMastery = hasAliMastery,
                        isKevin = isKevin,
                        isDark = isDark,
                        borderColor = borderColor,
                        textColor = textColor,
                        cardBg = cardBg,
                        isEditMode = isEditMode,
                        onEditName = {
                            editingSpiritId = spiritId
                            editingSpiritInitialName = currentName
                        },
                        onDeleteSpirit = {
                            deletingSpiritId = spiritId
                        },
                        onToggleCheck = onToggleCheck,
                        onToggleMastery = onToggleMastery,
                        customImageUrl = customImages[spiritId],
                        imageRefreshKey = imageRefreshKey
                    )
                }
            } else {
                categories.forEach { category ->
                    val filteredCategorySpiritIds = category.spiritIds.filter { matchesFilter(it) }
                    if (filteredCategorySpiritIds.isNotEmpty()) {
                        val isExpanded = expandedCategories.contains(category.name)
                        val categoryKevinCount = category.spiritIds.count { kevinList.contains(it) }
                        val categoryAliCount = category.spiritIds.count { aliList.contains(it) }
                        val categoryKevinMasteryCount = category.spiritIds.count { kevinMastery.contains(it) }
                        val categoryAliMasteryCount = category.spiritIds.count { aliMastery.contains(it) }
                        val displayCategoryName = customCategories[category.name] ?: category.name
                        
                        item(key = category.name) {
                            CategoryHeader(
                                category = category,
                                displayName = displayCategoryName,
                                isExpanded = isExpanded,
                                borderColor = borderColor,
                                cardBg = cardBg,
                                textColor = textColor,
                                kevinCount = categoryKevinCount,
                                aliCount = categoryAliCount,
                                kevinMasteryCount = categoryKevinMasteryCount,
                                aliMasteryCount = categoryAliMasteryCount,
                                isEditMode = isEditMode,
                                onEditCategoryName = {
                                    editingCategoryOriginalName = category.name
                                    editingCategoryInitialName = displayCategoryName
                                },
                                onDeleteCategory = {
                                    deletingCategoryName = category.name
                                },
                                onClick = {
                                    expandedCategories = if (isExpanded) {
                                        expandedCategories - category.name
                                    } else {
                                        expandedCategories + category.name
                                    }
                                }
                            )
                        }
                        
                        if (isExpanded) {
                            items(filteredCategorySpiritIds, key = { "spirit_$it" }) { spiritId ->
                                val index = spiritId.toInt() - 1
                                val hasKevin = kevinList.contains(spiritId)
                                val hasAli = aliList.contains(spiritId)
                                val hasKevinMastery = kevinMastery.contains(spiritId)
                                val hasAliMastery = aliMastery.contains(spiritId)
                                val spiritResId = remember(spiritId, context) {
                                    val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                    if (id != 0) id else android.R.drawable.ic_menu_gallery
                                }
                                val currentName = customNames[spiritId] ?: spiritNames.getOrElse(index) { "Espíritu #$spiritId" }
                                SpiritRow(
                                    spiritId = spiritId,
                                    spiritResId = spiritResId,
                                    spiritName = currentName,
                                    hasKevin = hasKevin,
                                    hasAli = hasAli,
                                    hasKevinMastery = hasKevinMastery,
                                    hasAliMastery = hasAliMastery,
                                    isKevin = isKevin,
                                    isDark = isDark,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    cardBg = cardBg,
                                    isEditMode = isEditMode,
                                    onEditName = {
                                        editingSpiritId = spiritId
                                        editingSpiritInitialName = currentName
                                    },
                                    onDeleteSpirit = {
                                        deletingSpiritId = spiritId
                                    },
                                    onToggleCheck = onToggleCheck,
                                    onToggleMastery = onToggleMastery,
                                    customImageUrl = customImages[spiritId],
                                    imageRefreshKey = imageRefreshKey
                                )
                            }
                        }
                    }
                }

                // Render "Sin Categoría / Sueltos" Virtual Category at the bottom!
                val categorizedIds = categories.flatMap { it.spiritIds }.toSet()
                val uncategorizedIds = spiritsList.filter { !categorizedIds.contains(it) && matchesFilter(it) }
                if (uncategorizedIds.isNotEmpty()) {
                    val virtualCategoryName = "Sin Categoría / Sueltos"
                    val isExpanded = expandedCategories.contains(virtualCategoryName)
                    val categoryKevinCount = uncategorizedIds.count { kevinList.contains(it) }
                    val categoryAliCount = uncategorizedIds.count { aliList.contains(it) }
                    val categoryKevinMasteryCount = uncategorizedIds.count { kevinMastery.contains(it) }
                    val categoryAliMasteryCount = uncategorizedIds.count { aliMastery.contains(it) }
                    
                    item(key = virtualCategoryName) {
                        CategoryHeader(
                            category = SpiritCategory(virtualCategoryName, uncategorizedIds),
                            displayName = virtualCategoryName,
                            isExpanded = isExpanded,
                            borderColor = borderColor,
                            cardBg = cardBg,
                            textColor = textColor,
                            kevinCount = categoryKevinCount,
                            aliCount = categoryAliCount,
                            kevinMasteryCount = categoryKevinMasteryCount,
                            aliMasteryCount = categoryAliMasteryCount,
                            isEditMode = false, // Virtual category cannot be renamed or deleted
                            onClick = {
                                expandedCategories = if (isExpanded) {
                                    expandedCategories - virtualCategoryName
                                } else {
                                    expandedCategories + virtualCategoryName
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        items(uncategorizedIds, key = { "spirit_$it" }) { spiritId ->
                            val index = spiritId.toInt() - 1
                            val hasKevin = kevinList.contains(spiritId)
                            val hasAli = aliList.contains(spiritId)
                            val hasKevinMastery = kevinMastery.contains(spiritId)
                            val hasAliMastery = aliMastery.contains(spiritId)
                            val spiritResId = remember(spiritId, context) {
                                val id = context.resources.getIdentifier("ic_spirit_$spiritId", "drawable", context.packageName)
                                if (id != 0) id else android.R.drawable.ic_menu_gallery
                            }
                            val currentName = customNames[spiritId] ?: spiritNames.getOrElse(index) { "Espíritu #$spiritId" }
                            SpiritRow(
                                spiritId = spiritId,
                                spiritResId = spiritResId,
                                spiritName = currentName,
                                hasKevin = hasKevin,
                                hasAli = hasAli,
                                hasKevinMastery = hasKevinMastery,
                                hasAliMastery = hasAliMastery,
                                isKevin = isKevin,
                                isDark = isDark,
                                borderColor = borderColor,
                                textColor = textColor,
                                cardBg = cardBg,
                                isEditMode = isEditMode,
                                onEditName = {
                                    editingSpiritId = spiritId
                                    editingSpiritInitialName = currentName
                                },
                                onDeleteSpirit = {
                                    deletingSpiritId = spiritId
                                },
                                onToggleCheck = onToggleCheck,
                                onToggleMastery = onToggleMastery,
                                customImageUrl = customImages[spiritId],
                                imageRefreshKey = imageRefreshKey
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingSpiritId != null) {
        val spiritId = editingSpiritId!!
        val currentCat = categories.find { it.spiritIds.contains(spiritId) }
        val currentCatName = currentCat?.name ?: ""
        val displayCategoryName = customCategories[currentCatName] ?: currentCatName
        
        var selectedCategoryName by remember(spiritId) { mutableStateOf(currentCatName) }
        var selectedType by remember(spiritId) {
            val name = editingSpiritInitialName
            val suffix = when {
                name == displayCategoryName -> "Normal"
                name.startsWith(displayCategoryName) -> name.substring(displayCategoryName.length).trim()
                else -> "Normal"
            }
            val types = listOf("Dorado", "Gomita", "Galaxia", "Gema", "Holofoil", "Cubo", "Extra", "Especial")
            val t = if (types.contains(suffix)) suffix else "Normal"
            mutableStateOf(t)
        }
        var tempName by remember(spiritId) { mutableStateOf(editingSpiritInitialName) }
        
        val updateGeneratedName = { categoryName: String, typeName: String ->
            val catDisplayName = customCategories[categoryName] ?: categoryName
            tempName = if (typeName == "Normal") {
                catDisplayName
            } else {
                "$catDisplayName $typeName"
            }
        }

        AlertDialog(
            onDismissRequest = { editingSpiritId = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "EDITAR ESPÍRITU",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "ID del Espíritu: $spiritId",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Nombre:",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor
                        ),
                        placeholder = {
                            Text(
                                "Escribe el nombre...",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category selector column
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "Categoría:",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, borderColor)
                                    .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFBEA))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    val isSelected = selectedCategoryName.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedCategoryName = ""
                                                // If category is removed, we don't automatically generate name
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "[Sin Categoría]" + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                                items(categories) { cat ->
                                    val displayCat = customCategories[cat.name] ?: cat.name
                                    val isSelected = selectedCategoryName == cat.name
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedCategoryName = cat.name 
                                                updateGeneratedName(cat.name, selectedType)
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = displayCat + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Type / Variant selector column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tipo:",
                                fontFamily = Vt323,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, borderColor)
                                    .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFBEA))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val types = listOf("Normal", "Dorado", "Gomita", "Galaxia", "Gema", "Holofoil", "Cubo", "Extra", "Especial")
                                items(types) { t ->
                                    val isSelected = selectedType == t
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) borderColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                selectedType = t 
                                                if (selectedCategoryName.isNotEmpty()) {
                                                    updateGeneratedName(selectedCategoryName, t)
                                                }
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = t + (if (isSelected) " ✓" else ""),
                                            fontFamily = Vt323,
                                            fontSize = 16.sp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.Red)
                            .clickable {
                                deletingSpiritId = spiritId
                                editingSpiritId = null
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "ELIMINAR",
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .clickable { editingSpiritId = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "CANCELAR",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(2.dp, borderColor)
                                .clickable {
                                    onSaveSpiritChanges(spiritId, tempName, selectedCategoryName)
                                    editingSpiritId = null
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "GUARDAR",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        )
    }

    if (editingCategoryOriginalName != null) {
        var tempCategoryName by remember(editingCategoryOriginalName) { mutableStateOf(editingCategoryInitialName) }
        AlertDialog(
            onDismissRequest = { editingCategoryOriginalName = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "EDITAR CATEGORÍA",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Categoría Original: $editingCategoryOriginalName",
                        fontFamily = Vt323,
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempCategoryName,
                        onValueChange = { tempCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Vt323,
                            fontSize = 18.sp,
                            color = textColor
                        ),
                        placeholder = {
                            Text(
                                "Escribe el nombre de la categoría...",
                                fontFamily = Vt323,
                                fontSize = 18.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            editingCategoryOriginalName?.let { orig ->
                                onSaveCategoryName(orig, tempCategoryName)
                            }
                            editingCategoryOriginalName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "GUARDAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { editingCategoryOriginalName = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
    }



    if (deletingSpiritId != null) {
        val spiritId = deletingSpiritId!!
        AlertDialog(
            onDismissRequest = { deletingSpiritId = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "ELIMINAR ESPÍRITU",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar este espíritu de la lista permanente? Esta acción no se puede deshacer.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            onDeleteSpirit(spiritId)
                            deletingSpiritId = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ELIMINAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { deletingSpiritId = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
    }

    if (deletingCategoryName != null) {
        val categoryName = deletingCategoryName!!
        val displayName = customCategories[categoryName] ?: categoryName
        AlertDialog(
            onDismissRequest = { deletingCategoryName = null },
            modifier = Modifier.border(3.dp, borderColor),
            containerColor = cardBg,
            title = {
                Text(
                    text = "ELIMINAR CATEGORÍA",
                    fontFamily = Vt323,
                    fontSize = 24.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar la categoría \"$displayName\"? Los espíritus que pertenezcan a ella se mantendrán en la lista general pero quedarán sin categoría.",
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable {
                            onDeleteCategory(categoryName)
                            deletingCategoryName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ELIMINAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor)
                        .clickable { deletingCategoryName = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCELAR",
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        )
    }
}

