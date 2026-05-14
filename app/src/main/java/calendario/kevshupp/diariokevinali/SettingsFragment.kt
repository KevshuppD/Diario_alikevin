package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.SettingsScreen

class SettingsFragment : Fragment() {
    private var theme: String = "Pixel Claro"

    companion object {
        @JvmStatic
        fun newInstance(userId: String, partnerId: String, theme: String): SettingsFragment {
            val f = SettingsFragment()
            val a = Bundle()
            a.putString("theme", theme)
            f.arguments = a
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme = arguments?.getString("theme") ?: "Pixel Claro"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val act = activity as? MainActivity
                val prefs = act?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                
                // Usar remember para que Compose mantenga el estado correctamente
                var currentTheme by remember { mutableStateOf(theme) }

                androidx.compose.material3.MaterialTheme {
                    SettingsScreen(
                        currentTheme = currentTheme,
                        versionName = BuildConfig.VERSION_NAME,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            theme = newTheme // Actualizar la propiedad del fragmento también
                            prefs?.edit()?.putString("theme", newTheme)?.apply()
                            act?.applyTheme(newTheme)
                        },
                        onCheckUpdates = {
                            act?.getUpdateManager()?.checkForUpdates(object : UpdateManager.UpdateCallback {
                                override fun onUpdateAvailable(url: String) { act.showUpdateDialog(url) }
                                override fun onNoUpdate() {}
                                override fun onDownloadProgress(progress: Int) {}
                                override fun onDownloadComplete() {}
                            })
                        },
                        onLogout = { act?.logout() },
                        onBack = { act?.onBackPressedDispatcher?.onBackPressed() },
                        onColorSelect = { colorHex ->
                            val isDark = currentTheme == "Pixel Oscuro"
                            if (isDark) {
                                prefs?.edit()?.putString("darkColor", colorHex)?.apply()
                                act?.applyTheme(currentTheme, null, colorHex)
                            } else {
                                prefs?.edit()?.putString("lightColor", colorHex)?.apply()
                                act?.applyTheme(currentTheme, colorHex, null)
                            }
                        }
                    )
                }
            }
        }
    }
}
