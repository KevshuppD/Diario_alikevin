package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.ProfileScreen
import com.google.firebase.firestore.FirebaseFirestore

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
                    onSaveProfile = { newName, newImage ->
                        if (userId != null) {
                            val db = FirebaseFirestore.getInstance()
                            val updates = mutableMapOf<String, Any>()
                            updates["userName"] = newName
                            updates["userId"] = userId!!
                            if (partnerId != null) updates["coupleId"] = partnerId!!
                            if (newImage != null) updates["profileImageUrl"] = newImage
                            
                            db.collection("users").document(userId!!).set(updates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    val prefs = act?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                                    prefs?.edit()?.putString("userName", newName)?.apply()
                                    if (newImage != null) prefs?.edit()?.putString("userImage", newImage)?.apply()
                                    
                                    currentUserName = newName
                                    act?.runOnUiThread {
                                        Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    },
                    onLogout = { act?.logout() }
                )
            }
        }
    }
}
