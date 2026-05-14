package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.AlbumScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class AlbumFragment : Fragment() {
    private var coupleId: String? = null
    private var userId: String? = null
    private var userName: String? = null
    private var userImageUri: String? = null
    private var theme: String = "Pixel Claro"
    private val db = FirebaseFirestore.getInstance()
    private var albumListener: ListenerRegistration? = null
    private var albumManager: AlbumManager? = null

    companion object {
        @JvmStatic
        fun newInstance(coupleId: String, userId: String, userName: String, userImageUri: String?, theme: String): AlbumFragment {
            val fragment = AlbumFragment()
            val args = Bundle()
            args.putString("coupleId", coupleId)
            args.putString("userId", userId)
            args.putString("userName", userName)
            args.putString("userImageUri", userImageUri)
            args.putString("theme", theme)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            coupleId = arguments?.getString("coupleId")
            userId = arguments?.getString("userId")
            userName = arguments?.getString("userName")
            userImageUri = arguments?.getString("userImageUri")
            theme = arguments?.getString("theme") ?: "Pixel Claro"
        }
        val safeCoupleId = coupleId ?: ""
        val safeUserId = userId ?: ""
        val safeUserName = userName ?: ""
        albumManager = AlbumManager(requireContext(), safeCoupleId, safeUserId, safeUserName, userImageUri)
        albumManager?.setTheme(theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var moments by remember { mutableStateOf(listOf<Message>()) }
                val isDark = theme == "Pixel Oscuro"
                val activityRef = activity as? MainActivity

                LaunchedEffect(Unit) {
                    if (coupleId.isNullOrEmpty()) {
                        val prefs = activityRef?.getSharedPreferences("DiarioPrefs", android.content.Context.MODE_PRIVATE)
                        coupleId = prefs?.getString("coupleId", coupleId)
                        userId = prefs?.getString("userId", userId)
                        userName = prefs?.getString("userName", userName)
                        userImageUri = prefs?.getString("userImage", userImageUri)
                        theme = prefs?.getString("theme", theme) ?: theme
                        albumManager?.setTheme(theme)
                    }
                }

                DisposableEffect(coupleId) {
                    albumListener?.remove()
                    albumListener = coupleId?.let { id ->
                        db.collection("messages")
                            .whereEqualTo("partnerId", id)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { shots, _ ->
                                if (shots != null) {
                                    val items = shots.mapNotNull { doc ->
                                        val m = doc.toObject(Message::class.java)
                                        if (m.content?.startsWith("[ALBUM]") == true) m else null
                                    }
                                    moments = items
                                }
                            }
                    }
                    onDispose {
                        albumListener?.remove()
                        albumListener = null
                    }
                }

                AlbumScreen(
                    moments = moments,
                    isDark = isDark,
                    onAddMoment = {
                        albumManager?.showAddMomentDialog(object : AlbumManager.AlbumCallback {
                            override fun onPickImage() {
                                activityRef?.pickImage(4)
                            }

                            override fun onMomentSaved() {
                                activityRef?.sendNotificationV1("Nuevo momento en el álbum 📸", null)
                            }
                        })
                    },
                    onOpenMoment = { msg ->
                        albumManager?.showAlbumDetail(msg)
                    },
                    onEditMoment = { msg ->
                        albumManager?.showEditAlbumDialog(msg)
                    },
                    onDeleteMoment = { msg ->
                        msg.messageId?.let { db.collection("messages").document(it).delete() }
                    },
                    isOwner = { msg -> msg.authorId == userId }
                )
            }
        }
    }
}
