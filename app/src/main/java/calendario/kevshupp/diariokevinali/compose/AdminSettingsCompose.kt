package calendario.kevshupp.diariokevinali.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AdminSettingsCompose(
    currentTheme: String,
    onResetRanking: () -> Unit,
    onResetMinigames: () -> Unit,
    onResetPets: () -> Unit,
    onIncorrectPassword: () -> Unit
) {
    val isDark = currentTheme == "Pixel Oscuro"
    val isMono = currentTheme == "Pixel Monocromático"

    val textColor = when {
        isDark -> Color.White
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val borderColor = when {
        isDark -> Color(0xFF91465F)
        isMono -> Color.Black
        else -> Color(0xFF4A2511)
    }

    val boxBackground = when {
        isDark -> Color(0xFF282828)
        isMono -> Color.White
        else -> Color(0xFFFFFBEA)
    }

    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    if (!isUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, borderColor)
                .background(boxBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔐 ACCESO DE ADMINISTRADOR",
                fontFamily = Vt323,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ingresa la clave para acceder a las herramientas de control maestro y reinicio del sistema.",
                fontFamily = Vt323,
                fontSize = 18.sp,
                color = textColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    errorMessage = null
                },
                label = { Text("Contraseña", fontFamily = Vt323, fontSize = 16.sp) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor.copy(alpha = 0.6f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = textColor
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp)
                    .clickable {
                        if (passwordInput == "123") {
                            isUnlocked = true
                            errorMessage = null
                            passwordInput = ""
                        } else {
                            errorMessage = "Contraseña incorrecta ❌"
                            onIncorrectPassword()
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .offset(y = 4.dp)
                        .background(borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .border(2.dp, borderColor)
                        .background(if (isDark) Color(0xFFB71C1C) else Color(0xFFEC4899)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔓 DESBLOQUEAR PANEL",
                        fontFamily = Vt323,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, borderColor)
                    .background(boxBackground)
                    .padding(14.dp)
            ) {
                Text(
                    text = "👑 PANEL DE CONTROL (ADMIN)",
                    fontFamily = Vt323,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
                )
                Text(
                    text = "Herramientas de reinicio de base de datos Firestore y sincronización.",
                    fontFamily = Vt323,
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            AdminActionCard(
                icon = "🏆",
                title = "REINICIAR RANKING DE CUIDADORES",
                description = "Pone a 0 los puntos totales y por mascota de Kevin y Ali, junto con todos los contadores de interacción (comida, baño, pelota y minijuegos).",
                buttonText = "🔄 REINICIAR RANKING",
                buttonBg = if (isDark) Color(0xFFC2185B) else Color(0xFFD81B60),
                boxBackground = boxBackground,
                textColor = textColor,
                borderColor = borderColor
            ) {
                pendingAction = "ranking"
            }

            AdminActionCard(
                icon = "🎮",
                title = "REINICIAR ESTADÍSTICA DE MINIJUEGOS",
                description = "Restablece los récords máximos (High Scores) de Flappy y Snake de ambos a 0, y limpia los límites diarios para poder ganar recompensas nuevamente hoy.",
                buttonText = "🔄 REINICIAR MINIJUEGOS",
                buttonBg = if (isDark) Color(0xFF0288D1) else Color(0xFF1976D2),
                boxBackground = boxBackground,
                textColor = textColor,
                borderColor = borderColor
            ) {
                pendingAction = "minigames"
            }

            AdminActionCard(
                icon = "🐾",
                title = "REINICIAR A LAS MASCOTAS",
                description = "Restablece a Thor y Cuky al Nivel 1 con 0 EXP, 100% de felicidad, 100% de limpieza, 0 hambre y despiertos.",
                buttonText = "⚠️ REINICIAR MASCOTAS",
                buttonBg = if (isDark) Color(0xFFB71C1C) else Color(0xFFD32F2F),
                boxBackground = boxBackground,
                textColor = textColor,
                borderColor = borderColor
            ) {
                pendingAction = "pets"
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (pendingAction != null) {
        val (actionTitle, actionDesc) = when (pendingAction) {
            "ranking" -> "Reiniciar Ranking" to "¿Estás seguro de reiniciar todo el ranking y contadores de cuidadores a 0?"
            "minigames" -> "Reiniciar Minijuegos" to "¿Estás seguro de reiniciar los récords (High Scores) y registros de minijuegos a 0?"
            else -> "Reiniciar Mascotas" to "¿Estás seguro de reiniciar a Thor y Cuky al Nivel 1 y 0 EXP?"
        }

        Dialog(onDismissRequest = { pendingAction = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .border(3.dp, borderColor),
                color = boxBackground,
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ CONFIRMACIÓN",
                        fontFamily = Vt323,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = actionDesc,
                        fontFamily = Vt323,
                        fontSize = 18.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { pendingAction = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .border(2.dp, borderColor),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF424242) else Color(0xFFCCCCCC)),
                            shape = RectangleShape
                        ) {
                            Text("CANCELAR", fontFamily = Vt323, fontSize = 18.sp, color = textColor)
                        }

                        Button(
                            onClick = {
                                when (pendingAction) {
                                    "ranking" -> onResetRanking()
                                    "minigames" -> onResetMinigames()
                                    "pets" -> onResetPets()
                                }
                                pendingAction = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .border(2.dp, borderColor),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RectangleShape
                        ) {
                            Text("SÍ, REINICIAR", fontFamily = Vt323, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActionCard(
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    buttonBg: Color,
    boxBackground: Color,
    textColor: Color,
    borderColor: Color,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(boxBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = Vt323,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontFamily = Vt323,
            fontSize = 16.sp,
            color = textColor.copy(alpha = 0.85f),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onAction() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .offset(y = 4.dp)
                    .background(borderColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .border(2.dp, borderColor)
                    .background(buttonBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontFamily = Vt323,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
