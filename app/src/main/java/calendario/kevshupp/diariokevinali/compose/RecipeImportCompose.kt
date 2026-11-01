package calendario.kevshupp.diariokevinali.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class ImportedRecipe(val title: String, val ingredients: String, val steps: String, val sourceUrl: String? = null)

sealed class RecipeParseResult {
    data class Ok(val recipe: ImportedRecipe) : RecipeParseResult()
    data class Error(val message: String) : RecipeParseResult()
}

/** Prompt para pegar en una IA con acceso a internet; pide el JSON que entiende [parseRecipeJson]. */
fun buildRecipePrompt(url: String): String = """
Extrae la receta de este enlace (video de YouTube o página web):
$url

Entrega el resultado listo para copiar y pegar: responde ÚNICAMENTE con un bloque de código JSON (con su botón de copiar), sin texto antes ni después, con este formato:
{
  "title": "Nombre de la receta",
  "source": "$url",
  "ingredients": ["**200 g** de harina", "**2** huevos", "Sal"],
  "steps": ["Mezcla todo durante **5 min**.", "Hornea **20 min** a **180 °C**."]
}

Reglas:
- Escribe todo en español, con cantidades y unidades tal como aparecen en la fuente.
- Cada ingrediente y cada paso va en su propio elemento, sin numeración.
- Copia "source" tal cual.
- Es obligatorio escribir los ** dentro del JSON: en cada ingrediente, la cantidad va entre ** (usa números y abreviaturas como g, ml, cda, cdta); si no tiene cantidad, va sin **.
- En los pasos, marca con ** solo lo clave, como tiempos y temperaturas. No uses otro formato Markdown.
- Usa solo lo que dice la fuente. No inventes ni completes ingredientes, cantidades o pasos.
- Si no puedes acceder al enlace o no contiene una receta, responde solo con un bloque de código JSON: {"error": "motivo"}
""".trim()

private val BOLD_MARKUP = Regex("""\*\*([^*\n]+)\*\*""")

/** Convierte **texto** en negrita. */
fun boldMarkup(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    BOLD_MARKUP.findAll(text).forEach { match ->
        append(text.substring(last, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        last = match.range.last + 1
    }
    append(text.substring(last))
}

private val LEADING_AMOUNT = Regex(
    """^(?:\d+(?:[.,/]\d+)?|medio|media|un|una|dos|tres|cuatro|cinco|seis)(?:\s+(?:o|a|y)\s+\d+(?:[.,/]\d+)?)?""" +
        """(?:\s+(?:de\s+)?(?:kg|g|gr|ml|l|cc|cda|cdas|cdta|cdtas|tazas?|cucharadas?|cucharaditas?|unidad(?:es)?|dientes?|hojas?|latas?|pizcas?|tarros?)""" +
        """(?:\s+(?:grandes?|pequeñas?|soperas?))?)?(?=[\s,]|$)""",
    RegexOption.IGNORE_CASE
)

/** Ingrediente con su cantidad en negrita: respeta los ** de la receta o detecta la cantidad inicial. */
fun ingredientMarkup(line: String): AnnotatedString {
    if (BOLD_MARKUP.containsMatchIn(line)) return boldMarkup(line)
    val amount = LEADING_AMOUNT.find(line.trimStart())?.value ?: return AnnotatedString(line)
    val trimmed = line.trimStart()
    return boldMarkup("**$amount**" + trimmed.substring(amount.length))
}

fun stripBoldMarkup(text: String): String = BOLD_MARKUP.replace(text) { it.groupValues[1] }

private val HTTP_URL = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

/** Saca el enlace del texto pegado: al compartir, muchas apps agregan texto o caracteres alrededor. */
fun extractRecipeUrl(text: String): String? {
    val url = HTTP_URL.find(text)?.value ?: return null
    return url.takeUnless { android.net.Uri.parse(it).host.isNullOrBlank() }
}

/** Acepta la respuesta tal como la copia el usuario, incluso con texto o bloque de código alrededor. */
fun parseRecipeJson(raw: String): RecipeParseResult {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start < 0 || end <= start) return RecipeParseResult.Error("No encontré un JSON en el texto pegado.")

    val json = try {
        JSONObject(raw.substring(start, end + 1))
    } catch (e: JSONException) {
        return RecipeParseResult.Error("El JSON está incompleto o mal formado.")
    }

    val aiError = jsonText(json, "error")
    if (aiError.isNotEmpty()) return RecipeParseResult.Error("La IA no pudo leer la receta: $aiError")

    val title = jsonText(json, "title")
    val ingredients = jsonLines(json, "ingredients", numbered = false)
    val steps = jsonLines(json, "steps", numbered = true)
    return when {
        title.isEmpty() -> RecipeParseResult.Error("Falta el título de la receta (\"title\").")
        ingredients.isEmpty() -> RecipeParseResult.Error("Faltan los ingredientes (\"ingredients\").")
        steps.isEmpty() -> RecipeParseResult.Error("Faltan los pasos (\"steps\").")
        else -> RecipeParseResult.Ok(
            ImportedRecipe(title, ingredients, steps, extractRecipeUrl(jsonText(json, "source")))
        )
    }
}

private fun jsonText(json: JSONObject, key: String): String =
    if (json.isNull(key)) "" else (json.opt(key) as? String).orEmpty().trim()

private fun jsonLines(json: JSONObject, key: String, numbered: Boolean): String {
    val array = json.opt(key) as? JSONArray ?: return jsonText(json, key)
    val lines = (0 until array.length())
        .mapNotNull { i -> (array.opt(i) as? String)?.trim()?.takeIf { it.isNotEmpty() } }
    return lines.mapIndexed { i, line -> if (numbered) "${i + 1}. $line" else line }.joinToString("\n")
}

private class ImportColors(val text: Color, val bg: Color, val border: Color, val error: Color)

@Composable
private fun importColors(theme: String) = when (theme) {
    "Pixel Oscuro" -> ImportColors(Color.White, Color(0xFF282828), Color(0xFF91465F), Color(0xFFFF8A80))
    "Pixel Monocromático" -> ImportColors(Color.Black, Color.White, Color.Black, Color.Black)
    else -> ImportColors(Color(0xFF4A2511), Color(0xFFFFFBEA), Color(0xFF4A2511), Color(0xFFB91C1C))
}

@Composable
private fun ImportDialogFrame(
    title: String,
    colors: ImportColors,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, colors.border)
                .background(colors.bg)
                .padding(14.dp)
        ) {
            Text(title, fontFamily = Vt323, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ImportButton(text: String, colors: ImportColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .border(2.dp, colors.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = Vt323, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.text)
    }
}

@Composable
private fun ImportField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: ImportColors,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        placeholder = { Text(placeholder, fontFamily = Vt323, fontSize = 18.sp, color = colors.text.copy(alpha = 0.6f)) },
        textStyle = TextStyle(fontFamily = Vt323, fontSize = 18.sp, color = colors.text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.border,
            unfocusedBorderColor = colors.border,
            cursorColor = colors.text
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun RecipePromptDialog(theme: String, onDismiss: () -> Unit) {
    val colors = importColors(theme)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    ImportDialogFrame("🔗 Prompt desde un enlace", colors, onDismiss) {
        Text(
            "Pega un video de YouTube o una web de recetas. Copiaré un prompt para pegarlo en una IA con acceso a internet.",
            fontFamily = Vt323, fontSize = 18.sp, color = colors.text
        )
        Spacer(Modifier.height(8.dp))
        ImportField(url, { url = it; showError = false }, "https://...", colors, singleLine = true)
        if (showError) {
            Text("No encontré un enlace válido con http:// o https://", fontFamily = Vt323, fontSize = 17.sp, color = colors.error)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImportButton("Cancelar", colors, Modifier.weight(1f), onDismiss)
            ImportButton("Copiar prompt", colors, Modifier.weight(1f)) {
                val validUrl = extractRecipeUrl(url)
                if (validUrl != null) {
                    clipboard.setText(AnnotatedString(buildRecipePrompt(validUrl)))
                    Toast.makeText(context, "Prompt copiado 📋", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    showError = true
                }
            }
        }
    }
}

@Composable
fun RecipePasteDialog(theme: String, onSave: (ImportedRecipe) -> Unit, onDismiss: () -> Unit) {
    val colors = importColors(theme)
    val clipboard = LocalClipboardManager.current
    var raw by remember { mutableStateOf("") }
    val result = remember(raw) { if (raw.isBlank()) null else parseRecipeJson(raw) }

    ImportDialogFrame("📋 Pegar receta", colors, onDismiss) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Pega el JSON que te respondió la IA.", fontFamily = Vt323, fontSize = 18.sp, color = colors.text)
            Spacer(Modifier.height(8.dp))
            ImportField(raw, { raw = it }, "{ \"title\": ... }", colors, Modifier.heightIn(min = 110.dp, max = 180.dp))
            Spacer(Modifier.height(8.dp))
            ImportButton("Pegar del portapapeles", colors, Modifier.fillMaxWidth()) {
                clipboard.getText()?.text?.let { raw = it }
            }
            when (result) {
                is RecipeParseResult.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Text(result.message, fontFamily = Vt323, fontSize = 18.sp, color = colors.error)
                }
                is RecipeParseResult.Ok -> {
                    Spacer(Modifier.height(8.dp))
                    Text(result.recipe.title, fontFamily = Vt323, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("Ingredientes", fontFamily = Vt323, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(boldMarkup(result.recipe.ingredients), fontFamily = Vt323, fontSize = 18.sp, color = colors.text)
                    Spacer(Modifier.height(4.dp))
                    Text("Pasos", fontFamily = Vt323, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(boldMarkup(result.recipe.steps), fontFamily = Vt323, fontSize = 18.sp, color = colors.text)
                }
                null -> Unit
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImportButton("Cancelar", colors, Modifier.weight(1f), onDismiss)
            if (result is RecipeParseResult.Ok) {
                ImportButton("Guardar receta", colors, Modifier.weight(1f)) { onSave(result.recipe) }
            }
        }
    }
}
