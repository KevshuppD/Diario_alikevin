package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.MiscScreen

class MiscFragment : Fragment() {
    private var theme: String = "Pixel Claro"

    companion object {
        @JvmStatic
        fun newInstance(theme: String): MiscFragment {
            val f = MiscFragment()
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
                androidx.compose.material3.MaterialTheme {
                    MiscScreen(
                        theme = theme,
                        onBack = { (activity as? MainActivity)?.onBackPressedDispatcher?.onBackPressed() }
                    )
                }
            }
        }
    }
}
