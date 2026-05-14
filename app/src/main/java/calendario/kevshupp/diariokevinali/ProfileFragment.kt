package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.ProfileScreen

class ProfileFragment : Fragment() {
    private var userId: String? = null
    private var partnerId: String? = null
    private var theme: String = "Pixel Claro"
    private var currentUserName: String by mutableStateOf("Kevin")
    private var currentUserImageUri: String? by mutableStateOf(null)

    companion object {
        @JvmStatic
        fun newInstance(userId: String, partnerId: String, theme: String): ProfileFragment {
            val f = ProfileFragment()
            val a = Bundle()
            a.putString("userId", userId)
            a.putString("partnerId", partnerId)
            a.putString("theme", theme)
            f.arguments = a
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
        partnerId = arguments?.getString("partnerId")
        theme = arguments?.getString("theme") ?: "Pixel Claro"
        loadPrefs()
    }

    fun setProfileImage(url: String) {
        currentUserImageUri = url
    }

    override fun onResume() {
        super.onResume()
        loadPrefs()
    }

    private fun loadPrefs() {
        val prefs = activity?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
        currentUserName = prefs?.getString("userName", currentUserName) ?: currentUserName
        currentUserImageUri = prefs?.getString("userImage", currentUserImageUri)
        theme = prefs?.getString("theme", theme) ?: theme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val act = activity as? MainActivity
                ProfileScreen(
                    currentUserName = currentUserName,
                    currentUserImageUri = currentUserImageUri,
                    theme = theme,
                    coupleId = partnerId,
                    currentUserId = userId ?: "",
                    onPickImage = { act?.pickImage(1) },
                    onSaveProfile = { },
                    onLogout = { act?.logout() }
                )
            }
        }
    }
}
